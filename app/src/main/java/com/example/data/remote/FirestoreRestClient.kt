package com.example.data.remote

import com.example.model.ChatMessage
import com.example.model.PublicProfile
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class FirestoreRestClient(
  private val baseUrl: String = DEFAULT_FIRESTORE_REST_BASE_URL,
) {

  suspend fun upsertAppUser(profile: PublicProfile): Result<Unit> =
    withContext(Dispatchers.IO) {
      val nowIso = formatIsoTimestamp(System.currentTimeMillis())
      val createdIso =
        formatIsoTimestamp(
          profile.createdAtMillis.takeIf { it > 0L } ?: System.currentTimeMillis()
        )
      val cleanUsername =
        profile.handle.replace("[^a-zA-Z0-9_.]".toRegex(), "").ifBlank { "SL_USER" }.take(40)

      val fields =
        JSONObject()
          .put("uid", strVal(profile.userId))
          .put("displayName", strVal(profile.displayName.take(100)))
          .put("username", strVal(cleanUsername))
          .put("handle", strVal(profile.handle.take(40)))
          .put("city", strVal(profile.city.take(80)))
          .put("address", strVal(profile.address.take(200)))
          .put("bio", strVal(profile.bio.take(300)))
          .put("isOnline", boolVal(true))
          .put("isBlocked", boolVal(profile.isBlocked))
          .put("blockReason", strVal(profile.blockReason.take(200)))
          .put("followersCount", intVal(profile.followersCount))
          .put("followingCount", intVal(profile.followingCount))
          .put("likesReceivedCount", intVal(profile.likesCount))
          .put("createdAt", tsVal(createdIso))
          .put("updatedAt", tsVal(nowIso))

      val payload = JSONObject().put("fields", fields).toString()
      executeRequest(
        urlString = "$baseUrl/appUsers/${profile.userId}",
        method = "PATCH",
        jsonBody = payload,
      ).map {}
    }

  suspend fun fetchAppUsers(): Result<List<PublicProfile>> =
    withContext(Dispatchers.IO) {
      executeRequest(
        urlString = "$baseUrl/appUsers?pageSize=100",
        method = "GET",
        jsonBody = null,
      ).map { raw ->
        val root = JSONObject(raw)
        val docs = root.optJSONArray("documents") ?: JSONArray()
        buildList {
          for (i in 0 until docs.length()) {
            val doc = docs.optJSONObject(i) ?: continue
            parseAppUserDocument(doc)?.let { add(it) }
          }
        }
      }
    }

  suspend fun fetchSingleAppUser(uid: String): Result<PublicProfile?> =
    withContext(Dispatchers.IO) {
      executeRequest(
        urlString = "$baseUrl/appUsers/$uid",
        method = "GET",
        jsonBody = null,
      ).map { raw ->
        val doc = JSONObject(raw)
        parseAppUserDocument(doc)
      }
    }

  suspend fun upsertSocialEdge(
    actorId: String,
    targetId: String,
    actorName: String,
    targetName: String,
    type: String,
  ): Result<Unit> =
    withContext(Dispatchers.IO) {
      val edgeId = "${actorId}_${type}_${targetId}"
      val fields =
        JSONObject()
          .put("actorId", strVal(actorId))
          .put("targetId", strVal(targetId))
          .put("actorName", strVal(actorName.take(100)))
          .put("targetName", strVal(targetName.take(100)))
          .put("type", strVal(type))
          .put("createdAt", tsVal(formatIsoTimestamp(System.currentTimeMillis())))

      val payload = JSONObject().put("fields", fields).toString()
      executeRequest(
        urlString = "$baseUrl/socialEdges/$edgeId",
        method = "PATCH",
        jsonBody = payload,
      ).map {}
    }

  suspend fun deleteSocialEdge(
    actorId: String,
    targetId: String,
    type: String,
  ): Result<Unit> =
    withContext(Dispatchers.IO) {
      val edgeId = "${actorId}_${type}_${targetId}"
      executeRequest(
        urlString = "$baseUrl/socialEdges/$edgeId",
        method = "DELETE",
        jsonBody = null,
      ).map {}
    }

  suspend fun sendChatMessage(
    roomId: String,
    senderProfile: PublicProfile,
    receiverProfile: PublicProfile,
    text: String,
    messageCountEstimate: Long,
  ): Result<Unit> =
    withContext(Dispatchers.IO) {
      val nowIso = formatIsoTimestamp(System.currentTimeMillis())

      val roomFields =
        JSONObject()
          .put("participantIds", strArrayVal(listOf(senderProfile.userId, receiverProfile.userId)))
          .put(
            "participantNames",
            strArrayVal(listOf(senderProfile.displayName.take(100), receiverProfile.displayName.take(100))),
          )
          .put("lastMessage", strVal(text.take(1000)))
          .put("lastSenderId", strVal(senderProfile.userId))
          .put("messageCount", intVal(messageCountEstimate.coerceAtLeast(1L)))
          .put("flaggedSpam", boolVal(false))
          .put("updatedAt", tsVal(nowIso))

      val roomPayload = JSONObject().put("fields", roomFields).toString()
      val roomResult =
        executeRequest(
          urlString = "$baseUrl/chatRooms/$roomId",
          method = "PATCH",
          jsonBody = roomPayload,
        )
      if (roomResult.isFailure) {
        return@withContext Result.failure(roomResult.exceptionOrNull()!!)
      }

      val msgFields =
        JSONObject()
          .put("roomId", strVal(roomId))
          .put("senderId", strVal(senderProfile.userId))
          .put("senderName", strVal(senderProfile.displayName.take(100)))
          .put("senderHandle", strVal(senderProfile.handle.take(40)))
          .put("receiverId", strVal(receiverProfile.userId))
          .put("text", strVal(text.take(2000)))
          .put("isFlagged", boolVal(false))
          .put("createdAt", tsVal(nowIso))

      val msgPayload = JSONObject().put("fields", msgFields).toString()
      executeRequest(
        urlString = "$baseUrl/chatRooms/$roomId/messages",
        method = "POST",
        jsonBody = msgPayload,
      ).map {}
    }

  suspend fun fetchRoomMessages(roomId: String): Result<List<ChatMessage>> =
    withContext(Dispatchers.IO) {
      executeRequest(
        urlString = "$baseUrl/chatRooms/$roomId/messages?orderBy=createdAt&pageSize=100",
        method = "GET",
        jsonBody = null,
      ).map { raw ->
        val root = JSONObject(raw)
        val docs = root.optJSONArray("documents") ?: JSONArray()
        buildList {
          for (i in 0 until docs.length()) {
            val doc = docs.optJSONObject(i) ?: continue
            parseMessageDocument(roomId, doc)?.let { add(it) }
          }
        }
      }
    }

  private fun executeRequest(
    urlString: String,
    method: String,
    jsonBody: String?,
  ): Result<String> {
    var connection: HttpURLConnection? = null
    return try {
      val url = URL(urlString)
      connection =
        (url.openConnection() as HttpURLConnection).apply {
          connectTimeout = 8_000
          readTimeout = 8_000
          if (method == "PATCH") {
            requestMethod = "POST"
            setRequestProperty("X-HTTP-Method-Override", "PATCH")
          } else {
            requestMethod = method
          }
          setRequestProperty("Accept", "application/json")
          if (jsonBody != null) {
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            OutputStreamWriter(outputStream, Charsets.UTF_8).use { writer ->
              writer.write(jsonBody)
              writer.flush()
            }
          }
        }

      val code = connection.responseCode
      val stream = if (code in 200..299) connection.inputStream else connection.errorStream
      val responseText = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()

      if (code in 200..299) {
        Result.success(responseText)
      } else {
        Result.failure(IllegalStateException("HTTP $code: ${responseText.take(240)}"))
      }
    } catch (e: Exception) {
      Result.failure(e)
    } finally {
      connection?.disconnect()
    }
  }

  private fun parseAppUserDocument(doc: JSONObject): PublicProfile? {
    val fields = doc.optJSONObject("fields") ?: return null
    val docNameId = doc.optString("name").substringAfterLast("/").trim()
    val uid = readString(fields, "uid").ifBlank { docNameId }
    if (uid.isBlank()) return null

    val rawUsername = readString(fields, "username")
    val explicitHandle = readString(fields, "handle")
    val resolvedHandle =
      when {
        explicitHandle.isNotBlank() -> explicitHandle
        rawUsername.startsWith("@") -> rawUsername
        rawUsername.isNotBlank() -> "@$rawUsername"
        else -> "@SL_${uid.takeLast(4).uppercase()}"
      }

    return PublicProfile(
      id = uid,
      userId = uid,
      handle = resolvedHandle,
      displayName = readString(fields, "displayName").ifBlank { "Shayari Lover" },
      city = readString(fields, "city"),
      address = readString(fields, "address"),
      bio = readString(fields, "bio"),
      followersCount = readLong(fields, "followersCount"),
      followingCount = readLong(fields, "followingCount"),
      likesCount = readLong(fields, "likesReceivedCount").takeIf { it > 0L }
        ?: readLong(fields, "likesCount"),
      isBlocked = readBoolean(fields, "isBlocked"),
      blockReason = readString(fields, "blockReason"),
      createdAtMillis = parseIsoTimestamp(readTimestamp(fields, "createdAt")),
      updatedAtMillis = parseIsoTimestamp(readTimestamp(fields, "updatedAt")),
    )
  }

  private fun parseMessageDocument(roomId: String, doc: JSONObject): ChatMessage? {
    val fields = doc.optJSONObject("fields") ?: return null
    val docId = doc.optString("name").substringAfterLast("/").trim()
    val text = readString(fields, "text")
    val senderId = readString(fields, "senderId")
    if (text.isBlank() || senderId.isBlank()) return null

    return ChatMessage(
      id = docId.ifBlank { "${senderId}_${text.hashCode()}" },
      chatId = readString(fields, "roomId").ifBlank { roomId },
      senderId = senderId,
      senderHandle = readString(fields, "senderHandle"),
      recipientId = readString(fields, "receiverId"),
      text = text,
      isRead = false,
      createdAtMillis =
        parseIsoTimestamp(readTimestamp(fields, "createdAt")).takeIf { it > 0L }
          ?: System.currentTimeMillis(),
    )
  }

  private fun strVal(v: String): JSONObject = JSONObject().put("stringValue", v)

  private fun boolVal(v: Boolean): JSONObject = JSONObject().put("booleanValue", v)

  private fun intVal(v: Long): JSONObject = JSONObject().put("integerValue", v.toString())

  private fun tsVal(iso: String): JSONObject = JSONObject().put("timestampValue", iso)

  private fun strArrayVal(items: List<String>): JSONObject {
    val arr = JSONArray()
    items.forEach { arr.put(strVal(it)) }
    return JSONObject().put("arrayValue", JSONObject().put("values", arr))
  }

  private fun readString(fields: JSONObject, key: String): String =
    fields.optJSONObject(key)?.optString("stringValue").orEmpty()

  private fun readBoolean(fields: JSONObject, key: String): Boolean =
    fields.optJSONObject(key)?.optBoolean("booleanValue", false) ?: false

  private fun readLong(fields: JSONObject, key: String): Long {
    val obj = fields.optJSONObject(key) ?: return 0L
    val raw = obj.optString("integerValue")
    if (raw.isNotBlank()) return raw.toLongOrNull() ?: 0L
    return obj.optLong("integerValue", 0L)
  }

  private fun readTimestamp(fields: JSONObject, key: String): String =
    fields.optJSONObject(key)?.optString("timestampValue").orEmpty()

  companion object {
    const val DEFAULT_FIRESTORE_REST_BASE_URL =
      "https://firestore.googleapis.com/v1/projects/psychic-advantage-n1mzh/databases/ai-studio-f7f3be7b-496c-4d08-bf57-38a268f047a4/documents"

    private fun formatIsoTimestamp(millis: Long): String {
      val sdf =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
          timeZone = TimeZone.getTimeZone("UTC")
        }
      return sdf.format(Date(millis))
    }

    private fun parseIsoTimestamp(iso: String): Long {
      if (iso.isBlank()) return 0L
      val patterns =
        listOf(
          "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
          "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'",
          "yyyy-MM-dd'T'HH:mm:ss'Z'",
        )
      for (pattern in patterns) {
        try {
          val sdf =
            SimpleDateFormat(pattern, Locale.US).apply {
              timeZone = TimeZone.getTimeZone("UTC")
            }
          val parsed = sdf.parse(iso)
          if (parsed != null) return parsed.time
        } catch (_: Exception) {
          // Try next timestamp format
        }
      }
      return 0L
    }
  }
}
