package com.pitchpulse.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.pitchpulse.MainActivity
import com.pitchpulse.R

class LiveScoresMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d(TAG, "From: ${remoteMessage.from}")

        // Check if message contains a data payload.
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
            handleDataMessage(remoteMessage.data)
        }

        // Check if message contains a notification payload.
        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
            showNotification(it.title ?: "Live Scores Update", it.body ?: "")
        }
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")
        // Normally we'd send this to the backend server.
    }

    private fun handleDataMessage(data: Map<String, String>) {
        val type = data["type"] // goal, match_start
        val homeTeam = data["homeTeam"] ?: "Home"
        val awayTeam = data["awayTeam"] ?: "Away"
        val homeScore = data["homeScore"] ?: "0"
        val awayScore = data["awayScore"] ?: "0"

        when (type) {
            "goal" -> {
                val title = "GOAL! ⚽"
                val body = "$homeTeam $homeScore - $awayScore $awayTeam"
                showNotification(title, body)
            }
            "match_start" -> {
                val title = "Match Started!"
                val body = "$homeTeam vs $awayTeam is now live!"
                showNotification(title, body)
            }
            else -> {
                // Default notification if type is unknown
                val title = data["title"] ?: "Live Scores Update"
                val body = data["body"] ?: ""
                if (body.isNotEmpty()) {
                    showNotification(title, body)
                }
            }
        }
    }

    private fun showNotification(title: String, body: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "match_updates"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Match Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Real-time updates for goals and match starts"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher) // Use default launcher icon for now
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    companion object {
        private const val TAG = "LiveScoresFCM"
    }
}
