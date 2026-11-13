package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.model.ShayariConfig
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class ShayariTextSize(val label: String, val fontSizeSp: Int, val lineHeightSp: Int) {
  COMPACT("Compact", 16, 26),
  MEDIUM("Standard", 18, 29),
  LARGE("Large", 21, 33),
}

/**
 * Ultra-lightweight data repository using pure Android HttpURLConnection,
 * org.json, and SharedPreferences for offline caching, reading preferences, and favorites.
 *
 * The remote config endpoint is kept strictly internal (never exposed in the UI).
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
    config.notifications.joinToString("|") { "${it.id}:${it.title}" }

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

  suspend fun fetchRemoteConfig(): Result<ShayariConfig> =
    withContext(Dispatchers.IO) {
      if (!isNetworkAvailable()) {
        return@withContext Result.failure(IllegalStateException("Offline mode active"))
      }

      var connection: HttpURLConnection? = null
      try {
        val url = URL(REMOTE_CONFIG_URL)
        connection =
          (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            useCaches = false
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Cache-Control", "no-cache")
          }

        val responseCode = connection.responseCode
        if (responseCode !in 200..299) {
          return@withContext Result.failure(
            IllegalStateException("Server responded with HTTP $responseCode")
          )
        }

        val rawJson =
          BufferedReader(InputStreamReader(connection.inputStream, Charsets.UTF_8)).use { reader ->
            reader.readText()
          }

        val parsedConfig = ShayariConfig.fromJson(rawJson)
        val now = System.currentTimeMillis()
        prefs.edit()
          .putString(KEY_CACHED_CONFIG_JSON, rawJson)
          .putLong(KEY_LAST_SYNCED_AT, now)
          .apply()

        Result.success(parsedConfig)
      } catch (e: Exception) {
        Result.failure(e)
      } finally {
        connection?.disconnect()
      }
    }

  private fun isNetworkAvailable(): Boolean {
    val connectivityManager =
      appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        ?: return false
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
  }

  companion object {
    // Hardcoded internal GitHub Raw config.json endpoint — never exposed to end users in UI
    private const val REMOTE_CONFIG_URL = DefaultShayariData.DEFAULT_REMOTE_CONFIG_URL
    private const val PREFS_NAME = "shayari_lok_prefs"
    private const val KEY_CACHED_CONFIG_JSON = "cached_config_json"
    private const val KEY_LAST_SYNCED_AT = "last_synced_at"
    private const val KEY_FAVORITE_IDS = "favorite_shayari_ids"
    private const val KEY_TEXT_SIZE = "shayari_text_size"
    private const val KEY_SEEN_NOTIFICATIONS_SIG = "seen_notifications_sig"
    private const val CONNECT_TIMEOUT_MS = 8000
    private const val READ_TIMEOUT_MS = 8000
  }
}
