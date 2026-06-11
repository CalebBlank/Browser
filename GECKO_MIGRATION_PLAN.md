# Browser Engine Migration — WebView → GeckoView

Status: **planned** (spike proven 2026-06-11). Do the work on a **copy**, `git init` it, commit per phase.

## Why
- **OAuth is unfixable on WebView.** The app already strips the `; wv` UA token (`BrowserTabActivity.kt:193`) and Google still blocks sign-in. Spike result: a real **Google sign-in with 2FA** completed in GeckoView on the phone.
- **Persistence ports cleanly.** Spike proved `GeckoSession.SessionState` survives real process death via disk (emulator: force-stop → new PID → page restored + rendered).
- **The novel core is engine-agnostic.** Tab = recents Activity task, the Deck broadcast sync (`ACTION_BROWSER_TAB_OPENED/_FOCUSED/_PAGE_LOADED`, `EnumerateTabsReceiver`, `CloseTabReceiver`), and the `ReopenTabActivity` trampoline are about Android tasks, not the web engine. They **do not change.** The migration is contained to the engine-integration layer.

## Ground rules
- Work on a **copy** (`Browser-Gecko/`). `git init` + commit per phase (the project has no VCS today — no safety net otherwise).
- Toolchain unchanged: AGP 8.9.1, Kotlin 2.3.20, Gradle 8.13, compileSdk 36, minSdk 26, targetSdk 35. (CLAUDE.md is stale — fix it at cutover.)
- Dep: `org.mozilla.geckoview:geckoview:151.x` from `https://maven.mozilla.org/maven2`.
- **DROP the UA hacks** — both the `; wv` strip (`BrowserTabActivity:193`) and the hardcoded Chrome desktop UA (`BrowserScreen:649`). Gecko's default Firefox UA is *what won OAuth*; spoofing Chrome risks re-triggering the block. Desktop mode = `GeckoSessionSettings.userAgentMode = DESKTOP` (+ viewportMode), not a UA string.
- APK: strip symbols + per-ABI splits. The spike's 185 MB (unstripped, single-ABI) is NOT the target.

## Decision points (resolve before/at Phase 5 — they shrink the work)
1. **Theming convergence → drop the WebExtension from the migration.** Today the status-bar tint comes from page background color via `evaluateJavascript` (`BrowserScreen:331`) — the *only* engine-dependent JS hook, and the one thing that would force a WebExtension. Feature #2 wants **favicon-color** theming (like Reverb), which is **engine-independent** (the app already fetches `favicon.ico` → can Palette it). **Recommended: replace page-color tint with favicon-color.** Then no WebExtension is needed until feature #4. (Keep page-color only if you specifically prefer it — that path needs a WebExtension and a check that Gecko even surfaces theme-color.)
2. **`HermesBridge` → delete.** The injected JS scroll-bridge (`addJavascriptInterface`, `BrowserScreen:395`) is redundant with the native `setOnScrollChangeListener` (`:399`) doing the same toolbar hide/show. `ScrollDelegate.onScrollChanged` replaces the native listener; the bridge is dropped.
3. **`__hermes_spacer` JS injection (`BrowserScreen:298`) → drop.** It offsets page content under the transparent status bar. Handle the status bar with native layout/inset instead (position GeckoView below the bar, or a scrim) — no per-page JS.

## Phases

### Phase 0 — Setup
- Copy `Browser` → `Browser-Gecko`; `git init`; initial commit.
- Add GeckoView dep + `maven.mozilla.org` repo (`settings.gradle.kts`).
- Shared `GeckoRuntime` singleton in `BrowserApp` (one runtime/process; every tab Activity shares it).
- Configure ABI splits.

### Phase 1 — Core swap: a tab that loads + persists  ← **re-verify on device**
- `BrowserTabActivity`: `webView: WebView` → `geckoView: GeckoView` + `session: GeckoSession` (opened on the shared runtime), as the bottom layer of the existing `root` FrameLayout (ComposeView stays on top).
- **SessionState persistence (productionized spike pattern):** cache via `ProgressDelegate.onSessionStateChange`; in `onStop` write to **disk keyed by `taskId`** (`filesDir/sessions/<taskId>`) *and* mirror into `onSaveInstanceState`. Cold-start restore order: bundle → disk(taskId) → `loadUri(intent.dataString)` → default. The disk path is what survives a reaped document task.
- Basic nav: `session.loadUri`, `goBack/goForward`, reload/stop; back/forward enabled-state from `NavigationDelegate.onCanGoBack/onCanGoForward` (Gecko has no synchronous `canGoBack`).
- Deck broadcasts + insets/edge-to-edge: unchanged.
- **Gate (real app, phone):** load; open 2–3 tabs (Deck cards appear); background + `am force-stop` a tab → reopen restores; complete a **Google sign-in**. Commit.

### Phase 2 — Delegate parity (the WebViewClient/ChromeClient surface)
- `ProgressDelegate`: onPageStart/onPageStop (isLoading, page-loaded broadcast, back-overlay clear), onProgressChange.
- `NavigationDelegate`: onLocationChange (url/addressbar/title); onLoadRequest (ALLOW; external schemes); **onNewSession → new tab**: launch `BrowserTabActivity` (`NEW_DOCUMENT|MULTIPLE_TASK`) with the URL, return null — **deletes** the `onCreateWindow` helper-WebView hack (`:360`).
- `ContentDelegate`: onTitleChange; **onExternalResponse → downloads** (the `WebResponse` carries the authed body stream — **drop the `CookieManager` dance**, `:482`); onContextMenu → long-press link/image menu (replaces `hitTestResult`, `:449`); onCrash → recover.
- `ScrollDelegate.onScrollChanged` → toolbar hide/show (replaces native scroll listener; HermesBridge gone).
- `HistoryDelegate.onVisited` → `HistoryDatabase.record` (replaces the onPageFinished write).
- JS toggle → `GeckoSessionSettings.allowJavascript` (runtime-mutable). Desktop toggle → `userAgentMode=DESKTOP`.

### Phase 3 — Predictive-back snapshot
- Replace `findSurfaceViewIn`/`PixelCopy`/`webView.draw()` with `GeckoView.capturePixels()` (async `GeckoResult<Bitmap>`).
- Keep the proactive capture-after-load pattern (capture on onPageStop into `currentPageShot`) so the bitmap is ready when the gesture starts. All the overlay/animation logic in `BrowserTabActivity` is unchanged.

### Phase 4 — Find-in-page, favicon, recents
- Find-in-page → `session.finder` / `SessionFinder.find` (replaces `findAllAsync/findNext/setFindListener`).
- Favicon via the existing `favicon.ico` fetch (engine-independent) — feeds theming + `setTaskDescription` per tab (title+favicon in recents; was backlog).

### Phase 5 — Theming = feature #2
- Settings toggle: **System/dynamic M3** OR **favicon-color of current site** (Reverb-style: favicon → Palette → accent → status bar + UI tint).
- Removes the page-bg-color `evaluateJavascript` path entirely (decision #1).

### Phase 6 — Parity test + cutover
- Phone matrix: load, multi-tab Deck cards + reconcile, reap+restore, predictive back, downloads, find, long-press menu, new-tab/multi-window, **OAuth re-confirm**, default-browser role, desktop mode, JS toggle.
- At parity: replace the original `Browser` with `Browser-Gecko`; fix `CLAUDE.md` (engine + toolchain).
- Then the remaining features: **#1 pull-to-refresh** (swipe at scrollY==0 → reload, using `ScrollDelegate` position) and **#4 custom CSS/JS + AI** — *now* the WebExtension appears (bundled extension + content-script injection; Claude API per `project_deck_claude_provider`).

## Deletions (net simplifications)
`onCreateWindow` helper-WebView hack · `HermesBridge` JS bridge · download `CookieManager` dance · UA hacks (`; wv` strip + Chrome desktop UA) · `__hermes_spacer` injection.

## Risks / verify-first
- Gecko surfacing page/theme-color via a delegate is **uncertain** — moot if we converge to favicon-color (recommended).
- `GeckoRuntime` is one-per-process shared by many document-task activities — fine (one process), but watch memory with several live tabs.
- `capturePixels()` timing for the instant back snapshot.
- APK size — strip + ABI splits to land ~60–90 MB/ABI.
