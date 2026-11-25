package com.vibeplay.music.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.vibeplay.music.MainActivity
import com.vibeplay.music.R
import com.vibeplay.music.data.local.prefs.UserPreferencesDataStore
import com.vibeplay.music.data.local.prefs.WidgetPlaybackSnapshot
import com.vibeplay.music.player.PlaybackService
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class VibePlayWidget : GlanceAppWidget() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WidgetEntryPoint {
        fun prefs(): UserPreferencesDataStore
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetEntryPoint::class.java
        )
        val snapshot = entryPoint.prefs().getWidgetSnapshotOnce()
        val artBitmap = withContext(Dispatchers.IO) {
            loadWidgetArtworkBitmap(context, snapshot)
        }

        provideContent {
            WidgetContent(
                context = context,
                snapshot = snapshot,
                artBitmap = artBitmap
            )
        }
    }

    @Composable
    private fun WidgetContent(
        context: Context,
        snapshot: WidgetPlaybackSnapshot,
        artBitmap: Bitmap?
    ) {
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(PlaybackService.EXTRA_OPEN_NOW_PLAYING, snapshot.songId > 0L)
        }

        Row(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color(0xFF181818))
                .cornerRadius(16.dp)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = GlanceModifier
                    .defaultWeight()
                    .clickable(actionStartActivity(launchIntent)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = GlanceModifier
                        .size(52.dp)
                        .background(Color(0xFF282828))
                        .cornerRadius(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (artBitmap != null) {
                        Image(
                            provider = ImageProvider(artBitmap),
                            contentDescription = snapshot.title,
                            contentScale = ContentScale.Crop,
                            modifier = GlanceModifier.fillMaxSize()
                        )
                    } else {
                        Image(
                            provider = ImageProvider(R.drawable.ic_widget_music_note),
                            contentDescription = "NeoMusic",
                            modifier = GlanceModifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = GlanceModifier.width(10.dp))

                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = snapshot.title,
                        style = TextStyle(
                            color = ColorProvider(Color.White),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1
                    )
                    Text(
                        text = snapshot.artist,
                        style = TextStyle(
                            color = ColorProvider(Color(0xFFB3B3B3)),
                            fontSize = 12.sp
                        ),
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = GlanceModifier.width(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = GlanceModifier
                        .size(36.dp)
                        .cornerRadius(18.dp)
                        .clickable(
                            actionRunCallback<WidgetPlaybackAction>(
                                actionParametersOf(
                                    WidgetPlaybackAction.ACTION_KEY to PlaybackService.ACTION_WIDGET_PREV
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_widget_prev),
                        contentDescription = "Previous",
                        modifier = GlanceModifier.size(22.dp)
                    )
                }

                Spacer(modifier = GlanceModifier.width(4.dp))

                Box(
                    modifier = GlanceModifier
                        .size(40.dp)
                        .background(Color(0xFF1DB954))
                        .cornerRadius(20.dp)
                        .clickable(
                            actionRunCallback<WidgetPlaybackAction>(
                                actionParametersOf(
                                    WidgetPlaybackAction.ACTION_KEY to PlaybackService.ACTION_WIDGET_PLAY_PAUSE
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = ImageProvider(
                            if (snapshot.isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play
                        ),
                        contentDescription = if (snapshot.isPlaying) "Pause" else "Play",
                        modifier = GlanceModifier.size(22.dp)
                    )
                }

                Spacer(modifier = GlanceModifier.width(4.dp))

                Box(
                    modifier = GlanceModifier
                        .size(36.dp)
                        .cornerRadius(18.dp)
                        .clickable(
                            actionRunCallback<WidgetPlaybackAction>(
                                actionParametersOf(
                                    WidgetPlaybackAction.ACTION_KEY to PlaybackService.ACTION_WIDGET_NEXT
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_widget_next),
                        contentDescription = "Next",
                        modifier = GlanceModifier.size(22.dp)
                    )
                }
            }
        }
    }

    private fun loadWidgetArtworkBitmap(context: Context, snapshot: WidgetPlaybackSnapshot): Bitmap? {
        if (snapshot.contentUri.isNotBlank()) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, Uri.parse(snapshot.contentUri))
                val bytes = retriever.embeddedPicture
                if (bytes != null && bytes.isNotEmpty()) {
                    return decodeSampledBitmap(bytes, 160, 160)
                }
            } catch (_: Throwable) {
                // Fall back to album art URI
            } finally {
                runCatching { retriever.release() }
            }
        }

        if (snapshot.albumArtUri.isNotBlank()) {
            runCatching {
                context.contentResolver.openInputStream(Uri.parse(snapshot.albumArtUri))?.use { stream ->
                    val bytes = stream.readBytes()
                    if (bytes.isNotEmpty()) {
                        return decodeSampledBitmap(bytes, 160, 160)
                    }
                }
            }
        }
        return null
    }

    private fun decodeSampledBitmap(bytes: ByteArray, reqWidth: Int, reqHeight: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var inSampleSize = 1
        while (bounds.outHeight / inSampleSize > reqHeight && bounds.outWidth / inSampleSize > reqWidth) {
            inSampleSize *= 2
        }
        val opts = BitmapFactory.Options().apply { this.inSampleSize = inSampleSize }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
    }
}
