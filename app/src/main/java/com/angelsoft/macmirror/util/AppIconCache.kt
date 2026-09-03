package com.angelsoft.macmirror.util

import android.content.Context
import androidx.collection.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * High-performance, memory-efficient LRU cache for Android app icons.
 * Keeps app icons in memory to prevent blocking the Main (UI) thread during LazyColumn scrolling.
 */
object AppIconCache {
    // 250 icons of 80x80 ARGB_8888 takes ~6.4MB of memory, easily fitting within app budget.
    private val memoryCache = LruCache<String, ImageBitmap>(250)

    fun get(packageName: String): ImageBitmap? {
        return memoryCache.get(packageName)
    }

    suspend fun loadIcon(context: Context, packageName: String): ImageBitmap? = withContext(Dispatchers.IO) {
        memoryCache.get(packageName)?.let { return@withContext it }
        try {
            val pm = context.packageManager
            val drawable = pm.getApplicationIcon(packageName)
            val bitmap = drawable.toBitmap(width = 80, height = 80).asImageBitmap()
            memoryCache.put(packageName, bitmap)
            bitmap
        } catch (e: Exception) {
            null
        }
    }
}
