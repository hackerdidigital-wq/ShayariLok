package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.ShayariNotification
import com.example.model.SocialNotification
import com.example.ui.SocialUiState
import com.example.ui.theme.BadgeBlue
import com.example.ui.theme.BadgePink
import com.example.ui.theme.BadgePurple
import com.example.ui.theme.CrownGold
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonVioletGlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedAlertsSheet(
  adminNotifications: List<ShayariNotification>,
  socialState: SocialUiState,
  onSocialNotificationClick: (SocialNotification) -> Unit,
  onMarkSocialRead: () -> Unit,
  onDismiss: () -> Unit,
) {
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    containerColor = DarkSurface,
    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "Live Alerts & Updates",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.ExtraBold,
          color = Color.White,
        )
        if (socialState.unreadNotificationsCount > 0) {
          TextButton(
            onClick = onMarkSocialRead,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("mark_all_alerts_read"),
          ) {
            Text("Mark Read", color = CrownGold, fontWeight = FontWeight.SemiBold)
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))
      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        AlertSummaryPill(
          label = "${socialState.unreadMessageAlertsCount} Msg",
          color = BadgePink,
          modifier = Modifier.weight(1f),
        )
        AlertSummaryPill(
          label = "${socialState.unreadFollowAlertsCount} Follow",
          color = BadgePurple,
          modifier = Modifier.weight(1f),
        )
        AlertSummaryPill(
          label = "${socialState.unreadLikeAlertsCount} Likes",
          color = BadgeBlue,
          modifier = Modifier.weight(1f),
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      LazyColumn(
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        if (socialState.socialNotifications.isNotEmpty()) {
          item {
            Text(
              text = "Friends, Followers & Messages",
              style = MaterialTheme.typography.labelLarge,
              color = CrownGold,
              fontWeight = FontWeight.Bold,
            )
          }
          items(socialState.socialNotifications, key = { "social_${it.id}" }) { notif ->
            SocialAlertItemCard(
              notif = notif,
              onClick = { onSocialNotificationClick(notif) },
            )
          }
        }

        if (adminNotifications.isNotEmpty()) {
          item {
            Text(
              text = "Official Shayari Announcements",
              style = MaterialTheme.typography.labelLarge,
              color = CrownGold,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(top = 6.dp),
            )
          }
          items(adminNotifications, key = { "admin_${it.id}" }) { item ->
            AdminAnnouncementCard(item = item)
          }
        }

        if (socialState.socialNotifications.isEmpty() && adminNotifications.isEmpty()) {
          item {
            Text(
              text = "All caught up! New chats, followers, likes, and Shayari updates appear here.",
              style = MaterialTheme.typography.bodyMedium,
              color = Color(0xFFD2C4EB),
              modifier = Modifier.padding(vertical = 24.dp),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun AlertSummaryPill(
  label: String,
  color: Color,
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = color.copy(alpha = 0.25f),
    border = BorderStroke(1.dp, color.copy(alpha = 0.6f)),
    modifier = modifier,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium,
      fontWeight = FontWeight.Bold,
      color = Color.White,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
    )
  }
}

@Composable
private fun SocialAlertItemCard(
  notif: SocialNotification,
  onClick: () -> Unit,
) {
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
    shape = RoundedCornerShape(16.dp),
    color = if (!notif.isRead) DarkSurfaceVariant else DarkSurface.copy(alpha = 0.72f),
    border =
      BorderStroke(
        1.dp,
        if (!notif.isRead) CrownGold.copy(alpha = 0.55f) else NeonVioletGlow.copy(alpha = 0.25f),
      ),
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .clickable(onClick = onClick)
        .minimumInteractiveComponentSize()
        .testTag("social_notification_card_${notif.id}"),
  ) {
    Row(
      modifier = Modifier.padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(40.dp).background(accent, CircleShape),
      ) {
        Icon(
          painter = painterResource(id = iconRes),
          contentDescription = null,
          tint = Color.White,
          modifier = Modifier.size(18.dp),
        )
      }
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = notif.senderName.ifBlank { "Shayar Friend" },
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = notif.senderHandle,
            style = MaterialTheme.typography.labelSmall,
            color = CrownGold,
          )
        }
        Text(
          text = notif.previewText,
          style = MaterialTheme.typography.bodySmall,
          color = Color(0xFFD2C4EB),
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }
      if (!notif.isRead) {
        Box(modifier = Modifier.size(8.dp).background(NeonPink, CircleShape))
      }
    }
  }
}

@Composable
private fun AdminAnnouncementCard(item: ShayariNotification) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = DarkSurfaceVariant.copy(alpha = 0.6f),
    border = BorderStroke(1.dp, NeonVioletGlow.copy(alpha = 0.35f)),
    modifier = Modifier.fillMaxWidth().testTag("admin_notification_card_${item.id}"),
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = item.badge,
          style = MaterialTheme.typography.labelSmall,
          color = CrownGold,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = item.timeLabel,
          style = MaterialTheme.typography.labelSmall,
          color = Color(0xFFD2C4EB),
        )
      }
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = item.title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = Color.White,
      )
      if (item.message.isNotBlank()) {
        Spacer(modifier = Modifier.height(3.dp))
        Text(
          text = item.message,
          style = MaterialTheme.typography.bodySmall,
          color = Color(0xFFD2C4EB),
        )
      }
    }
  }
}
