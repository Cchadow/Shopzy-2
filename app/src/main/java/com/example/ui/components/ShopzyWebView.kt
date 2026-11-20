package com.example.ui.components

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import androidx.core.net.toUri
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.R
import com.example.notifications.NotificationPreferences
import com.example.notifications.NotificationSettings
import com.example.notifications.ShopzyNotificationHelper
import com.example.notifications.ShopzyWebNotificationBridge
import com.example.ui.theme.ShopzyOrange
import java.io.File
import kotlinx.coroutines.launch

const val SHOPZY_TARGET_URL = "https://shopzyhaiti.lovable.app"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ShopzyWebView(
  onLoadError: () -> Unit,
  onLoadSuccess: () -> Unit,
  pendingNavigationUrl: String? = null,
  onPendingNavigationHandled: () -> Unit = {},
  openNotificationSettingsTrigger: Boolean = false,
  onNotificationSettingsTriggerHandled: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val coroutineScope = rememberCoroutineScope()

  val notificationPrefs = remember(context) { NotificationPreferences(context) }
  val notificationSettings by notificationPrefs.settingsFlow.collectAsStateWithLifecycle(
    initialValue = NotificationSettings()
  )

  var hasOsPermission by remember {
    mutableStateOf(ShopzyNotificationHelper.areNotificationsEnabled(context))
  }
  var showSettingsSheet by remember { mutableStateOf(false) }
  var pendingPermissionCallback by remember { mutableStateOf<((Boolean) -> Unit)?>(null) }

  var webViewRef by remember { mutableStateOf<WebView?>(null) }
  var canGoBack by remember { mutableStateOf(false) }
  var loadingProgress by remember { mutableFloatStateOf(0f) }
  var isPageLoading by remember { mutableStateOf(true) }
  var isPullRefreshing by remember { mutableStateOf(false) }

  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { granted ->
    val enabled = granted && ShopzyNotificationHelper.areNotificationsEnabled(context)
    hasOsPermission = enabled
    pendingPermissionCallback?.invoke(enabled)
    pendingPermissionCallback = null
  }

  val requestOsPermissionFlow: ((Boolean) -> Unit) -> Unit = remember(context) {
    { onResult ->
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        !ShopzyNotificationHelper.hasPostNotificationPermission(context)
      ) {
        pendingPermissionCallback = onResult
        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
      } else {
        val enabled = ShopzyNotificationHelper.areNotificationsEnabled(context)
        hasOsPermission = enabled
        onResult(enabled)
      }
    }
  }

  val notificationBridge: ShopzyWebNotificationBridge = remember(context) {
    ShopzyWebNotificationBridge(
      context = context,
      coroutineScope = coroutineScope,
      onRequestOsPermission = requestOsPermissionFlow
    )
  }

  // Refresh OS permission status when returning from System Settings
  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        hasOsPermission = ShopzyNotificationHelper.areNotificationsEnabled(context)
        notificationBridge.syncPermissionToWeb(webViewRef)
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }

  // Prompt notification permission on Android 13+ when entering the WebView, like Chrome
  LaunchedEffect(Unit) {
    ShopzyNotificationHelper.ensureDefaultChannel(context, notificationSettings)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
      !ShopzyNotificationHelper.hasPostNotificationPermission(context)
    ) {
      permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
  }

  // Keep Web Notification permission synced when user toggles settings or OS permission
  LaunchedEffect(hasOsPermission, notificationSettings.enabled) {
    notificationBridge.syncPermissionToWeb(webViewRef)
  }

  // Handle deep-link URLs delivered by tapping a notification
  LaunchedEffect(pendingNavigationUrl, webViewRef) {
    val url = pendingNavigationUrl
    val wv = webViewRef
    if (!url.isNullOrBlank() && wv != null) {
      wv.loadUrl(url)
      onPendingNavigationHandled()
    }
  }

  // Open customization sheet when "Customize" action on a notification is tapped
  LaunchedEffect(openNotificationSettingsTrigger) {
    if (openNotificationSettingsTrigger) {
      showSettingsSheet = true
      onNotificationSettingsTriggerHandled()
    }
  }

  BackHandler(enabled = canGoBack) {
    webViewRef?.let {
      if (it.canGoBack()) {
        it.goBack()
      }
    }
  }

  DisposableEffect(Unit) {
    onDispose {
      webViewRef?.let { wv ->
        wv.stopLoading()
        wv.removeJavascriptInterface(ShopzyWebNotificationBridge.JS_INTERFACE_NAME)
        (wv.parent as? ViewGroup)?.removeView(wv)
        wv.destroy()
      }
      webViewRef = null
    }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .windowInsetsPadding(WindowInsets.safeDrawing)
  ) {
      AndroidView(
        factory = { ctx ->
          try {
            val codeCacheDir = File(ctx.cacheDir, "WebView/Default/HTTP Cache/Code Cache")
            File(codeCacheDir, "js").mkdirs()
            File(codeCacheDir, "wasm").mkdirs()
          } catch (_: Exception) {}

          val wv = WebView(ctx).apply {
            layoutParams = ViewGroup.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT,
              ViewGroup.LayoutParams.MATCH_PARENT
            )

            settings.apply {
              javaScriptEnabled = true
              domStorageEnabled = true
              cacheMode = WebSettings.LOAD_DEFAULT
              useWideViewPort = true
              loadWithOverviewMode = true
              setSupportZoom(true)
              builtInZoomControls = true
              displayZoomControls = false
              allowFileAccess = true
              mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            }

            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            cookieManager.setAcceptThirdPartyCookies(this, true)

            notificationBridge.registerOn(this)

            webViewClient = object : WebViewClient() {
              override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                isPageLoading = true
                ShopzyWebNotificationBridge.injectPolyfill(view)
              }

              override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                isPageLoading = false
                isPullRefreshing = false
                canGoBack = view?.canGoBack() ?: false
                ShopzyWebNotificationBridge.injectPolyfill(view)
                notificationBridge.syncPermissionToWeb(view)
                onLoadSuccess()
              }

              override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
              ) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true) {
                  isPageLoading = false
                  isPullRefreshing = false
                  onLoadError()
                }
              }

              override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
              ): Boolean {
                val url = request?.url?.toString() ?: return false
                return handleUrl(ctx, url)
              }

              private fun handleUrl(ctx: android.content.Context, url: String): Boolean {
                if (url.startsWith("tel:") ||
                  url.startsWith("mailto:") ||
                  url.startsWith("whatsapp:") ||
                  url.startsWith("sms:")
                ) {
                  return try {
                    val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                    ctx.startActivity(intent)
                    true
                  } catch (_: Exception) {
                    false
                  }
                }
                return false
              }
            }

            webChromeClient = object : WebChromeClient() {
              override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                loadingProgress = newProgress / 100f
                if (newProgress >= 100) {
                  isPageLoading = false
                  isPullRefreshing = false
                }
              }
            }

            loadUrl(pendingNavigationUrl ?: SHOPZY_TARGET_URL)
          }

          webViewRef = wv

          SwipeRefreshLayout(ctx).apply {
            layoutParams = ViewGroup.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT,
              ViewGroup.LayoutParams.MATCH_PARENT
            )
            setColorSchemeColors(ShopzyOrange.toArgb())
            setProgressBackgroundColorSchemeColor(Color.Black.toArgb())
            setOnChildScrollUpCallback { _, _ ->
              wv.scrollY > 0 || wv.canScrollVertically(-1)
            }
            setOnRefreshListener {
              if (!com.example.util.NetworkUtils.isOnline(ctx)) {
                isPullRefreshing = false
                isRefreshing = false
                onLoadError()
              } else {
                isPullRefreshing = true
                wv.reload()
              }
            }
            addView(wv)
          }
        },
        update = { swipeRefreshLayout ->
          if (swipeRefreshLayout.isRefreshing != isPullRefreshing) {
            swipeRefreshLayout.isRefreshing = isPullRefreshing
          }
          val childWebView = swipeRefreshLayout.getChildAt(0) as? WebView
          if (childWebView != null) {
            webViewRef = childWebView
            notificationBridge.attachWebView(childWebView)
          }
        },
        modifier = Modifier
          .fillMaxSize()
          .testTag("shopzy_webview")
      )

      // Subtle top loading indicator overlay while the page loads (hidden during pull-to-refresh)
      AnimatedVisibility(
        visible = isPageLoading && !isPullRefreshing && loadingProgress < 1f,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.align(Alignment.TopCenter)
      ) {
        LinearProgressIndicator(
          progress = { loadingProgress },
          modifier = Modifier
            .fillMaxWidth()
            .height(3.dp),
          color = ShopzyOrange,
          trackColor = Color.Transparent
        )
      }

  }

  if (showSettingsSheet) {
    NotificationSettingsSheet(
      settings = notificationSettings,
      hasOsPermission = hasOsPermission,
      onDismiss = { showSettingsSheet = false },
      onRequestOsPermission = {
        requestOsPermissionFlow { granted ->
          hasOsPermission = granted
        }
      },
      onToggleEnabled = { enabled ->
        coroutineScope.launch { notificationPrefs.setEnabled(enabled) }
      },
      onToggleSound = { sound ->
        coroutineScope.launch { notificationPrefs.setSoundEnabled(sound) }
      },
      onToggleVibration = { vibration ->
        coroutineScope.launch { notificationPrefs.setVibrationEnabled(vibration) }
      },
      onToggleHeadsUp = { headsUp ->
        coroutineScope.launch { notificationPrefs.setHeadsUpEnabled(headsUp) }
      },
      onToggleExpanded = { expanded ->
        coroutineScope.launch { notificationPrefs.setExpandedStyle(expanded) }
      },
      onSelectAccent = { accent ->
        coroutineScope.launch { notificationPrefs.setAccent(accent) }
      },
      onSendTestNotification = {
        coroutineScope.launch {
          ShopzyNotificationHelper.showWebNotification(
            context = context,
            title = context.getString(R.string.notification_test_title),
            body = context.getString(R.string.notification_test_body),
            targetUrl = SHOPZY_TARGET_URL
          )
        }
      }
    )
  }
}
