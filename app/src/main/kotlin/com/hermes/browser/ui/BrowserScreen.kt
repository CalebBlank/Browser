package com.hermes.browser.ui

import android.app.DownloadManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import android.graphics.Color
import android.net.Uri
import android.view.View
import android.os.Message
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import android.content.SharedPreferences
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material3.HorizontalDivider
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.unit.Velocity
import com.hermes.browser.AnthropicCssClient
import com.hermes.browser.BrowserApp
import com.hermes.browser.DeckCredentials
import com.hermes.browser.Favicons
import com.hermes.browser.UserCss
import com.hermes.browser.data.BookmarksDatabase
import com.hermes.browser.data.HistoryDatabase
import org.json.JSONObject
import org.mozilla.geckoview.WebExtension
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings

@Composable
fun BrowserScreen(
    session: GeckoSession,
    statusBarHeightPx: Int,
    onNewTab: (String) -> Unit,
    onPageBackgroundColor: (color: Int, transparent: Boolean) -> Unit = { _, _ -> },
    onSetDefaultBrowser: () -> Unit = {},
    onStatusBarTransparentChanged: (Boolean) -> Unit = {},
    backSnapshot: android.graphics.Bitmap? = null,
    backProgress: Float = 0f,
    backSwipeEdge: Int = 0,
    prevPageShot: android.graphics.Bitmap? = null,
    showBackOverlay: Boolean = false,
    onBackOverlayDismiss: () -> Unit = {},
    onPageLoaded: () -> Unit = {},
    onNavigatingAway: () -> Unit = {},
    onSessionStateChanged: (GeckoSession.SessionState) -> Unit = {},
    onCanGoBackChanged: (Boolean) -> Unit = {},
    onPageInfo: (url: String, title: String) -> Unit = { _, _ -> },
    onPageScroll: (Int) -> Unit = {},
    themeMode: String = "website",
    themeFixedColor: Int = 0,
    onSelectThemeWebsite: () -> Unit = {},
    onSelectThemeSystem: () -> Unit = {},
    onSelectThemeColor: (Int) -> Unit = {},
    onRequestRecolor: () -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("browser_prefs", Context.MODE_PRIVATE) }
    var statusBarTransparent by remember { mutableStateOf(prefs.getBoolean("status_bar_transparent", true)) }
    val lastKnownColor = remember { mutableStateOf<Int?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var javaScriptEnabled by remember { mutableStateOf(true) }
    val latestTransparent by rememberUpdatedState(statusBarTransparent)
    val latestShowBackOverlay by rememberUpdatedState(showBackOverlay)
    val latestOnBackOverlayDismiss by rememberUpdatedState(onBackOverlayDismiss)
    val latestOnPageLoaded by rememberUpdatedState(onPageLoaded)
    val latestOnNavigatingAway by rememberUpdatedState(onNavigatingAway)

    val currentUrl = remember { mutableStateOf("") }
    val addressBarText = remember { mutableStateOf("") }
    val isLoading = remember { mutableStateOf(false) }
    val toolbarVisible = remember { mutableStateOf(true) }
    val isAddressBarFocused = remember { mutableStateOf(false) }
    // "Search is open" — a deliberate state that, unlike raw TextField focus, SURVIVES the keyboard
    // being swiped down and the back gesture's focus teardown. The back handler keys off this so a
    // back-swipe closes the search instead of falling through to page navigation. Cleared only on an
    // explicit close, a navigation, or page interaction (scroll).
    val searchOpen = remember { mutableStateOf(false) }
    val latestStatusBarHeight by rememberUpdatedState(statusBarHeightPx)

    val pageTitle = remember { mutableStateOf("") }
    var requestDesktop by remember { mutableStateOf(false) }

    var showFindBar by remember { mutableStateOf(false) }
    var findQuery by remember { mutableStateOf("") }
    var findActiveMatch by remember { mutableStateOf(0) }
    var findTotalMatches by remember { mutableStateOf(0) }
    val focusManager = LocalFocusManager.current

    var isPanelOpen by remember { mutableStateOf(false) }
    var isBookmarked by remember { mutableStateOf(false) }

    // GeckoView permission bridge. Android runtime perms (camera/mic/location) go through a Compose
    // launcher; site content/media requests (geolocation, notifications, getUserMedia) show an
    // Allow/Block dialog (permPrompt).
    val pendingAndroidCallback = remember { mutableStateOf<GeckoSession.PermissionDelegate.Callback?>(null) }
    val androidPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        pendingAndroidCallback.value?.let { cb ->
            if (results.values.all { it }) cb.grant() else cb.reject()
        }
        pendingAndroidCallback.value = null
    }
    var permPrompt by remember { mutableStateOf<PermPrompt?>(null) }

    // Custom CSS: the live content-script port for THIS session (used to push edits without a reload),
    // and the drawer toggle.
    val cssPort = remember { mutableStateOf<WebExtension.Port?>(null) }
    var showCssSheet by remember { mutableStateOf(false) }
    // Latest page element outline (from the content script) — fed to the AI for precise selectors.
    val cssContext = remember { mutableStateOf("") }

    // Wire the bundled usercss WebExtension's content-script messaging to THIS session. The extension
    // installs async (BrowserApp.ensureBuiltIn), so wait for it. The content script asks for its URL's
    // CSS; we answer from UserCss and keep the port to push live edits.
    LaunchedEffect(session) {
        var ext = BrowserApp.userCssExtension(context)
        while (ext == null) { delay(100); ext = BrowserApp.userCssExtension(context) }
        session.webExtensionController.setMessageDelegate(ext, object : WebExtension.MessageDelegate {
            override fun onConnect(port: WebExtension.Port) {
                cssPort.value = port
                port.setDelegate(object : WebExtension.PortDelegate {
                    override fun onPortMessage(message: Any, port: WebExtension.Port) {
                        val obj = message as? JSONObject ?: return
                        if (obj.has("url")) {
                            val css = UserCss.getCssForUrl(context, obj.optString("url")) ?: ""
                            port.postMessage(JSONObject().put("css", css))
                        }
                        if (obj.has("context")) {
                            cssContext.value = obj.optString("context")
                        }
                    }
                    override fun onDisconnect(port: WebExtension.Port) {
                        if (cssPort.value === port) cssPort.value = null
                    }
                })
            }
        }, "browser")
    }

    BackHandler(enabled = searchOpen.value) {
        searchOpen.value = false
        focusManager.clearFocus()
    }

    BackHandler(enabled = showFindBar) {
        showFindBar = false
        findQuery = ""
        findActiveMatch = 0
        findTotalMatches = 0
        session.finder.clear()
    }

    val contextMenuTransition = remember { MutableTransitionState(false) }
    var contextMenuLinkUrl by remember { mutableStateOf<String?>(null) }
    var contextMenuImageUrl by remember { mutableStateOf<String?>(null) }
    var contextMenuX by remember { mutableStateOf(0) }
    var contextMenuY by remember { mutableStateOf(0) }

    var suggestions by remember { mutableStateOf(emptyList<String>()) }
    var recentQueries by remember { mutableStateOf(emptyList<String>()) }

    LaunchedEffect(addressBarText.value) {
        if (!isAddressBarFocused.value || addressBarText.value.isEmpty()) {
            suggestions = emptyList()
            return@LaunchedEffect
        }
        delay(300L)
        suggestions = fetchSuggestions(addressBarText.value)
    }

    LaunchedEffect(currentUrl.value) {
        val url = currentUrl.value
        if (url.isEmpty() || url.startsWith("about:") || url.startsWith("data:")) {
            isBookmarked = false
            return@LaunchedEffect
        }
        isBookmarked = withContext(Dispatchers.IO) {
            BookmarksDatabase.get(context).isBookmarked(url)
        }
    }

    DisposableEffect(session) {
        var lastScrollY = 0

        session.progressDelegate = object : GeckoSession.ProgressDelegate {
            override fun onPageStart(s: GeckoSession, url: String) {
                latestOnNavigatingAway()
                isLoading.value = true
                currentUrl.value = url
                if (!isAddressBarFocused.value) addressBarText.value = simplifyUrl(url)
            }

            override fun onPageStop(s: GeckoSession, success: Boolean) {
                isLoading.value = false
                latestOnPageLoaded()
                val url = currentUrl.value
                if (url.isNotEmpty() && !url.startsWith("about:") && !url.startsWith("data:")) {
                    HistoryDatabase.get(context).record(url, pageTitle.value.ifEmpty { url })
                }
                onPageInfo(url, pageTitle.value)
            }

            override fun onProgressChange(s: GeckoSession, progress: Int) {
                if (progress >= 100) isLoading.value = false
            }

            override fun onSessionStateChange(s: GeckoSession, sessionState: GeckoSession.SessionState) {
                onSessionStateChanged(sessionState)
            }
        }

        session.navigationDelegate = object : GeckoSession.NavigationDelegate {
            override fun onCanGoBack(s: GeckoSession, canGoBack: Boolean) {
                onCanGoBackChanged(canGoBack)
            }

            override fun onNewSession(s: GeckoSession, uri: String): GeckoResult<GeckoSession>? {
                // New tab = new Activity (NEW_DOCUMENT|MULTIPLE_TASK), not a child session here.
                onNewTab(uri)
                return GeckoResult.fromValue(null)
            }

            override fun onLoadRequest(
                s: GeckoSession,
                request: GeckoSession.NavigationDelegate.LoadRequest
            ): GeckoResult<org.mozilla.geckoview.AllowOrDeny>? {
                // Hand non-web schemes (mailto:, tel:, intent:, market:, ...) to the system.
                val scheme = android.net.Uri.parse(request.uri).scheme?.lowercase()
                if (scheme != null && scheme !in setOf("http", "https", "about", "data", "blob", "javascript")) {
                    runCatching {
                        context.startActivity(
                            android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(request.uri))
                                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                    return GeckoResult.fromValue(org.mozilla.geckoview.AllowOrDeny.DENY)
                }
                return GeckoResult.fromValue(org.mozilla.geckoview.AllowOrDeny.ALLOW)
            }
        }

        session.contentDelegate = object : GeckoSession.ContentDelegate {
            override fun onTitleChange(s: GeckoSession, title: String?) {
                pageTitle.value = title ?: ""
                onPageInfo(currentUrl.value, pageTitle.value)
            }

            override fun onContextMenu(
                s: GeckoSession, screenX: Int, screenY: Int,
                element: GeckoSession.ContentDelegate.ContextElement
            ) {
                val link = element.linkUri
                val img = if (element.type == GeckoSession.ContentDelegate.ContextElement.TYPE_IMAGE) element.srcUri else null
                if (link == null && img == null) return
                contextMenuLinkUrl = link
                contextMenuImageUrl = img
                contextMenuX = screenX
                contextMenuY = screenY
                contextMenuTransition.targetState = true
            }

            override fun onExternalResponse(s: GeckoSession, response: org.mozilla.geckoview.WebResponse) {
                downloadWebResponse(context, response)
            }
        }

        session.scrollDelegate = object : GeckoSession.ScrollDelegate {
            override fun onScrollChanged(s: GeckoSession, scrollX: Int, scrollY: Int) {
                // Drives pull-to-refresh gating (only at the very top of the page).
                onPageScroll(scrollY)
                // Engaging the page (scroll) ends a stranded search session (e.g. user tapped the page
                // to dismiss the keyboard, collapsing the search but leaving searchOpen set).
                if (searchOpen.value) searchOpen.value = false
                if (!isAddressBarFocused.value) {
                    val delta = scrollY - lastScrollY
                    when {
                        delta > 20 && scrollY > 150 -> toolbarVisible.value = false
                        delta < -20 || scrollY < 50 -> toolbarVisible.value = true
                    }
                    lastScrollY = scrollY
                }
            }
        }

        session.permissionDelegate = object : GeckoSession.PermissionDelegate {
            override fun onAndroidPermissionsRequest(
                s: GeckoSession,
                permissions: Array<out String>?,
                callback: GeckoSession.PermissionDelegate.Callback
            ) {
                val perms = permissions?.filterNotNull()?.toTypedArray() ?: emptyArray()
                if (perms.isEmpty()) { callback.grant(); return }
                pendingAndroidCallback.value = callback
                androidPermLauncher.launch(perms)
            }

            override fun onContentPermissionRequest(
                s: GeckoSession,
                perm: GeckoSession.PermissionDelegate.ContentPermission
            ): GeckoResult<Int> {
                val result = GeckoResult<Int>()
                when (perm.permission) {
                    GeckoSession.PermissionDelegate.PERMISSION_AUTOPLAY_INAUDIBLE ->
                        result.complete(GeckoSession.PermissionDelegate.ContentPermission.VALUE_ALLOW)
                    GeckoSession.PermissionDelegate.PERMISSION_GEOLOCATION,
                    GeckoSession.PermissionDelegate.PERMISSION_DESKTOP_NOTIFICATION,
                    GeckoSession.PermissionDelegate.PERMISSION_PERSISTENT_STORAGE,
                    GeckoSession.PermissionDelegate.PERMISSION_MEDIA_KEY_SYSTEM_ACCESS -> {
                        permPrompt = PermPrompt(originOf(perm.uri), contentActionFor(perm.permission)) { allow ->
                            result.complete(
                                if (allow) GeckoSession.PermissionDelegate.ContentPermission.VALUE_ALLOW
                                else GeckoSession.PermissionDelegate.ContentPermission.VALUE_DENY
                            )
                            permPrompt = null
                        }
                    }
                    else -> result.complete(GeckoSession.PermissionDelegate.ContentPermission.VALUE_DENY)
                }
                return result
            }

            override fun onMediaPermissionRequest(
                s: GeckoSession,
                uri: String,
                video: Array<out GeckoSession.PermissionDelegate.MediaSource>?,
                audio: Array<out GeckoSession.PermissionDelegate.MediaSource>?,
                callback: GeckoSession.PermissionDelegate.MediaCallback
            ) {
                val action = when {
                    video != null && audio != null -> "use your camera and microphone"
                    video != null -> "use your camera"
                    audio != null -> "use your microphone"
                    else -> { callback.reject(); return }
                }
                permPrompt = PermPrompt(originOf(uri), action) { allow ->
                    if (allow) callback.grant(video?.firstOrNull(), audio?.firstOrNull())
                    else callback.reject()
                    permPrompt = null
                }
            }
        }

        onDispose {
            session.progressDelegate = null
            session.navigationDelegate = null
            session.contentDelegate = null
            session.scrollDelegate = null
            session.permissionDelegate = null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading.value) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .align(Alignment.TopCenter)
            )
        }

        if (showFindBar) {
            FindBar(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding(),
                query = findQuery,
                activeMatch = findActiveMatch,
                totalMatches = findTotalMatches,
                onQueryChange = { q ->
                    findQuery = q
                    if (q.isNotEmpty()) {
                        session.finder.find(q, 0).accept { r ->
                            if (r != null) { findActiveMatch = r.current; findTotalMatches = r.total }
                        }
                    } else session.finder.clear()
                },
                onNext = {
                    session.finder.find(null, GeckoSession.FINDER_FIND_FORWARD).accept { r ->
                        if (r != null) { findActiveMatch = r.current; findTotalMatches = r.total }
                    }
                },
                onPrevious = {
                    session.finder.find(null, GeckoSession.FINDER_FIND_BACKWARDS).accept { r ->
                        if (r != null) { findActiveMatch = r.current; findTotalMatches = r.total }
                    }
                },
                onClose = {
                    showFindBar = false
                    findQuery = ""
                    findActiveMatch = 0
                    findTotalMatches = 0
                    session.finder.clear()
                }
            )
        }

        if (showSettings) {
            SettingsSheet(
                transparent = statusBarTransparent,
                onTransparentChange = { newValue ->
                    statusBarTransparent = newValue
                    prefs.edit().putBoolean("status_bar_transparent", newValue).apply()
                    onStatusBarTransparentChanged(newValue)
                    lastKnownColor.value?.let { onPageBackgroundColor(it, newValue) }
                },
                onSetDefaultBrowser = onSetDefaultBrowser,
                themeMode = themeMode,
                themeFixedColor = themeFixedColor,
                onSelectThemeWebsite = onSelectThemeWebsite,
                onSelectThemeSystem = onSelectThemeSystem,
                onSelectThemeColor = onSelectThemeColor,
                onDismiss = { showSettings = false }
            )
        }

        permPrompt?.let { p ->
            AlertDialog(
                onDismissRequest = { p.onResult(false) },
                title = { Text(p.origin, maxLines = 1) },
                text = { Text("Allow this site to ${p.action}?") },
                confirmButton = { TextButton(onClick = { p.onResult(true) }) { Text("Allow") } },
                dismissButton = { TextButton(onClick = { p.onResult(false) }) { Text("Block") } }
            )
        }

        if (showCssSheet) {
            val cssDomain = remember(currentUrl.value) { Favicons.keyOf(currentUrl.value) ?: "" }
            CssSheet(
                domain = cssDomain,
                initialCss = remember(cssDomain) { UserCss.getCss(context, cssDomain) ?: "" },
                pageContext = cssContext.value,
                onSave = { newCss ->
                    if (cssDomain.isNotBlank()) {
                        UserCss.setCss(context, cssDomain, newCss)
                        // Live-apply to the current page without a reload.
                        cssPort.value?.postMessage(JSONObject().put("css", newCss))
                        // The CSS may have changed the page background — re-match the status bar.
                        onRequestRecolor()
                    }
                },
                onReset = {
                    if (cssDomain.isNotBlank()) {
                        UserCss.setCss(context, cssDomain, "")
                        cssPort.value?.postMessage(JSONObject().put("css", ""))
                        onRequestRecolor()
                    }
                },
                onDismiss = { showCssSheet = false }
            )
        }

        if (contextMenuTransition.currentState || contextMenuTransition.targetState) {
            LongPressContextMenu(
                visibleState = contextMenuTransition,
                offsetX  = contextMenuX,
                offsetY  = contextMenuY,
                linkUrl  = contextMenuLinkUrl,
                imageUrl = contextMenuImageUrl,
                onDismiss = { contextMenuTransition.targetState = false },
                onNewTab  = { url ->
                    contextMenuTransition.targetState = false
                    onNewTab(url)
                },
                onCopyLink = { url ->
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("url", url))
                    contextMenuTransition.targetState = false
                },
                onDownloadImage = { url ->
                    val req = android.app.DownloadManager.Request(Uri.parse(url)).apply {
                        setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, Uri.parse(url).lastPathSegment ?: "image")
                    }
                    (context.getSystemService(Context.DOWNLOAD_SERVICE) as android.app.DownloadManager).enqueue(req)
                    contextMenuTransition.targetState = false
                }
            )
        }

        FloatingNavBar(
            modifier = Modifier.fillMaxSize(),
            isVisible = toolbarVisible.value,
            isExpanded = isAddressBarFocused.value,
            addressText = addressBarText.value,
            onAddressTextChange = { addressBarText.value = it },
            onNavigate = {
                val input = addressBarText.value
                if (input.isNotBlank()) saveRecentQuery(prefs, input)
                recentQueries = getRecentQueries(prefs)
                session.loadUri(normalizeUrl(input))
                searchOpen.value = false
                focusManager.clearFocus()
            },
            isLoading = isLoading.value,
            onRefreshOrStop = { if (isLoading.value) session.stop() else session.reload() },
            onNewTab = { onNewTab(currentUrl.value.ifEmpty { "https://start.duckduckgo.com" }) },
            onOpenSettings = { showSettings = true },
            onCustomCss = { showCssSheet = true },
            isJavaScriptEnabled = javaScriptEnabled,
            onToggleJavaScript = {
                val newValue = !javaScriptEnabled
                javaScriptEnabled = newValue
                session.settings.allowJavascript = newValue
                session.reload()
            },
            onFocusGained = {
                isAddressBarFocused.value = true
                searchOpen.value = true
                toolbarVisible.value = true
                val url = currentUrl.value
                addressBarText.value = if (simplifyUrl(url).isEmpty()) "" else url
                recentQueries = getRecentQueries(prefs)
            },
            onFocusLost = {
                // Note: does NOT clear searchOpen — the keyboard hiding (or the back gesture's focus
                // teardown) must not disable the search-close back handler. searchOpen is cleared only
                // by an explicit close, a navigation, or page scroll.
                isAddressBarFocused.value = false
                addressBarText.value = simplifyUrl(currentUrl.value)
            },
            onCloseSearch = {
                searchOpen.value = false
                focusManager.clearFocus()
            },
            onFindInPage = { showFindBar = true },
            isDesktopSite = requestDesktop,
            onToggleDesktopSite = {
                requestDesktop = !requestDesktop
                session.settings.userAgentMode = if (requestDesktop)
                    GeckoSessionSettings.USER_AGENT_MODE_DESKTOP else GeckoSessionSettings.USER_AGENT_MODE_MOBILE
                session.settings.viewportMode = if (requestDesktop)
                    GeckoSessionSettings.VIEWPORT_MODE_DESKTOP else GeckoSessionSettings.VIEWPORT_MODE_MOBILE
                session.reload()
            },
            onShare = {
                val url = currentUrl.value.ifEmpty { return@FloatingNavBar }
                context.startActivity(
                    android.content.Intent.createChooser(
                        android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(android.content.Intent.EXTRA_TEXT, url)
                        },
                        null
                    ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            },
            suggestions = suggestions,
            recentQueries = recentQueries,
            onFillText = { text -> addressBarText.value = text },
            onSuggestionNavigate = { input ->
                if (input.isNotBlank()) saveRecentQuery(prefs, input)
                recentQueries = getRecentQueries(prefs)
                session.loadUri(normalizeUrl(input))
                searchOpen.value = false
                focusManager.clearFocus()
            },
            isBookmarked = isBookmarked,
            onToggleBookmark = {
                val url = currentUrl.value
                val title = pageTitle.value.ifEmpty { url }
                if (url.isNotEmpty() && !url.startsWith("about:") && !url.startsWith("data:")) {
                    val db = BookmarksDatabase.get(context)
                    if (isBookmarked) {
                        db.remove(url)
                        isBookmarked = false
                    } else {
                        db.add(url, title)
                        isBookmarked = true
                    }
                }
            },
            isPanelOpen = isPanelOpen,
            onOpenPanel = { isPanelOpen = true },
            onClosePanel = { isPanelOpen = false },
            onPanelNavigate = { url ->
                session.loadUri(url)
                isPanelOpen = false
            },
        )

        // Covers stale WebView content before the first URL loads (new/restored tab).
        if (currentUrl.value.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        }

        // Previous page screenshot — visible during the back gesture as the destination preview.
        // Only shown when the snapshot is sliding (backSnapshot != null), so it doesn't linger.
        if (prevPageShot != null && backSnapshot != null) {
            val density = LocalDensity.current
            val gestureProgress = backProgress.coerceIn(0f, 1f)
            // Starts slightly zoomed-out, zooms to full as the gesture progresses (iOS-style).
            val prevScale = 0.92f + 0.08f * gestureProgress
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { scaleX = prevScale; scaleY = prevScale }
            ) {
                androidx.compose.foundation.Image(
                    bitmap             = prevPageShot.asImageBitmap(),
                    contentDescription = null,
                    modifier           = Modifier.fillMaxSize(),
                    contentScale       = ContentScale.FillBounds
                )
            }
        }

        // Solid overlay active after gesture commits — hides the WebView while page A loads.
        if (showBackOverlay) {
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        }

        // Snapshot overlay — rendered on top of the blank background above.
        if (backSnapshot != null) {
            val density = LocalDensity.current
            val gestureProgress = backProgress.coerceIn(0f, 1f)
            val exitProgress = (backProgress - 1f).coerceAtLeast(0f)
            val scale = (1f - 0.08f * gestureProgress).coerceAtLeast(0f)
            val gestureDx = with(density) { 64.dp.toPx() } * gestureProgress
            val exitDx    = with(density) { 600.dp.toPx() } * exitProgress
            val translationX = if (backSwipeEdge == 0) gestureDx + exitDx else -(gestureDx + exitDx)
            val cornerDp = (gestureProgress * 28f).dp
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.translationX = translationX
                    }
                    .clip(RoundedCornerShape(cornerDp))
            ) {
                androidx.compose.foundation.Image(
                    bitmap             = backSnapshot.asImageBitmap(),
                    contentDescription = null,
                    modifier           = Modifier.fillMaxSize(),
                    contentScale       = ContentScale.FillBounds
                )
            }
        }
    }
}

@Composable
private fun FloatingNavBar(
    modifier: Modifier = Modifier,
    isVisible: Boolean = true,
    isExpanded: Boolean = false,
    addressText: String,
    onAddressTextChange: (String) -> Unit,
    onNavigate: () -> Unit,
    isLoading: Boolean,
    onRefreshOrStop: () -> Unit,
    onNewTab: () -> Unit,
    onOpenSettings: () -> Unit = {},
    onCustomCss: () -> Unit = {},
    isJavaScriptEnabled: Boolean = true,
    onToggleJavaScript: () -> Unit = {},
    onFocusGained: () -> Unit = {},
    onFocusLost: () -> Unit = {},
    onFindInPage: () -> Unit = {},
    isDesktopSite: Boolean = false,
    onToggleDesktopSite: () -> Unit = {},
    onShare: () -> Unit = {},
    suggestions: List<String> = emptyList(),
    recentQueries: List<String> = emptyList(),
    onFillText: (String) -> Unit = {},
    onSuggestionNavigate: (String) -> Unit = {},
    isBookmarked: Boolean = false,
    onToggleBookmark: () -> Unit = {},
    isPanelOpen: Boolean = false,
    onOpenPanel: () -> Unit = {},
    onClosePanel: () -> Unit = {},
    onPanelNavigate: (String) -> Unit = {},
    onCloseSearch: () -> Unit = {},
) {
    var showMenu by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    val menuTransition = remember { MutableTransitionState(false) }
    menuTransition.targetState = showMenu

    val horizontalPad by animateDpAsState(
        targetValue = if (isFocused) 12.dp else 24.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "hPad"
    )

    val filteredRecent = if (addressText.isEmpty()) recentQueries.take(4)
                         else recentQueries.filter { it.contains(addressText, ignoreCase = true) }.take(3)
    val shownSuggestions = suggestions.take(5)
    val hasSuggestions = isFocused && (filteredRecent.isNotEmpty() || shownSuggestions.isNotEmpty())

    BoxWithConstraints(modifier = modifier) {
        val fullHeightPx  = constraints.maxHeight.toFloat()
        val statusBarPx   = WindowInsets.statusBars.getTop(density).toFloat()
        val navBarPx      = WindowInsets.navigationBars.getBottom(density).toFloat()
        val imeBottomPx   = WindowInsets.ime.getBottom(density).toFloat()
        val bottomGapPx   = with(density) { 4.dp.toPx() }
        val topGapPx      = with(density) { 13.dp.toPx() }

        val pillHeightPx  = with(density) { 56.dp.toPx() }
        val pillBottomY   = fullHeightPx - maxOf(navBarPx, imeBottomPx) - bottomGapPx
        val maxExpandedH  = (pillBottomY - statusBarPx - topGapPx).coerceAtLeast(pillHeightPx)

        val heightAnim = remember { Animatable(pillHeightPx) }
        val liveDragOffsetState = remember { mutableStateOf(0f) }
        LaunchedEffect(isExpanded, isPanelOpen, maxExpandedH) {
            if (heightAnim.value > maxExpandedH) heightAnim.snapTo(maxExpandedH)
            heightAnim.animateTo(
                if (isPanelOpen || isExpanded) maxExpandedH else pillHeightPx,
                spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
            )
        }

        val hideExtraOffset by animateDpAsState(
            targetValue = if (isVisible || isExpanded || isPanelOpen) 0.dp else 120.dp,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
            label = "hideOffset"
        )

        val renderedHeight   = (heightAnim.value + liveDragOffsetState.value).coerceIn(pillHeightPx, maxExpandedH)
        val cornerRadiusDp   = with(density) { (renderedHeight / 2f).coerceAtMost(28.dp.toPx()).toDp() }
        val renderedHeightDp = with(density) { renderedHeight.toDp() }
        val navBarShape      = RoundedCornerShape(cornerRadiusDp)

        val panelProgress = ((renderedHeight - pillHeightPx) / (maxExpandedH - pillHeightPx)).coerceIn(0f, 1f)
        // Show the bookmarks/history panel while it's being dragged open or is open — but NOT
        // when the height growth is from the address bar expanding for search (isExpanded).
        val showPanel = (panelProgress > 0f || isPanelOpen) && !isExpanded

        val listState = rememberLazyListState()
        val isPanelOpenState = rememberUpdatedState(isPanelOpen)
        val onClosePanelState = rememberUpdatedState(onClosePanel)
        val maxExpandedHState = rememberUpdatedState(maxExpandedH)
        val pillHeightPxState = rememberUpdatedState(pillHeightPx)

        val nestedScrollConnection = remember(listState, heightAnim, liveDragOffsetState) {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    if (!isPanelOpenState.value || source != NestedScrollSource.Drag) return Offset.Zero
                    // Panel partially closing and user reverses upward → re-open before list scrolls.
                    val partiallyClosing = liveDragOffsetState.value < 0f
                    if (partiallyClosing && available.y < 0f) {
                        val prev = liveDragOffsetState.value
                        liveDragOffsetState.value = (prev - available.y).coerceAtMost(0f)
                        val consumed = prev - liveDragOffsetState.value
                        return Offset(0f, consumed)
                    }
                    // At top of list and user drags down → start closing.
                    val atTop = listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
                    if (atTop && available.y > 0f) {
                        val prev = liveDragOffsetState.value
                        liveDragOffsetState.value = (prev - available.y).coerceIn(
                            pillHeightPxState.value - heightAnim.value,
                            maxExpandedHState.value - heightAnim.value
                        )
                        val consumed = prev - liveDragOffsetState.value
                        return Offset(0f, consumed)
                    }
                    return Offset.Zero
                }

                override suspend fun onPreFling(available: Velocity): Velocity {
                    val offset = liveDragOffsetState.value
                    if (!isPanelOpenState.value || offset >= 0f) return Velocity.Zero
                    val pillH = pillHeightPxState.value
                    val maxH = maxExpandedHState.value
                    val current = (heightAnim.value + offset).coerceIn(pillH, maxH)
                    heightAnim.snapTo(current)
                    liveDragOffsetState.value = 0f
                    val mid = (pillH + maxH) / 2f
                    return if (current < mid || available.y > 800f) {
                        onClosePanelState.value()
                        heightAnim.animateTo(pillH, spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow))
                        available  // consume the downward fling
                    } else {
                        heightAnim.animateTo(maxH, spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow))
                        Velocity.Zero  // re-opening — let list fling if needed
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .padding(horizontal = horizontalPad)
                .fillMaxWidth()
                .height(renderedHeightDp)
                .offset {
                    IntOffset(
                        0,
                        (pillBottomY - renderedHeight + with(density) { hideExtraOffset.toPx() }).roundToInt()
                    )
                }
                .shadow(elevation = 12.dp, shape = navBarShape, clip = false)
                .clip(navBarShape)
                .background(surfaceColor)
                .nestedScroll(nestedScrollConnection)
                // Open gesture on the nav bar box itself — no Z-order overlay needed.
                // Only tracks upward drags so taps always reach the address bar.
                .pointerInput(Unit) {
                    val slopPx = 8.dp.toPx()
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        if (isPanelOpenState.value) return@awaitEachGesture
                        val vt = VelocityTracker()
                        vt.addPointerInputChange(down)
                        var cumDelta = 0f
                        var tracking = false
                        dragLoop@ while (true) {
                            val event = awaitPointerEvent()
                            for (change in event.changes) {
                                vt.addPointerInputChange(change)
                                if (!change.pressed) break@dragLoop
                                val delta = change.position.y - change.previousPosition.y
                                cumDelta += delta
                                if (!tracking && cumDelta < -slopPx) tracking = true
                                if (tracking) {
                                    change.consume()
                                    liveDragOffsetState.value = (liveDragOffsetState.value - delta).coerceIn(
                                        pillHeightPxState.value - heightAnim.value,
                                        maxExpandedHState.value - heightAnim.value
                                    )
                                }
                            }
                        }
                        if (tracking) {
                            val velocity = vt.calculateVelocity().y
                            scope.launch {
                                val current = (heightAnim.value + liveDragOffsetState.value)
                                    .coerceIn(pillHeightPxState.value, maxExpandedHState.value)
                                heightAnim.snapTo(current)
                                liveDragOffsetState.value = 0f
                                val mid = (pillHeightPxState.value + maxExpandedHState.value) / 2f
                                if (current > mid || velocity < -800f) {
                                    onOpenPanel()
                                } else {
                                    heightAnim.animateTo(
                                        pillHeightPxState.value,
                                        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
                                    )
                                }
                            }
                        }
                    }
                }
        ) {
            if (menuTransition.currentState || menuTransition.targetState) {
                val rowHeightPx = with(density) { 56.dp.toPx() }
                val menuGapPx = with(density) { 8.dp.toPx() }
                Popup(
                    alignment = if (isExpanded && !isPanelOpen) Alignment.TopStart else Alignment.BottomStart,
                    offset = when {
                        isExpanded && !isPanelOpen -> IntOffset(0, (rowHeightPx + menuGapPx).roundToInt())
                        isPanelOpen               -> IntOffset(0, -(rowHeightPx + menuGapPx).roundToInt())
                        else                      -> IntOffset(0, -(renderedHeight + menuGapPx).roundToInt())
                    },
                    onDismissRequest = { showMenu = false },
                    properties = PopupProperties(focusable = true)
                ) {
                    AnimatedVisibility(
                        visibleState = menuTransition,
                        enter = scaleIn(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            transformOrigin = if (isExpanded) TransformOrigin(0f, 0f) else TransformOrigin(0f, 1f)
                        ) + fadeIn(tween(150)),
                        exit = scaleOut(
                            animationSpec = tween(120),
                            targetScale = 0.85f,
                            transformOrigin = if (isExpanded) TransformOrigin(0f, 0f) else TransformOrigin(0f, 1f)
                        ) + fadeOut(tween(120))
                    ) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            tonalElevation = 3.dp,
                            shadowElevation = 8.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .width(IntrinsicSize.Max)
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                BrowserMenuItem(
                                    icon = if (isLoading) Icons.Default.Stop else Icons.Default.Refresh,
                                    label = if (isLoading) "Stop" else "Refresh",
                                    onClick = { onRefreshOrStop(); showMenu = false }
                                )
                                BrowserMenuItem(
                                    icon = Icons.Default.Add,
                                    label = "New tab",
                                    onClick = { onNewTab(); showMenu = false }
                                )
                                BrowserMenuItem(
                                    icon = if (isBookmarked) Icons.Default.Bookmark else Icons.Outlined.Bookmark,
                                    label = if (isBookmarked) "Remove bookmark" else "Add bookmark",
                                    onClick = { onToggleBookmark(); showMenu = false }
                                )
                                BrowserMenuItem(
                                    icon = Icons.Default.FindInPage,
                                    label = "Find in page",
                                    onClick = { onFindInPage(); showMenu = false }
                                )
                                BrowserMenuItem(
                                    icon = Icons.Default.DesktopWindows,
                                    label = if (isDesktopSite) "Request mobile site" else "Request desktop site",
                                    onClick = { onToggleDesktopSite(); showMenu = false }
                                )
                                BrowserMenuItem(
                                    icon = Icons.Default.Share,
                                    label = "Share",
                                    onClick = { onShare(); showMenu = false }
                                )
                                BrowserMenuItem(
                                    icon = Icons.Default.Code,
                                    label = if (isJavaScriptEnabled) "Disable JavaScript" else "Enable JavaScript",
                                    onClick = { onToggleJavaScript(); showMenu = false }
                                )
                                BrowserMenuItem(
                                    icon = Icons.Default.Brush,
                                    label = "Custom CSS",
                                    onClick = { onCustomCss(); showMenu = false }
                                )
                                BrowserMenuItem(
                                    icon = Icons.Default.Settings,
                                    label = "Settings",
                                    onClick = { onOpenSettings(); showMenu = false }
                                )
                            }
                        }
                    }
                }
            }
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
                PredictiveBackHandler(enabled = isPanelOpen) { progress ->
                    try {
                        progress.collect { backEvent ->
                            heightAnim.snapTo(
                                maxExpandedH - (maxExpandedH - pillHeightPx) * backEvent.progress
                            )
                        }
                        onClosePanel()
                    } catch (e: CancellationException) {
                        heightAnim.animateTo(
                            maxExpandedH,
                            spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
                        )
                    }
                }
                if (panelProgress < 1f || isExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer { alpha = if (isExpanded) 1f else (1f - panelProgress * 2f).coerceAtLeast(0f) }
                    ) {
                        NavBarRow(
                            onShowMenu = { showMenu = true },
                            addressText = addressText,
                            onAddressTextChange = onAddressTextChange,
                            onNavigate = onNavigate,
                            isFocused = isFocused,
                            onFocusChanged = { focused ->
                                if (focused && !isFocused) onFocusGained()
                                else if (!focused && isFocused) onFocusLost()
                                isFocused = focused
                            },
                            onCloseSearch = onCloseSearch,
                        )
                        if (hasSuggestions && !isPanelOpen) {
                            Column(modifier = Modifier.padding(bottom = 8.dp)) {
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                                filteredRecent.forEach { query ->
                                    SuggestionRow(
                                        icon = Icons.Default.History,
                                        text = query,
                                        onFillText = { onFillText(query) },
                                        onNavigate = { onSuggestionNavigate(query) }
                                    )
                                }
                                if (filteredRecent.isNotEmpty() && shownSuggestions.isNotEmpty()) {
                                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                                }
                                shownSuggestions.forEach { suggestion ->
                                    SuggestionRow(
                                        icon = Icons.Default.Search,
                                        text = suggestion,
                                        onFillText = { onFillText(suggestion) },
                                        onNavigate = { onSuggestionNavigate(suggestion) }
                                    )
                                }
                            }
                        }
                    }
                }
                if (showPanel) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = panelProgress }
                    ) {
                        BrowserPanel(
                            modifier = Modifier.fillMaxSize(),
                            listState = listState,
                            onNavigate = onPanelNavigate
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NavBarRow(
    onShowMenu: () -> Unit,
    addressText: String,
    onAddressTextChange: (String) -> Unit,
    onNavigate: () -> Unit,
    isFocused: Boolean,
    onFocusChanged: (Boolean) -> Unit,
    onCloseSearch: () -> Unit = {},
) {
    val fm = LocalFocusManager.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isFocused) {
            IconButton(onClick = onCloseSearch) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Close search")
            }
        } else {
            IconButton(onClick = onShowMenu) {
                Icon(Icons.Default.MoreVert, contentDescription = "More")
            }
        }
        BasicTextField(
            value = addressText,
            onValueChange = onAddressTextChange,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp)
                .onFocusChanged { state ->
                    onFocusChanged(state.isFocused)
                }
                .onKeyEvent { event ->
                    if (event.key == Key.Enter && event.type == KeyEventType.KeyUp) {
                        onNavigate(); true
                    } else false
                },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { onNavigate() }),
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            ),
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.Center) {
                    if (addressText.isEmpty()) {
                        Text(
                            text = "Search or enter URL",
                            style = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                    innerTextField()
                }
            }
        )
        if (isFocused) {
            IconButton(onClick = {
                if (addressText.isEmpty()) onCloseSearch()
                else onAddressTextChange("")
            }) {
                Icon(Icons.Default.Close, contentDescription = "Clear")
            }
        } else {
            IconButton(onClick = onNavigate) {
                Icon(Icons.Default.Search, contentDescription = "Go")
            }
        }
    }
}

@Composable
private fun BrowserPanel(
    onNavigate: (String) -> Unit,
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var bookmarks by remember { mutableStateOf(emptyList<Pair<String, String>>()) }
    var history   by remember { mutableStateOf(emptyList<Pair<String, String>>()) }

    LaunchedEffect(Unit) {
        val (bms, hist) = withContext(Dispatchers.IO) {
            BookmarksDatabase.get(context).getAll() to
                HistoryDatabase.get(context).search("", limit = 30)
        }
        bookmarks = bms
        history   = hist
    }

    val surfaceColor = MaterialTheme.colorScheme.surfaceContainerHigh
    if (bookmarks.isEmpty() && history.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                "No bookmarks or history yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        Box(modifier = modifier) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                state = listState,
                contentPadding = PaddingValues(top = 40.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (bookmarks.isNotEmpty()) {
                    item(key = "bookmarks_favicons") {
                        Text(
                            "Bookmarks",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(start = 4.dp, end = 4.dp, bottom = 8.dp)
                        ) {
                            items(bookmarks, key = { "fav_${it.first}" }) { (url, title) ->
                                BookmarkFaviconItem(
                                    url = url,
                                    title = title,
                                    onClick = { onNavigate(url) }
                                )
                            }
                        }
                    }
                }
                if (history.isNotEmpty()) {
                    item(key = "history_header") {
                        if (bookmarks.isNotEmpty()) HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Text(
                            "History",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                        )
                    }
                    items(history, key = { "h_${it.first}" }) { (url, title) ->
                        PanelRow(
                            title = title,
                            url = url,
                            onClick = { onNavigate(url) },
                            onDelete = null
                        )
                    }
                }
            }
            Box(Modifier.fillMaxWidth().height(40.dp).align(Alignment.TopCenter)
                .background(Brush.verticalGradient(listOf(surfaceColor, surfaceColor.copy(alpha = 0f)))))
            Box(Modifier.fillMaxWidth().height(40.dp).align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(surfaceColor.copy(alpha = 0f), surfaceColor))))
        }
    }
}

@Composable
private fun BookmarkFaviconItem(url: String, title: String, onClick: () -> Unit) {
    val domain = remember(url) {
        try { Uri.parse(url).host?.removePrefix("www.") ?: "" } catch (e: Exception) { "" }
    }
    // Shared first-party resolver (parses <link rel=icon>, falls back to /favicon.ico, caches per
    // domain) — same path the tab uses, so e.g. Polygon's CDN-hosted icon resolves here too.
    val favicon by produceState<Bitmap?>(Favicons.cached(domain), domain) {
        if (domain.isEmpty()) return@produceState
        Favicons.cached(domain)?.let { value = it; return@produceState }
        value = withContext(Dispatchers.IO) { Favicons.fetch(url) }
    }
    Column(
        modifier = Modifier.width(64.dp).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center
        ) {
            if (favicon != null) {
                androidx.compose.foundation.Image(
                    bitmap             = favicon!!.asImageBitmap(),
                    contentDescription = title,
                    modifier           = Modifier.fillMaxSize(),
                    contentScale       = ContentScale.Crop
                )
            } else {
                Text(
                    text  = domain.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text     = domain.ifEmpty { title },
            style    = MaterialTheme.typography.labelSmall,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun PanelRow(
    title: String,
    url: String,
    onClick: () -> Unit,
    onDelete: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(
                start = 12.dp,
                end = if (onDelete != null) 4.dp else 12.dp,
                top = 8.dp,
                bottom = 8.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                url,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (onDelete != null) {
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.Delete, null,
                    Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FindBar(
    modifier: Modifier = Modifier,
    query: String,
    activeMatch: Int,
    totalMatches: Int,
    onQueryChange: (String) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onClose: () -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .height(48.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Search,
                null,
                Modifier
                    .size(18.dp)
                    .padding(start = 4.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onNext() }),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) {
                            Text(
                                "Find in page",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        inner()
                    }
                }
            )
            if (totalMatches > 0) {
                Text(
                    "$activeMatch/$totalMatches",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
            IconButton(onClick = onPrevious) {
                Icon(Icons.Default.KeyboardArrowUp, "Previous", Modifier.size(20.dp))
            }
            IconButton(onClick = onNext) {
                Icon(Icons.Default.KeyboardArrowDown, "Next", Modifier.size(20.dp))
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, "Close", Modifier.size(20.dp))
            }
        }
    }
}

// A pending site permission prompt (geolocation / notifications / camera / mic, etc.).
private data class PermPrompt(val origin: String, val action: String, val onResult: (Boolean) -> Unit)

private fun originOf(uri: String): String = runCatching {
    val u = Uri.parse(uri)
    if (!u.host.isNullOrBlank()) "${u.scheme}://${u.host}" else uri
}.getOrDefault(uri)

private fun contentActionFor(permission: Int): String = when (permission) {
    GeckoSession.PermissionDelegate.PERMISSION_GEOLOCATION -> "use your location"
    GeckoSession.PermissionDelegate.PERMISSION_DESKTOP_NOTIFICATION -> "show notifications"
    GeckoSession.PermissionDelegate.PERMISSION_PERSISTENT_STORAGE -> "store data on your device"
    GeckoSession.PermissionDelegate.PERMISSION_MEDIA_KEY_SYSTEM_ACCESS -> "play protected (DRM) content"
    else -> "access a device feature"
}

// Material seed presets for the "Theme color" picker (Website / System / a fixed accent).
private val themePresetColors = listOf(
    0xFF1A73E8.toInt(), // blue
    0xFF009688.toInt(), // teal
    0xFF43A047.toInt(), // green
    0xFFFB8C00.toInt(), // amber
    0xFFE91E63.toInt(), // pink
    0xFF7E57C2.toInt(), // purple
)

private fun cssHex(color: Int): String = "#%06X".format(0xFFFFFF and color)

@Composable
private fun ColorPickerDialog(initial: Int, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    val start = remember { FloatArray(3).also { android.graphics.Color.colorToHSV(initial, it) } }
    var hue by remember { mutableStateOf(start[0]) }
    var sat by remember { mutableStateOf(start[1]) }
    var value by remember { mutableStateOf(start[2]) }
    val current = android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, value))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pick a color") },
        confirmButton = { TextButton(onClick = { onPick(current) }) { Text("Apply") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Saturation (x) / brightness (y) panel for the current hue.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .pointerInput(Unit) {
                            detectTapGestures { o ->
                                sat = (o.x / size.width).coerceIn(0f, 1f)
                                value = (1f - o.y / size.height).coerceIn(0f, 1f)
                            }
                        }
                        .pointerInput(Unit) {
                            detectDragGestures { change, _ ->
                                sat = (change.position.x / size.width).coerceIn(0f, 1f)
                                value = (1f - change.position.y / size.height).coerceIn(0f, 1f)
                            }
                        }
                ) {
                    val hueColor = ComposeColor(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f)))
                    Canvas(Modifier.matchParentSize()) {
                        drawRect(Brush.horizontalGradient(listOf(ComposeColor.White, hueColor)))
                        drawRect(Brush.verticalGradient(listOf(ComposeColor.Transparent, ComposeColor.Black)))
                        drawCircle(
                            ComposeColor.White,
                            radius = 12f,
                            center = Offset(sat * size.width, (1f - value) * size.height),
                            style = Stroke(width = 3f)
                        )
                    }
                }
                // Hue strip.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(26.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .pointerInput(Unit) {
                            detectTapGestures { o -> hue = (o.x / size.width * 360f).coerceIn(0f, 360f) }
                        }
                        .pointerInput(Unit) {
                            detectDragGestures { c, _ -> hue = (c.position.x / size.width * 360f).coerceIn(0f, 360f) }
                        }
                ) {
                    Canvas(Modifier.matchParentSize()) {
                        val hues = (0..6).map {
                            ComposeColor(android.graphics.Color.HSVToColor(floatArrayOf(it * 60f, 1f, 1f)))
                        }
                        drawRect(Brush.horizontalGradient(hues))
                        val x = hue / 360f * size.width
                        drawLine(ComposeColor.White, Offset(x, 0f), Offset(x, size.height), strokeWidth = 3f)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier.size(36.dp).clip(CircleShape).background(ComposeColor(current))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                    )
                    Text(
                        cssHex(current),
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CssSheet(
    domain: String,
    initialCss: String,
    pageContext: String = "",
    onSave: (String) -> Unit,
    onReset: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    var css by remember { mutableStateOf(initialCss) }
    var prompt by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<Pair<String, Int>?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        windowInsets = WindowInsets(0)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Custom CSS", style = MaterialTheme.typography.titleMedium)
            Text(
                domain.ifBlank { "this page" },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            // AI prompt — generate/edit the CSS below from a natural-language request (Claude via Deck).
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Ask AI to restyle this site…") },
                    enabled = !busy,
                    maxLines = 3
                )
                IconButton(
                    onClick = {
                        val p = prompt.trim()
                        if (p.isEmpty() || busy) return@IconButton
                        busy = true; error = null
                        scope.launch {
                            val result = withContext(Dispatchers.IO) {
                                val creds = DeckCredentials.read(context)
                                    ?: return@withContext Result.failure<String>(
                                        RuntimeException("No Claude key found in Deck. Set it in Deck settings.")
                                    )
                                AnthropicCssClient.generate(
                                    creds.first, creds.second, domain.ifBlank { "this page" }, css, p, pageContext
                                )
                            }
                            busy = false
                            result.onSuccess { css = it; prompt = "" }
                                .onFailure { error = it.message ?: "Generation failed." }
                        }
                    },
                    enabled = !busy && prompt.isNotBlank()
                ) {
                    if (busy) CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    else Icon(Icons.Default.AutoAwesome, contentDescription = "Generate CSS")
                }
            }
            error?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            // CSS editor (left, same width as the AI field above) + a right gutter of color swatches,
            // each aligned to the line its color appears on. Tap a swatch to edit it.
            val cssPad = 12.dp
            var cssLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
            val colorHits = remember(css) { CssColors.findAll(css) }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 180.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                        .padding(cssPad)
                ) {
                    if (css.isEmpty()) {
                        Text(
                            "/* CSS applied to ${domain.ifBlank { "this page" }} */",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    BasicTextField(
                        value = css,
                        onValueChange = { css = it },
                        onTextLayout = { cssLayout = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                    )
                }
                // Gutter width matches the AI row's icon button so the CSS box == AI field width.
                Box(modifier = Modifier.width(48.dp).heightIn(min = 180.dp)) {
                    val layout = cssLayout
                    if (layout != null) {
                        colorHits.forEach { (token, color, offset) ->
                            val box = runCatching {
                                layout.getBoundingBox(offset.coerceIn(0, layout.layoutInput.text.length))
                            }.getOrNull()
                            if (box != null) {
                                val yDp = with(density) { (cssPad.toPx() + box.center.y).toDp() } - 12.dp
                                Box(
                                    modifier = Modifier
                                        .offset(x = 12.dp, y = yDp)
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(ComposeColor(color))
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                        .clickable { editing = token to color }
                                )
                            }
                        }
                    }
                }
            }
            editing?.let { (token, value) ->
                ColorPickerDialog(
                    initial = value,
                    onPick = { picked ->
                        // Replace this token wherever it appears. Hex: guard against matching a prefix
                        // of a longer hex (#fff in #ffffff). Named color: whole-word only.
                        val pattern = when {
                            token.startsWith("#") -> Regex.escape(token) + "(?![0-9a-fA-F])"
                            token.startsWith("rgb", ignoreCase = true) -> Regex.escape(token)
                            else -> "\\b" + Regex.escape(token) + "\\b"
                        }
                        css = css.replace(Regex(pattern, RegexOption.IGNORE_CASE), cssHex(picked))
                        editing = null
                    },
                    onDismiss = { editing = null }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { css = ""; onReset() },
                    enabled = domain.isNotBlank()
                ) { Text("Reset", color = MaterialTheme.colorScheme.error) }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Button(
                    onClick = { onSave(css); onDismiss() },
                    enabled = domain.isNotBlank()
                ) { Text("Save") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsSheet(
    transparent: Boolean,
    onTransparentChange: (Boolean) -> Unit,
    onSetDefaultBrowser: () -> Unit,
    themeMode: String = "website",
    themeFixedColor: Int = 0,
    onSelectThemeWebsite: () -> Unit = {},
    onSelectThemeSystem: () -> Unit = {},
    onSelectThemeColor: (Int) -> Unit = {},
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        windowInsets = WindowInsets(0)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Settings", style = MaterialTheme.typography.titleMedium)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Notification bar",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = transparent,
                        onClick = { onTransparentChange(true) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) { Text("Transparent") }
                    SegmentedButton(
                        selected = !transparent,
                        onClick = { onTransparentChange(false) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) { Text("Opaque") }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Theme color",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = themeMode == "website",
                        onClick = onSelectThemeWebsite,
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) { Text("Website") }
                    SegmentedButton(
                        selected = themeMode == "system",
                        onClick = onSelectThemeSystem,
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) { Text("System") }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    themePresetColors.forEach { c ->
                        val selected = themeMode == "fixed" && themeFixedColor == c
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(ComposeColor(c))
                                .border(
                                    width = if (selected) 3.dp else 1.dp,
                                    color = if (selected) MaterialTheme.colorScheme.onSurface
                                            else MaterialTheme.colorScheme.outlineVariant,
                                    shape = CircleShape
                                )
                                .clickable { onSelectThemeColor(c) }
                        )
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSetDefaultBrowser(); onDismiss() }
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.Language, null, Modifier.size(20.dp))
                Text("Set as default browser", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun BrowserMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

@Composable
private fun LongPressContextMenu(
    visibleState: MutableTransitionState<Boolean>,
    offsetX: Int,
    offsetY: Int,
    linkUrl: String?,
    imageUrl: String?,
    onDismiss: () -> Unit,
    onNewTab: (String) -> Unit,
    onCopyLink: (String) -> Unit,
    onDownloadImage: (String) -> Unit
) {
    Popup(
        alignment = Alignment.TopStart,
        offset = IntOffset(offsetX, offsetY),
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        AnimatedVisibility(
            visibleState = visibleState,
            enter = scaleIn(
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                transformOrigin = TransformOrigin(0f, 0f)
            ) + fadeIn(tween(150)),
            exit = scaleOut(
                animationSpec = tween(120),
                targetScale = 0.85f,
                transformOrigin = TransformOrigin(0f, 0f)
            ) + fadeOut(tween(120))
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                tonalElevation = 3.dp,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .width(IntrinsicSize.Max)
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    val displayUrl = linkUrl ?: imageUrl
                    if (displayUrl != null) {
                        Text(
                            text = displayUrl,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .widthIn(max = 220.dp)
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                        HorizontalDivider(modifier = Modifier.padding(bottom = 2.dp))
                    }
                    if (linkUrl != null) {
                        BrowserMenuItem(Icons.Default.OpenInNew, "Open in new tab") { onNewTab(linkUrl) }
                        BrowserMenuItem(Icons.Default.ContentCopy, "Copy link") { onCopyLink(linkUrl) }
                    }
                    if (imageUrl != null && imageUrl != linkUrl) {
                        BrowserMenuItem(Icons.Default.ContentCopy, "Copy image URL") { onCopyLink(imageUrl) }
                        BrowserMenuItem(Icons.Default.Download, "Download image") { onDownloadImage(imageUrl) }
                    } else if (imageUrl != null) {
                        BrowserMenuItem(Icons.Default.Download, "Download image") { onDownloadImage(imageUrl) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionRow(
    icon: ImageVector,
    text: String,
    onFillText: () -> Unit,
    onNavigate: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onNavigate)
            .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon, null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        )
        IconButton(onClick = onFillText, modifier = Modifier.size(40.dp)) {
            Icon(
                Icons.Default.NorthEast, null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun simplifyUrl(url: String): String {
    if (url.isEmpty()) return ""
    return try {
        val host = Uri.parse(url).host?.removePrefix("www.") ?: return url
        if (host == "start.duckduckgo.com") "" else host
    } catch (e: Exception) {
        url
    }
}

private fun normalizeUrl(input: String): String {
    val trimmed = input.trim()
    return when {
        trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
        trimmed.contains(".") && !trimmed.contains(" ") -> "https://$trimmed"
        else -> "https://duckduckgo.com/?q=${Uri.encode(trimmed)}"
    }
}

// Stream a GeckoView external response (download) to the Downloads collection. The WebResponse body
// is already authenticated by the engine, so unlike the old WebView path there's no cookie dance.
private fun downloadWebResponse(context: Context, response: org.mozilla.geckoview.WebResponse) {
    val body = response.body ?: return
    val cd = response.headers["Content-Disposition"]
    val mime = response.headers["Content-Type"]
    val fileName = android.webkit.URLUtil.guessFileName(response.uri, cd, mime)
    Thread {
        val ok = runCatching {
            val resolver = context.contentResolver
            val values = android.content.ContentValues().apply {
                put(android.provider.MediaStore.Downloads.DISPLAY_NAME, fileName)
                if (!mime.isNullOrBlank()) put(android.provider.MediaStore.Downloads.MIME_TYPE, mime.substringBefore(';').trim())
            }
            val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: error("no download uri")
            resolver.openOutputStream(uri)?.use { out -> body.use { it.copyTo(out) } }
        }.isSuccess
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            android.widget.Toast.makeText(
                context,
                if (ok) "Downloaded $fileName" else "Download failed",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }.start()
}

private fun parseRgbColor(jsValue: String): Int? {
    val match = Regex("""rgba?\((\d+),\s*(\d+),\s*(\d+)""").find(jsValue) ?: return null
    val (r, g, b) = match.destructured
    return Color.rgb(r.toInt(), g.toInt(), b.toInt())
}

private suspend fun fetchSuggestions(query: String): List<String> = withContext(Dispatchers.IO) {
    try {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val conn = URL("https://duckduckgo.com/ac/?q=$encoded&type=list").openConnection() as HttpURLConnection
        conn.connectTimeout = 3000
        conn.readTimeout = 3000
        conn.setRequestProperty("Accept", "application/json")
        val response = conn.inputStream.bufferedReader().readText()
        conn.disconnect()
        // Response: ["query", ["suggestion1", "suggestion2", ...], ...]
        val match = Regex("""\["[^"]*",\[([^\]]*)\]""").find(response) ?: return@withContext emptyList()
        match.groupValues[1]
            .split(",")
            .map { it.trim().trim('"') }
            .filter { it.isNotEmpty() }
            .take(6)
    } catch (_: Exception) { emptyList() }
}

private fun getRecentQueries(prefs: SharedPreferences): List<String> {
    val stored = prefs.getString("recent_queries", "") ?: ""
    return if (stored.isEmpty()) emptyList()
           else stored.split("\n").filter { it.isNotEmpty() }
}

private fun saveRecentQuery(prefs: SharedPreferences, input: String) {
    val trimmed = input.trim()
    if (trimmed.isEmpty()) return
    val current = getRecentQueries(prefs).toMutableList()
    current.remove(trimmed)
    current.add(0, trimmed)
    prefs.edit().putString("recent_queries", current.take(20).joinToString("\n")).apply()
}
