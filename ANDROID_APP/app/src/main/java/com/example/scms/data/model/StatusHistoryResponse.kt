package com.example.scms.data.model
import com.example.scms.*
import com.example.scms.ui.screens.* 
import com.example.scms.ui.components.* 
import com.example.scms.data.model.* 
import com.example.scms.data.network.* 
import com.example.scms.utils.* 
import com.example.scms.service.*

import com.google.gson.annotations.SerializedName

data class StatusHistoryResponse(
    @SerializedName("status")     val status: String,
    @SerializedName("timestamp")  val timestamp: String?,
    @SerializedName("note")       val note: String?
)
