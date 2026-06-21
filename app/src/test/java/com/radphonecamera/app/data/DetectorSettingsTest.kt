package com.radphonecamera.app.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DetectorSettingsTest {
    @Test
    fun defaultsKeepLocalLogsEnabledAndExpertDiagnosticsOff() {
        val settings = DetectorSettings()

        assertTrue(settings.localEventLogEnabled)
        assertFalse(settings.expertDiagnosticsEnabled)
    }
}
