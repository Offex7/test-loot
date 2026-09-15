package com.offex7.streamhub

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.dataStore by preferencesDataStore("streamhub_settings")
class SettingsStore(private val context: Context) {
 private val sectionKey = stringPreferencesKey("last_section")
 private val sourceKey = intPreferencesKey("tv_source_index")
 suspend fun lastSection(): Section? = context.dataStore.data.first()[sectionKey]?.let { runCatching { Section.valueOf(it) }.getOrNull() }
 suspend fun setSection(section: Section) { context.dataStore.edit { it[sectionKey] = section.name } }
 suspend fun clearSection() { context.dataStore.edit { it.remove(sectionKey) } }
 suspend fun sourceIndex(): Int = context.dataStore.data.first()[sourceKey] ?: 0
 suspend fun setSourceIndex(index: Int) { context.dataStore.edit { it[sourceKey] = index } }
}
