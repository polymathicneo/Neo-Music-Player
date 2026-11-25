package com.vibeplay.music

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.vibeplay.music.player.PlaybackService
import com.vibeplay.music.ui.VibePlayApp
import com.vibeplay.music.ui.theme.VibePlayTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var openNowPlayingFromIntent by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        openNowPlayingFromIntent = intent?.getBooleanExtra(PlaybackService.EXTRA_OPEN_NOW_PLAYING, false) == true

        setContent {
            VibePlayTheme {
                VibePlayApp(openNowPlayingInitially = openNowPlayingFromIntent)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(PlaybackService.EXTRA_OPEN_NOW_PLAYING, false)) {
            openNowPlayingFromIntent = true
        }
    }
}
