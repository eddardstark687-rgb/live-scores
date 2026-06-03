package com.pitchpulse.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pitchpulse.data.local.LiveScoresDatabase
import com.pitchpulse.data.model.FootballQuote
import com.pitchpulse.data.model.LeagueTodaySummary
import com.pitchpulse.data.model.QuizQuestion
import com.pitchpulse.data.repository.HomeContentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeExtrasUiState(
    val leagueSummaries: List<LeagueTodaySummary> = emptyList(),
    val currentQuiz: QuizQuestion? = null,
    val quizIndex: Int = 0,
    val quizPool: List<QuizQuestion> = emptyList(),
    val selectedOptionIndex: Int? = null,
    val quote: FootballQuote? = null,
    val fact: String? = null,
    val isLoadingContent: Boolean = true,
    val contentError: String? = null
)

class HomeExtrasViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = HomeContentRepository(
        dao = LiveScoresDatabase.getInstance(application).dao
    )

    private val _uiState = MutableStateFlow(HomeExtrasUiState())
    val uiState: StateFlow<HomeExtrasUiState> = _uiState.asStateFlow()

    val leagueSummaries: StateFlow<List<LeagueTodaySummary>> =
        repository.observeLeagueSummariesToday()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                listOf(
                    LeagueTodaySummary("Premier League", 0),
                    LeagueTodaySummary("La Liga", 0),
                    LeagueTodaySummary("Bundesliga", 0)
                )
            )

    init {
        loadAiContent()
        viewModelScope.launch {
            leagueSummaries.collect { leagues ->
                _uiState.value = _uiState.value.copy(leagueSummaries = leagues)
            }
        }
    }

    fun loadAiContent() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingContent = true, contentError = null)
            try {
                val bundle = repository.fetchAiHomeContent()
                val pool = bundle.quizzes.ifEmpty { listOfNotNull(_uiState.value.currentQuiz) }
                _uiState.value = _uiState.value.copy(
                    quizPool = pool,
                    quizIndex = 0,
                    currentQuiz = pool.firstOrNull(),
                    selectedOptionIndex = null,
                    quote = bundle.quote,
                    fact = bundle.fact,
                    isLoadingContent = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingContent = false,
                    contentError = e.message ?: "Could not load home content"
                )
            }
        }
    }

    fun onQuizOptionSelected(index: Int) {
        val state = _uiState.value
        if (state.selectedOptionIndex != null) return
        _uiState.value = state.copy(selectedOptionIndex = index)
    }

    fun nextQuizQuestion() {
        val state = _uiState.value
        val pool = state.quizPool
        if (pool.isEmpty()) return
        val nextIndex = (state.quizIndex + 1) % pool.size
        _uiState.value = state.copy(
            quizIndex = nextIndex,
            currentQuiz = pool[nextIndex],
            selectedOptionIndex = null
        )
    }
}
