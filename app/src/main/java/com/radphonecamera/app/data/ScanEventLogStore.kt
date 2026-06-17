package com.radphonecamera.app.data

import android.content.Context

class ScanEventLogStore(
    context: Context,
) {
    private val prefs = context.getSharedPreferences("scan_event_log", Context.MODE_PRIVATE)

    fun load(): List<ScanEvent> =
        ScanEventLogCodec.decode(prefs.getString(KEY_EVENTS, null).orEmpty())

    fun append(event: ScanEvent) {
        save((listOf(event) + load()).take(MAX_EVENTS))
    }

    private fun save(events: List<ScanEvent>) {
        prefs.edit()
            .putString(KEY_EVENTS, ScanEventLogCodec.encode(events))
            .apply()
    }

    companion object {
        private const val KEY_EVENTS = "events"
        private const val MAX_EVENTS = 20
    }
}
