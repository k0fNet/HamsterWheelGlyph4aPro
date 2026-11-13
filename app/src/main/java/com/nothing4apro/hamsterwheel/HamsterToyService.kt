package com.nothing4apro.hamsterwheel

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.util.Log
import com.nothing.ketchum.GlyphToy

/**
 * Official Nothing OS Glyph Toy Service for Nothing Phone (4a) Pro (`Glyph.DEVICE_25111p`).
 * Also listens directly for power & screen-wake broadcasts while bound by Nothing OS so the
 * animation works seamlessly even when the user disables the Foreground Service notification!
 */
class HamsterToyService : Service() {

    companion object {
        private const val TAG = "HamsterToyService"
    }

    private var receiverRegistered = false

    private val powerAndScreenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (!HamsterAnimatorEngine.isGlyphEnabled) return
            when (intent.action) {
                Intent.ACTION_POWER_CONNECTED -> HamsterAnimatorEngine.onPowerStateChanged(context, true)
                Intent.ACTION_POWER_DISCONNECTED -> HamsterAnimatorEngine.onPowerStateChanged(context, false)
                Intent.ACTION_BATTERY_CHANGED -> {
                    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                    val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
                    val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                            status == BatteryManager.BATTERY_STATUS_FULL ||
                            plugged != 0
                    HamsterAnimatorEngine.onPowerStateChanged(context, isCharging)
                }
                Intent.ACTION_SCREEN_ON,
                Intent.ACTION_USER_PRESENT -> HamsterAnimatorEngine.onScreenWake()
            }
        }
    }

    private val serviceHandler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            if (msg.what == GlyphToy.MSG_GLYPH_TOY) {
                val bundle = msg.data
                val event = bundle?.getString(GlyphToy.MSG_GLYPH_TOY_DATA)
                when (event) {
                    GlyphToy.EVENT_AOD -> {
                        HamsterAnimatorEngine.loadPrefs(applicationContext)
                        if (HamsterAnimatorEngine.isGlyphEnabled) {
                            HamsterAnimatorEngine.start(applicationContext)
                        }
                    }
                    GlyphToy.EVENT_CHANGE -> {
                        val currentOverride = HamsterAnimatorEngine.manualChargingOverride
                        val next = if (currentOverride == null) {
                            !HamsterAnimatorEngine.isChargingNow
                        } else {
                            !currentOverride
                        }
                        HamsterAnimatorEngine.setManualOverride(next)
                    }
                }
            } else {
                super.handleMessage(msg)
            }
        }
    }

    private val serviceMessenger = Messenger(serviceHandler)

    override fun onBind(intent: Intent?): IBinder {
        Log.i(TAG, "HamsterToyService bound by Nothing OS")
        registerDynamicReceivers()
        HamsterAnimatorEngine.loadPrefs(applicationContext)
        HamsterAnimatorEngine.onToyBindStateChanged(applicationContext, true)
        if (HamsterAnimatorEngine.isGlyphEnabled && HamsterAnimatorEngine.foregroundNotificationEnabled) {
            HamsterForegroundService.startService(applicationContext)
        }
        return serviceMessenger.binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.i(TAG, "HamsterToyService unbound by Nothing OS")
        unregisterDynamicReceivers()
        HamsterAnimatorEngine.onToyBindStateChanged(applicationContext, false)
        return false
    }

    override fun onDestroy() {
        unregisterDynamicReceivers()
        super.onDestroy()
    }

    private fun registerDynamicReceivers() {
        if (receiverRegistered) return
        try {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_POWER_CONNECTED)
                addAction(Intent.ACTION_POWER_DISCONNECTED)
                addAction(Intent.ACTION_BATTERY_CHANGED)
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_USER_PRESENT)
            }
            registerReceiver(powerAndScreenReceiver, filter)
            receiverRegistered = true
        } catch (_: Throwable) {}
    }

    private fun unregisterDynamicReceivers() {
        if (!receiverRegistered) return
        try {
            unregisterReceiver(powerAndScreenReceiver)
        } catch (_: Throwable) {}
        receiverRegistered = false
    }
}
