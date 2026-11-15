package com.example.data

import android.content.Context
import com.example.R
import com.example.model.ChatMessage
import com.example.model.ChatRoom
import com.example.model.PublicProfile
import com.example.model.SocialNotification
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class SocialRepository(private val db: FirebaseFirestore) {

  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private val auth = Firebase.auth

  private fun requireUserId(): String =
    auth.currentUser?.uid
      ?: throw IllegalStateException("User must sign in with Google before accessing Firestore.")

  fun observeMyProfile(selfUid: String): Flow<PublicProfile?> = flow {
    requireUserId()
    val docRef = db.collection("appUsers").document(selfUid)
    emitAll(
      docRef
        .snapshots()
        .map { snapshot ->
          if (!snapshot.exists()) null else snapshot.toPublicProfile()
        }
        .catch { cause ->
          if (cause is CancellationException) throw cause
          handleFirestoreError(cause, OperationType.GET, docRef.path)
          throw cause
        }
    )
  }

  fun observeDiscoverableProfiles(): Flow<List<PublicProfile>> = flow {
    requireUserId()
    val path = "appUsers"
    emitAll(
      db.collection(path)
        .limit(100)
        .snapshots()
        .map { snapshot ->
          snapshot.documents
            .mapNotNull { it.toPublicProfile() }
            .sortedByDescending { it.updatedAtMillis }
        }
        .catch { cause ->
          if (cause is CancellationException) throw cause
          handleFirestoreError(cause, OperationType.LIST, path)
          throw cause
        }
    )
  }

  fun observeMyFollowingIds(selfUid: String): Flow<Set<String>> = flow {
    requireUserId()
    val path = "appUsers/$selfUid/following"
    emitAll(
      db.collection("appUsers")
        .document(selfUid)
        .collection("following")
        .snapshots()
        .map { snapshot -> snapshot.documents.map { it.id }.toSet() }
        .catch { cause ->
          if (cause is CancellationException) throw cause
          handleFirestoreError(cause, OperationType.LIST, path)
          throw cause
        }
    )
  }

  fun observeMyLikedProfileIds(selfUid: String): Flow<Set<String>> = flow {
    requireUserId()
    val path = "appUsers/$selfUid/likes"
    emitAll(
      db.collection("appUsers")
        .document(selfUid)
        .collection("likes")
        .snapshots()
        .map { snapshot -> snapshot.documents.map { it.id }.toSet() }
        .catch { cause ->
          if (cause is CancellationException) throw cause
          handleFirestoreError(cause, OperationType.LIST, path)
          throw cause
        }
    )
  }

  fun observeMyChats(selfUid: String): Flow<List<ChatRoom>> = flow {
    requireUserId()
    val path = "chatRooms"
    emitAll(
      db.collection(path)
        .whereArrayContains("participantIds", selfUid)
        .snapshots()
        .map { snapshot ->
          snapshot.documents
            .map { doc -> doc.toChatRoom(selfUid) }
            .sortedByDescending { it.updatedAtMillis }
        }
        .catch { cause ->
          if (cause is CancellationException) throw cause
          handleFirestoreError(cause, OperationType.LIST, path)
          throw cause
        }
    )
  }

  fun observeChatMessages(chatId: String): Flow<List<ChatMessage>> = flow {
    requireUserId()
    val path = "chatRooms/$chatId/messages"
    emitAll(
      db.collection("chatRooms")
        .document(chatId)
        .collection("messages")
        .orderBy("createdAt", Query.Direction.ASCENDING)
        .limit(150)
        .snapshots()
        .map { snapshot ->
          snapshot.documents.mapNotNull { it.toChatMessage(chatId) }
        }
        .catch { cause ->
          if (cause is CancellationException) throw cause
          handleFirestoreError(cause, OperationType.LIST, path)
          throw cause
        }
    )
  }

  fun observeSocialNotifications(selfUid: String): Flow<List<SocialNotification>> = flow {
    requireUserId()
    val path = "appUsers/$selfUid/notifications"
    emitAll(
      db.collection("appUsers")
        .document(selfUid)
        .collection("notifications")
        .orderBy("createdAt", Query.Direction.DESCENDING)
        .limit(50)
        .snapshots()
        .map { snapshot ->
          snapshot.documents.map { it.toSocialNotification(selfUid) }
        }
        .catch { cause ->
          if (cause is CancellationException) throw cause
          handleFirestoreError(cause, OperationType.LIST, path)
          throw cause
        }
    )
  }

  suspend fun syncGoogleUserOnLogin(
    defaultHandle: String,
    defaultDisplayName: String,
    email: String,
    photoUrl: String,
  ): Result<PublicProfile> {
    val uid =
      try {
        requireUserId()
      } catch (e: Exception) {
        return Result.failure(e)
      }

    val safeHandle = defaultHandle.trim().ifBlank { "@SL_${uid.takeLast(4).uppercase()}" }.take(32)
    val cleanUsername = safeHandle.removePrefix("@").ifBlank { "SL_${uid.takeLast(4).uppercase()}" }
    val safeName = defaultDisplayName.trim().ifBlank { "Shayari Lover" }.take(60)
    val safeEmail = email.trim().take(160)
    val safePhoto = photoUrl.trim().take(500)

    val privateUserRef = db.collection("users").document(uid)
    val publicUserRef = db.collection("appUsers").document(uid)

    return try {
      val existingPrivate = privateUserRef.get().await()
      val privatePayload =
        buildMap<String, Any> {
          put("uid", uid)
          put("displayName", safeName)
          put("email", safeEmail)
          if (safePhoto.isNotEmpty()) put("photoUrl", safePhoto)
          put("handle", safeHandle)
          if (!existingPrivate.exists()) {
            put("createdAt", FieldValue.serverTimestamp())
          }
          put("updatedAt", FieldValue.serverTimestamp())
        }
      privateUserRef.set(privatePayload, SetOptions.merge()).await()

      val existingPublic = publicUserRef.get().await()
      val publicPayload =
        if (!existingPublic.exists()) {
          buildMap<String, Any> {
            put("uid", uid)
            put("userId", uid)
            put("displayName", safeName)
            put("username", cleanUsername)
            put("handle", safeHandle)
            if (safePhoto.isNotEmpty()) put("photoUrl", safePhoto)
            put("city", "")
            put("bio", "Shayari Lok Member")
            put("isOnline", true)
            put("isBlocked", false)
            put("blockReason", "")
            put("followersCount", 0L)
            put("followingCount", 0L)
            put("likesReceivedCount", 0L)
            put("createdAt", FieldValue.serverTimestamp())
            put("updatedAt", FieldValue.serverTimestamp())
          }
        } else {
          buildMap<String, Any> {
            val currentName = existingPublic.getString("displayName").orEmpty()
            if (currentName.isBlank()) {
              put("displayName", safeName)
            }
            if (safePhoto.isNotEmpty()) put("photoUrl", safePhoto)
            put("isOnline", true)
            put("updatedAt", FieldValue.serverTimestamp())
          }
        }
      publicUserRef.set(publicPayload, SetOptions.merge()).await()

      val freshSnapshot = publicUserRef.get().await()
      val profile =
        freshSnapshot.toPublicProfile()
          ?: PublicProfile(
            id = uid,
            userId = uid,
            uid = uid,
            handle = safeHandle,
            username = cleanUsername,
            displayName = safeName,
            photoUrl = safePhoto,
          )
      Result.success(profile)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, publicUserRef.path)
      Result.failure(e)
    }
  }

  suspend fun updateMyProfile(
    displayName: String,
    city: String,
    address: String,
    bio: String,
  ): Result<Unit> {
    val uid =
      try {
        requireUserId()
      } catch (e: Exception) {
        return Result.failure(e)
      }
    val cleanName = displayName.trim().take(60)
    if (cleanName.isBlank()) {
      return Result.failure(IllegalArgumentException("Name cannot be empty"))
    }
    val publicRef = db.collection("appUsers").document(uid)
    val privateRef = db.collection("users").document(uid)

    return try {
      val batch = db.batch()
      batch.set(
        publicRef,
        mapOf(
          "displayName" to cleanName,
          "city" to city.trim().take(80),
          "bio" to bio.trim().take(220),
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
        SetOptions.merge(),
      )
      batch.set(
        privateRef,
        mapOf(
          "displayName" to cleanName,
          "address" to address.trim().take(160),
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
        SetOptions.merge(),
      )
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, publicRef.path)
      Result.failure(e)
    }
  }

  suspend fun toggleFollow(
    selfProfile: PublicProfile,
    targetProfile: PublicProfile,
    currentlyFollowing: Boolean,
  ): Result<Boolean> {
    val uid =
      try {
        requireUserId()
      } catch (e: Exception) {
        return Result.failure(e)
      }
    val targetUid = targetProfile.effectiveUid
    if (uid == targetUid || targetUid.isBlank()) {
      return Result.failure(IllegalArgumentException("Cannot follow this profile"))
    }

    val followSubDoc = db.collection("appUsers").document(uid).collection("following").document(targetUid)
    val edgeId = "${uid}_follow_${targetUid}"
    val edgeDoc = db.collection("socialEdges").document(edgeId)
    val selfDoc = db.collection("appUsers").document(uid)
    val targetDoc = db.collection("appUsers").document(targetUid)

    return try {
      val nextState = !currentlyFollowing
      val delta = if (nextState) 1L else -1L
      val batch = db.batch()

      if (nextState) {
        batch.set(
          followSubDoc,
          mapOf(
            "targetId" to targetUid,
            "targetName" to targetProfile.displayName.take(100),
            "targetHandle" to targetProfile.effectiveHandle.take(40),
            "createdAt" to FieldValue.serverTimestamp(),
          ),
        )
        batch.set(
          edgeDoc,
          mapOf(
            "actorId" to uid,
            "targetId" to targetUid,
            "actorName" to selfProfile.displayName.take(100),
            "targetName" to targetProfile.displayName.take(100),
            "type" to "follow",
            "createdAt" to FieldValue.serverTimestamp(),
          ),
        )
        val notifDoc = db.collection("appUsers").document(targetUid).collection("notifications").document()
        batch.set(
          notifDoc,
          mapOf(
            "recipientId" to targetUid,
            "senderId" to uid,
            "senderHandle" to selfProfile.effectiveHandle.take(40),
            "senderName" to selfProfile.displayName.take(100),
            "type" to "follow",
            "previewText" to "${selfProfile.displayName} started following you",
            "chatId" to "",
            "isRead" to false,
            "createdAt" to FieldValue.serverTimestamp(),
          ),
        )
      } else {
        batch.delete(followSubDoc)
        batch.delete(edgeDoc)
      }

      batch.update(selfDoc, mapOf("followingCount" to FieldValue.increment(delta), "updatedAt" to FieldValue.serverTimestamp()))
      batch.update(targetDoc, mapOf("followersCount" to FieldValue.increment(delta), "updatedAt" to FieldValue.serverTimestamp()))
      batch.commit().await()
      Result.success(nextState)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, edgeDoc.path)
      Result.failure(e)
    }
  }

  suspend fun toggleProfileLike(
    selfProfile: PublicProfile,
    targetProfile: PublicProfile,
    currentlyLiked: Boolean,
  ): Result<Boolean> {
    val uid =
      try {
        requireUserId()
      } catch (e: Exception) {
        return Result.failure(e)
      }
    val targetUid = targetProfile.effectiveUid
    if (uid == targetUid || targetUid.isBlank()) {
      return Result.failure(IllegalArgumentException("Cannot like this profile"))
    }

    val likeSubDoc = db.collection("appUsers").document(uid).collection("likes").document(targetUid)
    val edgeId = "${uid}_like_${targetUid}"
    val edgeDoc = db.collection("socialEdges").document(edgeId)
    val targetDoc = db.collection("appUsers").document(targetUid)

    return try {
      val nextState = !currentlyLiked
      val delta = if (nextState) 1L else -1L
      val batch = db.batch()

      if (nextState) {
        batch.set(
          likeSubDoc,
          mapOf(
            "targetId" to targetUid,
            "createdAt" to FieldValue.serverTimestamp(),
          ),
        )
        batch.set(
          edgeDoc,
          mapOf(
            "actorId" to uid,
            "targetId" to targetUid,
            "actorName" to selfProfile.displayName.take(100),
            "targetName" to targetProfile.displayName.take(100),
            "type" to "like",
            "createdAt" to FieldValue.serverTimestamp(),
          ),
        )
        val notifDoc = db.collection("appUsers").document(targetUid).collection("notifications").document()
        batch.set(
          notifDoc,
          mapOf(
            "recipientId" to targetUid,
            "senderId" to uid,
            "senderHandle" to selfProfile.effectiveHandle.take(40),
            "senderName" to selfProfile.displayName.take(100),
            "type" to "like",
            "previewText" to "${selfProfile.displayName} liked your Shayari profile ❤️",
            "chatId" to "",
            "isRead" to false,
            "createdAt" to FieldValue.serverTimestamp(),
          ),
        )
      } else {
        batch.delete(likeSubDoc)
        batch.delete(edgeDoc)
      }

      batch.update(
        targetDoc,
        mapOf(
          "likesReceivedCount" to FieldValue.increment(delta),
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      )
      batch.commit().await()
      Result.success(nextState)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, edgeDoc.path)
      Result.failure(e)
    }
  }

  suspend fun sendChatMessage(
    selfProfile: PublicProfile,
    targetProfile: PublicProfile,
    rawText: String,
  ): Result<String> {
    val uid =
      try {
        requireUserId()
      } catch (e: Exception) {
        return Result.failure(e)
      }
    if (selfProfile.isBlocked) {
      val reason = selfProfile.blockReason.ifBlank { "Messaging suspended by Admin moderation" }
      return Result.failure(IllegalStateException(reason))
    }
    val cleanText = rawText.trim().take(1000)
    if (cleanText.isBlank()) {
      return Result.failure(IllegalArgumentException("Message cannot be empty"))
    }
    val targetUid = targetProfile.effectiveUid
    val roomId = buildChatRoomId(uid, targetUid)
    val roomRef = db.collection("chatRooms").document(roomId)
    val msgRef = roomRef.collection("messages").document()
    val notifRef = db.collection("appUsers").document(targetUid).collection("notifications").document()

    return try {
      val batch = db.batch()
      batch.set(
        roomRef,
        mapOf(
          "participantIds" to listOf(uid, targetUid).sorted(),
          "participantNames" to
            listOf(selfProfile.displayName.take(100), targetProfile.displayName.take(100)),
          "participantHandles" to
            mapOf(uid to selfProfile.effectiveHandle, targetUid to targetProfile.effectiveHandle),
          "participantDisplayNames" to
            mapOf(uid to selfProfile.displayName, targetUid to targetProfile.displayName),
          "lastMessage" to cleanText.take(500),
          "lastSenderId" to uid,
          "messageCount" to FieldValue.increment(1L),
          "flaggedSpam" to false,
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
        SetOptions.merge(),
      )
      batch.set(
        msgRef,
        mapOf(
          "roomId" to roomId,
          "senderId" to uid,
          "senderName" to selfProfile.displayName.take(100),
          "senderHandle" to selfProfile.effectiveHandle.take(40),
          "receiverId" to targetUid,
          "text" to cleanText,
          "isRead" to false,
          "isFlagged" to false,
          "createdAt" to FieldValue.serverTimestamp(),
        ),
      )
      batch.set(
        notifRef,
        mapOf(
          "recipientId" to targetUid,
          "senderId" to uid,
          "senderHandle" to selfProfile.effectiveHandle.take(40),
          "senderName" to selfProfile.displayName.take(100),
          "type" to "message",
          "previewText" to cleanText.take(140),
          "chatId" to roomId,
          "isRead" to false,
          "createdAt" to FieldValue.serverTimestamp(),
        ),
      )
      batch.commit().await()
      Result.success(msgRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, msgRef.path)
      Result.failure(e)
    }
  }

  suspend fun markMessagesRead(chatId: String, unreadMessages: List<ChatMessage>): Result<Unit> {
    val uid =
      try {
        requireUserId()
      } catch (e: Exception) {
        return Result.failure(e)
      }
    val toUpdate = unreadMessages.filter { !it.isRead && it.effectiveRecipientId == uid && it.id.isNotBlank() }
    if (toUpdate.isEmpty()) return Result.success(Unit)
    val roomRef = db.collection("chatRooms").document(chatId)
    return try {
      val batch = db.batch()
      toUpdate.forEach { msg ->
        batch.update(roomRef.collection("messages").document(msg.id), "isRead", true)
      }
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, "${roomRef.path}/messages")
      Result.failure(e)
    }
  }

  suspend fun markNotificationsRead(selfUid: String, unread: List<SocialNotification>): Result<Unit> {
    val uid =
      try {
        requireUserId()
      } catch (e: Exception) {
        return Result.failure(e)
      }
    if (uid != selfUid) return Result.success(Unit)
    val toMark = unread.filter { !it.isRead && it.id.isNotBlank() }
    if (toMark.isEmpty()) return Result.success(Unit)
    val colRef = db.collection("appUsers").document(uid).collection("notifications")
    return try {
      val batch = db.batch()
      toMark.forEach { notif ->
        batch.update(colRef.document(notif.id), "isRead", true)
      }
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, colRef.path)
      Result.failure(e)
    }
  }

  private fun DocumentSnapshot.toPublicProfile(): PublicProfile? {
    val uidVal = getString("uid").orEmpty().ifBlank { getString("userId").orEmpty() }.ifBlank { id }
    if (uidVal.isBlank()) return null
    val rawHandle = getString("handle").orEmpty()
    val rawUsername = getString("username").orEmpty()
    val resolvedHandle =
      when {
        rawHandle.isNotBlank() -> rawHandle
        rawUsername.startsWith("@") -> rawUsername
        rawUsername.isNotBlank() -> "@$rawUsername"
        else -> "@SL_${uidVal.takeLast(4).uppercase()}"
      }
    val likesVal =
      maxOf(
        getLong("likesReceivedCount") ?: 0L,
        getLong("likesCount") ?: 0L,
      )
    val createdTs =
      getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
    val updatedTs =
      getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)

    return PublicProfile(
      id = id,
      userId = uidVal,
      uid = uidVal,
      handle = resolvedHandle,
      username = rawUsername.ifBlank { resolvedHandle.removePrefix("@") },
      displayName = getString("displayName").orEmpty().ifBlank { "Shayari Lover" },
      photoUrl = getString("photoUrl").orEmpty(),
      city = getString("city").orEmpty(),
      address = getString("address").orEmpty(),
      bio = getString("bio").orEmpty(),
      followersCount = getLong("followersCount") ?: 0L,
      followingCount = getLong("followingCount") ?: 0L,
      likesCount = likesVal,
      likesReceivedCount = likesVal,
      isOnline = getBoolean("isOnline") ?: true,
      isBlocked = getBoolean("isBlocked") ?: false,
      blockReason = getString("blockReason").orEmpty(),
      createdAt = createdTs,
      updatedAt = updatedTs,
      createdAtMillis = createdTs?.toDate()?.time ?: 0L,
      updatedAtMillis = updatedTs?.toDate()?.time ?: 0L,
    )
  }

  private fun DocumentSnapshot.toChatRoom(selfUid: String): ChatRoom {
    val participants =
      (get("participantIds") as? List<*>)?.mapNotNull { it as? String }.orEmpty()
    val partnerId = participants.firstOrNull { it != selfUid } ?: getString("partnerId").orEmpty()
    val namesMap = (get("participantDisplayNames") as? Map<*, *>)
    val handlesMap = (get("participantHandles") as? Map<*, *>)
    val partnerName =
      (namesMap?.get(partnerId) as? String)
        ?: getString("partnerName").orEmpty().ifBlank { "Shayar Friend" }
    val partnerHandle =
      (handlesMap?.get(partnerId) as? String)
        ?: getString("partnerHandle").orEmpty().ifBlank { "@SL_${partnerId.takeLast(4).uppercase()}" }
    val updatedTs =
      getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)

    return ChatRoom(
      id = id,
      participantIds = participants,
      partnerId = partnerId,
      partnerHandle = partnerHandle,
      partnerName = partnerName,
      lastMessage = getString("lastMessage").orEmpty(),
      lastSenderId = getString("lastSenderId").orEmpty(),
      messageCount = getLong("messageCount") ?: 0L,
      flaggedSpam = getBoolean("flaggedSpam") ?: false,
      updatedAt = updatedTs,
      updatedAtMillis = updatedTs?.toDate()?.time ?: 0L,
    )
  }

  private fun DocumentSnapshot.toChatMessage(roomId: String): ChatMessage? {
    val text = getString("text").orEmpty()
    val senderId = getString("senderId").orEmpty()
    if (text.isBlank() || senderId.isBlank()) return null
    val receiverId = getString("receiverId").orEmpty().ifBlank { getString("recipientId").orEmpty() }
    val createdTs =
      getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)

    return ChatMessage(
      id = id,
      chatId = roomId,
      roomId = getString("roomId").orEmpty().ifBlank { roomId },
      senderId = senderId,
      senderName = getString("senderName").orEmpty(),
      senderHandle = getString("senderHandle").orEmpty(),
      recipientId = receiverId,
      receiverId = receiverId,
      text = text,
      isRead = getBoolean("isRead") ?: false,
      isFlagged = getBoolean("isFlagged") ?: false,
      createdAt = createdTs,
      createdAtMillis = createdTs?.toDate()?.time ?: 0L,
    )
  }

  private fun DocumentSnapshot.toSocialNotification(selfUid: String): SocialNotification {
    val createdTs =
      getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
    return SocialNotification(
      id = id,
      recipientId = getString("recipientId").orEmpty().ifBlank { selfUid },
      senderId = getString("senderId").orEmpty(),
      senderHandle = getString("senderHandle").orEmpty(),
      senderName = getString("senderName").orEmpty().ifBlank { "Shayari Lok" },
      type = getString("type").orEmpty().ifBlank { "message" },
      previewText = getString("previewText").orEmpty(),
      chatId = getString("chatId").orEmpty(),
      isRead = getBoolean("isRead") ?: false,
      createdAt = createdTs,
      createdAtMillis = createdTs?.toDate()?.time ?: 0L,
    )
  }

  companion object {
    fun buildChatRoomId(uidA: String, uidB: String): String =
      "room_" + listOf(uidA, uidB).sorted().joinToString("_")

    fun buildHandleFromUid(uid: String): String {
      val hash = uid.hashCode().let { if (it == Int.MIN_VALUE) 0 else kotlin.math.abs(it) }
      val fourDigits = (1000 + (hash % 9000)).toString()
      return "@SL_$fourDigits"
    }
  }
}
