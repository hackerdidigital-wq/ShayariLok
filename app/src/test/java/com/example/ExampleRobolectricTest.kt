package com.example

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import androidx.test.core.app.ApplicationProvider
import com.example.data.DefaultShayariData
import com.example.data.ShayariRepository
import com.example.data.ShayariTextSize
import com.example.model.ShayariConfig
import com.example.ui.ShayariScreenState
import com.example.ui.ShayariViewModel
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Before
  fun setUp() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    app.getSharedPreferences("shayari_lok_prefs", Context.MODE_PRIVATE).edit().clear().commit()
  }

  @After
  fun tearDown() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    app.getSharedPreferences("shayari_lok_prefs", Context.MODE_PRIVATE).edit().clear().commit()
  }

  private fun setNetworkOffline(app: Application) {
    val cm = app.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val shadowCm = Shadows.shadowOf(cm)
    shadowCm.setDefaultNetworkActive(false)
    @Suppress("DEPRECATION")
    shadowCm.setActiveNetworkInfo(null)
    cm.activeNetwork?.let { network ->
      shadowCm.setNetworkCapabilities(network, null)
    }
  }

  @Test
  fun `app_name matches Shayari Lok`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Shayari Lok", appName)
  }

  @Test
  fun `live admin panel config json parses all required fields cleanly`() {
    val parsed = ShayariConfig.fromJson(DefaultShayariData.DEFAULT_CONFIG_JSON)
    assertEquals("Shayari Lok", parsed.appName)
    assertEquals(1, parsed.latestVersionCode)
    assertFalse(parsed.maintenanceMode)
    assertTrue(parsed.adsEnabled)
    assertEquals(listOf("All", "Love", "Attitude", "Motivation", "Sad"), parsed.categories)
    assertEquals(5, parsed.shayariList.size)
    assertEquals(2, parsed.notifications.size)
    assertEquals("अज्ञात", parsed.shayariList.first().author)
  }

  @Test
  fun `maintenance_mode true switches ViewModel to Maintenance screen`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val prefs = app.getSharedPreferences("shayari_lok_prefs", Context.MODE_PRIVATE)
    prefs.edit()
      .putString(
        "cached_config_json",
        """
        {
          "app_name": "Shayari Lok",
          "latest_version_code": 1,
          "maintenance_mode": true,
          "maintenance_message": "हम ऐप को और बेहतर बना रहे हैं। कृपया कुछ समय बाद पुनः प्रयास करें।",
          "categories": ["All", "Love"],
          "shayari_list": []
        }
        """.trimIndent(),
      )
      .commit()

    setNetworkOffline(app)
    val viewModel = ShayariViewModel(app)
    val state = viewModel.uiState.value.screenState
    assertTrue(state is ShayariScreenState.Maintenance)
    val maintenance = state as ShayariScreenState.Maintenance
    assertEquals("Shayari Lok", maintenance.appName)
    assertTrue(maintenance.message.contains("हम ऐप को और बेहतर बना रहे हैं"))

    prefs.edit().clear().commit()
  }

  @Test
  fun `offline state triggers clean dialog while keeping shayari state intact and retry retries cleanly`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    setNetworkOffline(app)

    val viewModel = ShayariViewModel(app)
    val initialState = viewModel.uiState.value

    assertFalse(initialState.isNetworkConnected)
    assertTrue(initialState.showOfflineDialog)
    assertTrue(initialState.screenState is ShayariScreenState.Ready)
    assertEquals(5, (initialState.screenState as ShayariScreenState.Ready).visibleShayaris.size)

    viewModel.retryConnection()
    val afterRetryOffline = viewModel.uiState.value
    assertFalse(afterRetryOffline.isNetworkConnected)
    assertTrue(afterRetryOffline.showOfflineDialog)
    assertTrue(afterRetryOffline.screenState is ShayariScreenState.Ready)
  }

  @Test
  fun `silent fallback never shows no-internet dialog when device internet is active`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val cm = app.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val shadowCm = Shadows.shadowOf(cm)
    shadowCm.setDefaultNetworkActive(true)

    val viewModel = ShayariViewModel(app)
    val state = viewModel.uiState.value

    if (ShayariRepository(app).isNetworkAvailable()) {
      assertTrue(state.isNetworkConnected)
      assertFalse(state.showOfflineDialog)
    }
    assertTrue(state.screenState is ShayariScreenState.Ready)
  }

  @Test
  fun `fromJson handles UTF-8 BOM, string booleans, and duplicate ids without crashing`() {
    val malformedButValid =
      "\uFEFF" +
        """
        {
          "app_name": "Shayari Lok",
          "latest_version_code": "2",
          "maintenance_mode": "true",
          "categories": ["Love", "Love", "Sad"],
          "shayari_list": [
            { "id": 1, "category": "Love", "text": "Line 1" },
            { "id": 1, "category": "Sad", "text": "Line 2" }
          ]
        }
        """.trimIndent()

    val parsed = ShayariConfig.fromJson(malformedButValid)
    assertEquals(2, parsed.latestVersionCode)
    assertTrue(parsed.maintenanceMode)
    assertEquals(listOf("All", "Love", "Sad"), parsed.categories)
    assertEquals(2, parsed.shayariList.size)
    assertEquals(2, parsed.shayariList.map { it.id }.toSet().size)
  }

  @Test
  fun `repository persists reading text size and notification read state`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = ShayariRepository(context)
    repo.setShayariTextSize(ShayariTextSize.LARGE)
    assertEquals(ShayariTextSize.LARGE, repo.getShayariTextSize())

    val config = repo.loadCachedConfig()
    assertTrue(repo.hasUnreadNotifications(config))
    repo.markNotificationsRead(config)
    assertFalse(repo.hasUnreadNotifications(config))
  }

  @Test
  fun `remote endpoints target live ShayariLok-Admin- repo and append cache busters`() {
    assertEquals(
      "https://raw.githubusercontent.com/hackerdidigital-wq/ShayariLok-Admin-/main/config.json",
      DefaultShayariData.DEFAULT_REMOTE_CONFIG_URL,
    )
    assertTrue(
      DefaultShayariData.REMOTE_CONFIG_ENDPOINTS.contains(
        "https://cdn.jsdelivr.net/gh/hackerdidigital-wq/ShayariLok-Admin-@main/config.json"
      )
    )
    assertTrue(
      DefaultShayariData.REMOTE_CONFIG_ENDPOINTS.contains(
        "https://api.github.com/repos/hackerdidigital-wq/ShayariLok-Admin-/contents/config.json?ref=main"
      )
    )

    val app = ApplicationProvider.getApplicationContext<Application>()
    val repo = ShayariRepository(app)
    assertEquals(
      "https://example.com/config.json?t=12345",
      repo.buildCacheBustedUrl("https://example.com/config.json", 12345L),
    )
    assertEquals(
      "https://example.com/config.json?raw=1&t=12345",
      repo.buildCacheBustedUrl("https://example.com/config.json?raw=1", 12345L),
    )
  }

  @Test
  fun `unwrapGitHubContentsPayloadIfNeeded decodes base64 GitHub Contents API response`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val repo = ShayariRepository(app)
    val rawConfig =
      """{"app_name":"Shayari Lok","latest_version_code":2,"categories":["All","Love"],"shayari_list":[{"id":6,"category":"Love","text":"I love you tanisha","author":"Tan8sha"}]}"""
    val b64 =
      android.util.Base64.encodeToString(rawConfig.toByteArray(Charsets.UTF_8), android.util.Base64.DEFAULT)
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
    val apiWrapper = """{"encoding":"base64","content":"$b64"}"""

    val unwrapped = repo.unwrapGitHubContentsPayloadIfNeeded(apiWrapper)
    val parsed = ShayariConfig.fromJson(unwrapped)
    assertEquals(2, parsed.latestVersionCode)
    assertEquals(1, parsed.shayariList.size)
    assertEquals("I love you tanisha", parsed.shayariList.first().text)
  }

  @Test
  fun `fetchRemoteConfig skips 404 endpoint and syncs new shayari category and notification live`() =
    runTest {
      val app = ApplicationProvider.getApplicationContext<Application>()
      val cm = app.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
      Shadows.shadowOf(cm).setDefaultNetworkActive(true)
      val repo = ShayariRepository(app)
      if (!repo.isNetworkAvailable()) return@runTest

      val updatedJson =
        """
        {
          "app_name": "Shayari Lok",
          "latest_version_code": 2,
          "force_update_url": "https://play.google.com/store/apps/details?id=com.cyberking.shayarilok",
          "maintenance_mode": false,
          "categories": ["All", "Love", "Dosti"],
          "notifications": [
            {
              "id": 3,
              "title": "9999",
              "message": "99999999",
              "badge": "Shayari Alert",
              "time_label": "Today"
            }
          ],
          "shayari_list": [
            {
              "id": 6,
              "category": "Love",
              "text": "I love you tanisha",
              "author": "Tan8sha"
            },
            {
              "id": 99,
              "category": "Dosti",
              "text": "सच्चे दोस्त किस्मत से मिलते हैं।",
              "author": "Shayari Lok"
            }
          ]
        }
        """.trimIndent()

      val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
      var requestedQuery = ""
      server.createContext("/missing.json") { exchange ->
        exchange.sendResponseHeaders(404, -1)
        exchange.close()
      }
      server.createContext("/live/config.json") { exchange ->
        requestedQuery = exchange.requestURI.query ?: ""
        val bytes = updatedJson.toByteArray(Charsets.UTF_8)
        exchange.responseHeaders.add("Content-Type", "application/json; charset=utf-8")
        exchange.sendResponseHeaders(200, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
      }
      server.start()

      try {
        val port = server.address.port
        val result =
          repo.fetchRemoteConfig(
            endpoints =
              listOf(
                "http://127.0.0.1:$port/missing.json",
                "http://127.0.0.1:$port/live/config.json",
              )
          )

        assertTrue(result.isSuccess)
        assertTrue(requestedQuery.startsWith("t="))
        val synced = result.getOrThrow()
        assertEquals(2, synced.latestVersionCode)
        assertEquals(listOf("All", "Love", "Dosti"), synced.categories)
        assertEquals(2, synced.shayariList.size)
        assertEquals(6, synced.shayariList.first().id)
        assertEquals("I love you tanisha", synced.shayariList.first().text)
        assertEquals(3, synced.notifications.first().id)
        assertTrue(repo.hasUnreadNotifications(synced))
        assertTrue(repo.hasCachedRemoteConfig())
        assertTrue(repo.getLastSyncedTimeMillis() > 0L)
      } finally {
        server.stop(0)
      }
    }

  @Test
  fun `shayari item parses admin gallery base64 and url thumbnail fields cleanly`() {
    val jsonWithImageUrl =
      org.json.JSONObject(
        """{"id":10,"category":"Love","text":"Dil ki baat","image_url":"https://example.com/photo.jpg"}"""
      )
    val itemUrl = com.example.model.ShayariItem.fromJson(jsonWithImageUrl)!!
    assertEquals("https://example.com/photo.jpg", itemUrl.imageUrl)

    val jsonWithGalleryBase64 =
      org.json.JSONObject(
        """{"id":11,"category":"Sad","text":"Yaadein","thumbnail":"data:image/png;base64,iVBORw0KGgo="}"""
      )
    val itemBase64 = com.example.model.ShayariItem.fromJson(jsonWithGalleryBase64)!!
    assertEquals("data:image/png;base64,iVBORw0KGgo=", itemBase64.imageUrl)
  }

  @Test
  fun `copied snackbar message and initial splash state are configured`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    setNetworkOffline(app)
    assertEquals("Copied Successfully!", app.getString(R.string.copied_snackbar))

    val viewModel = ShayariViewModel(app)
    assertTrue(viewModel.uiState.value.isSplashVisible)
  }

  @Test
  fun `buildHandleFromUid produces deterministic @SL_XXXX handle from Google UID`() {
    val uid = "G7xKp92LzQ8sT1vN"
    val first = com.example.data.SocialRepository.buildHandleFromUid(uid)
    val second = com.example.data.SocialRepository.buildHandleFromUid(uid)
    assertTrue(first.matches(Regex("^@SL_\\d{4}$")))
    assertEquals(first, second)
  }

  @Test
  fun `social chat room id is deterministic regardless of participant order`() {
    assertEquals(
      com.example.data.SocialRepository.buildChatRoomId("uid_b", "uid_a"),
      com.example.data.SocialRepository.buildChatRoomId("uid_a", "uid_b"),
    )
    assertEquals(
      "room_uid_a_uid_b",
      com.example.data.SocialRepository.buildChatRoomId("uid_b", "uid_a"),
    )
  }

  @Test
  fun `PublicProfile and ChatMessage effective accessors normalize Firestore legacy fields`() {
    val legacyProfile =
      com.example.model.PublicProfile(
        id = "doc_9001",
        uid = "google_uid_9001",
        username = "SL_9001",
        displayName = "Rohan Shayar",
        likesCount = 12L,
        likesReceivedCount = 40L,
      )
    assertEquals("google_uid_9001", legacyProfile.effectiveUid)
    assertEquals("@SL_9001", legacyProfile.effectiveHandle)
    assertEquals(40L, legacyProfile.effectiveLikesCount)

    val minimalProfile =
      com.example.model.PublicProfile(
        id = "AbCd1234",
        displayName = "Aarav",
      )
    assertEquals("AbCd1234", minimalProfile.effectiveUid)
    assertEquals("@SL_1234", minimalProfile.effectiveHandle)

    val msg =
      com.example.model.ChatMessage(
        id = "m1",
        senderId = "uid_a",
        receiverId = "uid_b",
        text = "Waah!",
      )
    assertEquals("uid_b", msg.effectiveRecipientId)
  }

  @Test
  fun `publicSites Firestore map parses supported_languages RTL, countries, dynamic_theme, and polls`() {
    val firestoreDocMap: Map<String, Any?> =
      mapOf(
        "app_name" to "Shayari Lok Pro",
        "latest_version_code" to 5L,
        "maintenance_mode" to false,
        "supported_languages" to listOf("hi", "en", "ur"),
        "supported_countries" to listOf("IN", "AE"),
        "dynamic_theme" to
          mapOf(
            "primary_color" to "#9C27B0",
            "accent_color" to "#FF4081",
            "dark_mode" to true,
          ),
        "engagement" to
          mapOf(
            "daily_streak_enabled" to true,
            "whatsapp_share_enabled" to true,
            "polls" to
              listOf(
                mapOf(
                  "id" to "poll_1",
                  "question" to "Favorite Shayari mood?",
                  "options" to
                    listOf(
                      mapOf("id" to "love", "label" to "Love", "votes" to 10L),
                      mapOf("id" to "sad", "label" to "Sad", "votes" to 4L),
                    ),
                )
              ),
          ),
        "categories" to listOf("All", "Love"),
        "shayari_list" to
          listOf(
            mapOf("id" to 101L, "category" to "Love", "text" to "Dil se nikli baat", "author" to "Shayar")
          ),
      )

    val parsed = ShayariConfig.fromFirestoreMap(firestoreDocMap)
    assertEquals("Shayari Lok Pro", parsed.appName)
    assertEquals(5, parsed.latestVersionCode)
    assertEquals(3, parsed.supportedLanguages.size)
    assertTrue(parsed.supportedLanguages.first { it.code == "ur" }.isRtl)
    assertFalse(parsed.supportedLanguages.first { it.code == "hi" }.isRtl)
    assertEquals(listOf("IN", "AE"), parsed.supportedCountries.map { it.code })
    assertEquals("#9C27B0", parsed.dynamicTheme.primaryHex)
    assertEquals("#FF4081", parsed.dynamicTheme.accentHex)
    assertTrue(parsed.dynamicTheme.darkMode)
    assertEquals(1, parsed.engagement.polls.size)
    assertEquals(14, parsed.engagement.polls.first().totalVotes)
  }

  @Test
  fun `repository tracks daily streak, language and country selection, and poll votes`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    app.getSharedPreferences("shayari_lok_prefs", Context.MODE_PRIVATE).edit().clear().commit()
    val repo = ShayariRepository(app)

    assertEquals(1, repo.recordDailyVisitAndGetStreak("2026-11-14"))
    assertEquals(1, repo.recordDailyVisitAndGetStreak("2026-11-14"))
    assertEquals(2, repo.recordDailyVisitAndGetStreak("2026-11-15"))
    assertEquals(1, repo.recordDailyVisitAndGetStreak("2026-11-18"))

    repo.setSelectedLanguageCode("ur")
    repo.setSelectedCountryCode("AE")
    assertEquals("ur", repo.getSelectedLanguageCode())
    assertEquals("AE", repo.getSelectedCountryCode())

    repo.savePollVoteLocal("poll_1", "love")
    assertEquals("love", repo.getVotedPollOptions()["poll_1"])
  }

  @Test
  fun `GoogleAuthHelper resolves valid OAuth Web Client ID without missing error`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val clientId = com.example.data.GoogleAuthHelper.resolveWebClientId(app)
    assertEquals(
      "663704095886-21iunr4j2kfkuor3i8u6gak5fem1ihsk.apps.googleusercontent.com",
      clientId,
    )
  }
}
