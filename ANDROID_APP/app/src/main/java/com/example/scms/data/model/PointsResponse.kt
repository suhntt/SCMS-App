package com.example.scms.data.model
import com.example.scms.*
import com.example.scms.ui.screens.* 
import com.example.scms.ui.components.* 
import com.example.scms.data.model.* 
import com.example.scms.data.network.* 
import com.example.scms.utils.* 
import com.example.scms.service.*

data class PointsResponse(
    val points: Int,
    val email: String?,
    val phone: String?,
    val name: String?,
    val profile_picture: String?,
    val badgeLevel: String?,
    val id: Int?
)
