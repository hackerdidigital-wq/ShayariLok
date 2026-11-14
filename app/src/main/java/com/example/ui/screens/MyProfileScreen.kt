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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.ChatRoom
import com.example.ui.SocialUiState
import com.example.ui.theme.BadgePink
import com.example.ui.theme.BadgePurple
import com.example.ui.theme.CrownGold
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonVioletGlow

@Composable
fun MyProfileScreen(
  uiState: SocialUiState,
  onCopyHandle: (String) -> Unit,
  onUpdateProfile: (name: String, city: String, address: String, bio: String, onDone: () -> Unit) -> Unit,
  onOpenChatRoom: (ChatRoom) -> Unit,
  onOpenSettings: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var showEditDialog by remember { mutableStateOf(false) }
  val profile = uiState.myProfile

  LazyColumn(
    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = modifier.fillMaxSize(),
  ) {
    item {
      Surface(
        shape = RoundedCornerShape(24.dp),
        color = DarkSurface.copy(alpha = 0.94f),
        border = BorderStroke(1.dp, NeonVioletGlow.copy(alpha = 0.45f)),
        shadowElevation = 6.dp,
        modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.padding(20.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Box(
            contentAlignment = Alignment.Center,
            modifier =
              Modifier.size(74.dp)
                .background(
                  Brush.linearGradient(colors = listOf(BadgePurple, NeonPink)),
                  CircleShape,
                ),
          ) {
            Text(
              text = profile.displayName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "S",
              color = Color.White,
              fontWeight = FontWeight.ExtraBold,
              fontSize = 28.sp,
            )
          }

          Spacer(modifier = Modifier.height(12.dp))

          Text(
            text = profile.displayName.ifBlank { "Shayari Lover" },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center,
          )

          Spacer(modifier = Modifier.height(6.dp))

          Surface(
            shape = RoundedCornerShape(50),
            color = DarkSurfaceVariant,
            border = BorderStroke(1.dp, CrownGold.copy(alpha = 0.65f)),
            modifier =
              Modifier.clip(RoundedCornerShape(50))
                .clickable { onCopyHandle(profile.handle) }
                .minimumInteractiveComponentSize()
                .testTag("copy_my_handle_button"),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            ) {
              Text(
                text = profile.handle,
                style = MaterialTheme.typography.labelLarge,
                color = CrownGold,
                fontWeight = FontWeight.ExtraBold,
              )
              Spacer(modifier = Modifier.width(6.dp))
              Icon(
                painter = painterResource(id = R.drawable.ic_copy),
                contentDescription = "Copy Handle",
                tint = CrownGold,
                modifier = Modifier.size(14.dp),
              )
            }
          }

          val locationLabel =
            listOf(profile.city.trim(), profile.address.trim())
              .filter { it.isNotBlank() }
              .distinct()
              .joinToString(", ")

          if (locationLabel.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                painter = painterResource(id = R.drawable.ic_location),
                contentDescription = null,
                tint = Color(0xFFD2C4EB),
                modifier = Modifier.size(15.dp),
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = locationLabel,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFD2C4EB),
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          Text(
            text = profile.bio.ifBlank { "Tap Edit Profile to add your Name, City, Address & Bio" },
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.9f),
            textAlign = TextAlign.Center,
          )

          Spacer(modifier = Modifier.height(16.dp))

          Surface(
            shape = RoundedCornerShape(16.dp),
            color = DarkSurfaceVariant.copy(alpha = 0.8f),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
              horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
              LiveStatPill(label = "Followers", count = profile.followersCount)
              LiveStatPill(label = "Following", count = profile.followingCount)
              LiveStatPill(label = "Likes", count = profile.likesCount)
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Button(
              onClick = { showEditDialog = true },
              shape = RoundedCornerShape(14.dp),
              colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
              modifier =
                Modifier.weight(1f).minimumInteractiveComponentSize().testTag("edit_profile_button"),
            ) {
              Icon(
                painter = painterResource(id = R.drawable.ic_edit),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text("Edit Profile", color = Color.White, fontWeight = FontWeight.SemiBold)
            }

            OutlinedButton(
              onClick = onOpenSettings,
              shape = RoundedCornerShape(14.dp),
              border = BorderStroke(1.dp, NeonVioletGlow.copy(alpha = 0.5f)),
              modifier = Modifier.minimumInteractiveComponentSize().testTag("profile_settings_button"),
            ) {
              Icon(
                painter = painterResource(id = R.drawable.ic_settings),
                contentDescription = "Settings",
                tint = Color.White,
                modifier = Modifier.size(18.dp),
              )
            }
          }
        }
      }
    }

    item {
      Row(
        modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "Recent 1-on-1 Chats (${uiState.chats.size})",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = Color.White,
        )
      }
    }

    if (uiState.chats.isEmpty()) {
      item {
        Surface(
          shape = RoundedCornerShape(18.dp),
          color = DarkSurface.copy(alpha = 0.72f),
          border = BorderStroke(1.dp, NeonVioletGlow.copy(alpha = 0.25f)),
          modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth(),
        ) {
          Text(
            text = "No private chats yet. Open Friends tab to message any user!",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFD2C4EB),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(18.dp),
          )
        }
      }
    } else {
      items(uiState.chats, key = { it.id }) { room ->
        Box(modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth()) {
          ChatThreadPreviewCard(
            room = room,
            onClick = { onOpenChatRoom(room) },
          )
        }
      }
    }

    item { Spacer(modifier = Modifier.height(14.dp)) }
  }

  if (showEditDialog) {
    EditProfileDialog(
      initialName = profile.displayName,
      initialCity = profile.city,
      initialAddress = profile.address,
      initialBio = profile.bio,
      isBusy = uiState.isBusy,
      onDismiss = { showEditDialog = false },
      onSave = { name, city, address, bio ->
        onUpdateProfile(name, city, address, bio) { showEditDialog = false }
      },
    )
  }
}

@Composable
private fun LiveStatPill(label: String, count: Long) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(
      text = count.toString(),
      style = MaterialTheme.typography.titleLarge,
      fontWeight = FontWeight.ExtraBold,
      color = CrownGold,
    )
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = Color(0xFFD2C4EB),
    )
  }
}

@Composable
private fun EditProfileDialog(
  initialName: String,
  initialCity: String,
  initialAddress: String,
  initialBio: String,
  isBusy: Boolean,
  onDismiss: () -> Unit,
  onSave: (name: String, city: String, address: String, bio: String) -> Unit,
) {
  var name by remember { mutableStateOf(initialName) }
  var city by remember { mutableStateOf(initialCity) }
  var address by remember { mutableStateOf(initialAddress) }
  var bio by remember { mutableStateOf(initialBio) }

  AlertDialog(
    onDismissRequest = { if (!isBusy) onDismiss() },
    containerColor = DarkSurface,
    shape = RoundedCornerShape(24.dp),
    title = {
      Text(
        text = "Edit My Profile",
        color = Color.White,
        fontWeight = FontWeight.Bold,
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = name,
          onValueChange = { if (it.length <= 60) name = it },
          singleLine = true,
          label = { Text("Display Name") },
          colors =
            OutlinedTextFieldDefaults.colors(
              focusedBorderColor = CrownGold,
              unfocusedBorderColor = NeonVioletGlow.copy(alpha = 0.5f),
              focusedTextColor = Color.White,
              unfocusedTextColor = Color.White,
              focusedLabelColor = CrownGold,
              unfocusedLabelColor = Color(0xFFD2C4EB),
            ),
          modifier = Modifier.fillMaxWidth().testTag("edit_name_input"),
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
          modifier = Modifier.fillMaxWidth().testTag("edit_city_input"),
        )
        OutlinedTextField(
          value = address,
          onValueChange = { if (it.length <= 160) address = it },
          singleLine = true,
          label = { Text("Address") },
          colors =
            OutlinedTextFieldDefaults.colors(
              focusedBorderColor = CrownGold,
              unfocusedBorderColor = NeonVioletGlow.copy(alpha = 0.5f),
              focusedTextColor = Color.White,
              unfocusedTextColor = Color.White,
              focusedLabelColor = CrownGold,
              unfocusedLabelColor = Color(0xFFD2C4EB),
            ),
          modifier = Modifier.fillMaxWidth().testTag("edit_address_input"),
        )
        OutlinedTextField(
          value = bio,
          onValueChange = { if (it.length <= 220) bio = it },
          minLines = 2,
          maxLines = 4,
          label = { Text("Bio / Favorite Shayari Line") },
          colors =
            OutlinedTextFieldDefaults.colors(
              focusedBorderColor = CrownGold,
              unfocusedBorderColor = NeonVioletGlow.copy(alpha = 0.5f),
              focusedTextColor = Color.White,
              unfocusedTextColor = Color.White,
              focusedLabelColor = CrownGold,
              unfocusedLabelColor = Color(0xFFD2C4EB),
            ),
          modifier = Modifier.fillMaxWidth().testTag("edit_bio_input"),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onSave(name, city, address, bio) },
        enabled = !isBusy && name.isNotBlank(),
        colors = ButtonDefaults.buttonColors(containerColor = BadgePink),
        modifier = Modifier.minimumInteractiveComponentSize().testTag("save_profile_button"),
      ) {
        Text(if (isBusy) "Saving..." else "Save Profile", color = Color.White)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss, enabled = !isBusy) {
        Text("Cancel", color = Color(0xFFD2C4EB))
      }
    },
  )
}
