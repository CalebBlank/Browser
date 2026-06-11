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
    private var statusBarBgView: View? = null
    private var statusBarHeightPx by mutableStateOf(0)
    private var statusBarTransparent = true

    // Fed by BrowserScreen's delegates.
    private var latestState: GeckoSession.SessionState? = null
    private var canGoBack = false
    private val faviconCache = java.util.concurrent.ConcurrentHashMap<String, android.graphics.Bitmap>()

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

        val statusBarBg = View(this).also { statusBarBgView = it }

        statusBarTransparent = getSharedPreferences("browser_prefs", MODE_PRIVATE)
            .getBoolean("status_bar_transparent", true)

        val composeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                BrowserTheme {
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
                            // Capture this page so it's ready as the snapshot for the next back gesture.
                            captureGeckoViewAsync { bmp -> currentPageShot = bmp }
                            // Delay Deck's screenshot trigger: GeckoView's compositor paints
                            // asynchronously after onPageStop, so an immediate capture can be blank
                            // (a freshly-opened tab showed no preview on its Deck card). Let it paint.
                            root.postDelayed({
                                sendBroadcast(
                                    Intent("com.hermes.deck.ACTION_BROWSER_PAGE_LOADED").apply {
                                        setPackage("com.hermes.deck")
                                        putExtra("task_id", taskId)
                                    }
                                )
                            }, 400)
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
                        }
                    )
                }
            }
        }

        root = FrameLayout(this)
        root.addView(geckoView, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))

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
        (geckoView.layoutParams as? FrameLayout.LayoutParams)?.let { params ->
            params.topMargin = margin
            geckoView.requestLayout()
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

    // Tab title + favicon in system recents (and Deck once integrated). GeckoView doesn't push
    // favicons, so fetch the site's OWN /favicon.ico (engine-independent; never a third party, to
    // avoid leaking every visited domain).
    private fun updateTaskDescription(url: String, title: String) {
        val label = title.ifBlank { url }.take(80)
        runCatching {
            @Suppress("DEPRECATION")
            setTaskDescription(android.app.ActivityManager.TaskDescription(label))
        }
        val domain = runCatching { Uri.parse(url).host?.removePrefix("www.") }.getOrNull()
            ?.takeIf { it.isNotBlank() } ?: return
        faviconCache[domain]?.let { setRecentsIcon(label, it); return }
        Thread {
            val bmp = runCatching {
                val conn = java.net.URL("https://$domain/favicon.ico").openConnection() as java.net.HttpURLConnection
                conn.connectTimeout = 4000; conn.readTimeout = 4000
                conn.inputStream.use { android.graphics.BitmapFactory.decodeStream(it) }
            }.getOrNull()
            if (bmp != null) {
                faviconCache[domain] = bmp
                runOnUiThread { setRecentsIcon(label, bmp) }
            }
        }.start()
    }

    private fun setRecentsIcon(label: String, bmp: android.graphics.Bitmap) {
        runCatching {
            @Suppress("DEPRECATION")
            setTaskDescription(android.app.ActivityManager.TaskDescription(label, bmp))
        }
    }

    override fun onResume() {
        super.onResume()
        session.setActive(true)
        sendBroadcast(
            Intent("com.hermes.deck.ACTION_BROWSER_TAB_FOCUSED").apply {
                setPackage("com.hermes.deck")
                putExtra("task_id", taskId)
            }
        )
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
