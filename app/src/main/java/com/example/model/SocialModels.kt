package com.example.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class PublicProfile(
  @DocumentId val id: String = "",
  val userId: String = "",
  val uid: String = "",
  val handle: String = "",
  val username: String = "",
  val displayName: String = "",
  val photoUrl: String = "",
  val city: String = "",
  val address: String = "",
  val bio: String = "",
  val followersCount: Long = 0L,
  val followingCount: Long = 0L,
  val likesCount: Long = 0L,
  val likesReceivedCount: Long = 0L,
  val isOnline: Boolean = true,
  val isFollowing: Boolean = false,
  val isLiked: Boolean = false,
  val isBlocked: Boolean = false,
  val blockReason: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
  val createdAtMillis: Long = 0L,
  val updatedAtMillis: Long = 0L,
) {
  val effectiveUid: String
    get() = userId.ifBlank { uid }.ifBlank { id }

  val effectiveHandle: String
    get() =
      when {
        handle.isNotBlank() -> handle
        username.startsWith("@") -> username
        username.isNotBlank() -> "@$username"
        else -> "@SL_${effectiveUid.takeLast(4).uppercase()}"
      }

  val effectiveLikesCount: Long
    get() = maxOf(likesReceivedCount, likesCount)
}

@IgnoreExtraProperties
data class PrivateUserAccount(
  @DocumentId val id: String = "",
  val uid: String = "",
  val displayName: String = "",
  val email: String = "",
  val photoUrl: String = "",
  val handle: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class ChatRoom(
  @DocumentId val id: String = "",
  val participantIds: List<String> = emptyList(),
  val participantNames: List<String> = emptyList(),
  val partnerId: String = "",
  val partnerHandle: String = "",
  val partnerName: String = "",
  val lastMessage: String = "",
  val lastSenderId: String = "",
  val messageCount: Long = 0L,
  val flaggedSpam: Boolean = false,
  @ServerTimestamp val updatedAt: Timestamp? = null,
  val updatedAtMillis: Long = 0L,
)

@IgnoreExtraProperties
data class ChatMessage(
  @DocumentId val id: String = "",
  val chatId: String = "",
  val roomId: String = "",
  val senderId: String = "",
  val senderName: String = "",
  val senderHandle: String = "",
  val recipientId: String = "",
  val receiverId: String = "",
  val text: String = "",
  val isRead: Boolean = false,
  val isFlagged: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null,
  val createdAtMillis: Long = 0L,
) {
  val effectiveRecipientId: String
    get() = recipientId.ifBlank { receiverId }
}

@IgnoreExtraProperties
data class SocialNotification(
  @DocumentId val id: String = "",
  val recipientId: String = "",
  val senderId: String = "",
  val senderHandle: String = "",
  val senderName: String = "",
  val type: String = "message",
  val previewText: String = "",
  val chatId: String = "",
  val isRead: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null,
  val createdAtMillis: Long = 0L,
)
