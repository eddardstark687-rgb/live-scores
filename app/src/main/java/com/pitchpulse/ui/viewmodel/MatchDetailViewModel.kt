package com.pitchpulse.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.pitchpulse.core.network.RetrofitClient
import com.pitchpulse.data.local.LiveScoresDatabase
import com.pitchpulse.data.repository.FootballRepository
import com.pitchpulse.ui.state.MatchDetailUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

class MatchDetailViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val db = LiveScoresDatabase.getInstance(application)
    private val repository = FootballRepository(
        api = RetrofitClient.api,
        dao = db.dao,
        favoriteTeamDao = db.favoriteTeamDao
    )

    private val fixtureId: Int = checkNotNull(savedStateHandle["fixtureId"])

    private val _uiState = MutableStateFlow<MatchDetailUiState>(MatchDetailUiState.Loading)
    val uiState: StateFlow<MatchDetailUiState> = _uiState.asStateFlow()

    init {
        loadMatchDetails()
    }

    private fun loadMatchDetails() {
        viewModelScope.launch {
            repository.getMatchDetails(fixtureId)
                .filterNotNull()
                .collect { match ->
                    val currentState = _uiState.value
                    if (currentState is MatchDetailUiState.Success) {
                        _uiState.value = currentState.copy(match = match)
                    } else {
                        _uiState.value = MatchDetailUiState.Success(
                            match = match,
                            lineups = emptyList(),
                            stats = null
                        )
                    }

                    // Fetch intelligence data concurrently if not already loaded or if match is live
                    if (match.isLive || (currentState !is MatchDetailUiState.Success) || currentState.lineups.isEmpty() || match.events.isEmpty()) {
                        launch {
                            // Proactively sync fixture details to get goal scorers if missing
                            if (match.events.isEmpty()) {
                                repository.syncFixtureDetails(fixtureId)
                            }
                            
                            val lineups = repository.getLineups(fixtureId)
                            val stats = repository.getStatistics(fixtureId)
                            
                            val updatedState = _uiState.value
                            if (updatedState is MatchDetailUiState.Success) {
                                _uiState.value = updatedState.copy(lineups = lineups, stats = stats)
                            }
                        }
                    }
                }
        }
        
        // Polling for live updates
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(60_000) // 1 minute
                val state = _uiState.value
                if (state is MatchDetailUiState.Success && state.match.isLive) {
                    // Sync full fixture details (including goal scorers) for live matches
                    repository.syncFixtureDetails(fixtureId)
                }
            }
        }
    }
}
