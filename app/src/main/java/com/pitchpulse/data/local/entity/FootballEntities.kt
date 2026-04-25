package com.pitchpulse.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.pitchpulse.data.model.Match
import com.pitchpulse.data.model.MatchEvent

@Entity(tableName = "matches")
data class MatchEntity(
    @PrimaryKey val id: Int,
    val homeTeam: String,
    val homeTeamId: Int,
    val homeTeamLogo: String?,
    val awayTeam: String,
    val awayTeamId: Int,
    val awayTeamLogo: String?,
    val homeScore: Int?,
    val awayScore: Int?,
    val time: String,
    val isLive: Boolean,
    val competition: String,
    val isFavoriteLeague: Boolean,
    val dateString: String,
    val events: List<MatchEvent> = emptyList()
) {
    fun toDomainModel(): Match {
        return Match(
            id = id,
            homeTeam = homeTeam,
            homeTeamId = homeTeamId,
            homeTeamLogo = homeTeamLogo,
            awayTeam = awayTeam,
            awayTeamId = awayTeamId,
            awayTeamLogo = awayTeamLogo,
            homeScore = homeScore,
            awayScore = awayScore,
            time = time,
            date = dateString,
            isLive = isLive,
            competition = competition,
            isFavoriteLeague = isFavoriteLeague,
            events = events
        )
    }
}

@Entity(tableName = "favorite_teams")
data class FavoriteTeamEntity(
    @PrimaryKey val teamId: Int,
    val name: String,
    val logoUrl: String?
)

@Entity(tableName = "fetch_metadata")
data class FetchMetadataEntity(
    @PrimaryKey val date: String,
    val lastFetchTime: Long,
    val lastFetchAttemptTime: Long,
    val lastLiveFetchTime: Long,
    val hasLiveMatches: Boolean
)

@Entity(tableName = "lineups")
data class LineupEntity(
    @PrimaryKey val fixtureId: Int,
    val lineups: List<com.pitchpulse.data.model.Lineup>,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "statistics")
data class StatisticEntity(
    @PrimaryKey val fixtureId: Int,
    val statistics: com.pitchpulse.data.model.MatchStatistics,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "api_usage")
data class ApiUsageEntity(
    @PrimaryKey val day: String, // yyyy-MM-dd
    val callCount: Int
)
