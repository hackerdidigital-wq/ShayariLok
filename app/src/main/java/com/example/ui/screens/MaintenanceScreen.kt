package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R

@Composable
fun MaintenanceScreen(
  appName: String,
  message: String,
  socialLink: String,
  isRefreshing: Boolean,
  onRetryClick: () -> Unit,
  onOpenSocialLink: (String) -> Unit,
) {
  val bgGradient =
    Brush.verticalGradient(
      colors =
        listOf(
          MaterialTheme.colorScheme.background,
          MaterialTheme.colorScheme.surfaceVariant,
        )
    )

  Box(
    modifier = Modifier.fillMaxSize().background(bgGradient).safeDrawingPadding().padding(24.dp),
    contentAlignment = Alignment.Center,
  ) {
    Surface(
      shape = RoundedCornerShape(28.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 3.dp,
      modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth(),
    ) {
      Column(
        modifier = Modifier.padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
      ) {
        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.primaryContainer,
          modifier = Modifier.size(72.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              painter = painterResource(id = R.drawable.ic_settings),
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.size(34.dp),
            )
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
          text = appName,
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.secondary,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = stringResource(R.string.maintenance_title),
          style = MaterialTheme.typography.headlineMedium,
          color = MaterialTheme.colorScheme.onSurface,
          textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
          text = message,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
          onClick = onRetryClick,
          enabled = !isRefreshing,
          modifier =
            Modifier.fillMaxWidth()
              .minimumInteractiveComponentSize()
              .testTag("maintenance_retry_button"),
        ) {
          Icon(
            painter = painterResource(id = R.drawable.ic_refresh),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(text = if (isRefreshing) "Checking..." else "Check Again")
        }

        if (socialLink.isNotBlank()) {
          Spacer(modifier = Modifier.height(10.dp))
          OutlinedButton(
            onClick = { onOpenSocialLink(socialLink) },
            modifier =
              Modifier.fillMaxWidth()
                .minimumInteractiveComponentSize()
                .testTag("maintenance_social_button"),
          ) {
            Icon(
              painter = painterResource(id = R.drawable.ic_open_in_new),
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = stringResource(R.string.join_community_title))
          }
        }
      }
    }
  }
}
