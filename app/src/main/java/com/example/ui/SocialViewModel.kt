package com.example.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import android.util.Log
import com.example.data.ImageUploadHelper
import com.example.data.SocialRepository
import com.example.model.ChatMessage
import com.example.model.ChatRoom
import com.example.model.PublicProfile
import com.example.model.SocialNotification
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

enum class CommunityFilter {
  ALL,
  FOLLOWING,
  CHATS,
}

private data class SocialStreamSnapshot(
  val myProfile: PublicProfile?,
  val discoverableProfiles: List<PublicProfile>,
  val followingIds: Set<String>,
  val likedIds: Set<String>,
  val chats: List<ChatRoom>,
  val notifications: List<SocialNotification>,
)

private data class SocialLocalInteraction(
  val isBootstrapping: Boolean = true,
  val isBusy: Boolean = false,
  val isUploadingAvatar: Boolean = false,
  val isSearchingRemote: Boolean = false,
  val isSendingMessage: Boolean = false,
  val searchQuery: String = "",
  val remoteSearchMatches: List<PublicProfile> = emptyList(),
  val filter: CommunityFilter = CommunityFilter.ALL,
  val activeChatPartner: PublicProfile? = null,
  val inspectedProfileId: String? = null,
  val actionFeedback: String? = null,
)

data class SocialUiState(
  val currentUserId: String,
  val userEmail: String = "",
  val myProfile: PublicProfile,
  val isBootstrapping: Boolean = true,
  val isBusy: Boolean = false,
  val isUploadingAvatar: Boolean = false,
  val isSearchingRemote: Boolean = false,
  val isSendingMessage: Boolean = false,
  val communityProfiles: List<PublicProfile> = emptyList(),
  val remoteSearchMatches: List<PublicProfile> = emptyList(),
  val followingIds: Set<String> = emptySet(),
  val likedProfileIds: Set<String> = emptySet(),
  val chats: List<ChatRoom> = emptyList(),
  val socialNotifications: List<SocialNotification> = emptyList(),
  val searchQuery: String = "",
  val communityFilter: CommunityFilter = CommunityFilter.ALL,
  val activeChatPartner: PublicProfile? = null,
  val activeChatMessages: List<ChatMessage> = emptyList(),
  val inspectedProfile: PublicProfile? = null,
  val actionFeedback: String? = null,
) {
  val unreadNotificationsCount: Int
    get() = socialNotifications.count { !it.isRead }

  val unreadMessageAlertsCount: Int
    get() = socialNotifications.count { !it.isRead && it.type == "message" }

  val unreadFollowAlertsCount: Int
    get() = socialNotifications.count { !it.isRead && it.type == "follow" }

  val unreadLikeAlertsCount: Int
    get() = socialNotifications.count { !it.isRead && it.type == "like" }

  val otherUsers: List<PublicProfile>
    get() {
      val merged = linkedMapOf<String, PublicProfile>()
      communityProfiles.forEach { profile ->
        if (profile.effectiveUid != currentUserId && profile.effectiveUid.isNotBlank()) {
          merged[profile.effectiveUid] = profile
        }
      }
      remoteSearchMatches.forEach { profile ->
        if (profile.effectiveUid != currentUserId && profile.effectiveUid.isNotBlank()) {
          merged.putIfAbsent(profile.effectiveUid, profile)
        }
      }
      return merged.values.toList()
    }

  val filteredCommunityUsers: List<PublicProfile>
    get() {
      val base =
        when (communityFilter) {
          CommunityFilter.ALL -> otherUsers
          CommunityFilter.FOLLOWING -> otherUsers.filter { it.effectiveUid in followingIds }
          CommunityFilter.CHATS -> {
            val chatPartnerIds = chats.map { it.partnerId }.toSet()
            otherUsers.filter { it.effectiveUid in chatPartnerIds }
          }
        }
      val q = searchQuery.trim()
      if (q.isEmpty()) return base
      return base.filter { user -> user.matchesQuery(q) }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class SocialViewModel(
  private val repository: SocialRepository,
  private val currentUserId: String,
  private val defaultHandle: String,
  private val defaultName: String,
  private val userEmail: String,
  private val userPhotoUrl: String,
) : ViewModel() {

  private val fallbackProfile =
    PublicProfile(
      id = currentUserId,
      userId = currentUserId,
      uid = currentUserId,
      handle = defaultHandle,
      displayName = defaultName,
      photoUrl = userPhotoUrl,
      city = "",
      bio = "Shayari Lok Member",
    )

  private val interactionState = MutableStateFlow(SocialLocalInteraction())
  private var remoteSearchJob: Job? = null

  private fun unwrapFirestoreRootError(error: Throwable): Throwable {
    val firestoreError =
      generateSequence(error) { it.cause }
        .filterIsInstance<FirebaseFirestoreException>()
        .firstOrNull()
    if (firestoreError == null && error is CancellationException) throw error
    return firestoreError ?: error
  }

  private val myProfileFlow =
    repository.observeMyProfile(currentUserId).catch { cause ->
      handleBackgroundStreamError(unwrapFirestoreRootError(cause))
      emit(null)
    }

  private val discoverableProfilesFlow =
    repository.observeDiscoverableProfiles().catch { cause ->
      handleBackgroundStreamError(unwrapFirestoreRootError(cause))
      emit(emptyList())
    }

  private val followingIdsFlow =
    repository.observeMyFollowingIds(currentUserId).catch { cause ->
      handleBackgroundStreamError(unwrapFirestoreRootError(cause))
      emit(emptySet())
    }

  private val likedIdsFlow =
    repository.observeMyLikedProfileIds(currentUserId).catch { cause ->
      handleBackgroundStreamError(unwrapFirestoreRootError(cause))
      emit(emptySet())
    }

  private val chatsFlow =
    repository.observeMyChats(currentUserId).catch { cause ->
      handleBackgroundStreamError(unwrapFirestoreRootError(cause))
      emit(emptyList())
    }

  private val notificationsFlow =
    repository.observeSocialNotifications(currentUserId).catch { cause ->
      handleBackgroundStreamError(unwrapFirestoreRootError(cause))
      emit(emptyList())
    }

  private val activeChatMessagesFlow =
    interactionState.flatMapLatest { state ->
      val partner = state.activeChatPartner
      if (partner == null || partner.effectiveUid == currentUserId) {
        flowOf(emptyList())
      } else {
        val chatId = SocialRepository.buildChatRoomId(currentUserId, partner.effectiveUid)
        repository.observeChatMessages(chatId).catch { cause ->
          reportError(unwrapFirestoreRootError(cause))
          emit(emptyList())
        }
      }
    }

  private val streamSnapshotFlow =
    combine(
      myProfileFlow,
      discoverableProfilesFlow,
      followingIdsFlow,
      likedIdsFlow,
      chatsFlow,
    ) { myProf, profiles, following, liked, chatRooms ->
      SocialStreamSnapshot(
        myProfile = myProf,
        discoverableProfiles = profiles,
        followingIds = following,
        likedIds = liked,
        chats = chatRooms,
        notifications = emptyList(),
      )
    }.combine(notificationsFlow) { snapshot, notifications ->
      snapshot.copy(notifications = notifications)
    }

  val uiState: StateFlow<SocialUiState> =
    combine(streamSnapshotFlow, interactionState, activeChatMessagesFlow) {
        snapshot,
        interaction,
        messages,
      ->
      val liveMyProfile = snapshot.myProfile ?: fallbackProfile
      val allKnown = snapshot.discoverableProfiles + interaction.remoteSearchMatches
      val livePartner =
        interaction.activeChatPartner?.let { partner ->
          allKnown.firstOrNull { it.effectiveUid == partner.effectiveUid } ?: partner
        }
      val inspected =
        interaction.inspectedProfileId?.let { inspectedId ->
          allKnown.firstOrNull { it.effectiveUid == inspectedId }
        }
      SocialUiState(
        currentUserId = currentUserId,
        userEmail = userEmail,
        myProfile = liveMyProfile,
        isBootstrapping = interaction.isBootstrapping,
        isBusy = interaction.isBusy,
        isUploadingAvatar = interaction.isUploadingAvatar,
        isSearchingRemote = interaction.isSearchingRemote,
        isSendingMessage = interaction.isSendingMessage,
        communityProfiles = snapshot.discoverableProfiles,
        remoteSearchMatches = interaction.remoteSearchMatches,
        followingIds = snapshot.followingIds,
        likedProfileIds = snapshot.likedIds,
        chats = snapshot.chats,
        socialNotifications = snapshot.notifications,
        searchQuery = interaction.searchQuery,
        communityFilter = interaction.filter,
        activeChatPartner = livePartner,
        activeChatMessages = messages,
        inspectedProfile = inspected,
        actionFeedback = interaction.actionFeedback,
      )
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5_000L),
      initialValue =
        SocialUiState(
          currentUserId = currentUserId,
          userEmail = userEmail,
          myProfile = fallbackProfile,
        ),
    )

  init {
    syncAccountNow()
  }

  fun syncAccountNow() {
    viewModelScope.launch {
      interactionState.update { it.copy(isBootstrapping = true) }
      val firstAttempt =
        repository.syncGoogleUserOnLogin(
          defaultHandle = defaultHandle,
          defaultDisplayName = defaultName,
          email = userEmail,
          photoUrl = userPhotoUrl,
        )
      if (firstAttempt.isFailure) {
        val root = firstAttempt.exceptionOrNull()?.let { unwrapFirestoreRootError(it) }
        val isPermDenied =
          (root as? FirebaseFirestoreException)?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
        if (isPermDenied) {
          runCatching { Firebase.auth.currentUser?.getIdToken(true)?.await() }
          kotlinx.coroutines.delay(500L)
          val retryResult =
            repository.syncGoogleUserOnLogin(
              defaultHandle = defaultHandle,
              defaultDisplayName = defaultName,
              email = userEmail,
              photoUrl = userPhotoUrl,
            )
          retryResult.onFailure { handleBackgroundStreamError(unwrapFirestoreRootError(it)) }
        } else if (root != null) {
          handleBackgroundStreamError(root)
        }
      }
      interactionState.update { it.copy(isBootstrapping = false) }
    }
  }

  fun updateSearchQuery(query: String) {
    interactionState.update { it.copy(searchQuery = query) }
    remoteSearchJob?.cancel()
    val trimmed = query.trim()
    if (trimmed.length < 2) {
      interactionState.update { it.copy(isSearchingRemote = false, remoteSearchMatches = emptyList()) }
      return
    }
    remoteSearchJob =
      viewModelScope.launch {
        delay(280L)
        interactionState.update { it.copy(isSearchingRemote = true) }
        repository.searchDiscoverableProfiles(trimmed).fold(
          onSuccess = { matches ->
            interactionState.update {
              it.copy(isSearchingRemote = false, remoteSearchMatches = matches)
            }
          },
          onFailure = {
            interactionState.update { it.copy(isSearchingRemote = false) }
          },
        )
      }
  }

  fun refreshSearchAndDirectory() {
    val q = interactionState.value.searchQuery.trim()
    syncAccountNow()
    if (q.length >= 2) {
      updateSearchQuery(q)
    }
  }

  fun selectFilter(filter: CommunityFilter) {
    interactionState.update { it.copy(filter = filter) }
  }

  fun inspectProfile(profile: PublicProfile) {
    interactionState.update { it.copy(inspectedProfileId = profile.effectiveUid) }
  }

  fun dismissProfileSheet() {
    interactionState.update { it.copy(inspectedProfileId = null) }
  }

  fun openChatWith(partner: PublicProfile) {
    if (partner.effectiveUid == currentUserId || partner.effectiveUid.isBlank()) {
      interactionState.update { it.copy(actionFeedback = "Cannot start chat with this user") }
      return
    }
    interactionState.update {
      it.copy(activeChatPartner = partner, inspectedProfileId = null)
    }
  }

  fun openChatFromRoom(room: ChatRoom) {
    val allKnown = uiState.value.communityProfiles + uiState.value.remoteSearchMatches
    val existing = allKnown.firstOrNull { it.effectiveUid == room.partnerId }
    val partner =
      existing
        ?: PublicProfile(
          id = room.partnerId,
          userId = room.partnerId,
          uid = room.partnerId,
          handle = room.partnerHandle.ifBlank { "@SL_USER" },
          displayName = room.partnerName.ifBlank { "Shayar Friend" },
          photoUrl = room.partnerPhotoUrl,
        )
    openChatWith(partner)
  }

  fun openFromNotification(notification: SocialNotification) {
    val allKnown = uiState.value.communityProfiles + uiState.value.remoteSearchMatches
    val partner =
      allKnown.firstOrNull { it.effectiveUid == notification.senderId }
        ?: PublicProfile(
          id = notification.senderId,
          userId = notification.senderId,
          uid = notification.senderId,
          handle = notification.senderHandle.ifBlank { "@SL_USER" },
          displayName = notification.senderName.ifBlank { "Shayar Friend" },
        )
    if (notification.type == "message") {
      openChatWith(partner)
    } else {
      inspectProfile(partner)
    }
  }

  fun closeChat() {
    interactionState.update { it.copy(activeChatPartner = null) }
  }

  fun uploadGalleryAvatar(context: Context, uri: Uri, onResult: (String?) -> Unit = {}) {
    if (interactionState.value.isUploadingAvatar) return
    viewModelScope.launch {
      interactionState.update { it.copy(isUploadingAvatar = true) }
      val encoded = ImageUploadHelper.processPickedAvatarUri(context, uri)
      encoded.fold(
        onSuccess = { dataUri ->
          repository.updateMyAvatarPhoto(dataUri).fold(
            onSuccess = {
              interactionState.update {
                it.copy(isUploadingAvatar = false, actionFeedback = "Profile photo updated!")
              }
              onResult(dataUri)
            },
            onFailure = { err ->
              interactionState.update {
                it.copy(
                  isUploadingAvatar = false,
                  actionFeedback = err.localizedMessage ?: "Failed to save profile photo",
                )
              }
              onResult(null)
            },
          )
        },
        onFailure = { err ->
          interactionState.update {
            it.copy(
              isUploadingAvatar = false,
              actionFeedback = err.localizedMessage ?: "Could not process selected photo",
            )
          }
          onResult(null)
        },
      )
    }
  }

  fun clearAvatarPhoto() {
    if (interactionState.value.isUploadingAvatar) return
    viewModelScope.launch {
      interactionState.update { it.copy(isUploadingAvatar = true) }
      repository.updateMyAvatarPhoto("").fold(
        onSuccess = {
          interactionState.update {
            it.copy(isUploadingAvatar = false, actionFeedback = "Profile photo removed")
          }
        },
        onFailure = { err ->
          interactionState.update {
            it.copy(
              isUploadingAvatar = false,
              actionFeedback = err.localizedMessage ?: "Could not remove photo",
            )
          }
        },
      )
    }
  }

  fun updateProfile(
    displayName: String,
    handle: String?,
    city: String,
    address: String,
    bio: String,
    photoUrl: String? = null,
    onSuccess: () -> Unit = {},
  ) {
    if (interactionState.value.isBusy) return
    viewModelScope.launch {
      interactionState.update { it.copy(isBusy = true) }
      repository
        .updateMyProfile(
          displayName = displayName,
          handle = handle,
          city = city,
          address = address,
          bio = bio,
          photoUrl = photoUrl,
        )
        .onSuccess {
          interactionState.update {
            it.copy(isBusy = false, actionFeedback = "Profile synced to Firestore!")
          }
          onSuccess()
        }
        .onFailure { err ->
          interactionState.update {
            it.copy(
              isBusy = false,
              actionFeedback = err.localizedMessage ?: "Could not update profile",
            )
          }
        }
    }
  }

  fun toggleFollow(target: PublicProfile) {
    val state = uiState.value
    val currentlyFollowing = target.effectiveUid in state.followingIds
    viewModelScope.launch {
      repository
        .toggleFollow(
          selfProfile = state.myProfile,
          targetProfile = target,
          currentlyFollowing = currentlyFollowing,
        )
        .onSuccess { isNowFollowing ->
          val msg =
            if (isNowFollowing) "Following ${target.displayName}" else "Unfollowed ${target.displayName}"
          interactionState.update { it.copy(actionFeedback = msg) }
        }
        .onFailure { reportError(it) }
    }
  }

  fun toggleLike(target: PublicProfile) {
    val state = uiState.value
    val currentlyLiked = target.effectiveUid in state.likedProfileIds
    viewModelScope.launch {
      repository
        .toggleProfileLike(
          selfProfile = state.myProfile,
          targetProfile = target,
          currentlyLiked = currentlyLiked,
        )
        .onFailure { reportError(it) }
    }
  }

  fun sendMessage(rawText: String, onSent: () -> Unit = {}) {
    if (interactionState.value.isSendingMessage) return
    val state = uiState.value
    val partner = state.activeChatPartner ?: return
    val clean = rawText.trim()
    if (clean.isEmpty()) return
    viewModelScope.launch {
      interactionState.update { it.copy(isSendingMessage = true) }
      repository
        .sendChatMessage(
          selfProfile = state.myProfile,
          targetProfile = partner,
          rawText = clean,
        )
        .onSuccess {
          interactionState.update { it.copy(isSendingMessage = false) }
          onSent()
        }
        .onFailure { err ->
          interactionState.update { it.copy(isSendingMessage = false) }
          reportError(err)
        }
    }
  }

  fun markActiveChatMessagesRead(messages: List<ChatMessage>) {
    val partner = uiState.value.activeChatPartner ?: return
    val chatId = SocialRepository.buildChatRoomId(currentUserId, partner.effectiveUid)
    if (messages.none { !it.isRead && it.effectiveRecipientId == currentUserId }) return
    viewModelScope.launch {
      repository.markMessagesRead(chatId = chatId, unreadMessages = messages)
    }
  }

  fun markAllNotificationsRead() {
    val currentNotifs = uiState.value.socialNotifications
    if (currentNotifs.none { !it.isRead }) return
    viewModelScope.launch {
      repository.markNotificationsRead(selfUid = currentUserId, unread = currentNotifs)
    }
  }

  fun clearActionFeedback() {
    interactionState.update { it.copy(actionFeedback = null) }
  }

  private fun handleBackgroundStreamError(cause: Throwable) {
    Log.w("SocialViewModel", "Firestore stream fallback active: ${cause.localizedMessage}")
  }

  private fun reportError(cause: Throwable) {
    val root = unwrapFirestoreRootError(cause)
    val msg =
      if (root is FirebaseFirestoreException &&
        root.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
      ) {
        "Database permission restricted. Please check Firestore Rules in Firebase Console."
      } else {
        root.localizedMessage ?: "Firestore sync error"
      }
    interactionState.update { it.copy(actionFeedback = msg) }
  }

  companion object {
    fun factory(
      repository: SocialRepository,
      currentUserId: String,
      defaultHandle: String,
      defaultName: String,
      userEmail: String,
      userPhotoUrl: String,
    ): ViewModelProvider.Factory = viewModelFactory {
      initializer {
        SocialViewModel(
          repository = repository,
          currentUserId = currentUserId,
          defaultHandle = defaultHandle,
          defaultName = defaultName,
          userEmail = userEmail,
          userPhotoUrl = userPhotoUrl,
        )
      }
    }
  }
}
