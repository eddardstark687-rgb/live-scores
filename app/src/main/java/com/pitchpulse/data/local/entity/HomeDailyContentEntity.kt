package com.pitchpulse.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "home_daily_content")
data class HomeDailyContentEntity(
    @PrimaryKey val dateString: String,
    val contentJson: String,
    val fetchedAt: Long = System.currentTimeMillis()
)
