package com.nothing4apro.hamsterwheel

import android.content.ComponentName
import android.content.Context
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.provider.Settings
import android.service.notification.NotificationListenerService

/**
 * Lightweight NotificationListenerService that allows reading active MediaSessions
 * so we can ignore video apps like YouTube (`com.google.android.youtube`, ReVanced, browsers)
 * while reacting to music players (Spotify, YouTube Music, Yandex Music, Apple Music, etc.).
 */
class MediaSessionMonitorService : NotificationListenerService() {

    companion object {
        // Video / browser packages that should NOT trigger the dancing hamster
        private val IGNORED_VIDEO_PACKAGES = setOf(
            "com.google.android.youtube",
            "app.revanced.android.youtube",
            "app.rvx.android.youtube",
            "org.schabi.newpipe",
            "com.zhiliaoapp.musically", // TikTok
            "com.ss.android.ugc.trill", // TikTok
            "com.instagram.android",
            "tv.twitch.android.app",
            "com.netflix.mediaclient",
            "org.videolan.vlc",
            "com.mxtech.videoplayer.ad",
            "com.mxtech.videoplayer.pro"
        )

        fun isNotificationListenerGranted(context: Context): Boolean {
            val flat = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            ) ?: return false
            val cn = ComponentName(context, MediaSessionMonitorService::class.java).flattenToString()
            return flat.contains(cn)
        }

        /**
         * Returns:
         * - `true` if an actual music app is actively playing
         * - `false` if only YouTube / video app is playing, or nothing is playing
         * - `null` if NotificationListener permission isn't granted yet (fallback to AudioManager)
         */
        fun isMusicAppPlayingExcludingYoutube(context: Context): Boolean? {
            if (!isNotificationListenerGranted(context)) return null
            return try {
                val msm = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
                    ?: return null
                val component = ComponentName(context, MediaSessionMonitorService::class.java)
                val controllers: List<MediaController> = msm.getActiveSessions(component)
                if (controllers.isEmpty()) return false

                var anyAllowedMusicPlaying = false
                var anyIgnoredVideoPlaying = false

                for (controller in controllers) {
                    val state = controller.playbackState?.state
                    val isPlaying = state == PlaybackState.STATE_PLAYING
                    if (!isPlaying) continue

                    val pkg = controller.packageName.lowercase()
                    // Keep YouTube Music ("com.google.android.apps.youtube.music") allowed,
                    // but block regular YouTube ("com.google.android.youtube") and video players!
                    val isYoutubeVideo = (pkg == "com.google.android.youtube" ||
                            pkg.endsWith(".youtube") && !pkg.contains("music"))
                    if (isYoutubeVideo || IGNORED_VIDEO_PACKAGES.contains(pkg)) {
                        anyIgnoredVideoPlaying = true
                    } else {
                        anyAllowedMusicPlaying = true
                    }
                }

                when {
                    anyAllowedMusicPlaying -> true
                    anyIgnoredVideoPlaying -> false
                    else -> false
                }
            } catch (_: Throwable) {
                null
            }
        }
    }
}
