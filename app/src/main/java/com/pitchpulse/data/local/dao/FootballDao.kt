package com.pitchpulse.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pitchpulse.data.local.entity.MatchEntity
import com.pitchpulse.data.local.entity.FetchMetadataEntity
import com.pitchpulse.data.local.entity.TeamDetailsEntity
import com.pitchpulse.data.local.entity.LineupEntity
import com.pitchpulse.data.local.entity.StatisticEntity
import com.pitchpulse.data.local.entity.ApiUsageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FootballDao {
    @Query("SELECT * FROM matches WHERE isFavoriteLeague = 1 ORDER BY dateString DESC, id ASC")
    fun getFavoriteMatchesFlow(): Flow<List<MatchEntity>>

    @Query("SELECT * FROM matches WHERE dateString = :date ORDER BY isFavoriteLeague DESC, id ASC")
    fun getDailyMatchesFlow(date: String): Flow<List<MatchEntity>>

    @Query("SELECT * FROM matches WHERE dateString = :date")
    suspend fun getDailyMatches(date: String): List<MatchEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatches(matches: List<MatchEntity>)

    @Query("DELETE FROM matches WHERE isFavoriteLeague = 1")
    suspend fun clearFavoriteMatches()

    @Query("DELETE FROM matches WHERE dateString = :date AND isFavoriteLeague = 0")
    suspend fun clearDailyMatches(date: String)

    @Query("DELETE FROM matches")
    suspend fun nukeMatches()

    @Query("SELECT * FROM matches WHERE id = :id")
    fun getMatchByIdFlow(id: Int): Flow<MatchEntity?>

    @Query("SELECT * FROM matches WHERE id = :id")
    suspend fun getMatchById(id: Int): MatchEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMetadata(metadata: FetchMetadataEntity)

    @Query("SELECT * FROM fetch_metadata WHERE date = :date")
    suspend fun getMetadata(date: String): FetchMetadataEntity?

    @Query("SELECT COUNT(*) FROM matches WHERE dateString = :date AND isFavoriteLeague = 0")
    suspend fun countMatchesForDate(date: String): Int

    @Query("DELETE FROM fetch_metadata")
    suspend fun clearMetadata()

    // Team Details Caching
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeamDetails(team: TeamDetailsEntity)

    @Query("SELECT * FROM team_details WHERE id = :teamId")
    suspend fun getTeamDetails(teamId: Int): TeamDetailsEntity?

    // Lineups & Statistics Caching
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLineups(lineups: LineupEntity)

    @Query("SELECT * FROM lineups WHERE fixtureId = :fixtureId")
    suspend fun getLineups(fixtureId: Int): LineupEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStatistics(stats: StatisticEntity)

    @Query("SELECT * FROM statistics WHERE fixtureId = :fixtureId")
    suspend fun getStatistics(fixtureId: Int): StatisticEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApiUsage(usage: ApiUsageEntity)

    @Query("SELECT * FROM api_usage WHERE day = :day")
    suspend fun getApiUsage(day: String): ApiUsageEntity?

    @Query("UPDATE api_usage SET callCount = callCount + 1 WHERE day = :day")
    suspend fun incrementApiUsage(day: String)

    @Query("""
        SELECT * FROM matches 
        WHERE (homeTeamId = :teamId OR awayTeamId = :teamId) 
        AND dateString >= :today 
        ORDER BY dateString ASC, time ASC 
        LIMIT 1
    """)
    suspend fun getNextMatchForTeam(teamId: Int, today: String): MatchEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM matches WHERE dateString = :date AND isLive = 1)")
    suspend fun hasLiveMatches(date: String): Boolean

    @Query("SELECT * FROM matches WHERE homeTeamId = :teamId OR awayTeamId = :teamId ORDER BY dateString DESC")
    suspend fun getTeamFixtures(teamId: Int): List<MatchEntity>
}
