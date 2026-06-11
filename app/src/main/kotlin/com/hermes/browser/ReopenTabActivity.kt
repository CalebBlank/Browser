package com.hermes.browser

import android.app.Activity
import android.app.ActivityManager
import android.content.Intent
import android.os.Bundle
import android.widget.Toast

/**
 * Invisible trampoline used by Deck to reopen a specific browser tab.
 *
 * Cross-app `ActivityManager.moveTaskToFront(taskId)` from Deck is unreliable on Android 14/15
 * (the system intermittently rejects fronting another app's task — the tab stays backgrounded
 * and "nothing happens"). A task can always be fronted by its OWN process, though. Deck — which
 * is in the foreground when the user taps a card, so it's allowed to start activities — launches
 * this activity; it runs in the browser process, brings the target tab task to the front via the
 * browser's own [ActivityManager.AppTask], then finishes. No background-activity-start grant is
 * needed because Deck's foreground start brings this process forward first.
 */
class ReopenTabActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleReopen(intent)
    }

    // singleInstance reuses one task, so a second launch is delivered here, not to onCreate.
    // Without handling it the reopen would silently no-op (and the task would linger).
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleReopen(intent)
    }

    private fun handleReopen(intent: Intent) {
        val taskId = intent.getIntExtra(EXTRA_TASK_ID, -1)
        if (taskId != -1) {
            val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager
            // Front the tab task by id directly. A backgrounded excludeFromRecents document task is
            // NOT returned by getAppTasks() even while it's alive, so we must not gate on that lookup
            // (the old `if (task != null)` guard wrongly reported "tab gone" and never fronted it).
            // moveTaskToFront() fronts the task at the system level regardless; it needs REORDER_TASKS
            // (declared in the manifest). Only a genuine failure (e.g. the task really is gone) is
            // surfaced as a Toast.
            runCatching { am.moveTaskToFront(taskId, 0) }.exceptionOrNull()?.let {
                Toast.makeText(this, "reopen failed: ${it.message}", Toast.LENGTH_LONG).show()
            }
        }
        // Remove our own task so this trampoline never lingers as a shell that Deck might
        // mistake for a tab.
        finishAndRemoveTask()
    }

    companion object {
        const val EXTRA_TASK_ID = "task_id"
        const val ACTION_TAB_GONE = "com.hermes.deck.ACTION_BROWSER_TAB_GONE"
    }
}
