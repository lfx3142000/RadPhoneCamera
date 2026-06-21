package com.radphonecamera.app.data

import android.content.Context

data class DetectorSettings(
    val localEventLogEnabled: Boolean = true,
    val expertDiagnosticsEnabled: Boolean = false,
)

class DetectorSettingsStore(
    context: Context,
) {
    private val prefs = context.getSharedPreferences("detector_settings", Context.MODE_PRIVATE)

    fun load(): DetectorSettings = DetectorSettings(
        localEventLogEnabled = prefs.getBoolean(KEY_LOCAL_EVENT_LOG_ENABLED, true),
        expertDiagnosticsEnabled = prefs.getBoolean(KEY_EXPERT_DIAGNOSTICS_ENABLED, false),
    )

    fun save(settings: DetectorSettings) {
        prefs.edit()
            .putBoolean(KEY_LOCAL_EVENT_LOG_ENABLED, settings.localEventLogEnabled)
            .putBoolean(KEY_EXPERT_DIAGNOSTICS_ENABLED, settings.expertDiagnosticsEnabled)
            .apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        private const val KEY_LOCAL_EVENT_LOG_ENABLED = "local_event_log_enabled"
        private const val KEY_EXPERT_DIAGNOSTICS_ENABLED = "expert_diagnostics_enabled"
    }
}
