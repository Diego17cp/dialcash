package com.dialcadev.dialcash.core.utils.extensions

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Long.toReadableDate(): String {
    return try {
        val outputFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        outputFormat.format(Date(this))
    } catch (e: Exception) {
        this.toString()
    }
}