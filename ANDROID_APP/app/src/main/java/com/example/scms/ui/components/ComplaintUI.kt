package com.example.scms.ui.components
import com.example.scms.*
import com.example.scms.ui.screens.* 
import com.example.scms.ui.components.* 
import com.example.scms.data.model.* 
import com.example.scms.data.network.* 
import com.example.scms.utils.* 
import com.example.scms.service.*

data class ComplaintUI(
    val category: String,
    val address: String,
    val lat: String,
    val lon: String,
    val dateTime: String,
    val description: String,
    val status: String,
    var upvotes: Int = 0
)
