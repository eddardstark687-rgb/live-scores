package com.pitchpulse.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "suggested_teams")
data class SuggestedTeamEntity(
    @PrimaryKey val teamId: Int,
    val name: String,
    val logo: String,
    val dateString: String // "yyyy-MM-dd"
)
