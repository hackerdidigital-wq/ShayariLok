package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.ShayariRepository
import com.example.model.ShayariNotification
import com.example.ui.theme.DarkBackgroundBottom
import com.example.ui.theme.LightBackgroundBottom
import com.example.ui.theme.ShayariLokTheme
import kotlinx.coroutines.launch

class NotificationsActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    val repository = ShayariRepository(this)
    setContent {
      ShayariLokTheme {
        NotificationsContent(
          repository = repository,
          onBack = { finish() },
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationsContent(
  repository: ShayariRepository,
  onBack: () -> Unit,
) {
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }
  var config by remember { mutableStateOf(repository.loadCachedConfig()) }
  var isRefreshing by remember { mutableStateOf(false) }

  LaunchedEffect(config) {
    repository.markNotificationsRead(config)
  }

  val isDark = isSystemInDarkTheme()
  val bgGradient =
    Brush.verticalGradient(
      colors =
        listOf(
          MaterialTheme.colorScheme.background,
          if (isDark) DarkBackgroundBottom else LightBackgroundBottom,
        )
    )

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0f),
    contentWindowInsets = WindowInsets.safeDrawing,
    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    topBar = {
      Column {
        TopAppBar(
          title = {
            Text(
              text = stringResource(R.string.notifications_title),
              style = MaterialTheme.typography.titleLarge,
            )
          },
          navigationIcon = {
            IconButton(
              onClick = onBack,
              modifier =
                Modifier.minimumInteractiveComponentSize().testTag("notifications_back_button"),
            ) {
              Icon(
                painter = painterResource(id = R.drawable.ic_arrow_back),
                contentDescription = stringResource(R.string.cd_back),
              )
            }
          },
          actions = {
            IconButton(
              onClick = {
                if (!isRefreshing) {
                  scope.launch {
                    isRefreshing = true
                    repository.fetchRemoteConfig().fold(
                      onSuccess = { updated ->
                        config = updated
                        repository.markNotificationsRead(updated)
                        snackbarHostState.showSnackbar("Checked latest updates")
                      },
                      onFailure = {
                        snackbarHostState.showSnackbar("Offline mode: showing saved alerts")
                      },
                    )
                    isRefreshing = false
                  }
                }
              },
              enabled = !isRefreshing,
              modifier =
                Modifier.minimumInteractiveComponentSize().testTag("notifications_refresh_button"),
            ) {
              Icon(
                painter = painterResource(id = R.drawable.ic_refresh),
                contentDescription = stringResource(R.string.cd_refresh),
              )
            }
          },
          colors =
            TopAppBarDefaults.topAppBarColors(
              containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.94f),
              titleContentColor = MaterialTheme.colorScheme.onBackground,
            ),
        )
        if (isRefreshing) {
          LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth().height(2.dp),
            color = MaterialTheme.colorScheme.secondary,
          )
        }
      }
    },
  ) { innerPadding ->
    Box(
      modifier = Modifier.fillMaxSize().background(bgGradient).padding(innerPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      val notifications = config.notifications
      if (notifications.isEmpty()) {
        Column(
          modifier = Modifier.fillMaxSize().padding(28.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center,
        ) {
          Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(68.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                painter = painterResource(id = R.drawable.ic_notification),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(30.dp),
              )
            }
          }
          Spacer(modifier = Modifier.height(16.dp))
          Text(
            text = stringResource(R.string.empty_notifications_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = stringResource(R.string.empty_notifications_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 320.dp),
          )
        }
      } else {
        LazyColumn(
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
          modifier = Modifier.widthIn(max = 620.dp).fillMaxSize(),
        ) {
          items(notifications, key = { it.id }) { item ->
            NotificationAlertCard(item = item)
          }
        }
      }
    }
  }
}

@Composable
private fun NotificationAlertCard(item: ShayariNotification) {
  Surface(
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
    tonalElevation = 1.dp,
    modifier = Modifier.fillMaxWidth().testTag("notification_card_${item.id}"),
  ) {
    Row(
      modifier = Modifier.padding(18.dp),
      verticalAlignment = Alignment.Top,
    ) {
      Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.size(42.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            painter = painterResource(id = R.drawable.ic_notification),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(20.dp),
          )
        }
      }

      Spacer(modifier = Modifier.width(14.dp))

      Column(modifier = Modifier.weight(1f)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.secondaryContainer,
          ) {
            Text(
              text = item.badge,
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSecondaryContainer,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
            )
          }
          Text(
            text = item.timeLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = item.title,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
        )
        if (item.message.isNotBlank()) {
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = item.message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}
