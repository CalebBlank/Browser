package com.hermes.browser

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Deck can't see the browser's tab tasks (they're excludeFromRecents Activity tasks, and one app
 * can't query another's tasks). So Deck broadcasts [ACTION_ENUMERATE_TABS] and this receiver, which
 * runs in the browser process, replies with the task ids of every live tab.
 *
 * Only [BrowserTabActivity] tasks count — the trampoline (ReopenTabActivity) and any stray tasks
 * are filtered out by component name. Deck reconciles its persisted tab cards against this list:
 * dropping cards whose task is gone and adding cards for tabs it didn't know about.
 */
class EnumerateTabsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_ENUMERATE_TABS) return
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return
        val tabActivity = BrowserTabActivity::class.java.name

        val tasks = runCatching { am.appTasks }.getOrNull().orEmpty().mapNotNull { task ->
            val info = runCatching { task.taskInfo }.getOrNull() ?: return@mapNotNull null
            // baseIntent is the most reliable component for a document-launch task; fall back to
            // topActivity when available.
            val component = info.baseIntent.component ?: info.topActivity
            if (component?.className == tabActivity) {
                Pair(info.taskId, info.baseIntent.dataString ?: "")
            } else null
        }

        val taskIds = tasks.map { it.first }.toIntArray()
        val urls = tasks.map { it.second }.toTypedArray()

        // Diagnostic: appTasks completeness in a possibly cold-started process (the process is
        // usually dead after a tab is backgrounded; this broadcast spawns it). If this logs fewer
        // tab tasks than actually survive in `dumpsys recents`, the reconcile drop-half is unsafe.
        val rawCount = runCatching { am.appTasks }.getOrNull()?.size ?: -1
        Log.d("EnumTabs", "appTasks raw=$rawCount tabTaskIds=${taskIds.toList()}")

        context.sendBroadcast(
            Intent(ACTION_TABS_LIST).apply {
                setPackage("com.hermes.deck")
                putExtra(EXTRA_TASK_IDS, taskIds)
                putExtra(EXTRA_TASK_URLS, urls)
            }
        )
    }

    companion object {
        const val ACTION_ENUMERATE_TABS = "com.hermes.browser.ACTION_ENUMERATE_TABS"
        const val ACTION_TABS_LIST = "com.hermes.deck.ACTION_BROWSER_TABS_LIST"
        const val EXTRA_TASK_IDS = "task_ids"
        const val EXTRA_TASK_URLS = "task_urls"
    }
}
