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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
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
    get() = communityProfiles.filter { it.userId != currentUserId }

  val filteredCommunityUsers: List<PublicProfile>
    get() {
      val base =
        when (communityFilter) {
          CommunityFilter.ALL -> otherUsers
          CommunityFilter.FOLLOWING -> otherUsers.filter { it.userId in followingIds }
          CommunityFilter.CHATS -> {
            val chatPartnerIds = chats.map { it.partnerId }.toSet()
            otherUsers.filter { it.userId in chatPartnerIds }
          }
        }
      val q = searchQuery.trim()
      if (q.isEmpty()) return base
      return base.filter { user ->
        user.displayName.contains(q, ignoreCase = true) ||
          user.handle.contains(q, ignoreCase = true) ||
          user.city.contains(q, ignoreCase = true) ||
          user.address.contains(q, ignoreCase = true) ||
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
) : ViewModel() {

  private val fallbackProfile =
    PublicProfile(
      id = currentUserId,
      userId = currentUserId,
      handle = defaultHandle,
      displayName = defaultName,
      city = "",
      address = "",
      bio = "Shayari Lok Member",
    )

  private val interactionState = MutableStateFlow(SocialLocalInteraction())

  private val myProfileFlow =
    repository.observeMyProfile(currentUserId).catch { cause ->
      if (cause is CancellationException) throw cause
      reportError(cause)
      emit(null)
    }

  private val discoverableProfilesFlow =
    repository.observeDiscoverableProfiles().catch { cause ->
      if (cause is CancellationException) throw cause
      reportError(cause)
      emit(emptyList())
    }

  private val followingIdsFlow =
    repository.observeMyFollowingIds().catch { cause ->
      if (cause is CancellationException) throw cause
      reportError(cause)
      emit(emptySet())
    }

  private val likedIdsFlow =
    repository.observeMyLikedProfileIds().catch { cause ->
      if (cause is CancellationException) throw cause
      reportError(cause)
      emit(emptySet())
    }

  private val chatsFlow =
    repository.observeMyChats().catch { cause ->
      if (cause is CancellationException) throw cause
      reportError(cause)
      emit(emptyList())
    }

  private val notificationsFlow =
    repository.observeSocialNotifications().catch { cause ->
      if (cause is CancellationException) throw cause
      reportError(cause)
      emit(emptyList())
    }

  private val activeChatMessagesFlow =
    interactionState.flatMapLatest { state ->
      val partner = state.activeChatPartner
      if (partner == null) {
        flowOf(emptyList())
      } else {
        val chatId = SocialRepository.buildChatRoomId(currentUserId, partner.userId)
        repository.observeChatMessages(chatId).catch { cause ->
          if (cause is CancellationException) throw cause
          reportError(cause)
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
          snapshot.discoverableProfiles.firstOrNull { it.userId == partner.userId } ?: partner
        }
      val inspected =
        interaction.inspectedProfileId?.let { inspectedId ->
          snapshot.discoverableProfiles.firstOrNull { it.userId == inspectedId }
        }
      SocialUiState(
        currentUserId = currentUserId,
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
          myProfile = fallbackProfile,
        ),
    )

  init {
    viewModelScope.launch {
      repository
        .ensureUserBootstrap(
          selfUid = currentUserId,
          defaultHandle = defaultHandle,
          defaultDisplayName = defaultName,
        )
        .onFailure { reportError(it) }
      interactionState.update { it.copy(isBootstrapping = false) }
    }

    viewModelScope.launch {
      interactionState
        .map { it.activeChatPartner?.userId }
        .distinctUntilChanged()
        .collectLatest { partnerId ->
          if (partnerId != null) {
            val roomId = SocialRepository.buildChatRoomId(currentUserId, partnerId)
            while (isActive) {
              repository.syncRemoteRoomMessages(selfUid = currentUserId, roomId = roomId)
              delay(3_500L)
            }
          }
        }
    }
  }

  fun refreshCommunity() {
    viewModelScope.launch {
      repository.syncRemoteUsersAndBlockStatus(currentUserId)
    }
  }

  fun updateSearchQuery(query: String) {
    interactionState.update { it.copy(searchQuery = query) }
  }

  fun selectFilter(filter: CommunityFilter) {
    interactionState.update { it.copy(filter = filter) }
  }

  fun inspectProfile(profile: PublicProfile) {
    interactionState.update { it.copy(inspectedProfileId = profile.userId) }
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
    val existing = uiState.value.communityProfiles.firstOrNull { it.userId == room.partnerId }
    val partner =
      existing
        ?: PublicProfile(
          id = room.partnerId,
          userId = room.partnerId,
          handle = room.partnerHandle.ifBlank { "@SL_USER" },
          displayName = room.partnerName.ifBlank { "Shayar Friend" },
        )
    openChatWith(partner)
  }

  fun openFromNotification(notification: SocialNotification) {
    val partner =
      uiState.value.communityProfiles.firstOrNull { it.userId == notification.senderId }
        ?: PublicProfile(
          id = notification.senderId,
          userId = notification.senderId,
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
          selfUid = currentUserId,
          displayName = displayName,
          city = city,
          address = address,
          bio = bio,
        )
        .onSuccess {
          interactionState.update {
            it.copy(isBusy = false, actionFeedback = "Profile updated!")
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

  fun addFriend(
    displayName: String,
    handle: String,
    city: String,
    bio: String,
    onSuccess: () -> Unit = {},
  ) {
    if (interactionState.value.isBusy) return
    viewModelScope.launch {
      interactionState.update { it.copy(isBusy = true) }
      repository
        .addFriendProfile(displayName = displayName, rawHandle = handle, city = city, bio = bio)
        .onSuccess { friend ->
          interactionState.update {
            it.copy(isBusy = false, actionFeedback = "${friend.displayName} added to Friends!")
          }
          onSuccess()
        }
        .onFailure { err ->
          interactionState.update {
            it.copy(isBusy = false, actionFeedback = err.localizedMessage ?: "Could not add friend")
          }
        }
    }
  }

  fun toggleFollow(target: PublicProfile) {
    val state = uiState.value
    val currentlyFollowing = target.userId in state.followingIds
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
    val currentlyLiked = target.userId in state.likedProfileIds
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
    val hasUnread = messages.any { !it.isRead && it.recipientId == currentUserId }
    if (!hasUnread) return
    val chatId = SocialRepository.buildChatRoomId(currentUserId, partner.userId)
    viewModelScope.launch {
      repository.markMessagesRead(chatId = chatId, recipientId = currentUserId)
    }
  }

  fun markAllNotificationsRead() {
    if (uiState.value.socialNotifications.none { !it.isRead }) return
    viewModelScope.launch {
      repository.markNotificationsRead()
    }
  }

  fun clearActionFeedback() {
    interactionState.update { it.copy(actionFeedback = null) }
  }

  private fun reportError(cause: Throwable) {
    val msg = cause.localizedMessage ?: "Local save failed"
    interactionState.update { it.copy(actionFeedback = msg) }
  }

  companion object {
    fun factory(
      repository: SocialRepository,
      currentUserId: String,
      defaultHandle: String,
      defaultName: String,
    ): ViewModelProvider.Factory = viewModelFactory {
      initializer {
        SocialViewModel(
          repository = repository,
          currentUserId = currentUserId,
          defaultHandle = defaultHandle,
          defaultName = defaultName,
        )
      }
    }
  }
}
