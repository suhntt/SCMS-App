package com.example.scms.utils

import android.content.Context
import android.util.Log
import com.example.scms.data.model.Complaint
import java.util.concurrent.ConcurrentHashMap

/**
 * Handles real-time proximity alerts for citizens.
 */
object GeofenceManager {
    private const val ALERT_RADIUS_METERS = 50.0
    private val notifiedHazards = ConcurrentHashMap<String, Long>() // ID -> Timestamp

    fun checkProximity(context: Context, userLat: Double, userLon: Double, complaints: List<Complaint>) {
        val currentTime = System.currentTimeMillis()
        
        complaints.filter { it.severity == "High" && it.status != "Resolved" }
            .forEach { hazard ->
                val hazardLat = hazard.latitude?.toDoubleOrNull() ?: return@forEach
                val hazardLon = hazard.longitude?.toDoubleOrNull() ?: return@forEach
                
                val distanceKm = calculateDistanceInKm(userLat, userLon, hazardLat, hazardLon)
                val distanceMeters = distanceKm * 1000

                if (distanceMeters <= ALERT_RADIUS_METERS) {
                    // Prevent spamming: only notify once every 30 minutes for the same hazard
                    val lastNotified = notifiedHazards[hazard.id] ?: 0L
                    if (currentTime - lastNotified > 30 * 60 * 1000) {
                        Log.d("GeofenceManager", "⚠️ Safety Alert: User near ${hazard.category} at ${distanceMeters}m")
                        NotificationUtils.showProximityAlert(context, hazard.category ?: "Hazard", distanceMeters.toInt())
                        notifiedHazards[hazard.id] = currentTime
                    }
                }
            }
    }
}
