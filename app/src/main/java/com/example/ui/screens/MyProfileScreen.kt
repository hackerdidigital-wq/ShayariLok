package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.example.ui.components.UserAvatar
import com.example.ui.i18n.LocalAppStrings
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
  allowAdultContent: Boolean,
  onToggleAdultContent: (Boolean) -> Unit,
  onOpenLocalePicker: () -> Unit,
  onCopyHandle: (String) -> Unit,
  onPickGalleryAvatar: (Uri) -> Unit,
  onRemoveAvatar: () -> Unit,
  onUpdateProfile: (name: String, handle: String, city: String, address: String, bio: String, onDone: () -> Unit) -> Unit,
  onOpenChatRoom: (ChatRoom) -> Unit,
  onOpenSettings: () -> Unit,
  onSignOut: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val strings = LocalAppStrings.current
  var showEditDialog by remember { mutableStateOf(false) }
  var showAgeConfirmDialog by remember { mutableStateOf(false) }
  val profile = uiState.myProfile

  val photoPickerLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) {
        onPickGalleryAvatar(uri)
      }
    }

  val launchGalleryPicker: () -> Unit = {
    photoPickerLauncher.launch(
      PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
    )
  }

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
            contentAlignment = Alignment.BottomEnd,
            modifier =
              Modifier.clip(CircleShape)
                .clickable(enabled = !uiState.isUploadingAvatar) { launchGalleryPicker() }
                .testTag("profile_avatar_picker"),
          ) {
            UserAvatar(
              photoUrl = profile.photoUrl,
              displayName = profile.displayName,
              size = 86.dp,
              initialFontSize = 30.sp,
            )
            Surface(
              shape = CircleShape,
              color = if (uiState.isUploadingAvatar) DarkSurfaceVariant else BadgePink,
              border = BorderStroke(1.5.dp, Color.White),
              modifier = Modifier.size(28.dp),
            ) {
              Box(contentAlignment = Alignment.Center) {
                if (uiState.isUploadingAvatar) {
                  CircularProgressIndicator(
                    color = CrownGold,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(14.dp),
                  )
                } else {
                  Icon(
                    painter = painterResource(id = R.drawable.ic_edit),
                    contentDescription = strings.changePhotoGallery,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp),
                  )
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(6.dp))
          TextButton(
            onClick = launchGalleryPicker,
            enabled = !uiState.isUploadingAvatar,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("change_photo_button"),
          ) {
            Text(
              text =
                if (uiState.isUploadingAvatar) strings.uploadingPhoto else strings.changePhotoGallery,
              style = MaterialTheme.typography.labelMedium,
              color = CrownGold,
              fontWeight = FontWeight.SemiBold,
            )
          }

          Spacer(modifier = Modifier.height(4.dp))

          Text(
            text = profile.displayName.ifBlank { "Shayari Lover" },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center,
          )

          if (uiState.userEmail.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = uiState.userEmail,
              style = MaterialTheme.typography.bodySmall,
              color = Color(0xFFD2C4EB),
              textAlign = TextAlign.Center,
            )
          }

          Spacer(modifier = Modifier.height(8.dp))

          Surface(
            shape = RoundedCornerShape(50),
            color = DarkSurfaceVariant,
            border = BorderStroke(1.dp, CrownGold.copy(alpha = 0.65f)),
            modifier =
              Modifier.clip(RoundedCornerShape(50))
                .clickable { onCopyHandle(profile.effectiveHandle) }
                .minimumInteractiveComponentSize()
                .testTag("copy_my_handle_button"),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            ) {
              Text(
                text = profile.effectiveHandle,
                style = MaterialTheme.typography.labelLarge,
                color = CrownGold,
                fontWeight = FontWeight.ExtraBold,
              )
              Spacer(modifier = Modifier.width(6.dp))
              Icon(
                painter = painterResource(id = R.drawable.ic_copy),
                contentDescription = strings.actionCopy,
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
            text = profile.bio.ifBlank { strings.tapEditBioHint },
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
              LiveStatPill(label = strings.followersLabel, count = profile.followersCount)
              LiveStatPill(label = strings.followingLabel, count = profile.followingCount)
              LiveStatPill(label = strings.likesLabel, count = profile.effectiveLikesCount)
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
              Text(strings.editProfile, color = Color.White, fontWeight = FontWeight.SemiBold)
            }

            OutlinedButton(
              onClick = onOpenLocalePicker,
              shape = RoundedCornerShape(14.dp),
              border = BorderStroke(1.dp, CrownGold.copy(alpha = 0.5f)),
              modifier =
                Modifier.minimumInteractiveComponentSize().testTag("profile_language_button"),
            ) {
              Icon(
                painter = painterResource(id = R.drawable.ic_language),
                contentDescription = strings.appLanguageTitle,
                tint = CrownGold,
                modifier = Modifier.size(18.dp),
              )
            }

            OutlinedButton(
              onClick = onOpenSettings,
              shape = RoundedCornerShape(14.dp),
              border = BorderStroke(1.dp, NeonVioletGlow.copy(alpha = 0.5f)),
              modifier =
                Modifier.minimumInteractiveComponentSize().testTag("profile_settings_button"),
            ) {
              Icon(
                painter = painterResource(id = R.drawable.ic_settings),
                contentDescription = strings.tabSettings,
                tint = Color.White,
                modifier = Modifier.size(18.dp),
              )
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          // 18+ Content Safety & Maturity Filter Card
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = DarkSurfaceVariant.copy(alpha = 0.85f),
            border =
              BorderStroke(
                1.dp,
                if (allowAdultContent) NeonPink.copy(alpha = 0.65f) else NeonVioletGlow.copy(alpha = 0.35f),
              ),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                painter = painterResource(id = R.drawable.ic_shield),
                contentDescription = null,
                tint = if (allowAdultContent) NeonPink else CrownGold,
                modifier = Modifier.size(20.dp),
              )
              Spacer(modifier = Modifier.width(10.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = strings.adultContentTitle,
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  color = Color.White,
                )
                Text(
                  text =
                    if (allowAdultContent) {
                      strings.adultContentSubtitleOn
                    } else {
                      strings.adultContentSubtitleOff
                    },
                  style = MaterialTheme.typography.labelSmall,
                  color = Color(0xFFD2C4EB),
                )
              }
              Switch(
                checked = allowAdultContent,
                onCheckedChange = { wantEnable ->
                  if (wantEnable) {
                    showAgeConfirmDialog = true
                  } else {
                    onToggleAdultContent(false)
                  }
                },
                colors =
                  SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = NeonPink,
                    uncheckedThumbColor = CrownGold,
                    uncheckedTrackColor = DarkSurface,
                  ),
                modifier = Modifier.testTag("profile_adult_content_switch"),
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          OutlinedButton(
            onClick = onSignOut,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, NeonPink.copy(alpha = 0.55f)),
            modifier =
              Modifier.fillMaxWidth()
                .minimumInteractiveComponentSize()
                .testTag("sign_out_button"),
          ) {
            Text(
              text = strings.signOut,
              color = Color(0xFFFFB6CE),
              fontWeight = FontWeight.SemiBold,
            )
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
          text = "${strings.recentChatsTitle} (${uiState.chats.size})",
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
            text = strings.noPrivateChatsYet,
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
      initialHandle = profile.effectiveHandle,
      initialCity = profile.city,
      initialAddress = profile.address,
      initialBio = profile.bio,
      currentPhotoUrl = profile.photoUrl,
      isBusy = uiState.isBusy,
      isUploadingAvatar = uiState.isUploadingAvatar,
      onPickGalleryPhoto = launchGalleryPicker,
      onRemovePhoto = onRemoveAvatar,
      onDismiss = { showEditDialog = false },
      onSave = { name, handle, city, address, bio ->
        onUpdateProfile(name, handle, city, address, bio) { showEditDialog = false }
      },
    )
  }

  if (showAgeConfirmDialog) {
    AgeConfirmationDialog(
      onConfirm18Plus = {
        showAgeConfirmDialog = false
        onToggleAdultContent(true)
      },
      onDismiss = { showAgeConfirmDialog = false },
    )
  }
}

@Composable
fun AgeConfirmationDialog(
  onConfirm18Plus: () -> Unit,
  onDismiss: () -> Unit,
) {
  val strings = LocalAppStrings.current
  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = DarkSurface,
    shape = RoundedCornerShape(24.dp),
    title = {
      Text(
        text = strings.ageConfirmTitle,
        color = Color.White,
        fontWeight = FontWeight.Bold,
      )
    },
    text = {
      Text(
        text = strings.ageConfirmBody,
        color = Color(0xFFD2C4EB),
        style = MaterialTheme.typography.bodyMedium,
      )
    },
    confirmButton = {
      Button(
        onClick = onConfirm18Plus,
        colors = ButtonDefaults.buttonColors(containerColor = BadgePink),
        modifier = Modifier.minimumInteractiveComponentSize().testTag("confirm_18_plus_yes"),
      ) {
        Text(strings.ageConfirmYes, color = Color.White, fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.minimumInteractiveComponentSize().testTag("confirm_18_plus_no"),
      ) {
        Text(strings.ageConfirmNo, color = CrownGold)
      }
    },
  )
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
  initialHandle: String,
  initialCity: String,
  initialAddress: String,
  initialBio: String,
  currentPhotoUrl: String,
  isBusy: Boolean,
  isUploadingAvatar: Boolean,
  onPickGalleryPhoto: () -> Unit,
  onRemovePhoto: () -> Unit,
  onDismiss: () -> Unit,
  onSave: (name: String, handle: String, city: String, address: String, bio: String) -> Unit,
) {
  val strings = LocalAppStrings.current
  var name by remember { mutableStateOf(initialName) }
  var handle by remember { mutableStateOf(initialHandle.removePrefix("@")) }
  var city by remember { mutableStateOf(initialCity) }
  var address by remember { mutableStateOf(initialAddress) }
  var bio by remember { mutableStateOf(initialBio) }

  AlertDialog(
    onDismissRequest = { if (!isBusy) onDismiss() },
    containerColor = DarkSurface,
    shape = RoundedCornerShape(24.dp),
    title = {
      Text(
        text = strings.editMyProfileTitle,
        color = Color.White,
        fontWeight = FontWeight.Bold,
      )
    },
    text = {
      Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.verticalScroll(rememberScrollState()),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          UserAvatar(
            photoUrl = currentPhotoUrl,
            displayName = name.ifBlank { initialName },
            size = 56.dp,
            initialFontSize = 20.sp,
          )
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            OutlinedButton(
              onClick = onPickGalleryPhoto,
              enabled = !isUploadingAvatar && !isBusy,
              shape = RoundedCornerShape(12.dp),
              border = BorderStroke(1.dp, CrownGold.copy(alpha = 0.65f)),
              modifier = Modifier.minimumInteractiveComponentSize().testTag("dialog_pick_photo_button"),
            ) {
              Text(
                text =
                  if (isUploadingAvatar) strings.uploadingPhoto else strings.changePhotoGallery,
                color = CrownGold,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
              )
            }
            if (currentPhotoUrl.isNotBlank()) {
              TextButton(
                onClick = onRemovePhoto,
                enabled = !isUploadingAvatar && !isBusy,
                modifier = Modifier.testTag("dialog_remove_photo_button"),
              ) {
                Text(
                  text = strings.removePhoto,
                  color = Color(0xFFFFB6CE),
                  style = MaterialTheme.typography.labelSmall,
                )
              }
            }
          }
        }

        OutlinedTextField(
          value = name,
          onValueChange = { if (it.length <= 60) name = it },
          singleLine = true,
          label = { Text(strings.displayNameLabel) },
          colors = profileFieldColors(),
          modifier = Modifier.fillMaxWidth().testTag("edit_name_input"),
        )
        OutlinedTextField(
          value = handle,
          onValueChange = { raw ->
            val sanitized = raw.filter { it.isLetterOrDigit() || it == '_' }
            if (sanitized.length <= 24) handle = sanitized
          },
          singleLine = true,
          prefix = { Text("@", color = CrownGold, fontWeight = FontWeight.Bold) },
          label = { Text(strings.handleLabel) },
          supportingText = {
            Text(strings.handleHelper, color = Color(0xFFBBAAD9), fontSize = 11.sp)
          },
          colors = profileFieldColors(),
          modifier = Modifier.fillMaxWidth().testTag("edit_handle_input"),
        )
        OutlinedTextField(
          value = city,
          onValueChange = { if (it.length <= 80) city = it },
          singleLine = true,
          label = { Text(strings.cityLabel) },
          colors = profileFieldColors(),
          modifier = Modifier.fillMaxWidth().testTag("edit_city_input"),
        )
        OutlinedTextField(
          value = address,
          onValueChange = { if (it.length <= 160) address = it },
          singleLine = true,
          label = { Text(strings.addressLabel) },
          colors = profileFieldColors(),
          modifier = Modifier.fillMaxWidth().testTag("edit_address_input"),
        )
        OutlinedTextField(
          value = bio,
          onValueChange = { if (it.length <= 220) bio = it },
          minLines = 2,
          maxLines = 4,
          label = { Text(strings.bioLabel) },
          colors = profileFieldColors(),
          modifier = Modifier.fillMaxWidth().testTag("edit_bio_input"),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onSave(name, handle, city, address, bio) },
        enabled = !isBusy && name.isNotBlank() && handle.length >= 3,
        colors = ButtonDefaults.buttonColors(containerColor = BadgePink),
        modifier = Modifier.minimumInteractiveComponentSize().testTag("save_profile_button"),
      ) {
        Text(if (isBusy) strings.savingLabel else strings.saveProfile, color = Color.White)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss, enabled = !isBusy) {
        Text(strings.cancelLabel, color = Color(0xFFD2C4EB))
      }
    },
  )
}

@Composable
private fun profileFieldColors() =
  OutlinedTextFieldDefaults.colors(
    focusedBorderColor = CrownGold,
    unfocusedBorderColor = NeonVioletGlow.copy(alpha = 0.5f),
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedLabelColor = CrownGold,
    unfocusedLabelColor = Color(0xFFD2C4EB),
  )
