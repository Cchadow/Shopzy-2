package com.example.notifications

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.json.JSONObject

class ShopzyWebNotificationBridge(
  private val context: Context,
  private val coroutineScope: CoroutineScope,
  private val onRequestOsPermission: (onResult: (Boolean) -> Unit) -> Unit
) {
  private val mainHandler = Handler(Looper.getMainLooper())
  private var attachedWebView: WebView? = null
  private val prefs = NotificationPreferences(context)

  fun attachWebView(webView: WebView) {
    attachedWebView = webView
  }

  @android.annotation.SuppressLint("JavascriptInterface")
  fun registerOn(webView: WebView) {
    attachedWebView = webView
    webView.addJavascriptInterface(this, JS_INTERFACE_NAME)
  }

  @JavascriptInterface
  fun getPermissionStatus(): String {
    return if (ShopzyNotificationHelper.areNotificationsEnabled(context)) {
      "granted"
    } else {
      "default"
    }
  }

  @JavascriptInterface
  fun requestPermission(callbackId: String) {
    mainHandler.post {
      if (ShopzyNotificationHelper.areNotificationsEnabled(context)) {
        resolvePermissionCallback(callbackId, "granted")
      } else {
        onRequestOsPermission { granted ->
          val status = if (granted && ShopzyNotificationHelper.areNotificationsEnabled(context)) {
            "granted"
          } else {
            "denied"
          }
          resolvePermissionCallback(callbackId, status)
        }
      }
    }
  }

  @JavascriptInterface
  fun showNotification(payloadJson: String) {
    coroutineScope.launch {
      try {
        val json = JSONObject(payloadJson)
        val title = json.optString("title", context.getString(com.example.R.string.app_name))
        val body = json.optString("body", "")
        val icon = json.optString("icon", "").takeIf { it.isNotBlank() }
        val image = json.optString("image", "").takeIf { it.isNotBlank() }
        val tag = json.optString("tag", "").takeIf { it.isNotBlank() }
        val url = json.optString("url", "").takeIf { it.isNotBlank() }
        val silent = json.optBoolean("silent", false)

        if (!prefs.getCurrentSettings().enabled) return@launch

        ShopzyNotificationHelper.showWebNotification(
          context = context,
          title = title,
          body = body,
          iconUrl = icon,
          imageUrl = image,
          tag = tag,
          targetUrl = url,
          silent = silent
        )
      } catch (_: Exception) {
        // Ignore malformed JS notification payloads safely
      }
    }
  }

  @JavascriptInterface
  fun closeNotification(tagOrId: String) {
    ShopzyNotificationHelper.cancelNotification(context, tagOrId)
  }

  private fun resolvePermissionCallback(callbackId: String, status: String) {
    val safeId = JSONObject.quote(callbackId)
    val safeStatus = JSONObject.quote(status)
    attachedWebView?.evaluateJavascript(
      "if (window.__shopzyResolveNotificationPermission) { window.__shopzyResolveNotificationPermission($safeId, $safeStatus); }",
      null
    )
  }

  fun syncPermissionToWeb(webView: WebView?) {
    val status = JSONObject.quote(getPermissionStatus())
    webView?.evaluateJavascript(
      "if (window.__shopzySyncPermission) { window.__shopzySyncPermission($status); }",
      null
    )
  }

  companion object {
    const val JS_INTERFACE_NAME = "ShopzyAndroidNotifier"

    fun injectPolyfill(webView: WebView?) {
      webView?.evaluateJavascript(NOTIFICATION_POLYFILL_JS, null)
    }

    private const val NOTIFICATION_POLYFILL_JS = """
      (function() {
        if (window.__shopzyNotificationBridgeInstalled) {
          if (window.__shopzySyncPermission && window.ShopzyAndroidNotifier) {
            window.__shopzySyncPermission(window.ShopzyAndroidNotifier.getPermissionStatus());
          }
          return;
        }
        if (!window.ShopzyAndroidNotifier) return;
        window.__shopzyNotificationBridgeInstalled = true;

        var currentPerm = window.ShopzyAndroidNotifier.getPermissionStatus();
        var permCallbacks = {};
        var cbCounter = 0;

        window.__shopzySyncPermission = function(status) {
          currentPerm = status || 'default';
        };

        window.__shopzyResolveNotificationPermission = function(id, status) {
          currentPerm = status || 'default';
          var entry = permCallbacks[id];
          if (entry) {
            delete permCallbacks[id];
            if (typeof entry.cb === 'function') {
              try { entry.cb(currentPerm); } catch (e) {}
            }
            entry.resolve(currentPerm);
          }
        };

        function ShopzyNotification(title, options) {
          options = options || {};
          this.title = String(title || '');
          this.body = options.body ? String(options.body) : '';
          this.icon = options.icon ? String(options.icon) : '';
          this.image = options.image ? String(options.image) : '';
          this.tag = options.tag ? String(options.tag) : ('shopzy_' + Date.now() + '_' + Math.random().toString(36).slice(2));
          this.data = options.data || null;
          this.silent = !!options.silent;
          this.requireInteraction = !!options.requireInteraction;
          this.onclick = null;
          this.onshow = null;
          this.onerror = null;
          this.onclose = null;

          var targetUrl = window.location.href;
          if (options.data && typeof options.data === 'object' && options.data.url) {
            targetUrl = String(options.data.url);
          } else if (options.url) {
            targetUrl = String(options.url);
          }

          try {
            window.ShopzyAndroidNotifier.showNotification(JSON.stringify({
              title: this.title,
              body: this.body,
              icon: this.icon,
              image: this.image,
              tag: this.tag,
              url: targetUrl,
              silent: this.silent
            }));
            var self = this;
            setTimeout(function() {
              if (typeof self.onshow === 'function') {
                try { self.onshow(new Event('show')); } catch (e) {}
              }
            }, 0);
          } catch (err) {
            if (typeof this.onerror === 'function') {
              try { this.onerror(new Event('error')); } catch (e) {}
            }
          }
        }

        ShopzyNotification.prototype.close = function() {
          try {
            window.ShopzyAndroidNotifier.closeNotification(this.tag);
            if (typeof this.onclose === 'function') {
              this.onclose(new Event('close'));
            }
          } catch (e) {}
        };

        ShopzyNotification.prototype.addEventListener = function(type, listener) {
          if (type === 'show') this.onshow = listener;
          if (type === 'click') this.onclick = listener;
          if (type === 'close') this.onclose = listener;
          if (type === 'error') this.onerror = listener;
        };

        ShopzyNotification.prototype.removeEventListener = function() {};

        Object.defineProperty(ShopzyNotification, 'permission', {
          get: function() {
            try {
              currentPerm = window.ShopzyAndroidNotifier.getPermissionStatus();
            } catch (e) {}
            return currentPerm;
          },
          configurable: true
        });

        Object.defineProperty(ShopzyNotification, 'maxActions', {
          get: function() { return 2; },
          configurable: true
        });

        ShopzyNotification.requestPermission = function(callback) {
          return new Promise(function(resolve) {
            var id = 'perm_' + (++cbCounter);
            permCallbacks[id] = { resolve: resolve, cb: callback };
            try {
              window.ShopzyAndroidNotifier.requestPermission(id);
            } catch (e) {
              delete permCallbacks[id];
              if (typeof callback === 'function') callback('denied');
              resolve('denied');
            }
          });
        };

        window.Notification = ShopzyNotification;

        if (typeof ServiceWorkerRegistration !== 'undefined' && ServiceWorkerRegistration.prototype) {
          var origShowNotification = ServiceWorkerRegistration.prototype.showNotification;
          ServiceWorkerRegistration.prototype.showNotification = function(title, options) {
            try {
              new ShopzyNotification(title, options);
              return Promise.resolve();
            } catch (e) {
              if (origShowNotification) {
                return origShowNotification.apply(this, arguments);
              }
              return Promise.reject(e);
            }
          };
        }
      })();
    """
  }
}
