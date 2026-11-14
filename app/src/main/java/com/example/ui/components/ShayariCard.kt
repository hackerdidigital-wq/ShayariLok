package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.ShayariTextSize
import com.example.model.ShayariItem
import com.example.ui.theme.CrownGold
import com.example.ui.theme.NeonPink
import kotlinx.coroutines.delay

@Composable
fun ShayariCard(
  item: ShayariItem,
  isFavorite: Boolean,
  textSize: ShayariTextSize = ShayariTextSize.MEDIUM,
  onCopyClick: (ShayariItem) -> Unit,
  onShareClick: (ShayariItem) -> Unit,
  onFavoriteToggle: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  var justCopied by remember(item.id) { mutableStateOf(false) }

  LaunchedEffect(justCopied) {
    if (justCopied) {
      delay(1600)
      justCopied = false
    }
  }

  val heartColor by
    animateColorAsState(
      targetValue = if (isFavorite) NeonPink else Color.White.copy(alpha = 0.92f),
      animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
      label = "favoriteColor",
    )

  val backdropRes =
    remember(item.id, item.category) {
      when {
        item.category.equals("Sad", ignoreCase = true) -> R.drawable.img_card_silhouette
        item.category.equals("Motivation", ignoreCase = true) ||
          item.category.equals("Motivational", ignoreCase = true) -> R.drawable.img_card_roses
        item.category.equals("Attitude", ignoreCase = true) -> R.drawable.img_card_roses
        item.id % 3 == 2 -> R.drawable.img_card_silhouette
        item.id % 3 == 0 -> R.drawable.img_card_roses
        else -> R.drawable.img_card_sunset
      }
    }

  val cardShape = RoundedCornerShape(16.dp)

  ElevatedCard(
    shape = cardShape,
    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
    colors = CardDefaults.elevatedCardColors(containerColor = Color(0xFF100C18)),
    modifier = modifier.fillMaxWidth().testTag("shayari_card_${item.id}"),
  ) {
    Box(modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp)) {
      Image(
        painter = painterResource(id = backdropRes),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.matchParentSize(),
      )

      // Smooth left-to-right gradient scrim matching the reference card layout
      Box(
        modifier =
          Modifier.matchParentSize()
            .background(
              Brush.horizontalGradient(
                0.0f to Color(0x140A0810),
                0.35f to Color(0x880A0810),
                0.62f to Color(0xDC0A0810),
                1.0f to Color(0xF20A0810),
              )
            )
      )

      Column(
        modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.SpaceBetween,
      ) {
        Spacer(modifier = Modifier.height(4.dp))

        // Right-aligned crisp Hindi/Hinglish Shayari text
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Spacer(modifier = Modifier.weight(0.30f))
          Text(
            text = item.text,
            style =
              MaterialTheme.typography.bodyLarge.copy(
                fontSize = textSize.fontSizeSp.sp,
                lineHeight = textSize.lineHeightSp.sp,
                fontWeight = FontWeight.Medium,
              ),
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(0.70f),
          )
        }

        // Bottom-right clean realistic icon actions (no blue tap flash, >=48dp touch target)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          CardIconAction(
            iconRes = if (justCopied) R.drawable.ic_check else R.drawable.ic_copy,
            contentDescription = stringResource(R.string.action_copy),
            tint = if (justCopied) CrownGold else Color.White.copy(alpha = 0.88f),
            testTag = "copy_button_${item.id}",
            onClick = {
              justCopied = true
              onCopyClick(item)
            },
          )
          Spacer(modifier = Modifier.width(4.dp))
          CardIconAction(
            iconRes = R.drawable.ic_share,
            contentDescription = stringResource(R.string.action_share),
            tint = Color.White.copy(alpha = 0.88f),
            testTag = "share_button_${item.id}",
            onClick = { onShareClick(item) },
          )
          Spacer(modifier = Modifier.width(4.dp))
          CardIconAction(
            iconRes = if (isFavorite) R.drawable.ic_heart_filled else R.drawable.ic_heart,
            contentDescription = stringResource(R.string.action_favorite),
            tint = heartColor,
            testTag = "favorite_button_${item.id}",
            onClick = { onFavoriteToggle(item.id) },
          )
        }
      }
    }
  }
}

@Composable
private fun CardIconAction(
  iconRes: Int,
  contentDescription: String,
  tint: Color,
  testTag: String,
  onClick: () -> Unit,
) {
  val interactionSource = remember { MutableInteractionSource() }
  Box(
    contentAlignment = Alignment.Center,
    modifier =
      Modifier.minimumInteractiveComponentSize()
        .size(44.dp)
        .clickable(
          interactionSource = interactionSource,
          indication = null,
          onClick = onClick,
        )
        .testTag(testTag),
  ) {
    Icon(
      painter = painterResource(id = iconRes),
      contentDescription = contentDescription,
      tint = tint,
      modifier = Modifier.size(21.dp),
    )
  }
}
