package com.example.data

import android.content.Context
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ChatRoomEntity
import com.example.data.local.SocialDao
import com.example.data.local.SocialDatabase
import com.example.data.local.SocialNotificationEntity
import com.example.data.local.UserProfileEntity
import com.example.data.remote.FirestoreRestClient
import com.example.model.ChatMessage
import com.example.model.ChatRoom
import com.example.model.PublicProfile
import com.example.model.SocialNotification
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SocialRepository(
  private val dao: SocialDao,
  private val restClient: FirestoreRestClient = FirestoreRestClient(),
) {

  constructor(context: Context) : this(
    dao = SocialDatabase.getInstance(context).socialDao(),
    restClient = FirestoreRestClient(),
  )

  fun observeMyProfile(selfUid: String): Flow<PublicProfile?> =
    dao.observeSelfProfile(selfUid).map { it?.toModel() }

  fun observeDiscoverableProfiles(): Flow<List<PublicProfile>> =
    dao.observeCommunityProfiles().map { list -> list.map { it.toModel() } }

  fun observeMyFollowingIds(): Flow<Set<String>> =
    dao.observeCommunityProfiles().map { list ->
      list.filter { it.isFollowing }.map { it.userId }.toSet()
    }

  fun observeMyLikedProfileIds(): Flow<Set<String>> =
    dao.observeCommunityProfiles().map { list ->
      list.filter { it.isLiked }.map { it.userId }.toSet()
    }

  fun observeMyChats(): Flow<List<ChatRoom>> =
    dao.observeChatRooms().map { rooms -> rooms.map { it.toModel() } }

  fun observeChatMessages(chatId: String): Flow<List<ChatMessage>> =
    dao.observeMessages(chatId).map { msgs -> msgs.map { it.toModel() } }

  fun observeSocialNotifications(): Flow<List<SocialNotification>> =
    dao.observeNotifications().map { list -> list.map { it.toModel() } }

  suspend fun ensureUserBootstrap(
    selfUid: String,
    defaultHandle: String,
    defaultDisplayName: String,
  ): Result<PublicProfile> =
    try {
      val now = System.currentTimeMillis()
      val existingSelf = dao.getProfileById(selfUid)
      if (existingSelf == null) {
        val safeHandle = defaultHandle.trim().ifBlank { "@SL_1001" }.take(32)
        val safeName = defaultDisplayName.trim().ifBlank { "Shayari Lover" }.take(60)
        val selfEntity =
          UserProfileEntity(
            userId = selfUid,
            handle = safeHandle,
            displayName = safeName,
            city = "",
            address = "",
            bio = "Shayari Lok Member",
            followersCount = 0L,
            followingCount = 0L,
            likesCount = 0L,
            isSelf = true,
            isFollowing = false,
            isLiked = false,
            isBlocked = false,
            blockReason = "",
            createdAtMillis = now,
            updatedAtMillis = now,
          )
        dao.upsertProfile(selfEntity)
      }

      if (dao.countCommunityProfiles() == 0) {
        seedInitialCommunity(selfUid = selfUid, now = now)
      }

      val loaded =
        dao.getProfileById(selfUid)?.toModel()
          ?: PublicProfile(
            id = selfUid,
            userId = selfUid,
            handle = defaultHandle,
            displayName = defaultDisplayName,
          )

      // Push local profile directly to /appUsers via Firestore REST API (non-fatal silent fallback)
      restClient.upsertAppUser(loaded)
      syncRemoteUsersAndBlockStatus(selfUid)

      val refreshed = dao.getProfileById(selfUid)?.toModel() ?: loaded
      Result.success(refreshed)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }

  suspend fun syncRemoteUsersAndBlockStatus(selfUid: String) {
    try {
      restClient.fetchSingleAppUser(selfUid).onSuccess { remoteSelf ->
        if (remoteSelf != null) {
          dao.updateBlockStatus(
            uid = selfUid,
            isBlocked = remoteSelf.isBlocked,
            blockReason = remoteSelf.blockReason,
            updatedAt = System.currentTimeMillis(),
          )
        }
      }

      restClient.fetchAppUsers().onSuccess { remoteUsers ->
        for (remoteUser in remoteUsers) {
          if (remoteUser.userId == selfUid) continue
          val existing = dao.getProfileById(remoteUser.userId)
          val entity =
            UserProfileEntity(
              userId = remoteUser.userId,
              handle = remoteUser.handle,
              displayName = remoteUser.displayName,
              city = remoteUser.city,
              address = remoteUser.address,
              bio = remoteUser.bio,
              followersCount = maxOf(remoteUser.followersCount, existing?.followersCount ?: 0L),
              followingCount = maxOf(remoteUser.followingCount, existing?.followingCount ?: 0L),
              likesCount = maxOf(remoteUser.likesCount, existing?.likesCount ?: 0L),
              isSelf = false,
              isFollowing = existing?.isFollowing ?: false,
              isLiked = existing?.isLiked ?: false,
              isBlocked = remoteUser.isBlocked,
              blockReason = remoteUser.blockReason,
              createdAtMillis =
                remoteUser.createdAtMillis.takeIf { it > 0L }
                  ?: existing?.createdAtMillis
                  ?: System.currentTimeMillis(),
              updatedAtMillis = System.currentTimeMillis(),
            )
          dao.upsertProfile(entity)
        }
      }
    } catch (e: CancellationException) {
      throw e
    } catch (_: Exception) {
      // Keep Room local cache active seamlessly
    }
  }

  suspend fun syncRemoteRoomMessages(selfUid: String, roomId: String) {
    try {
      restClient.fetchRoomMessages(roomId).onSuccess { remoteMsgs ->
        val localExisting = dao.getRoomMessagesSnapshot(roomId)
        val existingSignatures =
          localExisting.map {
            it.remoteDocId.ifBlank { "${it.senderId}:${it.text}" }
          }.toSet()

        for (msg in remoteMsgs) {
          val docKey = msg.id
          val contentKey = "${msg.senderId}:${msg.text}"
          if (docKey !in existingSignatures && contentKey !in existingSignatures) {
            dao.insertMessage(
              ChatMessageEntity(
                remoteDocId = docKey,
                roomId = roomId,
                senderId = msg.senderId,
                senderHandle = msg.senderHandle.ifBlank { "@SL_USER" },
                recipientId = msg.recipientId,
                text = msg.text,
                isRead = msg.senderId == selfUid,
                createdAtMillis = msg.createdAtMillis,
              )
            )
          }
        }
      }
    } catch (e: CancellationException) {
      throw e
    } catch (_: Exception) {
      // Silent fallback to local SQLite
    }
  }

  private suspend fun seedInitialCommunity(selfUid: String, now: Long) {
    val seedUsers =
      listOf(
        UserProfileEntity(
          userId = "shayar_aarav_1024",
          handle = "@SL_1024",
          displayName = "Aarav Verma",
          city = "Lucknow",
          address = "Hazratganj, Lucknow",
          bio = "दिल के जज़्बात और मोहब्बत की शायरी लिखता हूँ ✨",
          followersCount = 128L,
          followingCount = 34L,
          likesCount = 240L,
          isSelf = false,
          isFollowing = false,
          isLiked = false,
          createdAtMillis = now - 60_000L * 50,
          updatedAtMillis = now - 60_000L * 50,
        ),
        UserProfileEntity(
          userId = "shayar_ananya_2048",
          handle = "@SL_2048",
          displayName = "Ananya Sharma",
          city = "Jaipur",
          address = "Malviya Nagar, Jaipur",
          bio = "ख़ामोश लफ़्ज़ों में गहरी बातें लिखने का शौक़ 🌹",
          followersCount = 96L,
          followingCount = 21L,
          likesCount = 185L,
          isSelf = false,
          isFollowing = false,
          isLiked = false,
          createdAtMillis = now - 60_000L * 40,
          updatedAtMillis = now - 60_000L * 40,
        ),
        UserProfileEntity(
          userId = "shayar_kabir_3096",
          handle = "@SL_3096",
          displayName = "Kabir Khan",
          city = "Delhi",
          address = "Connaught Place, New Delhi",
          bio = "दोस्ती, मोटिवेशन और शायरी — हर शाम एक नया शेर 🔥",
          followersCount = 154L,
          followingCount = 48L,
          likesCount = 310L,
          isSelf = false,
          isFollowing = false,
          isLiked = false,
          createdAtMillis = now - 60_000L * 30,
          updatedAtMillis = now - 60_000L * 30,
        ),
        UserProfileEntity(
          userId = "shayar_meera_4112",
          handle = "@SL_4112",
          displayName = "Meera Joshi",
          city = "Indore",
          address = "Vijay Nagar, Indore",
          bio = "इश्क़, यादें और बरसात की कविताएँ 💫",
          followersCount = 72L,
          followingCount = 19L,
          likesCount = 142L,
          isSelf = false,
          isFollowing = false,
          isLiked = false,
          createdAtMillis = now - 60_000L * 20,
          updatedAtMillis = now - 60_000L * 20,
        ),
        UserProfileEntity(
          userId = "shayar_zoya_5220",
          handle = "@SL_5220",
          displayName = "Zoya Mirza",
          city = "Bhopal",
          address = "Arera Colony, Bhopal",
          bio = "अल्फ़ाज़ जो सीधे दिल को छू जाएं ❤️",
          followersCount = 112L,
          followingCount = 29L,
          likesCount = 215L,
          isSelf = false,
          isFollowing = false,
          isLiked = false,
          createdAtMillis = now - 60_000L * 10,
          updatedAtMillis = now - 60_000L * 10,
        ),
      )
    dao.insertProfilesIgnore(seedUsers)

    val welcomeRoomId = buildChatRoomId(selfUid, "shayar_aarav_1024")
    val welcomeMessage = "Shayari Lok में स्वागत है दोस्त! आपकी पसंदीदा शायरी कौनसी है? ✨"
    dao.upsertChatRoom(
      ChatRoomEntity(
        roomId = welcomeRoomId,
        partnerId = "shayar_aarav_1024",
        partnerHandle = "@SL_1024",
        partnerName = "Aarav Verma",
        lastMessage = welcomeMessage,
        lastSenderId = "shayar_aarav_1024",
        updatedAtMillis = now - 120_000L,
      )
    )
    dao.insertMessage(
      ChatMessageEntity(
        remoteDocId = "welcome_${welcomeRoomId}",
        roomId = welcomeRoomId,
        senderId = "shayar_aarav_1024",
        senderHandle = "@SL_1024",
        recipientId = selfUid,
        text = welcomeMessage,
        isRead = false,
        createdAtMillis = now - 120_000L,
      )
    )
    dao.insertNotification(
      SocialNotificationEntity(
        recipientId = selfUid,
        senderId = "shayar_aarav_1024",
        senderHandle = "@SL_1024",
        senderName = "Aarav Verma",
        type = "message",
        previewText = welcomeMessage,
        chatRoomId = welcomeRoomId,
        isRead = false,
        createdAtMillis = now - 120_000L,
      )
    )
  }

  suspend fun updateMyProfile(
    selfUid: String,
    displayName: String,
    city: String,
    address: String,
    bio: String,
  ): Result<Unit> {
    val cleanName = displayName.trim().take(60)
    if (cleanName.isBlank()) {
      return Result.failure(IllegalArgumentException("Name cannot be empty"))
    }
    return try {
      dao.updateSelfProfile(
        uid = selfUid,
        displayName = cleanName,
        city = city.trim().take(80),
        address = address.trim().take(160),
        bio = bio.trim().take(220),
        updatedAt = System.currentTimeMillis(),
      )
      dao.getProfileById(selfUid)?.toModel()?.let { updated ->
        restClient.upsertAppUser(updated)
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun addFriendProfile(
    displayName: String,
    rawHandle: String,
    city: String,
    bio: String,
  ): Result<PublicProfile> {
    val cleanName = displayName.trim().take(60)
    if (cleanName.isBlank()) {
      return Result.failure(IllegalArgumentException("Friend name cannot be empty"))
    }
    val digits = rawHandle.filter { it.isDigit() }.ifBlank { (6000..9999).random().toString() }
    val normalizedHandle = "@SL_${digits.take(6)}"
    val uid =
      "local_friend_${normalizedHandle.lowercase().replace("@", "")}_${System.currentTimeMillis() % 10000}"
    val now = System.currentTimeMillis()
    val entity =
      UserProfileEntity(
        userId = uid,
        handle = normalizedHandle,
        displayName = cleanName,
        city = city.trim().take(80),
        address = city.trim().take(160),
        bio = bio.trim().ifBlank { "Shayari Lok Friend" }.take(220),
        followersCount = 1L,
        followingCount = 0L,
        likesCount = 0L,
        isSelf = false,
        isFollowing = false,
        isLiked = false,
        createdAtMillis = now,
        updatedAtMillis = now,
      )
    return try {
      dao.upsertProfile(entity)
      val model = entity.toModel()
      restClient.upsertAppUser(model)
      Result.success(model)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun toggleFollow(
    selfProfile: PublicProfile,
    targetProfile: PublicProfile,
    currentlyFollowing: Boolean,
  ): Result<Boolean> {
    if (selfProfile.userId == targetProfile.userId) {
      return Result.failure(IllegalArgumentException("Cannot follow yourself"))
    }
    return try {
      val now = System.currentTimeMillis()
      val nextState = !currentlyFollowing
      val delta = if (nextState) 1L else -1L
      dao.updateTargetFollowing(
        targetUid = targetProfile.userId,
        isFollowing = nextState,
        followersDelta = delta,
        updatedAt = now,
      )
      dao.adjustSelfFollowingCount(
        selfUid = selfProfile.userId,
        delta = delta,
        updatedAt = now,
      )
      if (nextState) {
        restClient.upsertSocialEdge(
          actorId = selfProfile.userId,
          targetId = targetProfile.userId,
          actorName = selfProfile.displayName,
          targetName = targetProfile.displayName,
          type = "follow",
        )
      } else {
        restClient.deleteSocialEdge(
          actorId = selfProfile.userId,
          targetId = targetProfile.userId,
          type = "follow",
        )
      }
      dao.getProfileById(selfProfile.userId)?.toModel()?.let { restClient.upsertAppUser(it) }
      dao.getProfileById(targetProfile.userId)?.toModel()?.let { restClient.upsertAppUser(it) }
      Result.success(nextState)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun toggleProfileLike(
    selfProfile: PublicProfile,
    targetProfile: PublicProfile,
    currentlyLiked: Boolean,
  ): Result<Boolean> {
    if (selfProfile.userId == targetProfile.userId) {
      return Result.failure(IllegalArgumentException("Cannot like your own profile"))
    }
    return try {
      val now = System.currentTimeMillis()
      val nextState = !currentlyLiked
      val delta = if (nextState) 1L else -1L
      dao.updateTargetLike(
        targetUid = targetProfile.userId,
        isLiked = nextState,
        likesDelta = delta,
        updatedAt = now,
      )
      if (nextState) {
        restClient.upsertSocialEdge(
          actorId = selfProfile.userId,
          targetId = targetProfile.userId,
          actorName = selfProfile.displayName,
          targetName = targetProfile.displayName,
          type = "like",
        )
      } else {
        restClient.deleteSocialEdge(
          actorId = selfProfile.userId,
          targetId = targetProfile.userId,
          type = "like",
        )
      }
      dao.getProfileById(targetProfile.userId)?.toModel()?.let { restClient.upsertAppUser(it) }
      Result.success(nextState)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun sendChatMessage(
    selfProfile: PublicProfile,
    targetProfile: PublicProfile,
    rawText: String,
  ): Result<String> {
    if (selfProfile.isBlocked) {
      val reason = selfProfile.blockReason.ifBlank { "Messaging suspended by Admin moderation" }
      return Result.failure(IllegalStateException(reason))
    }
    val cleanText = rawText.trim().take(1000)
    if (cleanText.isBlank()) {
      return Result.failure(IllegalArgumentException("Message cannot be empty"))
    }
    val chatId = buildChatRoomId(selfProfile.userId, targetProfile.userId)
    val now = System.currentTimeMillis()
    return try {
      dao.upsertChatRoom(
        ChatRoomEntity(
          roomId = chatId,
          partnerId = targetProfile.userId,
          partnerHandle = targetProfile.handle,
          partnerName = targetProfile.displayName,
          lastMessage = cleanText.take(500),
          lastSenderId = selfProfile.userId,
          updatedAtMillis = now,
        )
      )
      val insertedId =
        dao.insertMessage(
          ChatMessageEntity(
            remoteDocId = "local_${now}",
            roomId = chatId,
            senderId = selfProfile.userId,
            senderHandle = selfProfile.handle,
            recipientId = targetProfile.userId,
            text = cleanText,
            isRead = true,
            createdAtMillis = now,
          )
        )
      val totalCount = dao.countRoomMessages(chatId).toLong()
      restClient.sendChatMessage(
        roomId = chatId,
        senderProfile = selfProfile,
        receiverProfile = targetProfile,
        text = cleanText,
        messageCountEstimate = totalCount,
      )
      Result.success(insertedId.toString())
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun markMessagesRead(chatId: String, recipientId: String): Result<Unit> =
    try {
      dao.markRoomMessagesRead(roomId = chatId, recipientId = recipientId)
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }

  suspend fun markNotificationsRead(): Result<Unit> =
    try {
      dao.markAllNotificationsRead()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }

  private fun UserProfileEntity.toModel(): PublicProfile =
    PublicProfile(
      id = userId,
      userId = userId,
      handle = handle,
      displayName = displayName,
      city = city,
      address = address,
      bio = bio,
      followersCount = followersCount,
      followingCount = followingCount,
      likesCount = likesCount,
      isFollowing = isFollowing,
      isLiked = isLiked,
      isBlocked = isBlocked,
      blockReason = blockReason,
      createdAtMillis = createdAtMillis,
      updatedAtMillis = updatedAtMillis,
    )

  private fun ChatRoomEntity.toModel(): ChatRoom =
    ChatRoom(
      id = roomId,
      partnerId = partnerId,
      partnerHandle = partnerHandle,
      partnerName = partnerName,
      lastMessage = lastMessage,
      lastSenderId = lastSenderId,
      updatedAtMillis = updatedAtMillis,
    )

  private fun ChatMessageEntity.toModel(): ChatMessage =
    ChatMessage(
      id = id.toString(),
      chatId = roomId,
      senderId = senderId,
      senderHandle = senderHandle,
      recipientId = recipientId,
      text = text,
      isRead = isRead,
      createdAtMillis = createdAtMillis,
    )

  private fun SocialNotificationEntity.toModel(): SocialNotification =
    SocialNotification(
      id = id.toString(),
      recipientId = recipientId,
      senderId = senderId,
      senderHandle = senderHandle,
      senderName = senderName,
      type = type,
      previewText = previewText,
      chatId = chatRoomId,
      isRead = isRead,
      createdAtMillis = createdAtMillis,
    )

  companion object {
    fun buildChatRoomId(uidA: String, uidB: String): String =
      "room_" + listOf(uidA, uidB).sorted().joinToString("_")
  }
}
