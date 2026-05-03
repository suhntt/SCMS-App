package com.example.scms.data.model
import com.example.scms.*
import com.example.scms.ui.screens.* 
import com.example.scms.ui.components.* 
import com.example.scms.data.model.* 
import com.example.scms.data.network.* 
import com.example.scms.utils.* 
import com.example.scms.service.*

data class User(
    val id: Int,
    val firebaseUid: String? = null,
    val name: String,
    val phone: String,
    val email: String? = null,
    val profile_picture: String? = null,
    val points: Int = 0,
    val badgeLevel: String = "Citizen",
    val district: String? = null          // ✅ Assam district (e.g. "Kamrup Metropolitan")
)
