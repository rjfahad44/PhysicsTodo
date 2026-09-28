package com.bitbytestudio.physicstodo.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue

class MotionSensor(
    context: Context
) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    var tiltX by mutableFloatStateOf(0f)
        private set

    var tiltY by mutableFloatStateOf(0f)
        private set

    private var filteredX = 0f
    private var filteredY = 0f

    fun start() {
        accelerometer ?: return
        sensorManager.registerListener(
            this,
            accelerometer,
            SensorManager.SENSOR_DELAY_GAME
        )
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(
        event: SensorEvent?
    ) {
        event ?: return
        if (
            event.sensor.type !=
            Sensor.TYPE_ACCELEROMETER
        ) {
            return
        }

        val rawX = event.values[0]
        val rawY = event.values[1]

        /*
         * Low pass filter.
         */
        filteredX += (rawX - filteredX) * 0.08f
        filteredY += (rawY - filteredY) * 0.08f

        /*
         * Normalize acceleration.
         */
        tiltX = (-filteredX / 9.81f).coerceIn(-1f, 1f)
        tiltY = (filteredY / 9.81f).coerceIn(-1f, 1f)
    }

    override fun onAccuracyChanged(
        sensor: Sensor?,
        accuracy: Int
    ) {
        // Nothing needed.
    }
}