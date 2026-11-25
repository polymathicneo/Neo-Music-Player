package com.vibeplay.music.ui.navigation

import android.net.Uri

enum class CollectionType {
    PLAYLIST,
    ALBUM,
    ARTIST,
    FOLDER,
    LIKED_SONGS
}

sealed class Route(val route: String) {
    data object Home : Route("home")
    data object Search : Route("search")
    data object Library : Route("library")
    data object Settings : Route("settings")
    data object Equalizer : Route("equalizer")

    data object CollectionDetail : Route("detail/{type}/{id}/{title}") {
        fun createRoute(type: CollectionType, id: String, title: String): String {
            return "detail/${type.name}/${Uri.encode(id)}/${Uri.encode(title)}"
        }
    }
}
