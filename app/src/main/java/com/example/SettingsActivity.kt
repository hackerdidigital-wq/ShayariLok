package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.data.ShayariRepository
import com.example.data.ShayariTextSize
import com.example.model.SupportedLanguage
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.i18n.appStringsFor
import com.example.ui.screens.AgeConfirmationDialog
import com.example.ui.theme.BadgePink
import com.example.ui.theme.CrownGold
import com.example.ui.theme.DarkBackgroundBottom
import com.example.ui.theme.ShayariLokTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SettingsActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    val repository = ShayariRepository(this)
    setContent {
      val config = remember { repository.loadCachedConfig() }
      var selectedLanguageCode by remember { mutableStateOf(repository.getSelectedLanguageCode()) }
      val isRtl =
        remember(selectedLanguageCode, config) {
          config.supportedLanguages.firstOrNull {
            it.code.equals(selectedLanguageCode, ignoreCase = true)
          }?.isRtl ?: (selectedLanguageCode.lowercase() in SupportedLanguage.RTL_LANGUAGE_CODES)
        }
      val strings = remember(selectedLanguageCode) { appStringsFor(selectedLanguageCode) }
      val layoutDir = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

      ShayariLokTheme(dynamicTheme = config.dynamicTheme) {
        CompositionLocalProvider(
          LocalAppStrings provides strings,
          LocalLayoutDirection provides layoutDir,
        ) {
          SettingsContent(
            repository = repository,
            selectedLanguageCode = selectedLanguageCode,
            onSelectLanguage = { code ->
              selectedLanguageCode = code
              repository.setSelectedLanguageCode(code)
            },
            onOpenPrivacyPolicy = {
              startActivity(Intent(this, PrivacyPolicyActivity::class.java))
            },
            onBack = { finish() },
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SettingsContent(
  repository: ShayariRepository,
  selectedLanguageCode: String,
  onSelectLanguage: (String) -> Unit,
  onOpenPrivacyPolicy: () -> Unit,
  onBack: () -> Unit,
) {
  val strings = LocalAppStrings.current
  val config = remember { repository.loadCachedConfig() }
  val lastSyncedMillis = remember { repository.getLastSyncedTimeMillis() }
  val hasRemoteCache = remember { repository.hasCachedRemoteConfig() }
  var selectedTextSize by remember { mutableStateOf(repository.getShayariTextSize()) }
  var allowAdultContent by remember { mutableStateOf(repository.isAdultContentAllowed()) }
  var showAgeConfirmDialog by remember { mutableStateOf(false) }

  val bgGradient =
    Brush.verticalGradient(
      colors = listOf(MaterialTheme.colorScheme.background, DarkBackgroundBottom)
    )

  val formattedSyncTime =
    remember(lastSyncedMillis, strings.neverSynced) {
      if (lastSyncedMillis <= 0L) {
        strings.neverSynced
      } else {
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(lastSyncedMillis))
      }
    }

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0f),
    contentWindowInsets = WindowInsets.safeDrawing,
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = strings.settingsTitle,
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
        // 1. Live App Language Selector
        Surface(
          shape = RoundedCornerShape(20.dp),
          color = MaterialTheme.colorScheme.surface,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
        ) {
          Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                painter = painterResource(id = R.drawable.ic_language),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
              )
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = strings.appLanguageTitle,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
              )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = strings.appLanguageSubtitle,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(10.dp))
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              config.supportedLanguages.forEach { lang ->
                val isSelected = lang.code.equals(selectedLanguageCode, ignoreCase = true)
                FilterChip(
                  selected = isSelected,
                  onClick = { onSelectLanguage(lang.code) },
                  label = {
                    Text(
                      text =
                        if (lang.nativeName.isNotBlank()) {
                          "${lang.nativeName} (${lang.name})"
                        } else {
                          lang.name
                        }
                    )
                  },
                  colors =
                    FilterChipDefaults.filterChipColors(
                      selectedContainerColor = MaterialTheme.colorScheme.primary,
                      selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                  modifier =
                    Modifier.minimumInteractiveComponentSize()
                      .testTag("settings_lang_${lang.code.lowercase()}"),
                )
              }
            }
          }
        }

        // 2. Reading Text Size Preference
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
                text = strings.readingTextSizeTitle,
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
                val labelText =
                  when (option) {
                    ShayariTextSize.COMPACT -> strings.sizeCompact
                    ShayariTextSize.MEDIUM -> strings.sizeMedium
                    ShayariTextSize.LARGE -> strings.sizeLarge
                  }
                FilterChip(
                  selected = isSelected,
                  onClick = {
                    selectedTextSize = option
                    repository.setShayariTextSize(option)
                  },
                  label = { Text(labelText) },
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

        // 3. Content Safety & 18+ Preferences
        Surface(
          shape = RoundedCornerShape(20.dp),
          color = MaterialTheme.colorScheme.surface,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
        ) {
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp)
                .testTag("settings_adult_content_row"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.weight(1f).padding(end = 12.dp),
            ) {
              Icon(
                painter = painterResource(id = R.drawable.ic_shield),
                contentDescription = null,
                tint = if (allowAdultContent) BadgePink else CrownGold,
                modifier = Modifier.size(22.dp),
              )
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = strings.adultContentTitle,
                  style = MaterialTheme.typography.titleMedium,
                  color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text =
                    if (allowAdultContent) {
                      strings.adultContentSubtitleOn
                    } else {
                      strings.adultContentSubtitleOff
                    },
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
            Switch(
              checked = allowAdultContent,
              onCheckedChange = { wantAdult ->
                if (!wantAdult) {
                  allowAdultContent = false
                  repository.setAdultContentAllowed(false)
                } else if (repository.isAgeVerified()) {
                  allowAdultContent = true
                  repository.setAdultContentAllowed(true)
                } else {
                  showAgeConfirmDialog = true
                }
              },
              colors =
                SwitchDefaults.colors(
                  checkedThumbColor = Color.White,
                  checkedTrackColor = BadgePink,
                ),
              modifier = Modifier.testTag("settings_adult_content_switch"),
            )
          }
        }

        // 4. Shayari Collection Status
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
                text = strings.collectionTitle,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primaryContainer,
              ) {
                Text(
                  text = if (hasRemoteCache) strings.liveSynced else strings.readyStatus,
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
                "${strings.lastUpdatedPrefix} $formattedSyncTime • ${config.shayariList.size} ${strings.shayariAvailableSuffix}",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        // 5. Privacy Policy (In-App Full Page)
        Surface(
          shape = RoundedCornerShape(20.dp),
          color = MaterialTheme.colorScheme.surface,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
        ) {
          val rowInteraction = remember { MutableInteractionSource() }
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .clickable(
                  interactionSource = rowInteraction,
                  indication = null,
                  onClick = onOpenPrivacyPolicy,
                )
                .minimumInteractiveComponentSize()
                .padding(horizontal = 18.dp, vertical = 16.dp)
                .testTag("privacy_policy_button"),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
              Text(
                text = strings.privacyPolicyTitle,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = strings.privacyPolicySubtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Icon(
              painter = painterResource(id = R.drawable.ic_shield),
              contentDescription = strings.privacyPolicyTitle,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(22.dp),
            )
          }
        }

        // 6. App & Developer Info
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
                text = strings.developerInfoTitle,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
              )
            }
            InfoMetaRow(label = strings.appNameLabel, value = config.appName)
            InfoMetaRow(label = strings.versionLabel, value = BuildConfig.VERSION_NAME)
            InfoMetaRow(
              label = strings.developerLabel,
              value = stringResource(R.string.developer_name),
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))
      }
    }
  }

  if (showAgeConfirmDialog) {
    AgeConfirmationDialog(
      onConfirm18Plus = {
        showAgeConfirmDialog = false
        allowAdultContent = true
        repository.setAdultContentAllowed(true)
      },
      onDismiss = { showAgeConfirmDialog = false },
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
