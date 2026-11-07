package com.nothing4apro.hamsterwheel

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class PowerStateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        HamsterAnimatorEngine.loadPrefs(context)
        if (!HamsterAnimatorEngine.isGlyphEnabled) return

        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                HamsterForegroundService.startService(context)
            }
            Intent.ACTION_POWER_CONNECTED -> {
                HamsterForegroundService.startService(context)
                HamsterAnimatorEngine.onPowerStateChanged(context, true)
            }
            Intent.ACTION_POWER_DISCONNECTED -> {
                HamsterForegroundService.startService(context)
                HamsterAnimatorEngine.onPowerStateChanged(context, false)
            }
        }
    }
}
