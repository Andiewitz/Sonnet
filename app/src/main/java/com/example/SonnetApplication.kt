package com.example

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.util.DebugLogger
import com.example.di.AppContainer

class SonnetApplication : Application(), ImageLoaderFactory {
    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            android.util.Log.e("SonnetCrash", "FATAL CRASH on thread ${thread.name}: ${throwable.message}", throwable)
            try {
                val crashFile = filesDir.resolve("last_crash.txt")
                crashFile.writeText("Crash on ${java.util.Date()}:\n" + throwable.stackTraceToString())
            } catch (e: Exception) {
                // Ignore failure writing crash file
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }

        // Access container on Main Thread to eagerly initialize
        container
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(100L * 1024 * 1024) // 100MB
                    .build()
            }
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .respectCacheHeaders(false)
            .crossfade(150)
            .build()
    }
}

