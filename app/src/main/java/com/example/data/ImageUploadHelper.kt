package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import kotlin.math.min
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Zero-permission image processor using Android's system Photo Picker (PickVisualMedia)
 * to convert user-selected gallery photos into compact JPEG Base64 Data URIs stored directly
 * on the user's Firestore profile — no external URLs or broad storage permissions required.
 */
object ImageUploadHelper {
  private const val MAX_AVATAR_PX = 256
  private const val MAX_DATA_URI_LENGTH = 160_000

  suspend fun processPickedAvatarUri(context: Context, uri: Uri): Result<String> =
    withContext(Dispatchers.IO) {
      try {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { stream ->
          BitmapFactory.decodeStream(stream, null, bounds)
        } ?: return@withContext Result.failure(IllegalArgumentException("Unable to read selected image"))

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
          return@withContext Result.failure(IllegalArgumentException("Invalid image file selected"))
        }

        var sampleSize = 1
        val maxDim = maxOf(bounds.outWidth, bounds.outHeight)
        while (maxDim / sampleSize > MAX_AVATAR_PX * 2) {
          sampleSize *= 2
        }

        val decodeOpts =
          BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
          }
        val decoded =
          resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOpts)
          } ?: return@withContext Result.failure(IllegalStateException("Could not decode gallery photo"))

        val dataUri = encodeBitmapToAvatarDataUri(decoded)
        if (!decoded.isRecycled) decoded.recycle()
        Result.success(dataUri)
      } catch (e: Exception) {
        Result.failure(e)
      }
    }

  fun encodeBitmapToAvatarDataUri(source: Bitmap): String {
    val square = centerCropAndScaleSquare(source, MAX_AVATAR_PX)
    var quality = 82
    var bytes: ByteArray
    while (true) {
      val out = ByteArrayOutputStream()
      square.compress(Bitmap.CompressFormat.JPEG, quality, out)
      bytes = out.toByteArray()
      if (bytes.size * 4 / 3 <= MAX_DATA_URI_LENGTH || quality <= 45) {
        break
      }
      quality -= 10
    }
    if (square !== source && !square.isRecycled) {
      square.recycle()
    }
    val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
    return "data:image/jpeg;base64,$encoded"
  }

  private fun centerCropAndScaleSquare(source: Bitmap, targetPx: Int): Bitmap {
    val width = source.width.coerceAtLeast(1)
    val height = source.height.coerceAtLeast(1)
    val side = min(width, height)
    val xOffset = (width - side) / 2
    val yOffset = (height - side) / 2
    val cropped =
      if (width != height) {
        Bitmap.createBitmap(source, xOffset, yOffset, side, side)
      } else {
        source
      }
    if (side <= targetPx) {
      return cropped
    }
    val scaled = Bitmap.createScaledBitmap(cropped, targetPx, targetPx, true)
    if (cropped !== source && cropped !== scaled && !cropped.isRecycled) {
      cropped.recycle()
    }
    return scaled
  }
}
