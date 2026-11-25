package com.vibeplay.music.ui.screens.nowplaying

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vibeplay.music.domain.model.PlaybackState
import com.vibeplay.music.domain.model.RepeatMode
import com.vibeplay.music.domain.model.SleepTimerMode
import com.vibeplay.music.domain.model.SleepTimerState
import com.vibeplay.music.ui.components.ArtworkImage
import com.vibeplay.music.ui.components.formatDuration
import com.vibeplay.music.ui.theme.VibeBackground
import com.vibeplay.music.ui.theme.VibeGreen
import com.vibeplay.music.ui.theme.VibePrimaryText
import com.vibeplay.music.ui.theme.VibeSecondaryText

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NowPlayingScreen(
    playbackState: PlaybackState,
    dominantColor: Color,
    sleepTimerState: SleepTimerState,
    onCollapse: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    lyricsSlot: @Composable () -> Unit = {}
) {
    val song = playbackState.currentSong ?: return
    val topColor by animateColorAsState(
        targetValue = lerp(VibeBackground, dominantColor, 0.72f),
        label = "np_top_gradient"
    )
    val midColor by animateColorAsState(
        targetValue = lerp(VibeBackground, dominantColor, 0.30f),
        label = "np_mid_gradient"
    )

    var isSeeking by remember { mutableStateOf(false) }
    var seekFraction by remember { mutableFloatStateOf(0f) }

    val durationMs = playbackState.durationMs.coerceAtLeast(1L)
    val displayFraction = if (isSeeking) {
        seekFraction
    } else {
        (playbackState.positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    }
    val displayPositionMs = (displayFraction * durationMs).toLong()

    var dragAccumulated by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(topColor, midColor, VibeBackground)
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCollapse) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Collapse",
                    tint = VibePrimaryText,
                    modifier = Modifier.size(32.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "PLAYING FROM LIBRARY",
                    style = MaterialTheme.typography.labelMedium,
                    color = VibeSecondaryText
                )
                Text(
                    text = song.album,
                    style = MaterialTheme.typography.labelLarge,
                    color = VibePrimaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onOpenEqualizer) {
                Icon(
                    imageVector = Icons.Rounded.Equalizer,
                    contentDescription = "Equalizer",
                    tint = VibePrimaryText
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .shadow(18.dp, RoundedCornerShape(12.dp))
                .pointerInput(song.id) {
                    detectHorizontalDragGestures(
                        onDragStart = { dragAccumulated = 0f },
                        onHorizontalDrag = { _, dragAmount ->
                            dragAccumulated += dragAmount
                        },
                        onDragEnd = {
                            if (dragAccumulated < -120f) {
                                onSkipNext()
                            } else if (dragAccumulated > 120f) {
                                onSkipPrevious()
                            }
                            dragAccumulated = 0f
                        }
                    )
                }
        ) {
            ArtworkImage(
                songId = song.id,
                contentUri = song.contentUri,
                albumArtUri = song.albumArtUri,
                contentDescription = song.album,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxSize(),
                iconSizeDp = 72
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = VibePrimaryText,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.titleMedium,
                    color = VibeSecondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (song.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (song.isFavorite) VibeGreen else VibePrimaryText,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Slider(
            value = displayFraction,
            onValueChange = { value ->
                isSeeking = true
                seekFraction = value
            },
            onValueChangeFinished = {
                onSeekTo((seekFraction * durationMs).toLong())
                isSeeking = false
            },
            colors = SliderDefaults.colors(
                thumbColor = VibePrimaryText,
                activeTrackColor = VibePrimaryText,
                inactiveTrackColor = Color.White.copy(alpha = 0.24f)
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatDuration(displayPositionMs),
                style = MaterialTheme.typography.labelMedium,
                color = VibeSecondaryText
            )
            Text(
                text = formatDuration(playbackState.durationMs),
                style = MaterialTheme.typography.labelMedium,
                color = VibeSecondaryText
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onToggleShuffle) {
                    Icon(
                        imageVector = Icons.Rounded.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (playbackState.shuffleEnabled) VibeGreen else VibeSecondaryText,
                        modifier = Modifier.size(26.dp)
                    )
                }
                if (playbackState.shuffleEnabled) {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .background(VibeGreen, CircleShape)
                    )
                }
            }

            IconButton(onClick = onSkipPrevious, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = Icons.Rounded.SkipPrevious,
                    contentDescription = "Previous",
                    tint = VibePrimaryText,
                    modifier = Modifier.size(38.dp)
                )
            }

            Surface(
                onClick = onTogglePlayPause,
                shape = CircleShape,
                color = VibePrimaryText,
                modifier = Modifier.size(68.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (playbackState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                        tint = VibeBackground,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }

            IconButton(onClick = onSkipNext, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = Icons.Rounded.SkipNext,
                    contentDescription = "Next",
                    tint = VibePrimaryText,
                    modifier = Modifier.size(38.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onCycleRepeat) {
                    Icon(
                        imageVector = if (playbackState.repeatMode == RepeatMode.ONE) {
                            Icons.Rounded.RepeatOne
                        } else {
                            Icons.Rounded.Repeat
                        },
                        contentDescription = "Repeat",
                        tint = if (playbackState.repeatMode != RepeatMode.OFF) VibeGreen else VibeSecondaryText,
                        modifier = Modifier.size(26.dp)
                    )
                }
                if (playbackState.repeatMode != RepeatMode.OFF) {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .background(VibeGreen, CircleShape)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onOpenEqualizer) {
                Icon(
                    imageVector = Icons.Rounded.Equalizer,
                    contentDescription = "Equalizer",
                    tint = VibeSecondaryText
                )
            }

            Surface(
                onClick = onOpenSleepTimer,
                shape = RoundedCornerShape(50),
                color = if (sleepTimerState.isActive) VibeGreen.copy(alpha = 0.2f) else Color.Transparent
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Bedtime,
                        contentDescription = "Sleep timer",
                        tint = if (sleepTimerState.isActive) VibeGreen else VibeSecondaryText,
                        modifier = Modifier.size(20.dp)
                    )
                    if (sleepTimerState.isActive) {
                        Spacer(modifier = Modifier.width(6.dp))
                        val label = when (sleepTimerState.mode) {
                            SleepTimerMode.END_OF_TRACK -> "End of track"
                            SleepTimerMode.DURATION -> formatDuration(sleepTimerState.remainingMs)
                            SleepTimerMode.OFF -> ""
                        }
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = VibeGreen
                        )
                    }
                }
            }

            IconButton(onClick = onOpenQueue) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                    contentDescription = "Queue",
                    tint = VibeSecondaryText
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        lyricsSlot()
        Spacer(modifier = Modifier.height(24.dp))
    }
}
