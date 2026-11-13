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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.DarkBackgroundBottom
import com.example.ui.theme.LightBackgroundBottom
import com.example.ui.theme.ShayariLokTheme

class PrivacyPolicyActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      ShayariLokTheme {
        PrivacyPolicyScreen(onBack = { finish() })
      }
    }
  }
}

private data class PolicySection(
  val number: String,
  val title: String,
  val body: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PrivacyPolicyScreen(onBack: () -> Unit) {
  val isDark = isSystemInDarkTheme()
  val bgGradient =
    Brush.verticalGradient(
      colors =
        listOf(
          MaterialTheme.colorScheme.background,
          if (isDark) DarkBackgroundBottom else LightBackgroundBottom,
        )
    )

  val sections =
    listOf(
      PolicySection(
        number = "01",
        title = "No Login & Zero Personal Data",
        body =
          "Shayari Lok is designed as a privacy-first, no-login reading app. We never ask you to create an account, sign in, or share personal details such as your name, email address, phone number, contacts, or precise location.",
      ),
      PolicySection(
        number = "02",
        title = "Local Offline Storage (SharedPreferences)",
        body =
          "Your saved favorite Shayari items, reading text size preference, and the last synced Shayari catalog are stored strictly on your device inside Android SharedPreferences. Clearing app data or uninstalling the app deletes all local records immediately.",
      ),
      PolicySection(
        number = "03",
        title = "Read-Only Network Sync",
        body =
          "The app requests only minimal standard network permissions (INTERNET and ACCESS_NETWORK_STATE) to fetch public, read-only Shayari updates and app announcements from the remote configuration feed. No analytics trackers, behavioral profiling, or invasive SDKs are bundled.",
      ),
      PolicySection(
        number = "04",
        title = "Clipboard & System Share Sheet",
        body =
          "When you tap Copy or Share on a Shayari card, the app interacts only with Android's built-in ClipboardManager or system share chooser on your explicit action. Clipboard contents are never read in the background.",
      ),
      PolicySection(
        number = "05",
        title = "Updates & Contact Information",
        body =
          "This privacy policy applies to Shayari Lok (${BuildConfig.APPLICATION_ID}). As new features or content categories are added, any policy updates will be reflected directly inside this screen alongside the current version (${BuildConfig.VERSION_NAME}).",
      ),
    )

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0f),
    contentWindowInsets = WindowInsets.safeDrawing,
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = stringResource(R.string.privacy_policy_title),
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
        modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        // Hero Summary Card
        Surface(
          shape = RoundedCornerShape(22.dp),
          color = MaterialTheme.colorScheme.primaryContainer,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Surface(
              shape = CircleShape,
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(48.dp),
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  painter = painterResource(id = R.drawable.ic_shield),
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onPrimary,
                  modifier = Modifier.size(24.dp),
                )
              }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
              Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = "100% Offline-First & No-Login Policy",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Package: ${BuildConfig.APPLICATION_ID} • v${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
              )
            }
          }
        }

        sections.forEach { section ->
          Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.padding(18.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                  Text(
                    text = section.number,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                  )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                  text = section.title,
                  style = MaterialTheme.typography.titleMedium,
                  color = MaterialTheme.colorScheme.onSurface,
                )
              }
              Spacer(modifier = Modifier.height(10.dp))
              Text(
                text = section.body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))
      }
    }
  }
}
