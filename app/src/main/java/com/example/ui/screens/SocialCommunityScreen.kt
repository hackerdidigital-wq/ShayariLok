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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.ChatRoom
import com.example.model.PublicProfile
import com.example.ui.CommunityFilter
import com.example.ui.SocialUiState
import com.example.ui.theme.BadgeBlue
import com.example.ui.theme.BadgePink
import com.example.ui.theme.BadgePurple
import com.example.ui.theme.CrownGold
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonVioletGlow

@Composable
fun SocialCommunityScreen(
  uiState: SocialUiState,
  onSearchQueryChange: (String) -> Unit,
  onSelectFilter: (CommunityFilter) -> Unit,
  onInspectProfile: (PublicProfile) -> Unit,
  onDismissProfileSheet: () -> Unit,
  onToggleFollow: (PublicProfile) -> Unit,
  onToggleLike: (PublicProfile) -> Unit,
  onOpenChatWith: (PublicProfile) -> Unit,
  onOpenChatRoom: (ChatRoom) -> Unit,
  onAddFriend: (name: String, handle: String, city: String, bio: String, onDone: () -> Unit) -> Unit,
  modifier: Modifier = Modifier,
) {
  var showAddFriendDialog by remember { mutableStateOf(false) }

  Column(
    modifier = modifier.fillMaxSize(),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Column(
      modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth().padding(horizontal = 14.dp)
    ) {
      Spacer(modifier = Modifier.height(10.dp))
      OutlinedTextField(
        value = uiState.searchQuery,
        onValueChange = onSearchQueryChange,
        singleLine = true,
        placeholder = {
          Text(
            text = "Search by Name, City, or @SL ID...",
            color = Color.White.copy(alpha = 0.65f),
          )
        },
        leadingIcon = {
          Icon(
            painter = painterResource(id = R.drawable.ic_search),
            contentDescription = null,
            tint = CrownGold,
            modifier = Modifier.size(20.dp),
          )
        },
        shape = RoundedCornerShape(16.dp),
        colors =
          OutlinedTextFieldDefaults.colors(
            focusedBorderColor = CrownGold,
            unfocusedBorderColor = NeonVioletGlow.copy(alpha = 0.45f),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedContainerColor = DarkSurface.copy(alpha = 0.85f),
            unfocusedContainerColor = DarkSurface.copy(alpha = 0.72f),
          ),
        modifier = Modifier.fillMaxWidth().testTag("community_search_input"),
      )

      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 10.dp),
      ) {
        item {
          CommunityFilterChip(
            label = "All Friends (${uiState.otherUsers.size})",
            selected = uiState.communityFilter == CommunityFilter.ALL,
            testTag = "filter_all_friends",
            onClick = { onSelectFilter(CommunityFilter.ALL) },
          )
        }
        item {
          CommunityFilterChip(
            label = "Following (${uiState.followingIds.size})",
            selected = uiState.communityFilter == CommunityFilter.FOLLOWING,
            testTag = "filter_following",
            onClick = { onSelectFilter(CommunityFilter.FOLLOWING) },
          )
        }
        item {
          CommunityFilterChip(
            label = "Chats (${uiState.chats.size})",
            selected = uiState.communityFilter == CommunityFilter.CHATS,
            testTag = "filter_chats",
            onClick = { onSelectFilter(CommunityFilter.CHATS) },
          )
        }
        item {
          CommunityFilterChip(
            label = "+ Add Friend",
            selected = false,
            testTag = "add_friend_chip",
            onClick = { showAddFriendDialog = true },
          )
        }
      }
    }

    if (uiState.communityFilter == CommunityFilter.CHATS) {
      val query = uiState.searchQuery.trim()
      val filteredRooms =
        if (query.isEmpty()) {
          uiState.chats
        } else {
          uiState.chats.filter { room ->
            room.partnerName.contains(query, ignoreCase = true) ||
              room.partnerHandle.contains(query, ignoreCase = true) ||
              room.lastMessage.contains(query, ignoreCase = true)
          }
        }
      if (filteredRooms.isEmpty()) {
        CommunityEmptyBox(
          title = stringResource(R.string.empty_chats_title),
          subtitle = stringResource(R.string.empty_chats_subtitle),
          iconRes = R.drawable.ic_chat,
        )
      } else {
        LazyColumn(
          contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.widthIn(max = 720.dp).fillMaxSize(),
        ) {
          items(filteredRooms, key = { it.id }) { room ->
            ChatThreadPreviewCard(
              room = room,
              onClick = { onOpenChatRoom(room) },
            )
          }
          item { Spacer(modifier = Modifier.height(12.dp)) }
        }
      }
    } else {
      val users = uiState.filteredCommunityUsers
      if (users.isEmpty()) {
        CommunityEmptyBox(
          title = stringResource(R.string.empty_community_title),
          subtitle = stringResource(R.string.empty_community_subtitle),
          iconRes = R.drawable.ic_community,
        )
      } else {
        LazyColumn(
          contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.widthIn(max = 720.dp).fillMaxSize(),
        ) {
          items(users, key = { it.userId }) { user ->
            FriendProfileCard(
              user = user,
              isFollowing = user.userId in uiState.followingIds,
              isLiked = user.userId in uiState.likedProfileIds,
              onCardClick = { onInspectProfile(user) },
              onFollowClick = { onToggleFollow(user) },
              onLikeClick = { onToggleLike(user) },
              onChatClick = { onOpenChatWith(user) },
            )
          }
          item { Spacer(modifier = Modifier.height(12.dp)) }
        }
      }
    }
  }

  val inspected = uiState.inspectedProfile
  if (inspected != null) {
    UserProfileDetailDialog(
      user = inspected,
      isFollowing = inspected.userId in uiState.followingIds,
      isLiked = inspected.userId in uiState.likedProfileIds,
      onDismiss = onDismissProfileSheet,
      onToggleFollow = { onToggleFollow(inspected) },
      onToggleLike = { onToggleLike(inspected) },
      onStartChat = { onOpenChatWith(inspected) },
    )
  }

  if (showAddFriendDialog) {
    AddFriendDialog(
      isBusy = uiState.isBusy,
      onDismiss = { showAddFriendDialog = false },
      onAdd = { name, handle, city, bio ->
        onAddFriend(name, handle, city, bio) { showAddFriendDialog = false }
      },
    )
  }
}

@Composable
private fun CommunityFilterChip(
  label: String,
  selected: Boolean,
  testTag: String,
  onClick: () -> Unit,
) {
  Surface(
    shape = RoundedCornerShape(50),
    color = if (selected) BadgePurple else DarkSurfaceVariant,
    border =
      BorderStroke(
        1.dp,
        if (selected) CrownGold.copy(alpha = 0.8f) else NeonVioletGlow.copy(alpha = 0.35f),
      ),
    modifier =
      Modifier.clip(RoundedCornerShape(50))
        .clickable(onClick = onClick)
        .minimumInteractiveComponentSize()
        .testTag(testTag),
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelLarge,
      fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
      color = Color.White,
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
    )
  }
}

@Composable
private fun FriendProfileCard(
  user: PublicProfile,
  isFollowing: Boolean,
  isLiked: Boolean,
  onCardClick: () -> Unit,
  onFollowClick: () -> Unit,
  onLikeClick: () -> Unit,
  onChatClick: () -> Unit,
) {
  Surface(
    shape = RoundedCornerShape(20.dp),
    color = DarkSurface.copy(alpha = 0.92f),
    border = BorderStroke(1.dp, NeonVioletGlow.copy(alpha = 0.32f)),
    tonalElevation = 2.dp,
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(20.dp))
        .clickable(onClick = onCardClick)
        .testTag("friend_card_${user.userId}"),
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Box(
          contentAlignment = Alignment.Center,
          modifier =
            Modifier.size(48.dp)
              .background(
                Brush.linearGradient(colors = listOf(BadgePurple, NeonPink)),
                CircleShape,
              ),
        ) {
          Text(
            text = user.displayName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "S",
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 18.sp,
          )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = user.displayName.ifBlank { "Shayari Lover" },
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = Color.White,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f, fill = false),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = DarkSurfaceVariant,
              border = BorderStroke(0.8.dp, CrownGold.copy(alpha = 0.55f)),
            ) {
              Text(
                text = user.handle,
                style = MaterialTheme.typography.labelSmall,
                color = CrownGold,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
              )
            }
          }

          if (user.city.isNotBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                painter = painterResource(id = R.drawable.ic_location),
                contentDescription = null,
                tint = Color(0xFFD2C4EB),
                modifier = Modifier.size(13.dp),
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = user.city,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFD2C4EB),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }
      }

      if (user.bio.isNotBlank()) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = user.bio,
          style = MaterialTheme.typography.bodySmall,
          color = Color.White.copy(alpha = 0.85f),
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text(
          text = "${user.followersCount} Followers • ${user.likesCount} Likes",
          style = MaterialTheme.typography.labelSmall,
          color = Color(0xFFD2C4EB),
          modifier = Modifier.weight(1f),
        )

        OutlinedButton(
          onClick = onLikeClick,
          shape = RoundedCornerShape(12.dp),
          border =
            BorderStroke(
              1.dp,
              if (isLiked) NeonPink else NeonVioletGlow.copy(alpha = 0.45f),
            ),
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
          modifier =
            Modifier.minimumInteractiveComponentSize().testTag("like_user_${user.userId}"),
        ) {
          Icon(
            painter =
              painterResource(
                id = if (isLiked) R.drawable.ic_heart_filled else R.drawable.ic_heart
              ),
            contentDescription = "Like profile",
            tint = if (isLiked) NeonPink else Color.White,
            modifier = Modifier.size(16.dp),
          )
        }

        Button(
          onClick = onFollowClick,
          shape = RoundedCornerShape(12.dp),
          colors =
            ButtonDefaults.buttonColors(
              containerColor = if (isFollowing) DarkSurfaceVariant else BadgePurple,
              contentColor = Color.White,
            ),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
          modifier =
            Modifier.minimumInteractiveComponentSize().testTag("follow_user_${user.userId}"),
        ) {
          Text(
            text = if (isFollowing) "Following" else "Follow",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
          )
        }

        Button(
          onClick = onChatClick,
          shape = RoundedCornerShape(12.dp),
          colors =
            ButtonDefaults.buttonColors(
              containerColor = BadgePink,
              contentColor = Color.White,
            ),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
          modifier =
            Modifier.minimumInteractiveComponentSize().testTag("chat_user_${user.userId}"),
        ) {
          Icon(
            painter = painterResource(id = R.drawable.ic_chat),
            contentDescription = "Chat",
            tint = Color.White,
            modifier = Modifier.size(15.dp),
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text("Chat", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
      }
    }
  }
}

@Composable
fun ChatThreadPreviewCard(
  room: ChatRoom,
  onClick: () -> Unit,
) {
  val partnerName = room.partnerName.ifBlank { "Shayar Friend" }
  val partnerHandle = room.partnerHandle.ifBlank { "@SL_USER" }

  Surface(
    shape = RoundedCornerShape(18.dp),
    color = DarkSurface.copy(alpha = 0.92f),
    border = BorderStroke(1.dp, NeonVioletGlow.copy(alpha = 0.35f)),
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .clickable(onClick = onClick)
        .minimumInteractiveComponentSize()
        .testTag("chat_thread_${room.id}"),
  ) {
    Row(
      modifier = Modifier.padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        contentAlignment = Alignment.Center,
        modifier =
          Modifier.size(46.dp)
            .background(
              Brush.linearGradient(colors = listOf(BadgeBlue, NeonPurple)),
              CircleShape,
            ),
      ) {
        Text(
          text = partnerName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "C",
          color = Color.White,
          fontWeight = FontWeight.Bold,
          fontSize = 17.sp,
        )
      }
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = partnerName,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = partnerHandle,
            style = MaterialTheme.typography.labelSmall,
            color = CrownGold,
          )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
          text = room.lastMessage.ifBlank { "Tap to open chat" },
          style = MaterialTheme.typography.bodySmall,
          color = Color(0xFFD2C4EB),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Icon(
        painter = painterResource(id = R.drawable.ic_chat),
        contentDescription = "Open Chat",
        tint = CrownGold,
        modifier = Modifier.size(20.dp),
      )
    }
  }
}

@Composable
private fun AddFriendDialog(
  isBusy: Boolean,
  onDismiss: () -> Unit,
  onAdd: (name: String, handle: String, city: String, bio: String) -> Unit,
) {
  var name by remember { mutableStateOf("") }
  var handle by remember { mutableStateOf("@SL_") }
  var city by remember { mutableStateOf("") }
  var bio by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = { if (!isBusy) onDismiss() },
    containerColor = DarkSurface,
    shape = RoundedCornerShape(24.dp),
    title = {
      Text(text = "Add Friend Profile", color = Color.White, fontWeight = FontWeight.Bold)
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = name,
          onValueChange = { if (it.length <= 60) name = it },
          singleLine = true,
          label = { Text("Friend Name") },
          colors =
            OutlinedTextFieldDefaults.colors(
              focusedBorderColor = CrownGold,
              unfocusedBorderColor = NeonVioletGlow.copy(alpha = 0.5f),
              focusedTextColor = Color.White,
              unfocusedTextColor = Color.White,
              focusedLabelColor = CrownGold,
              unfocusedLabelColor = Color(0xFFD2C4EB),
            ),
          modifier = Modifier.fillMaxWidth().testTag("add_friend_name_input"),
        )
        OutlinedTextField(
          value = handle,
          onValueChange = { if (it.length <= 16) handle = it },
          singleLine = true,
          label = { Text("Unique ID (e.g. @SL_7788)") },
          colors =
            OutlinedTextFieldDefaults.colors(
              focusedBorderColor = CrownGold,
              unfocusedBorderColor = NeonVioletGlow.copy(alpha = 0.5f),
              focusedTextColor = Color.White,
              unfocusedTextColor = Color.White,
              focusedLabelColor = CrownGold,
              unfocusedLabelColor = Color(0xFFD2C4EB),
            ),
          modifier = Modifier.fillMaxWidth().testTag("add_friend_handle_input"),
        )
        OutlinedTextField(
          value = city,
          onValueChange = { if (it.length <= 80) city = it },
          singleLine = true,
          label = { Text("City") },
          colors =
            OutlinedTextFieldDefaults.colors(
              focusedBorderColor = CrownGold,
              unfocusedBorderColor = NeonVioletGlow.copy(alpha = 0.5f),
              focusedTextColor = Color.White,
              unfocusedTextColor = Color.White,
              focusedLabelColor = CrownGold,
              unfocusedLabelColor = Color(0xFFD2C4EB),
            ),
          modifier = Modifier.fillMaxWidth().testTag("add_friend_city_input"),
        )
        OutlinedTextField(
          value = bio,
          onValueChange = { if (it.length <= 200) bio = it },
          maxLines = 3,
          label = { Text("Bio / Shayari") },
          colors =
            OutlinedTextFieldDefaults.colors(
              focusedBorderColor = CrownGold,
              unfocusedBorderColor = NeonVioletGlow.copy(alpha = 0.5f),
              focusedTextColor = Color.White,
              unfocusedTextColor = Color.White,
              focusedLabelColor = CrownGold,
              unfocusedLabelColor = Color(0xFFD2C4EB),
            ),
          modifier = Modifier.fillMaxWidth().testTag("add_friend_bio_input"),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onAdd(name, handle, city, bio) },
        enabled = !isBusy && name.isNotBlank(),
        colors = ButtonDefaults.buttonColors(containerColor = BadgePink),
        modifier = Modifier.minimumInteractiveComponentSize().testTag("save_friend_button"),
      ) {
        Text("Add Friend", color = Color.White)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss, enabled = !isBusy) {
        Text("Cancel", color = Color(0xFFD2C4EB))
      }
    },
  )
}

@Composable
private fun UserProfileDetailDialog(
  user: PublicProfile,
  isFollowing: Boolean,
  isLiked: Boolean,
  onDismiss: () -> Unit,
  onToggleFollow: () -> Unit,
  onToggleLike: () -> Unit,
  onStartChat: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = DarkSurface,
    shape = RoundedCornerShape(24.dp),
    title = {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Box(
          contentAlignment = Alignment.Center,
          modifier =
            Modifier.size(60.dp)
              .background(
                Brush.linearGradient(colors = listOf(BadgePurple, NeonPink)),
                CircleShape,
              ),
        ) {
          Text(
            text = user.displayName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "S",
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 22.sp,
          )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
          text = user.displayName.ifBlank { "Shayari Lover" },
          color = Color.White,
          fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = user.handle, color = CrownGold, style = MaterialTheme.typography.labelLarge)
        if (user.city.isNotBlank()) {
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = user.city,
            color = Color(0xFFD2C4EB),
            style = MaterialTheme.typography.bodySmall,
          )
        }
      }
    },
    text = {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
      ) {
        if (user.bio.isNotBlank()) {
          Text(
            text = user.bio,
            color = Color.White.copy(alpha = 0.9f),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
          )
          Spacer(modifier = Modifier.height(14.dp))
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
          ProfileStatColumn(label = "Followers", count = user.followersCount)
          ProfileStatColumn(label = "Following", count = user.followingCount)
          ProfileStatColumn(label = "Likes", count = user.likesCount)
        }
      }
    },
    confirmButton = {
      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        OutlinedButton(
          onClick = onToggleLike,
          shape = RoundedCornerShape(12.dp),
          border = BorderStroke(1.dp, if (isLiked) NeonPink else CrownGold.copy(alpha = 0.6f)),
          modifier = Modifier.minimumInteractiveComponentSize(),
        ) {
          Icon(
            painter =
              painterResource(
                id = if (isLiked) R.drawable.ic_heart_filled else R.drawable.ic_heart
              ),
            contentDescription = "Like profile",
            tint = if (isLiked) NeonPink else Color.White,
            modifier = Modifier.size(16.dp),
          )
        }
        Button(
          onClick = onToggleFollow,
          shape = RoundedCornerShape(12.dp),
          colors =
            ButtonDefaults.buttonColors(
              containerColor = if (isFollowing) DarkSurfaceVariant else BadgePurple
            ),
          modifier = Modifier.weight(1f).minimumInteractiveComponentSize(),
        ) {
          Text(if (isFollowing) "Following" else "Follow", color = Color.White)
        }
        Button(
          onClick = onStartChat,
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = BadgePink),
          modifier = Modifier.weight(1f).minimumInteractiveComponentSize(),
        ) {
          Text("Chat", color = Color.White)
        }
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Close", color = Color(0xFFD2C4EB))
      }
    },
  )
}

@Composable
private fun ProfileStatColumn(label: String, count: Long) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(
      text = count.toString(),
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.ExtraBold,
      color = CrownGold,
    )
    Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color(0xFFD2C4EB))
  }
}

@Composable
private fun CommunityEmptyBox(
  title: String,
  subtitle: String,
  iconRes: Int,
) {
  Column(
    modifier = Modifier.fillMaxSize().padding(24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Surface(
      shape = CircleShape,
      color = NeonPurple.copy(alpha = 0.25f),
      border = BorderStroke(1.dp, NeonVioletGlow.copy(alpha = 0.45f)),
      modifier = Modifier.size(64.dp),
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          painter = painterResource(id = iconRes),
          contentDescription = null,
          tint = CrownGold,
          modifier = Modifier.size(28.dp),
        )
      }
    }
    Spacer(modifier = Modifier.height(14.dp))
    Text(
      text = title,
      style = MaterialTheme.typography.titleLarge,
      color = Color.White,
      textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
      text = subtitle,
      style = MaterialTheme.typography.bodyMedium,
      color = Color(0xFFD2C4EB),
      textAlign = TextAlign.Center,
      modifier = Modifier.widthIn(max = 300.dp),
    )
  }
}
