package com.example.scms.utils
import com.example.scms.*
import com.example.scms.ui.screens.* 
import com.example.scms.ui.components.* 
import com.example.scms.data.model.* 
import com.example.scms.data.network.* 
import com.example.scms.utils.* 
import com.example.scms.service.*

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object NotificationUtils {
    private const val CHANNEL_ID = "scms_sync_channel"
    private const val CHANNEL_NAME = "SCMS Mission Status"
    private const val NOTIFICATION_ID = 888

    private val _syncSuccessFlow = MutableStateFlow(0)
    val syncSuccessFlow = _syncSuccessFlow.asStateFlow()

    fun resetSyncFlow() { _syncSuccessFlow.value = 0 }

    fun showSyncSuccessNotification(context: Context, count: Int) {
        _syncSuccessFlow.value = count
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Notifications for background report synchronization"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("SCMS Mission Certified 🏆")
            .setContentText("Status Update: $count civic report(s) successfully synchronized and logged.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText("Status Update: $count civic report(s) successfully synchronized and logged with the City Command Center. Thank you for your contribution!"))
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    fun showProximityAlert(context: Context, category: String, distance: Int, complaintId: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val proximityChannelId = "scms_safety_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(proximityChannelId, "SCMS Safety Alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Proximity alerts for nearby infrastructure hazards"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("screen", "timeline")
            putExtra("complaintId", complaintId)
        }
        val pendingIntent = PendingIntent.getActivity(context, 1, intent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(context, proximityChannelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("⚠️ SCMS Safety Alert")
            .setContentText("Warning: $category reported ${distance}m ahead. Stay safe!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(999, notification)
    }
}
