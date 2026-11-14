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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
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
import com.example.ui.screens.ShayariSplashScreen
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
              viewModel.onAppResumed()
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

        AnimatedContent(
          targetState =
            if (uiState.isSplashVisible) {
              "SPLASH"
            } else {
              when (uiState.screenState) {
                is ShayariScreenState.Loading -> "SPLASH"
                is ShayariScreenState.Maintenance -> "MAINTENANCE"
                is ShayariScreenState.Ready -> "READY"
              }
            },
          transitionSpec = {
            (fadeIn(animationSpec = tween(380)) + scaleIn(initialScale = 0.98f, animationSpec = tween(380)))
              .togetherWith(fadeOut(animationSpec = tween(260)))
          },
          label = "rootScreenTransition",
        ) { targetStage ->
          when (targetStage) {
            "SPLASH" -> {
              ShayariSplashScreen(appName = uiState.splashAppName)
            }

            "MAINTENANCE" -> {
              val maintenance = uiState.screenState as? ShayariScreenState.Maintenance
              if (maintenance != null) {
                MaintenanceScreen(
                  appName = maintenance.appName,
                  message = maintenance.message,
                  socialLink = maintenance.socialLink,
                  isRefreshing = uiState.isRefreshing,
                  onRetryClick = { viewModel.retryConnection() },
                  onOpenSocialLink = { url -> openExternalBrowser(context, url) },
                )
              }
            }

            else -> {
              val ready = uiState.screenState as? ShayariScreenState.Ready
              if (ready != null) {
                HomeScreen(
                  readyState = ready,
                  textSize = uiState.textSize,
                  isRefreshing = uiState.isRefreshing,
                  isSearchVisible = uiState.isSearchVisible,
                  unreadNotificationCount = uiState.unreadNotificationCount,
                  snackbarHostState = snackbarHostState,
                  onSelectCategory = viewModel::selectCategory,
                  onSelectTab = viewModel::selectTab,
                  onToggleSearch = viewModel::toggleSearchVisibility,
                  onSearchQueryChange = viewModel::updateSearchQuery,
                  onRefreshClick = { viewModel.refreshConfig(isSilentSync = false) },
                  onNotificationsClick = {
                    context.startActivity(Intent(context, NotificationsActivity::class.java))
                  },
                  onSettingsClick = {
                    context.startActivity(Intent(context, SettingsActivity::class.java))
                  },
                  onShareAppClick = {
                    shareAppWithFriends(context, ready.config.appName)
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
                    shareShayari(context, ready.config.appName, item)
                  },
                  onFavoriteToggle = viewModel::toggleFavorite,
                )

                if (uiState.showUpdateDialog) {
                  AlertDialog(
                    onDismissRequest = viewModel::dismissUpdateDialog,
                    title = { Text(stringResource(R.string.update_title)) },
                    text = {
                      Text(
                        "A newer version (${ready.config.latestVersionCode}) of ${ready.config.appName} is available."
                      )
                    },
                    confirmButton = {
                      Button(
                        onClick = {
                          openExternalBrowser(context, ready.config.forceUpdateUrl)
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
          }
        }

        if (uiState.showOfflineDialog && !uiState.isSplashVisible) {
          CleanNoInternetDialog(
            isRetrying = uiState.isRefreshing,
            onRetryClick = viewModel::retryConnection,
          )
        }
      }
    }
  }
}

@Composable
private fun CleanNoInternetDialog(
  isRetrying: Boolean,
  onRetryClick: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = {},
    properties =
      DialogProperties(
        dismissOnBackPress = false,
        dismissOnClickOutside = false,
      ),
    shape = RoundedCornerShape(24.dp),
    containerColor = Color.White,
    icon = {
      Surface(
        shape = CircleShape,
        color = Color(0xFFF3E8FF),
        modifier = Modifier.size(56.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            painter = painterResource(id = R.drawable.ic_wifi_off),
            contentDescription = null,
            tint = BadgePurple,
            modifier = Modifier.size(28.dp),
          )
        }
      }
    },
    title = {
      Text(
        text = stringResource(R.string.offline_dialog_title),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF1D0D36),
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
      )
    },
    text = {
      Text(
        text = stringResource(R.string.offline_dialog_message),
        style = MaterialTheme.typography.bodyMedium,
        color = Color(0xFF473E5C),
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
      )
    },
    confirmButton = {
      Button(
        onClick = onRetryClick,
        enabled = !isRetrying,
        shape = RoundedCornerShape(14.dp),
        colors =
          ButtonDefaults.buttonColors(
            containerColor = BadgePurple,
            contentColor = Color.White,
          ),
        modifier =
          Modifier.fillMaxWidth()
            .minimumInteractiveComponentSize()
            .testTag("offline_dialog_retry_button"),
      ) {
        if (isRetrying) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(
              color = Color.White,
              strokeWidth = 2.dp,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = stringResource(R.string.action_retry_hindi))
          }
        } else {
          Text(
            text = stringResource(R.string.action_retry_hindi),
            fontWeight = FontWeight.SemiBold,
          )
        }
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
