package com.example.data

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.util.Base64
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
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

        // Strategy 1 (Android 9+ / API 28+): ImageDecoder handles content://, cloud PickVisualMedia,
        // HEIC/WebP, and EXIF rotation in a single native pass without requiring multiple streams.
        val nativeBitmap =
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            runCatching { decodeWithImageDecoder(resolver, uri) }.getOrNull()
          } else {
            null
          }

        val resolvedBitmap =
          nativeBitmap
            ?: run {
              val rawBytes = readUriBytesOnce(resolver, uri)
                ?: return@withContext Result.failure(
                  IllegalArgumentException("Unable to read selected image")
                )
              decodeBitmapFromBytes(rawBytes)
                ?: return@withContext Result.failure(
                  IllegalArgumentException("Unsupported or corrupted image selected")
                )
            }

        val dataUri = encodeBitmapToAvatarDataUri(resolvedBitmap)
        if (!resolvedBitmap.isRecycled) resolvedBitmap.recycle()
        Result.success(dataUri)
      } catch (e: SecurityException) {
        Result.failure(
          IllegalStateException("Gallery photo permission expired. Please pick the photo again.", e)
        )
      } catch (e: Exception) {
        Result.failure(e)
      }
    }

  private fun decodeWithImageDecoder(resolver: ContentResolver, uri: Uri): Bitmap {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
      throw IllegalStateException("ImageDecoder requires API 28+")
    }
    val source = ImageDecoder.createSource(resolver, uri)
    return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
      decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
      val maxDim = maxOf(info.size.width, info.size.height)
      var sample = 1
      while (maxDim / sample > MAX_AVATAR_PX * 2) {
        sample *= 2
      }
      if (sample > 1) {
        decoder.setTargetSampleSize(sample)
      }
    }
  }

  private fun readUriBytesOnce(resolver: ContentResolver, uri: Uri): ByteArray? {
    // Attempt 1: standard ContentResolver InputStream
    runCatching {
      resolver.openInputStream(uri)?.use { stream ->
        val bytes = stream.readBytes()
        if (bytes.isNotEmpty()) return bytes
      }
    }

    // Attempt 2: AssetFileDescriptor (handles virtual documents & Google Photos cloud providers)
    runCatching {
      resolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
        afd.createInputStream().use { stream ->
          val bytes = stream.readBytes()
          if (bytes.isNotEmpty()) return bytes
        }
      }
    }

    // Attempt 3: ParcelFileDescriptor
    runCatching {
      resolver.openFileDescriptor(uri, "r")?.use { pfd ->
        FileInputStream(pfd.fileDescriptor).use { stream ->
          val bytes = stream.readBytes()
          if (bytes.isNotEmpty()) return bytes
        }
      }
    }

    return null
  }

  fun decodeBitmapFromBytes(bytes: ByteArray): Bitmap? {
    if (bytes.isEmpty()) return null
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

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
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOpts) ?: return null
    return applyExifOrientationIfNeeded(bitmap, bytes)
  }

  private fun applyExifOrientationIfNeeded(bitmap: Bitmap, bytes: ByteArray): Bitmap {
    val orientation =
      runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
          ExifInterface(ByteArrayInputStream(bytes))
            .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } else {
          ExifInterface.ORIENTATION_NORMAL
        }
      }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

    val matrix = Matrix()
    when (orientation) {
      ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
      ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
      ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
      ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
      ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
      else -> return bitmap
    }

    return runCatching {
      val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
      if (rotated !== bitmap && !bitmap.isRecycled) {
        bitmap.recycle()
      }
      rotated
    }.getOrDefault(bitmap)
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
