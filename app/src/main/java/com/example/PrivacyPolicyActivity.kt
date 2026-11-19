package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.data.ShayariRepository
import com.example.data.rememberShayariImageBitmap
import com.example.model.ShayariConfig
import com.example.model.ShayariPolicySection
import com.example.model.SupportedLanguage
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.i18n.appStringsFor
import com.example.ui.theme.LocalShayariColors
import com.example.ui.theme.ShayariLokTheme
import com.google.firebase.firestore.FirebaseFirestore

class PrivacyPolicyActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    val repository = ShayariRepository(this)
    setContent {
      val langCode = remember { repository.getSelectedLanguageCode() }
      var config by remember { mutableStateOf(repository.loadEffectiveCachedConfig()) }

      LaunchedEffect(Unit) {
        try {
          val dbId = getString(R.string.firestore_database_id).trim()
          if (dbId.isNotEmpty()) {
            val firestore = FirebaseFirestore.getInstance(dbId)
            repository.observeRealtimeShayariOverlay(firestore).collect { overlay ->
            val current = repository.loadEffectiveCachedConfig()
            val site = overlay.publicSiteConfig
            val app = overlay.appConfigOverride
            config =
              current.copy(
                appName = app.appName.ifBlank { site?.appName ?: current.appName },
                appLogoUrl = app.appLogoUrl.ifBlank { site?.appLogoUrl ?: current.appLogoUrl },
                privacyPolicyUrl =
                  app.privacyPolicyUrl.ifBlank { site?.privacyPolicyUrl ?: current.privacyPolicyUrl },
                privacyPolicyTitle =
                  app.privacyPolicyTitle.ifBlank { site?.privacyPolicyTitle ?: current.privacyPolicyTitle },
                privacyPolicyText =
                  app.privacyPolicyText.ifBlank { site?.privacyPolicyText ?: current.privacyPolicyText },
                privacyPolicySections =
                  app.privacyPolicySections?.takeIf { it.isNotEmpty() }
                    ?: site?.privacyPolicySections?.takeIf { it.isNotEmpty() }
                    ?: current.privacyPolicySections,
                dynamicTheme = app.dynamicTheme ?: site?.dynamicTheme ?: current.dynamicTheme,
              )
            }
          }
        } catch (_: Exception) {
          // Safe fallback to cached effective config when running offline or in unit tests
        }
      }

      val isDark = remember(config.dynamicTheme.darkMode) {
        repository.getUserDarkModeOverride() ?: config.dynamicTheme.darkMode
      }
      val isRtl =
        remember(langCode, config) {
          config.supportedLanguages.firstOrNull {
            it.code.equals(langCode, ignoreCase = true)
          }?.isRtl ?: (langCode.lowercase() in SupportedLanguage.RTL_LANGUAGE_CODES)
        }
      val strings = remember(langCode) { appStringsFor(langCode) }
      val layoutDir = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

      ShayariLokTheme(darkTheme = isDark, dynamicTheme = config.dynamicTheme) {
        CompositionLocalProvider(
          LocalAppStrings provides strings,
          LocalLayoutDirection provides layoutDir,
        ) {
          PrivacyPolicyScreen(
            config = config,
            onBack = { finish() },
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PrivacyPolicyScreen(
  config: ShayariConfig,
  onBack: () -> Unit,
) {
  val strings = LocalAppStrings.current
  val palette = LocalShayariColors.current
  val context = LocalContext.current
  val customLogoBitmap = rememberShayariImageBitmap(config.appLogoUrl)

  val resolvedTitle = config.privacyPolicyTitle.ifBlank { strings.privacyPolicyTitle }
  val resolvedAppName = config.appName.ifBlank { stringResource(R.string.app_name) }
  val resolvedSections =
    remember(config.privacyPolicySections, strings.privacySections) {
      if (config.privacyPolicySections.isNotEmpty()) {
        config.privacyPolicySections
      } else {
        strings.privacySections.map {
          ShayariPolicySection(number = it.number, title = it.title, body = it.body)
        }
      }
    }

  val bgGradient =
    Brush.verticalGradient(
      colors = listOf(palette.bgTop, palette.bgMid, palette.bgBottom)
    )

  Scaffold(
    containerColor = Color.Transparent,
    contentWindowInsets = WindowInsets.safeDrawing,
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = resolvedTitle,
            style = MaterialTheme.typography.titleLarge,
          )
        },
        navigationIcon = {
          IconButton(
            onClick = onBack,
            modifier =
              Modifier.minimumInteractiveComponentSize().testTag("privacy_back_button"),
          ) {
            Icon(
              painter = painterResource(id = R.drawable.ic_arrow_back),
              contentDescription = stringResource(R.string.cd_back),
              tint = Color.White,
            )
          }
        },
        colors =
          TopAppBarDefaults.topAppBarColors(
            containerColor = palette.headerStart,
            titleContentColor = Color.White,
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
        modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        Surface(
          shape = RoundedCornerShape(22.dp),
          color = palette.surface.copy(alpha = 0.96f),
          border = BorderStroke(1.dp, palette.accentViolet.copy(alpha = 0.45f)),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Surface(
              shape = CircleShape,
              color = palette.accentPurple,
              modifier = Modifier.size(50.dp),
            ) {
              Box(contentAlignment = Alignment.Center) {
                if (customLogoBitmap != null) {
                  Image(
                    bitmap = customLogoBitmap,
                    contentDescription = resolvedAppName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                  )
                } else {
                  Icon(
                    painter = painterResource(id = R.drawable.ic_shield),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp),
                  )
                }
              }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
              Text(
                text = resolvedAppName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary,
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = strings.privacyHeroSubtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textSecondary,
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "${strings.versionLabel} ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.labelSmall,
                color = palette.textMuted,
              )
            }
          }
        }

        if (config.privacyPolicyText.isNotBlank()) {
          Surface(
            shape = RoundedCornerShape(20.dp),
            color = palette.surface.copy(alpha = 0.96f),
            border = BorderStroke(1.dp, palette.accentGold.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth().testTag("privacy_admin_body_card"),
          ) {
            Text(
              text = config.privacyPolicyText,
              style = MaterialTheme.typography.bodyMedium,
              color = palette.textPrimary,
              modifier = Modifier.padding(18.dp),
            )
          }
        }

        resolvedSections.forEach { section ->
          Surface(
            shape = RoundedCornerShape(20.dp),
            color = palette.surface.copy(alpha = 0.96f),
            border = BorderStroke(1.dp, palette.outline.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.padding(18.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = palette.pillSurface,
                ) {
                  Text(
                    text = section.number,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = palette.accentGold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                  )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                  text = section.title,
                  style = MaterialTheme.typography.titleMedium,
                  color = palette.textPrimary,
                )
              }
              Spacer(modifier = Modifier.height(10.dp))
              Text(
                text = section.body,
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textSecondary,
              )
            }
          }
        }

        val externalPolicyUrl = normalizeExternalUrl(config.privacyPolicyUrl)
        if (externalPolicyUrl != null) {
          Button(
            onClick = {
              runCatching {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(externalPolicyUrl)))
              }
            },
            shape = RoundedCornerShape(16.dp),
            colors =
              ButtonDefaults.buttonColors(
                containerColor = palette.accentPurple,
                contentColor = Color.White,
              ),
            modifier =
              Modifier.fillMaxWidth()
                .minimumInteractiveComponentSize()
                .testTag("privacy_open_url_button"),
          ) {
            Icon(
              painter = painterResource(id = R.drawable.ic_open_in_new),
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = strings.viewFullPolicyOnline,
              fontWeight = FontWeight.SemiBold,
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))
      }
    }
  }
}
