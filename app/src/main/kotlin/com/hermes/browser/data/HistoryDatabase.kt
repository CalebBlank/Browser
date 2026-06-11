package com.hermes.browser.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.util.concurrent.Executors

class HistoryDatabase private constructor(context: Context) :
    SQLiteOpenHelper(context, "history.db", null, 1) {

    // Writes go through a single background thread so they never block the UI thread (record()
    // is called from WebViewClient.onPageFinished on every page load). SQLite serializes writes.
    private val ioExecutor = Executors.newSingleThreadExecutor()

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE history(url TEXT PRIMARY KEY, title TEXT NOT NULL, visited_at INTEGER NOT NULL)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, old: Int, new: Int) {
        db.execSQL("DROP TABLE IF EXISTS history")
        onCreate(db)
    }

    fun record(url: String, title: String) {
        ioExecutor.execute {
            writableDatabase.execSQL(
                "INSERT OR REPLACE INTO history(url, title, visited_at) VALUES(?,?,?)",
                arrayOf<Any>(url, title.ifBlank { url }, System.currentTimeMillis())
            )
        }
    }

    fun search(q: String, limit: Int = 8): List<Pair<String, String>> {
        val like = "%${q.replace("%", "\\%").replace("_", "\\_")}%"
        return readableDatabase.rawQuery(
            "SELECT url, title FROM history WHERE url LIKE ? ESCAPE '\\' OR title LIKE ? ESCAPE '\\' ORDER BY visited_at DESC LIMIT ?",
            arrayOf(like, like, "$limit")
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(cursor.getString(0) to cursor.getString(1))
            }
        }
    }

    companion object {
        @Volatile private var instance: HistoryDatabase? = null
        fun get(ctx: Context): HistoryDatabase = instance ?: synchronized(this) {
            instance ?: HistoryDatabase(ctx.applicationContext).also { instance = it }
        }
    }
}
