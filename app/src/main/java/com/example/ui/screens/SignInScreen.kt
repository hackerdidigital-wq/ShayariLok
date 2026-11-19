package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.GoogleAuthHelper
import com.example.ui.theme.BadgePurple
import com.example.ui.theme.CrownGold
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.DeepRoyalPurple
import com.example.ui.theme.MidnightIndigo
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonVioletGlow

import androidx.compose.foundation.Image
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import com.example.data.rememberShayariImageBitmap

@Composable
fun SignInScreen(
  appLogoUrl: String = "",
  onAuthSuccess: () -> Unit,
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }
  val customLogoBitmap = rememberShayariImageBitmap(appLogoUrl)

  var isLoading by remember { mutableStateOf(false) }
  var isCheckingAutoSignIn by remember { mutableStateOf(true) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(Unit) {
    GoogleAuthHelper.attemptAutoSignIn(
      context = context,
      credentialManager = credentialManager,
      onAuthSuccess = {
        isCheckingAutoSignIn = false
        onAuthSuccess()
      },
      onUnauthenticated = {
        isCheckingAutoSignIn = false
      },
      scope = coroutineScope,
    )
  }

  Box(
    contentAlignment = Alignment.Center,
    modifier =
      Modifier.fillMaxSize()
        .background(
          Brush.verticalGradient(
            colors = listOf(DeepRoyalPurple, MidnightIndigo, Color(0xFF0E051E))
          )
        )
        .windowInsetsPadding(WindowInsets.safeDrawing)
        .padding(horizontal = 24.dp, vertical = 20.dp),
  ) {
    Surface(
      shape = RoundedCornerShape(28.dp),
      color = DarkSurface.copy(alpha = 0.94f),
      border = BorderStroke(1.2.dp, NeonVioletGlow.copy(alpha = 0.48f)),
      shadowElevation = 12.dp,
      modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth(),
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp),
      ) {
        Surface(
          shape = CircleShape,
          color = DarkSurfaceVariant,
          border = BorderStroke(1.5.dp, CrownGold.copy(alpha = 0.78f)),
          modifier = Modifier.size(84.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            if (customLogoBitmap != null) {
              Image(
                bitmap = customLogoBitmap,
                contentDescription = stringResource(R.string.app_name),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(CircleShape),
              )
            } else {
              Icon(
                painter = painterResource(id = R.drawable.ic_crown),
                contentDescription = stringResource(R.string.app_name),
                tint = CrownGold,
                modifier = Modifier.size(44.dp),
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
          text = "Live Shayari",
          style = MaterialTheme.typography.headlineMedium,
          fontWeight = FontWeight.ExtraBold,
          color = Color.White,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
          text = stringResource(R.string.app_name),
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = CrownGold,
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
          text = stringResource(R.string.brand_tagline),
          style = MaterialTheme.typography.bodyMedium,
          color = Color(0xFFD9CCEE),
          textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(22.dp))

        Surface(
          shape = RoundedCornerShape(18.dp),
          color = DarkSurfaceVariant.copy(alpha = 0.8f),
          border = BorderStroke(0.8.dp, NeonVioletGlow.copy(alpha = 0.35f)),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(16.dp),
          ) {
            FeatureBullet(iconRes = R.drawable.ic_refresh, text = "Real-time Shayari & Admin Updates")
            FeatureBullet(iconRes = R.drawable.ic_chat, text = "Live 1-on-1 Chat & Follow Friends")
            FeatureBullet(iconRes = R.drawable.ic_shield, text = "One-tap Google Account Sync")
          }
        }

        if (!errorMessage.isNullOrBlank()) {
          Spacer(modifier = Modifier.height(14.dp))
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF4A1025),
            border = BorderStroke(0.8.dp, NeonPink.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = errorMessage!!,
              style = MaterialTheme.typography.bodySmall,
              color = Color(0xFFFFD4E0),
              textAlign = TextAlign.Center,
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            )
          }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
          onClick = {
            errorMessage = null
            isLoading = true
            GoogleAuthHelper.onGoogleSignInClicked(
              context = context,
              credentialManager = credentialManager,
              onAuthSuccess = {
                isLoading = false
                onAuthSuccess()
              },
              onAuthError = { msg ->
                isLoading = false
                errorMessage = msg
              },
              scope = coroutineScope,
              onAuthCancelled = { hintMessage ->
                isLoading = false
                if (!hintMessage.isNullOrBlank()) {
                  errorMessage = hintMessage
                }
              },
            )
          },
          enabled = !isLoading && !isCheckingAutoSignIn,
          shape = RoundedCornerShape(16.dp),
          colors =
            ButtonDefaults.buttonColors(
              containerColor = Color.White,
              contentColor = Color(0xFF1F1F1F),
              disabledContainerColor = Color.White.copy(alpha = 0.72f),
              disabledContentColor = Color(0xFF444444),
            ),
          elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
          modifier =
            Modifier.fillMaxWidth()
              .height(54.dp)
              .minimumInteractiveComponentSize()
              .testTag("google_sign_in_button"),
        ) {
          if (isLoading || isCheckingAutoSignIn) {
            CircularProgressIndicator(
              modifier = Modifier.size(22.dp),
              color = BadgePurple,
              strokeWidth = 2.4.dp,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
              text =
                if (isCheckingAutoSignIn) {
                  "Checking Google Account..."
                } else {
                  "Signing in..."
                },
              fontWeight = FontWeight.Bold,
              fontSize = 15.sp,
            )
          } else {
            GoogleColorMark(modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(
              text = stringResource(R.string.sign_in_with_google),
              fontWeight = FontWeight.Bold,
              fontSize = 16.sp,
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
          text = "No email/password signup needed • Powered by Google",
          style = MaterialTheme.typography.labelSmall,
          color = Color(0xFFBBAAD9),
          textAlign = TextAlign.Center,
        )
      }
    }
  }
}

@Composable
private fun FeatureBullet(iconRes: Int, text: String) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Surface(
      shape = CircleShape,
      color = NeonPurple.copy(alpha = 0.28f),
      modifier = Modifier.size(28.dp),
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          painter = painterResource(id = iconRes),
          contentDescription = null,
          tint = CrownGold,
          modifier = Modifier.size(15.dp),
        )
      }
    }
    Spacer(modifier = Modifier.width(10.dp))
    Text(
      text = text,
      style = MaterialTheme.typography.bodySmall,
      color = Color.White.copy(alpha = 0.92f),
      fontWeight = FontWeight.Medium,
    )
  }
}

@Composable
private fun GoogleColorMark(modifier: Modifier = Modifier) {
  Canvas(modifier = modifier) {
    if (size.minDimension <= 1f) return@Canvas
    val strokeWidth = size.minDimension * 0.22f
    val inset = strokeWidth / 2f
    val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
    val topLeft = Offset(inset, inset)

    // Red top arc
    drawArc(
      color = Color(0xFFEA4335),
      startAngle = -145f,
      sweepAngle = 100f,
      useCenter = false,
      topLeft = topLeft,
      size = arcSize,
      style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
    )
    // Yellow left arc
    drawArc(
      color = Color(0xFFFBBC05),
      startAngle = 145f,
      sweepAngle = 70f,
      useCenter = false,
      topLeft = topLeft,
      size = arcSize,
      style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
    )
    // Green bottom arc
    drawArc(
      color = Color(0xFF34A853),
      startAngle = 45f,
      sweepAngle = 100f,
      useCenter = false,
      topLeft = topLeft,
      size = arcSize,
      style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
    )
    // Blue right arc
    drawArc(
      color = Color(0xFF4285F4),
      startAngle = -10f,
      sweepAngle = 60f,
      useCenter = false,
      topLeft = topLeft,
      size = arcSize,
      style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
    )
    // Horizontal bar of 'G'
    val midY = size.height / 2f
    drawLine(
      color = Color(0xFF4285F4),
      start = Offset(size.width * 0.52f, midY),
      end = Offset(size.width - inset, midY),
      strokeWidth = strokeWidth,
      cap = StrokeCap.Round,
    )
  }
}
