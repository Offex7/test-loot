package com.offex7.streamhub

/**
 * Compatibility helpers for the v3 UI call sites while SettingsStore uses
 * explicit TV/Radio namespaces in v4.
 */
suspend fun SettingsStore.addUsageMillis(section: Section, millis: Long) {
    if (millis > 0L) addUsageSeconds(section, millis / 1000L)
}

suspend fun SettingsStore.favorites(): Set<String> = favorites(Section.TV)

suspend fun SettingsStore.isFavorite(channelId: String): Boolean {
    val section = if (RADIO_STATIONS.any { it.url == channelId }) Section.RADIO else Section.TV
    return favorites(section).contains(channelId)
}

suspend fun SettingsStore.setFavorite(channelId: String, value: Boolean) {
    val section = if (RADIO_STATIONS.any { it.url == channelId }) Section.RADIO else Section.TV
    setFavorite(section, channelId, value)
}
