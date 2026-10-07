package com.offex7.streamhub

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("streamhub_settings")

class SettingsStore(private val context: Context) {
    private val sectionKey = stringPreferencesKey("last_section")
    private val sourceKey = intPreferencesKey("tv_source_index")
    private val tvSourceInitializedKey = booleanPreferencesKey("tv_source_initialized_v1")
    private val activeSourceKeyStorage = stringPreferencesKey("tv_active_source")
    private val userPlaylistsStorage = stringPreferencesKey("tv_user_playlists_v1")
    private val exitKey = longPreferencesKey("last_exit_time")
    private val tvNameKey = stringPreferencesKey("last_tv_name")
    private val tvUrlKey = stringPreferencesKey("last_tv_url")
    private val tvLogoKey = stringPreferencesKey("last_tv_logo")
    private val radioNameKey = stringPreferencesKey("last_radio_name")
    private val radioUrlKey = stringPreferencesKey("last_radio_url")
    private val radioLogoKey = stringPreferencesKey("last_radio_logo")
    private val tvFavoritesKey = stringSetPreferencesKey("favorites_tv")
    private val radioFavoritesKey = stringSetPreferencesKey("favorites_radio")
    private val tvFavoriteTimesKey = stringPreferencesKey("favorites_tv_added_at_v1")
    private val radioFavoriteTimesKey = stringPreferencesKey("favorites_radio_added_at_v1")
    private val pipKey = booleanPreferencesKey("pip_on_minimize")
    private val tvUsageKey = longPreferencesKey("usage_tv_seconds")
    private val radioUsageKey = longPreferencesKey("usage_radio_seconds")
    private val tvChannelUsageKey = stringPreferencesKey("usage_tv_channels")
    private val radioStationUsageKey = stringPreferencesKey("usage_radio_stations")
    private val tvScrollIndexKey = intPreferencesKey("tv_scroll_index")
    private val tvScrollOffsetKey = intPreferencesKey("tv_scroll_offset")
    private val radioScrollIndexKey = intPreferencesKey("radio_scroll_index")
    private val radioScrollOffsetKey = intPreferencesKey("radio_scroll_offset")
    private val channelZoomsStorage = stringPreferencesKey("tv_channel_zooms_v1")
    private val firstLaunchKey = booleanPreferencesKey("first_launch_v2")
    private val hiddenChannelsKey = stringSetPreferencesKey("hiddenChannels")
    private val tvSearchHistoryKey = stringPreferencesKey("search_history_tv_v1")
    private val radioSearchHistoryKey = stringPreferencesKey("search_history_radio_v1")
    private val energySavingModeKey = stringPreferencesKey("energy_saving_mode_v1")
    private val hapticsKey = booleanPreferencesKey("haptics_enabled_v1")
    private val autoStartKey = booleanPreferencesKey("autostart_android_tv_v1")
    private val soundFeedbackKey = booleanPreferencesKey("sound_feedback_enabled_v1")
    private val equalizerBassKey = intPreferencesKey("radio_eq_bass_v1")
    private val equalizerMidKey = intPreferencesKey("radio_eq_mid_v1")
    private val equalizerTrebleKey = intPreferencesKey("radio_eq_treble_v1")
    private val equalizerPresetKey = stringPreferencesKey("radio_eq_preset_v1")
    private val normalizeKey = booleanPreferencesKey("radio_normalize_v1")
    private val updateDismissUntilKey = longPreferencesKey("update_dismiss_until_v1")
    private val updateDismissVersionKey = stringPreferencesKey("update_dismiss_version_v1")

    suspend fun disclaimerShown(): Boolean = context.dataStore.data.first()[firstLaunchKey] ?: false
    suspend fun setDisclaimerShown(shown: Boolean) { context.dataStore.edit { it[firstLaunchKey] = shown } }

    fun hiddenChannelsFlow(): Flow<Set<String>> =
        context.dataStore.data.map { it[hiddenChannelsKey] ?: emptySet() }

    suspend fun hiddenChannels(): Set<String> = context.dataStore.data.first()[hiddenChannelsKey] ?: emptySet()

    suspend fun addHiddenChannel(channelId: String) {
        val id = channelId.trim()
        if (id.isBlank()) return
        context.dataStore.edit {
            val set = (it[hiddenChannelsKey] ?: emptySet()).toMutableSet()
            set.add(id)
            it[hiddenChannelsKey] = set
        }
    }

    suspend fun removeHiddenChannel(channelId: String) {
        val id = channelId.trim()
        if (id.isBlank()) return
        context.dataStore.edit {
            val set = (it[hiddenChannelsKey] ?: emptySet()).toMutableSet()
            set.remove(id)
            it[hiddenChannelsKey] = set
        }
    }

    suspend fun clearHiddenChannels() {
        context.dataStore.edit { it[hiddenChannelsKey] = emptySet() }
    }


    suspend fun lastSection(): Section? = context.dataStore.data.first()[sectionKey]?.let { runCatching { Section.valueOf(it) }.getOrNull() }
    suspend fun setSection(section: Section) { context.dataStore.edit { it[sectionKey] = section.name } }
    suspend fun clearSection() { context.dataStore.edit { it.remove(sectionKey) } }

    suspend fun sourceIndex(): Int = context.dataStore.data.first()[sourceKey] ?: 0
    suspend fun ensureInitialTvSource(): Boolean {
        val prefs = context.dataStore.data.first()
        if (prefs[tvSourceInitializedKey] == true) return false
        context.dataStore.edit {
            it[activeSourceKeyStorage] = builtinSourceKey(0)
            it[sourceKey] = 0
            it[tvSourceInitializedKey] = true
        }
        return true
    }


    suspend fun setSourceIndex(index: Int) {
        setActiveSourceKey(builtinSourceKey(index))
    }

    suspend fun activeSourceKey(): String {
        val prefs = context.dataStore.data.first()
        return prefs[activeSourceKeyStorage] ?: builtinSourceKey(prefs[sourceKey] ?: 0)
    }

    fun activeSourceFlow(): Flow<String> =
        context.dataStore.data.map { prefs ->
            prefs[activeSourceKeyStorage] ?: builtinSourceKey(prefs[sourceKey] ?: 0)
        }

    suspend fun setActiveSourceKey(key: String) {
        val normalized = key.trim()
        if (normalized.isBlank()) return
        context.dataStore.edit {
            it[activeSourceKeyStorage] = normalized
            if (normalized.startsWith(BUILTIN_SOURCE_PREFIX)) {
                normalized.removePrefix(BUILTIN_SOURCE_PREFIX).toIntOrNull()?.let { index ->
                    it[sourceKey] = index.coerceIn(0, TV_SOURCES.lastIndex)
                }
            }
        }
    }

    suspend fun userPlaylists(): List<UserPlaylist> {
        val raw = context.dataStore.data.first()[userPlaylistsStorage].orEmpty()
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val json = org.json.JSONArray(raw)
            buildList {
                for (i in 0 until json.length()) {
                    val item = json.optJSONObject(i) ?: continue
                    val name = item.optString("name").trim()
                    val url = item.optString("url").trim()
                    if (name.isNotBlank() && (url.startsWith("http://", true) || url.startsWith("https://", true))) {
                        add(UserPlaylist(name, url))
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    suspend fun addUserPlaylist(name: String, url: String): Boolean {
        val cleanName = name.trim()
        val cleanUrl = url.trim()
        if (cleanName.isBlank() || !(cleanUrl.startsWith("http://", true) || cleanUrl.startsWith("https://", true))) return false
        val current = userPlaylists()
        if (current.any { it.url.equals(cleanUrl, ignoreCase = true) }) return false
        context.dataStore.edit { it[userPlaylistsStorage] = encodeUserPlaylists(current + UserPlaylist(cleanName, cleanUrl)) }
        return true
    }

    suspend fun removeUserPlaylist(url: String) {
        val cleanUrl = url.trim()
        val updated = userPlaylists().filterNot { it.url.equals(cleanUrl, ignoreCase = true) }
        context.dataStore.edit { it[userPlaylistsStorage] = encodeUserPlaylists(updated) }
        if (activeSourceKey() == userPlaylistKey(cleanUrl)) setActiveSourceKey(builtinSourceKey(0))
    }

    private fun encodeUserPlaylists(items: List<UserPlaylist>): String =
        org.json.JSONArray().apply {
            items.forEach { put(org.json.JSONObject().apply { put("name", it.name); put("url", it.url) }) }
        }.toString()

    suspend fun lastExitTime(): Long = context.dataStore.data.first()[exitKey] ?: 0L
    suspend fun setLastExitTime(timestamp: Long) { context.dataStore.edit { it[exitKey] = timestamp } }

    suspend fun saveLastStream(section: Section, item: StreamItem) {
        context.dataStore.edit {
            when (section) {
                Section.TV -> {
                    it[tvNameKey] = item.name; it[tvUrlKey] = item.url
                    item.logoUrl?.let { value -> it[tvLogoKey] = value } ?: it.remove(tvLogoKey)
                }
                Section.RADIO -> {
                    it[radioNameKey] = item.name; it[radioUrlKey] = item.url
                    item.logoUrl?.let { value -> it[radioLogoKey] = value } ?: it.remove(radioLogoKey)
                }
            }
        }
    }

    suspend fun lastStream(section: Section): StreamItem? {
        val prefs = context.dataStore.data.first()
        val nk = if (section == Section.TV) tvNameKey else radioNameKey
        val uk = if (section == Section.TV) tvUrlKey else radioUrlKey
        val lk = if (section == Section.TV) tvLogoKey else radioLogoKey
        val name = prefs[nk] ?: return null
        val url = prefs[uk] ?: return null
        return StreamItem(name, url, prefs[lk])
    }

    suspend fun favorites(section: Section): Set<String> =
        context.dataStore.data.first()[if (section == Section.TV) tvFavoritesKey else radioFavoritesKey] ?: emptySet()

    suspend fun favoriteAddedAt(section: Section): Map<String, Long> {
        val prefs = context.dataStore.data.first()
        val favoriteKey = if (section == Section.TV) tvFavoritesKey else radioFavoritesKey
        val timeKey = if (section == Section.TV) tvFavoriteTimesKey else radioFavoriteTimesKey
        val favorites = prefs[favoriteKey].orEmpty()
        val existing = decodeFavoriteTimes(prefs[timeKey]).toMutableMap()
        val missing = favorites.filterNot { existing.containsKey(it) }.sorted()
        if (missing.isNotEmpty()) {
            var next = maxOf(System.currentTimeMillis(), (existing.values.maxOrNull() ?: 0L) + 1L)
            missing.forEach { id ->
                existing[id] = next
                next += 1L
            }
            context.dataStore.edit { it[timeKey] = encodeFavoriteTimes(existing) }
        }
        return existing
    }

    suspend fun setFavorite(section: Section, channelId: String, value: Boolean) {
        val id = channelId.trim()
        if (id.isBlank()) return
        context.dataStore.edit {
            val favoriteKey = if (section == Section.TV) tvFavoritesKey else radioFavoritesKey
            val timeKey = if (section == Section.TV) tvFavoriteTimesKey else radioFavoriteTimesKey
            val set = (it[favoriteKey] ?: emptySet()).toMutableSet()
            val times = decodeFavoriteTimes(it[timeKey]).toMutableMap()
            if (value) {
                set.add(id)
                val now = System.currentTimeMillis()
                val next = maxOf(now, (times.values.maxOrNull() ?: 0L) + 1L)
                times[id] = next
            } else {
                set.remove(id)
                times.remove(id)
            }
            it[favoriteKey] = set
            it[timeKey] = encodeFavoriteTimes(times)
        }
    }

    fun favoritesFlow(section: Section): Flow<Set<String>> =
        context.dataStore.data.map { it[if (section == Section.TV) tvFavoritesKey else radioFavoritesKey] ?: emptySet() }

    private fun decodeFavoriteTimes(raw: String?): Map<String, Long> {
        if (raw.isNullOrBlank()) return emptyMap()
        return runCatching {
            val json = org.json.JSONObject(raw)
            buildMap {
                json.keys().forEach { key ->
                    val value = json.optLong(key, 0L)
                    if (key.isNotBlank() && value > 0L) put(key, value)
                }
            }
        }.getOrDefault(emptyMap())
    }

    private fun encodeFavoriteTimes(values: Map<String, Long>): String =
        org.json.JSONObject().apply {
            values.filterValues { it > 0L }.forEach { (key, value) -> put(key, value) }
        }.toString()

    suspend fun pipEnabled(): Boolean = context.dataStore.data.first()[pipKey] ?: true
    suspend fun setPipEnabled(enabled: Boolean) { context.dataStore.edit { it[pipKey] = enabled } }

    fun usageFlow(section: Section): Flow<Long> = context.dataStore.data.map { it[if (section == Section.TV) tvUsageKey else radioUsageKey] ?: 0L }
    suspend fun addUsageSeconds(section: Section, seconds: Long) {
        if (seconds <= 0L) return
        context.dataStore.edit {
            val key = if (section == Section.TV) tvUsageKey else radioUsageKey
            it[key] = (it[key] ?: 0L) + seconds
        }
    }

    fun channelUsageFlow(section: Section): Flow<Map<String, Long>> =
        context.dataStore.data.map { prefs ->
            decodeUsageMap(prefs[if (section == Section.TV) tvChannelUsageKey else radioStationUsageKey])
        }

    suspend fun addChannelUsage(section: Section, usage: Map<String, Long>) {
        val cleaned = usage.filterValues { it > 0L }
        if (cleaned.isEmpty()) return
        context.dataStore.edit { prefs ->
            val key = if (section == Section.TV) tvChannelUsageKey else radioStationUsageKey
            val current = decodeUsageMap(prefs[key]).toMutableMap()
            cleaned.forEach { (id, seconds) ->
                if (id.isNotBlank() && seconds > 0L) current[id] = (current[id] ?: 0L) + seconds
            }
            prefs[key] = encodeUsageMap(current)
        }
    }

    suspend fun addPlaybackUsage(section: Section, usage: Map<String, Long>) {
        val total = usage.values.filter { it > 0L }.sum()
        if (total <= 0L) return
        context.dataStore.edit { prefs ->
            val totalKey = if (section == Section.TV) tvUsageKey else radioUsageKey
            val mapKey = if (section == Section.TV) tvChannelUsageKey else radioStationUsageKey
            prefs[totalKey] = (prefs[totalKey] ?: 0L) + total
            val current = decodeUsageMap(prefs[mapKey]).toMutableMap()
            usage.forEach { (id, seconds) ->
                if (id.isNotBlank() && seconds > 0L) current[id] = (current[id] ?: 0L) + seconds
            }
            prefs[mapKey] = encodeUsageMap(current)
        }
    }

    suspend fun resetUsage(section: Section) {
        context.dataStore.edit {
            it[if (section == Section.TV) tvUsageKey else radioUsageKey] = 0L
            it[if (section == Section.TV) tvChannelUsageKey else radioStationUsageKey] = "{}"
        }
    }

    private fun decodeUsageMap(raw: String?): Map<String, Long> {
        if (raw.isNullOrBlank()) return emptyMap()
        return runCatching {
            val json = org.json.JSONObject(raw)
            json.keys().asSequence().associateWith { key -> json.optLong(key, 0L).coerceAtLeast(0L) }
        }.getOrDefault(emptyMap())
    }

    private fun encodeUsageMap(map: Map<String, Long>): String =
        org.json.JSONObject().apply { map.forEach { (key, value) -> put(key, value.coerceAtLeast(0L)) } }.toString()

    suspend fun scrollPosition(section: Section): Pair<Int, Int> {
        val p = context.dataStore.data.first()
        val i = p[if (section == Section.TV) tvScrollIndexKey else radioScrollIndexKey] ?: 0
        val o = p[if (section == Section.TV) tvScrollOffsetKey else radioScrollOffsetKey] ?: 0
        return i.coerceAtLeast(0) to o.coerceAtLeast(0)
    }

    suspend fun saveScrollPosition(section: Section, index: Int, offset: Int) {
        context.dataStore.edit {
            if (section == Section.TV) { it[tvScrollIndexKey] = index; it[tvScrollOffsetKey] = offset }
            else { it[radioScrollIndexKey] = index; it[radioScrollOffsetKey] = offset }
        }
    }

    suspend fun channelZooms(): Map<String, Float> {
        val raw = context.dataStore.data.first()[channelZoomsStorage].orEmpty()
        return decodeZoomMap(raw)
    }

    suspend fun setChannelZoom(channelId: String, zoom: Float) {
        val id = channelId.trim()
        if (id.isBlank()) return
        val safeZoom = zoom.coerceIn(1f, 3f)
        context.dataStore.edit { prefs ->
            val current = decodeZoomMap(prefs[channelZoomsStorage]).toMutableMap()
            current[id] = safeZoom
            // Keep the store bounded in case the user watches many channels.
            while (current.size > 500) {
                current.remove(current.keys.first())
            }
            prefs[channelZoomsStorage] = encodeZoomMap(current)
        }
    }

    suspend fun removeChannelZoom(channelId: String) {
        val id = channelId.trim()
        if (id.isBlank()) return
        context.dataStore.edit { prefs ->
            val current = decodeZoomMap(prefs[channelZoomsStorage]).toMutableMap()
            current.remove(id)
            prefs[channelZoomsStorage] = encodeZoomMap(current)
        }
    }

    private fun decodeZoomMap(raw: String?): Map<String, Float> {
        if (raw.isNullOrBlank()) return emptyMap()
        return runCatching {
            val json = org.json.JSONObject(raw)
            json.keys().asSequence().associateWith { key ->
                json.optDouble(key, 1.0).toFloat().coerceIn(1f, 3f)
            }
        }.getOrDefault(emptyMap())
    }

    private fun encodeZoomMap(map: Map<String, Float>): String =
        org.json.JSONObject().apply {
            map.forEach { (id, zoom) -> put(id, zoom.toDouble()) }
        }.toString()


    suspend fun searchHistory(section: Section): List<String> {
        val key = if (section == Section.TV) tvSearchHistoryKey else radioSearchHistoryKey
        return decodeSearchHistory(context.dataStore.data.first()[key])
    }

    suspend fun rememberSearch(section: Section, query: String) {
        val clean = query.trim().replace(Regex("\\s+"), " ").take(80)
        if (clean.isBlank()) return
        val key = if (section == Section.TV) tvSearchHistoryKey else radioSearchHistoryKey
        val current = decodeSearchHistory(context.dataStore.data.first()[key])
        val updated = buildList {
            add(clean)
            current.filterNot { it.equals(clean, ignoreCase = true) }.forEach { add(it) }
        }.take(4)
        context.dataStore.edit { it[key] = org.json.JSONArray(updated).toString() }
    }

    suspend fun clearSearchHistory(section: Section) {
        val key = if (section == Section.TV) tvSearchHistoryKey else radioSearchHistoryKey
        context.dataStore.edit { it.remove(key) }
    }

    private fun decodeSearchHistory(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val json = org.json.JSONArray(raw)
            buildList {
                for (i in 0 until json.length()) {
                    json.optString(i).trim().takeIf { it.isNotBlank() }?.let(::add)
                }
            }.distinctBy { it.lowercase(java.util.Locale.ROOT) }.take(4)
        }.getOrDefault(emptyList())
    }

    suspend fun energySavingMode(): String =
        context.dataStore.data.first()[energySavingModeKey] ?: "OFF"

    fun energySavingModeFlow(): Flow<String> =
        context.dataStore.data.map { it[energySavingModeKey] ?: "OFF" }

    suspend fun setEnergySavingMode(mode: String) {
        val normalized = mode.uppercase(java.util.Locale.ROOT)
        if (normalized !in setOf("AUTO", "ON", "OFF")) return
        context.dataStore.edit {
            it[energySavingModeKey] = normalized
            if (normalized == "ON") it[autoStartKey] = false
        }
    }

    suspend fun hapticsEnabled(): Boolean =
        context.dataStore.data.first()[hapticsKey] ?: true

    fun hapticsFlow(): Flow<Boolean> =
        context.dataStore.data.map { it[hapticsKey] ?: true }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[hapticsKey] = enabled }
    }

    suspend fun autoStartEnabled(): Boolean =
        context.dataStore.data.first()[autoStartKey] ?: true

    fun autoStartFlow(): Flow<Boolean> =
        context.dataStore.data.map { it[autoStartKey] ?: true }

    suspend fun setAutoStartEnabled(enabled: Boolean) {
        context.dataStore.edit {
            val energyMode = it[energySavingModeKey] ?: "OFF"
            it[autoStartKey] = if (energyMode == "ON") false else enabled
        }
    }

    fun soundFeedbackFlow(): Flow<Boolean> =
        context.dataStore.data.map { it[soundFeedbackKey] ?: true }

    suspend fun setSoundFeedbackEnabled(enabled: Boolean) {
        context.dataStore.edit { it[soundFeedbackKey] = enabled }
    }

    data class RadioEqualizerSettings(
        val bass: Int = 0,
        val mid: Int = 0,
        val treble: Int = 0,
        val preset: String = "Flat",
        val normalize: Boolean = false
    )

    suspend fun radioEqualizerSettings(): RadioEqualizerSettings {
        val prefs = context.dataStore.data.first()
        return RadioEqualizerSettings(
            bass = (prefs[equalizerBassKey] ?: 0).coerceIn(-1500, 1500),
            mid = (prefs[equalizerMidKey] ?: 0).coerceIn(-1500, 1500),
            treble = (prefs[equalizerTrebleKey] ?: 0).coerceIn(-1500, 1500),
            preset = prefs[equalizerPresetKey] ?: "Flat",
            normalize = prefs[normalizeKey] ?: false
        )
    }

    fun radioEqualizerFlow(): Flow<RadioEqualizerSettings> =
        context.dataStore.data.map { prefs ->
            RadioEqualizerSettings(
                bass = (prefs[equalizerBassKey] ?: 0).coerceIn(-1500, 1500),
                mid = (prefs[equalizerMidKey] ?: 0).coerceIn(-1500, 1500),
                treble = (prefs[equalizerTrebleKey] ?: 0).coerceIn(-1500, 1500),
                preset = prefs[equalizerPresetKey] ?: "Flat",
                normalize = prefs[normalizeKey] ?: false
            )
        }

    suspend fun setRadioEqualizer(settings: RadioEqualizerSettings) {
        context.dataStore.edit {
            it[equalizerBassKey] = settings.bass.coerceIn(-1500, 1500)
            it[equalizerMidKey] = settings.mid.coerceIn(-1500, 1500)
            it[equalizerTrebleKey] = settings.treble.coerceIn(-1500, 1500)
            it[equalizerPresetKey] = settings.preset.take(32)
            it[normalizeKey] = settings.normalize
        }
    }

    suspend fun isUpdateDismissed(versionName: String, now: Long = System.currentTimeMillis()): Boolean {
        val prefs = context.dataStore.data.first()
        return prefs[updateDismissVersionKey] == versionName &&
            (prefs[updateDismissUntilKey] ?: 0L) > now
    }

    suspend fun dismissUpdate(versionName: String, until: Long) {
        context.dataStore.edit {
            it[updateDismissVersionKey] = versionName
            it[updateDismissUntilKey] = until
        }
    }

    suspend fun resetAll() { context.dataStore.edit { it.clear() } }
}