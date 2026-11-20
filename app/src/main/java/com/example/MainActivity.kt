package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.example.notifications.ShopzyNotificationHelper
import com.example.ui.components.OfflineScreen
import com.example.ui.components.ShopzyWebView
import com.example.ui.components.SplashScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.util.NetworkUtils
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ScreenState {
  SPLASH,
  WEBVIEW,
  OFFLINE
}

class MainActivity : ComponentActivity() {
  private var pendingNotificationUrl by mutableStateOf<String?>(null)
  private var openNotificationSettings by mutableStateOf(false)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    handleNotificationIntent(intent)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        ShopzyAppRoot(
          pendingNotificationUrl = pendingNotificationUrl,
          onPendingNotificationUrlHandled = { pendingNotificationUrl = null },
          openNotificationSettingsTrigger = openNotificationSettings,
          onNotificationSettingsTriggerHandled = { openNotificationSettings = false }
        )
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    handleNotificationIntent(intent)
  }

  private fun handleNotificationIntent(intent: Intent?) {
    if (intent == null) return
    val url = intent.getStringExtra(ShopzyNotificationHelper.EXTRA_NOTIFICATION_URL)
    if (!url.isNullOrBlank()) {
      pendingNotificationUrl = url
      intent.removeExtra(ShopzyNotificationHelper.EXTRA_NOTIFICATION_URL)
    }
    if (intent.getBooleanExtra(ShopzyNotificationHelper.EXTRA_OPEN_NOTIFICATION_SETTINGS, false)) {
      openNotificationSettings = true
      intent.removeExtra(ShopzyNotificationHelper.EXTRA_OPEN_NOTIFICATION_SETTINGS)
    }
  }
}

@Composable
fun ShopzyAppRoot(
  pendingNotificationUrl: String? = null,
  onPendingNotificationUrlHandled: () -> Unit = {},
  openNotificationSettingsTrigger: Boolean = false,
  onNotificationSettingsTriggerHandled: () -> Unit = {}
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

  var currentScreen by remember { mutableStateOf(ScreenState.SPLASH) }
  var isRetrying by remember { mutableStateOf(false) }

  // Initial Splash Screen display logic (4 seconds) + pre-warm WebView cache dirs + live network observation
  LaunchedEffect(Unit) {
    withContext(Dispatchers.IO) {
      ensureWebViewCacheDirs(context.cacheDir)
    }
    delay(4000)
    NetworkUtils.observeNetworkState(context).collect { isOnline ->
      currentScreen = if (isOnline) {
        ScreenState.WEBVIEW
      } else {
        ScreenState.OFFLINE
      }
    }
  }

  Surface(
    modifier = Modifier.fillMaxSize(),
    color = MaterialTheme.colorScheme.background
  ) {
    Crossfade(
      targetState = currentScreen,
      animationSpec = tween(400),
      label = "screen_transition"
    ) { screen ->
      when (screen) {
        ScreenState.SPLASH -> {
          SplashScreen()
        }
        ScreenState.WEBVIEW -> {
          ShopzyWebView(
            onLoadError = {
              currentScreen = ScreenState.OFFLINE
            },
            onLoadSuccess = {
              // Confirmed loaded successfully
            },
            pendingNavigationUrl = pendingNotificationUrl,
            onPendingNavigationHandled = onPendingNotificationUrlHandled,
            openNotificationSettingsTrigger = openNotificationSettingsTrigger,
            onNotificationSettingsTriggerHandled = onNotificationSettingsTriggerHandled
          )
        }
        ScreenState.OFFLINE -> {
          OfflineScreen(
            onRetry = {
              coroutineScope.launch {
                isRetrying = true
                delay(800) // Brief delay to simulate/validate network check
                if (NetworkUtils.isOnline(context)) {
                  isRetrying = false
                  currentScreen = ScreenState.WEBVIEW
                } else {
                  isRetrying = false
                  // Remains in offline state
                }
              }
            },
            isRetrying = isRetrying
          )
        }
      }
    }
  }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
  Text(text = "Hello $name!", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun ShopzyPreview() {
  MyApplicationTheme {
    ShopzyAppRoot()
  }
}

private fun ensureWebViewCacheDirs(cacheDir: File) {
  try {
    val codeCacheDir = File(cacheDir, "WebView/Default/HTTP Cache/Code Cache")
    File(codeCacheDir, "js").mkdirs()
    File(codeCacheDir, "wasm").mkdirs()
  } catch (_: Exception) {
    // Non-fatal if cache directory creation is restricted
  }
}
