package com.hermes.browser

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Deck asks the browser to CLOSE a specific tab (its card was swiped away in Deck) so the tab also
 * leaves the system recents — keeping Deck and native recents showing the same set of tab cards.
 *
 * A tab task can only be finished by its OWN process, and one app can't finish another's task. So
 * Deck broadcasts [ACTION_CLOSE_TAB] with the task id; this receiver runs in the browser process,
 * finds that [BrowserTabActivity] task via [ActivityManager.AppTask] and calls finishAndRemoveTask().
 * (The broadcast cold-starts the process if it was killed; getAppTasks() still sees the surviving
 * recents task records.)
 */
class CloseTabReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_CLOSE_TAB) return
        val taskId = intent.getIntExtra(EXTRA_TASK_ID, -1)
        if (taskId == -1) return
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return
        val tabActivity = BrowserTabActivity::class.java.name

        var closed = false
        runCatching { am.appTasks }.getOrNull().orEmpty().forEach { task ->
            val info = runCatching { task.taskInfo }.getOrNull() ?: return@forEach
            val component = info.baseIntent.component ?: info.topActivity
            if (info.taskId == taskId && component?.className == tabActivity) {
                runCatching { task.finishAndRemoveTask() }
                closed = true
            }
        }
        Log.d("CloseTab", "close taskId=$taskId found=$closed")
    }

    companion object {
        const val ACTION_CLOSE_TAB = "com.hermes.browser.ACTION_CLOSE_TAB"
        const val EXTRA_TASK_ID = "task_id"
    }
}
