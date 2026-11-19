package com.example.ui.screens

import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import com.example.R
import com.example.data.rememberShayariImageBitmap
import com.example.ui.theme.CrownGold
import com.example.ui.theme.DeepRoyalPurple
import com.example.ui.theme.HeaderPurpleEnd
import com.example.ui.theme.HeaderPurpleStart
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonVioletGlow
import com.example.ui.theme.WarmAmber
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ShayariSplashScreen(
  appName: String,
  appLogoUrl: String = "",
  modifier: Modifier = Modifier,
) {
  val customLogoBitmap = rememberShayariImageBitmap(appLogoUrl)
  val infiniteTransition = rememberInfiniteTransition(label = "splashVfx")

  val pulseScale by
    infiniteTransition.animateFloat(
      initialValue = 0.96f,
      targetValue = 1.06f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = 1200, easing = EaseInOutSine),
          repeatMode = RepeatMode.Reverse,
        ),
      label = "crownPulse",
    )

  val particlePhase by
    infiniteTransition.animateFloat(
      initialValue = 0f,
      targetValue = 1f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = 4800, easing = LinearEasing),
          repeatMode = RepeatMode.Restart,
        ),
      label = "particlePhase",
    )

  val shimmerProgress by
    infiniteTransition.animateFloat(
      initialValue = 0f,
      targetValue = 1f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = 1350, easing = EaseInOutSine),
          repeatMode = RepeatMode.Reverse,
        ),
      label = "loaderProgress",
    )

  Box(
    modifier =
      modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            colors =
              listOf(
                Color(0xFF100424),
                DeepRoyalPurple,
                HeaderPurpleStart,
                Color(0xFF190633),
              )
          )
        )
        .testTag("splash_loading_screen"),
    contentAlignment = Alignment.Center,
  ) {
    // Ambient radial neon aura & drifting sparkle/heart VFX
    Canvas(modifier = Modifier.fillMaxSize()) {
      if (size.minDimension <= 1f) return@Canvas
      drawCircle(
        brush =
          Brush.radialGradient(
            colors = listOf(NeonPink.copy(alpha = 0.24f), Color.Transparent),
            center = Offset(size.width * 0.82f, size.height * 0.18f),
            radius = size.minDimension * 0.55f,
          ),
        center = Offset(size.width * 0.82f, size.height * 0.18f),
        radius = size.minDimension * 0.55f,
      )
      drawCircle(
        brush =
          Brush.radialGradient(
            colors = listOf(NeonPurple.copy(alpha = 0.34f), Color.Transparent),
            center = Offset(size.width * 0.2f, size.height * 0.72f),
            radius = size.minDimension * 0.65f,
          ),
        center = Offset(size.width * 0.2f, size.height * 0.72f),
        radius = size.minDimension * 0.65f,
      )

      val angle = particlePhase * 2f * PI.toFloat()
      val sparks =
        listOf(
          Triple(0.16f, 0.22f, CrownGold),
          Triple(0.84f, 0.28f, NeonPink),
          Triple(0.24f, 0.78f, NeonVioletGlow),
          Triple(0.76f, 0.72f, CrownGold),
          Triple(0.50f, 0.16f, NeonPink),
        )
      sparks.forEachIndexed { idx, (fx, fy, color) ->
        val driftX = cos(angle + idx * 1.1f) * 14f
        val driftY = sin(angle + idx * 1.4f) * 18f
        val center = Offset(size.width * fx + driftX, size.height * fy + driftY)
        if (idx % 2 == 1) {
          drawMiniHeart(center = center, radius = 14f, color = color.copy(alpha = 0.55f))
        } else {
          drawCircle(
            color = color.copy(alpha = 0.45f),
            radius = 5.5f,
            center = center,
          )
        }
      }
    }

    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      modifier = Modifier.safeDrawingPadding().padding(horizontal = 28.dp),
    ) {
      // Glowing Poster Emblem Badge
      Box(
        contentAlignment = Alignment.Center,
        modifier =
          Modifier.size(116.dp)
            .scale(pulseScale)
            .background(
              brush =
                Brush.radialGradient(
                  colors =
                    listOf(
                      CrownGold.copy(alpha = 0.35f),
                      NeonPink.copy(alpha = 0.16f),
                      Color.Transparent,
                    )
                ),
              shape = CircleShape,
            ),
      ) {
        Surface(
          shape = CircleShape,
          color = Color(0xFF280B57),
          shadowElevation = 12.dp,
          modifier =
            Modifier.size(86.dp)
              .border(
                width = 2.dp,
                brush = Brush.linearGradient(colors = listOf(CrownGold, NeonPink, NeonVioletGlow)),
                shape = CircleShape,
              ),
        ) {
          Box(contentAlignment = Alignment.Center) {
            if (customLogoBitmap != null) {
              Image(
                bitmap = customLogoBitmap,
                contentDescription = appName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(CircleShape),
              )
            } else {
              Image(
                painter = painterResource(id = R.drawable.ic_crown),
                contentDescription = appName,
                modifier = Modifier.size(50.dp),
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(22.dp))

      // "Live Shayari" Poster Title
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = "Live ",
          fontFamily = FontFamily.Serif,
          fontWeight = FontWeight.ExtraBold,
          fontStyle = FontStyle.Italic,
          fontSize = 34.sp,
          color = CrownGold,
        )
        Text(
          text = "Shayari",
          fontFamily = FontFamily.Serif,
          fontWeight = FontWeight.ExtraBold,
          fontStyle = FontStyle.Italic,
          fontSize = 34.sp,
          color = Color.White,
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Poster-matched Golden Brush Pill ("Shayari Lok")
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = CrownGold,
        shadowElevation = 6.dp,
      ) {
        Text(
          text = appName,
          fontFamily = FontFamily.Serif,
          fontWeight = FontWeight.ExtraBold,
          fontStyle = FontStyle.Italic,
          fontSize = 20.sp,
          color = Color(0xFF1A0736),
          modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      Text(
        text = stringResource(R.string.brand_tagline),
        style = MaterialTheme.typography.titleMedium,
        fontStyle = FontStyle.Italic,
        color = Color(0xFFFFD6EC),
        textAlign = TextAlign.Center,
      )

      Spacer(modifier = Modifier.height(34.dp))

      // Smooth Neon Progress Bar
      Box(
        modifier =
          Modifier.widthIn(max = 220.dp)
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.14f))
      ) {
        Box(
          modifier =
            Modifier.fillMaxWidth(0.32f + 0.58f * shimmerProgress)
              .fillMaxHeight()
              .clip(RoundedCornerShape(50))
              .background(
                Brush.horizontalGradient(
                  colors = listOf(NeonPink, HeaderPurpleEnd, CrownGold, WarmAmber)
                )
              )
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      Text(
        text = stringResource(R.string.splash_loading_subtitle),
        style = MaterialTheme.typography.labelLarge,
        color = Color.White.copy(alpha = 0.78f),
        textAlign = TextAlign.Center,
      )
    }
  }
}

private fun DrawScope.drawMiniHeart(
  center: Offset,
  radius: Float,
  color: Color,
) {
  val path =
    Path().apply {
      moveTo(center.x, center.y + radius * 0.7f)
      cubicTo(
        center.x - radius * 1.1f,
        center.y + radius * 0.05f,
        center.x - radius * 0.9f,
        center.y - radius * 0.85f,
        center.x,
        center.y - radius * 0.3f,
      )
      cubicTo(
        center.x + radius * 0.9f,
        center.y - radius * 0.85f,
        center.x + radius * 1.1f,
        center.y + radius * 0.05f,
        center.x,
        center.y + radius * 0.7f,
      )
      close()
    }
  drawPath(path = path, color = color)
}
