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
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
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
    val publicDocRef = db.collection("appUsers").document(selfUid)
    val altPublicDocRef = db.collection("appusers").document(selfUid)
    val privateDocRef = db.collection("users").document(selfUid)
    val publicFlow =
      publicDocRef.snapshots().map { snapshot ->
        if (!snapshot.exists()) null else snapshot.toPublicProfile()
      }
    val altPublicFlow =
      altPublicDocRef
        .snapshots()
        .map { snapshot -> if (!snapshot.exists()) null else snapshot.toPublicProfile() }
        .catch { cause ->
          val firestoreErr =
            generateSequence(cause) { it.cause }
              .filterIsInstance<FirebaseFirestoreException>()
              .firstOrNull()
          if (firestoreErr == null && cause is CancellationException) throw cause
          emit(null)
        }
    val privateFlow =
      privateDocRef
        .snapshots()
        .map { snapshot -> if (!snapshot.exists()) null else snapshot.toPublicProfile() }
        .catch { cause ->
          val firestoreErr =
            generateSequence(cause) { it.cause }
              .filterIsInstance<FirebaseFirestoreException>()
              .firstOrNull()
          if (firestoreErr == null && cause is CancellationException) throw cause
          emit(null)
        }
    emitAll(
      kotlinx.coroutines.flow
        .combine(publicFlow, altPublicFlow, privateFlow) { publicProf, altProf, privateProf ->
          val basePublic = publicProf ?: altProf
          when {
            basePublic != null && privateProf != null ->
              basePublic.copy(
                displayName = basePublic.displayName.ifBlank { privateProf.displayName },
                photoUrl = basePublic.photoUrl.ifBlank { privateProf.photoUrl },
                handle = basePublic.handle.ifBlank { privateProf.handle },
                city = basePublic.city.ifBlank { privateProf.city },
                address = basePublic.address.ifBlank { privateProf.address },
              )
            basePublic != null -> basePublic
            else -> privateProf
          }
        }
        .catch { cause ->
          val firestoreErr =
            generateSequence(cause) { it.cause }
              .filterIsInstance<FirebaseFirestoreException>()
              .firstOrNull()
          if (firestoreErr == null && cause is CancellationException) throw cause
          handleFirestoreError(firestoreErr ?: cause, OperationType.GET, publicDocRef.path)
          throw cause
        }
    )
  }

  fun observeDiscoverableProfiles(): Flow<List<PublicProfile>> = flow {
    requireUserId()
    val path = "appUsers"
    val primaryFlow =
      db.collection(path)
        .limit(200)
        .snapshots()
        .map { snapshot -> snapshot.documents.mapNotNull { it.toPublicProfile() } }
    val lowercaseFlow =
      db.collection("appusers")
        .limit(200)
        .snapshots()
        .map { snapshot -> snapshot.documents.mapNotNull { it.toPublicProfile() } }
        .catch { cause ->
          val firestoreErr =
            generateSequence(cause) { it.cause }
              .filterIsInstance<FirebaseFirestoreException>()
              .firstOrNull()
          if (firestoreErr == null && cause is CancellationException) throw cause
          emit(emptyList())
        }
    emitAll(
      kotlinx.coroutines.flow
        .combine(primaryFlow, lowercaseFlow) { primary, secondary ->
          val merged = linkedMapOf<String, PublicProfile>()
          primary.forEach { prof ->
            if (prof.effectiveUid.isNotBlank()) merged[prof.effectiveUid] = prof
          }
          secondary.forEach { prof ->
            if (prof.effectiveUid.isNotBlank()) merged.putIfAbsent(prof.effectiveUid, prof)
          }
          merged.values.sortedByDescending { it.updatedAtMillis }
        }
        .catch { cause ->
          val firestoreErr =
            generateSequence(cause) { it.cause }
              .filterIsInstance<FirebaseFirestoreException>()
              .firstOrNull()
          if (firestoreErr == null && cause is CancellationException) throw cause
          handleFirestoreError(firestoreErr ?: cause, OperationType.LIST, path)
          throw cause
        }
    )
  }

  suspend fun searchDiscoverableProfiles(rawQuery: String): Result<List<PublicProfile>> {
    val uid =
      try {
        requireUserId()
      } catch (e: Exception) {
        return Result.failure(e)
      }
    val q = rawQuery.trim()
    if (q.length < 2) return Result.success(emptyList())
    val stripped = q.removePrefix("@").trim()
    val handleWithAt = if (stripped.isBlank()) "" else "@$stripped"
    val resultsByUid = linkedMapOf<String, PublicProfile>()

    fun collectDocs(docs: List<DocumentSnapshot>) {
      docs.mapNotNull { it.toPublicProfile() }.forEach { profile ->
        if (profile.effectiveUid.isNotBlank() && profile.effectiveUid != uid) {
          resultsByUid.putIfAbsent(profile.effectiveUid, profile)
        }
      }
    }

    return try {
      for (collectionName in listOf("appUsers", "appusers")) {
        val col = db.collection(collectionName)
        if (handleWithAt.isNotBlank()) {
          runCatching { col.whereEqualTo("handle", handleWithAt).limit(10).get().await() }
            .onSuccess { collectDocs(it.documents) }
          runCatching { col.whereEqualTo("handle", handleWithAt.uppercase()).limit(10).get().await() }
            .onSuccess { collectDocs(it.documents) }
          runCatching { col.whereEqualTo("handleLower", handleWithAt.lowercase()).limit(10).get().await() }
            .onSuccess { collectDocs(it.documents) }
          runCatching { col.whereEqualTo("username", stripped).limit(10).get().await() }
            .onSuccess { collectDocs(it.documents) }
          runCatching { col.whereEqualTo("username", stripped.uppercase()).limit(10).get().await() }
            .onSuccess { collectDocs(it.documents) }
          runCatching { col.whereEqualTo("usernameLower", stripped.lowercase()).limit(10).get().await() }
            .onSuccess { collectDocs(it.documents) }
        }
        runCatching {
          col
            .orderBy("displayNameLower")
            .startAt(q.lowercase())
            .endAt("${q.lowercase()}\uf8ff")
            .limit(25)
            .get()
            .await()
        }.onSuccess { collectDocs(it.documents) }

        runCatching {
          col
            .orderBy("displayName")
            .startAt(q)
            .endAt("${q}\uf8ff")
            .limit(25)
            .get()
            .await()
        }.onSuccess { collectDocs(it.documents) }
      }

      // Also perform a broad case-insensitive substring scan on recent discoverable docs
      // so partial @SL_ IDs, cities, or older user docs missing lowercase index fields always resolve.
      for (collectionName in listOf("appUsers", "appusers")) {
        runCatching {
          db.collection(collectionName).limit(150).get().await()
        }.onSuccess { snap ->
          collectDocs(
            snap.documents.filter { doc ->
              val prof = doc.toPublicProfile() ?: return@filter false
              prof.matchesQuery(q)
            }
          )
        }.onFailure { err ->
          if (err !is CancellationException && collectionName == "appUsers" && resultsByUid.isEmpty()) {
            handleFirestoreError(err, OperationType.LIST, collectionName)
          }
        }
      }

      Result.success(resultsByUid.values.toList())
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.LIST, "appUsers")
      Result.failure(e)
    }
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

  @OptIn(ExperimentalCoroutinesApi::class)
  fun observeChatMessages(chatId: String): Flow<List<ChatMessage>> = flow {
    val selfUid = requireUserId()
    val roomRef = db.collection("chatRooms").document(chatId)
    emitAll(
      roomRef
        .snapshots()
        .catch { cause ->
          if (cause is CancellationException) throw cause
          handleFirestoreError(cause, OperationType.GET, roomRef.path)
          throw cause
        }
        .flatMapLatest { roomSnap ->
          if (!roomSnap.exists()) {
            flowOf(emptyList())
          } else {
            val participants =
              (roomSnap.get("participantIds") as? List<*>)?.mapNotNull { it as? String }.orEmpty()
            if (participants.isNotEmpty() && selfUid !in participants) {
              throw SecurityException("Access denied: you are not a participant of this private chat.")
            }
            val path = "chatRooms/$chatId/messages"
            roomRef
              .collection("messages")
              .orderBy("createdAt", Query.Direction.ASCENDING)
              .limit(150)
              .snapshots()
              .map { snapshot ->
                snapshot.documents
                  .mapNotNull { it.toChatMessage(chatId) }
                  .filter { msg ->
                    msg.senderId == selfUid ||
                      msg.effectiveRecipientId == selfUid ||
                      selfUid in msg.participantIds
                  }
              }
              .catch { cause ->
                if (cause is CancellationException) throw cause
                handleFirestoreError(cause, OperationType.LIST, path)
                throw cause
              }
          }
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

    val safeEmail = email.trim().take(160)
    val initialCandidateHandle =
      defaultHandle.trim().ifBlank { buildHandleFromEmail(safeEmail, uid) }.take(32)
    val safeName = defaultDisplayName.trim().ifBlank { "Shayari Lover" }.take(60)
    val safePhoto = photoUrl.trim().take(MAX_AVATAR_URI_CHARS)

    val privateUserRef = db.collection("users").document(uid)
    val publicUserRef = db.collection("appUsers").document(uid)

    return try {
      val existingPrivate = privateUserRef.get().await()
      val existingPrivatePhoto = existingPrivate.getString("photoUrl").orEmpty()
      val existingPublic = publicUserRef.get().await()
      val existingPublicPhoto = existingPublic.getString("photoUrl").orEmpty()
      val preservedCustomPhoto =
        when {
          existingPublicPhoto.startsWith("data:image", ignoreCase = true) -> existingPublicPhoto
          existingPrivatePhoto.startsWith("data:image", ignoreCase = true) -> existingPrivatePhoto
          existingPublicPhoto.isNotBlank() && !existingPublicPhoto.contains("googleusercontent.com") -> existingPublicPhoto
          else -> safePhoto
        }

      var resolvedHandle = existingPublic.getString("handle").orEmpty().ifBlank { initialCandidateHandle }
      if (!existingPublic.exists() || existingPublic.getString("handle").isNullOrBlank()) {
        val collisionSnap =
          runCatching {
            db.collection("appUsers")
              .whereEqualTo("handleLower", resolvedHandle.lowercase())
              .limit(3)
              .get()
              .await()
          }.getOrNull()
        val conflictExists =
          collisionSnap?.documents?.any { doc ->
            val ownerUid = doc.getString("uid").orEmpty().ifBlank { doc.id }
            ownerUid != uid
          } == true
        if (conflictExists) {
          resolvedHandle = buildCollisionSafeHandle(safeEmail, uid)
        }
      }

      val cleanUsername =
        existingPublic.getString("username").orEmpty().ifBlank { resolvedHandle.removePrefix("@") }
      val existingName = existingPublic.getString("displayName").orEmpty().ifBlank { safeName }

      val privatePayload =
        buildMap<String, Any> {
          put("uid", uid)
          put("displayName", existingName)
          put("email", safeEmail)
          if (preservedCustomPhoto.isNotEmpty()) put("photoUrl", preservedCustomPhoto)
          put("handle", resolvedHandle)
          put("username", cleanUsername)
          if (!existingPrivate.exists()) {
            put("createdAt", FieldValue.serverTimestamp())
          }
          put("updatedAt", FieldValue.serverTimestamp())
        }
      privateUserRef.set(privatePayload, SetOptions.merge()).await()

      val publicPayload =
        if (!existingPublic.exists()) {
          buildMap<String, Any> {
            put("uid", uid)
            put("userId", uid)
            put("displayName", safeName)
            put("displayNameLower", safeName.lowercase())
            put("username", cleanUsername)
            put("usernameLower", cleanUsername.lowercase())
            put("handle", resolvedHandle)
            put("handleLower", resolvedHandle.lowercase())
            if (preservedCustomPhoto.isNotEmpty()) put("photoUrl", preservedCustomPhoto)
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
            put("uid", uid)
            put("userId", uid)
            if (existingPublic.getString("displayName").isNullOrBlank()) {
              put("displayName", safeName)
              put("displayNameLower", safeName.lowercase())
            } else {
              put("displayNameLower", existingName.lowercase())
            }
            put("handle", resolvedHandle)
            put("handleLower", resolvedHandle.lowercase())
            put("username", cleanUsername)
            put("usernameLower", cleanUsername.lowercase())
            if (existingPublicPhoto.isBlank() && preservedCustomPhoto.isNotEmpty()) {
              put("photoUrl", preservedCustomPhoto)
            }
            if (existingPublic.get("followersCount") == null) put("followersCount", 0L)
            if (existingPublic.get("followingCount") == null) put("followingCount", 0L)
            if (existingPublic.get("likesReceivedCount") == null) put("likesReceivedCount", 0L)
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
            handle = resolvedHandle,
            username = cleanUsername,
            displayName = existingName,
            photoUrl = preservedCustomPhoto,
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
    handle: String? = null,
    city: String,
    address: String,
    bio: String,
    photoUrl: String? = null,
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

    val normalizedHandlePair: Pair<String, String>? =
      if (handle != null) {
        val raw = handle.trim().removePrefix("@")
        if (!HANDLE_REGEX.matches(raw)) {
          return Result.failure(
            IllegalArgumentException("Handle must be 3-24 characters (letters, numbers, _)")
          )
        }
        "@$raw" to raw
      } else {
        null
      }

    val publicRef = db.collection("appUsers").document(uid)
    val privateRef = db.collection("users").document(uid)

    return try {
      if (normalizedHandlePair != null) {
        val (desiredHandle, _) = normalizedHandlePair
        val conflictSnap =
          runCatching {
            db.collection("appUsers")
              .whereEqualTo("handleLower", desiredHandle.lowercase())
              .limit(5)
              .get()
              .await()
          }.getOrNull()
        val hasConflict =
          conflictSnap?.documents?.any { doc ->
            val ownerUid = doc.getString("uid").orEmpty().ifBlank { doc.id }
            ownerUid != uid
          } == true
        if (hasConflict) {
          return Result.failure(IllegalStateException("Handle $desiredHandle is already taken"))
        }
      }

      val publicUpdates =
        buildMap<String, Any> {
          put("displayName", cleanName)
          put("displayNameLower", cleanName.lowercase())
          put("city", city.trim().take(80))
          put("bio", bio.trim().take(220))
          if (normalizedHandlePair != null) {
            put("handle", normalizedHandlePair.first)
            put("handleLower", normalizedHandlePair.first.lowercase())
            put("username", normalizedHandlePair.second)
            put("usernameLower", normalizedHandlePair.second.lowercase())
          }
          if (photoUrl != null) {
            put("photoUrl", photoUrl.trim().take(MAX_AVATAR_URI_CHARS))
          }
          put("updatedAt", FieldValue.serverTimestamp())
        }

      val privateUpdates =
        buildMap<String, Any> {
          put("uid", uid)
          put("displayName", cleanName)
          put("address", address.trim().take(160))
          if (normalizedHandlePair != null) {
            put("handle", normalizedHandlePair.first)
            put("username", normalizedHandlePair.second)
          }
          if (photoUrl != null) {
            put("photoUrl", photoUrl.trim().take(MAX_AVATAR_URI_CHARS))
          }
          put("updatedAt", FieldValue.serverTimestamp())
        }

      val batch = db.batch()
      batch.set(publicRef, publicUpdates, SetOptions.merge())
      batch.set(privateRef, privateUpdates, SetOptions.merge())
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, publicRef.path)
      Result.failure(e)
    }
  }

  suspend fun updateMyAvatarPhoto(dataUriOrEmpty: String): Result<Unit> {
    val uid =
      try {
        requireUserId()
      } catch (e: Exception) {
        return Result.failure(e)
      }
    val safePhoto = dataUriOrEmpty.trim().take(MAX_AVATAR_URI_CHARS)
    val publicRef = db.collection("appUsers").document(uid)
    val privateRef = db.collection("users").document(uid)

    return try {
      val batch = db.batch()
      batch.set(
        publicRef,
        mapOf(
          "photoUrl" to safePhoto,
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
        SetOptions.merge(),
      )
      batch.set(
        privateRef,
        mapOf(
          "photoUrl" to safePhoto,
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

  private suspend fun resolveCounterTargetDoc(targetUid: String) =
    if (runCatching { db.collection("appUsers").document(targetUid).get().await().exists() }.getOrDefault(false)) {
      db.collection("appUsers").document(targetUid)
    } else if (runCatching { db.collection("appusers").document(targetUid).get().await().exists() }.getOrDefault(false)) {
      db.collection("appusers").document(targetUid)
    } else {
      null
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
    val targetDoc = resolveCounterTargetDoc(targetUid)

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

      batch.set(
        selfDoc,
        mapOf(
          "uid" to uid,
          "followingCount" to FieldValue.increment(delta),
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
        SetOptions.merge(),
      )
      if (targetDoc != null) {
        batch.update(
          targetDoc,
          mapOf(
            "followersCount" to FieldValue.increment(delta),
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
      }
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
    val targetDoc = resolveCounterTargetDoc(targetUid)

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

      if (targetDoc != null) {
        batch.update(
          targetDoc,
          mapOf(
            "likesReceivedCount" to FieldValue.increment(delta),
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
      }
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
    if (targetUid.isBlank() || targetUid == uid) {
      return Result.failure(IllegalArgumentException("Cannot send private message to this recipient"))
    }
    val sortedParticipants = listOf(uid, targetUid).sorted()
    val roomId = buildChatRoomId(uid, targetUid)
    val roomRef = db.collection("chatRooms").document(roomId)
    val msgRef = roomRef.collection("messages").document()
    val notifRef = db.collection("appUsers").document(targetUid).collection("notifications").document()

    return try {
      val batch = db.batch()
      batch.set(
        roomRef,
        mapOf(
          "participantIds" to sortedParticipants,
          "participantNames" to
            listOf(selfProfile.displayName.take(100), targetProfile.displayName.take(100)),
          "participantHandles" to
            mapOf(uid to selfProfile.effectiveHandle, targetUid to targetProfile.effectiveHandle),
          "participantDisplayNames" to
            mapOf(uid to selfProfile.displayName, targetUid to targetProfile.displayName),
          "participantPhotos" to
            mapOf(
              uid to selfProfile.photoUrl.take(MAX_AVATAR_URI_CHARS),
              targetUid to targetProfile.photoUrl.take(MAX_AVATAR_URI_CHARS),
            ),
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
          "participantIds" to sortedParticipants,
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
    val photosMap = (get("participantPhotos") as? Map<*, *>)
    val partnerName =
      (namesMap?.get(partnerId) as? String)
        ?: getString("partnerName").orEmpty().ifBlank { "Shayar Friend" }
    val partnerHandle =
      (handlesMap?.get(partnerId) as? String)
        ?: getString("partnerHandle").orEmpty().ifBlank { "@SL_${partnerId.takeLast(4).uppercase()}" }
    val partnerPhoto = (photosMap?.get(partnerId) as? String).orEmpty()
    val updatedTs =
      getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)

    return ChatRoom(
      id = id,
      participantIds = participants,
      partnerId = partnerId,
      partnerHandle = partnerHandle,
      partnerName = partnerName,
      partnerPhotoUrl = partnerPhoto,
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
    val msgParticipants =
      (get("participantIds") as? List<*>)?.mapNotNull { it as? String }.orEmpty()
    val createdTs =
      getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)

    return ChatMessage(
      id = id,
      chatId = roomId,
      roomId = getString("roomId").orEmpty().ifBlank { roomId },
      participantIds = msgParticipants,
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
    const val MAX_AVATAR_URI_CHARS = 250_000
    private val HANDLE_REGEX = Regex("^[a-zA-Z0-9_]{3,24}$")

    fun buildChatRoomId(uidA: String, uidB: String): String =
      "room_" + listOf(uidA, uidB).sorted().joinToString("_")

    fun buildHandleFromEmail(email: String, fallbackUid: String = ""): String {
      val cleanEmail = email.trim().lowercase()
      if (cleanEmail.isBlank()) {
        return buildHandleFromUid(fallbackUid)
      }
      val digest =
        java.security.MessageDigest.getInstance("SHA-256")
          .digest(cleanEmail.toByteArray(Charsets.UTF_8))
      val rawInt =
        ((digest[0].toInt() and 0x7F) shl 24) or
          ((digest[1].toInt() and 0xFF) shl 16) or
          ((digest[2].toInt() and 0xFF) shl 8) or
          (digest[3].toInt() and 0xFF)
      val fourDigits = (1000 + (rawInt % 9000)).toString()
      return "@SL_$fourDigits"
    }

    private fun buildCollisionSafeHandle(email: String, uid: String): String {
      val seed = "${email.trim().lowercase()}|$uid"
      val digest =
        java.security.MessageDigest.getInstance("SHA-256")
          .digest(seed.toByteArray(Charsets.UTF_8))
      val rawInt =
        ((digest[0].toInt() and 0x7F) shl 24) or
          ((digest[1].toInt() and 0xFF) shl 16) or
          ((digest[2].toInt() and 0xFF) shl 8) or
          (digest[3].toInt() and 0xFF)
      val fiveDigits = (10000 + (rawInt % 90000)).toString()
      return "@SL_$fiveDigits"
    }

    fun buildHandleFromUid(uid: String): String {
      val hash = uid.hashCode().let { if (it == Int.MIN_VALUE) 0 else kotlin.math.abs(it) }
      val fourDigits = (1000 + (hash % 9000)).toString()
      return "@SL_$fourDigits"
    }
  }
}
