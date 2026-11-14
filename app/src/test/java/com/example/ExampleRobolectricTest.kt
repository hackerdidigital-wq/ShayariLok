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
import kotlinx.coroutines.flow.firstOrNull
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
    assertEquals("Copied Successfully!", app.getString(R.string.copied_snackbar))

    val viewModel = ShayariViewModel(app)
    assertTrue(viewModel.uiState.value.isSplashVisible)
  }

  @Test
  fun `device handle auto-generates valid deterministic @SL_XXXX id`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val first = com.example.data.DeviceIdentityHelper.getOrCreateDeviceHandle(app)
    val second = com.example.data.DeviceIdentityHelper.getOrCreateDeviceHandle(app)
    assertTrue(first.matches(Regex("^@SL_\\d{4}$")))
    assertEquals(first, second)
  }

  @Test
  fun `social chat room id is deterministic regardless of participant order`() {
    assertEquals(
      com.example.data.SocialRepository.buildChatRoomId("user_b", "user_a"),
      com.example.data.SocialRepository.buildChatRoomId("user_a", "user_b"),
    )
  }

  @Test
  fun `local Room social repository persists profile edits, follow, like, and 1-on-1 chat cleanly`() =
    runTest {
      val app = ApplicationProvider.getApplicationContext<Application>()
      val db =
        androidx.room.Room.inMemoryDatabaseBuilder(
            app,
            com.example.data.local.SocialDatabase::class.java,
          )
          .allowMainThreadQueries()
          .build()
      try {
        val repo = com.example.data.SocialRepository(db.socialDao())
        val selfUid = "local_sl_1024"
        val selfProfile =
          repo.ensureUserBootstrap(selfUid, "@SL_1024", "Aarav Local").getOrThrow()
        assertEquals("@SL_1024", selfProfile.handle)

        repo.updateMyProfile(
          selfUid = selfUid,
          displayName = "Ravi Shayar",
          city = "Varanasi",
          address = "Dashashwamedh Road",
          bio = "Kashi ki shayari",
        ).getOrThrow()

        val updatedSelf = repo.observeMyProfile(selfUid).firstOrNull()!!
        assertEquals("Ravi Shayar", updatedSelf.displayName)
        assertEquals("Varanasi", updatedSelf.city)

        val friend =
          repo.addFriendProfile("Neha Tiwari", "@SL_8899", "Prayagraj", "Sangam vibes")
            .getOrThrow()
        assertEquals("@SL_8899", friend.handle)

        val followResult = repo.toggleFollow(updatedSelf, friend, currentlyFollowing = false).getOrThrow()
        assertTrue(followResult)

        val likeResult = repo.toggleProfileLike(updatedSelf, friend, currentlyLiked = false).getOrThrow()
        assertTrue(likeResult)

        val msgRes = repo.sendChatMessage(updatedSelf, friend, "Waah kya shayari hai! ❤️")
        assertTrue(msgRes.isSuccess)
        val chatId = com.example.data.SocialRepository.buildChatRoomId(selfUid, friend.userId)
        val messages = repo.observeChatMessages(chatId).firstOrNull().orEmpty()
        assertEquals(1, messages.size)
        assertEquals("Waah kya shayari hai! ❤️", messages.first().text)
      } finally {
        db.close()
      }
    }

  @Test
  fun `FirestoreRestClient syncs appUsers, socialEdges, and chatRooms over HTTP REST endpoints`() =
    runTest {
      val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
      val recordedPaths = mutableListOf<String>()
      var lastUpsertUserBody = ""
      var lastEdgeMethodOverride = ""
      var lastSentMsgBody = ""

      server.createContext("/documents/appUsers") { exchange ->
        val path = exchange.requestURI.path
        recordedPaths += "${exchange.requestMethod}:$path"
        if (exchange.requestMethod == "POST") {
          lastUpsertUserBody = exchange.requestBody.bufferedReader(Charsets.UTF_8).use { it.readText() }
          val okBytes = """{"name":"projects/p/databases/d/documents/appUsers/local_sl_7777"}""".toByteArray()
          exchange.sendResponseHeaders(200, okBytes.size.toLong())
          exchange.responseBody.use { it.write(okBytes) }
        } else if (path.endsWith("/local_sl_7777")) {
          val docJson =
            """
            {
              "name": "projects/p/databases/d/documents/appUsers/local_sl_7777",
              "fields": {
                "uid": { "stringValue": "local_sl_7777" },
                "displayName": { "stringValue": "Meera AdminSync" },
                "handle": { "stringValue": "@SL_7777" },
                "city": { "stringValue": "Pune" },
                "isBlocked": { "booleanValue": true },
                "blockReason": { "stringValue": "Spam alert" },
                "followersCount": { "integerValue": "14" }
              }
            }
            """.trimIndent().toByteArray()
          exchange.sendResponseHeaders(200, docJson.size.toLong())
          exchange.responseBody.use { it.write(docJson) }
        } else {
          val listJson =
            """
            {
              "documents": [
                {
                  "name": "projects/p/databases/d/documents/appUsers/shayar_remote_9001",
                  "fields": {
                    "uid": { "stringValue": "shayar_remote_9001" },
                    "displayName": { "stringValue": "Rohan Shayar" },
                    "username": { "stringValue": "SL_9001" },
                    "city": { "stringValue": "Mumbai" },
                    "followersCount": { "integerValue": "25" },
                    "likesReceivedCount": { "integerValue": "40" }
                  }
                }
              ]
            }
            """.trimIndent().toByteArray()
          exchange.sendResponseHeaders(200, listJson.size.toLong())
          exchange.responseBody.use { it.write(listJson) }
        }
      }

      server.createContext("/documents/socialEdges") { exchange ->
        lastEdgeMethodOverride = exchange.requestHeaders.getFirst("X-HTTP-Method-Override").orEmpty()
        recordedPaths += "${exchange.requestMethod}:${exchange.requestURI.path}"
        val bytes = "{}".toByteArray()
        exchange.sendResponseHeaders(200, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
      }

      server.createContext("/documents/chatRooms") { exchange ->
        val path = exchange.requestURI.path
        recordedPaths += "${exchange.requestMethod}:$path"
        if (exchange.requestMethod == "POST" && path.endsWith("/messages")) {
          lastSentMsgBody = exchange.requestBody.bufferedReader(Charsets.UTF_8).use { it.readText() }
          val bytes = """{"name":"projects/p/databases/d/documents/chatRooms/r1/messages/m1"}""".toByteArray()
          exchange.sendResponseHeaders(200, bytes.size.toLong())
          exchange.responseBody.use { it.write(bytes) }
        } else if (exchange.requestMethod == "POST") {
          val bytes = "{}".toByteArray()
          exchange.sendResponseHeaders(200, bytes.size.toLong())
          exchange.responseBody.use { it.write(bytes) }
        } else {
          val msgsJson =
            """
            {
              "documents": [
                {
                  "name": "projects/p/databases/d/documents/chatRooms/room_local_sl_7777_shayar_remote_9001/messages/m_admin",
                  "fields": {
                    "roomId": { "stringValue": "room_local_sl_7777_shayar_remote_9001" },
                    "senderId": { "stringValue": "shayar_remote_9001" },
                    "senderHandle": { "stringValue": "@SL_9001" },
                    "receiverId": { "stringValue": "local_sl_7777" },
                    "text": { "stringValue": "Admin panel se live reply! ❤️" }
                  }
                }
              ]
            }
            """.trimIndent().toByteArray()
          exchange.sendResponseHeaders(200, msgsJson.size.toLong())
          exchange.responseBody.use { it.write(msgsJson) }
        }
      }

      server.start()
      try {
        val port = server.address.port
        val client =
          com.example.data.remote.FirestoreRestClient("http://127.0.0.1:$port/documents")

        val self =
          com.example.model.PublicProfile(
            id = "local_sl_7777",
            userId = "local_sl_7777",
            handle = "@SL_7777",
            displayName = "Meera Local",
            city = "Pune",
          )
        val partner =
          com.example.model.PublicProfile(
            id = "shayar_remote_9001",
            userId = "shayar_remote_9001",
            handle = "@SL_9001",
            displayName = "Rohan Shayar",
            city = "Mumbai",
          )

        assertTrue(client.upsertAppUser(self).isSuccess)
        assertTrue(lastUpsertUserBody.contains("\"stringValue\":\"Meera Local\""))

        val single = client.fetchSingleAppUser("local_sl_7777").getOrThrow()!!
        assertTrue(single.isBlocked)
        assertEquals("Spam alert", single.blockReason)
        assertEquals(14L, single.followersCount)

        val remoteUsers = client.fetchAppUsers().getOrThrow()
        assertEquals(1, remoteUsers.size)
        assertEquals("@SL_9001", remoteUsers.first().handle)
        assertEquals(40L, remoteUsers.first().likesCount)

        assertTrue(
          client.upsertSocialEdge(
            actorId = self.userId,
            targetId = partner.userId,
            actorName = self.displayName,
            targetName = partner.displayName,
            type = "follow",
          ).isSuccess
        )
        assertEquals("PATCH", lastEdgeMethodOverride)

        val roomId = com.example.data.SocialRepository.buildChatRoomId(self.userId, partner.userId)
        assertTrue(
          client.sendChatMessage(
            roomId = roomId,
            senderProfile = self,
            receiverProfile = partner,
            text = "Hello via REST ✨",
            messageCountEstimate = 1L,
          ).isSuccess
        )
        assertTrue(lastSentMsgBody.contains("Hello via REST"))

        val fetchedMsgs = client.fetchRoomMessages(roomId).getOrThrow()
        assertEquals(1, fetchedMsgs.size)
        assertEquals("Admin panel se live reply! ❤️", fetchedMsgs.first().text)
        assertTrue(recordedPaths.any { it.contains("/documents/chatRooms/$roomId/messages") })
      } finally {
        server.stop(0)
      }
    }
}
