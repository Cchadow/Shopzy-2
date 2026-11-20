package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.notifications.NotificationAccent
import com.example.notifications.NotificationSettings
import com.example.ui.theme.ShopzyOrange
import com.example.ui.theme.SuccessGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsSheet(
  settings: NotificationSettings,
  hasOsPermission: Boolean,
  onDismiss: () -> Unit,
  onRequestOsPermission: () -> Unit,
  onToggleEnabled: (Boolean) -> Unit,
  onToggleSound: (Boolean) -> Unit,
  onToggleVibration: (Boolean) -> Unit,
  onToggleHeadsUp: (Boolean) -> Unit,
  onToggleExpanded: (Boolean) -> Unit,
  onSelectAccent: (NotificationAccent) -> Unit,
  onSendTestNotification: () -> Unit
) {
  val context = LocalContext.current
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = Color(0xFF121212),
    contentColor = Color.White,
    modifier = Modifier.testTag("notification_settings_sheet")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp)
        .navigationBarsPadding()
        .padding(bottom = 24.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      // Header
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Box(
          modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(ShopzyOrange.copy(alpha = 0.16f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = if (hasOsPermission && settings.enabled) {
              Icons.Outlined.NotificationsActive
            } else {
              Icons.Outlined.NotificationsOff
            },
            contentDescription = null,
            tint = ShopzyOrange,
            modifier = Modifier.size(24.dp)
          )
        }
        Column {
          Text(
            text = stringResource(R.string.notification_settings_title),
            style = MaterialTheme.typography.titleLarge.copy(
              fontWeight = FontWeight.Bold,
              color = Color.White
            )
          )
          Text(
            text = stringResource(R.string.notification_settings_subtitle),
            style = MaterialTheme.typography.bodySmall.copy(
              color = Color.White.copy(alpha = 0.68f)
            )
          )
        }
      }

      // System permission card
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (hasOsPermission) {
          SuccessGreen.copy(alpha = 0.14f)
        } else {
          ShopzyOrange.copy(alpha = 0.16f)
        },
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = stringResource(
              if (hasOsPermission) R.string.notification_permission_granted
              else R.string.notification_permission_needed
            ),
            style = MaterialTheme.typography.bodyMedium.copy(
              fontWeight = FontWeight.SemiBold,
              color = if (hasOsPermission) Color(0xFF86EFAC) else ShopzyOrange
            ),
            modifier = Modifier.weight(1f)
          )
          if (!hasOsPermission) {
            Spacer(modifier = Modifier.width(8.dp))
            Button(
              onClick = onRequestOsPermission,
              colors = ButtonDefaults.buttonColors(
                containerColor = ShopzyOrange,
                contentColor = Color.Black
              ),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.testTag("grant_notification_permission_button")
            ) {
              Text(
                text = stringResource(R.string.notification_enable_action),
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }

      // Master toggle & behavior toggles
      Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF1C1C1E),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.fillMaxWidth()) {
          NotificationToggleRow(
            title = stringResource(R.string.notification_master_toggle),
            subtitle = stringResource(R.string.notification_master_desc),
            checked = settings.enabled,
            onCheckedChange = onToggleEnabled,
            testTag = "toggle_notifications_enabled"
          )
          HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
          NotificationToggleRow(
            title = stringResource(R.string.notification_sound_toggle),
            subtitle = stringResource(R.string.notification_sound_desc),
            checked = settings.soundEnabled,
            enabled = settings.enabled,
            onCheckedChange = onToggleSound,
            testTag = "toggle_notifications_sound"
          )
          HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
          NotificationToggleRow(
            title = stringResource(R.string.notification_vibration_toggle),
            subtitle = stringResource(R.string.notification_vibration_desc),
            checked = settings.vibrationEnabled,
            enabled = settings.enabled,
            onCheckedChange = onToggleVibration,
            testTag = "toggle_notifications_vibration"
          )
          HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
          NotificationToggleRow(
            title = stringResource(R.string.notification_headsup_toggle),
            subtitle = stringResource(R.string.notification_headsup_desc),
            checked = settings.headsUpEnabled,
            enabled = settings.enabled,
            onCheckedChange = onToggleHeadsUp,
            testTag = "toggle_notifications_headsup"
          )
          HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
          NotificationToggleRow(
            title = stringResource(R.string.notification_expanded_toggle),
            subtitle = stringResource(R.string.notification_expanded_desc),
            checked = settings.expandedStyle,
            enabled = settings.enabled,
            onCheckedChange = onToggleExpanded,
            testTag = "toggle_notifications_expanded"
          )
        }
      }

      // Accent color selector
      Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF1C1C1E),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Text(
            text = stringResource(R.string.notification_accent_label),
            style = MaterialTheme.typography.titleSmall.copy(
              fontWeight = FontWeight.SemiBold,
              color = Color.White
            )
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            NotificationAccent.entries.forEach { accent ->
              val isSelected = settings.accent == accent
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                  .weight(1f)
                  .clip(RoundedCornerShape(12.dp))
                  .clickable(role = Role.RadioButton) { onSelectAccent(accent) }
                  .padding(vertical = 6.dp)
                  .testTag("accent_${accent.name.lowercase()}")
              ) {
                Box(
                  modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accent.color)
                    .then(
                      if (isSelected) {
                        Modifier.border(2.5.dp, Color.White, CircleShape)
                      } else {
                        Modifier
                      }
                    ),
                  contentAlignment = Alignment.Center
                ) {
                  if (isSelected) {
                    Icon(
                      imageVector = Icons.Outlined.Check,
                      contentDescription = accent.label,
                      tint = Color.White,
                      modifier = Modifier.size(20.dp)
                    )
                  }
                }
                Text(
                  text = accent.label,
                  style = MaterialTheme.typography.labelSmall.copy(
                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.65f),
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                  )
                )
              }
            }
          }
        }
      }

      // Actions: Send Test Notification & System Notification Settings
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Button(
          onClick = {
            if (!hasOsPermission) {
              onRequestOsPermission()
            } else {
              onSendTestNotification()
            }
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = ShopzyOrange,
            contentColor = Color.Black
          ),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .weight(1f)
            .height(50.dp)
            .testTag("send_test_notification_button")
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Outlined.Send,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = stringResource(R.string.notification_test_button),
            fontWeight = FontWeight.Bold,
            maxLines = 1
          )
        }

        OutlinedButton(
          onClick = {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
              Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
              }
            } else {
              Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
              }
            }
            context.startActivity(intent)
          },
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .height(50.dp)
            .testTag("open_system_notification_settings_button")
        ) {
          Icon(
            imageVector = Icons.Outlined.Settings,
            contentDescription = stringResource(R.string.notification_open_os_settings),
            tint = Color.White,
            modifier = Modifier.size(20.dp)
          )
        }
      }
    }
  }
}

@Composable
private fun NotificationToggleRow(
  title: String,
  subtitle: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  testTag: String,
  enabled: Boolean = true
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(enabled = enabled) { onCheckedChange(!checked) }
      .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
      Text(
        text = title,
        style = MaterialTheme.typography.bodyLarge.copy(
          fontWeight = FontWeight.SemiBold,
          color = if (enabled) Color.White else Color.White.copy(alpha = 0.4f)
        )
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall.copy(
          color = if (enabled) Color.White.copy(alpha = 0.65f) else Color.White.copy(alpha = 0.3f)
        )
      )
    }
    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange,
      enabled = enabled,
      colors = SwitchDefaults.colors(
        checkedThumbColor = Color.Black,
        checkedTrackColor = ShopzyOrange,
        uncheckedThumbColor = Color.White.copy(alpha = 0.8f),
        uncheckedTrackColor = Color(0xFF333336)
      ),
      modifier = Modifier.testTag(testTag)
    )
  }
}
