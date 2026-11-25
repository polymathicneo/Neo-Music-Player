package com.vibeplay.music.widget

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.vibeplay.music.player.PlaybackService

class WidgetPlaybackAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val action = parameters[ACTION_KEY] ?: return
        val intent = Intent(context, PlaybackService::class.java).apply {
            this.action = action
        }
        try {
            context.startService(intent)
        } catch (_: IllegalStateException) {
            ContextCompat.startForegroundService(context, intent)
        }
    }

    companion object {
        val ACTION_KEY = ActionParameters.Key<String>("playback_action")
    }
}
