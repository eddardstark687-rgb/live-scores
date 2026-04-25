package com.pitchpulse.data.local.dao

import androidx.room.*
import com.pitchpulse.data.local.entity.FavoriteTeamEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteTeamDao {
    @Query("SELECT * FROM favorite_teams")
    fun getFavoriteTeamsFlow(): Flow<List<FavoriteTeamEntity>>

    @Query("SELECT * FROM favorite_teams")
    suspend fun getFavoriteTeams(): List<FavoriteTeamEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavoriteTeam(team: FavoriteTeamEntity)

    @Delete
    suspend fun deleteFavoriteTeam(team: FavoriteTeamEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_teams WHERE teamId = :teamId)")
    suspend fun isTeamFavorite(teamId: Int): Boolean
}
