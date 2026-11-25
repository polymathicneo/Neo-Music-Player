package com.vibeplay.music

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import com.vibeplay.music.data.image.AudioArtworkKeyer
import com.vibeplay.music.data.image.EmbeddedArtFetcher
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class VibePlayApplication : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(AudioArtworkKeyer())
                add(EmbeddedArtFetcher.Factory(this@VibePlayApplication))
            }
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.20)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("artwork_cache"))
                    .maxSizePercent(0.05)
                    .build()
            }
            .networkCachePolicy(CachePolicy.DISABLED)
            .crossfade(true)
            .build()
    }
}
