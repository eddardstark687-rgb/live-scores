package com.pitchpulse.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pitchpulse.core.network.RetrofitClient
import com.pitchpulse.data.local.LiveScoresDatabase
import com.pitchpulse.data.repository.FootballRepository
import java.text.SimpleDateFormat
import java.util.*

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val db = LiveScoresDatabase.getInstance(applicationContext)
        val repository = FootballRepository(
            api = RetrofitClient.api,
            dao = db.dao,
            favoriteTeamDao = db.favoriteTeamDao
        )

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val today = dateFormat.format(Date())
        val yesterday = dateFormat.format(Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }.time)

        return try {
            Log.d("SyncWorker", "Starting background sync for $yesterday and $today")
            
            // Sync both days to ensure final scores and upcoming fixtures are up-to-date
            repository.syncIfNeeded(yesterday)
            repository.syncIfNeeded(today)
            
            Log.d("SyncWorker", "Background sync completed successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e("SyncWorker", "Background sync failed: ${e.message}")
            Result.retry()
        }
    }
}
