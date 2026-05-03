package com.example.scms.data.model
import com.example.scms.*
import com.example.scms.ui.screens.* 
import com.example.scms.ui.components.* 
import com.example.scms.data.model.* 
import com.example.scms.data.network.* 
import com.example.scms.utils.* 
import com.example.scms.service.*

data class LeaderboardEntry(
    val id: Int,
    val firebase_uid: String? = null,
    val name: String,
    val points: Int,
    val total_points: Int = 0,
    val total_complaints: Int,
    val resolved_complaints: Int,
    val total_upvotes: Int,
    val profile_picture: String? = null
)
