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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.credentials.CredentialManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.GoogleAuthHelper
import com.example.data.GoogleAuthHelper.authStateFlow
import com.example.data.SocialRepository
import com.example.model.ShayariItem
import com.example.ui.MainTab
import com.example.ui.ShayariScreenState
import com.example.ui.ShayariViewModel
import com.example.ui.SocialViewModel
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.i18n.appStringsFor
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MaintenanceScreen
import com.example.ui.screens.MyProfileScreen
import com.example.ui.screens.RealTimeChatScreen
import com.example.ui.screens.ShayariSplashScreen
import com.example.ui.screens.SignInScreen
import com.example.ui.screens.SocialCommunityScreen
import com.example.ui.screens.UnifiedAlertsSheet
import com.example.ui.theme.BadgePurple
import com.example.ui.theme.ShayariLokTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      ShayariLokTheme {
        ShayariLokRootGate()
      }
    }
  }
}

@Composable
private fun ShayariLokRootGate() {
  val auth = Firebase.auth
  val currentUser by
    auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
  val user = currentUser

  if (user == null) {
    SignInScreen(
      onAuthSuccess = {
        // AuthStateListener updates currentUser reactively
      }
    )
  } else {
    key(user.uid) {
      AuthenticatedShayariApp(firebaseUser = user)
    }
  }
}

@Composable
private fun AuthenticatedShayariApp(firebaseUser: FirebaseUser) {
  val context = LocalContext.current
  val appContext = context.applicationContext
  val lifecycleOwner = LocalLifecycleOwner.current
  val snackbarHostState = remember { SnackbarHostState() }
  val coroutineScope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  val databaseId = remember(appContext) { appContext.getString(R.string.firestore_database_id) }
  val firestore = remember(databaseId) { FirebaseFirestore.getInstance(databaseId) }

  val viewModel: ShayariViewModel = viewModel(key = "shayari_${firebaseUser.uid}")
  LaunchedEffect(firestore, firebaseUser.uid) {
    viewModel.attachRealtimeFirestore(firestore, firebaseUser.uid)
  }

  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val activeDynamicTheme =
    (uiState.screenState as? ShayariScreenState.Ready)?.config?.dynamicTheme
      ?: com.example.model.DynamicThemeConfig()
  val appStrings = remember(uiState.selectedLanguageCode) { appStringsFor(uiState.selectedLanguageCode) }
  val layoutDir = if (uiState.isRtlLayout) LayoutDirection.Rtl else LayoutDirection.Ltr
  val copiedMessage = appStrings.copiedSnackbar
  val handleCopiedMessage = appStrings.handleCopiedSnackbar

  val derivedHandle = remember(firebaseUser.uid) { SocialRepository.buildHandleFromUid(firebaseUser.uid) }
  val googleName =
    firebaseUser.displayName?.trim()?.takeIf { it.isNotEmpty() }
      ?: firebaseUser.email?.substringBefore("@")?.takeIf { it.isNotEmpty() }
      ?: "Shayari Lover"
  val googleEmail = firebaseUser.email.orEmpty()
  val googlePhoto = firebaseUser.photoUrl?.toString().orEmpty()

  val socialRepository = remember(firestore) { SocialRepository(firestore) }
  val socialViewModel: SocialViewModel =
    viewModel(
      key = "social_${firebaseUser.uid}",
      factory =
        SocialViewModel.factory(
          repository = socialRepository,
          currentUserId = firebaseUser.uid,
          defaultHandle = derivedHandle,
          defaultName = googleName,
          userEmail = googleEmail,
          userPhotoUrl = googlePhoto,
        ),
    )
  val socialState by socialViewModel.uiState.collectAsStateWithLifecycle()
  var showAlertsSheet by remember { mutableStateOf(false) }

  val copyHandleToClipboard: (String) -> Unit = { handle ->
    copyPlainText(context, "Shayari Lok ID", handle)
    coroutineScope.launch {
      snackbarHostState.currentSnackbarData?.dismiss()
      snackbarHostState.showSnackbar(handleCopiedMessage)
    }
  }

  LaunchedEffect(socialState.actionFeedback) {
    val msg = socialState.actionFeedback
    if (!msg.isNullOrBlank()) {
      snackbarHostState.currentSnackbarData?.dismiss()
      snackbarHostState.showSnackbar(msg)
      socialViewModel.clearActionFeedback()
    }
  }

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

  val totalUnreadAlerts = uiState.unreadNotificationCount + socialState.unreadNotificationsCount

  ShayariLokTheme(dynamicTheme = activeDynamicTheme) {
    CompositionLocalProvider(
      LocalLayoutDirection provides layoutDir,
      LocalAppStrings provides appStrings,
    ) {
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
          val activePartner = socialState.activeChatPartner
          if (activePartner != null) {
            RealTimeChatScreen(
              currentUserId = socialState.currentUserId,
              partner = activePartner,
              messages = socialState.activeChatMessages,
              isFollowingPartner = activePartner.effectiveUid in socialState.followingIds,
              isSendingMessage = socialState.isSendingMessage,
              isBlocked = socialState.myProfile.isBlocked,
              blockReason = socialState.myProfile.blockReason,
              onToggleFollowPartner = { socialViewModel.toggleFollow(activePartner) },
              onSendMessage = { text, onSent ->
                socialViewModel.sendMessage(text, onSent)
              },
              onMarkMessagesRead = socialViewModel::markActiveChatMessagesRead,
              onBack = socialViewModel::closeChat,
            )
          } else {
            HomeScreen(
              readyState = ready,
              textSize = uiState.textSize,
              isRefreshing = uiState.isRefreshing,
              isSearchVisible = uiState.isSearchVisible,
              unreadNotificationCount = totalUnreadAlerts,
              snackbarHostState = snackbarHostState,
              selectedLanguageCode = uiState.selectedLanguageCode,
              selectedCountryCode = uiState.selectedCountryCode,
              dailyStreakDays = uiState.dailyStreakDays,
              votedPollOptions = uiState.votedPollOptions,
              myProfile = socialState.myProfile,
              userEmail = socialState.userEmail,
              onSignOutClick = {
                GoogleAuthHelper.signOut(
                  credentialManager = credentialManager,
                  scope = coroutineScope,
                )
              },
              onSelectCategory = viewModel::selectCategory,
              onSelectTab = viewModel::selectTab,
              onToggleSearch = viewModel::toggleSearchVisibility,
              onSearchQueryChange = viewModel::updateSearchQuery,
              onRefreshClick = { viewModel.refreshConfig(isSilentSync = false) },
              onNotificationsClick = {
                viewModel.markAdminNotificationsRead()
                showAlertsSheet = true
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
              onWhatsAppShareShayari = { item ->
                shareShayariOnWhatsApp(context, ready.config.appName, item)
              },
              onFavoriteToggle = viewModel::toggleFavorite,
              onSelectLanguage = viewModel::selectLanguage,
              onSelectCountry = viewModel::selectCountry,
              onToggleAdultContent = viewModel::setAdultContentAllowed,
              onVotePoll = viewModel::voteInPoll,
              socialTabContent = { tab, openLocalePicker ->
                if (tab == MainTab.COMMUNITY) {
                  SocialCommunityScreen(
                    uiState = socialState,
                    onSearchQueryChange = socialViewModel::updateSearchQuery,
                    onSelectFilter = socialViewModel::selectFilter,
                    onInspectProfile = socialViewModel::inspectProfile,
                    onDismissProfileSheet = socialViewModel::dismissProfileSheet,
                    onToggleFollow = socialViewModel::toggleFollow,
                    onToggleLike = socialViewModel::toggleLike,
                    onOpenChatWith = socialViewModel::openChatWith,
                    onOpenChatRoom = socialViewModel::openChatFromRoom,
                    onCopyHandle = copyHandleToClipboard,
                    onRefreshDirectory = socialViewModel::refreshSearchAndDirectory,
                    onShareInvite = {
                      shareAppWithFriends(context, ready.config.appName)
                    },
                  )
                } else {
                  MyProfileScreen(
                    uiState = socialState,
                    allowAdultContent = uiState.allowAdultContent,
                    onToggleAdultContent = viewModel::setAdultContentAllowed,
                    onOpenLocalePicker = openLocalePicker,
                    onCopyHandle = copyHandleToClipboard,
                    onPickGalleryAvatar = { uri ->
                      socialViewModel.uploadGalleryAvatar(context, uri)
                    },
                    onRemoveAvatar = socialViewModel::clearAvatarPhoto,
                    onUpdateProfile = { name, handle, city, address, bio, onDone ->
                      socialViewModel.updateProfile(
                        displayName = name,
                        handle = handle,
                        city = city,
                        address = address,
                        bio = bio,
                        onSuccess = onDone,
                      )
                    },
                    onOpenChatRoom = socialViewModel::openChatFromRoom,
                    onOpenSettings = {
                      context.startActivity(Intent(context, SettingsActivity::class.java))
                    },
                    onSignOut = {
                      GoogleAuthHelper.signOut(
                        credentialManager = credentialManager,
                        scope = coroutineScope,
                      )
                    },
                  )
                }
              },
            )
          }

          if (showAlertsSheet) {
            UnifiedAlertsSheet(
              adminNotifications = ready.config.notifications,
              socialState = socialState,
              onSocialNotificationClick = { notif ->
                showAlertsSheet = false
                viewModel.selectTab(MainTab.COMMUNITY)
                socialViewModel.openFromNotification(notif)
              },
              onMarkSocialRead = socialViewModel::markAllNotificationsRead,
              onDismiss = { showAlertsSheet = false },
            )
          }

          if (uiState.showUpdateDialog) {
            AlertDialog(
              onDismissRequest = viewModel::dismissUpdateDialog,
              title = { Text(appStrings.updateAvailable) },
              text = {
                Text(
                  "${ready.config.appName} v${ready.config.latestVersionCode}"
                )
              },
              confirmButton = {
                Button(
                  onClick = {
                    openExternalBrowser(context, ready.config.forceUpdateUrl)
                  },
                  modifier = Modifier.testTag("update_now_button"),
                ) {
                  Text(appStrings.updateNow)
                }
              },
              dismissButton = {
                TextButton(
                  onClick = viewModel::dismissUpdateDialog,
                  modifier = Modifier.testTag("update_later_button"),
                ) {
                  Text(appStrings.laterLabel)
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

@Composable
private fun CleanNoInternetDialog(
  isRetrying: Boolean,
  onRetryClick: () -> Unit,
) {
  val strings = LocalAppStrings.current
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
        text = strings.offlineTitle,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF1D0D36),
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
      )
    },
    text = {
      Text(
        text = strings.offlineMessage,
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
            Text(text = strings.retryLabel)
          }
        } else {
          Text(
            text = strings.retryLabel,
            fontWeight = FontWeight.SemiBold,
          )
        }
      }
    },
  )
}

private fun copyPlainText(context: Context, label: String, text: String) {
  val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
  clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
}

private fun copyShayariToClipboard(context: Context, item: ShayariItem) {
  copyPlainText(context, "Shayari", item.text)
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

private fun shareShayariOnWhatsApp(context: Context, appName: String, item: ShayariItem) {
  val shareText = "${item.text}\n\n— $appName ✨"
  val whatsappIntent =
    Intent(Intent.ACTION_SEND).apply {
      type = "text/plain"
      setPackage("com.whatsapp")
      putExtra(Intent.EXTRA_TEXT, shareText)
    }
  try {
    context.startActivity(whatsappIntent)
  } catch (_: Exception) {
    shareShayari(context, appName, item)
  }
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
