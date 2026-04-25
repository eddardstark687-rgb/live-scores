package com.pitchpulse.ui.state

import com.pitchpulse.data.local.entity.FavoriteTeamEntity
import com.pitchpulse.data.model.Match

sealed class MatchUiState {
    object Loading : MatchUiState()
    
    data class Success(
        val favorites: List<Match>,
        val dailyMatches: List<Match>,
        val selectedDate: String,
        val availableDates: List<Triple<String, String, String>> = emptyList(),
        val favoriteTeams: List<FavoriteTeamEntity> = emptyList(),
        val favoriteUpcomingMatches: List<Match> = emptyList(),
        val lastUpdated: Long = 0L
    ) : MatchUiState()
    
    data class Error(val message: String) : MatchUiState()
}

sealed class MatchDetailUiState {
    object Loading : MatchDetailUiState()
    data class Success(
        val match: Match,
        val lineups: List<com.pitchpulse.data.model.Lineup> = emptyList(),
        val stats: com.pitchpulse.data.model.MatchStatistics? = null
    ) : MatchDetailUiState()
    data class Error(val message: String) : MatchDetailUiState()
}
