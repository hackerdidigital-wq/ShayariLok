package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
  @PrimaryKey val userId: String,
  val handle: String,
  val displayName: String,
  val city: String,
  val address: String,
  val bio: String,
  val followersCount: Long,
  val followingCount: Long,
  val likesCount: Long,
  val isSelf: Boolean,
  val isFollowing: Boolean,
  val isLiked: Boolean,
  val isBlocked: Boolean = false,
  val blockReason: String = "",
  val createdAtMillis: Long,
  val updatedAtMillis: Long,
)

@Entity(tableName = "chat_rooms")
data class ChatRoomEntity(
  @PrimaryKey val roomId: String,
  val partnerId: String,
  val partnerHandle: String,
  val partnerName: String,
  val lastMessage: String,
  val lastSenderId: String,
  val updatedAtMillis: Long,
)

@Entity(
  tableName = "chat_messages",
  indices = [Index(value = ["roomId"])],
)
data class ChatMessageEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0L,
  val remoteDocId: String = "",
  val roomId: String,
  val senderId: String,
  val senderHandle: String,
  val recipientId: String,
  val text: String,
  val isRead: Boolean,
  val createdAtMillis: Long,
)

@Entity(tableName = "social_notifications")
data class SocialNotificationEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0L,
  val recipientId: String,
  val senderId: String,
  val senderHandle: String,
  val senderName: String,
  val type: String,
  val previewText: String,
  val chatRoomId: String,
  val isRead: Boolean,
  val createdAtMillis: Long,
)

@Dao
interface SocialDao {
  @Query("SELECT * FROM user_profiles WHERE userId = :selfUid LIMIT 1")
  fun observeSelfProfile(selfUid: String): Flow<UserProfileEntity?>

  @Query("SELECT * FROM user_profiles WHERE isSelf = 0 ORDER BY followersCount DESC, displayName ASC")
  fun observeCommunityProfiles(): Flow<List<UserProfileEntity>>

  @Query("SELECT * FROM chat_rooms ORDER BY updatedAtMillis DESC")
  fun observeChatRooms(): Flow<List<ChatRoomEntity>>

  @Query("SELECT * FROM chat_messages WHERE roomId = :roomId ORDER BY createdAtMillis ASC, id ASC")
  fun observeMessages(roomId: String): Flow<List<ChatMessageEntity>>

  @Query("SELECT * FROM social_notifications ORDER BY createdAtMillis DESC, id DESC")
  fun observeNotifications(): Flow<List<SocialNotificationEntity>>

  @Query("SELECT COUNT(*) FROM user_profiles WHERE isSelf = 0")
  suspend fun countCommunityProfiles(): Int

  @Query("SELECT COUNT(*) FROM chat_messages WHERE roomId = :roomId")
  suspend fun countRoomMessages(roomId: String): Int

  @Query("SELECT * FROM chat_messages WHERE roomId = :roomId")
  suspend fun getRoomMessagesSnapshot(roomId: String): List<ChatMessageEntity>

  @Query("SELECT * FROM user_profiles WHERE userId = :uid LIMIT 1")
  suspend fun getProfileById(uid: String): UserProfileEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertProfile(profile: UserProfileEntity)

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insertProfilesIgnore(profiles: List<UserProfileEntity>)

  @Query(
    "UPDATE user_profiles SET displayName = :displayName, city = :city, address = :address, bio = :bio, updatedAtMillis = :updatedAt WHERE userId = :uid"
  )
  suspend fun updateSelfProfile(
    uid: String,
    displayName: String,
    city: String,
    address: String,
    bio: String,
    updatedAt: Long,
  )

  @Query(
    "UPDATE user_profiles SET isBlocked = :isBlocked, blockReason = :blockReason, updatedAtMillis = :updatedAt WHERE userId = :uid"
  )
  suspend fun updateBlockStatus(
    uid: String,
    isBlocked: Boolean,
    blockReason: String,
    updatedAt: Long,
  )

  @Query(
    "UPDATE user_profiles SET isFollowing = :isFollowing, followersCount = MAX(0, followersCount + :followersDelta), updatedAtMillis = :updatedAt WHERE userId = :targetUid"
  )
  suspend fun updateTargetFollowing(
    targetUid: String,
    isFollowing: Boolean,
    followersDelta: Long,
    updatedAt: Long,
  )

  @Query(
    "UPDATE user_profiles SET followingCount = MAX(0, followingCount + :delta), updatedAtMillis = :updatedAt WHERE userId = :selfUid"
  )
  suspend fun adjustSelfFollowingCount(
    selfUid: String,
    delta: Long,
    updatedAt: Long,
  )

  @Query(
    "UPDATE user_profiles SET isLiked = :isLiked, likesCount = MAX(0, likesCount + :likesDelta), updatedAtMillis = :updatedAt WHERE userId = :targetUid"
  )
  suspend fun updateTargetLike(
    targetUid: String,
    isLiked: Boolean,
    likesDelta: Long,
    updatedAt: Long,
  )

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertChatRoom(room: ChatRoomEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertMessage(message: ChatMessageEntity): Long

  @Query(
    "UPDATE chat_messages SET isRead = 1 WHERE roomId = :roomId AND recipientId = :recipientId AND isRead = 0"
  )
  suspend fun markRoomMessagesRead(roomId: String, recipientId: String)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertNotification(notification: SocialNotificationEntity): Long

  @Query("UPDATE social_notifications SET isRead = 1 WHERE isRead = 0")
  suspend fun markAllNotificationsRead()
}

@Database(
  entities = [
    UserProfileEntity::class,
    ChatRoomEntity::class,
    ChatMessageEntity::class,
    SocialNotificationEntity::class,
  ],
  version = 2,
  exportSchema = false,
)
abstract class SocialDatabase : RoomDatabase() {
  abstract fun socialDao(): SocialDao

  companion object {
    @Volatile private var INSTANCE: SocialDatabase? = null

    fun getInstance(context: Context): SocialDatabase =
      INSTANCE
        ?: synchronized(this) {
          INSTANCE
            ?: Room.databaseBuilder(
                context.applicationContext,
                SocialDatabase::class.java,
                "shayari_lok_social.db",
              )
              .fallbackToDestructiveMigration(dropAllTables = true)
              .build()
              .also { INSTANCE = it }
        }
  }
}
