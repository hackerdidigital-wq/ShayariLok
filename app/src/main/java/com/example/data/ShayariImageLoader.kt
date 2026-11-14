package com.example.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Lightweight native image loader supporting both Admin Panel base64 gallery uploads
 * (data:image/...;base64,...) and remote HTTPS image URLs, backed by an in-memory LruCache.
 */
object ShayariImageLoader {
  private const val MAX_DECODED_DIMENSION_PX = 512
  private val bitmapCache = object : LruCache<String, ImageBitmap>(24) {}

  fun getCached(source: String): ImageBitmap? {
    if (source.isBlank()) return null
    return bitmapCache.get(source)
  }

  suspend fun loadBitmap(source: String): ImageBitmap? {
    val trimmed = source.trim()
    if (trimmed.isEmpty()) return null
    bitmapCache.get(trimmed)?.let { return it }

    val decoded =
      withContext(Dispatchers.IO) {
        try {
          when {
            trimmed.startsWith("data:image", ignoreCase = true) || isRawBase64(trimmed) -> {
              val payload = trimmed.substringAfter("base64,", trimmed)
              val bytes = Base64.decode(payload, Base64.DEFAULT)
              decodeSampledByteArray(bytes)
            }
            trimmed.startsWith("http://", ignoreCase = true) ||
              trimmed.startsWith("https://", ignoreCase = true) -> {
              fetchAndDecodeUrl(trimmed)
            }
            else -> null
          }
        } catch (_: Exception) {
          null
        }
      } ?: return null

    val imageBitmap = decoded.asImageBitmap()
    bitmapCache.put(trimmed, imageBitmap)
    return imageBitmap
  }

  private fun isRawBase64(value: String): Boolean {
    if (value.startsWith("http://", true) || value.startsWith("https://", true)) return false
    return value.length > 64 && !value.contains("://")
  }

  private fun fetchAndDecodeUrl(imageUrl: String): Bitmap? {
    var connection: HttpURLConnection? = null
    return try {
      connection =
        (URL(imageUrl).openConnection() as HttpURLConnection).apply {
          requestMethod = "GET"
          instanceFollowRedirects = true
          connectTimeout = 8000
          readTimeout = 8000
          setRequestProperty("User-Agent", "ShayariLok-Android/1.0")
        }
      if (connection.responseCode !in 200..299) {
        connection.errorStream?.close()
        return null
      }
      val bytes = connection.inputStream.use { it.readBytes() }
      decodeSampledByteArray(bytes)
    } finally {
      connection?.disconnect()
    }
  }

  private fun decodeSampledByteArray(bytes: ByteArray): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    var sampleSize = 1
    while (
      bounds.outWidth / sampleSize > MAX_DECODED_DIMENSION_PX ||
        bounds.outHeight / sampleSize > MAX_DECODED_DIMENSION_PX
    ) {
      sampleSize *= 2
    }

    val opts =
      BitmapFactory.Options().apply {
        inSampleSize = sampleSize
        inPreferredConfig = Bitmap.Config.RGB_565
      }
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
  }
}

@Composable
fun rememberShayariImageBitmap(imageSource: String): ImageBitmap? {
  var bitmap by remember(imageSource) { mutableStateOf(ShayariImageLoader.getCached(imageSource)) }

  LaunchedEffect(imageSource) {
    if (imageSource.isNotBlank() && bitmap == null) {
      bitmap = ShayariImageLoader.loadBitmap(imageSource)
    }
  }

  return bitmap
}
