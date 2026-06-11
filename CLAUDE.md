# Browser — Android Tab-per-Activity Browser

Each browser tab is a separate `Activity` instance launched with `FLAG_ACTIVITY_NEW_DOCUMENT |
FLAG_ACTIVITY_MULTIPLE_TASK`, giving it its own task and thumbnail in system recents. This pairs
 with the Deck launcher, where each tab surfaces as a card.

## Engine

Android system **WebView** (not GeckoView — that was the original plan; the app migrated to
WebView). One `WebView` per `BrowserTabActivity`. State saved/restored via
`WebView.saveState()` / `restoreState()`.

## Architecture

BrowserApp.kt          — empty `Application` subclass
BrowserTabActivity.kt  — one instance per tab; owns a `WebView`; handles the predictive-back
                          gesture (snapshot overlay), and broadcasts tab events to Deck
ReopenTabActivity.kt   — invisible trampoline Deck launches to front a specific tab task (see below)
ui/BrowserScreen.kt    — full-screen Box: WebView underneath, FloatingNavBar overlay at bottom;
                          also the bookmarks/history panel and address-bar/search UI
data/HistoryDatabase.kt, BookmarksDatabase.kt — SQLite singletons (writes on a background thread)
HistoryProvider.kt     — `ContentProvider` exposing history to Deck (signature-permission gated)
ui/Theme.kt            — Material 3 dynamic color, fallback dark/light

## Key Design Decisions

- **Tab = Activity task** — `documentLaunchMode="always"` + `autoRemoveFromRecents="false"` in the
  manifest, and `FLAG_ACTIVITY_NEW_DOCUMENT | FLAG_ACTIVITY_MULTIPLE_TASK` on new-tab intents. Never
  manage tabs in a tab tray. (`autoRemoveFromRecents="false"` is required — document activities
  default it to `true`, which made tabs self-destruct when backgrounded.)
- **Tabs are NORMAL recents tasks, NOT `excludeFromRecents` (changed 2026-06-04).** The original
  design excluded tabs from system recents (Deck-only). But the system *reaps* backgrounded
  excludeFromRecents document tasks — after ~2 tabs, a backgrounded tab's task was destroyed
  (measured: nexuslauncher `TasksRepository: removeTasks`), so its Deck card held a dead taskId and
  `moveTaskToFront` silently no-op'd ("tab won't reopen"). A normal recents task record persists
  across process death (the empty-shell `restoreState()`/`loadUrl` path handles re-entry), and
  `getAppTasks()` reliably enumerates them. Cost: tabs also show in the system recents UI. Deck keeps
  the two surfaces in sync (enumerate-on-resume reconcile + `CloseTabReceiver`).
- **One WebView per activity** — created in `onCreate`. On (re)create, `restoreState()`; if it
  returns null (emptied task shell), fall back to `loadUrl(intent.dataString)`.
- **Floating nav bar** (`FloatingNavBar` in `BrowserScreen.kt`) pinned to `BottomCenter`: address
  field (`BasicTextField`), menu, and a drag-up bookmarks/history panel.
- **Predictive back** uses a captured snapshot bitmap overlay; `onPageCommitVisible` + a timeout
  clear the overlay so bfcache restores don't leave a grey screen.

## Deck Integration (built — via broadcasts, not the plugin ContentProvider)

The browser broadcasts to `com.hermes.deck` (its `BrowserTabReceiver`):
- `ACTION_BROWSER_TAB_OPENED` (on fresh `onCreate`) with `task_id` + `parent_package` (from
  `Activity.getReferrer()`, so Deck stacks the tab with the app it was launched from).
- `ACTION_BROWSER_TAB_FOCUSED` (on `onResume`) and `ACTION_BROWSER_PAGE_LOADED` (drives screenshot
  capture keyed `com.hermes.browser:<taskId>`).
- `ACTION_BROWSER_TAB_GONE` (sent by `ReopenTabActivity` when a tab task no longer exists) so Deck
  drops the dead card.

Deck → Browser receivers (keep Deck's tab cards in sync with the live tab tasks / system recents):
- `EnumerateTabsReceiver` (`ACTION_ENUMERATE_TABS`) — replies `ACTION_BROWSER_TABS_LIST` with every
  live tab's `task_id` + `url` (via `getAppTasks()`). Deck calls this on resume and reconciles:
  drops cards whose task is gone, adds tabs it didn't know about.
- `CloseTabReceiver` (`ACTION_CLOSE_TAB` + `task_id`) — finds that tab's `AppTask` and
  `finishAndRemoveTask()`s it, so a card swiped away in Deck also leaves system recents.

Deck reopens a tab by starting `ReopenTabActivity` (Deck is foreground, so it may start it); that
trampoline runs in the browser process and calls `moveTaskToFront(taskId)` on the tab's own task —
reliable where cross-app `moveTaskToFront` is rejected on Android 14/15. `HistoryProvider` (a
real `ContentProvider`) is the one piece Deck reads directly, for history search.

## What's Not Yet Built

- `setTaskDescription()` per tab (page title + favicon in recents)
- Address bar select-all on focus, domain-only display while unfocused
- Deck ContentProvider plugin
- Permission delegates (location, camera, microphone)

## Build

- AGP 8.4.0, Kotlin 2.0.0, Compose BOM 2024.06.00
- `compileSdk 35`, `minSdk 26`, `targetSdk 35`
- No Hilt — no ViewModels yet, state lives in the Activity and composables
- Target ABI: arm64-v8a only (Clicks Communicator)
