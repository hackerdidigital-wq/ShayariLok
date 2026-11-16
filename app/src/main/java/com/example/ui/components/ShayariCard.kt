package com.example.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.ShayariTextSize
import com.example.data.rememberShayariImageBitmap
import com.example.model.ShayariItem
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.CrownGold
import com.example.ui.theme.LocalShayariColors
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonVioletGlow
import kotlinx.coroutines.delay

@Composable
fun ShayariCard(
  item: ShayariItem,
  isFavorite: Boolean,
  textSize: ShayariTextSize = ShayariTextSize.MEDIUM,
  whatsappShareEnabled: Boolean = true,
  onCopyClick: (ShayariItem) -> Unit,
  onShareClick: (ShayariItem) -> Unit,
  onWhatsAppClick: (ShayariItem) -> Unit = onShareClick,
  onFavoriteToggle: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  val strings = LocalAppStrings.current
  val palette = LocalShayariColors.current
  var justCopied by remember(item.id) { mutableStateOf(false) }

  // Smooth entrance animation per card
  val entranceAlpha = remember(item.id) { Animatable(0f) }
  val entranceOffsetY = remember(item.id) { Animatable(18f) }
  LaunchedEffect(item.id) {
    entranceAlpha.animateTo(1f, tween(durationMillis = 280, easing = FastOutSlowInEasing))
  }
  LaunchedEffect(item.id) {
    entranceOffsetY.animateTo(
      0f,
      spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
    )
  }

  LaunchedEffect(justCopied) {
    if (justCopied) {
      delay(1600)
      justCopied = false
    }
  }

  // Heart burst spring VFX when favoriting
  val heartScale by
    animateFloatAsState(
      targetValue = if (isFavorite) 1.18f else 1f,
      animationSpec =
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
      label = "heartBurst",
    )

  val heartColor by
    animateColorAsState(
      targetValue = if (isFavorite) NeonPink else palette.textSecondary,
      animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
      label = "favoriteColor",
    )

  val remoteOrGalleryBitmap = rememberShayariImageBitmap(item.imageUrl)

  val fallbackBackdropRes =
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

  val cardShape = RoundedCornerShape(18.dp)
  val thumbShape = RoundedCornerShape(14.dp)

  ElevatedCard(
    shape = cardShape,
    elevation = CardDefaults.elevatedCardElevation(defaultElevation = if (palette.isDark) 6.dp else 3.dp),
    colors = CardDefaults.elevatedCardColors(containerColor = palette.cardGradientStart),
    modifier =
      modifier
        .fillMaxWidth()
        .graphicsLayer {
          alpha = entranceAlpha.value
          translationY = entranceOffsetY.value
        }
        .border(
          width = 1.dp,
          brush =
            Brush.linearGradient(
              colors =
                listOf(
                  palette.accentViolet.copy(alpha = if (palette.isDark) 0.45f else 0.28f),
                  palette.accentPink.copy(alpha = if (palette.isDark) 0.25f else 0.18f),
                  palette.accentGold.copy(alpha = if (palette.isDark) 0.28f else 0.22f),
                )
            ),
          shape = cardShape,
        )
        .testTag("shayari_card_${item.id}"),
  ) {
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .background(
            Brush.horizontalGradient(
              colors =
                if (palette.isDark) {
                  listOf(
                    Color(0xFF1C0A3A),
                    Color(0xFF130726),
                    Color(0xFF0E061B),
                  )
                } else {
                  listOf(
                    palette.cardGradientStart,
                    palette.cardGradientEnd,
                  )
                }
            )
          )
          .padding(12.dp),
      verticalAlignment = Alignment.Top,
    ) {
      // Compact Proportional Side Thumbnail (fixed aspect box so multi-line text never stretches it)
      Box(
        modifier =
          Modifier.size(width = 104.dp, height = 114.dp)
            .clip(thumbShape)
            .background(
              Brush.linearGradient(
                colors = listOf(Color(0xFF421970), Color(0xFF7B2163), Color(0xFF210B3E))
              )
            )
            .border(
              width = 1.1.dp,
              brush =
                Brush.verticalGradient(
                  colors = listOf(CrownGold.copy(alpha = 0.65f), NeonPink.copy(alpha = 0.55f))
                ),
              shape = thumbShape,
            )
            .testTag("shayari_thumbnail_${item.id}")
      ) {
        Crossfade(
          targetState = remoteOrGalleryBitmap,
          animationSpec = tween(durationMillis = 320),
          modifier = Modifier.fillMaxSize(),
          label = "thumbnailCrossfade",
        ) { loadedBitmap ->
          if (loadedBitmap != null) {
            Image(
              bitmap = loadedBitmap,
              contentDescription = item.category,
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize(),
            )
          } else {
            Image(
              painter = painterResource(id = fallbackBackdropRes),
              contentDescription = item.category,
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize(),
            )
          }
        }

        // Subtle inner edge vignette so the framed photo blends softly without darkening artwork
        Box(
          modifier =
            Modifier.matchParentSize()
              .background(
                Brush.horizontalGradient(
                  0.0f to Color.Transparent,
                  0.75f to Color.Transparent,
                  1.0f to Color(0x360E061B),
                )
              )
        )
      }

      Spacer(modifier = Modifier.width(12.dp))

      // Right Content Column: Category Badge + Shayari Text + Smart Copy/Share/Favorite Bar
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.Top,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Surface(
            shape = RoundedCornerShape(50),
            color = palette.accentPurple.copy(alpha = if (palette.isDark) 0.28f else 0.12f),
            border = BorderStroke(0.8.dp, palette.accentViolet.copy(alpha = 0.45f)),
          ) {
            Text(
              text = strings.categoryLabel(item.category),
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.SemiBold,
              color = if (palette.isDark) Color(0xFFEBD8FF) else palette.accentPurple,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
            )
          }
          if (item.isAdult) {
            Surface(
              shape = RoundedCornerShape(50),
              color = NeonPink.copy(alpha = if (palette.isDark) 0.24f else 0.14f),
              border = BorderStroke(0.8.dp, NeonPink.copy(alpha = 0.65f)),
            ) {
              Text(
                text = strings.adultBadge18Plus,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (palette.isDark) Color(0xFFFFD0E3) else Color(0xFFA00049),
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(7.dp))

        Text(
          text = item.text,
          style =
            MaterialTheme.typography.bodyLarge.copy(
              fontSize = textSize.fontSizeSp.sp,
              lineHeight = textSize.lineHeightSp.sp,
              fontWeight = FontWeight.Medium,
            ),
          color = palette.textPrimary,
          textAlign = TextAlign.Start,
          modifier = Modifier.fillMaxWidth().padding(end = 2.dp),
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Smart Copy Button + Share + Heart Action Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          SmartCopyPillButton(
            justCopied = justCopied,
            testTag = "copy_button_${item.id}",
            onClick = {
              justCopied = true
              onCopyClick(item)
            },
          )
          Spacer(modifier = Modifier.width(2.dp))
          if (whatsappShareEnabled) {
            CardIconAction(
              iconRes = R.drawable.ic_whatsapp,
              contentDescription = strings.actionWhatsApp,
              tint = if (palette.isDark) Color(0xFF4ADE80) else Color(0xFF15803D),
              iconScale = 1f,
              testTag = "whatsapp_button_${item.id}",
              onClick = { onWhatsAppClick(item) },
            )
          }
          CardIconAction(
            iconRes = R.drawable.ic_share,
            contentDescription = strings.actionShare,
            tint = palette.textSecondary,
            iconScale = 1f,
            testTag = "share_button_${item.id}",
            onClick = { onShareClick(item) },
          )
          CardIconAction(
            iconRes = if (isFavorite) R.drawable.ic_heart_filled else R.drawable.ic_heart,
            contentDescription = strings.actionFavorite,
            tint = heartColor,
            iconScale = heartScale,
            testTag = "favorite_button_${item.id}",
            onClick = { onFavoriteToggle(item.id) },
          )
        }
      }
    }
  }
}

@Composable
private fun SmartCopyPillButton(
  justCopied: Boolean,
  testTag: String,
  onClick: () -> Unit,
) {
  val palette = LocalShayariColors.current
  val interactionSource = remember { MutableInteractionSource() }
  val containerColor by
    animateColorAsState(
      targetValue =
        if (justCopied) palette.accentGold.copy(alpha = if (palette.isDark) 0.22f else 0.16f)
        else palette.pillSurface,
      animationSpec = tween(durationMillis = 200),
      label = "copyBg",
    )
  val contentColor by
    animateColorAsState(
      targetValue = if (justCopied) palette.accentGold else palette.textPrimary,
      animationSpec = tween(durationMillis = 200),
      label = "copyTint",
    )
  val scale by
    animateFloatAsState(
      targetValue = if (justCopied) 1.05f else 1f,
      animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
      label = "copyScale",
    )

  Box(
    contentAlignment = Alignment.Center,
    modifier =
      Modifier.minimumInteractiveComponentSize()
        .graphicsLayer {
          scaleX = scale
          scaleY = scale
        }
        .clickable(
          interactionSource = interactionSource,
          indication = null,
          onClick = onClick,
        )
        .testTag(testTag),
  ) {
    Surface(
      shape = RoundedCornerShape(50),
      color = containerColor,
      border =
        BorderStroke(
          width = 0.9.dp,
          color =
            if (justCopied) palette.accentGold.copy(alpha = 0.7f)
            else palette.outline,
        ),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
      ) {
        val strings = LocalAppStrings.current
        Icon(
          painter = painterResource(id = if (justCopied) R.drawable.ic_check else R.drawable.ic_copy),
          contentDescription = strings.actionCopy,
          tint = contentColor,
          modifier = Modifier.size(14.dp),
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
          text = if (justCopied) strings.actionCopied else strings.actionCopy,
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.SemiBold,
          color = contentColor,
        )
      }
    }
  }
}

@Composable
private fun CardIconAction(
  iconRes: Int,
  contentDescription: String,
  tint: Color,
  iconScale: Float,
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
      modifier =
        Modifier.size(21.dp).graphicsLayer {
          scaleX = iconScale
          scaleY = iconScale
        },
    )
  }
}
