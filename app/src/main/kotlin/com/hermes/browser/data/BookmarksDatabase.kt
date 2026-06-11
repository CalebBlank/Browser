package com.hermes.browser.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.util.concurrent.Executors

class BookmarksDatabase private constructor(context: Context) :
    SQLiteOpenHelper(context, "bookmarks.db", null, 1) {

    // Single background thread for writes so add()/remove() never block the UI thread.
    private val ioExecutor = Executors.newSingleThreadExecutor()

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE bookmarks(url TEXT PRIMARY KEY, title TEXT NOT NULL, added_at INTEGER NOT NULL)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, old: Int, new: Int) {
        db.execSQL("DROP TABLE IF EXISTS bookmarks")
        onCreate(db)
    }

    fun add(url: String, title: String) {
        ioExecutor.execute {
            writableDatabase.execSQL(
                "INSERT OR IGNORE INTO bookmarks(url, title, added_at) VALUES(?,?,?)",
                arrayOf<Any>(url, title.ifBlank { url }, System.currentTimeMillis())
            )
        }
    }

    fun remove(url: String) {
        ioExecutor.execute {
            writableDatabase.execSQL("DELETE FROM bookmarks WHERE url = ?", arrayOf(url))
        }
    }

    fun isBookmarked(url: String): Boolean {
        return readableDatabase.rawQuery(
            "SELECT 1 FROM bookmarks WHERE url = ?", arrayOf(url)
        ).use { it.moveToFirst() }
    }

    fun getAll(): List<Pair<String, String>> {
        return readableDatabase.rawQuery(
            "SELECT url, title FROM bookmarks ORDER BY added_at DESC", null
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(cursor.getString(0) to cursor.getString(1))
            }
        }
    }

    fun search(q: String): List<Pair<String, String>> {
        val like = "%${q.replace("%", "\\%").replace("_", "\\_")}%"
        return readableDatabase.rawQuery(
            "SELECT url, title FROM bookmarks WHERE url LIKE ? ESCAPE '\\' OR title LIKE ? ESCAPE '\\' ORDER BY added_at DESC",
            arrayOf(like, like)
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(cursor.getString(0) to cursor.getString(1))
            }
        }
    }

    fun importAll(items: List<Pair<String, String>>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            for ((url, title) in items) {
                db.execSQL(
                    "INSERT OR IGNORE INTO bookmarks(url, title, added_at) VALUES(?,?,?)",
                    arrayOf<Any>(url, title.ifBlank { url }, System.currentTimeMillis())
                )
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    companion object {
        @Volatile private var instance: BookmarksDatabase? = null
        fun get(ctx: Context): BookmarksDatabase = instance ?: synchronized(this) {
            instance ?: BookmarksDatabase(ctx.applicationContext).also { instance = it }
        }
    }
}
