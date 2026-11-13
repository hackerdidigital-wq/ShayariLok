package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.ShayariRepository
import com.example.data.ShayariTextSize
import com.example.ui.theme.DarkBackgroundBottom
import com.example.ui.theme.LightBackgroundBottom
import com.example.ui.theme.ShayariLokTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

class SettingsActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    val repository = ShayariRepository(this)
    setContent {
      ShayariLokTheme {
        SettingsContent(
          repository = repository,
          onOpenPrivacyPolicy = {
            startActivity(Intent(this, PrivacyPolicyActivity::class.java))
          },
          onOpenNotifications = {
            startActivity(Intent(this, NotificationsActivity::class.java))
          },
          onOpenExternalUrl = { url -> openExternalLink(url) },
          onBack = { finish() },
        )
      }
    }
  }

  private fun openExternalLink(rawUrl: String) {
    val trimmed = rawUrl.trim()
    if (trimmed.isEmpty()) return
    val safeUrl =
      if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
        trimmed
      } else {
        "https://$trimmed"
      }
    try {
      startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(safeUrl)))
    } catch (_: Exception) {
      // No browser available
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsContent(
  repository: ShayariRepository,
  onOpenPrivacyPolicy: () -> Unit,
  onOpenNotifications: () -> Unit,
  onOpenExternalUrl: (String) -> Unit,
  onBack: () -> Unit,
) {
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }

  var config by remember { mutableStateOf(repository.loadCachedConfig()) }
  var lastSyncedMillis by remember { mutableLongStateOf(repository.getLastSyncedTimeMillis()) }
  var hasRemoteCache by remember { mutableStateOf(repository.hasCachedRemoteConfig()) }
  var selectedTextSize by remember { mutableStateOf(repository.getShayariTextSize()) }
  var isSyncing by remember { mutableStateOf(false) }

  val isDark = isSystemInDarkTheme()
  val bgGradient =
    Brush.verticalGradient(
      colors =
        listOf(
          MaterialTheme.colorScheme.background,
          if (isDark) DarkBackgroundBottom else LightBackgroundBottom,
        )
    )

  val formattedSyncTime =
    remember(lastSyncedMillis) {
      if (lastSyncedMillis <= 0L) {
        "Starter Offline Bundle"
      } else {
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(lastSyncedMillis))
      }
    }

  fun triggerSync() {
    if (isSyncing) return
    scope.launch {
      isSyncing = true
      val result = repository.fetchRemoteConfig()
      result.fold(
        onSuccess = { updated ->
          config = updated
          lastSyncedMillis = repository.getLastSyncedTimeMillis()
          hasRemoteCache = repository.hasCachedRemoteConfig()
          snackbarHostState.showSnackbar("Updated ${updated.shayariList.size} Shayari")
        },
        onFailure = {
          snackbarHostState.showSnackbar("Offline mode: using saved local cache")
        },
      )
      isSyncing = false
    }
  }

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0f),
    contentWindowInsets = WindowInsets.safeDrawing,
    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.titleLarge,
          )
        },
        navigationIcon = {
          IconButton(
            onClick = onBack,
            modifier =
              Modifier.minimumInteractiveComponentSize().testTag("settings_back_button"),
          ) {
            Icon(
              painter = painterResource(id = R.drawable.ic_arrow_back),
              contentDescription = stringResource(R.string.cd_back),
            )
          }
        },
        colors =
          TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.94f),
            titleContentColor = MaterialTheme.colorScheme.onBackground,
          ),
      )
    },
  ) { innerPadding ->
    Box(
      modifier =
        Modifier.fillMaxSize()
          .background(bgGradient)
          .padding(innerPadding)
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 16.dp, vertical = 12.dp),
      contentAlignment = Alignment.TopCenter,
    ) {
      Column(
        modifier = Modifier.widthIn(max = 620.dp).fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        // 1. Live Sync & Cache Status
        Surface(
          shape = RoundedCornerShape(20.dp),
          color = MaterialTheme.colorScheme.surface,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
        ) {
          Column(modifier = Modifier.padding(18.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = stringResource(R.string.refresh_config_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primaryContainer,
              ) {
                Text(
                  text = if (hasRemoteCache) "Synced Cache" else "Offline Bundle",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.SemiBold,
                  color = MaterialTheme.colorScheme.onPrimaryContainer,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
              }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text =
                "Last updated: $formattedSyncTime • ${config.shayariList.size} Shayari in ${config.categories.size} categories",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(14.dp))
            Button(
              onClick = { triggerSync() },
              enabled = !isSyncing,
              modifier =
                Modifier.fillMaxWidth()
                  .minimumInteractiveComponentSize()
                  .testTag("sync_now_button"),
            ) {
              Icon(
                painter = painterResource(id = R.drawable.ic_refresh),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(text = if (isSyncing) "Syncing..." else "Sync Latest Shayari")
            }
          }
        }

        // 2. Reading Comfort (Font Size)
        Surface(
          shape = RoundedCornerShape(20.dp),
          color = MaterialTheme.colorScheme.surface,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
        ) {
          Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                painter = painterResource(id = R.drawable.ic_text_fields),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
              )
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = stringResource(R.string.reading_comfort_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
              )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              ShayariTextSize.entries.forEach { option ->
                val isSelected = selectedTextSize == option
                FilterChip(
                  selected = isSelected,
                  onClick = {
                    selectedTextSize = option
                    repository.setShayariTextSize(option)
                  },
                  label = { Text(option.label) },
                  colors =
                    FilterChipDefaults.filterChipColors(
                      selectedContainerColor = MaterialTheme.colorScheme.primary,
                      selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                  modifier =
                    Modifier.weight(1f)
                      .minimumInteractiveComponentSize()
                      .testTag("text_size_${option.name.lowercase()}"),
                )
              }
            }
          }
        }

        // 3. Legal, Notifications & Community (In-App Native Navigation)
        Surface(
          shape = RoundedCornerShape(20.dp),
          color = MaterialTheme.colorScheme.surface,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
        ) {
          Column(modifier = Modifier.fillMaxWidth()) {
            SettingsActionRow(
              title = stringResource(R.string.privacy_policy_title),
              subtitle = stringResource(R.string.privacy_policy_subtitle),
              iconRes = R.drawable.ic_shield,
              testTag = "privacy_policy_button",
              onClick = onOpenPrivacyPolicy,
            )
            HorizontalDivider(
              modifier = Modifier.padding(horizontal = 18.dp),
              color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
            )
            SettingsActionRow(
              title = stringResource(R.string.notifications_title),
              subtitle = "${config.notifications.size} active notices from feed",
              iconRes = R.drawable.ic_notification,
              testTag = "settings_notifications_button",
              onClick = onOpenNotifications,
            )
            if (config.socialLink.isNotBlank()) {
              HorizontalDivider(
                modifier = Modifier.padding(horizontal = 18.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
              )
              SettingsActionRow(
                title = stringResource(R.string.join_community_title),
                subtitle = config.socialLink,
                iconRes = R.drawable.ic_open_in_new,
                testTag = "social_link_button",
                onClick = { onOpenExternalUrl(config.socialLink) },
              )
            }
          }
        }

        // 4. App & Developer Info Card
        Surface(
          shape = RoundedCornerShape(20.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                painter = painterResource(id = R.drawable.ic_info),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = stringResource(R.string.developer_info_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
              )
            }
            InfoMetaRow(label = "App Name", value = config.appName)
            InfoMetaRow(
              label = "Version",
              value = "${BuildConfig.VERSION_NAME} (Code ${BuildConfig.VERSION_CODE})",
            )
            InfoMetaRow(label = "Package ID", value = BuildConfig.APPLICATION_ID)
            InfoMetaRow(label = "Developer", value = stringResource(R.string.developer_name))
            InfoMetaRow(label = "Architecture", value = "Pure Native • R8 Ultra-Lightweight")
          }
        }

        Spacer(modifier = Modifier.height(12.dp))
      }
    }
  }
}

@Composable
private fun SettingsActionRow(
  title: String,
  subtitle: String,
  iconRes: Int,
  testTag: String,
  onClick: () -> Unit,
) {
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .clickable(onClick = onClick)
        .minimumInteractiveComponentSize()
        .padding(horizontal = 18.dp, vertical = 16.dp)
        .testTag(testTag),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Icon(
      painter = painterResource(id = iconRes),
      contentDescription = title,
      tint = MaterialTheme.colorScheme.primary,
      modifier = Modifier.size(22.dp),
    )
  }
}

@Composable
private fun InfoMetaRow(label: String, value: String) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
      text = value,
      style = MaterialTheme.typography.labelLarge,
      color = MaterialTheme.colorScheme.onSurface,
    )
  }
}
