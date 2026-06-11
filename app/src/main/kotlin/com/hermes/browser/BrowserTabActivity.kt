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
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.webkit.WebSettings
import android.webkit.WebView
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

class BrowserTabActivity : ComponentActivity() {
    private lateinit var webView: WebView
    private lateinit var root: FrameLayout
    private var statusBarBgView: View? = null
    private var statusBarHeightPx by mutableStateOf(0)
    private var statusBarTransparent = true
    private var backSnapshotBitmap    by mutableStateOf<android.graphics.Bitmap?>(null)
    private var backAnimProgress      by mutableStateOf(0f)
    private var backSwipeEdge         by mutableStateOf(0)
    private var backCommitAnimator: android.animation.ValueAnimator? = null
    private var showBackOverlay       by mutableStateOf(false)
    private var backGestureSeq        = 0

    // Proactive screenshot cache: captured after each page load via SurfaceView PixelCopy
    // or webView.draw() fallback. prevPageShot drives the "previous page" preview layer.
    private var currentPageShot: android.graphics.Bitmap? = null
    private var prevPageShot          by mutableStateOf<android.graphics.Bitmap?>(null)
    private var isNavigatingBack      = false

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

    // Traverse WebView's child hierarchy to find its internal SurfaceView, if any.
    private fun findSurfaceViewIn(view: View): android.view.SurfaceView? {
        if (view is android.view.SurfaceView) return view
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                findSurfaceViewIn(view.getChildAt(i))?.let { return it }
            }
        }
        return null
    }

    // Async capture via SurfaceView PixelCopy (non-blocking). If no SurfaceView is found
    // inside the WebView, falls back to webView.draw() (blocking but works on this device).
    private fun captureWebViewAsync(onDone: (android.graphics.Bitmap?) -> Unit) {
        val w = webView.width.coerceAtLeast(1)
        val h = webView.height.coerceAtLeast(1)
        val bmp = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
        val sv = findSurfaceViewIn(webView)
        if (sv != null) {
            android.view.PixelCopy.request(sv, null, bmp, { result ->
                onDone(if (result == android.view.PixelCopy.SUCCESS) bmp else null)
            }, android.os.Handler(android.os.Looper.getMainLooper()))
        } else {
            runCatching { webView.draw(android.graphics.Canvas(bmp)) }
            onDone(bmp)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackStarted(backEvent: BackEventCompat) {
                backCommitAnimator?.cancel()
                backCommitAnimator = null
                if (!webView.canGoBack()) return
                backGestureSeq++
                backSwipeEdge = backEvent.swipeEdge
                backAnimProgress = 0f
                // Use the proactively-captured shot (instant) or fall back to draw() now.
                val cached = currentPageShot
                if (cached != null) {
                    backSnapshotBitmap = cached
                } else {
                    val seq = backGestureSeq
                    captureWebViewAsync { bmp ->
                        if (seq == backGestureSeq) backSnapshotBitmap = bmp
                    }
                }
                // showBackOverlay stays false during the gesture — prevPageShot is the background.
                // It's set to true on commit so the overlay hides the WebView while page A loads.
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
                showBackOverlay = true  // hide WebView while page A loads after goBack()
                val seq = backGestureSeq
                if (backSnapshotBitmap != null) {
                    webView.goBack()
                } else {
                    if (webView.canGoBack()) webView.goBack() else {
                        isNavigatingBack = false
                        showBackOverlay = false
                        moveTaskToBack(true)
                    }
                }
                // Safety net: if goBack() restores a bfcache page that fires no paint callback,
                // onPageCommitVisible/onPageFinished never clears the overlay and it lingers grey.
                // Force-clear after a short delay (guarded by seq so a newer gesture wins).
                root.postDelayed({
                    if (seq == backGestureSeq && showBackOverlay) showBackOverlay = false
                }, 450)
                val fromProgress = backAnimProgress
                backCommitAnimator = android.animation.ValueAnimator.ofFloat(fromProgress, 2.0f).apply {
                    duration = 180
                    interpolator = android.view.animation.AccelerateInterpolator()
                    addUpdateListener { backAnimProgress = it.animatedValue as Float }
                    addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: android.animation.Animator) {
                            backSnapshotBitmap = null
                            backAnimProgress = 0f
                            backCommitAnimator = null
                        }
                    })
                    start()
                }
            }
        })

        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.setSupportZoom(true)
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            settings.setSupportMultipleWindows(true)
            settings.userAgentString = settings.userAgentString.replace("; wv", "")
        }

        val statusBarBg = View(this).also { statusBarBgView = it }

        statusBarTransparent = getSharedPreferences("browser_prefs", MODE_PRIVATE)
            .getBoolean("status_bar_transparent", true)

        val composeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                BrowserTheme {
                    BrowserScreen(
                        webView = webView,
                        statusBarHeightPx = statusBarHeightPx,
                        onNewTab = { url ->
                            startActivity(
                                Intent(this@BrowserTabActivity, BrowserTabActivity::class.java).apply {
                                    data = Uri.parse(url)
                                    // No EXCLUDE_FROM_RECENTS: an excluded document task gets reaped by the
                                    // system when backgrounded (proven: nexuslauncher's TasksRepository removed
                                    // a backgrounded tab task), so its Deck card pointed at a dead taskId and
                                    // moveTaskToFront() silently no-op'd. A normal recents task record persists
                                    // across process death (we already restoreState()/loadUrl on the empty shell),
                                    // so reopen-by-taskId stays durable. Cost: tabs also appear in system recents.
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                                }
                            )
                        },
                        onPageBackgroundColor = { color, transparent -> updateStatusBarColor(color, transparent) },
                        onSetDefaultBrowser = { requestDefaultBrowser() },
                        onStatusBarTransparentChanged = { newValue ->
                            statusBarTransparent = newValue
                            applyWebViewMargin()
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
                            // Proactively capture this page so it's ready as the snapshot
                            // for the next back gesture (and as prevPageShot for the one after).
                            captureWebViewAsync { bmp -> currentPageShot = bmp }
                            // Tell Deck the page is done loading so it can retake the preview
                            // with a fully-rendered screenshot instead of the early grey one.
                            sendBroadcast(
                                Intent("com.hermes.deck.ACTION_BROWSER_PAGE_LOADED").apply {
                                    setPackage("com.hermes.deck")
                                    putExtra("task_id", taskId)
                                }
                            )
                        },
                        onNavigatingAway = {
                            // Save current page as the "previous" preview for the next back gesture.
                            // Skip on back navigations — the prevPageShot should persist through those.
                            if (!isNavigatingBack) prevPageShot = currentPageShot
                            isNavigatingBack = false
                            currentPageShot = null
                        }
                    )
                }
            }
        }

        root = FrameLayout(this)

        val webParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        root.addView(webView, webParams)

        // Status bar background sits at top, height = status bar inset
        val sbParams = FrameLayout.LayoutParams(MATCH_PARENT, 0).apply {
            gravity = Gravity.TOP
        }
        root.addView(statusBarBg, sbParams)

        root.addView(composeView, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))

        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            statusBarHeightPx = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top

            (statusBarBg.layoutParams as FrameLayout.LayoutParams).height = statusBarHeightPx
            statusBarBg.requestLayout()

            applyWebViewMargin()

            insets
        }

        setContentView(root)

        // Notify Deck that a new tab was opened. Only on fresh creates — not on state restore.
        if (savedInstanceState == null) {
            // referrer is "android-app://<launching package>" for app-to-app VIEW intents; Deck
            // uses it to stack the tab with the app it was opened from (e.g. Inoreader).
            val parentPkg = referrer?.takeIf { it.scheme == "android-app" }?.host
            sendBroadcast(
                Intent("com.hermes.deck.ACTION_BROWSER_TAB_OPENED").apply {
                    setPackage("com.hermes.deck")
                    putExtra("task_id", taskId)
                    if (parentPkg != null) putExtra("parent_package", parentPkg)
                }
            )
        }

        // Defer load until after the first Compose frame so the WebViewClient (set in
        // BrowserScreen's DisposableEffect) is installed before onPageStarted fires.
        root.post {
            // restoreState() returns null when there's no usable saved WebView state — which
            // happens when the OS recreates an emptied task shell (process was killed). In that
            // case fall back to loading the task's URL so the tab actually shows content instead
            // of a blank grey screen ("can't open the tab" symptom).
            val restored = savedInstanceState != null && webView.restoreState(savedInstanceState) != null
            if (!restored) {
                val url = intent.dataString ?: webView.url ?: "https://start.duckduckgo.com"
                webView.loadUrl(url)
            }
        }
    }

    private fun applyWebViewMargin() {
        val margin = if (statusBarTransparent) 0 else statusBarHeightPx
        (webView.layoutParams as? FrameLayout.LayoutParams)?.let { params ->
            params.topMargin = margin
            webView.requestLayout()
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
        val isLight = isLightColor(color)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = isLight
    }

    private fun isLightColor(color: Int): Boolean {
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        return (0.299 * r + 0.587 * g + 0.114 * b) / 255 > 0.5
    }

    override fun onResume() {
        super.onResume()
        // Notify Deck which browser task is now in the foreground so it can store
        // the upcoming screenshot under the correct task-specific cache key.
        sendBroadcast(
            Intent("com.hermes.deck.ACTION_BROWSER_TAB_FOCUSED").apply {
                setPackage("com.hermes.deck")
                putExtra("task_id", taskId)
            }
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

}
