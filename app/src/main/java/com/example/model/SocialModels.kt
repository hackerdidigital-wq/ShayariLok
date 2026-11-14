package com.example.model

data class PublicProfile(
  val id: String = "",
  val userId: String = "",
  val handle: String = "",
  val displayName: String = "",
  val city: String = "",
  val address: String = "",
  val bio: String = "",
  val followersCount: Long = 0L,
  val followingCount: Long = 0L,
  val likesCount: Long = 0L,
  val isFollowing: Boolean = false,
  val isLiked: Boolean = false,
  val isBlocked: Boolean = false,
  val blockReason: String = "",
  val createdAtMillis: Long = 0L,
  val updatedAtMillis: Long = 0L,
)

data class ChatRoom(
  val id: String = "",
  val partnerId: String = "",
  val partnerHandle: String = "",
  val partnerName: String = "",
  val lastMessage: String = "",
  val lastSenderId: String = "",
  val updatedAtMillis: Long = 0L,
)

data class ChatMessage(
  val id: String = "",
  val chatId: String = "",
  val senderId: String = "",
  val senderHandle: String = "",
  val recipientId: String = "",
  val text: String = "",
  val isRead: Boolean = true,
  val createdAtMillis: Long = 0L,
)

data class SocialNotification(
  val id: String = "",
  val recipientId: String = "",
  val senderId: String = "",
  val senderHandle: String = "",
  val senderName: String = "",
  val type: String = "message",
  val previewText: String = "",
  val chatId: String = "",
  val isRead: Boolean = false,
  val createdAtMillis: Long = 0L,
)
