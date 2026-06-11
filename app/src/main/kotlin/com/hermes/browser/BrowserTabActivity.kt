package com.hermes.browser

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
 * One tab = one Activity task (see CLAUDE.md). GeckoView migration (Phase 1): the engine is now a
 * [GeckoSession] rendered in a [GeckoView] (was a WebView). The Deck broadcast/recents-task model is
 * unchanged. Tab state survives process death via [GeckoSession.SessionState] persisted to disk
 * keyed by [taskId] (the bundle is gone when the OS reaps a document task).
 *
 * BrowserScreen owns the session delegates (single delegate per type), and feeds this Activity the
 * two things it needs back: the latest SessionState (to persist) and the can-go-back flag (for the
 * back gesture). The predictive-back *snapshot* animation is deferred to Phase 3 (capturePixels).
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

    private fun sessionFile(): File = File(filesDir, "session_$taskId.json")

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
        android.util.Log.i("BTAB", "onCreate task=$taskId saved=${savedInstanceState != null} intentData=${intent.dataString}")

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (canGoBack) session.goBack() else moveTaskToBack(true)
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
                        onPageLoaded = {
                            sendBroadcast(
                                Intent("com.hermes.deck.ACTION_BROWSER_PAGE_LOADED").apply {
                                    setPackage("com.hermes.deck")
                                    putExtra("task_id", taskId)
                                }
                            )
                        },
                        onSessionStateChanged = { latestState = it; android.util.Log.i("BTAB", "stateChange task=$taskId len=${it.toString().length}") },
                        onCanGoBackChanged = { canGoBack = it }
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

        // Only on fresh creates — not on state restore. (Deck stacks the tab with its launching app.)
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
        // before onPageStart fires. Restore from the bundle, else the per-task disk file (survives a
        // reaped document task), else load the intent URL (emptied task shell).
        root.post {
            val stateStr = savedInstanceState?.getString(KEY_SESSION_STATE)
                ?: runCatching { sessionFile().takeIf { it.exists() }?.readText() }.getOrNull()
            val restored = stateStr?.let { runCatching { GeckoSession.SessionState.fromString(it) }.getOrNull() }
            android.util.Log.i("BTAB", "restore task=$taskId file=${sessionFile().name} exists=${sessionFile().exists()} stateLen=${stateStr?.length} restored=${restored != null}")
            if (restored != null) {
                session.restoreState(restored)
            } else {
                session.loadUri(intent.dataString ?: "https://start.duckduckgo.com")
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

    override fun onResume() {
        super.onResume()
        sendBroadcast(
            Intent("com.hermes.deck.ACTION_BROWSER_TAB_FOCUSED").apply {
                setPackage("com.hermes.deck")
                putExtra("task_id", taskId)
            }
        )
    }

    override fun onStop() {
        super.onStop()
        // Persist the latest SessionState (cached via onSessionStateChange) to disk. GeckoView only
        // PUSHES state via that callback and throttles it (~seconds; no on-demand saveState() in the
        // API), so a page backgrounded within a few seconds of loading may persist slightly older
        // state. Acceptable — normal use (navigating/reading) keeps it current.
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
