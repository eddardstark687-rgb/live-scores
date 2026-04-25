package com.pitchpulse.ui.viewmodel

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pitchpulse.core.network.RetrofitClient
import com.pitchpulse.core.util.DateObserver
import com.pitchpulse.data.local.LiveScoresDatabase
import com.pitchpulse.data.model.Match
import com.pitchpulse.data.repository.FootballRepository
import com.pitchpulse.ui.state.MatchUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private const val TAG = "API_DEBUG"

@OptIn(ExperimentalCoroutinesApi::class)
class MatchViewModel(application: Application) : AndroidViewModel(application) {

    private val db = LiveScoresDatabase.getInstance(application)
    private val repository = FootballRepository(
        api = RetrofitClient.api,
        dao = db.dao,
        favoriteTeamDao = db.favoriteTeamDao
    )

    private val dateFormat    = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val dayNameFormat = SimpleDateFormat("EEE", Locale.US)
    private val dayNumFormat  = SimpleDateFormat("dd", Locale.US)

    // ── Single source of truth ────────────────────────────────────────────
    private val _selectedDate = MutableStateFlow(dateFormat.format(Calendar.getInstance().time))
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _uiState = MutableStateFlow<MatchUiState>(MatchUiState.Loading)
    val uiState: StateFlow<MatchUiState> = _uiState.asStateFlow()

    // Upcoming fixtures for favorite teams — loaded separately, not inside the main flow
    private val _favoriteUpcoming = MutableStateFlow<List<Match>>(emptyList())

    val availableDates: StateFlow<List<Triple<String, String, String>>> =
        _selectedDate
            .map { buildDateList() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), buildDateList())

    // ── Foreground detection via ActivityLifecycleCallbacks ───────────────
    private var startedCount = 0
    private val lifecycleCallbacks = object : Application.ActivityLifecycleCallbacks {
        override fun onActivityStarted(activity: Activity) {
            if (startedCount == 0) onAppForegrounded()
            startedCount++
        }
        override fun onActivityStopped(activity: Activity)                          { startedCount-- }
        override fun onActivityCreated(activity: Activity, s: Bundle?)              {}
        override fun onActivityResumed(activity: Activity)                          {}
        override fun onActivityPaused(activity: Activity)                           {}
        override fun onActivitySaveInstanceState(activity: Activity, s: Bundle)     {}
        override fun onActivityDestroyed(activity: Activity)                        {}
    }

    private var pollingJob: kotlinx.coroutines.Job? = null
    private var lastForegroundSyncTime = 0L

    // ── Init ──────────────────────────────────────────────────────────────
    init {
        getApplication<Application>().registerActivityLifecycleCallbacks(lifecycleCallbacks)
        startMidnightObserver()
        startDataObserver()
        startSyncObserver()
        startFavoriteUpcomingObserver()
    }

    /**
     * Detects midnight date shifts and updates the selected date automatically.
     */
    private fun startMidnightObserver() {
        viewModelScope.launch {
            DateObserver.dateFlow().collect { newDate ->
                Log.d(TAG, "Midnight shift detected → $newDate")
                _selectedDate.value = newDate
            }
        }
    }

    /**
     * Pure DB observation → builds UI state.
     * NO API calls here. API is triggered separately in [startSyncObserver].
     */
    private fun startDataObserver() {
        viewModelScope.launch {
            _selectedDate
                .flatMapLatest { date ->
                    val coreFlow = combine(
                        repository.getDailyMatches(date),
                        repository.getFavoriteTeams().onStart { emit(emptyList()) },
                        repository.getFavoriteMatches().onStart { emit(emptyList()) }
                    ) { daily, teams, favMatches ->
                        val filteredDaily = daily.filter { it.isFavoriteLeague }
                        Triple(filteredDaily, teams, favMatches)
                    }

                    // Step 2: combine with dates ribbon + upcoming favorites
                    coreFlow.combine(
                        availableDates.combine(_favoriteUpcoming) { dates, upcoming ->
                            Pair(dates, upcoming)
                        }
                    ) { (daily, teams, favMatches), (dates, upcoming) ->
                        MatchUiState.Success(
                            dailyMatches            = daily,
                            favorites               = favMatches,
                            favoriteTeams           = teams,
                            favoriteUpcomingMatches = upcoming,
                            selectedDate            = date,
                            availableDates          = dates
                        )
                    }
                }
                .catch { e ->
                    if (e is CancellationException) throw e
                    Log.e(TAG, "Fatal data flow error: ${e.message}", e)
                    _uiState.value = MatchUiState.Error(e.message ?: "An unexpected error occurred")
                }
                .collect { state ->
                    _uiState.value = state
                }
        }
    }

    /**
     * API sync side-effect — completely decoupled from the data observation flow.
     * Uses collectLatest so rapid date switches cancel the in-progress sync.
     */
    private fun startSyncObserver() {
        viewModelScope.launch {
            _selectedDate.collectLatest { date ->
                // Add a small delay to prevent rapid API calls while swiping
                kotlinx.coroutines.delay(500)
                safeSync(date)
            }
        }
    }

    /**
     * Loads upcoming fixtures for favorite teams.
     * Only re-runs when the favorites list actually changes.
     */
    private fun startFavoriteUpcomingObserver() {
        viewModelScope.launch {
            repository.getFavoriteTeams()
                .distinctUntilChanged()
                .collectLatest { teams ->
                    _favoriteUpcoming.value = if (teams.isEmpty()) {
                        emptyList()
                    } else {
                        try {
                            repository.getFavoriteTeamsNextFixtures()
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            Log.e(TAG, "Favorite upcoming fetch error: ${e.message}")
                            emptyList()
                        }
                    }
                }
        }
    }

    /**
     * Called when the app comes to the foreground.
     * Syncs yesterday (for final scores) and today (for live/upcoming).
     */
    private fun onAppForegrounded() {
        startPolling()
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            if (now - lastForegroundSyncTime < 5 * 60 * 1000L) {
                Log.d(TAG, "Foreground sync skipped (recently synced)")
                return@launch
            }

            val today = dateFormat.format(Calendar.getInstance().time)
            val yesterday = dateFormat.format(
                Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }.time
            )
            Log.d(TAG, "APP FOREGROUNDED → checking sync: yesterday=$yesterday, today=$today")
            launch { safeSync(yesterday) }
            launch { safeSync(today) }
            lastForegroundSyncTime = now
        }
    }

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(120_000) // Poll every 2 minutes instead of 1
                val today = dateFormat.format(Calendar.getInstance().time)
                
                // Only sync if Today is selected AND there are actually live matches
                if (_selectedDate.value == today) {
                    val hasLive = db.dao.hasLiveMatches(today)
                    if (hasLive) {
                        Log.d(TAG, "POLLING → Auto-syncing live scores for Today")
                        safeSync(today)
                    } else {
                        Log.d(TAG, "POLLING → Skipped (no live matches today)")
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
        getApplication<Application>().unregisterActivityLifecycleCallbacks(lifecycleCallbacks)
    }

    private suspend fun safeSync(date: String) {
        try {
            repository.syncIfNeeded(date)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed for $date: ${e.message}")
        }
    }

    // ── Public API ────────────────────────────────────────────────────────

    fun selectDate(date: String) {
        if (_selectedDate.value != date) {
            _uiState.value = MatchUiState.Loading
            _selectedDate.value = date
        }
    }

    fun toggleFavoriteTeam(teamId: Int, name: String, logoUrl: String?) {
        viewModelScope.launch {
            try {
                repository.toggleFavoriteTeam(teamId, name, logoUrl)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "toggleFavoriteTeam error: ${e.message}")
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private fun buildDateList(): List<Triple<String, String, String>> {
        val cal = Calendar.getInstance()
        return (-7..7).map { offset ->
            val dayCal = Calendar.getInstance().apply {
                time = cal.time
                add(Calendar.DAY_OF_YEAR, offset)
            }
            Triple(
                when (offset) {
                    0  -> "Today"
                    -1 -> "Yesterday"
                    1  -> "Tomorrow"
                    else -> dayNameFormat.format(dayCal.time)
                },
                dayNumFormat.format(dayCal.time),
                dateFormat.format(dayCal.time)
            )
        }
    }
}
