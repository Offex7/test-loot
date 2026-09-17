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
    private val exitKey = longPreferencesKey("last_exit_time")
    private val tvNameKey = stringPreferencesKey("last_tv_name")
    private val tvUrlKey = stringPreferencesKey("last_tv_url")
    private val tvLogoKey = stringPreferencesKey("last_tv_logo")
    private val radioNameKey = stringPreferencesKey("last_radio_name")
    private val radioUrlKey = stringPreferencesKey("last_radio_url")
    private val radioLogoKey = stringPreferencesKey("last_radio_logo")
    private val tvFavoritesKey = stringSetPreferencesKey("favorites_tv")
    private val radioFavoritesKey = stringSetPreferencesKey("favorites_radio")
    private val pipKey = booleanPreferencesKey("pip_on_minimize")
    private val tvUsageKey = longPreferencesKey("usage_tv_seconds")
    private val radioUsageKey = longPreferencesKey("usage_radio_seconds")
    private val tvScrollIndexKey = intPreferencesKey("tv_scroll_index")
    private val tvScrollOffsetKey = intPreferencesKey("tv_scroll_offset")
    private val radioScrollIndexKey = intPreferencesKey("radio_scroll_index")
    private val radioScrollOffsetKey = intPreferencesKey("radio_scroll_offset")

    suspend fun lastSection(): Section? = context.dataStore.data.first()[sectionKey]?.let { runCatching { Section.valueOf(it) }.getOrNull() }
    suspend fun setSection(section: Section) { context.dataStore.edit { it[sectionKey] = section.name } }
    suspend fun clearSection() { context.dataStore.edit { it.remove(sectionKey) } }

    suspend fun sourceIndex(): Int = context.dataStore.data.first()[sourceKey] ?: 0
    suspend fun setSourceIndex(index: Int) { context.dataStore.edit { it[sourceKey] = index.coerceIn(0, TV_SOURCES.lastIndex) } }

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

    suspend fun favorites(section: Section): Set<String> = context.dataStore.data.first()[if (section == Section.TV) tvFavoritesKey else radioFavoritesKey] ?: emptySet()

    suspend fun setFavorite(section: Section, channelId: String, value: Boolean) {
        context.dataStore.edit {
            val key = if (section == Section.TV) tvFavoritesKey else radioFavoritesKey
            val set = (it[key] ?: emptySet()).toMutableSet()
            if (value) set.add(channelId) else set.remove(channelId)
            it[key] = set
        }
    }

    fun favoritesFlow(section: Section): Flow<Set<String>> = context.dataStore.data.map { it[if (section == Section.TV) tvFavoritesKey else radioFavoritesKey] ?: emptySet() }

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
    suspend fun resetUsage(section: Section) { context.dataStore.edit { it[if (section == Section.TV) tvUsageKey else radioUsageKey] = 0L } }

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

    suspend fun resetAll() { context.dataStore.edit { it.clear() } }
}
