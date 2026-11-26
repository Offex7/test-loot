package com.offex7.streamhub

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/**
 * Private, same-app bridge so the isolated Android Auto library can read the app's
 * DataStore-backed radio favourites without creating a second DataStore instance.
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
        if (uri.path != "/favorites/radio") return null
        val appContext = context?.applicationContext ?: return null
        val favorites = runBlocking(Dispatchers.IO) {
            SettingsStore(appContext).favorites(Section.RADIO)
        }
        return MatrixCursor(arrayOf("media_id")).apply {
            favorites.sorted().forEach { addRow(arrayOf(it)) }
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
