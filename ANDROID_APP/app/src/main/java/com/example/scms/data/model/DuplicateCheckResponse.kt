package com.example.scms.data.model
import com.example.scms.*
import com.example.scms.ui.screens.* 
import com.example.scms.ui.components.* 
import com.example.scms.data.model.* 
import com.example.scms.data.network.* 
import com.example.scms.utils.* 
import com.example.scms.service.*

import com.google.gson.annotations.SerializedName

data class DuplicateComplaint(
    @SerializedName("id") val id: Int,
    @SerializedName("description") val description: String?,
    @SerializedName("distance") val distance: Double,
    @SerializedName("similarity") val similarity: Double = 0.0
)

data class DuplicateCheckResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("isDuplicate") val isDuplicate: Boolean,
    @SerializedName("duplicate") val duplicate: DuplicateComplaint? = null
)
