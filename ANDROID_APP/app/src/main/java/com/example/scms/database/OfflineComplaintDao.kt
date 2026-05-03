package com.example.scms.database
import com.example.scms.ui.screens.* 
import com.example.scms.ui.components.* 
import com.example.scms.data.model.* 
import com.example.scms.data.network.* 
import com.example.scms.utils.* 
import com.example.scms.service.*

import androidx.room.*

@Dao
interface OfflineComplaintDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(complaint: OfflineComplaintEntity)

    @Query("SELECT * FROM offline_complaints WHERE status = 'pending_sync' ORDER BY timestamp ASC")
    suspend fun getPending(): List<OfflineComplaintEntity>

    @Query("UPDATE offline_complaints SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Int, status: String)

    @Delete
    suspend fun delete(complaint: OfflineComplaintEntity)

    @Query("DELETE FROM offline_complaints")
    suspend fun deleteAll()
}
