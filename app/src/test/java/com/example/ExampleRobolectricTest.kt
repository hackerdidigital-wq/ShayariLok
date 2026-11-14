package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.DefaultShayariData
import com.example.data.ShayariRepository
import com.example.data.ShayariTextSize
import com.example.model.ShayariConfig
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
  fun `default config json parses categories, shayari items, and notifications cleanly`() {
    val parsed = ShayariConfig.fromJson(DefaultShayariData.DEFAULT_CONFIG_JSON)
    assertEquals("Shayari Lok", parsed.appName)
    assertFalse(parsed.maintenanceMode)
    assertTrue(parsed.categories.contains("Popular"))
    assertTrue(parsed.categories.contains("Love"))
    assertTrue(parsed.categories.contains("Sad"))
    assertTrue(parsed.categories.contains("Motivational"))
    assertTrue(parsed.shayariList.isNotEmpty())
    assertTrue(parsed.notifications.isNotEmpty())
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
