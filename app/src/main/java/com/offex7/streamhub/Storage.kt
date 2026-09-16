package com.offex7.streamhub

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.dataStore by preferencesDataStore("streamhub_settings")

class SettingsStore(private val context: Context) {
    private val sectionKey = stringPreferencesKey("last_section")
    private val sourceKey = stringPreferencesKey("tv_source_index")
    private val exitKey = longPreferencesKey("last_exit_time")
    private val tvNameKey = stringPreferencesKey("last_tv_name")
    private val tvUrlKey = stringPreferencesKey("last_tv_url")
    private val tvLogoKey = stringPreferencesKey("last_tv_logo")
    private val radioNameKey = stringPreferencesKey("last_radio_name")
    private val radioUrlKey = stringPreferencesKey("last_radio_url")
    private val radioLogoKey = stringPreferencesKey("last_radio_logo")
    private val favoritesKey = stringSetPreferencesKey("favorites")
    private val pipKey = booleanPreferencesKey("pip_on_minimize")

    suspend fun lastSection(): Section? = context.dataStore.data.first()[sectionKey]?.let {
        runCatching { Section.valueOf(it) }.getOrNull()
    }

    suspend fun setSection(section: Section) { context.dataStore.edit { it[sectionKey] = section.name } }

    suspend fun clearSection() { context.dataStore.edit { it.remove(sectionKey) } }

    suspend fun sourceIndex(): Int = context.dataStore.data.first()[sourceKey]?.toIntOrNull() ?: 0

    suspend fun setSourceIndex(index: Int) { context.dataStore.edit { it[sourceKey] = index.toString() } }

    suspend fun lastExitTime(): Long = context.dataStore.data.first()[exitKey] ?: 0L

    suspend fun setLastExitTime(timestamp: Long) { context.dataStore.edit { it[exitKey] = timestamp } }

    suspend fun saveLastStream(section: Section, item: StreamItem) {
        context.dataStore.edit {
            when (section) {
                Section.TV -> {
                    it[tvNameKey] = item.name
                    it[tvUrlKey] = item.url
                    item.logoUrl?.let { value -> it[tvLogoKey] = value } ?: it.remove(tvLogoKey)
                }
                Section.RADIO -> {
                    it[radioNameKey] = item.name
                    it[radioUrlKey] = item.url
                    item.logoUrl?.let { value -> it[radioLogoKey] = value } ?: it.remove(radioLogoKey)
                }
            }
        }
    }

    suspend fun lastStream(section: Section): StreamItem? {
        val prefs = context.dataStore.data.first()
        val name = prefs[when (section) { Section.TV -> tvNameKey; Section.RADIO -> radioNameKey }] ?: return null
        val url = prefs[when (section) { Section.TV -> tvUrlKey; Section.RADIO -> radioUrlKey }] ?: return null
        val logo = prefs[when (section) { Section.TV -> tvLogoKey; Section.RADIO -> radioLogoKey }]
        return StreamItem(name, url, logoUrl = logo)
    }

    suspend fun favorites(): Set<String> = context.dataStore.data.first()[favoritesKey] ?: emptySet()

    suspend fun setFavorite(url: String, value: Boolean) {
        context.dataStore.edit {
            val set = (it[favoritesKey] ?: emptySet()).toMutableSet()
            if (value) set.add(url) else set.remove(url)
            it[favoritesKey] = set
        }
    }

    suspend fun isFavorite(url: String): Boolean = favorites().contains(url)

    suspend fun pipEnabled(): Boolean = context.dataStore.data.first()[pipKey] ?: true

    suspend fun setPipEnabled(enabled: Boolean) { context.dataStore.edit { it[pipKey] = enabled } }
}
