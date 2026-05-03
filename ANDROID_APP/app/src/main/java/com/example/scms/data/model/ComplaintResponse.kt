package com.example.scms.data.model
import com.example.scms.*
import com.example.scms.ui.screens.* 
import com.example.scms.ui.components.* 
import com.example.scms.data.model.* 
import com.example.scms.data.network.* 
import com.example.scms.utils.* 
import com.example.scms.service.*

data class ComplaintResponse(
    val id: Int,
    val category: String,
    val address: String,
    val latitude: String,
    val longitude: String,
    val description: String,
    val photo_url: String?,
    val date_time: String,
    val status: String
)
