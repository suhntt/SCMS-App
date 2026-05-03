package com.example.scms.database
import com.example.scms.ui.screens.* 
import com.example.scms.ui.components.* 
import com.example.scms.data.model.* 
import com.example.scms.data.network.* 
import com.example.scms.utils.* 
import com.example.scms.service.*

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "offline_complaints")
data class OfflineComplaintEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val category: String,
    val description: String,
    val address: String,
    val latitude: String,
    val longitude: String,
    val userId: String,
    val photoUris: List<String>,
    val status: String = "pending_sync",
    val timestamp: Long = System.currentTimeMillis()
)
