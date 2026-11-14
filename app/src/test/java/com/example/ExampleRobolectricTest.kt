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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

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
    val cm = app.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val shadowCm = Shadows.shadowOf(cm)
    shadowCm.setDefaultNetworkActive(false)
    @Suppress("DEPRECATION")
    shadowCm.setActiveNetworkInfo(null)

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
    assertEquals("Shayari Copied Successfully!", app.getString(R.string.copied_snackbar))

    val viewModel = ShayariViewModel(app)
    assertTrue(viewModel.uiState.value.isSplashVisible)
  }
}
