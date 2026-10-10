package com.radiotv.control.core

enum class RemoteDeviceType(val label: String) {
    ANDROID_TV("Android TV / Google TV"),
    CHROMECAST("Google Cast"),
    SAMSUNG("Samsung Tizen"),
    LG("LG webOS"),
    ROKU("Roku"),
    DLNA("DLNA / UPnP"),
    AIRPLAY("AirPlay"),
    UNKNOWN("Неизвестное устройство")
}

data class DiscoveredRemoteDevice(
    val ip: String,
    val name: String,
    val type: RemoteDeviceType,
    val brand: String? = null,
    val model: String? = null,
    val ports: Set<Int> = emptySet(),
    val services: Set<String> = emptySet()
)
