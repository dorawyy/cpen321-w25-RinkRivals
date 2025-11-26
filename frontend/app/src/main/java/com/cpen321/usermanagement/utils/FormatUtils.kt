package com.cpen321.usermanagement.utils

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FormatUtils {
    fun formatDateTime(dateTimeString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
            val outputFormat = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
            val date = inputFormat.parse(dateTimeString)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            Log.e("formatDateTime", "Error formatting date/time: ${e.message}")
            dateTimeString // Return original string if parsing fails
        }
    }
}