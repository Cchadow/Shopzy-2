package com.example.notifications

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.theme.ShopzyBlue
import com.example.ui.theme.ShopzyCoral
import com.example.ui.theme.ShopzyOrange
import com.example.ui.theme.SuccessGreen
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.notificationDataStore by preferencesDataStore(name = "shopzy_notification_prefs")

enum class NotificationAccent(
  val label: String,
  val argb: Int,
  val color: Color
) {
  ORANGE("Orange", 0xFFFF7A00.toInt(), ShopzyOrange),
  CORAL("Coral", 0xFFEA580C.toInt(), ShopzyCoral),
  BLUE("Blue", 0xFF1D4ED8.toInt(), ShopzyBlue),
  EMERALD("Emerald", 0xFF16A34A.toInt(), SuccessGreen);

  companion object {
    fun fromName(name: String?): NotificationAccent {
      return entries.firstOrNull { it.name == name } ?: ORANGE
    }
  }
}

data class NotificationSettings(
  val enabled: Boolean = true,
  val soundEnabled: Boolean = true,
  val vibrationEnabled: Boolean = true,
  val headsUpEnabled: Boolean = true,
  val expandedStyle: Boolean = true,
  val accent: NotificationAccent = NotificationAccent.ORANGE
)

class NotificationPreferences(private val context: Context) {

  private object Keys {
    val ENABLED = booleanPreferencesKey("notifications_enabled")
    val SOUND = booleanPreferencesKey("sound_enabled")
    val VIBRATION = booleanPreferencesKey("vibration_enabled")
    val HEADS_UP = booleanPreferencesKey("heads_up_enabled")
    val EXPANDED = booleanPreferencesKey("expanded_style")
    val ACCENT = stringPreferencesKey("accent_color")
  }

  val settingsFlow: Flow<NotificationSettings> = context.notificationDataStore.data.map { prefs ->
    NotificationSettings(
      enabled = prefs[Keys.ENABLED] ?: true,
      soundEnabled = prefs[Keys.SOUND] ?: true,
      vibrationEnabled = prefs[Keys.VIBRATION] ?: true,
      headsUpEnabled = prefs[Keys.HEADS_UP] ?: true,
      expandedStyle = prefs[Keys.EXPANDED] ?: true,
      accent = NotificationAccent.fromName(prefs[Keys.ACCENT])
    )
  }

  suspend fun getCurrentSettings(): NotificationSettings = settingsFlow.first()

  suspend fun setEnabled(enabled: Boolean) {
    context.notificationDataStore.edit { it[Keys.ENABLED] = enabled }
  }

  suspend fun setSoundEnabled(enabled: Boolean) {
    context.notificationDataStore.edit { it[Keys.SOUND] = enabled }
  }

  suspend fun setVibrationEnabled(enabled: Boolean) {
    context.notificationDataStore.edit { it[Keys.VIBRATION] = enabled }
  }

  suspend fun setHeadsUpEnabled(enabled: Boolean) {
    context.notificationDataStore.edit { it[Keys.HEADS_UP] = enabled }
  }

  suspend fun setExpandedStyle(enabled: Boolean) {
    context.notificationDataStore.edit { it[Keys.EXPANDED] = enabled }
  }

  suspend fun setAccent(accent: NotificationAccent) {
    context.notificationDataStore.edit { it[Keys.ACCENT] = accent.name }
  }
}
