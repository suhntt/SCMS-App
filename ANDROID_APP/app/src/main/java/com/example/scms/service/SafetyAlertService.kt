package com.example.scms.service

import android.app.*
import android.content.Intent
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.scms.MainActivity
import com.example.scms.R
import com.example.scms.data.network.RetrofitClient
import com.example.scms.utils.GeofenceManager
import com.google.android.gms.location.*
import kotlinx.coroutines.*

/**
 * Foreground Service that provides real-time "Safety Proximity Alerts".
 */
class SafetyAlertService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var hazardList = listOf<com.example.scms.data.model.Complaint>()

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        
        startForeground(1001, createNotification("SCMS Safety Shield Active", "Scanning for nearby hazards..."))
        
        // Periodic fetch of hazard data (every 10 minutes)
        serviceScope.launch {
            while (isActive) {
                try {
                    hazardList = RetrofitClient.api.getComplaints()
                    Log.d("SafetyService", "Fetched ${hazardList.size} complaints for geofencing")
                } catch (e: Exception) {
                    Log.e("SafetyService", "Fetch failed: ${e.message}")
                }
                delay(10 * 60 * 1000)
            }
        }

        setupLocationUpdates()
    }

    private fun setupLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
            .setMinUpdateIntervalMillis(5000)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation ?: return
                GeofenceManager.checkProximity(applicationContext, location.latitude, location.longitude, hazardList)
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        } catch (e: SecurityException) {
            Log.e("SafetyService", "Location permission missing")
        }
    }

    private fun createNotification(title: String, content: String): Notification {
        val channelId = "scms_safety_service_channel"
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            // 🔇 Use IMPORTANCE_LOW to keep it silent and subtle
            val channel = NotificationChannel(channelId, "Safety Shield Status", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("🛡️ SCMS Safety Shield Active")
            .setContentText("Your city safety is being protected.")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_MIN) // 📉 Lowest priority
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        serviceScope.cancel()
    }
}
