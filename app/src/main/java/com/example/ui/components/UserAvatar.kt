package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.rememberShayariImageBitmap
import com.example.ui.theme.BadgePurple
import com.example.ui.theme.CrownGold
import com.example.ui.theme.NeonPink

/**
 * Shared profile avatar component supporting gallery Base64 Data URIs, remote HTTPS photos,
 * and automatic gradient monogram fallback when no image is set or still loading.
 */
@Composable
fun UserAvatar(
  photoUrl: String,
  displayName: String,
  size: Dp,
  modifier: Modifier = Modifier,
  initialFontSize: TextUnit = (size.value * 0.38f).sp,
  showOnlineDot: Boolean = false,
  gradientColors: List<Color> = listOf(BadgePurple, NeonPink),
) {
  val bitmap = rememberShayariImageBitmap(photoUrl)

  Box(modifier = modifier.size(size)) {
    Box(
      contentAlignment = Alignment.Center,
      modifier =
        Modifier.fillMaxSize()
          .clip(CircleShape)
          .background(Brush.linearGradient(colors = gradientColors))
          .border(1.2.dp, CrownGold.copy(alpha = 0.55f), CircleShape),
    ) {
      if (bitmap != null) {
        Image(
          bitmap = bitmap,
          contentDescription = displayName.ifBlank { "Avatar" },
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize(),
        )
      } else {
        Text(
          text = displayName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "S",
          color = Color.White,
          fontWeight = FontWeight.ExtraBold,
          fontSize = initialFontSize,
        )
      }
    }

    if (showOnlineDot) {
      Surface(
        shape = CircleShape,
        color = Color(0xFF22C55E),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.surface),
        modifier =
          Modifier.size((size * 0.28f).coerceAtLeast(10.dp).coerceAtMost(14.dp))
            .align(Alignment.BottomEnd)
            .padding(1.dp),
      ) {}
    }
  }
}
