package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.SocialRepository
import com.example.model.ChatMessage
import com.example.model.ChatRoom
import com.example.model.PublicProfile
import com.example.model.SocialNotification
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
  val searchQuery: String = "",
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
  val communityProfiles: List<PublicProfile> = emptyList(),
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
    get() = communityProfiles.filter { it.effectiveUid != currentUserId }

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
      return base.filter { user ->
        user.displayName.contains(q, ignoreCase = true) ||
          user.effectiveHandle.contains(q, ignoreCase = true) ||
          user.city.contains(q, ignoreCase = true) ||
          user.bio.contains(q, ignoreCase = true)
      }
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
      reportError(unwrapFirestoreRootError(cause))
      emit(null)
    }

  private val discoverableProfilesFlow =
    repository.observeDiscoverableProfiles().catch { cause ->
      reportError(unwrapFirestoreRootError(cause))
      emit(emptyList())
    }

  private val followingIdsFlow =
    repository.observeMyFollowingIds(currentUserId).catch { cause ->
      reportError(unwrapFirestoreRootError(cause))
      emit(emptySet())
    }

  private val likedIdsFlow =
    repository.observeMyLikedProfileIds(currentUserId).catch { cause ->
      reportError(unwrapFirestoreRootError(cause))
      emit(emptySet())
    }

  private val chatsFlow =
    repository.observeMyChats(currentUserId).catch { cause ->
      reportError(unwrapFirestoreRootError(cause))
      emit(emptyList())
    }

  private val notificationsFlow =
    repository.observeSocialNotifications(currentUserId).catch { cause ->
      reportError(unwrapFirestoreRootError(cause))
      emit(emptyList())
    }

  private val activeChatMessagesFlow =
    interactionState.flatMapLatest { state ->
      val partner = state.activeChatPartner
      if (partner == null) {
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
      val livePartner =
        interaction.activeChatPartner?.let { partner ->
          snapshot.discoverableProfiles.firstOrNull { it.effectiveUid == partner.effectiveUid }
            ?: partner
        }
      val inspected =
        interaction.inspectedProfileId?.let { inspectedId ->
          snapshot.discoverableProfiles.firstOrNull { it.effectiveUid == inspectedId }
        }
      SocialUiState(
        currentUserId = currentUserId,
        userEmail = userEmail,
        myProfile = liveMyProfile,
        isBootstrapping = interaction.isBootstrapping,
        isBusy = interaction.isBusy,
        communityProfiles = snapshot.discoverableProfiles,
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
    viewModelScope.launch {
      repository
        .syncGoogleUserOnLogin(
          defaultHandle = defaultHandle,
          defaultDisplayName = defaultName,
          email = userEmail,
          photoUrl = userPhotoUrl,
        )
        .onFailure { reportError(it) }
      interactionState.update { it.copy(isBootstrapping = false) }
    }
  }

  fun updateSearchQuery(query: String) {
    interactionState.update { it.copy(searchQuery = query) }
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
    interactionState.update {
      it.copy(activeChatPartner = partner, inspectedProfileId = null)
    }
  }

  fun openChatFromRoom(room: ChatRoom) {
    val existing = uiState.value.communityProfiles.firstOrNull { it.effectiveUid == room.partnerId }
    val partner =
      existing
        ?: PublicProfile(
          id = room.partnerId,
          userId = room.partnerId,
          uid = room.partnerId,
          handle = room.partnerHandle.ifBlank { "@SL_USER" },
          displayName = room.partnerName.ifBlank { "Shayar Friend" },
        )
    openChatWith(partner)
  }

  fun openFromNotification(notification: SocialNotification) {
    val partner =
      uiState.value.communityProfiles.firstOrNull { it.effectiveUid == notification.senderId }
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

  fun updateProfile(
    displayName: String,
    city: String,
    address: String,
    bio: String,
    onSuccess: () -> Unit = {},
  ) {
    if (interactionState.value.isBusy) return
    viewModelScope.launch {
      interactionState.update { it.copy(isBusy = true) }
      repository
        .updateMyProfile(
          displayName = displayName,
          city = city,
          address = address,
          bio = bio,
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
    val state = uiState.value
    val partner = state.activeChatPartner ?: return
    val clean = rawText.trim()
    if (clean.isEmpty()) return
    viewModelScope.launch {
      repository
        .sendChatMessage(
          selfProfile = state.myProfile,
          targetProfile = partner,
          rawText = clean,
        )
        .onSuccess { onSent() }
        .onFailure { reportError(it) }
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

  private fun reportError(cause: Throwable) {
    val msg = cause.localizedMessage ?: "Firestore sync error"
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
