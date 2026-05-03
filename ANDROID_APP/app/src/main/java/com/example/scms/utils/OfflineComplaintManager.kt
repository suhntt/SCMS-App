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
import com.example.scms.database.AppDatabase
import com.example.scms.database.OfflineComplaintEntity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

data class OfflineComplaint(
    val title: String,
    val photoUris: List<String> = emptyList(),
    val category: String,
    val address: String,
    val latitude: String,
    val longitude: String,
    val description: String,
    val userId: String,
    val timestamp: Long = System.currentTimeMillis()
)

object OfflineComplaintManager {

    private val syncMutex = Mutex()

    suspend fun saveOffline(
        context: Context,
        title: String,
        category: String,
        description: String,
        address: String,
        latitude: String,
        longitude: String,
        imageFiles: List<File>
    ) {
        val database = AppDatabase.getDatabase(context)
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"
        
        val entity = OfflineComplaintEntity(
            title = title,
            category = category,
            description = description,
            address = address,
            latitude = latitude,
            longitude = longitude,
            userId = userId,
            photoUris = imageFiles.map { file: File -> file.absolutePath }
        )
        
        withContext(Dispatchers.IO) {
            database.offlineComplaintDao().insert(entity)
        }
        Log.d("OfflineSync", "Saved complaint locally: $title")
    }

    suspend fun syncOfflineComplaints(context: Context): Int {
        if (syncMutex.isLocked) return 0
        
        return withContext(Dispatchers.IO) {
            syncMutex.withLock {
                performSyncAction(context)
            }
        }
    }

    private suspend fun performSyncAction(context: Context): Int {
        val database = AppDatabase.getDatabase(context)
        val dao = database.offlineComplaintDao()
        
        val pending = dao.getPending()
        if (pending.isEmpty()) return 0

        var successCount = 0
        Log.d("OfflineSync", "Processing ${pending.size} reports...")

        pending.forEach { complaint: OfflineComplaintEntity ->
            try {
                dao.updateStatus(complaint.id, "syncing")

                val photoParts = complaint.photoUris.map { path: String ->
                    val file = File(path)
                    if (file.exists()) {
                        val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                        MultipartBody.Part.createFormData("photos", file.name, requestFile)
                    } else null
                }.filterNotNull()

                // Use district from the saved entity, or fall back to current session district
                val district = (UserSession.currentUser?.district ?: "").toRequestBody("text/plain".toMediaTypeOrNull())

                val res = RetrofitClient.api.submitComplaint(
                    photos = photoParts,
                    title = complaint.title.toRequestBody("text/plain".toMediaTypeOrNull()),
                    category = complaint.category.toRequestBody("text/plain".toMediaTypeOrNull()),
                    address = complaint.address.toRequestBody("text/plain".toMediaTypeOrNull()),
                    latitude = complaint.latitude.toRequestBody("text/plain".toMediaTypeOrNull()),
                    longitude = complaint.longitude.toRequestBody("text/plain".toMediaTypeOrNull()),
                    description = complaint.description.toRequestBody("text/plain".toMediaTypeOrNull()),
                    userId = complaint.userId.toRequestBody("text/plain".toMediaTypeOrNull()),
                    district = district                     // ✅ District for offline-synced complaints
                )

                if (res.isSuccessful) {
                    Log.d("OfflineSync", "Synced: ${complaint.title}")
                    dao.delete(complaint)
                    successCount++
                } else {
                    dao.updateStatus(complaint.id, "pending_sync")
                }
            } catch (e: Exception) {
                Log.e("OfflineSync", "Sync Error: ${e.message}")
                dao.updateStatus(complaint.id, "pending_sync")
            }
        }
        return successCount
    }
}
