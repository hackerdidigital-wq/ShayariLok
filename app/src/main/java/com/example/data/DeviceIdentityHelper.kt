package com.example.data

import android.content.Context
import android.provider.Settings
import kotlin.math.abs

object DeviceIdentityHelper {
  private const val PREFS_NAME = "shayari_lok_prefs"
  private const val KEY_DEVICE_HANDLE = "device_unique_handle"

  fun getOrCreateDeviceHandle(context: Context): String {
    val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val existing = prefs.getString(KEY_DEVICE_HANDLE, null)
    if (!existing.isNullOrBlank()) {
      return existing
    }

    val androidId =
      try {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
      } catch (_: Exception) {
        null
      }

    val code =
      if (!androidId.isNullOrBlank()) {
        1000 + (abs(androidId.hashCode()) % 9000)
      } else {
        (1000..9999).random()
      }

    val generated = "@SL_$code"
    prefs.edit().putString(KEY_DEVICE_HANDLE, generated).apply()
    return generated
  }
}
