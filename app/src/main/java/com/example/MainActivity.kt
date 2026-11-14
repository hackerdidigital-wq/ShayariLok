package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.ShayariItem
import com.example.ui.ShayariScreenState
import com.example.ui.ShayariViewModel
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MaintenanceScreen
import com.example.ui.theme.BadgePurple
import com.example.ui.theme.ShayariLokTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      ShayariLokTheme {
        val viewModel: ShayariViewModel = viewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current
        val snackbarHostState = remember { SnackbarHostState() }
        val coroutineScope = rememberCoroutineScope()
        val copiedMessage = stringResource(R.string.copied_snackbar)

        DisposableEffect(lifecycleOwner) {
          val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
              viewModel.reloadFromCache()
            }
          }
          lifecycleOwner.lifecycle.addObserver(observer)
          onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        LaunchedEffect(uiState.syncStatusMessage) {
          val message = uiState.syncStatusMessage
          if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearSyncStatusMessage()
          }
        }

        when (val screen = uiState.screenState) {
          is ShayariScreenState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
              CircularProgressIndicator()
            }
          }

          is ShayariScreenState.Maintenance -> {
            MaintenanceScreen(
              appName = screen.appName,
              message = screen.message,
              socialLink = screen.socialLink,
              isRefreshing = uiState.isRefreshing,
              onRetryClick = { viewModel.retryConnection() },
              onOpenSocialLink = { url -> openExternalBrowser(context, url) },
            )
          }

          is ShayariScreenState.Ready -> {
            HomeScreen(
              readyState = screen,
              textSize = uiState.textSize,
              isRefreshing = uiState.isRefreshing,
              isOffline = uiState.isOfflineBannerVisible,
              isNetworkConnected = uiState.isNetworkConnected,
              isSearchVisible = uiState.isSearchVisible,
              unreadNotificationCount = uiState.unreadNotificationCount,
              snackbarHostState = snackbarHostState,
              onSelectCategory = viewModel::selectCategory,
              onSelectTab = viewModel::selectTab,
              onToggleSearch = viewModel::toggleSearchVisibility,
              onSearchQueryChange = viewModel::updateSearchQuery,
              onRefreshClick = { viewModel.refreshConfig(isInitialAutoSync = false) },
              onRetryConnection = viewModel::retryConnection,
              onNotificationsClick = {
                context.startActivity(Intent(context, NotificationsActivity::class.java))
              },
              onSettingsClick = {
                context.startActivity(Intent(context, SettingsActivity::class.java))
              },
              onShareAppClick = {
                shareAppWithFriends(context, screen.config.appName)
              },
              onOpenCommunityClick = { url -> openExternalBrowser(context, url) },
              onCopyShayari = { item ->
                copyShayariToClipboard(context, item)
                coroutineScope.launch {
                  snackbarHostState.currentSnackbarData?.dismiss()
                  snackbarHostState.showSnackbar(copiedMessage)
                }
              },
              onShareShayari = { item ->
                shareShayari(context, screen.config.appName, item)
              },
              onFavoriteToggle = viewModel::toggleFavorite,
            )

            if (uiState.showUpdateDialog) {
              AlertDialog(
                onDismissRequest = viewModel::dismissUpdateDialog,
                title = { Text(stringResource(R.string.update_title)) },
                text = {
                  Text(
                    "A newer version (${screen.config.latestVersionCode}) of ${screen.config.appName} is available."
                  )
                },
                confirmButton = {
                  Button(
                    onClick = {
                      openExternalBrowser(context, screen.config.forceUpdateUrl)
                    },
                    modifier = Modifier.testTag("update_now_button"),
                  ) {
                    Text(stringResource(R.string.update_action))
                  }
                },
                dismissButton = {
                  TextButton(
                    onClick = viewModel::dismissUpdateDialog,
                    modifier = Modifier.testTag("update_later_button"),
                  ) {
                    Text(stringResource(R.string.later_action))
                  }
                },
              )
            }
          }
        }

        if (uiState.showOfflineDialog) {
          OfflineAlertDialog(
            isNetworkConnected = uiState.isNetworkConnected,
            isRetrying = uiState.isRefreshing,
            onRetryClick = viewModel::retryConnection,
            onDismissClick = viewModel::dismissOfflineDialog,
          )
        }
      }
    }
  }
}

@Composable
private fun OfflineAlertDialog(
  isNetworkConnected: Boolean,
  isRetrying: Boolean,
  onRetryClick: () -> Unit,
  onDismissClick: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismissClick,
    containerColor = Color.White,
    icon = {
      Icon(
        painter = painterResource(id = R.drawable.ic_wifi_off),
        contentDescription = null,
        tint = Color(0xFFB91C1C),
        modifier = Modifier.size(30.dp),
      )
    },
    title = {
      Text(
        text = stringResource(R.string.offline_dialog_title),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF1D0D36),
      )
    },
    text = {
      Text(
        text =
          stringResource(
            if (isNetworkConnected) {
              R.string.offline_server_unreachable_message
            } else {
              R.string.offline_dialog_message
            }
          ),
        style = MaterialTheme.typography.bodyMedium,
        color = Color(0xFF473E5C),
      )
    },
    confirmButton = {
      Button(
        onClick = onRetryClick,
        enabled = !isRetrying,
        colors =
          ButtonDefaults.buttonColors(
            containerColor = BadgePurple,
            contentColor = Color.White,
          ),
        modifier = Modifier.minimumInteractiveComponentSize().testTag("offline_dialog_retry_button"),
      ) {
        if (isRetrying) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(
              color = Color.White,
              strokeWidth = 2.dp,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = stringResource(R.string.action_retry))
          }
        } else {
          Text(text = stringResource(R.string.action_retry_hindi))
        }
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismissClick,
        modifier = Modifier.minimumInteractiveComponentSize().testTag("offline_dialog_dismiss_button"),
      ) {
        Text(
          text = stringResource(R.string.action_continue_offline),
          color = Color(0xFF5D5475),
        )
      }
    },
  )
}

private fun copyShayariToClipboard(context: Context, item: ShayariItem) {
  val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
  val clip = ClipData.newPlainText("Shayari", item.text)
  clipboard.setPrimaryClip(clip)
}

private fun shareShayari(context: Context, appName: String, item: ShayariItem) {
  val shareText = "${item.text}\n\n— Shared via $appName"
  val intent =
    Intent(Intent.ACTION_SEND).apply {
      type = "text/plain"
      putExtra(Intent.EXTRA_TEXT, shareText)
    }
  context.startActivity(Intent.createChooser(intent, "Share Shayari"))
}

private fun shareAppWithFriends(context: Context, appName: String) {
  val shareText =
    "Check out $appName — Apki Har Bhavna Ka Saathi! Read & share Hindi Shayari anytime:\nhttps://play.google.com/store/apps/details?id=${BuildConfig.APPLICATION_ID}"
  val intent =
    Intent(Intent.ACTION_SEND).apply {
      type = "text/plain"
      putExtra(Intent.EXTRA_TEXT, shareText)
    }
  context.startActivity(Intent.createChooser(intent, "Share $appName"))
}

private fun openExternalBrowser(context: Context, rawUrl: String) {
  val trimmed = rawUrl.trim()
  if (trimmed.isEmpty()) return
  val safeUrl =
    if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
      trimmed
    } else {
      "https://$trimmed"
    }
  try {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(safeUrl))
    context.startActivity(intent)
  } catch (_: Exception) {
    // Ignore if no browser is installed on device
  }
}
