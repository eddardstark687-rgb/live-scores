package com.pitchpulse.data.local

import androidx.room.TypeConverter
import com.pitchpulse.data.model.Lineup
import com.pitchpulse.data.model.MatchEvent
import com.pitchpulse.data.model.StatItem
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromLineupList(value: List<Lineup>): String = json.encodeToString(value)

    @TypeConverter
    fun toLineupList(value: String): List<Lineup> = json.decodeFromString(value)

    @TypeConverter
    fun fromMatchStatistics(value: com.pitchpulse.data.model.MatchStatistics): String = json.encodeToString(value)

    @TypeConverter
    fun toMatchStatistics(value: String): com.pitchpulse.data.model.MatchStatistics = json.decodeFromString(value)

    @TypeConverter
    fun fromStatItemList(value: List<StatItem>): String = json.encodeToString(value)

    @TypeConverter
    fun toStatItemList(value: String): List<StatItem> = json.decodeFromString(value)

    @TypeConverter
    fun fromMatchEventList(value: List<MatchEvent>): String = json.encodeToString(value)

    @TypeConverter
    fun toMatchEventList(value: String): List<MatchEvent> = json.decodeFromString(value)
}
