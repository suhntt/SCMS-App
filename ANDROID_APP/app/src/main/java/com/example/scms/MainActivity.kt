package com.example.scms

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.work.*
import com.example.scms.ui.theme.SCMSTheme
import com.example.scms.service.SafetyAlertService
import com.example.scms.utils.*
import com.google.firebase.messaging.FirebaseMessaging
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private val REQUEST_CODE_PERMISSIONS = 1001
    
    private val REQUIRED_PERMISSIONS = mutableListOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(Manifest.permission.BLUETOOTH_SCAN)
            add(Manifest.permission.BLUETOOTH_ADVERTISE)
            add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.NEARBY_WIFI_DEVICES)
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        UserSession.context = applicationContext

        // ✅ REQUEST NEARBY PERMISSIONS (REQUIRED FOR MESH)
        if (!hasPermissions()) {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        // ✅ RESTORE USER SESSION
        val sessionManager = SessionManager(this)
        UserSession.currentUser = sessionManager.getUser()

        // ✅ SUBSCRIBE TO ALERTS TOPIC
        FirebaseMessaging.getInstance().subscribeToTopic("all_users")

        // ✅ AUTO-SYNC OFFLINE COMPLAINTS WHEN INTERNET CONNECTS
        val connectivityManager = getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val networkRequest = android.net.NetworkRequest.Builder()
            .addCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(networkRequest, object : android.net.ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) {
                super.onAvailable(network)
                val oneTimeSync = OneTimeWorkRequestBuilder<SyncWorker>()
                    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                    .build()
                WorkManager.getInstance(this@MainActivity).enqueue(oneTimeSync)
            }
        })

        // 🔋 BATTERY & DATA OPTIMIZATION: Configure sync constraints
        val syncConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val syncWorkRequest = PeriodicWorkRequestBuilder<SyncWorker>(1, TimeUnit.HOURS)
            .setConstraints(syncConstraints)
            .build()
            
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "OfflineSync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncWorkRequest
        )

        // 🛡️ START SCMS SAFETY SHIELD (Foreground Service)
        val safetyIntent = Intent(this, SafetyAlertService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(safetyIntent)
        } else {
            startService(safetyIntent)
        }

        val complaintId = intent.getStringExtra("complaintId")
        val screen = intent.getStringExtra("screen")

        setContent {
            SCMSTheme {
                MainScreen(initialComplaintId = complaintId, initialScreen = screen)   // ✅ ENTRY POINT WITH DEEP LINK
            }
        }
    }

    private fun hasPermissions(): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ActivityCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }
}
