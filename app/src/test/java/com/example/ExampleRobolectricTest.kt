package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.DefaultShayariData
import com.example.data.ShayariRepository
import com.example.data.ShayariTextSize
import com.example.model.ShayariConfig
import com.example.ui.ShayariScreenState
import com.example.ui.ShayariViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
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
}
