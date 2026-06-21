package com.radphonecamera.app.baseline

import com.radphonecamera.app.detector.AlarmState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BaselineRefreshEvaluatorTest {
    @Test
    fun missingBaselineRequiresCollection() {
        val recommendation = BaselineRefreshEvaluator.evaluate(
            baseline = null,
            currentEnvironment = currentEnvironment,
            recentAlarmStates = emptyList(),
            nowMillis = NOW,
        )

        assertEquals(listOf(BaselineRefreshReason.Missing), recommendation.reasons)
    }

    @Test
    fun identifiesEnvironmentChangesAndRepeatedLimitedScans() {
        val baseline = usableBaseline(
            collectedAtMillis = NOW - STALE_MILLIS - 1L,
            environment = BaselineEnvironmentSnapshot(
                appVersion = "0.2.0",
                androidApiLevel = 34,
                deviceModel = "Test Phone",
                cameraSignature = "camera-0-v1",
                thermalStatus = "Normal",
            ),
        )

        val recommendation = BaselineRefreshEvaluator.evaluate(
            baseline = baseline,
            currentEnvironment = currentEnvironment,
            recentAlarmStates = listOf(
                AlarmState.LimitedSensitivity,
                AlarmState.Invalid,
                AlarmState.Baseline,
            ),
            nowMillis = NOW,
        )

        assertTrue(recommendation.reasons.contains(BaselineRefreshReason.Stale))
        assertTrue(recommendation.reasons.contains(BaselineRefreshReason.AppUpdated))
        assertTrue(recommendation.reasons.contains(BaselineRefreshReason.AndroidUpdated))
        assertTrue(recommendation.reasons.contains(BaselineRefreshReason.CameraChanged))
        assertTrue(recommendation.reasons.contains(BaselineRefreshReason.ThermalChanged))
        assertTrue(recommendation.reasons.contains(BaselineRefreshReason.RepeatedLimitedScans))
    }

    @Test
    fun legacyBaselineWithoutEnvironmentMetadataStaysUsable() {
        val recommendation = BaselineRefreshEvaluator.evaluate(
            baseline = usableBaseline(environment = BaselineEnvironmentSnapshot()),
            currentEnvironment = currentEnvironment,
            recentAlarmStates = listOf(AlarmState.Baseline),
            nowMillis = NOW,
        )

        assertFalse(recommendation.shouldRefresh)
    }

    private fun usableBaseline(
        collectedAtMillis: Long = NOW,
        environment: BaselineEnvironmentSnapshot,
    ): BaselineResult = BaselineResult(
        quality = BaselineQuality.Good,
        progress = BaselineProgress(totalFrames = 60, goodFrames = 60),
        message = "Ready",
        cameraId = "0",
        collectedAtMillis = collectedAtMillis,
        environment = environment,
    )

    private companion object {
        const val NOW = 1_000_000_000L
        const val STALE_MILLIS = 72L * 60L * 60L * 1_000L
        val currentEnvironment = BaselineEnvironmentSnapshot(
            appVersion = "0.2.1",
            androidApiLevel = 35,
            deviceModel = "Test Phone",
            cameraSignature = "camera-0-v2",
            thermalStatus = "Warm",
        )
    }
}
