package com.pitchpulse.core.util

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.text.SimpleDateFormat
import java.util.*

object DateObserver {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    /**
     * Emits the current date string immediately, then checks every minute
     * if the date has changed. If it has, emits the new date.
     */
    fun dateFlow(): Flow<String> = flow {
        var lastDate = ""
        while (true) {
            val currentDate = dateFormat.format(Date())
            if (currentDate != lastDate) {
                lastDate = currentDate
                emit(currentDate)
            }
            // Check every minute to handle midnight shifts gracefully
            delay(60_000)
        }
    }
}
