package com.example.model

import org.json.JSONArray
import org.json.JSONObject

/**
 * Pure native data models for Shayari Lok remote configuration.
 * Parsed using Android's built-in org.json to keep APK size ultra-lightweight.
 *
 * Schema matches the remote GitHub Raw config.json 1:1 so this file can
 * also be reused directly when building a connected Admin Panel.
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
    fun fromJson(json: JSONObject): ShayariItem? {
      val id = json.optInt("id", -1)
      val category = json.optString("category", "").trim()
      val text = json.optString("text", "").trim()
      val author = json.optString("author", "").trim()
      if (id < 0 || category.isEmpty() || text.isEmpty()) return null
      return ShayariItem(
        id = id,
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
      val id = json.optInt("id", fallbackId)
      val title = json.optString("title", "").trim()
      val message = json.optString("message", "").trim()
      if (title.isEmpty() && message.isEmpty()) return null
      val badge = json.optString("badge", "Update").trim().ifEmpty { "Update" }
      val timeLabel = json.optString("time_label", "Latest").trim().ifEmpty { "Latest" }
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
      "Shayari Lok is currently undergoing scheduled updates. Please check back shortly."
    val DEFAULT_CATEGORIES = listOf("Popular", "Love", "Sad", "Motivational", "Attitude")

    fun fromJson(rawJson: String): ShayariConfig {
      val root = JSONObject(rawJson)
      val appName = root.optString("app_name", "Shayari Lok").ifBlank { "Shayari Lok" }
      val latestVersionCode = root.optInt("latest_version_code", 1)
      val forceUpdateUrl = root.optString("force_update_url", "").trim()
      val maintenanceMode = root.optBoolean("maintenance_mode", false)
      val maintenanceMessage =
        root.optString("maintenance_message", DEFAULT_MAINTENANCE_MESSAGE)
          .ifBlank { DEFAULT_MAINTENANCE_MESSAGE }
      val privacyPolicyUrl = root.optString("privacy_policy_url", "").trim()
      val socialLink = root.optString("social_link", "").trim()
      val adsEnabled = root.optBoolean("ads_enabled", false)
      val bannerAdId = root.optString("banner_ad_id", "").trim()
      val interstitialAdId = root.optString("interstitial_ad_id", "").trim()
      val announcementTitle = root.optString("announcement_title", "").trim()
      val announcementMessage = root.optString("announcement_message", "").trim()

      val notifications = mutableListOf<ShayariNotification>()
      val notificationsJson = root.optJSONArray("notifications")
      if (notificationsJson != null) {
        for (i in 0 until notificationsJson.length()) {
          val obj = notificationsJson.optJSONObject(i) ?: continue
          ShayariNotification.fromJson(obj, fallbackId = i + 1)?.let { notifications.add(it) }
        }
      }
      if (notifications.isEmpty() && (announcementTitle.isNotBlank() || announcementMessage.isNotBlank())) {
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
          if (cat.isNotEmpty() && cat !in categories) {
            categories.add(cat)
          }
        }
      }
      if (categories.isEmpty()) {
        categories.addAll(DEFAULT_CATEGORIES)
      } else if (categories.none { it.equals("Popular", ignoreCase = true) || it.equals("All", ignoreCase = true) }) {
        categories.add(0, "Popular")
      }

      val shayariItems = mutableListOf<ShayariItem>()
      val listJson = root.optJSONArray("shayari_list")
      if (listJson != null) {
        for (i in 0 until listJson.length()) {
          val itemObj = listJson.optJSONObject(i) ?: continue
          ShayariItem.fromJson(itemObj)?.let { shayariItems.add(it) }
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
