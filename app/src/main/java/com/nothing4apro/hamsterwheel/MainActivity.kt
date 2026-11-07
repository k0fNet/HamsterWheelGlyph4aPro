package com.nothing4apro.hamsterwheel

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Companion UI for Charging Hamster with Dancing Hamster music mode on Nothing Phone (4a) Pro.
 */
class MainActivity : AppCompatActivity(), HamsterAnimatorEngine.FrameListener {

    private lateinit var previewView: GlyphMatrixPreviewView
    private lateinit var statusSubtitle: TextView
    private lateinit var powerToggleBtn: Button
    private lateinit var simulateBtn: Button
    private lateinit var simulateMusicBtn: Button
    private lateinit var musicDanceSwitch: Switch
    private lateinit var screenOnSwitch: Switch
    private lateinit var onlyChargingSwitch: Switch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestNotificationPermissionIfNeeded()
        HamsterAnimatorEngine.loadPrefs(this)
        if (HamsterAnimatorEngine.isGlyphEnabled) {
            HamsterForegroundService.startService(this)
            HamsterAnimatorEngine.start(this)
        }

        setContentView(buildUi())
    }

    override fun onResume() {
        super.onResume()
        HamsterAnimatorEngine.addListener(this)
        HamsterAnimatorEngine.checkMusicPlaybackState(this)
    }

    override fun onPause() {
        HamsterAnimatorEngine.removeListener(this)
        super.onPause()
    }

    override fun onFrameUpdated(
        frame169: IntArray,
        state: HamsterAnimatorEngine.State,
        frameDelayMs: Long,
        isCharging: Boolean,
        isMusicPlaying: Boolean,
        isEnabled: Boolean
    ) {
        runOnUiThread {
            previewView.updateFrame(frame169)

            statusSubtitle.text = when {
                !isEnabled -> "Игрушка ВЫКЛЮЧЕНА (матрица свободна для других игрушек)"
                state == HamsterAnimatorEngine.State.RUNNING -> "Хомячок бежит в колесе ⚡ (${frameDelayMs} мс)"
                state == HamsterAnimatorEngine.State.WHEEL_LOOP_STOP -> "Зарядка вынута: оборот 360° по часовой стрелке 🎡 (${frameDelayMs} мс)"
                state == HamsterAnimatorEngine.State.DANCING -> "Играет музыка: хомячок танцует под бит! 🎵 (${frameDelayMs} мс)"
                else -> "Хомячок спит в колесе 💤 (Частички сна улетают вверх)"
            }

            if (isEnabled) {
                powerToggleBtn.text = "⏹ ВЫКЛЮЧИТЬ ИГРУШКУ (Освободить матрицу)"
                powerToggleBtn.background = roundedBg("#2A1215", "#FF453A")
                powerToggleBtn.setTextColor(Color.parseColor("#FF6961"))
            } else {
                powerToggleBtn.text = "▶ ВКЛЮЧИТЬ ИГРУШКУ CHARGING HAMSTER"
                powerToggleBtn.background = roundedBg("#142E1F", "#30D158")
                powerToggleBtn.setTextColor(Color.parseColor("#30D158"))
            }

            simulateBtn.text = if (isCharging) {
                "Вынуть зарядку (Оборот 360° по часовой стрелке)"
            } else {
                "Подключить зарядку (Запуск бега)"
            }

            simulateMusicBtn.text = if (isMusicPlaying) {
                "Остановить симуляцию музыки"
            } else {
                "Симулировать музыку 🎵 (Танец хомячка)"
            }
        }
    }

    private fun buildUi(): ScrollView {
        val scroll = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#000000"))
            isFillViewport = true
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            val pad = dp(20)
            setPadding(pad, dp(28), pad, pad)
        }

        previewView = GlyphMatrixPreviewView(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(240), dp(240)).apply {
                bottomMargin = dp(20)
            }
        }
        root.addView(previewView)

        // Card 1: Master Info & Power Switch
        val mainCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedBg("#161618", "#26262A")
            val p = dp(18)
            setPadding(p, p, p, p)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(14)
            }
        }

        val title = TextView(this).apply {
            text = "CHARGING HAMSTER"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        mainCard.addView(title)

        statusSubtitle = TextView(this).apply {
            text = "A little hamster to power your phone (13×13)"
            setTextColor(Color.parseColor("#9A9AA2"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setPadding(0, dp(6), 0, dp(14))
        }
        mainCard.addView(statusSubtitle)

        powerToggleBtn = Button(this).apply {
            isAllCaps = false
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(10)
            }
            setOnClickListener {
                val next = !HamsterAnimatorEngine.isGlyphEnabled
                HamsterAnimatorEngine.setGlyphEnabled(this@MainActivity, next)
            }
        }
        mainCard.addView(powerToggleBtn)

        val settingsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }
        val editGlyphBtn = Button(this).apply {
            text = "Edit in Glyph settings"
            isAllCaps = false
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            background = roundedBg("#2C2C30", "#3A3A40", cornerDp = 20)
            setPadding(dp(18), dp(10), dp(18), dp(10))
            setOnClickListener { openNothingGlyphToySettings() }
        }
        settingsRow.addView(editGlyphBtn)
        mainCard.addView(settingsRow)
        root.addView(mainCard)

        // Card 2: Test buttons
        val testCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedBg("#161618", "#26262A")
            val p = dp(16)
            setPadding(p, p, p, p)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(14)
            }
        }

        simulateBtn = Button(this).apply {
            text = "Подключить зарядку (Запуск бега)"
            isAllCaps = false
            setTextColor(Color.WHITE)
            typeface = Typeface.MONOSPACE
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            background = roundedBg("#D71921", "#FF453A")
            setPadding(dp(14), dp(12), dp(14), dp(12))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(8)
            }
            setOnClickListener {
                if (!HamsterAnimatorEngine.isGlyphEnabled) {
                    HamsterAnimatorEngine.setGlyphEnabled(this@MainActivity, true)
                }
                val active = HamsterAnimatorEngine.manualChargingOverride
                    ?: HamsterAnimatorEngine.isChargingNow
                HamsterAnimatorEngine.setManualOverride(!active)
            }
        }
        testCard.addView(simulateBtn)

        simulateMusicBtn = Button(this).apply {
            text = "Симулировать музыку 🎵 (Танец хомячка)"
            isAllCaps = false
            setTextColor(Color.parseColor("#FFD60A"))
            typeface = Typeface.MONOSPACE
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            background = roundedBg("#2B2816", "#8A7400")
            setPadding(dp(12), dp(10), dp(12), dp(10))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(8)
            }
            setOnClickListener {
                val active = HamsterAnimatorEngine.manualMusicOverride
                    ?: HamsterAnimatorEngine.isMusicPlayingNow
                HamsterAnimatorEngine.setManualMusicOverride(!active)
            }
        }
        testCard.addView(simulateMusicBtn)

        val autoCableBtn = Button(this).apply {
            text = "Сбросить тесты (Реакция на реальный кабель и плеер)"
            isAllCaps = false
            setTextColor(Color.parseColor("#C8C8D0"))
            typeface = Typeface.MONOSPACE
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            background = roundedBg("#222226", "#323238")
            setPadding(dp(12), dp(10), dp(12), dp(10))
            setOnClickListener {
                HamsterAnimatorEngine.setManualOverride(null)
                HamsterAnimatorEngine.setManualMusicOverride(null)
                HamsterAnimatorEngine.syncBatteryChargingState(this@MainActivity)
                HamsterAnimatorEngine.checkMusicPlaybackState(this@MainActivity)
                Toast.makeText(this@MainActivity, "Автоматический режим включен", Toast.LENGTH_SHORT).show()
            }
        }
        testCard.addView(autoCableBtn)
        root.addView(testCard)

        // Card 3: Switches
        val optionsCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedBg("#161618", "#26262A")
            val p = dp(18)
            setPadding(p, p, p, p)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        musicDanceSwitch = Switch(this).apply {
            text = "ТАНЕЦ ПОД МУЗЫКУ (AUDIO ACTIVE)"
            setTextColor(Color.WHITE)
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            isChecked = HamsterAnimatorEngine.musicDanceEnabled
            setOnCheckedChangeListener { _, isChecked ->
                HamsterAnimatorEngine.setMusicDanceEnabled(this@MainActivity, isChecked)
            }
        }
        optionsCard.addView(musicDanceSwitch)

        val musicDesc = TextView(this).apply {
            text = "Когда в любом приложении играет музыка (Spotify, YouTube Music, VK и др.), хомячок танцует с парящими нотами ♪. При подключении зарядки бег в колесе имеет высший приоритет."
            setTextColor(Color.parseColor("#8E8E96"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setPadding(0, dp(4), 0, dp(16))
        }
        optionsCard.addView(musicDesc)

        onlyChargingSwitch = Switch(this).apply {
            text = "ONLY SHOW WHILE CHARGING"
            setTextColor(Color.WHITE)
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            isChecked = HamsterAnimatorEngine.onlyShowWhileCharging
            setOnCheckedChangeListener { _, isChecked ->
                HamsterAnimatorEngine.setOnlyShowWhileCharging(this@MainActivity, isChecked)
            }
        }
        optionsCard.addView(onlyChargingSwitch)

        val onlyChargingDesc = TextView(this).apply {
            text = "Когда телефон не заряжается, матрица гаснет вместо спящего хомячка. При отключении кабеля анимация оборота на колесе всё равно проигрывается один раз."
            setTextColor(Color.parseColor("#8E8E96"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setPadding(0, dp(4), 0, dp(16))
        }
        optionsCard.addView(onlyChargingDesc)

        screenOnSwitch = Switch(this).apply {
            text = "РАБОТАТЬ ПРИ ВКЛЮЧЕННОМ ЭКРАНЕ"
            setTextColor(Color.WHITE)
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            isChecked = HamsterAnimatorEngine.keepScreenOnActive
            setOnCheckedChangeListener { _, isChecked ->
                HamsterAnimatorEngine.setKeepScreenOnActive(this@MainActivity, isChecked)
            }
        }
        optionsCard.addView(screenOnSwitch)

        val screenOnDesc = TextView(this).apply {
            text = "Удерживает анимацию активной при разблокировке экрана. Отключите тумблер, если хотите работу строго как AOD-игрушка."
            setTextColor(Color.parseColor("#8E8E96"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setPadding(0, dp(4), 0, 0)
        }
        optionsCard.addView(screenOnDesc)

        root.addView(optionsCard)
        scroll.addView(root)
        return scroll
    }

    private fun roundedBg(fillHex: String, strokeHex: String, cornerDp: Int = 18): GradientDrawable {
        return GradientDrawable().apply {
            setColor(Color.parseColor(fillHex))
            cornerRadius = dp(cornerDp).toFloat()
            setStroke(dp(1), Color.parseColor(strokeHex))
        }
    }

    private fun openNothingGlyphToySettings() {
        try {
            val aodIntent = Intent().apply {
                component = ComponentName(
                    "com.nothing.thirdparty",
                    "com.nothing.thirdparty.matrix.toys.manager.AodToySelectActivity"
                )
            }
            startActivity(aodIntent)
            return
        } catch (_: Throwable) {}

        try {
            val managerIntent = Intent().apply {
                component = ComponentName(
                    "com.nothing.thirdparty",
                    "com.nothing.thirdparty.matrix.toys.manager.ToysManagerActivity"
                )
            }
            startActivity(managerIntent)
            return
        } catch (_: Throwable) {}

        try {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        } catch (_: Throwable) {}
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }
    }

    private fun dp(value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            resources.displayMetrics
        ).toInt()
    }
}
