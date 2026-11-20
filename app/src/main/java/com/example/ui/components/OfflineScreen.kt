package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.ShopzyOrange

@Composable
fun OfflineScreen(
  onRetry: () -> Unit,
  isRetrying: Boolean = false,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .fillMaxSize()
      .background(Color.Black)
      .safeDrawingPadding()
      .padding(24.dp)
      .testTag("offline_screen"),
    contentAlignment = Alignment.Center
  ) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = 420.dp),
      shape = RoundedCornerShape(28.dp),
      colors = CardDefaults.cardColors(
        containerColor = Color(0xFF121214)
      ),
      border = BorderStroke(1.dp, ShopzyOrange.copy(alpha = 0.25f)),
      elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 28.dp, vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        // Shopzy Brand Logo Emblem + Wordmark
        Surface(
          shape = RoundedCornerShape(22.dp),
          color = Color.Black,
          border = BorderStroke(1.5.dp, ShopzyOrange.copy(alpha = 0.85f)),
          modifier = Modifier
            .size(84.dp)
            .testTag("offline_shopzy_logo")
        ) {
          Image(
            painter = painterResource(id = R.drawable.img_shopzy_logo),
            contentDescription = stringResource(R.string.splash_title),
            contentScale = ContentScale.Crop,
            modifier = Modifier
              .fillMaxSize()
              .clip(RoundedCornerShape(22.dp))
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
          text = stringResource(R.string.splash_title),
          style = MaterialTheme.typography.headlineLarge.copy(
            fontSize = 34.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.5.sp,
            color = ShopzyOrange
          ),
          textAlign = TextAlign.Center,
          modifier = Modifier.testTag("offline_brand_name")
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Wifi Off Status Badge
        Box(
          modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(ShopzyOrange.copy(alpha = 0.14f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Outlined.WifiOff,
            contentDescription = stringResource(R.string.offline_title),
            modifier = Modifier.size(28.dp),
            tint = ShopzyOrange
          )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Exact requested offline message
        Text(
          text = stringResource(R.string.offline_title),
          style = MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.Bold,
            color = Color.White,
            lineHeight = 28.sp
          ),
          textAlign = TextAlign.Center,
          modifier = Modifier.testTag("offline_message")
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
          text = stringResource(R.string.offline_desc),
          style = MaterialTheme.typography.bodyMedium.copy(
            color = Color.White.copy(alpha = 0.68f),
            lineHeight = 20.sp
          ),
          textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(30.dp))

        Button(
          onClick = onRetry,
          enabled = !isRetrying,
          modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("retry_button"),
          shape = RoundedCornerShape(16.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = ShopzyOrange,
            contentColor = Color.Black,
            disabledContainerColor = ShopzyOrange.copy(alpha = 0.6f),
            disabledContentColor = Color.Black
          )
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
          ) {
            if (isRetrying) {
              CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color.Black,
                strokeWidth = 2.5.dp
              )
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = stringResource(R.string.checking_connection),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
              )
            } else {
              Icon(
                imageVector = Icons.Outlined.Refresh,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = stringResource(R.string.retry_button),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
              )
            }
          }
        }
      }
    }
  }
}
