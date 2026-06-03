package com.pitchpulse.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pitchpulse.core.network.RetrofitClient
import com.pitchpulse.data.local.LiveScoresDatabase
import com.pitchpulse.data.repository.FootballRepository
import com.pitchpulse.data.remote.dto.TeamDto
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.OptIn

sealed class SearchUiState {
    data class Initial(val suggestions: List<TeamDto>, val favoriteIds: Set<Int>) : SearchUiState()
    object Loading : SearchUiState()
    data class Success(val teams: List<TeamDto>, val favoriteIds: Set<Int>) : SearchUiState()
    data class Error(val message: String) : SearchUiState()
}

class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val db = LiveScoresDatabase.getInstance(application)
    private val repository = FootballRepository(
        api = RetrofitClient.api,
        dao = db.dao,
        favoriteTeamDao = db.favoriteTeamDao
    )

    private val _searchText = MutableStateFlow("")
    val searchText: StateFlow<String> = _searchText.asStateFlow()

    private val _favoriteIds = MutableStateFlow<Set<Int>>(emptySet())
    private val _suggestions = MutableStateFlow<List<TeamDto>>(emptyList())

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<SearchUiState> = _searchText
        .debounce(300)
        .flatMapLatest { query ->
            if (query.isBlank()) {
                _suggestions.map { suggestions ->
                    SearchUiState.Initial(suggestions, _favoriteIds.value)
                }
            } else {
                flow<SearchUiState> {
                    emit(SearchUiState.Loading)
                    try {
                        val results = repository.searchTeams(query)
                        emit(SearchUiState.Success(results, _favoriteIds.value))
                    } catch (e: Exception) {
                        emit(SearchUiState.Error(e.message ?: "Failed to find teams"))
                    }
                }
            }
        }
        .combine(_favoriteIds) { state, favorites ->
            when (state) {
                is SearchUiState.Success -> state.copy(favoriteIds = favorites)
                is SearchUiState.Initial -> state.copy(favoriteIds = favorites)
                else -> state
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SearchUiState.Initial(emptyList(), emptySet())
        )

    init {
        observeFavorites()
        loadSuggestions()
    }

    private fun observeFavorites() {
        viewModelScope.launch {
            repository.getFavoriteTeams().collect { favorites ->
                _favoriteIds.value = favorites.map { it.teamId }.toSet()
            }
        }
    }

    private fun loadSuggestions() {
        viewModelScope.launch {
            _suggestions.value = repository.getSuggestedTeams()
        }
    }

    fun onSearchTextChange(text: String) {
        _searchText.value = text
    }

    fun toggleFavorite(team: TeamDto) {
        viewModelScope.launch {
            repository.toggleFavoriteTeam(
                teamId = team.id,
                name = team.name,
                logoUrl = team.logo
            )
        }
    }
}
