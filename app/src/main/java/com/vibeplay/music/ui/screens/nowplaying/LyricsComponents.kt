package com.vibeplay.music.ui.screens.nowplaying

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vibeplay.music.data.local.lyrics.LrcParser
import com.vibeplay.music.domain.model.LyricsData
import com.vibeplay.music.domain.model.LyricsSource
import com.vibeplay.music.ui.theme.VibeBackground
import com.vibeplay.music.ui.theme.VibePrimaryText
import com.vibeplay.music.ui.theme.VibeSecondaryText

@Composable
fun LyricsPreviewCard(
    lyricsData: LyricsData,
    positionMs: Long,
    accentColor: Color,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardColor = lerp(Color(0xFF1F3B2C), accentColor, 0.45f)
    val activeIdx = if (lyricsData.isSynced) {
        LrcParser.findActiveLineIndex(lyricsData.lines, positionMs)
    } else {
        0
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = !lyricsData.isEmpty, onClick = onExpand),
        shape = RoundedCornerShape(16.dp),
        color = cardColor
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val badge = when (lyricsData.source) {
                    LyricsSource.LRC_FILE -> "Lyrics • Synced (.lrc)"
                    LyricsSource.EMBEDDED_SYNCED -> "Lyrics • Embedded Synced"
                    LyricsSource.EMBEDDED_UNSYNCED -> "Lyrics • Embedded"
                    LyricsSource.NONE -> "Lyrics"
                }
                Text(
                    text = badge,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = VibePrimaryText
                )
                if (!lyricsData.isEmpty) {
                    IconButton(onClick = onExpand) {
                        Icon(
                            imageVector = Icons.Rounded.OpenInFull,
                            contentDescription = "Expand lyrics",
                            tint = VibePrimaryText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (lyricsData.isEmpty) {
                Text(
                    text = "No .lrc file or embedded lyrics found for this track.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = VibePrimaryText.copy(alpha = 0.78f)
                )
            } else {
                val startIdx = (activeIdx - 1).coerceAtLeast(0)
                val previewLines = lyricsData.lines.drop(startIdx).take(4)
                previewLines.forEachIndexed { offset, line ->
                    val actualIdx = startIdx + offset
                    val isCurrent = lyricsData.isSynced && actualIdx == activeIdx
                    Text(
                        text = line.text,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.SemiBold
                        ),
                        color = if (isCurrent) VibePrimaryText else VibePrimaryText.copy(alpha = 0.6f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(vertical = 3.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun FullLyricsOverlay(
    songTitle: String,
    artistName: String,
    lyricsData: LyricsData,
    positionMs: Long,
    accentColor: Color,
    onSeekTo: (Long) -> Unit,
    onClose: () -> Unit
) {
    val bgColor = lerp(VibeBackground, accentColor, 0.55f)
    val listState = rememberLazyListState()
    val activeIdx = if (lyricsData.isSynced) {
        LrcParser.findActiveLineIndex(lyricsData.lines, positionMs)
    } else {
        -1
    }

    LaunchedEffect(activeIdx) {
        if (activeIdx >= 0) {
            listState.animateScrollToItem((activeIdx - 2).coerceAtLeast(0))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = songTitle,
                        style = MaterialTheme.typography.titleLarge,
                        color = VibePrimaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = artistName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = VibePrimaryText.copy(alpha = 0.78f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close lyrics",
                        tint = VibePrimaryText
                    )
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 28.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                itemsIndexed(lyricsData.lines, key = { idx, line -> "${idx}_${line.timestampMs}" }) { index, line ->
                    val isActive = lyricsData.isSynced && index == activeIdx
                    val isPast = lyricsData.isSynced && activeIdx >= 0 && index < activeIdx
                    val textColor = when {
                        !lyricsData.isSynced -> VibePrimaryText
                        isActive -> VibePrimaryText
                        isPast -> VibePrimaryText.copy(alpha = 0.45f)
                        else -> VibeSecondaryText.copy(alpha = 0.65f)
                    }

                    Text(
                        text = line.text,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = if (isActive || !lyricsData.isSynced) FontWeight.Bold else FontWeight.SemiBold
                        ),
                        color = textColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = lyricsData.isSynced) {
                                onSeekTo(line.timestampMs)
                            }
                    )
                }
            }
        }
    }
}
