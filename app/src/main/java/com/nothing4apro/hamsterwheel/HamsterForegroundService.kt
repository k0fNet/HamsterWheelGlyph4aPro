package com.nothing4apro.hamsterwheel

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat

/**
 * Optional Foreground Service for keeping the animation alive when the Toy is not bound by Nothing OS
 * or when screen-on persistence without AOD is requested.
 * Uses IMPORTANCE_MIN so it never shows an annoying icon in the status bar, and can be disabled
 * completely in the app settings.
 */
class HamsterForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "hamster_glyph_silent_channel"
        private const val NOTIFICATION_ID = 4041
        const val ACTION_STOP_SERVICE = "com.nothing4apro.hamsterwheel.STOP_SERVICE"

        fun startService(context: Context) {
            HamsterAnimatorEngine.loadPrefs(context)
            if (!HamsterAnimatorEngine.isGlyphEnabled) return
            if (!HamsterAnimatorEngine.foregroundNotificationEnabled) {
                // Run directly in-process (bound via HamsterToyService) without posting a ForegroundService notification!
                HamsterAnimatorEngine.start(context)
                return
            }
            val intent = Intent(context, HamsterForegroundService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (_: Throwable) {
                HamsterAnimatorEngine.start(context)
            }
        }

        fun stopService(context: Context) {
            try {
                val intent = Intent(context, HamsterForegroundService::class.java)
                context.stopService(intent)
            } catch (_: Throwable) {}
        }
    }

    private var wakeLock: PowerManager.WakeLock? = null

    private val dynamicReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (!HamsterAnimatorEngine.isGlyphEnabled) return
            when (intent.action) {
                Intent.ACTION_POWER_CONNECTED -> {
                    HamsterAnimatorEngine.onPowerStateChanged(context, true)
                    updateNotification()
                }
                Intent.ACTION_POWER_DISCONNECTED -> {
                    HamsterAnimatorEngine.onPowerStateChanged(context, false)
                    updateNotification()
                }
                Intent.ACTION_BATTERY_CHANGED -> {
                    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                    val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
                    val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                            status == BatteryManager.BATTERY_STATUS_FULL ||
                            plugged != 0
                    HamsterAnimatorEngine.onPowerStateChanged(context, isCharging)
                }
                Intent.ACTION_SCREEN_ON,
                Intent.ACTION_USER_PRESENT -> {
                    HamsterAnimatorEngine.onScreenWake()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val notification = buildNotification()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        registerReceiver(dynamicReceiver, filter)

        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "HamsterWheelGlyph::AnimLock").apply {
            setReferenceCounted(false)
            acquire(10 * 60 * 1000L)
        }

        HamsterAnimatorEngine.start(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_SERVICE) {
            HamsterAnimatorEngine.setGlyphEnabled(applicationContext, false)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        HamsterAnimatorEngine.loadPrefs(applicationContext)
        if (!HamsterAnimatorEngine.isGlyphEnabled || !HamsterAnimatorEngine.foregroundNotificationEnabled) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        HamsterAnimatorEngine.start(applicationContext)
        updateNotification()
        return START_STICKY
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(dynamicReceiver)
        } catch (_: Throwable) {}
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Throwable) {}
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Charging Hamster (Тихий фоновый режим)",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Минимальное уведомление фоновой службы (можно отключить в приложении)"
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val pendingOpen = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, HamsterForegroundService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val pendingStop = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val statusText = when (HamsterAnimatorEngine.currentState) {
            HamsterAnimatorEngine.State.RUNNING -> "Хомячок бежит в колесе (Зарядка)"
            HamsterAnimatorEngine.State.WHEEL_LOOP_STOP -> "Хомячок делает оборот на колесе..."
            HamsterAnimatorEngine.State.DANCING -> "Хомячок танцует под музыку"
            HamsterAnimatorEngine.State.SLEEPING -> "Хомячок спит в колесе"
            HamsterAnimatorEngine.State.DISABLED -> "Отключено"
            else -> "Активно"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_toy_preview)
            .setContentTitle("Charging Hamster")
            .setContentText(statusText)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(pendingOpen)
            .addAction(0, "Выключить", pendingStop)
            .build()
    }

    private fun updateNotification() {
        if (!HamsterAnimatorEngine.foregroundNotificationEnabled) return
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, buildNotification())
    }
}
