package com.example.model

import org.json.JSONArray
import org.json.JSONObject

/**
 * Pure native data models for Shayari Lok remote configuration.
 * Parsed using Android's built-in org.json to keep APK size ultra-lightweight.
 *
 * Crash-safe parsing handles BOM headers, string-typed booleans/numbers,
 * missing optional fields, and duplicate IDs cleanly.
 */
data class ShayariItem(
  val id: Int,
  val category: String,
  val text: String,
  val author: String = "",
) {
  fun toJson(): JSONObject =
    JSONObject().apply {
      put("id", id)
      put("category", category)
      put("text", text)
      if (author.isNotBlank()) {
        put("author", author)
      }
    }

  companion object {
    fun fromJson(json: JSONObject, fallbackId: Int = -1): ShayariItem? {
      val parsedId = json.optFlexibleInt("id", fallbackId)
      val category = json.optCleanString("category")
      val text = json.optCleanString("text")
      val author = json.optCleanString("author")
      if (parsedId < 0 || category.isEmpty() || text.isEmpty()) return null
      return ShayariItem(
        id = parsedId,
        category = category,
        text = text,
        author = author,
      )
    }
  }
}

data class ShayariNotification(
  val id: Int,
  val title: String,
  val message: String,
  val badge: String = "Update",
  val timeLabel: String = "Latest",
) {
  fun toJson(): JSONObject =
    JSONObject().apply {
      put("id", id)
      put("title", title)
      put("message", message)
      put("badge", badge)
      put("time_label", timeLabel)
    }

  companion object {
    fun fromJson(json: JSONObject, fallbackId: Int): ShayariNotification? {
      val id = json.optFlexibleInt("id", fallbackId).let { if (it < 0) fallbackId else it }
      val title = json.optCleanString("title")
      val message = json.optCleanString("message")
      if (title.isEmpty() && message.isEmpty()) return null
      val badge = json.optCleanString("badge").ifEmpty { "Update" }
      val timeLabel = json.optCleanString("time_label").ifEmpty { "Latest" }
      return ShayariNotification(
        id = id,
        title = title.ifEmpty { "Shayari Alert" },
        message = message,
        badge = badge,
        timeLabel = timeLabel,
      )
    }
  }
}

data class ShayariConfig(
  val appName: String,
  val latestVersionCode: Int,
  val forceUpdateUrl: String,
  val maintenanceMode: Boolean,
  val maintenanceMessage: String = DEFAULT_MAINTENANCE_MESSAGE,
  val privacyPolicyUrl: String = "",
  val socialLink: String = "",
  val adsEnabled: Boolean = false,
  val bannerAdId: String = "",
  val interstitialAdId: String = "",
  val announcementTitle: String = "",
  val announcementMessage: String = "",
  val notifications: List<ShayariNotification> = emptyList(),
  val categories: List<String> = DEFAULT_CATEGORIES,
  val shayariList: List<ShayariItem> = emptyList(),
) {
  fun toJson(): String {
    val root = JSONObject()
    root.put("app_name", appName)
    root.put("latest_version_code", latestVersionCode)
    root.put("force_update_url", forceUpdateUrl)
    root.put("maintenance_mode", maintenanceMode)
    if (maintenanceMessage.isNotBlank()) {
      root.put("maintenance_message", maintenanceMessage)
    }
    root.put("privacy_policy_url", privacyPolicyUrl)
    root.put("social_link", socialLink)
    root.put("ads_enabled", adsEnabled)
    root.put("banner_ad_id", bannerAdId)
    root.put("interstitial_ad_id", interstitialAdId)
    if (announcementTitle.isNotBlank()) {
      root.put("announcement_title", announcementTitle)
    }
    if (announcementMessage.isNotBlank()) {
      root.put("announcement_message", announcementMessage)
    }

    val notificationArray = JSONArray()
    notifications.forEach { notificationArray.put(it.toJson()) }
    root.put("notifications", notificationArray)

    val categoryArray = JSONArray()
    categories.forEach { categoryArray.put(it) }
    root.put("categories", categoryArray)

    val listArray = JSONArray()
    shayariList.forEach { listArray.put(it.toJson()) }
    root.put("shayari_list", listArray)
    return root.toString(2)
  }

  companion object {
    const val DEFAULT_MAINTENANCE_MESSAGE =
      "हम ऐप को और बेहतर बना रहे हैं। कृपया कुछ समय बाद पुनः प्रयास करें।"
    val DEFAULT_CATEGORIES = listOf("All", "Love", "Attitude", "Motivation", "Sad")

    fun fromJson(rawJson: String): ShayariConfig {
      val sanitized = rawJson.trimStart('\uFEFF').trim()
      val root = JSONObject(sanitized)

      val appName = root.optCleanString("app_name").ifEmpty { "Shayari Lok" }
      val latestVersionCode = root.optFlexibleInt("latest_version_code", 1).coerceAtLeast(1)
      val forceUpdateUrl = root.optCleanString("force_update_url")
      val maintenanceMode = root.optFlexibleBoolean("maintenance_mode", false)
      val maintenanceMessage =
        root.optCleanString("maintenance_message").ifEmpty { DEFAULT_MAINTENANCE_MESSAGE }
      val privacyPolicyUrl = root.optCleanString("privacy_policy_url")
      val socialLink = root.optCleanString("social_link")
      val adsEnabled = root.optFlexibleBoolean("ads_enabled", false)
      val bannerAdId = root.optCleanString("banner_ad_id")
      val interstitialAdId = root.optCleanString("interstitial_ad_id")
      val announcementTitle = root.optCleanString("announcement_title")
      val announcementMessage = root.optCleanString("announcement_message")

      val notifications = mutableListOf<ShayariNotification>()
      val seenNotificationIds = mutableSetOf<Int>()
      val notificationsJson = root.optJSONArray("notifications")
      if (notificationsJson != null) {
        for (i in 0 until notificationsJson.length()) {
          val obj = notificationsJson.optJSONObject(i) ?: continue
          val parsed = ShayariNotification.fromJson(obj, fallbackId = i + 1) ?: continue
          val uniqueId =
            if (seenNotificationIds.add(parsed.id)) {
              parsed.id
            } else {
              var candidate = (seenNotificationIds.maxOrNull() ?: 0) + 1
              while (!seenNotificationIds.add(candidate)) candidate++
              candidate
            }
          notifications.add(if (uniqueId == parsed.id) parsed else parsed.copy(id = uniqueId))
        }
      }
      if (
        notifications.isEmpty() &&
          (announcementTitle.isNotBlank() || announcementMessage.isNotBlank())
      ) {
        notifications.add(
          ShayariNotification(
            id = 1,
            title = announcementTitle.ifBlank { "Latest Announcement" },
            message = announcementMessage,
            badge = "Announcement",
            timeLabel = "Pinned",
          )
        )
      }

      val categories = mutableListOf<String>()
      val categoriesJson = root.optJSONArray("categories")
      if (categoriesJson != null) {
        for (i in 0 until categoriesJson.length()) {
          val cat = categoriesJson.optString(i, "").trim()
          if (cat.isNotEmpty() && !cat.equals("null", ignoreCase = true) && categories.none {
              it.equals(cat, ignoreCase = true)
            }
          ) {
            categories.add(cat)
          }
        }
      }
      if (categories.isEmpty()) {
        categories.addAll(DEFAULT_CATEGORIES)
      } else if (
        categories.none {
          it.equals("All", ignoreCase = true) || it.equals("Popular", ignoreCase = true)
        }
      ) {
        categories.add(0, "All")
      }

      val shayariItems = mutableListOf<ShayariItem>()
      val seenShayariIds = mutableSetOf<Int>()
      val listJson = root.optJSONArray("shayari_list")
      if (listJson != null) {
        for (i in 0 until listJson.length()) {
          val itemObj = listJson.optJSONObject(i) ?: continue
          val parsed = ShayariItem.fromJson(itemObj, fallbackId = i + 1) ?: continue
          val uniqueId =
            if (seenShayariIds.add(parsed.id)) {
              parsed.id
            } else {
              var candidate = (seenShayariIds.maxOrNull() ?: 0) + 1
              while (!seenShayariIds.add(candidate)) candidate++
              candidate
            }
          shayariItems.add(if (uniqueId == parsed.id) parsed else parsed.copy(id = uniqueId))
        }
      }

      return ShayariConfig(
        appName = appName,
        latestVersionCode = latestVersionCode,
        forceUpdateUrl = forceUpdateUrl,
        maintenanceMode = maintenanceMode,
        maintenanceMessage = maintenanceMessage,
        privacyPolicyUrl = privacyPolicyUrl,
        socialLink = socialLink,
        adsEnabled = adsEnabled,
        bannerAdId = bannerAdId,
        interstitialAdId = interstitialAdId,
        announcementTitle = announcementTitle,
        announcementMessage = announcementMessage,
        notifications = notifications,
        categories = categories,
        shayariList = shayariItems,
      )
    }
  }
}

private fun JSONObject.optCleanString(key: String): String {
  if (isNull(key)) return ""
  return optString(key, "").trim().let { if (it.equals("null", ignoreCase = true)) "" else it }
}

private fun JSONObject.optFlexibleInt(key: String, defaultValue: Int): Int {
  if (!has(key) || isNull(key)) return defaultValue
  return when (val raw = opt(key)) {
    is Number -> raw.toInt()
    is String -> raw.trim().toIntOrNull() ?: defaultValue
    else -> defaultValue
  }
}

private fun JSONObject.optFlexibleBoolean(key: String, defaultValue: Boolean): Boolean {
  if (!has(key) || isNull(key)) return defaultValue
  return when (val raw = opt(key)) {
    is Boolean -> raw
    is String ->
      when (raw.trim().lowercase()) {
        "true", "1", "yes" -> true
        "false", "0", "no" -> false
        else -> defaultValue
      }
    is Number -> raw.toInt() != 0
    else -> defaultValue
  }
}
