package com.offex7.streamhub

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class SavedRemoteDevice(
    val id: String,
    val name: String,
    val host: String,
    val connectionType: String,
    val lastConnectedAt: Long
)

/** Small bounded history of successful remote connections; values stay local to the app. */
class RemoteDeviceHistory(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("remote_device_history_v1", Context.MODE_PRIVATE)
    private val key = "devices"

    @Synchronized
    fun devices(): List<SavedRemoteDevice> {
        val raw = preferences.getString(key, null).orEmpty()
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val json = JSONArray(raw)
            buildList {
                for (index in 0 until json.length()) {
                    val item = json.optJSONObject(index) ?: continue
                    val host = item.optString("host").trim()
                    val type = item.optString("type").trim()
                    val name = item.optString("name").trim()
                    if (host.isBlank() || name.isBlank() || type !in setOf("Wi-Fi", "Bluetooth")) continue
                    add(
                        SavedRemoteDevice(
                            id = item.optString("id").ifBlank { "$type|$host" },
                            name = name.take(16),
                            host = host,
                            connectionType = type,
                            lastConnectedAt = item.optLong("at", 0L)
                        )
                    )
                }
            }.sortedByDescending { it.lastConnectedAt }
        }.getOrDefault(emptyList())
    }

    @Synchronized
    fun record(host: String, defaultName: String, connectionType: String) {
        val cleanHost = host.trim()
        val cleanName = defaultName.trim().take(16)
        if (cleanHost.isBlank() || cleanName.isBlank() || connectionType !in setOf("Wi-Fi", "Bluetooth")) return
        val id = "$connectionType|$cleanHost"
        val updated = devices().filterNot { it.id == id }.toMutableList()
        updated.add(SavedRemoteDevice(id, cleanName, cleanHost, connectionType, System.currentTimeMillis()))
        write(updated.sortedByDescending { it.lastConnectedAt }.take(20))
    }

    @Synchronized
    fun rename(id: String, name: String): Boolean {
        val clean = name.trim().take(16)
        if (clean.isBlank()) return false
        val current = devices()
        if (current.none { it.id == id }) return false
        write(current.map { if (it.id == id) it.copy(name = clean) else it })
        return true
    }

    @Synchronized
    fun remove(id: String) {
        write(devices().filterNot { it.id == id })
    }

    @Synchronized
    private fun write(devices: List<SavedRemoteDevice>) {
        val json = JSONArray()
        devices.forEach { device ->
            json.put(
                JSONObject()
                    .put("id", device.id)
                    .put("name", device.name.take(16))
                    .put("host", device.host)
                    .put("type", device.connectionType)
                    .put("at", device.lastConnectedAt)
            )
        }
        preferences.edit().putString(key, json.toString()).apply()
    }
}
