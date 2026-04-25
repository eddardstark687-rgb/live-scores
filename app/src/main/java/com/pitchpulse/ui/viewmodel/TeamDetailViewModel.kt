package com.pitchpulse.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pitchpulse.core.network.RetrofitClient
import com.pitchpulse.data.model.Match
import com.pitchpulse.data.model.TeamDetails
import com.pitchpulse.data.repository.FootballRepository
import com.pitchpulse.data.local.LiveScoresDatabase
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class TeamDetailUiState {
    object Loading : TeamDetailUiState()
    data class Success(
        val team: TeamDetails,
        val pastFixtures: List<Match>,
        val upcomingFixtures: List<Match>
    ) : TeamDetailUiState()
    data class Error(val message: String) : TeamDetailUiState()
}

class TeamDetailViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val teamId: Int = savedStateHandle.get<Int>("teamId") ?: 0
    
    private val db = LiveScoresDatabase.getInstance(application)
    private val repository = FootballRepository(
        api = RetrofitClient.api,
        dao = db.dao,
        favoriteTeamDao = db.favoriteTeamDao
    )

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    private val _uiState = MutableStateFlow<TeamDetailUiState>(TeamDetailUiState.Loading)
    val uiState: StateFlow<TeamDetailUiState> = _uiState.asStateFlow()

    init {
        loadTeamDetails()
        checkIfFavorite()
    }

    private fun checkIfFavorite() {
        viewModelScope.launch {
            _isFavorite.value = db.favoriteTeamDao.isTeamFavorite(teamId)
        }
    }

    private fun loadTeamDetails(isManualRefresh: Boolean = false) {
        if (teamId == 0) {
            _uiState.value = TeamDetailUiState.Error("Invalid Team ID")
            return
        }

        viewModelScope.launch {
            android.util.Log.d("TEAM_DEBUG", "Loading details for teamId: $teamId")
            if (isManualRefresh) {
                _isRefreshing.value = true
            } else if (_uiState.value !is TeamDetailUiState.Success) {
                _uiState.value = TeamDetailUiState.Loading
            }
            
            try {
                // Fetch team info first (which includes cache logic)
                val teamInfo = repository.getTeamInfo(teamId)
                
                if (teamInfo != null) {
                    // Fetch fixtures. Even if these fail, we want to show the team header.
                    val past = try { repository.getTeamFixtures(teamId, last = 10) } catch (e: Exception) { emptyList() }
                    val next = try { repository.getTeamFixtures(teamId, next = 10) } catch (e: Exception) { emptyList() }
                    
                    _uiState.value = TeamDetailUiState.Success(
                        team = teamInfo,
                        pastFixtures = past,
                        upcomingFixtures = next
                    )
                } else {
                    // Try to get minimal info if it's a favorite as a last resort
                    val favorite = db.favoriteTeamDao.getFavoriteTeams().find { it.teamId == teamId }
                    if (favorite != null) {
                        _uiState.value = TeamDetailUiState.Success(
                            team = com.pitchpulse.data.model.TeamDetails(
                                id = favorite.teamId,
                                name = favorite.name,
                                logo = favorite.logoUrl ?: "",
                                country = null, founded = null, isNational = false,
                                venueName = null, venueCity = null, venueCapacity = null, venueImage = null
                            ),
                            pastFixtures = emptyList(),
                            upcomingFixtures = emptyList()
                        )
                    } else if (_uiState.value !is TeamDetailUiState.Success) {
                        _uiState.value = TeamDetailUiState.Error("Team data unavailable. Please check your connection.")
                    }
                }
            } catch (e: Exception) {
                if (_uiState.value !is TeamDetailUiState.Success) {
                    _uiState.value = TeamDetailUiState.Error(e.message ?: "Unknown error occurred")
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun toggleFavorite() {
        val currentState = _uiState.value
        if (currentState is TeamDetailUiState.Success) {
            viewModelScope.launch {
                repository.toggleFavoriteTeam(
                    teamId = teamId,
                    name = currentState.team.name,
                    logoUrl = currentState.team.logo
                )
                _isFavorite.value = !_isFavorite.value
            }
        }
    }

    fun refresh() {
        loadTeamDetails(isManualRefresh = true)
        checkIfFavorite()
    }
}
