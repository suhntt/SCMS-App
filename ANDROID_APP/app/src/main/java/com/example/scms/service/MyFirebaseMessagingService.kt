package com.example.scms.service
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
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlin.random.Random
import kotlinx.coroutines.launch


// Handles incoming Push Notifications from backend ("Your complaint was resolved!")
class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title = message.notification?.title ?: message.data["title"] ?: "SCMS Tactical Alert"
        val body = message.notification?.body ?: message.data["body"] ?: "New civic incident reported in your vicinity."
        val complaintId = message.data["complaintId"]
        val screen = message.data["screen"]

        showNotification(title, body, complaintId, screen)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // ✅ Save the new FCM token to backend so admin updates reach this device
        val userId = UserSession.currentUser?.id
        if (userId != null) {
            kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    RetrofitClient.api.updateToken(
                        mapOf("user_id" to userId.toString(), "fcm_token" to token)
                    )
                } catch (_: Exception) {}
            }
        }
        // Also cache token locally for use after login
        getSharedPreferences("scms_prefs", MODE_PRIVATE)
            .edit().putString("pending_fcm_token", token).apply()
    }

    private fun showNotification(title: String, body: String, complaintId: String?, screen: String?) {
        val channelId = "scms_status_updates"
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // High-priority channel for status updates
            NotificationChannel(channelId, "Complaint Status Updates", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Notifies you when your complaint status changes"
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                enableLights(true)
                lightColor = android.graphics.Color.BLUE
                enableVibration(true)
                setShowBadge(true)
                manager.createNotificationChannel(this)
            }
            // Also keep a general alerts channel
            NotificationChannel("scms_tactical_alerts", "Civic Tactical Alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                enableLights(true); enableVibration(true)
                manager.createNotificationChannel(this)
            }
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            if (complaintId != null) {
                putExtra("complaintId", complaintId)
                putExtra("screen", screen ?: "complaints")
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this, Random.nextInt(), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_MAX)           // ⚡ HIGHEST PRIORITY
            .setDefaults(NotificationCompat.DEFAULT_ALL)            // 🔊 SOUND + VIBRATE
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)    // 🔓 SHOW ON LOCK SCREEN
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .build()

        manager.notify(Random.nextInt(), notification)
    }
}
