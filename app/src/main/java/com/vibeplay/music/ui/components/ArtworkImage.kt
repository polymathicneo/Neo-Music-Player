package com.vibeplay.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.vibeplay.music.data.image.AudioArtwork
import com.vibeplay.music.ui.theme.VibeMutedText
import com.vibeplay.music.ui.theme.VibeSurfaceElevated

@Composable
fun ArtworkImage(
    songId: Long?,
    contentUri: String?,
    albumArtUri: String?,
    contentDescription: String?,
    shape: Shape,
    modifier: Modifier = Modifier,
    iconSizeDp: Int = 24
) {
    var hasFailed by remember(songId, contentUri, albumArtUri) { mutableStateOf(false) }

    Box(
        modifier = modifier
            .clip(shape)
            .background(VibeSurfaceElevated),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.MusicNote,
            contentDescription = null,
            tint = VibeMutedText,
            modifier = Modifier.size(iconSizeDp.dp)
        )

        if (songId != null && !contentUri.isNullOrBlank() && !hasFailed) {
            AsyncImage(
                model = AudioArtwork(
                    songId = songId,
                    contentUri = contentUri,
                    albumArtUri = albumArtUri
                ),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                onError = { hasFailed = true },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
