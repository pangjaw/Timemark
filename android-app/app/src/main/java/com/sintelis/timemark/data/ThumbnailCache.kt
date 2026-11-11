package com.sintelis.timemark.data

import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object ThumbnailCache {

    // ~16 MB cache for decoded UI thumbnails and previews
    private val cache = object : LruCache<String, ImageBitmap>(16 * 1024) {
        override fun sizeOf(key: String, value: ImageBitmap): Int {
            return (value.width * value.height * 4) / 1024
        }
    }

    suspend fun load(path: String, maxDimensionPx: Int): ImageBitmap? {
        if (path.isBlank()) return null
        return withContext(Dispatchers.IO) {
            val file = File(path)
            if (!file.exists()) return@withContext null

            val key = "$path:${file.lastModified()}:$maxDimensionPx"
            cache.get(key)?.let { return@withContext it }

            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            val sampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxDimensionPx)

            val opts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = android.graphics.Bitmap.Config.RGB_565
            }
            val decoded = BitmapFactory.decodeFile(file.absolutePath, opts) ?: return@withContext null
            val imageBitmap = decoded.asImageBitmap()
            cache.put(key, imageBitmap)
            imageBitmap
        }
    }

    private fun calculateInSampleSize(width: Int, height: Int, maxDim: Int): Int {
        var inSampleSize = 1
        val largest = maxOf(width, height)
        if (largest > maxDim && maxDim > 0) {
            while ((largest / (inSampleSize * 2)) >= maxDim) {
                inSampleSize *= 2
            }
        }
        return inSampleSize.coerceAtLeast(1)
    }
}

@Composable
fun rememberAsyncBitmap(path: String?, maxDimensionPx: Int = 220): ImageBitmap? {
    val state by produceState<ImageBitmap?>(initialValue = null, path, maxDimensionPx) {
        value = if (path != null) ThumbnailCache.load(path, maxDimensionPx) else null
    }
    return state
}
