package com.radphonecamera.app.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class MotionStateProvider(
    context: Context,
) : SensorEventListener {
    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val classifier = MotionStateClassifier()
    private var onState: ((MotionState) -> Unit)? = null

    fun start(onState: (MotionState) -> Unit) {
        this.onState = onState
        val sensor = accelerometer
        if (sensor == null || sensorManager == null) {
            onState(MotionState.Unavailable)
            return
        }
        sensorManager.registerListener(
            this,
            sensor,
            SensorManager.SENSOR_DELAY_UI,
        )
    }

    fun stop() {
        sensorManager?.unregisterListener(this)
        onState = null
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER || event.values.size < 3) return
        val state = classifier.record(
            MotionSample(
                x = event.values[0],
                y = event.values[1],
                z = event.values[2],
            ),
        )
        onState?.invoke(state)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
