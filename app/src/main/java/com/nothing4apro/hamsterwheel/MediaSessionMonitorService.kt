package com.nothing4apro.hamsterwheel

import android.content.ComponentName
import android.content.Context
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.provider.Settings
import android.service.notification.NotificationListenerService

/**
 * Monitors active MediaSessions so the hamster dances to music players while ignoring
 * YouTube, video players, and any custom apps added by the user to the exclusion list.
 */
class MediaSessionMonitorService : NotificationListenerService() {

    companion object {
        private const val PREFS_NAME = "hamster_glyph_prefs"
        private const val KEY_CUSTOM_IGNORED_PKGS = "custom_ignored_packages"

        val DEFAULT_IGNORED_PACKAGES = setOf(
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

        fun getCustomIgnoredPackages(context: Context): Set<String> {
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getStringSet(KEY_CUSTOM_IGNORED_PKGS, emptySet())?.toSet() ?: emptySet()
        }

        fun setCustomIgnoredPackages(context: Context, packages: Set<String>) {
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putStringSet(KEY_CUSTOM_IGNORED_PKGS, packages).apply()
        }

        fun addIgnoredPackage(context: Context, pkg: String) {
            val cleaned = pkg.trim().lowercase()
            if (cleaned.isEmpty()) return
            val current = getCustomIgnoredPackages(context).toMutableSet()
            current.add(cleaned)
            setCustomIgnoredPackages(context, current)
        }

        fun removeIgnoredPackage(context: Context, pkg: String) {
            val current = getCustomIgnoredPackages(context).toMutableSet()
            current.remove(pkg.trim().lowercase())
            setCustomIgnoredPackages(context, current)
        }

        fun isNotificationListenerGranted(context: Context): Boolean {
            val flat = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            ) ?: return false
            val cn = ComponentName(context, MediaSessionMonitorService::class.java).flattenToString()
            return flat.contains(cn)
        }

        fun isMusicAppPlayingExcludingIgnored(context: Context, filterVideoAndCustom: Boolean): Boolean? {
            if (!isNotificationListenerGranted(context)) return null
            return try {
                val msm = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
                    ?: return null
                val component = ComponentName(context, MediaSessionMonitorService::class.java)
                val controllers: List<MediaController> = msm.getActiveSessions(component)
                if (controllers.isEmpty()) return false

                val customIgnored = getCustomIgnoredPackages(context)
                var anyAllowedMusicPlaying = false
                var anyIgnoredPlaying = false

                for (controller in controllers) {
                    val state = controller.playbackState?.state
                    val isPlaying = state == PlaybackState.STATE_PLAYING
                    if (!isPlaying) continue

                    val pkg = controller.packageName.lowercase()
                    val isCustomBlocked = customIgnored.contains(pkg)
                    val isDefaultVideoBlocked = filterVideoAndCustom && (
                        pkg == "com.google.android.youtube" ||
                        (pkg.endsWith(".youtube") && !pkg.contains("music")) ||
                        DEFAULT_IGNORED_PACKAGES.contains(pkg)
                    )

                    if (isCustomBlocked || isDefaultVideoBlocked) {
                        anyIgnoredPlaying = true
                    } else {
                        anyAllowedMusicPlaying = true
                    }
                }

                when {
                    anyAllowedMusicPlaying -> true
                    anyIgnoredPlaying -> false
                    else -> false
                }
            } catch (_: Throwable) {
                null
            }
        }
    }
}
