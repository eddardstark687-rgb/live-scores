package com.pitchpulse.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.pitchpulse.data.local.dao.FavoriteTeamDao
import com.pitchpulse.data.local.dao.FootballDao
import com.pitchpulse.data.local.entity.FavoriteTeamEntity
import com.pitchpulse.data.local.entity.MatchEntity
import com.pitchpulse.data.local.entity.FetchMetadataEntity
import com.pitchpulse.data.local.entity.TeamDetailsEntity
import com.pitchpulse.data.local.entity.LineupEntity
import com.pitchpulse.data.local.entity.StatisticEntity
import com.pitchpulse.data.local.entity.ApiUsageEntity
import androidx.room.TypeConverters

@Database(
    entities = [
        MatchEntity::class, 
        FavoriteTeamEntity::class, 
        FetchMetadataEntity::class, 
        TeamDetailsEntity::class,
        LineupEntity::class,
        StatisticEntity::class,
        ApiUsageEntity::class
    ],
    version = 8,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class PitchPulseDatabase : RoomDatabase() {

    abstract val dao: FootballDao
    abstract val favoriteTeamDao: FavoriteTeamDao

    companion object {
        @Volatile
        private var INSTANCE: PitchPulseDatabase? = null

        fun getInstance(context: Context): PitchPulseDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    PitchPulseDatabase::class.java,
                    "pitchpulse_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                .also { INSTANCE = it }
            }
        }
    }
}
