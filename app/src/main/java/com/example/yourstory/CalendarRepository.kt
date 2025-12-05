package com.example.yourstory

import android.content.Context
import android.provider.CalendarContract
import java.time.LocalDate
import java.time.ZoneId

class CalendarRepository(private val context: Context) {

    fun getTodaysEvents(): List<String> {
        val events = mutableListOf<String>()

        // DEBUG 1: List all Calendars on the device
        // This tells us if your "Personal" calendar is actually visible
        val calCursor = context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            arrayOf(
                CalendarContract.Calendars.NAME,
                CalendarContract.Calendars.ACCOUNT_NAME,
                CalendarContract.Calendars.VISIBLE
            ),
            null, null, null
        )

        println("APP_DEBUG: --- Available Calendars ---")
        calCursor?.use {
            while (it.moveToNext()) {
                val name = it.getString(0)
                val account = it.getString(1)
                val visible = it.getString(2)
                println("APP_DEBUG: Cal: '$name' (Account: $account) | Visible: $visible")
            }
        }

        // DEBUG 2: Widen search significantly (Last 24h to Next 48h)
        // Sometimes emulators act weird with UTC vs Local time
        val now = LocalDate.now()
        val startMillis = System.currentTimeMillis() - (24 * 60 * 60 * 1000) // 24h ago
        val endMillis = System.currentTimeMillis() + (48 * 60 * 60 * 1000)   // 48h from now

        println("APP_DEBUG: Querying Events from ${java.time.Instant.ofEpochMilli(startMillis)} to ${java.time.Instant.ofEpochMilli(endMillis)}")

        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon()
            .appendPath(startMillis.toString())
            .appendPath(endMillis.toString())
            .build()

        val projection = arrayOf(
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.CALENDAR_DISPLAY_NAME // Let's see which calendar owns the event
        )

        val cursor = context.contentResolver.query(
            uri, projection, null, null, "${CalendarContract.Instances.BEGIN} ASC"
        )

        cursor?.use {
            val titleIndex = it.getColumnIndex(CalendarContract.Instances.TITLE)
            val dateIndex = it.getColumnIndex(CalendarContract.Instances.BEGIN)
            val calNameIndex = it.getColumnIndex(CalendarContract.Instances.CALENDAR_DISPLAY_NAME)

            while (it.moveToNext()) {
                val title = it.getString(titleIndex)
                val time = it.getLong(dateIndex)
                val calName = it.getString(calNameIndex)

                println("APP_DEBUG: Found Event: '$title' at $time (Calendar: $calName)")

                if (title.isNotBlank()) {
                    // Filter out duplicates in the list itself
                    if (!events.contains(title)) {
                        events.add(title)
                    }
                }
            }
        }
        return events
    }
}