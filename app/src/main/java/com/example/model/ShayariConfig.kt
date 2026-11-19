package com.example.model

import org.json.JSONArray
import org.json.JSONObject

data class SupportedLanguage(
  val code: String,
  val name: String,
  val nativeName: String = name,
  val isRtl: Boolean = code.lowercase() in RTL_LANGUAGE_CODES,
) {
  companion object {
    val RTL_LANGUAGE_CODES = setOf("ur", "ar", "fa", "he", "sd", "ps")

    fun fromAny(raw: Any?): SupportedLanguage? =
      when (raw) {
        is JSONObject -> {
          val code = raw.optCleanString("code").ifEmpty { raw.optCleanString("id") }.lowercase()
          val name = raw.optCleanString("name").ifEmpty { labelForCode(code) }
          val native = raw.optCleanString("native_name").ifEmpty { raw.optCleanString("nativeName") }.ifEmpty { name }
          if (code.isBlank() && name.isBlank()) null
          else {
            val resolvedCode = code.ifBlank { name.take(2).lowercase() }
            val rtl =
              if (raw.has("rtl") || raw.has("is_rtl") || raw.has("isRtl")) {
                raw.optFlexibleBoolean("rtl", raw.optFlexibleBoolean("is_rtl", raw.optFlexibleBoolean("isRtl", resolvedCode in RTL_LANGUAGE_CODES)))
              } else {
                resolvedCode in RTL_LANGUAGE_CODES
              }
            SupportedLanguage(
              code = resolvedCode,
              name = name,
              nativeName = native,
              isRtl = rtl,
            )
          }
        }
        is String -> {
          val clean = raw.trim()
          if (clean.isEmpty() || clean.equals("null", ignoreCase = true)) null
          else {
            val code = clean.lowercase().takeIf { it.length <= 5 } ?: codeForName(clean)
            SupportedLanguage(
              code = code,
              name = labelForCode(code),
              nativeName = nativeForCode(code),
              isRtl = code in RTL_LANGUAGE_CODES,
            )
          }
        }
        else -> null
      }

    fun labelForCode(code: String): String =
      when (code.lowercase()) {
        "hi" -> "Hindi"
        "en" -> "English"
        "ur" -> "Urdu"
        "pa" -> "Punjabi"
        "mr" -> "Marathi"
        "gu" -> "Gujarati"
        "bn" -> "Bengali"
        "ar" -> "Arabic"
        else -> code.uppercase()
      }

    fun nativeForCode(code: String): String =
      when (code.lowercase()) {
        "hi" -> "हिन्दी"
        "en" -> "English"
        "ur" -> "اردو"
        "pa" -> "ਪੰਜਾਬੀ"
        "mr" -> "मराठी"
        "gu" -> "ગુજરાતી"
        "bn" -> "বাংলা"
        "ar" -> "العربية"
        else -> labelForCode(code)
      }

    private fun codeForName(name: String): String =
      when (name.trim().lowercase()) {
        "hindi", "हिन्दी" -> "hi"
        "english" -> "en"
        "urdu", "اردو" -> "ur"
        "punjabi", "ਪੰਜਾਬੀ" -> "pa"
        "arabic", "العربية" -> "ar"
        else -> name.trim().take(2).lowercase()
      }
  }
}

data class SupportedCountry(
  val code: String,
  val name: String,
  val flagEmoji: String = flagForCode(code),
) {
  companion object {
    fun fromAny(raw: Any?): SupportedCountry? =
      when (raw) {
        is JSONObject -> {
          val code = raw.optCleanString("code").ifEmpty { raw.optCleanString("id") }.uppercase()
          val name = raw.optCleanString("name").ifEmpty { nameForCode(code) }
          val flag = raw.optCleanString("flag").ifEmpty { raw.optCleanString("emoji") }.ifEmpty { flagForCode(code) }
          if (code.isBlank() && name.isBlank()) null
          else SupportedCountry(code = code.ifBlank { name.take(2).uppercase() }, name = name, flagEmoji = flag)
        }
        is String -> {
          val clean = raw.trim()
          if (clean.isEmpty() || clean.equals("null", ignoreCase = true)) null
          else {
            val code = if (clean.length <= 3) clean.uppercase() else codeForName(clean)
            SupportedCountry(code = code, name = nameForCode(code), flagEmoji = flagForCode(code))
          }
        }
        else -> null
      }

    fun nameForCode(code: String): String =
      when (code.uppercase()) {
        "IN" -> "India"
        "PK" -> "Pakistan"
        "AE" -> "UAE"
        "SA" -> "Saudi Arabia"
        "US" -> "USA"
        "GB", "UK" -> "United Kingdom"
        "CA" -> "Canada"
        else -> code.uppercase()
      }

    fun flagForCode(code: String): String =
      when (code.uppercase()) {
        "IN" -> "🇮🇳"
        "PK" -> "🇵🇰"
        "AE" -> "🇦🇪"
        "SA" -> "🇸🇦"
        "US" -> "🇺🇸"
        "GB", "UK" -> "🇬🇧"
        "CA" -> "🇨🇦"
        else -> "🌐"
      }

    private fun codeForName(name: String): String =
      when (name.trim().lowercase()) {
        "india" -> "IN"
        "pakistan" -> "PK"
        "uae", "united arab emirates" -> "AE"
        "saudi arabia" -> "SA"
        "usa", "united states" -> "US"
        "uk", "united kingdom" -> "GB"
        else -> name.trim().take(2).uppercase()
      }
  }
}

data class DynamicThemeConfig(
  val primaryHex: String = "#7B1CFA",
  val accentHex: String = "#FF2E93",
  val darkMode: Boolean = true,
  val cardStyle: String = "royal_glass",
) {
  companion object {
    fun fromJson(json: JSONObject?): DynamicThemeConfig {
      if (json == null) return DynamicThemeConfig()
      val primary =
        json.optCleanString("primary_color")
          .ifEmpty { json.optCleanString("primaryColor") }
          .ifEmpty { json.optCleanString("primaryHex") }
          .ifEmpty { "#7B1CFA" }
      val accent =
        json.optCleanString("accent_color")
          .ifEmpty { json.optCleanString("accentColor") }
          .ifEmpty { json.optCleanString("secondary_color") }
          .ifEmpty { "#FF2E93" }
      val dark =
        if (json.has("dark_mode")) json.optFlexibleBoolean("dark_mode", true)
        else json.optFlexibleBoolean("darkMode", true)
      val style =
        json.optCleanString("card_style")
          .ifEmpty { json.optCleanString("cardStyle") }
          .ifEmpty { "royal_glass" }
      return DynamicThemeConfig(
        primaryHex = primary,
        accentHex = accent,
        darkMode = dark,
        cardStyle = style,
      )
    }
  }
}

data class ShayariPollOption(
  val id: String,
  val label: String,
  val votes: Int = 0,
)

data class ShayariPoll(
  val id: String,
  val question: String,
  val options: List<ShayariPollOption>,
) {
  val totalVotes: Int
    get() = options.sumOf { it.votes.coerceAtLeast(0) }

  companion object {
    fun fromJson(json: JSONObject?, fallbackIndex: Int = 1): ShayariPoll? {
      if (json == null) return null
      val question = json.optCleanString("question").ifEmpty { json.optCleanString("title") }
      if (question.isBlank()) return null
      val id = json.optCleanString("id").ifEmpty { "poll_$fallbackIndex" }
      val optionsArr = json.optJSONArray("options") ?: return null
      val parsedOptions = mutableListOf<ShayariPollOption>()
      for (i in 0 until optionsArr.length()) {
        val raw = optionsArr.opt(i)
        when (raw) {
          is JSONObject -> {
            val label = raw.optCleanString("label").ifEmpty { raw.optCleanString("text") }
            if (label.isNotBlank()) {
              val optId = raw.optCleanString("id").ifEmpty { "opt_$i" }
              val votes = raw.optFlexibleInt("votes", raw.optFlexibleInt("count", 0)).coerceAtLeast(0)
              parsedOptions += ShayariPollOption(id = optId, label = label, votes = votes)
            }
          }
          is String -> {
            val label = raw.trim()
            if (label.isNotBlank()) {
              parsedOptions += ShayariPollOption(id = "opt_$i", label = label, votes = 0)
            }
          }
        }
      }
      if (parsedOptions.size < 2) return null
      return ShayariPoll(id = id, question = question, options = parsedOptions)
    }
  }
}

data class EngagementConfig(
  val dailyStreakEnabled: Boolean = true,
  val whatsappShareEnabled: Boolean = true,
  val polls: List<ShayariPoll> = emptyList(),
)

/**
 * Pure native data models for Shayari Lok remote configuration.
 * Parsed using Android's built-in org.json to keep APK size ultra-lightweight.
 */
data class ShayariItem(
  val id: Int,
  val category: String,
  val text: String,
  val author: String = "",
  val imageUrl: String = "",
  val isAdult: Boolean = false,
) {
  fun toJson(): JSONObject =
    JSONObject().apply {
      put("id", id)
      put("category", category)
      put("text", text)
      if (author.isNotBlank()) put("author", author)
      if (imageUrl.isNotBlank()) put("image_url", imageUrl)
      if (isAdult) put("is_adult", true)
    }

  companion object {
    private val IMAGE_KEYS =
      listOf("image_url", "image", "photo_url", "photo", "thumbnail", "thumbnail_url", "img")

    fun isAdultCategoryName(category: String): Boolean {
      val norm = category.trim().lowercase()
      return norm.contains("18+") ||
        norm.contains("adult") ||
        norm.contains("mature") ||
        norm.contains("nsfw") ||
        norm == "bold" ||
        norm == "hot"
    }

    fun fromJson(json: JSONObject, fallbackId: Int = -1): ShayariItem? {
      val rawId = json.optFlexibleInt("id", fallbackId)
      val parsedId = if (rawId >= 0) rawId else fallbackId.coerceAtLeast(1)
      val category = json.optCleanString("category").ifEmpty { "All" }
      val text = json.optCleanString("text").ifEmpty { json.optCleanString("shayari") }
      val author = json.optCleanString("author")
      val imageUrl =
        IMAGE_KEYS.firstNotNullOfOrNull { key ->
          json.optCleanString(key).takeIf { it.isNotEmpty() }
        } ?: ""
      val explicitAdult =
        json.optFlexibleBoolean(
          "is_adult",
          json.optFlexibleBoolean(
            "isAdult",
            json.optFlexibleBoolean(
              "adult",
              json.optFlexibleBoolean(
                "is_18_plus",
                json.optFlexibleBoolean("nsfw", json.optFlexibleBoolean("age_restricted", false)),
              ),
            ),
          ),
        )
      val adult = explicitAdult || isAdultCategoryName(category)
      if (text.isEmpty()) return null
      return ShayariItem(
        id = parsedId,
        category = category,
        text = text,
        author = author,
        imageUrl = imageUrl,
        isAdult = adult,
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
      val timeLabel = json.optCleanString("time_label").ifEmpty { json.optCleanString("timeLabel") }.ifEmpty { "Latest" }
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

data class ShayariFaq(
  val id: String,
  val question: String,
  val answer: String,
  val category: String = "General",
) {
  fun toJson(): JSONObject =
    JSONObject().apply {
      put("id", id)
      put("question", question)
      put("answer", answer)
      if (category.isNotBlank()) put("category", category)
    }

  companion object {
    fun fromJson(json: JSONObject?, fallbackIndex: Int = 1): ShayariFaq? {
      if (json == null) return null
      val question =
        json.optCleanString("question")
          .ifEmpty { json.optCleanString("q") }
          .ifEmpty { json.optCleanString("title") }
      val answer =
        json.optCleanString("answer")
          .ifEmpty { json.optCleanString("a") }
          .ifEmpty { json.optCleanString("body") }
          .ifEmpty { json.optCleanString("description") }
      if (question.isBlank() || answer.isBlank()) return null
      val id = json.optCleanString("id").ifEmpty { "faq_$fallbackIndex" }
      val category = json.optCleanString("category").ifEmpty { "General" }
      return ShayariFaq(
        id = id,
        question = question,
        answer = answer,
        category = category,
      )
    }
  }
}

data class ShayariPolicySection(
  val number: String,
  val title: String,
  val body: String,
) {
  fun toJson(): JSONObject =
    JSONObject().apply {
      put("number", number)
      put("title", title)
      put("body", body)
    }

  companion object {
    fun fromJson(json: JSONObject?, fallbackIndex: Int = 1): ShayariPolicySection? {
      if (json == null) return null
      val title = json.optCleanString("title").ifEmpty { json.optCleanString("heading") }
      val body =
        json.optCleanString("body")
          .ifEmpty { json.optCleanString("content") }
          .ifEmpty { json.optCleanString("description") }
          .ifEmpty { json.optCleanString("text") }
      if (title.isBlank() && body.isBlank()) return null
      val number =
        json.optCleanString("number")
          .ifEmpty { fallbackIndex.toString().padStart(2, '0') }
      return ShayariPolicySection(
        number = number,
        title = title.ifBlank { "Policy $number" },
        body = body,
      )
    }
  }
}

data class ShayariBanner(
  val id: String,
  val title: String,
  val subtitle: String = "",
  val imageUrl: String = "",
  val linkUrl: String = "",
  val ctaText: String = "",
  val badge: String = "",
) {
  fun toJson(): JSONObject =
    JSONObject().apply {
      put("id", id)
      if (title.isNotBlank()) put("title", title)
      if (subtitle.isNotBlank()) put("subtitle", subtitle)
      if (imageUrl.isNotBlank()) put("image_url", imageUrl)
      if (linkUrl.isNotBlank()) put("link_url", linkUrl)
      if (ctaText.isNotBlank()) put("cta_text", ctaText)
      if (badge.isNotBlank()) put("badge", badge)
    }

  companion object {
    private val IMAGE_KEYS =
      listOf("image_url", "imageUrl", "banner_url", "bannerUrl", "image", "photo_url", "thumbnail", "thumbnail_url")
    private val LINK_KEYS =
      listOf("link_url", "linkUrl", "redirect_url", "redirectUrl", "click_url", "clickUrl", "action_url", "actionUrl", "target_url", "targetUrl", "url", "href")

    fun fromJson(json: JSONObject?, fallbackIndex: Int = 1): ShayariBanner? {
      if (json == null) return null
      val isEnabled =
        if (json.has("enabled")) json.optFlexibleBoolean("enabled", true)
        else if (json.has("active")) json.optFlexibleBoolean("active", true)
        else json.optFlexibleBoolean("is_active", true)
      if (!isEnabled) return null

      val id = json.optCleanString("id").ifEmpty { "banner_$fallbackIndex" }
      val title =
        json.optCleanString("title")
          .ifEmpty { json.optCleanString("heading") }
          .ifEmpty { json.optCleanString("name") }
      val subtitle =
        json.optCleanString("subtitle")
          .ifEmpty { json.optCleanString("description") }
          .ifEmpty { json.optCleanString("body") }
          .ifEmpty { json.optCleanString("message") }
      val imageUrl =
        IMAGE_KEYS.firstNotNullOfOrNull { key ->
          json.optCleanString(key).takeIf { it.isNotEmpty() }
        } ?: ""
      val linkUrl =
        LINK_KEYS.firstNotNullOfOrNull { key ->
          json.optCleanString(key).takeIf { it.isNotEmpty() }
        } ?: ""
      val ctaText =
        json.optCleanString("cta_text")
          .ifEmpty { json.optCleanString("ctaText") }
          .ifEmpty { json.optCleanString("button_text") }
          .ifEmpty { json.optCleanString("buttonText") }
          .ifEmpty { json.optCleanString("cta") }
      val badge =
        json.optCleanString("badge")
          .ifEmpty { json.optCleanString("tag") }
          .ifEmpty { json.optCleanString("label") }

      if (title.isBlank() && imageUrl.isBlank() && subtitle.isBlank()) return null
      return ShayariBanner(
        id = id,
        title = title.ifBlank { "Featured Update" },
        subtitle = subtitle,
        imageUrl = imageUrl,
        linkUrl = linkUrl,
        ctaText = ctaText,
        badge = badge,
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
  val appLogoUrl: String = "",
  val privacyPolicyUrl: String = "",
  val privacyPolicyTitle: String = "",
  val privacyPolicyText: String = "",
  val privacyPolicySections: List<ShayariPolicySection> = emptyList(),
  val socialLink: String = "",
  val adsEnabled: Boolean = false,
  val admobAppId: String = "",
  val bannerAdId: String = "",
  val interstitialAdId: String = "",
  val nativeAdId: String = "",
  val announcementTitle: String = "",
  val announcementMessage: String = "",
  val notifications: List<ShayariNotification> = emptyList(),
  val faqs: List<ShayariFaq> = emptyList(),
  val banners: List<ShayariBanner> = emptyList(),
  val categories: List<String> = DEFAULT_CATEGORIES,
  val shayariList: List<ShayariItem> = emptyList(),
  val supportedLanguages: List<SupportedLanguage> = DEFAULT_LANGUAGES,
  val supportedCountries: List<SupportedCountry> = DEFAULT_COUNTRIES,
  val dynamicTheme: DynamicThemeConfig = DynamicThemeConfig(),
  val engagement: EngagementConfig = EngagementConfig(),
) {
  fun toJson(): String {
    val root = JSONObject()
    root.put("app_name", appName)
    root.put("latest_version_code", latestVersionCode)
    root.put("force_update_url", forceUpdateUrl)
    root.put("maintenance_mode", maintenanceMode)
    if (maintenanceMessage.isNotBlank()) root.put("maintenance_message", maintenanceMessage)
    if (appLogoUrl.isNotBlank()) root.put("app_logo_url", appLogoUrl)
    root.put("privacy_policy_url", privacyPolicyUrl)
    if (privacyPolicyTitle.isNotBlank()) root.put("privacy_policy_title", privacyPolicyTitle)
    if (privacyPolicyText.isNotBlank()) root.put("privacy_policy_text", privacyPolicyText)
    if (privacyPolicySections.isNotEmpty()) {
      val policyArr = JSONArray()
      privacyPolicySections.forEach { policyArr.put(it.toJson()) }
      root.put("privacy_policy_sections", policyArr)
    }
    root.put("social_link", socialLink)
    root.put("ads_enabled", adsEnabled)
    if (admobAppId.isNotBlank()) root.put("admob_app_id", admobAppId)
    root.put("banner_ad_id", bannerAdId)
    root.put("interstitial_ad_id", interstitialAdId)
    if (nativeAdId.isNotBlank()) root.put("native_ad_id", nativeAdId)
    if (announcementTitle.isNotBlank()) root.put("announcement_title", announcementTitle)
    if (announcementMessage.isNotBlank()) root.put("announcement_message", announcementMessage)

    val notificationArray = JSONArray()
    notifications.forEach { notificationArray.put(it.toJson()) }
    root.put("notifications", notificationArray)

    if (faqs.isNotEmpty()) {
      val faqArray = JSONArray()
      faqs.forEach { faqArray.put(it.toJson()) }
      root.put("faqs", faqArray)
    }

    if (banners.isNotEmpty()) {
      val bannerArray = JSONArray()
      banners.forEach { bannerArray.put(it.toJson()) }
      root.put("banners", bannerArray)
    }

    val categoryArray = JSONArray()
    categories.forEach { categoryArray.put(it) }
    root.put("categories", categoryArray)

    val listArray = JSONArray()
    shayariList.forEach { listArray.put(it.toJson()) }
    root.put("shayari_list", listArray)

    val langArray = JSONArray()
    supportedLanguages.forEach { lang ->
      langArray.put(
        JSONObject().apply {
          put("code", lang.code)
          put("name", lang.name)
          put("native_name", lang.nativeName)
          put("rtl", lang.isRtl)
        }
      )
    }
    root.put("supported_languages", langArray)

    val countryArray = JSONArray()
    supportedCountries.forEach { country ->
      countryArray.put(
        JSONObject().apply {
          put("code", country.code)
          put("name", country.name)
          put("flag", country.flagEmoji)
        }
      )
    }
    root.put("supported_countries", countryArray)

    root.put(
      "dynamic_theme",
      JSONObject().apply {
        put("primary_color", dynamicTheme.primaryHex)
        put("accent_color", dynamicTheme.accentHex)
        put("dark_mode", dynamicTheme.darkMode)
        put("card_style", dynamicTheme.cardStyle)
      },
    )

    val pollsArray = JSONArray()
    engagement.polls.forEach { poll ->
      val opts = JSONArray()
      poll.options.forEach { opt ->
        opts.put(
          JSONObject().apply {
            put("id", opt.id)
            put("label", opt.label)
            put("votes", opt.votes)
          }
        )
      }
      pollsArray.put(
        JSONObject().apply {
          put("id", poll.id)
          put("question", poll.question)
          put("options", opts)
        }
      )
    }
    root.put(
      "engagement",
      JSONObject().apply {
        put("daily_streak_enabled", engagement.dailyStreakEnabled)
        put("whatsapp_share_enabled", engagement.whatsappShareEnabled)
        put("polls", pollsArray)
      },
    )

    return root.toString(2)
  }

  companion object {
    const val DEFAULT_MAINTENANCE_MESSAGE =
      "हम ऐप को और बेहतर बना रहे हैं। कृपया कुछ समय बाद पुनः प्रयास करें।"
    val DEFAULT_CATEGORIES = listOf("All", "Love", "Attitude", "Motivation", "Sad")
    val DEFAULT_LANGUAGES =
      listOf(
        SupportedLanguage(code = "hi", name = "Hindi", nativeName = "हिन्दी", isRtl = false),
        SupportedLanguage(code = "en", name = "English", nativeName = "English", isRtl = false),
        SupportedLanguage(code = "ur", name = "Urdu", nativeName = "اردو", isRtl = true),
        SupportedLanguage(code = "pa", name = "Punjabi", nativeName = "ਪੰਜਾਬੀ", isRtl = false),
      )
    val DEFAULT_COUNTRIES =
      listOf(
        SupportedCountry(code = "IN", name = "India", flagEmoji = "🇮🇳"),
        SupportedCountry(code = "PK", name = "Pakistan", flagEmoji = "🇵🇰"),
        SupportedCountry(code = "AE", name = "UAE", flagEmoji = "🇦🇪"),
        SupportedCountry(code = "US", name = "USA", flagEmoji = "🇺🇸"),
        SupportedCountry(code = "GB", name = "United Kingdom", flagEmoji = "🇬🇧"),
      )

    fun fromFirestoreMap(data: Map<String, Any?>, fallback: ShayariConfig? = null): ShayariConfig {
      val nestedConfigJson = data["configJson"] as? String ?: data["config_json"] as? String
      if (!nestedConfigJson.isNullOrBlank()) {
        try {
          return fromJson(nestedConfigJson, fallback = fallback)
        } catch (_: Exception) {
          // Fall through to structured map conversion
        }
      }
      val nestedMap =
        (data["config"] as? Map<*, *>)
          ?: (data["siteConfig"] as? Map<*, *>)
          ?: (data["site_config"] as? Map<*, *>)
      val mergedMap =
        buildMap<String, Any?> {
          putAll(data)
          nestedMap?.forEach { (k, v) ->
            if (k is String) put(k, v)
          }
        }
      val rootJson = anyToJsonValue(mergedMap) as? JSONObject ?: JSONObject()
      return fromJsonObject(rootJson, fallback = fallback)
    }

    fun fromJson(rawJson: String, fallback: ShayariConfig? = null): ShayariConfig {
      val sanitized = rawJson.trimStart('\uFEFF').trim()
      val root = JSONObject(sanitized)
      return fromJsonObject(root, fallback = fallback)
    }

    fun fromJsonObject(root: JSONObject, fallback: ShayariConfig? = null): ShayariConfig {
      val appName =
        root.optCleanString("app_name")
          .ifEmpty { root.optCleanString("appName") }
          .ifEmpty { fallback?.appName ?: "Shayari Lok" }
      val latestVersionCode =
        root.optFlexibleInt(
          "latest_version_code",
          root.optFlexibleInt("latestVersionCode", fallback?.latestVersionCode ?: 1),
        ).coerceAtLeast(1)
      val forceUpdateUrl =
        root.optCleanString("force_update_url")
          .ifEmpty { root.optCleanString("forceUpdateUrl") }
          .ifEmpty { fallback?.forceUpdateUrl ?: "" }
      val maintenanceMode =
        if (root.has("maintenance_mode")) root.optFlexibleBoolean("maintenance_mode", false)
        else if (root.has("maintenanceMode")) root.optFlexibleBoolean("maintenanceMode", false)
        else fallback?.maintenanceMode ?: false
      val maintenanceMessage =
        root.optCleanString("maintenance_message")
          .ifEmpty { root.optCleanString("maintenanceMessage") }
          .ifEmpty { fallback?.maintenanceMessage ?: DEFAULT_MAINTENANCE_MESSAGE }
      val appLogoUrl =
        root.optCleanString("app_logo_url")
          .ifEmpty { root.optCleanString("appLogoUrl") }
          .ifEmpty { root.optCleanString("logo_url") }
          .ifEmpty { root.optCleanString("logoUrl") }
          .ifEmpty { root.optCleanString("site_logo") }
          .ifEmpty { root.optCleanString("logo") }
          .ifEmpty { fallback?.appLogoUrl ?: "" }
      val privacyPolicyUrl =
        root.optCleanString("privacy_policy_url")
          .ifEmpty { root.optCleanString("privacyPolicyUrl") }
          .ifEmpty { fallback?.privacyPolicyUrl ?: "" }
      val privacyPolicyTitle =
        root.optCleanString("privacy_policy_title")
          .ifEmpty { root.optCleanString("privacyPolicyTitle") }
          .ifEmpty { fallback?.privacyPolicyTitle ?: "" }
      val privacyPolicyText =
        root.optCleanString("privacy_policy_text")
          .ifEmpty { root.optCleanString("privacyPolicyText") }
          .ifEmpty { root.optCleanString("privacy_policy_content") }
          .ifEmpty { root.optCleanString("privacyPolicyContent") }
          .ifEmpty { root.optCleanString("privacy_policy") }
          .ifEmpty { root.optCleanString("privacyPolicy") }
          .ifEmpty { fallback?.privacyPolicyText ?: "" }
      val privacySectionsJson =
        root.optJSONArray("privacy_policy_sections")
          ?: root.optJSONArray("privacyPolicySections")
          ?: root.optJSONArray("privacy_sections")
          ?: root.optJSONArray("privacySections")
      val parsedPolicySections =
        if (privacySectionsJson != null) {
          buildList {
            for (i in 0 until privacySectionsJson.length()) {
              ShayariPolicySection.fromJson(privacySectionsJson.optJSONObject(i), fallbackIndex = i + 1)
                ?.let { add(it) }
            }
          }
        } else {
          fallback?.privacyPolicySections ?: emptyList()
        }
      val socialLink =
        root.optCleanString("social_link")
          .ifEmpty { root.optCleanString("socialLink") }
          .ifEmpty { fallback?.socialLink ?: "" }

      val admobObj = root.optJSONObject("admob") ?: root.optJSONObject("ads")
      val adsEnabled =
        when {
          root.has("ads_enabled") -> root.optFlexibleBoolean("ads_enabled", false)
          root.has("adsEnabled") -> root.optFlexibleBoolean("adsEnabled", false)
          admobObj?.has("enabled") == true -> admobObj.optFlexibleBoolean("enabled", false)
          admobObj?.has("ads_enabled") == true -> admobObj.optFlexibleBoolean("ads_enabled", false)
          admobObj?.has("adsEnabled") == true -> admobObj.optFlexibleBoolean("adsEnabled", false)
          else -> fallback?.adsEnabled ?: false
        }
      val admobAppId =
        root.optCleanString("admob_app_id")
          .ifEmpty { root.optCleanString("admobAppId") }
          .ifEmpty { admobObj?.optCleanString("app_id").orEmpty() }
          .ifEmpty { admobObj?.optCleanString("appId").orEmpty() }
          .ifEmpty { fallback?.admobAppId ?: "" }
      val bannerAdId =
        root.optCleanString("banner_ad_id")
          .ifEmpty { root.optCleanString("bannerAdId") }
          .ifEmpty { admobObj?.optCleanString("banner_ad_id").orEmpty() }
          .ifEmpty { admobObj?.optCleanString("bannerId").orEmpty() }
          .ifEmpty { fallback?.bannerAdId ?: "" }
      val interstitialAdId =
        root.optCleanString("interstitial_ad_id")
          .ifEmpty { root.optCleanString("interstitialAdId") }
          .ifEmpty { admobObj?.optCleanString("interstitial_ad_id").orEmpty() }
          .ifEmpty { admobObj?.optCleanString("interstitialId").orEmpty() }
          .ifEmpty { fallback?.interstitialAdId ?: "" }
      val nativeAdId =
        root.optCleanString("native_ad_id")
          .ifEmpty { root.optCleanString("nativeAdId") }
          .ifEmpty { admobObj?.optCleanString("native_ad_id").orEmpty() }
          .ifEmpty { admobObj?.optCleanString("nativeId").orEmpty() }
          .ifEmpty { fallback?.nativeAdId ?: "" }
      val announcementTitle =
        root.optCleanString("announcement_title")
          .ifEmpty { root.optCleanString("announcementTitle") }
          .ifEmpty { fallback?.announcementTitle ?: "" }
      val announcementMessage =
        root.optCleanString("announcement_message")
          .ifEmpty { root.optCleanString("announcementMessage") }
          .ifEmpty { fallback?.announcementMessage ?: "" }

      val notifications = mutableListOf<ShayariNotification>()
      val seenNotificationIds = mutableSetOf<Int>()
      val notificationsJson = root.optJSONArray("notifications")
      if (notificationsJson != null) {
        for (i in 0 until notificationsJson.length()) {
          val obj = notificationsJson.optJSONObject(i) ?: continue
          val parsed = ShayariNotification.fromJson(obj, fallbackId = i + 1) ?: continue
          val uniqueId =
            if (seenNotificationIds.add(parsed.id)) parsed.id
            else {
              var candidate = (seenNotificationIds.maxOrNull() ?: 0) + 1
              while (!seenNotificationIds.add(candidate)) candidate++
              candidate
            }
          notifications.add(if (uniqueId == parsed.id) parsed else parsed.copy(id = uniqueId))
        }
      } else if (fallback != null && fallback.notifications.isNotEmpty()) {
        notifications.addAll(fallback.notifications)
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

      val faqsJson =
        root.optJSONArray("faqs")
          ?: root.optJSONArray("faq_list")
          ?: root.optJSONArray("faqList")
          ?: root.optJSONArray("faq")
      val parsedFaqs =
        if (faqsJson != null) {
          buildList {
            for (i in 0 until faqsJson.length()) {
              ShayariFaq.fromJson(faqsJson.optJSONObject(i), fallbackIndex = i + 1)?.let { add(it) }
            }
          }
        } else {
          fallback?.faqs ?: emptyList()
        }

      val bannersJson =
        root.optJSONArray("banners")
          ?: root.optJSONArray("dynamic_banners")
          ?: root.optJSONArray("dynamicBanners")
          ?: root.optJSONArray("promo_banners")
          ?: root.optJSONArray("promoBanners")
          ?: root.optJSONArray("banner_list")
          ?: root.optJSONArray("bannerList")
      val parsedBanners =
        if (bannersJson != null) {
          buildList {
            for (i in 0 until bannersJson.length()) {
              ShayariBanner.fromJson(bannersJson.optJSONObject(i), fallbackIndex = i + 1)?.let { add(it) }
            }
          }
        } else {
          fallback?.banners ?: emptyList()
        }

      val categories = mutableListOf<String>()
      val categoriesJson = root.optJSONArray("categories")
      if (categoriesJson != null) {
        for (i in 0 until categoriesJson.length()) {
          val cat = categoriesJson.optString(i, "").trim()
          if (cat.isNotEmpty() && !cat.equals("null", ignoreCase = true) && categories.none { it.equals(cat, ignoreCase = true) }) {
            categories.add(cat)
          }
        }
      } else if (fallback != null && fallback.categories.isNotEmpty()) {
        categories.addAll(fallback.categories)
      }
      if (categories.isEmpty()) {
        categories.addAll(DEFAULT_CATEGORIES)
      } else if (categories.none { it.equals("All", ignoreCase = true) || it.equals("Popular", ignoreCase = true) }) {
        categories.add(0, "All")
      }

      val shayariItems = mutableListOf<ShayariItem>()
      val seenShayariIds = mutableSetOf<Int>()
      val listJson = root.optJSONArray("shayari_list") ?: root.optJSONArray("shayariList")
      if (listJson != null) {
        for (i in 0 until listJson.length()) {
          val itemObj = listJson.optJSONObject(i) ?: continue
          val parsed = ShayariItem.fromJson(itemObj, fallbackId = i + 1) ?: continue
          val uniqueId =
            if (seenShayariIds.add(parsed.id)) parsed.id
            else {
              var candidate = (seenShayariIds.maxOrNull() ?: 0) + 1
              while (!seenShayariIds.add(candidate)) candidate++
              candidate
            }
          shayariItems.add(if (uniqueId == parsed.id) parsed else parsed.copy(id = uniqueId))
        }
      } else if (fallback != null && fallback.shayariList.isNotEmpty()) {
        shayariItems.addAll(fallback.shayariList)
      }

      val langsJson = root.optJSONArray("supported_languages") ?: root.optJSONArray("supportedLanguages")
      val parsedLangs =
        if (langsJson != null) {
          buildList<SupportedLanguage> {
            for (i in 0 until langsJson.length()) {
              SupportedLanguage.fromAny(langsJson.opt(i))?.let { lang ->
                if (none { it.code.equals(lang.code, ignoreCase = true) }) add(lang)
              }
            }
          }.ifEmpty { fallback?.supportedLanguages ?: DEFAULT_LANGUAGES }
        } else {
          fallback?.supportedLanguages ?: DEFAULT_LANGUAGES
        }

      val countriesJson = root.optJSONArray("supported_countries") ?: root.optJSONArray("supportedCountries")
      val parsedCountries =
        if (countriesJson != null) {
          buildList<SupportedCountry> {
            for (i in 0 until countriesJson.length()) {
              SupportedCountry.fromAny(countriesJson.opt(i))?.let { country ->
                if (none { it.code.equals(country.code, ignoreCase = true) }) add(country)
              }
            }
          }.ifEmpty { fallback?.supportedCountries ?: DEFAULT_COUNTRIES }
        } else {
          fallback?.supportedCountries ?: DEFAULT_COUNTRIES
        }

      val themeObj =
        root.optJSONObject("dynamic_theme")
          ?: root.optJSONObject("dynamicTheme")
          ?: root.optJSONObject("theme")
      val parsedTheme =
        if (themeObj != null) DynamicThemeConfig.fromJson(themeObj)
        else (fallback?.dynamicTheme ?: DynamicThemeConfig())

      val engagementObj = root.optJSONObject("engagement")
      val dailyStreakEnabled =
        when {
          engagementObj?.has("daily_streak_enabled") == true -> engagementObj.optFlexibleBoolean("daily_streak_enabled", true)
          engagementObj?.has("dailyStreakEnabled") == true -> engagementObj.optFlexibleBoolean("dailyStreakEnabled", true)
          root.has("daily_streak_enabled") -> root.optFlexibleBoolean("daily_streak_enabled", true)
          root.has("dailyStreakEnabled") -> root.optFlexibleBoolean("dailyStreakEnabled", true)
          else -> fallback?.engagement?.dailyStreakEnabled ?: true
        }
      val whatsappEnabled =
        when {
          engagementObj?.has("whatsapp_share_enabled") == true -> engagementObj.optFlexibleBoolean("whatsapp_share_enabled", true)
          engagementObj?.has("whatsappShareEnabled") == true -> engagementObj.optFlexibleBoolean("whatsappShareEnabled", true)
          root.has("whatsapp_share_enabled") -> root.optFlexibleBoolean("whatsapp_share_enabled", true)
          root.has("whatsappShareEnabled") -> root.optFlexibleBoolean("whatsappShareEnabled", true)
          else -> fallback?.engagement?.whatsappShareEnabled ?: true
        }
      val pollsArray = engagementObj?.optJSONArray("polls") ?: root.optJSONArray("polls")
      val singleActivePoll =
        engagementObj?.optJSONObject("active_poll")
          ?: root.optJSONObject("active_poll")
          ?: root.optJSONObject("activePoll")
      val parsedPolls =
        when {
          pollsArray != null -> {
            buildList {
              for (i in 0 until pollsArray.length()) {
                ShayariPoll.fromJson(pollsArray.optJSONObject(i), fallbackIndex = i + 1)?.let { add(it) }
              }
            }
          }
          singleActivePoll != null -> listOfNotNull(ShayariPoll.fromJson(singleActivePoll, fallbackIndex = 1))
          else -> fallback?.engagement?.polls ?: emptyList()
        }

      return ShayariConfig(
        appName = appName,
        latestVersionCode = latestVersionCode,
        forceUpdateUrl = forceUpdateUrl,
        maintenanceMode = maintenanceMode,
        maintenanceMessage = maintenanceMessage,
        appLogoUrl = appLogoUrl,
        privacyPolicyUrl = privacyPolicyUrl,
        privacyPolicyTitle = privacyPolicyTitle,
        privacyPolicyText = privacyPolicyText,
        privacyPolicySections = parsedPolicySections,
        socialLink = socialLink,
        adsEnabled = adsEnabled,
        admobAppId = admobAppId,
        bannerAdId = bannerAdId,
        interstitialAdId = interstitialAdId,
        nativeAdId = nativeAdId,
        announcementTitle = announcementTitle,
        announcementMessage = announcementMessage,
        notifications = notifications,
        faqs = parsedFaqs,
        banners = parsedBanners,
        categories = categories,
        shayariList = shayariItems,
        supportedLanguages = parsedLangs,
        supportedCountries = parsedCountries,
        dynamicTheme = parsedTheme,
        engagement =
          EngagementConfig(
            dailyStreakEnabled = dailyStreakEnabled,
            whatsappShareEnabled = whatsappEnabled,
            polls = parsedPolls,
          ),
      )
    }

    private fun anyToJsonValue(value: Any?): Any? =
      when (value) {
        null -> JSONObject.NULL
        is Map<*, *> ->
          JSONObject().apply {
            value.forEach { (k, v) ->
              if (k is String) put(k, anyToJsonValue(v))
            }
          }
        is Iterable<*> ->
          JSONArray().apply {
            value.forEach { item -> put(anyToJsonValue(item)) }
          }
        is Boolean, is Number, is String -> value
        else -> value.toString()
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
