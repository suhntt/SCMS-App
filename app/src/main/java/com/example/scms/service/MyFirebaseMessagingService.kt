package com.example.scms.service
import com.example.scms.*
import com.example.scms.ui.screens.* 
import com.example.scms.ui.components.* 
import com.example.scms.data.model.* 
import com.example.scms.data.network.* 
import com.example.scms.utils.* 
import com.example.scms.service.*
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
        // Whenever Firebase generates a new token for this phone,
        // we can send it to our MySQL backend so it knows where to push alerts!
        // For now, it will just log.
        println("FCM Token Generated: $token")
    }

    private fun showNotification(title: String, body: String, complaintId: String?, screen: String?) {
        val channelId = "scms_tactical_alerts"
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Civic Tactical Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High-priority alerts for city incidents and emergencies"
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                enableLights(true)
                lightColor = android.graphics.Color.RED
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)
        }

        // Create an Intent to open the app
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            if (complaintId != null) {
                putExtra("complaintId", complaintId)
                putExtra("screen", screen ?: "complaints")
            }
        }
        
        val pendingIntent = android.app.PendingIntent.getActivity(
            this, Random.nextInt(), intent,
            android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // ⚡ HEADS-UP POPUP
            .setDefaults(NotificationCompat.DEFAULT_ALL)   // 🔊 SOUND + VIBRATE
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // 🔓 SHOW ON LOCK SCREEN
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setFullScreenIntent(pendingIntent, false) // 🚨 POP UP EVEN OVER OTHER APPS
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .build()

        manager.notify(Random.nextInt(), notification)
    }
}
