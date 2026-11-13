package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.ShayariTextSize
import com.example.model.ShayariItem
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
      targetValue =
        if (isFavorite) {
          MaterialTheme.colorScheme.secondary
        } else {
          MaterialTheme.colorScheme.onSurfaceVariant
        },
      animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
      label = "favoriteColor",
    )

  val cardShape = RoundedCornerShape(22.dp)
  val accentGradient =
    Brush.horizontalGradient(
      colors =
        listOf(
          MaterialTheme.colorScheme.primary.copy(alpha = 0.07f),
          MaterialTheme.colorScheme.secondary.copy(alpha = 0.04f),
          MaterialTheme.colorScheme.surface,
        )
    )

  ElevatedCard(
    shape = cardShape,
    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
    modifier = modifier.fillMaxWidth().testTag("shayari_card_${item.id}"),
  ) {
    Surface(
      shape = cardShape,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
      color = MaterialTheme.colorScheme.surface.copy(alpha = 0f),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().background(brush = accentGradient)
      ) {
        // Top strip: Category pill + Favorite icon button
        Row(
          modifier = Modifier.fillMaxWidth().padding(start = 18.dp, end = 8.dp, top = 12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.primaryContainer,
          ) {
            Text(
              text = item.category,
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            )
          }

          IconButton(
            onClick = { onFavoriteToggle(item.id) },
            modifier =
              Modifier.minimumInteractiveComponentSize().testTag("favorite_button_${item.id}"),
          ) {
            Icon(
              painter =
                painterResource(
                  id = if (isFavorite) R.drawable.ic_heart_filled else R.drawable.ic_heart
                ),
              contentDescription = stringResource(R.string.action_favorite),
              tint = heartColor,
              modifier = Modifier.size(22.dp),
            )
          }
        }

        // Shayari Couplet Body
        Box(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 14.dp)
        ) {
          // Subtle decorative opening quotation mark in background
          Text(
            text = "“",
            fontSize = 54.sp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
            modifier = Modifier.align(Alignment.TopStart),
          )

          Column(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Text(
              text = item.text,
              style =
                MaterialTheme.typography.bodyLarge.copy(
                  fontSize = textSize.fontSizeSp.sp,
                  lineHeight = textSize.lineHeightSp.sp,
                ),
              color = MaterialTheme.colorScheme.onSurface,
              textAlign = TextAlign.Center,
              modifier = Modifier.fillMaxWidth(),
            )
            if (item.author.isNotBlank()) {
              Spacer(modifier = Modifier.height(10.dp))
              Text(
                text = "— ${item.author}",
                style = MaterialTheme.typography.labelSmall,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }

        HorizontalDivider(
          modifier = Modifier.padding(horizontal = 16.dp),
          color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
        )

        // Bottom Action Bar: Copy & Share buttons (>=48dp touch height)
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
          horizontalArrangement = Arrangement.SpaceEvenly,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          TextButton(
            onClick = {
              justCopied = true
              onCopyClick(item)
            },
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            modifier =
              Modifier.weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .minimumInteractiveComponentSize()
                .testTag("copy_button_${item.id}"),
          ) {
            Icon(
              painter =
                painterResource(
                  id = if (justCopied) R.drawable.ic_check else R.drawable.ic_copy
                ),
              contentDescription = stringResource(R.string.action_copy),
              tint =
                if (justCopied) {
                  MaterialTheme.colorScheme.secondary
                } else {
                  MaterialTheme.colorScheme.primary
                },
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text =
                if (justCopied) {
                  stringResource(R.string.action_copied)
                } else {
                  stringResource(R.string.action_copy)
                },
              style = MaterialTheme.typography.labelLarge,
              color =
                if (justCopied) {
                  MaterialTheme.colorScheme.secondary
                } else {
                  MaterialTheme.colorScheme.primary
                },
            )
          }

          Box(
            modifier =
              Modifier.width(1.dp)
                .height(22.dp)
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))
          )

          TextButton(
            onClick = { onShareClick(item) },
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            modifier =
              Modifier.weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .minimumInteractiveComponentSize()
                .testTag("share_button_${item.id}"),
          ) {
            Icon(
              painter = painterResource(id = R.drawable.ic_share),
              contentDescription = stringResource(R.string.action_share),
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = stringResource(R.string.action_share),
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.primary,
            )
          }
        }
      }
    }
  }
}
