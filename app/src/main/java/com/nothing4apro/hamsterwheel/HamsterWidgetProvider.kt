package com.nothing4apro.hamsterwheel

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

/**
 * Home Screen & Lock Screen (keyguard) Widget that mirrors the 13×13 Charging Hamster
 * circle in real time (sleeping, running on charge, 360° clockwise wheel spin on unplug, and dancing to music).
 */
class HamsterWidgetProvider : AppWidgetProvider() {

    companion object {
        fun hasActiveWidgets(context: Context): Boolean {
            return try {
                val mgr = AppWidgetManager.getInstance(context)
                val ids = mgr.getAppWidgetIds(ComponentName(context, HamsterWidgetProvider::class.java))
                ids != null && ids.isNotEmpty()
            } catch (_: Throwable) {
                false
            }
        }

        fun updateAllWidgets(context: Context, frame169: IntArray) {
            try {
                val mgr = AppWidgetManager.getInstance(context)
                val cn = ComponentName(context, HamsterWidgetProvider::class.java)
                val ids = mgr.getAppWidgetIds(cn)
                if (ids == null || ids.isEmpty()) return

                val bmp = HamsterFrames13x13.toWidgetBitmap(frame169, sizePx = 260)
                val openIntent = Intent(context, MainActivity::class.java)
                val pendingOpen = PendingIntent.getActivity(
                    context,
                    0,
                    openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val views = RemoteViews(context.packageName, R.layout.hamster_widget).apply {
                    setImageViewBitmap(R.id.widget_matrix_image, bmp)
                    setOnClickPendingIntent(R.id.widget_matrix_image, pendingOpen)
                }

                mgr.updateAppWidget(ids, views)
            } catch (_: Throwable) {}
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        HamsterAnimatorEngine.loadPrefs(context)
        if (HamsterAnimatorEngine.isGlyphEnabled) {
            HamsterForegroundService.startService(context)
            HamsterAnimatorEngine.start(context)
        }
        updateAllWidgets(context, HamsterAnimatorEngine.getCurrentFrame())
    }
}
