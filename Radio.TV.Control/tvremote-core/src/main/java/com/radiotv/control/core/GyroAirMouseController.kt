package com.radiotv.control.core

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface AirMouseStatus {
    data object Off : AirMouseStatus
    data object Active : AirMouseStatus
    data class Unavailable(val reason: String) : AirMouseStatus
    data class Error(val reason: String) : AirMouseStatus
}

/**
 * Optional gyroscope-driven relative mouse for Bluetooth HID hosts.
 * It deliberately does not emulate a Wi-Fi cursor protocol: mouse reports go to the connected HID host.
 */
class GyroAirMouseController(
    context: Context,
    private val bluetoothHid: BluetoothHidController
) : SensorEventListener, AutoCloseable {
    private val appContext = context.applicationContext
    private val sensorManager = appContext.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val gyroscope: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val workerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val statusMutable = MutableStateFlow<AirMouseStatus>(AirMouseStatus.Off)
    val status: StateFlow<AirMouseStatus> = statusMutable.asStateFlow()

    @Volatile private var enabled = false
    private var lastTimestamp = 0L
    private var carryX = 0f
    private var carryY = 0f

    /**
     * Toggles the gyro pointer. Returns a user-facing explanation rather than pretending to enable
     * it if the phone has no gyroscope or no TV is connected over Bluetooth HID.
     */
    fun toggle(): String {
        if (enabled) {
            stopWith(AirMouseStatus.Off)
            return "Аэромышь выключена."
        }
        if (gyroscope == null || sensorManager == null) {
            val reason = "В телефоне нет доступного гироскопа."
            statusMutable.value = AirMouseStatus.Unavailable(reason)
            return reason
        }
        if (bluetoothHid.status.value !is BluetoothHidStatus.Connected) {
            val reason = "Сначала подключите телевизор через Bluetooth HID."
            statusMutable.value = AirMouseStatus.Unavailable(reason)
            return reason
        }

        val registered = runCatching {
            sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_GAME)
        }.getOrDefault(false)
        if (!registered) {
            val reason = "Не удалось запустить гироскопический датчик."
            statusMutable.value = AirMouseStatus.Error(reason)
            return reason
        }
        lastTimestamp = 0L
        carryX = 0f
        carryY = 0f
        enabled = true
        statusMutable.value = AirMouseStatus.Active
        return "Аэромышь включена. Поворачивайте телефон для перемещения курсора."
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!enabled || event.sensor.type != Sensor.TYPE_GYROSCOPE) return
        if (bluetoothHid.status.value !is BluetoothHidStatus.Connected) {
            stopWith(AirMouseStatus.Unavailable("Bluetooth HID соединение с ТВ потеряно; аэромышь выключена."))
            return
        }
        if (lastTimestamp == 0L) {
            lastTimestamp = event.timestamp
            return
        }
        val elapsed = ((event.timestamp - lastTimestamp).coerceAtLeast(0L) / NANOS_PER_SECOND)
            .toFloat()
            .coerceIn(0f, MAX_FRAME_SECONDS)
        lastTimestamp = event.timestamp
        if (elapsed <= 0f) return

        // Gyroscope values are rad/s. Integrate yaw/pitch and map angle deltas to relative HID pixels.
        carryX += -event.values[1] * elapsed * PIXELS_PER_RADIAN
        carryY += event.values[0] * elapsed * PIXELS_PER_RADIAN
        val dx = carryX.toInt().coerceIn(-127, 127)
        val dy = carryY.toInt().coerceIn(-127, 127)
        carryX -= dx
        carryY -= dy
        if (dx == 0 && dy == 0) return

        workerScope.launch {
            runCatching { bluetoothHid.moveMouse(dx, dy) }
                .onFailure {
                    statusMutable.value = AirMouseStatus.Error(
                        it.message ?: "Не удалось отправить движение аэромыши."
                    )
                }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun stopWith(next: AirMouseStatus) {
        enabled = false
        runCatching { sensorManager?.unregisterListener(this) }
        lastTimestamp = 0L
        carryX = 0f
        carryY = 0f
        statusMutable.value = next
    }

    override fun close() {
        stopWith(AirMouseStatus.Off)
        workerScope.cancel()
    }

    private companion object {
        const val NANOS_PER_SECOND = 1_000_000_000L
        const val MAX_FRAME_SECONDS = 0.05f
        const val PIXELS_PER_RADIAN = 520f
    }
}
