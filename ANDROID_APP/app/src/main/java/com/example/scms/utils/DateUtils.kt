package com.example.scms.utils
import com.example.scms.*
import com.example.scms.ui.screens.* 
import com.example.scms.ui.components.* 
import com.example.scms.data.model.* 
import com.example.scms.data.network.* 
import com.example.scms.utils.* 
import com.example.scms.service.*

import java.text.SimpleDateFormat
import java.util.*

fun formatTo12Hour(time: String?): String {
    if (time.isNullOrBlank()) return "N/A"

    val formats = listOf(
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss"
    )

    for (pattern in formats) {
        try {
            val inputFormat = SimpleDateFormat(pattern, Locale.getDefault())
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")

            val outputFormat =
                SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

            val date = inputFormat.parse(time)
            if (date != null) {
                return outputFormat.format(date)
            }
        } catch (_: Exception) {
        }
    }

    return time
}
