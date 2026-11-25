package com.vibeplay.music.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.vibeplay.music.domain.model.Song
import com.vibeplay.music.ui.components.AddToPlaylistSheet
import com.vibeplay.music.ui.components.CreatePlaylistDialog
import com.vibeplay.music.ui.components.MiniPlayer
import com.vibeplay.music.ui.components.PermissionGateScreen
import com.vibeplay.music.ui.navigation.CollectionType
import com.vibeplay.music.ui.navigation.Route
import com.vibeplay.music.ui.screens.detail.CollectionDetailScreen
import com.vibeplay.music.ui.screens.equalizer.EqualizerScreen
import com.vibeplay.music.ui.screens.home.HomeScreen
import com.vibeplay.music.ui.screens.library.LibraryScreen
import com.vibeplay.music.ui.screens.nowplaying.FullLyricsOverlay
import com.vibeplay.music.ui.screens.nowplaying.LyricsPreviewCard
import com.vibeplay.music.ui.screens.nowplaying.NowPlayingScreen
import com.vibeplay.music.ui.screens.nowplaying.SleepTimerSheet
import com.vibeplay.music.ui.screens.queue.QueueSheet
import com.vibeplay.music.ui.screens.search.SearchScreen
import com.vibeplay.music.ui.screens.settings.SettingsScreen
import com.vibeplay.music.ui.theme.VibeBackground
import com.vibeplay.music.ui.theme.VibePrimaryText
import com.vibeplay.music.ui.theme.VibeSecondaryText
import com.vibeplay.music.ui.theme.VibeSurface
import com.vibeplay.music.ui.viewmodel.CollectionDetailViewModel
import com.vibeplay.music.ui.viewmodel.EqualizerViewModel
import com.vibeplay.music.ui.viewmodel.HomeViewModel
import com.vibeplay.music.ui.viewmodel.LibraryViewModel
import com.vibeplay.music.ui.viewmodel.MainPlaybackViewModel
import com.vibeplay.music.ui.viewmodel.SearchViewModel
import com.vibeplay.music.ui.viewmodel.SettingsViewModel

@Composable
fun VibePlayApp(
    openNowPlayingInitially: Boolean = false,
    playbackViewModel: MainPlaybackViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val audioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, audioPermission) == PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionRequestedOnce by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        permissionRequestedOnce = true
        hasAudioPermission = results[audioPermission] == true ||
            ContextCompat.checkSelfPermission(context, audioPermission) == PackageManager.PERMISSION_GRANTED
    }

    LaunchedEffect(hasAudioPermission) {
        if (hasAudioPermission) {
            playbackViewModel.onPermissionGranted()
        }
    }

    if (!hasAudioPermission) {
        PermissionGateScreen(
            permanentlyDenied = permissionRequestedOnce,
            onRequestPermission = {
                val perms = buildList {
                    add(audioPermission)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }.toTypedArray()
                permissionLauncher.launch(perms)
            }
        )
        return
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val playbackState by playbackViewModel.playbackState.collectAsStateWithLifecycle()
    val dominantColor by playbackViewModel.dominantColor.collectAsStateWithLifecycle()
    val lyricsData by playbackViewModel.lyricsData.collectAsStateWithLifecycle()
    val sleepTimerState by playbackViewModel.sleepTimerState.collectAsStateWithLifecycle()
    val playlists by playbackViewModel.playlists.collectAsStateWithLifecycle()

    var showNowPlaying by remember(openNowPlayingInitially) {
        mutableStateOf(openNowPlayingInitially && playbackState.currentSong != null)
    }
    var showQueueSheet by remember { mutableStateOf(false) }
    var showSleepTimerSheet by remember { mutableStateOf(false) }
    var showFullLyrics by remember { mutableStateOf(false) }
    var songForPlaylistSheet by remember { mutableStateOf<Song?>(null) }
    var showCreatePlaylistForSong by remember { mutableStateOf(false) }

    val showBottomBar = currentRoute in setOf(
        Route.Home.route,
        Route.Search.route,
        Route.Library.route
    ) || currentRoute?.startsWith("detail/") == true

    val openCollection: (CollectionType, String, String) -> Unit = { type, id, title ->
        navController.navigate(Route.CollectionDetail.createRoute(type, id, title))
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = VibeBackground,
            bottomBar = {
                Column(modifier = Modifier.navigationBarsPadding()) {
                    if (playbackState.currentSong != null && !showNowPlaying) {
                        MiniPlayer(
                            playbackState = playbackState,
                            dominantColor = dominantColor,
                            onExpand = { showNowPlaying = true },
                            onTogglePlayPause = playbackViewModel::togglePlayPause,
                            onToggleFavorite = {
                                playbackState.currentSong?.let { playbackViewModel.toggleFavorite(it.id) }
                            }
                        )
                    }
                    if (showBottomBar) {
                        NavigationBar(
                            containerColor = VibeSurface.copy(alpha = 0.96f),
                            tonalElevation = 0.dp
                        ) {
                            val navColors = NavigationBarItemDefaults.colors(
                                selectedIconColor = VibePrimaryText,
                                selectedTextColor = VibePrimaryText,
                                unselectedIconColor = VibeSecondaryText,
                                unselectedTextColor = VibeSecondaryText,
                                indicatorColor = Color.Transparent
                            )
                            NavigationBarItem(
                                selected = currentRoute == Route.Home.route,
                                onClick = {
                                    navController.navigate(Route.Home.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(Icons.Rounded.Home, contentDescription = "Home") },
                                label = { Text("Home") },
                                colors = navColors
                            )
                            NavigationBarItem(
                                selected = currentRoute == Route.Search.route,
                                onClick = {
                                    navController.navigate(Route.Search.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(Icons.Rounded.Search, contentDescription = "Search") },
                                label = { Text("Search") },
                                colors = navColors
                            )
                            NavigationBarItem(
                                selected = currentRoute == Route.Library.route,
                                onClick = {
                                    navController.navigate(Route.Library.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(Icons.Rounded.LibraryMusic, contentDescription = "Library") },
                                label = { Text("Your Library") },
                                colors = navColors
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Route.Home.route
            ) {
                composable(Route.Home.route) {
                    val vm: HomeViewModel = hiltViewModel()
                    val state by vm.uiState.collectAsStateWithLifecycle()
                    HomeScreen(
                        uiState = state,
                        currentSongId = playbackState.currentSong?.id,
                        onOpenSettings = { navController.navigate(Route.Settings.route) },
                        onOpenEqualizer = { navController.navigate(Route.Equalizer.route) },
                        onOpenCollection = openCollection,
                        onPlaySong = { song, queue -> playbackViewModel.playSong(song, queue) },
                        onPlayNext = playbackViewModel::playNext,
                        onAddToQueue = playbackViewModel::addToQueue,
                        onAddToPlaylist = { songForPlaylistSheet = it },
                        onToggleFavorite = { playbackViewModel.toggleFavorite(it.id) },
                        onRescan = vm::rescanLibrary,
                        bottomPadding = innerPadding
                    )
                }

                composable(Route.Search.route) {
                    val vm: SearchViewModel = hiltViewModel()
                    val state by vm.uiState.collectAsStateWithLifecycle()
                    SearchScreen(
                        uiState = state,
                        currentSongId = playbackState.currentSong?.id,
                        onQueryChange = vm::onQueryChange,
                        onCommitQuery = vm::commitSearchQuery,
                        onSelectFilter = vm::selectFilter,
                        onDeleteHistoryItem = vm::deleteHistoryItem,
                        onClearHistory = vm::clearHistory,
                        onPlaySong = { song, queue -> playbackViewModel.playSong(song, queue) },
                        onPlayNext = playbackViewModel::playNext,
                        onAddToQueue = playbackViewModel::addToQueue,
                        onAddToPlaylist = { songForPlaylistSheet = it },
                        onToggleFavorite = { playbackViewModel.toggleFavorite(it.id) },
                        onOpenCollection = openCollection,
                        bottomPadding = innerPadding
                    )
                }

                composable(Route.Library.route) {
                    val vm: LibraryViewModel = hiltViewModel()
                    val state by vm.uiState.collectAsStateWithLifecycle()
                    LibraryScreen(
                        uiState = state,
                        currentSongId = playbackState.currentSong?.id,
                        onSelectTab = vm::selectTab,
                        onSelectSortOption = vm::selectSortOption,
                        onToggleSortDirection = vm::toggleSortDirection,
                        onCreatePlaylist = vm::createPlaylist,
                        onDeletePlaylist = vm::deletePlaylist,
                        onOpenCollection = openCollection,
                        onPlaySong = { song, queue -> playbackViewModel.playSong(song, queue) },
                        onPlayQueue = { songs, shuffle -> playbackViewModel.playQueue(songs, 0, shuffle) },
                        onPlayNext = playbackViewModel::playNext,
                        onAddToQueue = playbackViewModel::addToQueue,
                        onAddToPlaylist = { songForPlaylistSheet = it },
                        onToggleFavorite = { playbackViewModel.toggleFavorite(it.id) },
                        bottomPadding = innerPadding
                    )
                }

                composable(
                    route = Route.CollectionDetail.route,
                    arguments = listOf(
                        navArgument("type") { type = NavType.StringType },
                        navArgument("id") { type = NavType.StringType },
                        navArgument("title") { type = NavType.StringType }
                    )
                ) {
                    val vm: CollectionDetailViewModel = hiltViewModel()
                    val state by vm.uiState.collectAsStateWithLifecycle()
                    CollectionDetailScreen(
                        uiState = state,
                        currentSongId = playbackState.currentSong?.id,
                        onBack = { navController.popBackStack() },
                        onSelectSortOption = vm::selectSortOption,
                        onToggleSortDirection = vm::toggleSortDirection,
                        onPlayAll = { songs, shuffle -> playbackViewModel.playQueue(songs, 0, shuffle) },
                        onPlaySong = { song, queue -> playbackViewModel.playSong(song, queue) },
                        onPlayNext = playbackViewModel::playNext,
                        onAddToQueue = playbackViewModel::addToQueue,
                        onAddToPlaylist = { songForPlaylistSheet = it },
                        onToggleFavorite = { playbackViewModel.toggleFavorite(it.id) },
                        onRenamePlaylist = vm::renamePlaylist,
                        onDeletePlaylist = { vm.deletePlaylist { navController.popBackStack() } },
                        onRemoveFromPlaylist = vm::removeSongFromPlaylist,
                        bottomPadding = innerPadding
                    )
                }

                composable(Route.Settings.route) {
                    val vm: SettingsViewModel = hiltViewModel()
                    val state by vm.uiState.collectAsStateWithLifecycle()
                    SettingsScreen(
                        uiState = state,
                        onBack = { navController.popBackStack() },
                        onRescan = vm::rescanLibrary,
                        onSetMinDurationSeconds = vm::setMinDurationSeconds,
                        onToggleExcludedFolder = vm::toggleExcludedFolder,
                        onToggleIncludedFolder = vm::toggleIncludedFolder,
                        onClearIncludedFolders = vm::clearIncludedFolders,
                        onSetCrossfadeSeconds = vm::setCrossfadeSeconds,
                        onOpenEqualizer = { navController.navigate(Route.Equalizer.route) },
                        bottomPadding = innerPadding
                    )
                }

                composable(Route.Equalizer.route) {
                    val vm: EqualizerViewModel = hiltViewModel()
                    val state by vm.uiState.collectAsStateWithLifecycle()
                    EqualizerScreen(
                        uiState = state,
                        onBack = { navController.popBackStack() },
                        onSetEnabled = vm::setEnabled,
                        onBandLevelChange = vm::setBandLevel,
                        onBassBoostChange = vm::setBassBoost,
                        onSelectPreset = vm::selectPreset,
                        onSaveCustomPreset = vm::saveCustomPreset,
                        onDeleteCustomPreset = vm::deleteCustomPreset,
                        bottomPadding = innerPadding
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = showNowPlaying && playbackState.currentSong != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            NowPlayingScreen(
                playbackState = playbackState,
                dominantColor = dominantColor,
                sleepTimerState = sleepTimerState,
                onCollapse = { showNowPlaying = false },
                onTogglePlayPause = playbackViewModel::togglePlayPause,
                onSkipNext = playbackViewModel::skipNext,
                onSkipPrevious = playbackViewModel::skipPrevious,
                onSeekTo = playbackViewModel::seekTo,
                onToggleShuffle = playbackViewModel::toggleShuffle,
                onCycleRepeat = playbackViewModel::cycleRepeatMode,
                onToggleFavorite = {
                    playbackState.currentSong?.let { playbackViewModel.toggleFavorite(it.id) }
                },
                onOpenQueue = { showQueueSheet = true },
                onOpenEqualizer = {
                    showNowPlaying = false
                    navController.navigate(Route.Equalizer.route)
                },
                onOpenSleepTimer = { showSleepTimerSheet = true },
                lyricsSlot = {
                    LyricsPreviewCard(
                        lyricsData = lyricsData,
                        positionMs = playbackState.positionMs,
                        accentColor = dominantColor,
                        onExpand = { showFullLyrics = true }
                    )
                }
            )
        }

        val currentSongForLyrics = playbackState.currentSong
        if (showFullLyrics && currentSongForLyrics != null) {
            FullLyricsOverlay(
                songTitle = currentSongForLyrics.title,
                artistName = currentSongForLyrics.artist,
                lyricsData = lyricsData,
                positionMs = playbackState.positionMs,
                accentColor = dominantColor,
                onSeekTo = playbackViewModel::seekTo,
                onClose = { showFullLyrics = false }
            )
        }

        if (showSleepTimerSheet) {
            SleepTimerSheet(
                sleepTimerState = sleepTimerState,
                onSelectMinutes = playbackViewModel::startSleepTimerMinutes,
                onSelectEndOfTrack = playbackViewModel::startSleepTimerEndOfTrack,
                onCancelTimer = playbackViewModel::cancelSleepTimer,
                onDismiss = { showSleepTimerSheet = false }
            )
        }

        if (showQueueSheet) {
            QueueSheet(
                playbackState = playbackState,
                onPlayIndex = playbackViewModel::playQueueIndex,
                onMoveItem = playbackViewModel::moveQueueItem,
                onRemoveItem = playbackViewModel::removeQueueItem,
                onDismiss = { showQueueSheet = false }
            )
        }

        val targetSong = songForPlaylistSheet
        if (targetSong != null) {
            AddToPlaylistSheet(
                song = targetSong,
                playlists = playlists,
                onSelectPlaylist = { playlist ->
                    playbackViewModel.addSongToPlaylist(playlist.id, targetSong.id)
                    songForPlaylistSheet = null
                },
                onCreateNewPlaylist = { showCreatePlaylistForSong = true },
                onDismiss = { songForPlaylistSheet = null }
            )
        }

        if (showCreatePlaylistForSong) {
            CreatePlaylistDialog(
                onConfirm = { name ->
                    playbackViewModel.createPlaylistAndAddSong(name, songForPlaylistSheet?.id)
                    showCreatePlaylistForSong = false
                    songForPlaylistSheet = null
                },
                onDismiss = { showCreatePlaylistForSong = false }
            )
        }
    }
}
