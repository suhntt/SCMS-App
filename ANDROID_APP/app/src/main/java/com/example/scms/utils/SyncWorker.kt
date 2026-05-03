package com.example.scms.utils
import com.example.scms.*
import com.example.scms.ui.screens.* 
import com.example.scms.ui.components.* 
import com.example.scms.data.model.* 
import com.example.scms.data.network.* 
import com.example.scms.utils.* 
import com.example.scms.service.*

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("SyncWorker", "Executing background sync for offline complaints...")
        return try {
            val syncedCount = OfflineComplaintManager.syncOfflineComplaints(applicationContext)
            if (syncedCount > 0) {
                NotificationUtils.showSyncSuccessNotification(applicationContext, syncedCount)
            }
            Result.success()
        } catch (e: Exception) {
            Log.e("SyncWorker", "Error during background sync", e)
            Result.retry()
        }
    }
}
