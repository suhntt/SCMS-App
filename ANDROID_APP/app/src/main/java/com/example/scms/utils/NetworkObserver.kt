package com.example.scms.utils
import com.example.scms.*
import com.example.scms.ui.screens.* 
import com.example.scms.ui.components.* 
import com.example.scms.data.model.* 
import com.example.scms.data.network.* 
import com.example.scms.utils.* 
import com.example.scms.service.*

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

class NetworkObserver(context: Context) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val observe: Flow<Status> = callbackFlow {
        
        var lastStatus: Status? = null

        val pingJob = launch {
            while (true) {
                val hasInternet = checkActualInternet()
                val currentStatus = if (hasInternet) Status.Available else Status.Lost
                
                // Only send if the status has actually changed
                if (currentStatus != lastStatus) {
                    lastStatus = currentStatus
                    send(currentStatus)
                }
                
                delay(5000)
            }
        }

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                // Let the pingJob handle the Available status for better accuracy
            }

            override fun onLost(network: Network) {
                if (lastStatus != Status.Lost) {
                    lastStatus = Status.Lost
                    launch { send(Status.Lost) }
                }
            }
        }

        connectivityManager.registerDefaultNetworkCallback(callback)

        awaitClose {
            pingJob.cancel()
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.distinctUntilChanged()

    private suspend fun checkActualInternet(): Boolean = withContext(Dispatchers.IO) {
        try {
            val socket = Socket()
            // Quick 1.5s timeout to Google DNS
            socket.connect(InetSocketAddress("8.8.8.8", 53), 1500)
            socket.close()
            true
        } catch (e: Exception) {
            false
        }
    }

    enum class Status {
        Available, Lost
    }
}
