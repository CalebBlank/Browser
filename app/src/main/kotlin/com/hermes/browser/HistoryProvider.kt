package com.hermes.browser

import android.content.ContentProvider
import android.content.ContentValues
import android.database.MatrixCursor
import android.net.Uri
import com.hermes.browser.data.HistoryDatabase

class HistoryProvider : ContentProvider() {
    override fun onCreate() = true

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): android.database.Cursor? {
        val q = uri.getQueryParameter("q")?.takeIf { it.length >= 2 } ?: return null
        val rows = HistoryDatabase.get(context!!).search(q)
        val cursor = MatrixCursor(arrayOf("url", "title"))
        rows.forEach { (url, title) -> cursor.addRow(arrayOf(url, title)) }
        return cursor
    }

    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, s: String?, a: Array<String>?) = 0
    override fun update(uri: Uri, v: ContentValues?, s: String?, a: Array<String>?) = 0
}
