package com.radphonecamera.app.patrol

import com.radphonecamera.app.sensors.BatteryThermalState
import com.radphonecamera.app.sensors.DevicePosture
import com.radphonecamera.app.sensors.MotionQuality
import com.radphonecamera.app.sensors.MotionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PatrolSchedulerTest {
    @Test
    fun patrolRequiresUsableBaseline() {
        val status = PatrolScheduler.evaluate(
            enabled = true,
            mode = PatrolBatteryMode.Balanced,
            hasUsableBaseline = false,
            baselineStale = false,
            motionState = stillFaceDown,
            batteryThermalState = normalBattery,
        )

        assertEquals(PatrolReadiness.NeedsBaseline, status.readiness)
        assertFalse(status.allowsCameraBurst)
    }

    @Test
    fun lowBatteryPausesPatrolUnlessCharging() {
        val status = PatrolScheduler.evaluate(
            enabled = true,
            mode = PatrolBatteryMode.Balanced,
            hasUsableBaseline = true,
            baselineStale = false,
            motionState = stillFaceDown,
            batteryThermalState = normalBattery.copy(batteryPercent = 10),
        )

        assertEquals(PatrolReadiness.PausedLowBattery, status.readiness)
        assertFalse(status.allowsCameraBurst)
    }

    @Test
    fun stillFaceDownStateAllowsBoundedBurst() {
        val status = PatrolScheduler.evaluate(
            enabled = true,
            mode = PatrolBatteryMode.BatterySaver,
            hasUsableBaseline = true,
            baselineStale = false,
            motionState = stillFaceDown,
            batteryThermalState = normalBattery,
        )

        assertEquals(PatrolReadiness.ReadyForBurst, status.readiness)
        assertEquals(2, status.burstDurationSeconds)
        assertTrue(status.allowsCameraBurst)
    }

    @Test
    fun patrolClosesTheCameraWhenTheAppIsNotVisible() {
        val status = PatrolScheduler.evaluate(
            enabled = true,
            mode = PatrolBatteryMode.Balanced,
            hasUsableBaseline = true,
            baselineStale = false,
            motionState = stillFaceDown,
            batteryThermalState = normalBattery,
            appInForeground = false,
        )

        assertEquals(PatrolReadiness.PausedBackground, status.readiness)
        assertFalse(status.allowsCameraBurst)
    }

    private val stillFaceDown = MotionState(
        quality = MotionQuality.Still,
        posture = DevicePosture.FaceDown,
        sampleCount = 8,
        deltaG = 0.01,
        reason = "Still.",
    )

    private val normalBattery = BatteryThermalState(
        batteryPercent = 80,
        isCharging = false,
        thermalStatus = "Normal",
        isWarmOrHot = false,
    )
}
