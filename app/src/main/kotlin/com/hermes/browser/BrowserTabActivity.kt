package com.hermes.browser

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.role.RoleManager
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.activity.BackEventCompat
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.hermes.browser.ui.BrowserScreen
import com.hermes.browser.ui.BrowserTheme
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView
import java.io.File

/**
 * One tab = one Activity task (see CLAUDE.md). Engine = a [GeckoSession] in a [GeckoView].
 * Tab state persists via [GeckoSession.SessionState] (cached through onSessionStateChange, written to
 * disk in onStop, keyed by [taskId]). Predictive back uses a snapshot bitmap captured via
 * [GeckoView.capturePixels] (Phase 3 — replaces WebView's PixelCopy/draw). BrowserScreen owns the
 * session delegates and feeds back the latest SessionState + can-go-back flag.
 */
class BrowserTabActivity : ComponentActivity() {
    private lateinit var session: GeckoSession
    private lateinit var geckoView: GeckoView
    private lateinit var root: FrameLayout
    private lateinit var swipeRefresh: SwipeRefreshLayout
    // Latest page scroll offset, fed by BrowserScreen's ScrollDelegate; pull-to-refresh is only
    // allowed at the very top (scrollY == 0).
    private var pageScrollY = 0
    private var statusBarBgView: View? = null
    private var statusBarHeightPx by mutableStateOf(0)
    private var statusBarTransparent = true

    // Fed by BrowserScreen's delegates.
    private var latestState: GeckoSession.SessionState? = null
    private var canGoBack = false
    private var isForeground = false
    private var pageHasLoaded = false

    // Phase 5 favicon-color theming: the favicon's dominant color seeds the whole M3 scheme
    // (search bar, popup menu, panels all tint). Null until a favicon is decoded → system/dynamic.
    private var seedColor by mutableStateOf<Int?>(null)
    private val accentCache = java.util.concurrent.ConcurrentHashMap<String, Int>()
    // The domain currently shown; guards against a slow favicon fetch from an earlier navigation
    // applying its color after the user has already moved on to a different site.
    private var currentDomain: String? = null

    // Predictive-back snapshot animation.
    private var backSnapshotBitmap by mutableStateOf<android.graphics.Bitmap?>(null)
    private var backAnimProgress by mutableStateOf(0f)
    private var backSwipeEdge by mutableStateOf(0)
    private var backCommitAnimator: android.animation.ValueAnimator? = null
    private var showBackOverlay by mutableStateOf(false)
    private var backGestureSeq = 0
    private var currentPageShot: android.graphics.Bitmap? = null
    private var prevPageShot by mutableStateOf<android.graphics.Bitmap?>(null)
    private var isNavigatingBack = false

    private fun sessionFile(): File = File(filesDir, "session_$taskId.json")
    private fun urlFile(): File = File(filesDir, "url_$taskId.txt")

    // Async snapshot of the live page via GeckoView's compositor. onDone(null) on any failure
    // (e.g. compositor not ready) so the back gesture still works, just without the slide.
    private fun captureGeckoViewAsync(onDone: (android.graphics.Bitmap?) -> Unit) {
        runCatching {
            geckoView.capturePixels().accept({ bmp -> onDone(bmp) }, { onDone(null) })
        }.onFailure { onDone(null) }
    }

    // Ask Deck to (re)capture this tab's screenshot — but ONLY while it's actually foreground (Deck
    // screenshots whatever is foreground, so firing this when backgrounded captures the home screen),
    // and after a beat so GeckoView's async compositor has painted.
    private fun scheduleDeckCapture(delayMs: Long) {
        root.postDelayed({
            if (isForeground) {
                sendBroadcast(
                    Intent("com.hermes.deck.ACTION_BROWSER_PAGE_LOADED").apply {
                        setPackage("com.hermes.deck")
                        putExtra("task_id", taskId)
                    }
                )
                // Page is painted now: sample its top color for the status-bar tint (page-background
                // color, like the old WebView build) and refresh the back-gesture snapshot.
                captureGeckoViewAsync { bmp ->
                    bmp?.let {
                        currentPageShot = it
                        updateStatusBarColor(sampleTopColor(it), statusBarTransparent)
                    }
                }
            }
        }, delayMs)
    }

    // Average a thin horizontal strip near the top of the page bitmap = the color behind the status bar.
    private fun sampleTopColor(bmp: android.graphics.Bitmap): Int {
        val w = bmp.width; val h = bmp.height
        if (w <= 0 || h <= 0) return Color.WHITE
        val y = (h * 0.02f).toInt().coerceIn(0, h - 1)
        val x0 = (w * 0.25f).toInt(); val x1 = (w * 0.75f).toInt()
        var r = 0L; var g = 0L; var b = 0L; var n = 0
        var x = x0
        while (x < x1) {
            val p = bmp.getPixel(x, y)
            r += Color.red(p); g += Color.green(p); b += Color.blue(p); n++
            x += 6
        }
        return if (n == 0) Color.WHITE else Color.rgb((r / n).toInt(), (g / n).toInt(), (b / n).toInt())
    }

    private val requestBrowserRole = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { /* system handles the result */ }

    private fun requestDefaultBrowser() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            if (roleManager.isRoleAvailable(RoleManager.ROLE_BROWSER) &&
                !roleManager.isRoleHeld(RoleManager.ROLE_BROWSER)
            ) {
                requestBrowserRole.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_BROWSER))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackStarted(backEvent: BackEventCompat) {
                backCommitAnimator?.cancel()
                backCommitAnimator = null
                if (!canGoBack) return
                backGestureSeq++
                backSwipeEdge = backEvent.swipeEdge
                backAnimProgress = 0f
                val cached = currentPageShot
                if (cached != null) {
                    backSnapshotBitmap = cached
                } else {
                    val seq = backGestureSeq
                    captureGeckoViewAsync { bmp -> if (seq == backGestureSeq) backSnapshotBitmap = bmp }
                }
            }

            override fun handleOnBackProgressed(backEvent: BackEventCompat) {
                if (backSnapshotBitmap == null) return
                backAnimProgress = backEvent.progress
                backSwipeEdge = backEvent.swipeEdge
            }

            override fun handleOnBackCancelled() {
                backGestureSeq++
                android.animation.ValueAnimator.ofFloat(backAnimProgress, 0f).apply {
                    duration = 280
                    interpolator = android.view.animation.DecelerateInterpolator()
                    addUpdateListener { backAnimProgress = it.animatedValue as Float }
                    addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            backSnapshotBitmap = null
                            backAnimProgress = 0f
                            showBackOverlay = false
                        }
                    })
                    start()
                }
            }

            override fun handleOnBackPressed() {
                backGestureSeq++
                isNavigatingBack = true
                showBackOverlay = true // hide the page while the previous one loads after goBack()
                val seq = backGestureSeq
                if (canGoBack) {
                    session.goBack()
                } else {
                    isNavigatingBack = false
                    showBackOverlay = false
                    moveTaskToBack(true)
                }
                // Safety net: a bfcache restore may fire no paint callback to clear the overlay.
                root.postDelayed({
                    if (seq == backGestureSeq && showBackOverlay) showBackOverlay = false
                }, 450)
                val fromProgress = backAnimProgress
                backCommitAnimator = android.animation.ValueAnimator.ofFloat(fromProgress, 2.0f).apply {
                    duration = 180
                    interpolator = android.view.animation.AccelerateInterpolator()
                    addUpdateListener { backAnimProgress = it.animatedValue as Float }
                    addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            backSnapshotBitmap = null
                            backAnimProgress = 0f
                            backCommitAnimator = null
                        }
                    })
                    start()
                }
            }
        })

        session = GeckoSession()
        session.open(BrowserApp.runtime(this))

        geckoView = GeckoView(this)
        geckoView.setSession(session)

        // Pull-to-refresh: wrap the GeckoView. The pull is only honored at the top of the page —
        // setOnChildScrollUpCallback returns "can still scroll up" (true) whenever scrollY > 0, which
        // tells SwipeRefreshLayout NOT to start the gesture mid-page.
        swipeRefresh = SwipeRefreshLayout(this).apply {
            addView(geckoView, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
            setOnChildScrollUpCallback { _, _ -> pageScrollY > 0 }
            setOnRefreshListener { session.reload() }
        }

        val statusBarBg = View(this).also { statusBarBgView = it }

        statusBarTransparent = getSharedPreferences("browser_prefs", MODE_PRIVATE)
            .getBoolean("status_bar_transparent", true)

        val composeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                BrowserTheme(seedColor = seedColor) {
                    BrowserScreen(
                        session = session,
                        statusBarHeightPx = statusBarHeightPx,
                        onNewTab = { url ->
                            startActivity(
                                Intent(this@BrowserTabActivity, BrowserTabActivity::class.java).apply {
                                    data = Uri.parse(url)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                                }
                            )
                        },
                        onPageBackgroundColor = { color, transparent -> updateStatusBarColor(color, transparent) },
                        onSetDefaultBrowser = { requestDefaultBrowser() },
                        onStatusBarTransparentChanged = { newValue ->
                            statusBarTransparent = newValue
                            applyGeckoViewMargin()
                        },
                        backSnapshot = backSnapshotBitmap,
                        backProgress = backAnimProgress,
                        backSwipeEdge = backSwipeEdge,
                        prevPageShot = prevPageShot,
                        showBackOverlay = showBackOverlay,
                        onBackOverlayDismiss = {
                            showBackOverlay = false
                            prevPageShot = null
                        },
                        onPageLoaded = {
                            pageHasLoaded = true
                            swipeRefresh.isRefreshing = false
                            // Capture this page so it's ready as the snapshot for the next back gesture.
                            captureGeckoViewAsync { bmp -> currentPageShot = bmp }
                            scheduleDeckCapture(350)
                        },
                        onNavigatingAway = {
                            // Save the current page as the "previous" preview for the next back gesture.
                            // Skip on back navigations — prevPageShot must persist through those.
                            if (!isNavigatingBack) prevPageShot = currentPageShot
                            isNavigatingBack = false
                            currentPageShot = null
                        },
                        onSessionStateChanged = { latestState = it },
                        onCanGoBackChanged = { canGoBack = it },
                        onPageInfo = { url, title ->
                            updateTaskDescription(url, title)
                            // Persist the current URL immediately (no throttle) so a tab reopened
                            // before onSessionStateChange fires still lands on its page, not the default.
                            if (url.startsWith("http")) runCatching { urlFile().writeText(url) }
                        },
                        onPageScroll = { pageScrollY = it }
                    )
                }
            }
        }

        root = FrameLayout(this)
        root.addView(swipeRefresh, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))

        val sbParams = FrameLayout.LayoutParams(MATCH_PARENT, 0).apply { gravity = Gravity.TOP }
        root.addView(statusBarBg, sbParams)

        root.addView(composeView, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))

        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            statusBarHeightPx = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            (statusBarBg.layoutParams as FrameLayout.LayoutParams).height = statusBarHeightPx
            statusBarBg.requestLayout()
            applyGeckoViewMargin()
            insets
        }

        setContentView(root)

        if (savedInstanceState == null) {
            val parentPkg = referrer?.takeIf { it.scheme == "android-app" }?.host
            sendBroadcast(
                Intent("com.hermes.deck.ACTION_BROWSER_TAB_OPENED").apply {
                    setPackage("com.hermes.deck")
                    putExtra("task_id", taskId)
                    if (parentPkg != null) putExtra("parent_package", parentPkg)
                }
            )
        }

        // Defer until after the first compose frame so BrowserScreen's delegates are installed
        // before onPageStart fires. Restore from bundle, else the per-task disk file, else load.
        root.post {
            val stateStr = savedInstanceState?.getString(KEY_SESSION_STATE)
                ?: runCatching { sessionFile().takeIf { it.exists() }?.readText() }.getOrNull()
            val restored = stateStr?.let { runCatching { GeckoSession.SessionState.fromString(it) }.getOrNull() }
            android.util.Log.i("BTAB", "restore task=$taskId fromState=${restored != null} urlFile=${urlFile().exists()} intentData=${intent.dataString}")
            if (restored != null) {
                session.restoreState(restored)
            } else {
                // No SessionState (page backgrounded before GeckoView's throttled onSessionStateChange
                // fired). Fall back to the last URL persisted for this task so the tab reopens to its
                // page, not the default.
                val lastUrl = runCatching { urlFile().takeIf { it.exists() }?.readText() }.getOrNull()
                    ?.takeIf { it.startsWith("http") }
                session.loadUri(lastUrl ?: intent.dataString ?: "https://start.duckduckgo.com")
            }
        }
    }

    private fun applyGeckoViewMargin() {
        val margin = if (statusBarTransparent) 0 else statusBarHeightPx
        // The GeckoView is now wrapped by swipeRefresh; push the wrapper so both move together.
        (swipeRefresh.layoutParams as? FrameLayout.LayoutParams)?.let { params ->
            params.topMargin = margin
            swipeRefresh.requestLayout()
        }
    }

    private fun updateStatusBarColor(color: Int, transparent: Boolean) {
        if (transparent) {
            val transparentColor = color and 0x00FFFFFF
            statusBarBgView?.background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(color, transparentColor)
            )
        } else {
            statusBarBgView?.setBackgroundColor(color)
        }
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = isLightColor(color)
    }

    private fun isLightColor(color: Int): Boolean {
        val r = Color.red(color); val g = Color.green(color); val b = Color.blue(color)
        return (0.299 * r + 0.587 * g + 0.114 * b) / 255 > 0.5
    }

    // Tab title + favicon in system recents (and Deck), plus the favicon-color theme seed. GeckoView
    // doesn't push favicons, so we resolve the site's OWN icon (Favicons — first-party only, parses
    // <link rel=icon> then falls back to /favicon.ico; engine-independent).
    private fun updateTaskDescription(url: String, title: String) {
        val label = title.ifBlank { url }.take(80)
        runCatching {
            @Suppress("DEPRECATION")
            setTaskDescription(android.app.ActivityManager.TaskDescription(label))
        }
        val domain = runCatching { Uri.parse(url).host?.removePrefix("www.") }.getOrNull()
            ?.takeIf { it.isNotBlank() } ?: return
        currentDomain = domain
        // Fast paths from cache.
        Favicons.cached(domain)?.let { setRecentsIcon(label, it) }
        accentCache[domain]?.let { seedColor = it }
        if (Favicons.cached(domain) != null && accentCache.containsKey(domain)) return
        Thread {
            val bmp = Favicons.fetch(url)
            val accent = bmp?.let { extractAccent(it) }
            if (accent != null) accentCache[domain] = accent
            runOnUiThread {
                if (bmp != null) setRecentsIcon(label, bmp)
                // Apply only if still on this domain (out-of-order navigation guard). When a site has
                // no usable favicon, accent is null → reset to neutral so the theme stops inheriting
                // the previous page's color.
                if (domain == currentDomain) seedColor = accent
            }
        }.start()
    }

    // Pick a seed color from the favicon: prefer a vibrant swatch, fall back to the dominant one.
    // Skip near-grayscale results so a plain black/white favicon doesn't wash the whole UI gray.
    private fun extractAccent(bmp: android.graphics.Bitmap): Int? = runCatching {
        val palette = androidx.palette.graphics.Palette.from(bmp).generate()
        val swatch = palette.vibrantSwatch
            ?: palette.lightVibrantSwatch
            ?: palette.darkVibrantSwatch
            ?: palette.mutedSwatch
            ?: palette.dominantSwatch
        swatch?.rgb
    }.getOrNull()

    private fun setRecentsIcon(label: String, bmp: android.graphics.Bitmap) {
        runCatching {
            @Suppress("DEPRECATION")
            setTaskDescription(android.app.ActivityManager.TaskDescription(label, bmp))
        }
    }

    override fun onResume() {
        super.onResume()
        isForeground = true
        session.setActive(true)
        sendBroadcast(
            Intent("com.hermes.deck.ACTION_BROWSER_TAB_FOCUSED").apply {
                setPackage("com.hermes.deck")
                putExtra("task_id", taskId)
            }
        )
        // Returning to an already-loaded tab: recapture now that it's foreground (its Deck preview
        // may have been skipped while it was backgrounded).
        if (pageHasLoaded) scheduleDeckCapture(250)
    }

    override fun onPause() {
        super.onPause()
        isForeground = false
    }

    override fun onStop() {
        super.onStop()
        session.setActive(false) // recommended GeckoView lifecycle; also nudges a state flush
        // Persist the latest SessionState (cached via onSessionStateChange) to disk. That callback is
        // throttled (~seconds; no on-demand saveState() in the API), so a page backgrounded within a
        // few seconds of loading may persist slightly older state. Acceptable for normal use.
        latestState?.let { runCatching { sessionFile().writeText(it.toString()) } }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        latestState?.let { outState.putString(KEY_SESSION_STATE, it.toString()) }
    }

    companion object {
        private const val KEY_SESSION_STATE = "gecko_session_state"
    }
}
