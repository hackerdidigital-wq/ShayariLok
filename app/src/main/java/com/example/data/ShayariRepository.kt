package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.util.Base64
import com.example.R
import com.example.model.ShayariConfig
import com.example.model.ShayariItem
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject

enum class ShayariTextSize(val label: String, val fontSizeSp: Int, val lineHeightSp: Int) {
  COMPACT("Compact", 16, 26),
  MEDIUM("Standard", 18, 29),
  LARGE("Large", 21, 33),
}

/**
 * Ultra-lightweight data repository using pure Android HttpURLConnection,
 * org.json, ConnectivityManager, and SharedPreferences for live sync, real-time network
 * monitoring, offline caching, reading preferences, and favorites.
 */
class ShayariRepository(context: Context) {

  private val appContext = context.applicationContext
  private val prefs: SharedPreferences =
    appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  fun loadCachedConfig(): ShayariConfig {
    val cachedJson = prefs.getString(KEY_CACHED_CONFIG_JSON, null)
    if (!cachedJson.isNullOrBlank()) {
      try {
        return ShayariConfig.fromJson(cachedJson)
      } catch (_: Exception) {
        // Fallback to starter bundled config if cached JSON was corrupted
      }
    }
    return ShayariConfig.fromJson(DefaultShayariData.DEFAULT_CONFIG_JSON)
  }

  fun hasCachedRemoteConfig(): Boolean =
    !prefs.getString(KEY_CACHED_CONFIG_JSON, null).isNullOrBlank()

  fun getLastSyncedTimeMillis(): Long = prefs.getLong(KEY_LAST_SYNCED_AT, 0L)

  fun getShayariTextSize(): ShayariTextSize {
    val stored = prefs.getString(KEY_TEXT_SIZE, ShayariTextSize.MEDIUM.name)
    return ShayariTextSize.entries.firstOrNull { it.name == stored } ?: ShayariTextSize.MEDIUM
  }

  fun setShayariTextSize(size: ShayariTextSize) {
    prefs.edit().putString(KEY_TEXT_SIZE, size.name).apply()
  }

  fun hasUnreadNotifications(config: ShayariConfig): Boolean {
    if (config.notifications.isEmpty()) return false
    val latestSignature = buildNotificationSignature(config)
    val seenSignature = prefs.getString(KEY_SEEN_NOTIFICATIONS_SIG, null)
    return latestSignature != seenSignature
  }

  fun markNotificationsRead(config: ShayariConfig) {
    val signature = buildNotificationSignature(config)
    prefs.edit().putString(KEY_SEEN_NOTIFICATIONS_SIG, signature).apply()
  }

  private fun buildNotificationSignature(config: ShayariConfig): String =
    config.notifications.joinToString("|") { "${it.id}:${it.title}:${it.message}" }

  fun getFavoriteIds(): Set<Int> {
    val rawSet = prefs.getStringSet(KEY_FAVORITE_IDS, emptySet()) ?: emptySet()
    return rawSet.mapNotNull { it.toIntOrNull() }.toSet()
  }

  fun toggleFavorite(shayariId: Int): Set<Int> {
    val updated = getFavoriteIds().toMutableSet()
    if (!updated.add(shayariId)) {
      updated.remove(shayariId)
    }
    prefs.edit()
      .putStringSet(KEY_FAVORITE_IDS, updated.map { it.toString() }.toSet())
      .apply()
    return updated
  }

  fun getSelectedLanguageCode(): String =
    prefs.getString(KEY_SELECTED_LANGUAGE, "hi").orEmpty().ifBlank { "hi" }

  fun setSelectedLanguageCode(code: String) {
    prefs.edit().putString(KEY_SELECTED_LANGUAGE, code.trim().lowercase()).apply()
  }

  fun getSelectedCountryCode(): String =
    prefs.getString(KEY_SELECTED_COUNTRY, "IN").orEmpty().ifBlank { "IN" }

  fun setSelectedCountryCode(code: String) {
    prefs.edit().putString(KEY_SELECTED_COUNTRY, code.trim().uppercase()).apply()
  }

  fun isAdultContentAllowed(): Boolean = prefs.getBoolean(KEY_ALLOW_ADULT_18_PLUS, false)

  fun setAdultContentAllowed(allowed: Boolean) {
    prefs.edit()
      .putBoolean(KEY_ALLOW_ADULT_18_PLUS, allowed)
      .apply { if (allowed) putBoolean(KEY_AGE_VERIFIED_18_PLUS, true) }
      .apply()
  }

  fun isAgeVerified(): Boolean = prefs.getBoolean(KEY_AGE_VERIFIED_18_PLUS, false)

  fun getUserDarkModeOverride(): Boolean? =
    if (prefs.contains(KEY_USER_DARK_THEME)) prefs.getBoolean(KEY_USER_DARK_THEME, true)
    else null

  fun setUserDarkModeOverride(dark: Boolean) {
    prefs.edit().putBoolean(KEY_USER_DARK_THEME, dark).apply()
  }

  fun saveMergedOverlayConfig(config: ShayariConfig) {
    try {
      prefs.edit().putString(KEY_CACHED_OVERLAY_CONFIG_JSON, config.toJson()).apply()
    } catch (_: Exception) {
      // Ignore serialization failure
    }
  }

  fun loadEffectiveCachedConfig(): ShayariConfig {
    val overlayJson = prefs.getString(KEY_CACHED_OVERLAY_CONFIG_JSON, null)
    if (!overlayJson.isNullOrBlank()) {
      try {
        return ShayariConfig.fromJson(overlayJson)
      } catch (_: Exception) {
        // Fallback to cached remote config
      }
    }
    return loadCachedConfig()
  }

  suspend fun syncUserPreferencesToFirestore(
    db: FirebaseFirestore,
    uid: String,
    languageCode: String,
    adultContentEnabled: Boolean,
  ) {
    if (uid.isBlank()) return
    val userDoc = db.collection("users").document(uid)
    try {
      userDoc.set(
        mapOf(
          "selectedLanguage" to languageCode.trim().lowercase(),
          "adultContentEnabled" to adultContentEnabled,
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
        SetOptions.merge(),
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, userDoc.path)
    }
  }

  fun getDailyStreakDays(): Int = prefs.getInt(KEY_DAILY_STREAK_COUNT, 1).coerceAtLeast(1)

  fun recordDailyVisitAndGetStreak(todayKey: String = formatDateKey(Calendar.getInstance())): Int {
    val lastKey = prefs.getString(KEY_LAST_VISIT_DATE, null)
    val currentCount = prefs.getInt(KEY_DAILY_STREAK_COUNT, 0)
    if (lastKey == todayKey && currentCount > 0) {
      return currentCount
    }
    val nextCount =
      if (lastKey != null && isYesterday(lastKey, todayKey)) {
        (currentCount + 1).coerceAtLeast(1)
      } else {
        1
      }
    prefs.edit()
      .putString(KEY_LAST_VISIT_DATE, todayKey)
      .putInt(KEY_DAILY_STREAK_COUNT, nextCount)
      .apply()
    return nextCount
  }

  private fun isYesterday(previousKey: String, todayKey: String): Boolean {
    return try {
      val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
      val prevDate = fmt.parse(previousKey) ?: return false
      val cal = Calendar.getInstance().apply {
        time = prevDate
        add(Calendar.DAY_OF_YEAR, 1)
      }
      formatDateKey(cal) == todayKey
    } catch (_: Exception) {
      false
    }
  }

  private fun formatDateKey(cal: Calendar): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)

  fun getVotedPollOptions(): Map<String, String> {
    val raw = prefs.getString(KEY_POLL_VOTES_JSON, null) ?: return emptyMap()
    return try {
      val obj = JSONObject(raw)
      buildMap {
        obj.keys().forEach { key ->
          val value = obj.optString(key, "")
          if (key.isNotBlank() && value.isNotBlank()) put(key, value)
        }
      }
    } catch (_: Exception) {
      emptyMap()
    }
  }

  fun savePollVoteLocal(pollId: String, optionId: String) {
    val updated = getVotedPollOptions().toMutableMap()
    updated[pollId] = optionId
    val obj = JSONObject()
    updated.forEach { (k, v) -> obj.put(k, v) }
    prefs.edit().putString(KEY_POLL_VOTES_JSON, obj.toString()).apply()
  }

  suspend fun submitPollVoteToFirestore(
    db: FirebaseFirestore,
    adminSiteDocId: String,
    pollId: String,
    optionId: String,
    uid: String,
  ) {
    if (uid.isBlank() || pollId.isBlank() || optionId.isBlank()) return
    val voteDoc = db.collection("pollVotes").document("${pollId}_$uid")
    try {
      voteDoc.set(
        mapOf(
          "pollId" to pollId,
          "optionId" to optionId,
          "uid" to uid,
          "siteDocId" to adminSiteDocId,
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
        SetOptions.merge(),
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, voteDoc.path)
    }
  }

  suspend fun fetchRemoteConfig(
    endpoints: List<String> = DefaultShayariData.REMOTE_CONFIG_ENDPOINTS
  ): Result<ShayariConfig> =
    withContext(Dispatchers.IO) {
      if (!isNetworkAvailable()) {
        return@withContext Result.failure(IllegalStateException("Offline Mode: No Internet Connection"))
      }

      var lastError: Exception? = null
      for (endpoint in endpoints) {
        try {
          val rawResponse = fetchJsonFromUrl(buildCacheBustedUrl(endpoint))
          val rawJson = unwrapGitHubContentsPayloadIfNeeded(rawResponse)
          val parsedConfig = ShayariConfig.fromJson(rawJson)
          val now = System.currentTimeMillis()
          prefs.edit()
            .putString(KEY_CACHED_CONFIG_JSON, rawJson)
            .putLong(KEY_LAST_SYNCED_AT, now)
            .apply()
          return@withContext Result.success(parsedConfig)
        } catch (e: Exception) {
          lastError = e
        }
      }

      Result.failure(lastError ?: IllegalStateException("Unable to reach server config.json"))
    }

  internal fun buildCacheBustedUrl(
    endpoint: String,
    timestampMillis: Long = System.currentTimeMillis(),
  ): String {
    val separator = if (endpoint.contains("?")) "&" else "?"
    return "${endpoint}${separator}t=${timestampMillis}"
  }

  internal fun unwrapGitHubContentsPayloadIfNeeded(rawPayload: String): String {
    val trimmed = rawPayload.trimStart('\uFEFF').trim()
    val json = JSONObject(trimmed)
    val encoding = json.optString("encoding", "")
    val content = json.optString("content", "")
    if (encoding.equals("base64", ignoreCase = true) && content.isNotBlank() && !json.has("shayari_list")) {
      val cleanBase64 = content.replace("\n", "").replace("\r", "").trim()
      val decodedBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
      return String(decodedBytes, Charsets.UTF_8)
    }
    return trimmed
  }

  private fun fetchJsonFromUrl(endpoint: String): String {
    var connection: HttpURLConnection? = null
    try {
      val url = URL(endpoint)
      connection =
        (url.openConnection() as HttpURLConnection).apply {
          requestMethod = "GET"
          instanceFollowRedirects = true
          connectTimeout = CONNECT_TIMEOUT_MS
          readTimeout = READ_TIMEOUT_MS
          useCaches = false
          setRequestProperty(
            "Accept",
            "application/vnd.github.raw+json, application/json, text/plain;q=0.9, */*;q=0.8",
          )
          setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
          setRequestProperty("Pragma", "no-cache")
          setRequestProperty("User-Agent", "ShayariLok-Android/1.0")
        }

      val responseCode = connection.responseCode
      if (responseCode !in 200..299) {
        connection.errorStream?.close()
        throw IllegalStateException("HTTP $responseCode")
      }

      return BufferedReader(InputStreamReader(connection.inputStream, Charsets.UTF_8)).use { reader ->
        reader.readText()
      }
    } finally {
      connection?.disconnect()
    }
  }

  fun isNetworkAvailable(): Boolean {
    val connectivityManager =
      appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        ?: return false
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
  }

  /**
   * Emits real-time internet connectivity changes using Android's native
   * ConnectivityManager.NetworkCallback (API 24+).
   */
  fun observeNetworkConnectivity(): Flow<Boolean> =
    callbackFlow {
      trySend(isNetworkAvailable())

      val connectivityManager =
        appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
      if (connectivityManager == null) {
        awaitClose {}
        return@callbackFlow
      }

      val callback =
        object : ConnectivityManager.NetworkCallback() {
          override fun onAvailable(network: Network) {
            trySend(isNetworkAvailable())
          }

          override fun onLost(network: Network) {
            trySend(isNetworkAvailable())
          }

          override fun onUnavailable() {
            trySend(false)
          }

          override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities,
          ) {
            trySend(
              networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ||
                isNetworkAvailable()
            )
          }
        }

      val registered =
        try {
          connectivityManager.registerDefaultNetworkCallback(callback)
          true
        } catch (_: Exception) {
          false
        }

      awaitClose {
        if (registered) {
          try {
            connectivityManager.unregisterNetworkCallback(callback)
          } catch (_: Exception) {
            // Ignore if already unregistered
          }
        }
      }
    }.distinctUntilChanged()

  fun observeRealtimeShayariOverlay(
    db: FirebaseFirestore,
    explicitAdminUid: String =
      try {
        appContext.getString(R.string.admin_uid).trim()
      } catch (_: Exception) {
        ""
      },
  ): Flow<RealtimeShayariOverlay> {
    val publicSitesFlow =
      flow {
        if (explicitAdminUid.isNotBlank()) {
          val docRef = db.collection("publicSites").document(explicitAdminUid)
          emitAll(
            docRef.snapshots()
              .map { snap ->
                val data = snap.data
                if (!snap.exists() || data == null) {
                  null to ""
                } else {
                  ShayariConfig.fromFirestoreMap(data) to snap.id
                }
              }
              .catch { cause ->
                if (cause is CancellationException) throw cause
                handleFirestoreError(cause, OperationType.GET, docRef.path)
                emit(null to "")
              }
          )
        } else {
          val col = db.collection("publicSites").limit(5)
          emitAll(
            col.snapshots()
              .map { querySnap ->
                val doc = querySnap.documents.firstOrNull { it.exists() && it.data != null }
                val data = doc?.data
                if (doc == null || data == null) {
                  null to ""
                } else {
                  ShayariConfig.fromFirestoreMap(data) to doc.id
                }
              }
              .catch { cause ->
                if (cause is CancellationException) throw cause
                handleFirestoreError(cause, OperationType.LIST, "publicSites")
                emit(null to "")
              }
          )
        }
      }

    val shayarisFlow =
      flow {
        val col = db.collection("shayaris")
        emitAll(
          col.snapshots()
            .map { querySnap ->
              querySnap.documents.mapIndexedNotNull { index, doc ->
                val text =
                  doc.getString("text").orEmpty().ifBlank { doc.getString("shayari").orEmpty() }.trim()
                if (text.isBlank()) return@mapIndexedNotNull null
                val category = doc.getString("category").orEmpty().trim().ifBlank { "General" }
                val author = doc.getString("author").orEmpty().trim().ifBlank { "Shayari Lok" }
                val imageUrl =
                  doc.getString("image_url")
                    ?.trim()
                    ?.ifBlank { doc.getString("imageUrl")?.trim().orEmpty() }
                    ?.ifBlank { doc.getString("thumbnail")?.trim().orEmpty() }
                    .orEmpty()
                val numericId =
                  doc.getLong("id")?.toInt()?.takeIf { it > 0 }
                    ?: ((doc.id.hashCode() and 0x7FFFFFFF).takeIf { it > 0 } ?: (10000 + index))
                val isAdultFlag =
                  (doc.getBoolean("is_adult")
                    ?: doc.getBoolean("isAdult")
                    ?: doc.getBoolean("adult")
                    ?: doc.getBoolean("is_18_plus")
                    ?: doc.getBoolean("nsfw")
                    ?: false) || ShayariItem.isAdultCategoryName(category)

                ShayariItem(
                  id = numericId,
                  category = category,
                  text = text,
                  author = author,
                  imageUrl = imageUrl,
                  isAdult = isAdultFlag,
                )
              }
            }
            .catch { cause ->
              if (cause is CancellationException) throw cause
              handleFirestoreError(cause, OperationType.LIST, "shayaris")
              emit(emptyList())
            }
        )
      }

    val appConfigDocFlow =
      flow {
        val docRef = db.collection("appConfig").document("main")
        emitAll(
          docRef.snapshots()
            .map { snapshot ->
              val data = snapshot.data
              if (!snapshot.exists() || data == null) {
                RealtimeAppConfigOverride()
              } else {
                val parsedDoc = ShayariConfig.fromFirestoreMap(data)
                val hasMaintenance =
                  data.containsKey("maintenance_mode") || data.containsKey("maintenanceMode")
                val hasAdsEnabled =
                  data.containsKey("ads_enabled") ||
                    data.containsKey("adsEnabled") ||
                    data.containsKey("admob") ||
                    data.containsKey("ads")
                val hasDynamicTheme =
                  data.containsKey("dynamic_theme") ||
                    data.containsKey("dynamicTheme") ||
                    data.containsKey("theme")
                val hasFaqs =
                  data.containsKey("faqs") ||
                    data.containsKey("faq_list") ||
                    data.containsKey("faqList") ||
                    data.containsKey("faq")
                val hasNotifications = data.containsKey("notifications")
                val hasPolicySections =
                  data.containsKey("privacy_policy_sections") ||
                    data.containsKey("privacyPolicySections") ||
                    data.containsKey("privacy_sections") ||
                    data.containsKey("privacySections")
                val cats =
                  (data["categories"] as? List<*>)
                    ?.mapNotNull { (it as? String)?.trim()?.takeIf { s -> s.isNotEmpty() } }
                    .orEmpty()
                RealtimeAppConfigOverride(
                  hasOverride = true,
                  appName =
                    data["app_name"]?.toString()?.trim().orEmpty()
                      .ifBlank { data["appName"]?.toString()?.trim().orEmpty() },
                  appLogoUrl = parsedDoc.appLogoUrl,
                  maintenanceMode = if (hasMaintenance) parsedDoc.maintenanceMode else null,
                  maintenanceMessage =
                    data["maintenance_message"]?.toString()?.trim().orEmpty()
                      .ifBlank { data["maintenanceMessage"]?.toString()?.trim().orEmpty() },
                  announcementTitle = parsedDoc.announcementTitle,
                  announcementMessage = parsedDoc.announcementMessage,
                  privacyPolicyUrl = parsedDoc.privacyPolicyUrl,
                  privacyPolicyTitle = parsedDoc.privacyPolicyTitle,
                  privacyPolicyText = parsedDoc.privacyPolicyText,
                  privacyPolicySections = if (hasPolicySections) parsedDoc.privacyPolicySections else null,
                  adsEnabled = if (hasAdsEnabled) parsedDoc.adsEnabled else null,
                  admobAppId = parsedDoc.admobAppId,
                  bannerAdId = parsedDoc.bannerAdId,
                  interstitialAdId = parsedDoc.interstitialAdId,
                  nativeAdId = parsedDoc.nativeAdId,
                  faqs = if (hasFaqs) parsedDoc.faqs else null,
                  notifications = if (hasNotifications) parsedDoc.notifications else null,
                  dynamicTheme = if (hasDynamicTheme) parsedDoc.dynamicTheme else null,
                  categories = cats,
                )
              }
            }
            .catch { cause ->
              if (cause is CancellationException) throw cause
              handleFirestoreError(cause, OperationType.GET, "appConfig/main")
              emit(RealtimeAppConfigOverride())
            }
        )
      }

    return combine(publicSitesFlow, shayarisFlow, appConfigDocFlow) { publicPair, items, cfg ->
      RealtimeShayariOverlay(
        publicSiteConfig = publicPair.first,
        publicSiteDocId = publicPair.second,
        shayariItems = items,
        appConfigOverride = cfg,
      )
    }
  }

  data class RealtimeAppConfigOverride(
    val hasOverride: Boolean = false,
    val appName: String = "",
    val appLogoUrl: String = "",
    val maintenanceMode: Boolean? = null,
    val maintenanceMessage: String = "",
    val announcementTitle: String = "",
    val announcementMessage: String = "",
    val privacyPolicyUrl: String = "",
    val privacyPolicyTitle: String = "",
    val privacyPolicyText: String = "",
    val privacyPolicySections: List<com.example.model.ShayariPolicySection>? = null,
    val adsEnabled: Boolean? = null,
    val admobAppId: String = "",
    val bannerAdId: String = "",
    val interstitialAdId: String = "",
    val nativeAdId: String = "",
    val faqs: List<com.example.model.ShayariFaq>? = null,
    val notifications: List<com.example.model.ShayariNotification>? = null,
    val dynamicTheme: com.example.model.DynamicThemeConfig? = null,
    val categories: List<String> = emptyList(),
  )

  data class RealtimeShayariOverlay(
    val publicSiteConfig: ShayariConfig? = null,
    val publicSiteDocId: String = "",
    val shayariItems: List<ShayariItem> = emptyList(),
    val appConfigOverride: RealtimeAppConfigOverride = RealtimeAppConfigOverride(),
  )

  companion object {
    private const val PREFS_NAME = "shayari_lok_prefs"
    private const val KEY_CACHED_CONFIG_JSON = "cached_config_json"
    private const val KEY_CACHED_OVERLAY_CONFIG_JSON = "cached_overlay_config_json"
    private const val KEY_LAST_SYNCED_AT = "last_synced_at"
    private const val KEY_FAVORITE_IDS = "favorite_shayari_ids"
    private const val KEY_TEXT_SIZE = "shayari_text_size"
    private const val KEY_SEEN_NOTIFICATIONS_SIG = "seen_notifications_sig"
    private const val KEY_SELECTED_LANGUAGE = "selected_language_code"
    private const val KEY_SELECTED_COUNTRY = "selected_country_code"
    private const val KEY_LAST_VISIT_DATE = "last_visit_date"
    private const val KEY_DAILY_STREAK_COUNT = "daily_streak_count"
    private const val KEY_POLL_VOTES_JSON = "poll_votes_json"
    private const val KEY_ALLOW_ADULT_18_PLUS = "allow_adult_18_plus"
    private const val KEY_AGE_VERIFIED_18_PLUS = "age_verified_18_plus"
    private const val KEY_USER_DARK_THEME = "user_dark_theme_override"
    private const val CONNECT_TIMEOUT_MS = 10000
    private const val READ_TIMEOUT_MS = 10000
  }
}
