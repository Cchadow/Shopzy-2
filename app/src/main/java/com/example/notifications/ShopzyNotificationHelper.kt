package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.ui.components.SHOPZY_TARGET_URL
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ShopzyNotificationHelper {

  const val EXTRA_NOTIFICATION_URL = "extra_notification_url"
  const val EXTRA_OPEN_NOTIFICATION_SETTINGS = "extra_open_notification_settings"

  private const val MAX_IMAGE_BYTES = 1024 * 1024 // 1 MB safety cap

  fun hasPostNotificationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS
      ) == PackageManager.PERMISSION_GRANTED
    } else {
      true
    }
  }

  fun areNotificationsEnabled(context: Context): Boolean {
    return hasPostNotificationPermission(context) &&
      NotificationManagerCompat.from(context).areNotificationsEnabled()
  }

  fun ensureDefaultChannel(context: Context, settings: NotificationSettings = NotificationSettings()): String {
    val channelId = buildChannelId(settings)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      if (manager.getNotificationChannel(channelId) == null) {
        val importance = if (settings.headsUpEnabled) {
          NotificationManager.IMPORTANCE_HIGH
        } else {
          NotificationManager.IMPORTANCE_DEFAULT
        }
        val channelName = buildString {
          append("Shopzy Alerts")
          if (!settings.headsUpEnabled) append(" (Quiet)")
          if (!settings.soundEnabled) append(" (Muted)")
        }
        val channel = NotificationChannel(channelId, channelName, importance).apply {
          description = "Live updates and notifications from Shopzy Haiti"
          enableLights(true)
          lightColor = settings.accent.argb
          enableVibration(settings.vibrationEnabled)
          if (settings.vibrationEnabled) {
            vibrationPattern = longArrayOf(0, 180, 100, 180)
          }
          if (settings.soundEnabled) {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttrs = AudioAttributes.Builder()
              .setUsage(AudioAttributes.USAGE_NOTIFICATION)
              .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
              .build()
            setSound(soundUri, audioAttrs)
          } else {
            setSound(null, null)
          }
        }
        manager.createNotificationChannel(channel)
      }
    }
    return channelId
  }

  private fun buildChannelId(settings: NotificationSettings): String {
    val imp = if (settings.headsUpEnabled) "high" else "std"
    val snd = if (settings.soundEnabled) "snd" else "mut"
    val vib = if (settings.vibrationEnabled) "vib" else "novib"
    return "shopzy_push_${imp}_${snd}_${vib}"
  }

  suspend fun showWebNotification(
    context: Context,
    title: String,
    body: String,
    iconUrl: String? = null,
    imageUrl: String? = null,
    tag: String? = null,
    targetUrl: String? = null,
    silent: Boolean = false
  ) {
    if (!areNotificationsEnabled(context)) return

    val prefs = NotificationPreferences(context)
    val baseSettings = prefs.getCurrentSettings()
    if (!baseSettings.enabled) return

    val effectiveSettings = if (silent) {
      baseSettings.copy(soundEnabled = false, vibrationEnabled = false, headsUpEnabled = false)
    } else {
      baseSettings
    }

    val channelId = ensureDefaultChannel(context, effectiveSettings)
    val safeUrl = sanitizeUrl(targetUrl) ?: SHOPZY_TARGET_URL

    val largeIconBitmap = if (effectiveSettings.expandedStyle) {
      fetchBitmapSafely(iconUrl)
    } else {
      null
    }
    val bigPictureBitmap = if (effectiveSettings.expandedStyle && !imageUrl.isNullOrBlank()) {
      fetchBitmapSafely(imageUrl)
    } else {
      null
    }

    val notificationId = if (!tag.isNullOrBlank()) {
      tag.hashCode()
    } else {
      (System.currentTimeMillis() and 0x7FFFFFFF).toInt()
    }

    val contentIntent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
      putExtra(EXTRA_NOTIFICATION_URL, safeUrl)
    }
    val contentPendingIntent = PendingIntent.getActivity(
      context,
      notificationId,
      contentIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val customizeIntent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
      putExtra(EXTRA_OPEN_NOTIFICATION_SETTINGS, true)
    }
    val customizePendingIntent = PendingIntent.getActivity(
      context,
      notificationId + 1,
      customizeIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val builder = NotificationCompat.Builder(context, channelId)
      .setSmallIcon(R.drawable.ic_notification)
      .setContentTitle(title.ifBlank { context.getString(R.string.app_name) })
      .setContentText(body)
      .setSubText(context.getString(R.string.splash_title))
      .setColor(effectiveSettings.accent.argb)
      .setAutoCancel(true)
      .setContentIntent(contentPendingIntent)
      .setCategory(NotificationCompat.CATEGORY_MESSAGE)
      .setPriority(
        if (effectiveSettings.headsUpEnabled) {
          NotificationCompat.PRIORITY_HIGH
        } else {
          NotificationCompat.PRIORITY_DEFAULT
        }
      )
      .addAction(
        0,
        context.getString(R.string.notification_customize_action),
        customizePendingIntent
      )

    if (largeIconBitmap != null) {
      builder.setLargeIcon(largeIconBitmap)
    }

    if (effectiveSettings.expandedStyle) {
      if (bigPictureBitmap != null) {
        builder.setStyle(
          NotificationCompat.BigPictureStyle()
            .bigPicture(bigPictureBitmap)
            .setSummaryText(body)
        )
      } else if (body.isNotBlank()) {
        builder.setStyle(
          NotificationCompat.BigTextStyle()
            .bigText(body)
        )
      }
    }

    if (!effectiveSettings.soundEnabled) {
      builder.setSilent(true)
    } else {
      val defaults = if (effectiveSettings.vibrationEnabled) {
        NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE
      } else {
        NotificationCompat.DEFAULT_SOUND
      }
      builder.setDefaults(defaults)
    }

    try {
      NotificationManagerCompat.from(context).notify(notificationId, builder.build())
    } catch (_: SecurityException) {
      // Permission was revoked concurrently
    }
  }

  fun cancelNotification(context: Context, tagOrId: String) {
    if (tagOrId.isBlank()) return
    NotificationManagerCompat.from(context).cancel(tagOrId.hashCode())
  }

  private fun sanitizeUrl(url: String?): String? {
    if (url.isNullOrBlank()) return null
    val trimmed = url.trim()
    return when {
      trimmed.startsWith("https://") || trimmed.startsWith("http://") -> trimmed
      trimmed.startsWith("/") -> "$SHOPZY_TARGET_URL$trimmed"
      else -> null
    }
  }

  private suspend fun fetchBitmapSafely(urlString: String?): Bitmap? = withContext(Dispatchers.IO) {
    if (urlString.isNullOrBlank()) return@withContext null
    val resolvedUrl = if (urlString.startsWith("/")) {
      "$SHOPZY_TARGET_URL$urlString"
    } else if (urlString.startsWith("https://") || urlString.startsWith("http://")) {
      urlString
    } else {
      return@withContext null
    }

    var connection: HttpURLConnection? = null
    try {
      val url = URL(resolvedUrl)
      connection = (url.openConnection() as HttpURLConnection).apply {
        connectTimeout = 2500
        readTimeout = 2500
        instanceFollowRedirects = true
      }
      if (connection.responseCode == HttpURLConnection.HTTP_OK) {
        val bytes = connection.inputStream.use { input ->
          val buffer = ByteArray(8192)
          val out = java.io.ByteArrayOutputStream()
          var total = 0
          var read: Int
          while (input.read(buffer).also { read = it } != -1) {
            total += read
            if (total > MAX_IMAGE_BYTES) return@withContext null
            out.write(buffer, 0, read)
          }
          out.toByteArray()
        }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
      } else {
        null
      }
    } catch (_: Exception) {
      null
    } finally {
      connection?.disconnect()
    }
  }
}
