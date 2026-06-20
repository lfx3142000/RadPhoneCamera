package com.radphonecamera.app.sensors

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager

data class BatteryThermalState(
    val batteryPercent: Int?,
    val isCharging: Boolean,
    val thermalStatus: String,
    val isWarmOrHot: Boolean,
) {
    companion object {
        val Unknown = BatteryThermalState(
            batteryPercent = null,
            isCharging = false,
            thermalStatus = "Unknown",
            isWarmOrHot = false,
        )
    }
}

class BatteryThermalStateProvider(
    private val context: Context,
) {
    fun read(): BatteryThermalState {
        val batteryIntent = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
        )
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPercent = if (level >= 0 && scale > 0) {
            ((level * 100.0) / scale).toInt().coerceIn(0, 100)
        } else {
            null
        }

        val powerManager = context.getSystemService(PowerManager::class.java)
        val thermalStatus = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && powerManager != null) {
            powerManager.currentThermalStatus
        } else {
            PowerManager.THERMAL_STATUS_NONE
        }

        return BatteryThermalState(
            batteryPercent = batteryPercent,
            isCharging = isCharging,
            thermalStatus = thermalStatus.label(),
            isWarmOrHot = thermalStatus >= PowerManager.THERMAL_STATUS_LIGHT,
        )
    }

    private fun Int.label(): String = when (this) {
        PowerManager.THERMAL_STATUS_NONE -> "Normal"
        PowerManager.THERMAL_STATUS_LIGHT -> "Warm"
        PowerManager.THERMAL_STATUS_MODERATE -> "Moderate"
        PowerManager.THERMAL_STATUS_SEVERE -> "Severe"
        PowerManager.THERMAL_STATUS_CRITICAL -> "Critical"
        PowerManager.THERMAL_STATUS_EMERGENCY -> "Emergency"
        PowerManager.THERMAL_STATUS_SHUTDOWN -> "Shutdown"
        else -> "Unknown"
    }
}
