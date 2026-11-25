package com.vibeplay.music.data.image

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Size
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.key.Keyer
import coil.request.Options
import okio.Buffer
import java.io.ByteArrayOutputStream

data class AudioArtwork(
    val songId: Long,
    val contentUri: String,
    val albumArtUri: String?
)

class AudioArtworkKeyer : Keyer<AudioArtwork> {
    override fun key(data: AudioArtwork, options: Options): String {
        return "audio_art_${data.songId}_${data.albumArtUri.orEmpty()}_${options.size}"
    }
}

class EmbeddedArtFetcher(
    private val context: Context,
    private val data: AudioArtwork,
    private val options: Options
) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        // 1. Try embedded artwork directly from audio track via MediaMetadataRetriever
        val embeddedBytes = extractEmbeddedPicture(data.contentUri)
        if (embeddedBytes != null && embeddedBytes.isNotEmpty()) {
            val buffer = Buffer().write(embeddedBytes)
            return SourceResult(
                source = ImageSource(buffer, context),
                mimeType = null,
                dataSource = DataSource.DISK
            )
        }

        // 2. Fallback to MediaStore album art URI
        val albumArt = data.albumArtUri ?: return null
        val albumUri = Uri.parse(albumArt)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching {
                val bitmap = context.contentResolver.loadThumbnail(
                    Uri.parse(data.contentUri),
                    Size(512, 512),
                    null
                )
                val out = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                bitmap.recycle()
                val buffer = Buffer().write(out.toByteArray())
                return SourceResult(
                    source = ImageSource(buffer, context),
                    mimeType = "image/jpeg",
                    dataSource = DataSource.DISK
                )
            }
        }

        runCatching {
            context.contentResolver.openInputStream(albumUri)?.use { stream ->
                val bytes = stream.readBytes()
                if (bytes.isNotEmpty()) {
                    val buffer = Buffer().write(bytes)
                    return SourceResult(
                        source = ImageSource(buffer, context),
                        mimeType = null,
                        dataSource = DataSource.DISK
                    )
                }
            }
        }

        return null
    }

    private fun extractEmbeddedPicture(contentUri: String): ByteArray? {
        if (contentUri.isBlank()) return null
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, Uri.parse(contentUri))
            retriever.embeddedPicture
        } catch (_: Throwable) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    class Factory(private val context: Context) : Fetcher.Factory<AudioArtwork> {
        override fun create(
            data: AudioArtwork,
            options: Options,
            imageLoader: ImageLoader
        ): Fetcher = EmbeddedArtFetcher(context, data, options)
    }
}
