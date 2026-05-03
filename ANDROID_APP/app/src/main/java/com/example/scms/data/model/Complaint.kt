package com.example.scms.data.model
import com.example.scms.*
import com.example.scms.ui.screens.* 
import com.example.scms.ui.components.* 
import com.example.scms.data.model.* 
import com.example.scms.data.network.* 
import com.example.scms.utils.* 
import com.example.scms.service.*

data class Complaint(
    val id: String,
    val title: String? = null,
    val category: String? = null,
    val address: String? = null,
    val district: String? = null,          // ✅ Assam district
    val latitude: String? = null,
    val longitude: String? = null,
    val description: String? = null,
    val photo_url: String? = null,
    val photo_urls: List<String>? = null,
    val status: String? = null,
    val severity: String? = null,
    val upvotes: Int = 0,
    val created_at: String? = null,
    val department: String? = null,
    val municipality: String? = null,
    val user_id: Int? = null,
    val reporter_name: String? = null
)
