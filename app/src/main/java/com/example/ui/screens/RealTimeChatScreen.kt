package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.ChatMessage
import com.example.model.PublicProfile
import com.example.ui.components.UserAvatar
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.CrownGold
import com.example.ui.theme.LocalShayariColors
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val QUICK_SHAYARI_EMOJIS =
  listOf("❤️", "🌹", "✨", "🔥", "👏", "💔", "😍", "🙏", "💫", "😊")

@Composable
fun RealTimeChatScreen(
  currentUserId: String,
  partner: PublicProfile,
  messages: List<ChatMessage>,
  isFollowingPartner: Boolean,
  isSendingMessage: Boolean = false,
  isBlocked: Boolean = false,
  blockReason: String = "",
  onToggleFollowPartner: () -> Unit,
  onSendMessage: (text: String, onSent: () -> Unit) -> Unit,
  onMarkMessagesRead: (List<ChatMessage>) -> Unit,
  onBack: () -> Unit,
) {
  BackHandler { onBack() }

  val strings = LocalAppStrings.current
  val palette = LocalShayariColors.current
  var draft by remember { mutableStateOf("") }
  val listState = rememberLazyListState()

  LaunchedEffect(messages.size, partner.effectiveUid) {
    if (messages.isNotEmpty()) {
      listState.animateScrollToItem(messages.lastIndex)
      onMarkMessagesRead(messages)
    }
  }

  Scaffold(
    containerColor = Color.Transparent,
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    modifier =
      Modifier.fillMaxSize()
        .background(
          Brush.verticalGradient(
            colors = listOf(palette.bgTop, palette.bgMid, palette.bgBottom)
          )
        ),
    topBar = {
      Surface(
        shadowElevation = 6.dp,
        color = Color.Transparent,
        modifier =
          Modifier.fillMaxWidth()
            .background(
              Brush.verticalGradient(colors = listOf(palette.headerStart, palette.headerEnd))
            ),
      ) {
        Column(modifier = Modifier.fillMaxWidth().statusBarsPadding()) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            IconButton(
              onClick = onBack,
              modifier = Modifier.minimumInteractiveComponentSize().testTag("chat_back_button"),
            ) {
              Icon(
                painter = painterResource(id = R.drawable.ic_arrow_back),
                contentDescription = strings.closeAction,
                tint = Color.White,
              )
            }
            UserAvatar(
              photoUrl = partner.photoUrl,
              displayName = partner.displayName,
              size = 42.dp,
              showOnlineDot = partner.isOnline,
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = partner.displayName.ifBlank { "Shayar Friend" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text =
                  if (partner.city.isNotBlank()) {
                    "${partner.effectiveHandle} • ${partner.city}"
                  } else {
                    partner.effectiveHandle
                  },
                style = MaterialTheme.typography.labelSmall,
                color = CrownGold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
            Button(
              onClick = onToggleFollowPartner,
              shape = RoundedCornerShape(12.dp),
              colors =
                ButtonDefaults.buttonColors(
                  containerColor =
                    if (isFollowingPartner) Color.White.copy(alpha = 0.22f) else palette.accentPink
                ),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
              modifier = Modifier.minimumInteractiveComponentSize().testTag("chat_follow_button"),
            ) {
              Text(
                text = if (isFollowingPartner) strings.followingAction else strings.followAction,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
              )
            }
          }

          Surface(
            color = Color.Black.copy(alpha = 0.24f),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center,
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            ) {
              Icon(
                painter = painterResource(id = R.drawable.ic_shield),
                contentDescription = null,
                tint = Color(0xFF4ADE80),
                modifier = Modifier.size(13.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = strings.chatPrivateBadge,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFD9CCEE),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }
      }
    },
    bottomBar = {
      Surface(
        color = palette.surface.copy(alpha = 0.96f),
        border = BorderStroke(0.8.dp, palette.outline),
        modifier = Modifier.fillMaxWidth().imePadding().navigationBarsPadding(),
      ) {
        Column {
          if (isBlocked) {
            Surface(
              color = Color(0xFF4A1025),
              modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
              shape = RoundedCornerShape(12.dp),
            ) {
              Text(
                text = blockReason.ifBlank { strings.messagingDisabled },
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFFFFD4E0),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
              )
            }
          }

          LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            items(QUICK_SHAYARI_EMOJIS) { emoji ->
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = palette.surfaceVariant,
                modifier =
                  Modifier.clip(RoundedCornerShape(12.dp))
                    .clickable(enabled = !isBlocked) { draft += emoji }
                    .minimumInteractiveComponentSize(),
              ) {
                Box(
                  contentAlignment = Alignment.Center,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                  Text(text = emoji, fontSize = 18.sp)
                }
              }
            }
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 8.dp, bottom = 10.dp),
          ) {
            OutlinedTextField(
              value = draft,
              onValueChange = { if (it.length <= 1000) draft = it },
              enabled = !isBlocked,
              maxLines = 3,
              placeholder = {
                Text(
                  text = if (isBlocked) strings.messagingDisabled else strings.writeMessageHint,
                  color = palette.textMuted,
                )
              },
              shape = RoundedCornerShape(20.dp),
              colors =
                OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = palette.accentPurple,
                  unfocusedBorderColor = palette.outline,
                  focusedTextColor = palette.textPrimary,
                  unfocusedTextColor = palette.textPrimary,
                  focusedContainerColor = palette.surfaceVariant,
                  unfocusedContainerColor = palette.surfaceVariant,
                ),
              modifier = Modifier.weight(1f).testTag("chat_message_input"),
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
              onClick = {
                val toSend = draft
                onSendMessage(toSend) { draft = "" }
              },
              enabled = !isBlocked && !isSendingMessage && draft.isNotBlank(),
              modifier =
                Modifier.size(48.dp)
                  .background(
                    if (!isBlocked && !isSendingMessage && draft.isNotBlank()) {
                      Brush.linearGradient(colors = listOf(NeonPurple, NeonPink))
                    } else {
                      Brush.linearGradient(colors = listOf(palette.surfaceVariant, palette.surfaceVariant))
                    },
                    CircleShape,
                  )
                  .minimumInteractiveComponentSize()
                  .testTag("chat_send_button"),
            ) {
              if (isSendingMessage) {
                CircularProgressIndicator(
                  color = palette.accentGold,
                  strokeWidth = 2.dp,
                  modifier = Modifier.size(18.dp),
                )
              } else {
                Icon(
                  painter = painterResource(id = R.drawable.ic_send),
                  contentDescription = strings.chatAction,
                  tint = if (!isBlocked && draft.isNotBlank()) Color.White else palette.textMuted,
                  modifier = Modifier.size(20.dp),
                )
              }
            }
          }
        }
      }
    },
  ) { innerPadding ->
    Box(
      contentAlignment = Alignment.TopCenter,
      modifier = Modifier.fillMaxSize().padding(innerPadding),
    ) {
      if (messages.isEmpty()) {
        Column(
          modifier = Modifier.fillMaxSize().padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center,
        ) {
          Icon(
            painter = painterResource(id = R.drawable.ic_chat),
            contentDescription = null,
            tint = palette.accentGold,
            modifier = Modifier.size(40.dp),
          )
          Spacer(modifier = Modifier.height(10.dp))
          Text(
            text = "${strings.sayHelloPrefix} ${partner.displayName}!",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = palette.textPrimary,
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = strings.chatEmptySubtitle,
            style = MaterialTheme.typography.bodySmall,
            color = palette.textSecondary,
          )
        }
      } else {
        LazyColumn(
          state = listState,
          contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.widthIn(max = 720.dp).fillMaxSize(),
        ) {
          items(messages, key = { it.id }) { msg ->
            ChatMessageBubble(
              message = msg,
              isMine = msg.senderId == currentUserId,
              nowLabel = strings.nowLabel,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ChatMessageBubble(
  message: ChatMessage,
  isMine: Boolean,
  nowLabel: String,
) {
  val palette = LocalShayariColors.current
  val timeText =
    remember(message.createdAtMillis, nowLabel) {
      if (message.createdAtMillis > 0L) {
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(message.createdAtMillis))
      } else {
        nowLabel
      }
    }

  val textColor = if (isMine) Color.White else palette.textPrimary
  val timeColor = if (isMine) Color.White.copy(alpha = 0.78f) else palette.textMuted

  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
  ) {
    Surface(
      shape =
        RoundedCornerShape(
          topStart = 18.dp,
          topEnd = 18.dp,
          bottomStart = if (isMine) 18.dp else 4.dp,
          bottomEnd = if (isMine) 4.dp else 18.dp,
        ),
      color = if (isMine) palette.accentPurple else palette.surface,
      border =
        BorderStroke(
          0.8.dp,
          if (isMine) palette.accentPink.copy(alpha = 0.55f) else palette.outline,
        ),
      modifier = Modifier.widthIn(max = 300.dp),
    ) {
      Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)) {
        Text(
          text = message.text,
          style = MaterialTheme.typography.bodyMedium,
          color = textColor,
        )
        Spacer(modifier = Modifier.height(3.dp))
        Row(
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = timeText,
            style = MaterialTheme.typography.labelSmall,
            color = timeColor,
            fontSize = 10.sp,
          )
          if (isMine) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = if (message.isRead) "✓✓" else "✓",
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              color = if (message.isRead) CrownGold else Color.White.copy(alpha = 0.72f),
            )
          }
        }
      }
    }
  }
}
