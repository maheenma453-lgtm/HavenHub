package com.example.havenhub.utils
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageCompressor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // ── Compress image from Uri ────────────────────────────────────────
    fun compressFromUri(uri: Uri, maxSizeKb: Int = Constants.MAX_IMAGE_SIZE_KB): ByteArray? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val original = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            compress(original, maxSizeKb)
        } catch (e: Exception) {
            null
        }
    }

    // ── Compress Bitmap to ByteArray ──────────────────────────────────
    fun compress(bitmap: Bitmap, maxSizeKb: Int = Constants.MAX_IMAGE_SIZE_KB): ByteArray {
        val outputStream = ByteArrayOutputStream()
        var quality = 100
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        while (outputStream.toByteArray().size / 1024 > maxSizeKb && quality > 10) {
            outputStream.reset()
            quality -= 10
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        }
        return outputStream.toByteArray()
    }

    // ── Save ByteArray to temp file ───────────────────────────────────
    fun saveToTempFile(bytes: ByteArray, fileName: String = "img_${System.currentTimeMillis()}.jpg"): File? {
        return try {
            val file = File(context.cacheDir, fileName)
            FileOutputStream(file).use { it.write(bytes) }
            file
        } catch (e: Exception) {
            null
        }
    }

    // ── Resize Bitmap ─────────────────────────────────────────────────
    fun resize(bitmap: Bitmap, maxWidth: Int = 1024, maxHeight: Int = 1024): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxWidth && height <= maxHeight) return bitmap
        val ratio = minOf(maxWidth.toFloat() / width, maxHeight.toFloat() / height)
        return Bitmap.createScaledBitmap(bitmap, (width * ratio).toInt(), (height * ratio).toInt(), true)
    }

    // ── Size in KB ────────────────────────────────────────────────────
    fun sizeInKb(bytes: ByteArray): Int = bytes.size / 1024

    // ── Clear image cache ─────────────────────────────────────────────
    fun clearCache() {
        context.cacheDir.listFiles()?.forEach { file ->
            if (file.name.startsWith("img_")) file.delete()
        }
    }
}