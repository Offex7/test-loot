package com.offex7.streamhub

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/**
 * Same-app bridge from the isolated Android Auto module to DataStore-backed
 * radio and source-scoped TV favorites. TV rows are served from the existing
 * local playlist cache; this provider never starts a network playlist fetch.
 */
class CarFavoritesProvider : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        val path = uri.path ?: return null
        if (path != "/favorites/radio" && path != "/favorites/tv") return null
        val appContext = context?.applicationContext ?: return null
        val rows = runCatching {
            runBlocking(Dispatchers.IO) {
                val store = SettingsStore(appContext)
                when (path) {
                    "/favorites/radio" -> {
                        val favoriteIds = store.favorites(Section.RADIO)
                        RADIO_STATIONS
                            .filter { it.key in favoriteIds }
                            .map { Triple(it.key, it.name, it.url) }
                    }
                    "/favorites/tv" -> {
                        val sourceKey = store.activeSourceKey()
                        val favoriteIds = store.favorites(Section.TV, sourceKey)
                        val playlist = TvPlaylistRepositoryV8(appContext, store).cached(sourceKey)
                        playlist?.items.orEmpty()
                            .filter { it.key in favoriteIds }
                            .map { Triple(it.key, it.name, it.url) }
                    }
                    else -> emptyList()
                }
            }
        }.getOrElse { error ->
            LogExporter.log("Android Auto favorites query failed: " + (error.message ?: error.javaClass.simpleName))
            emptyList()
        }

        return MatrixCursor(arrayOf("media_id", "title", "media_url")).apply {
            rows.forEach { (mediaId, title, mediaUrl) ->
                addRow(arrayOf(mediaId, title, mediaUrl))
            }
        }
    }

    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = 0
}
