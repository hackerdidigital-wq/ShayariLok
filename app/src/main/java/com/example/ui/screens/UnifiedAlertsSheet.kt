package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.model.ShayariNotification
import com.example.model.SocialNotification
import com.example.ui.SocialUiState
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.BadgeBlue
import com.example.ui.theme.BadgePink
import com.example.ui.theme.BadgePurple
import com.example.ui.theme.CrownGold
import com.example.ui.theme.LocalShayariColors
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonVioletGlow

private enum class AlertFilter {
  ALL,
  MESSAGES,
  FOLLOWS,
  LIKES,
  OFFICIAL,
}

@Composable
fun UnifiedAlertsSheet(
  adminNotifications: List<ShayariNotification>,
  socialState: SocialUiState,
  onSocialNotificationClick: (SocialNotification) -> Unit,
  onMarkSocialRead: () -> Unit,
  onDismiss: () -> Unit,
) {
  val strings = LocalAppStrings.current
  val palette = LocalShayariColors.current
  var isVisible by remember { mutableStateOf(false) }
  var selectedFilter by remember { mutableStateOf(AlertFilter.ALL) }

  LaunchedEffect(Unit) {
    isVisible = true
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties =
      DialogProperties(
        usePlatformDefaultWidth = false,
        decorFitsSystemWindows = false,
      ),
  ) {
    BackHandler { onDismiss() }

    Box(
      modifier =
        Modifier.fillMaxSize()
          .background(
            Brush.verticalGradient(
              colors = listOf(palette.bgTop, palette.bgMid, palette.bgBottom)
            )
          )
    ) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        if (size.minDimension <= 1f) return@Canvas
        drawCircle(
          brush =
            Brush.radialGradient(
              colors = listOf(NeonPink.copy(alpha = if (palette.isDark) 0.16f else 0.09f), Color.Transparent),
              center = Offset(size.width * 0.86f, size.height * 0.14f),
              radius = size.minDimension * 0.55f,
            ),
          center = Offset(size.width * 0.86f, size.height * 0.14f),
          radius = size.minDimension * 0.55f,
        )
        drawCircle(
          brush =
            Brush.radialGradient(
              colors = listOf(NeonPurple.copy(alpha = if (palette.isDark) 0.24f else 0.10f), Color.Transparent),
              center = Offset(size.width * 0.14f, size.height * 0.82f),
              radius = size.minDimension * 0.65f,
            ),
          center = Offset(size.width * 0.14f, size.height * 0.82f),
          radius = size.minDimension * 0.65f,
        )
      }

      AnimatedVisibility(
        visible = isVisible,
        enter =
          slideInVertically(
            initialOffsetY = { it / 6 },
            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
          ) + fadeIn(animationSpec = tween(240)),
        exit =
          slideOutVertically(
            targetOffsetY = { it / 5 },
            animationSpec = tween(180),
          ) + fadeOut(animationSpec = tween(180)),
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier =
            Modifier.fillMaxSize()
              .statusBarsPadding()
              .navigationBarsPadding(),
        ) {
          // Modern Full-Screen Header Bar
          Row(
            modifier =
              Modifier.widthIn(max = 720.dp)
                .fillMaxWidth()
                .padding(start = 16.dp, end = 12.dp, top = 14.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.weight(1f),
            ) {
              Surface(
                shape = CircleShape,
                color = palette.accentPurple.copy(alpha = 0.20f),
                border = BorderStroke(1.2.dp, palette.accentGold.copy(alpha = 0.65f)),
                modifier = Modifier.size(44.dp),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    painter = painterResource(id = R.drawable.ic_notification),
                    contentDescription = null,
                    tint = palette.accentGold,
                    modifier = Modifier.size(22.dp),
                  )
                }
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = strings.liveAlertsTitle,
                  style = MaterialTheme.typography.titleLarge,
                  fontWeight = FontWeight.ExtraBold,
                  color = palette.textPrimary,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                val totalAlertsCount = socialState.socialNotifications.size + adminNotifications.size
                Text(
                  text =
                    if (socialState.unreadNotificationsCount > 0) {
                      "${socialState.unreadNotificationsCount} unread • $totalAlertsCount total"
                    } else {
                      "$totalAlertsCount ${strings.notificationsTitle}"
                    },
                  style = MaterialTheme.typography.labelMedium,
                  color = palette.textSecondary,
                )
              }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
              if (socialState.unreadNotificationsCount > 0) {
                TextButton(
                  onClick = onMarkSocialRead,
                  modifier =
                    Modifier.minimumInteractiveComponentSize()
                      .testTag("mark_all_alerts_read"),
                ) {
                  Text(
                    text = strings.markRead,
                    color = palette.accentGold,
                    fontWeight = FontWeight.Bold,
                  )
                }
              }
              Surface(
                shape = CircleShape,
                color = palette.pillSurface,
                border = BorderStroke(1.dp, palette.outline),
                modifier =
                  Modifier.minimumInteractiveComponentSize()
                    .size(42.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onDismiss)
                    .testTag("close_alerts_dialog"),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    painter = painterResource(id = R.drawable.ic_close),
                    contentDescription = stringResource(R.string.cd_back),
                    tint = palette.textPrimary,
                    modifier = Modifier.size(20.dp),
                  )
                }
              }
            }
          }

          // Interactive Filter Chips Row
          LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth(),
          ) {
            item {
              AlertFilterChip(
                label = strings.categoryLabel("All"),
                count = socialState.socialNotifications.size + adminNotifications.size,
                accentColor = palette.accentGold,
                selected = selectedFilter == AlertFilter.ALL,
                onClick = { selectedFilter = AlertFilter.ALL },
              )
            }
            item {
              AlertFilterChip(
                label = strings.msgShort,
                count = socialState.unreadMessageAlertsCount,
                accentColor = BadgePink,
                selected = selectedFilter == AlertFilter.MESSAGES,
                onClick = { selectedFilter = AlertFilter.MESSAGES },
              )
            }
            item {
              AlertFilterChip(
                label = strings.followShort,
                count = socialState.unreadFollowAlertsCount,
                accentColor = BadgePurple,
                selected = selectedFilter == AlertFilter.FOLLOWS,
                onClick = { selectedFilter = AlertFilter.FOLLOWS },
              )
            }
            item {
              AlertFilterChip(
                label = strings.likesShort,
                count = socialState.unreadLikeAlertsCount,
                accentColor = BadgeBlue,
                selected = selectedFilter == AlertFilter.LIKES,
                onClick = { selectedFilter = AlertFilter.LIKES },
              )
            }
            item {
              AlertFilterChip(
                label = strings.officialAnnouncements,
                count = adminNotifications.size,
                accentColor = NeonVioletGlow,
                selected = selectedFilter == AlertFilter.OFFICIAL,
                onClick = { selectedFilter = AlertFilter.OFFICIAL },
              )
            }
          }

          Spacer(modifier = Modifier.height(6.dp))
          HorizontalDivider(
            color = palette.outline.copy(alpha = 0.45f),
            modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth(),
          )

          val filteredSocial =
            when (selectedFilter) {
              AlertFilter.ALL -> socialState.socialNotifications
              AlertFilter.MESSAGES -> socialState.socialNotifications.filter { it.type == "message" }
              AlertFilter.FOLLOWS -> socialState.socialNotifications.filter { it.type == "follow" }
              AlertFilter.LIKES -> socialState.socialNotifications.filter { it.type == "like" }
              AlertFilter.OFFICIAL -> emptyList()
            }
          val filteredAdmin =
            if (selectedFilter == AlertFilter.ALL || selectedFilter == AlertFilter.OFFICIAL) {
              adminNotifications
            } else {
              emptyList()
            }

          if (filteredSocial.isEmpty() && filteredAdmin.isEmpty()) {
            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier.weight(1f).fillMaxWidth().padding(28.dp),
            ) {
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.widthIn(max = 320.dp),
              ) {
                Surface(
                  shape = CircleShape,
                  color = palette.accentPurple.copy(alpha = 0.20f),
                  border = BorderStroke(1.2.dp, palette.accentGold.copy(alpha = 0.45f)),
                  modifier = Modifier.size(76.dp),
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(
                      painter = painterResource(id = R.drawable.ic_notification),
                      contentDescription = null,
                      tint = palette.accentGold,
                      modifier = Modifier.size(34.dp),
                    )
                  }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                  text = strings.emptyAlertsTitle,
                  style = MaterialTheme.typography.titleLarge,
                  fontWeight = FontWeight.Bold,
                  color = palette.textPrimary,
                  textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                  text = strings.allCaughtUpAlerts,
                  style = MaterialTheme.typography.bodyMedium,
                  color = palette.textSecondary,
                  textAlign = TextAlign.Center,
                )
              }
            }
          } else {
            LazyColumn(
              contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp),
              modifier = Modifier.weight(1f).widthIn(max = 720.dp).fillMaxWidth(),
            ) {
              if (filteredSocial.isNotEmpty()) {
                item(key = "header_social") {
                  Text(
                    text = strings.friendsFollowersMessages,
                    style = MaterialTheme.typography.labelLarge,
                    color = palette.accentGold,
                    fontWeight = FontWeight.Bold,
                  )
                }
                items(filteredSocial, key = { "social_${it.id}" }) { notif ->
                  SocialAlertItemCard(
                    notif = notif,
                    onClick = { onSocialNotificationClick(notif) },
                  )
                }
              }

              if (filteredAdmin.isNotEmpty()) {
                item(key = "header_admin") {
                  Text(
                    text = strings.officialAnnouncements,
                    style = MaterialTheme.typography.labelLarge,
                    color = palette.accentGold,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = if (filteredSocial.isNotEmpty()) 8.dp else 0.dp),
                  )
                }
                items(filteredAdmin, key = { "admin_${it.id}" }) { item ->
                  AdminAnnouncementCard(item = item)
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun AlertFilterChip(
  label: String,
  count: Int,
  accentColor: Color,
  selected: Boolean,
  onClick: () -> Unit,
) {
  val palette = LocalShayariColors.current
  Surface(
    shape = RoundedCornerShape(50),
    color = if (selected) accentColor.copy(alpha = if (palette.isDark) 0.28f else 0.16f) else palette.surfaceVariant,
    border =
      BorderStroke(
        width = if (selected) 1.3.dp else 0.9.dp,
        color = if (selected) accentColor else palette.outline,
      ),
    modifier =
      Modifier.clip(RoundedCornerShape(50))
        .clickable(onClick = onClick)
        .minimumInteractiveComponentSize(),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        color = palette.textPrimary,
        maxLines = 1,
      )
      if (count > 0) {
        Surface(
          shape = CircleShape,
          color = accentColor.copy(alpha = 0.88f),
        ) {
          Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            color = if (accentColor == CrownGold) Color(0xFF1D0D36) else Color.White,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun SocialAlertItemCard(
  notif: SocialNotification,
  onClick: () -> Unit,
) {
  val palette = LocalShayariColors.current
  val iconRes =
    when (notif.type) {
      "message" -> R.drawable.ic_chat
      "like" -> R.drawable.ic_heart_filled
      else -> R.drawable.ic_person
    }
  val accent =
    when (notif.type) {
      "message" -> BadgePink
      "like" -> NeonPink
      else -> BadgePurple
    }

  Surface(
    shape = RoundedCornerShape(18.dp),
    color = if (!notif.isRead) palette.surfaceVariant else palette.surface.copy(alpha = 0.92f),
    border =
      BorderStroke(
        1.dp,
        if (!notif.isRead) palette.accentGold.copy(alpha = 0.6f) else palette.outline.copy(alpha = 0.5f),
      ),
    tonalElevation = if (!notif.isRead) 4.dp else 1.dp,
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .clickable(onClick = onClick)
        .minimumInteractiveComponentSize()
        .testTag("social_notification_card_${notif.id}"),
  ) {
    Row(
      modifier = Modifier.padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(44.dp).background(accent, CircleShape),
      ) {
        Icon(
          painter = painterResource(id = iconRes),
          contentDescription = null,
          tint = Color.White,
          modifier = Modifier.size(20.dp),
        )
      }
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = notif.senderName.ifBlank { "Shayari Lok" },
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = palette.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          if (notif.senderHandle.isNotBlank()) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = notif.senderHandle,
              style = MaterialTheme.typography.labelSmall,
              color = palette.accentGold,
            )
          }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = notif.previewText,
          style = MaterialTheme.typography.bodySmall,
          color = palette.textSecondary,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }
      if (!notif.isRead) {
        Spacer(modifier = Modifier.width(8.dp))
        Box(modifier = Modifier.size(9.dp).background(NeonPink, CircleShape))
      }
    }
  }
}

@Composable
private fun AdminAnnouncementCard(item: ShayariNotification) {
  val palette = LocalShayariColors.current
  Surface(
    shape = RoundedCornerShape(18.dp),
    color = palette.surface.copy(alpha = 0.94f),
    border = BorderStroke(1.dp, palette.accentGold.copy(alpha = 0.45f)),
    modifier = Modifier.fillMaxWidth().testTag("admin_notification_card_${item.id}"),
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Surface(
          shape = RoundedCornerShape(50),
          color = palette.accentGold.copy(alpha = 0.16f),
        ) {
          Text(
            text = item.badge,
            style = MaterialTheme.typography.labelSmall,
            color = palette.accentGold,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
          )
        }
        Text(
          text = item.timeLabel,
          style = MaterialTheme.typography.labelSmall,
          color = palette.textSecondary,
        )
      }
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = item.title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = palette.textPrimary,
      )
      if (item.message.isNotBlank()) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = item.message,
          style = MaterialTheme.typography.bodySmall,
          color = palette.textSecondary,
        )
      }
    }
  }
}
