package com.radiotv.tvremote.core

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class AirMouseEngine(context: Context, private val onMove: suspend (dx: Int, dy: Int) -> Unit) :
    SensorEventListener {
    private val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val gyro = manager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private var job: Job? = null

    val hasGyroscope: Boolean get() = gyro != null

    fun start() {
        val sensor = gyro ?: return
        manager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
    }
    fun stop() { manager.unregisterListener(this); job?.cancel(); job = null }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_GYROSCOPE) return
        val dx = (-event.values[1] * 18f).roundToInt().coerceIn(-20, 20)
        val dy = (event.values[0] * 18f).roundToInt().coerceIn(-20, 20)
        if (dx == 0 && dy == 0) return
        job?.cancel()
        job = CoroutineScope(Dispatchers.Main.immediate).launch { onMove(dx, dy) }
    }
    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
}
