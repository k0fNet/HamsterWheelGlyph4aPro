package com.nothing4apro.hamsterwheel

import android.Manifest
import android.app.AlertDialog
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Nothing OS styled companion UI modeled directly on the reference "CHARGING HAMSTER" screenshots:
 * - Authentic 5×7 dot-matrix headings (DotMatrixTextView)
 * - Clean dark #161618 rounded cards (24dp radius)
 * - Pill-shaped buttons ("Edit in Glyph settings", "Add Widget", "Excluded Apps")
 * - Zero annoying notifications by default (toggleable in settings)
 * - Built-in App Exclusion Manager to block any specific app from triggering the music dance
 */
class MainActivity : AppCompatActivity(), HamsterAnimatorEngine.FrameListener {

    private lateinit var previewView: GlyphMatrixPreviewView
    private lateinit var statusSubText: TextView
    private lateinit var powerChipBtn: Button
    private lateinit var simulateChargeBtn: Button
    private lateinit var simulateMusicBtn: Button
    private lateinit var grantNotifListenerBtn: Button
    private lateinit var excludedCountText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK

        HamsterAnimatorEngine.loadPrefs(this)
        if (HamsterAnimatorEngine.isGlyphEnabled) {
            if (HamsterAnimatorEngine.foregroundNotificationEnabled) {
                requestNotificationPermissionIfNeeded()
                HamsterForegroundService.startService(this)
            }
            HamsterAnimatorEngine.start(this)
        }

        setContentView(buildNothingOsUi())
    }

    override fun onResume() {
        super.onResume()
        HamsterAnimatorEngine.addListener(this)
        HamsterAnimatorEngine.pollMusicStateSilently(this)
        updateYoutubePermissionVisibility()
        updateExcludedAppsSummary()
    }

    override fun onPause() {
        HamsterAnimatorEngine.removeListener(this)
        super.onPause()
    }

    private fun updateYoutubePermissionVisibility() {
        if (!::grantNotifListenerBtn.isInitialized) return
        val granted = MediaSessionMonitorService.isNotificationListenerGranted(this)
        grantNotifListenerBtn.visibility = if (!granted) View.VISIBLE else View.GONE
    }

    private fun updateExcludedAppsSummary() {
        if (!::excludedCountText.isInitialized) return
        val custom = MediaSessionMonitorService.getCustomIgnoredPackages(this)
        val totalDefault = MediaSessionMonitorService.DEFAULT_IGNORED_PACKAGES.size
        excludedCountText.text = if (custom.isEmpty()) {
            "В списке исключений: YouTube, TikTok, Twitch и еще $totalDefault видеоплееров. Нажмите «Настроить исключения», чтобы добавить любое свое приложение."
        } else {
            "Исключено по умолчанию ($totalDefault) + ваши приложения (${custom.size}): ${custom.joinToString(", ")}"
        }
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

            statusSubText.text = when {
                !isEnabled -> "A little hamster to power your phone · OFF"
                state == HamsterAnimatorEngine.State.RUNNING -> "A little hamster to power your phone · Running ⚡"
                state == HamsterAnimatorEngine.State.WHEEL_LOOP_STOP -> "A little hamster to power your phone · Spinning stop 🎡"
                state == HamsterAnimatorEngine.State.DANCING -> "A little hamster to power your phone · Dancing to music 🎵"
                else -> "A little hamster to power your phone"
            }

            if (isEnabled) {
                powerChipBtn.text = "Active · Tap to turn OFF"
                powerChipBtn.background = pillBg("#26262B", "#3A3A42")
                powerChipBtn.setTextColor(Color.parseColor("#E5E5EA"))
            } else {
                powerChipBtn.text = "Disabled · Tap to turn ON"
                powerChipBtn.background = pillBg("#D71921", "#FF453A")
                powerChipBtn.setTextColor(Color.WHITE)
            }

            simulateChargeBtn.text = if (isCharging) "Unplug test (360° spin)" else "Test charging run"
            simulateMusicBtn.text = if (isMusicPlaying) "Stop music test" else "Test music dance"
        }
    }

    private fun buildNothingOsUi(): ScrollView {
        val scroll = ScrollView(this).apply {
            setBackgroundColor(Color.BLACK)
            isFillViewport = true
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(16), dp(20), dp(16), dp(32))
        }

        // 1. Top Nothing OS "WIDGETS   TOYS" header bar
        val topTabsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                leftMargin = dp(8)
                bottomMargin = dp(18)
            }
        }

        val widgetsTab = DotMatrixTextView(this).apply {
            text = "WIDGETS"
            dotColor = Color.parseColor("#63636A")
            dotDiameterPx = dpF(2.2f)
            dotGapPx = dpF(0.9f)
            setPadding(0, dp(4), dp(22), dp(6))
            setOnClickListener { requestPinHamsterWidget() }
        }
        topTabsRow.addView(widgetsTab)

        val toysTabCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }
        val toysTab = DotMatrixTextView(this).apply {
            text = "TOYS"
            dotColor = Color.WHITE
            dotDiameterPx = dpF(2.2f)
            dotGapPx = dpF(0.9f)
            setPadding(0, dp(4), 0, dp(6))
        }
        toysTabCol.addView(toysTab)
        val underline = View(this).apply {
            setBackgroundColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(dp(44), dp(2))
        }
        toysTabCol.addView(underline)
        topTabsRow.addView(toysTabCol)
        root.addView(topTabsRow)

        // 2. Center 13x13 Glyph Matrix Preview Circle
        previewView = GlyphMatrixPreviewView(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(236), dp(236)).apply {
                topMargin = dp(6)
                bottomMargin = dp(24)
            }
        }
        root.addView(previewView)

        // 3. Card 1: CHARGING HAMSTER Title Card (matches screenshot 1:1)
        val heroCard = createNothingCard()
        val dotTitle = DotMatrixTextView(this).apply {
            text = "CHARGING HAMSTER"
            dotColor = Color.WHITE
            dotDiameterPx = dpF(2.5f)
            dotGapPx = dpF(1.0f)
            setPadding(0, 0, 0, dp(10))
        }
        heroCard.addView(dotTitle)

        statusSubText = TextView(this).apply {
            text = "A little hamster to power your phone"
            setTextColor(Color.parseColor("#8E8E93"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            typeface = Typeface.SANS_SERIF
            setPadding(0, 0, 0, dp(20))
        }
        heroCard.addView(statusSubText)

        val heroBtnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        powerChipBtn = Button(this).apply {
            text = "Active · Tap to turn OFF"
            isAllCaps = false
            setTextColor(Color.parseColor("#E5E5EA"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            background = pillBg("#26262B", "#3A3A42")
            setPadding(dp(16), dp(8), dp(16), dp(8))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dp(40)
            ).apply {
                rightMargin = dp(10)
            }
            setOnClickListener {
                val next = !HamsterAnimatorEngine.isGlyphEnabled
                HamsterAnimatorEngine.setGlyphEnabled(this@MainActivity, next)
            }
        }
        heroBtnRow.addView(powerChipBtn)

        val editGlyphBtn = Button(this).apply {
            text = "Edit in Glyph settings"
            isAllCaps = false
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            background = pillBg("#2C2C30", "#3A3A40")
            setPadding(dp(18), dp(8), dp(18), dp(8))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dp(40)
            )
            setOnClickListener { openNothingGlyphToySettings() }
        }
        heroBtnRow.addView(editGlyphBtn)
        heroCard.addView(heroBtnRow)
        root.addView(heroCard)

        // 4. Card 2: ONLY SHOW WHILE CHARGING (matches screenshot 1:1)
        val onlyChargingCard = createNothingCard()
        onlyChargingCard.addView(
            createDotMatrixSwitchRow(
                dotLabel = "ONLY SHOW WHILE CHARGING",
                initialChecked = HamsterAnimatorEngine.onlyShowWhileCharging
            ) { isChecked ->
                HamsterAnimatorEngine.setOnlyShowWhileCharging(this, isChecked)
            }
        )
        val onlyChargingDesc = TextView(this).apply {
            text = "When your phone isn't charging, the Glyph Matrix stays blank instead of the sleeping hamster. The wheel spin animation still plays once when you unplug."
            setTextColor(Color.parseColor("#8E8E93"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setLineSpacing(0f, 1.15f)
            setPadding(0, dp(10), 0, 0)
        }
        onlyChargingCard.addView(onlyChargingDesc)
        root.addView(onlyChargingCard)

        // 5. Card 3: DANCE TO MUSIC & APP EXCLUSIONS
        val musicCard = createNothingCard()
        musicCard.addView(
            createDotMatrixSwitchRow(
                dotLabel = "DANCE TO MUSIC",
                initialChecked = HamsterAnimatorEngine.musicDanceEnabled
            ) { isChecked ->
                HamsterAnimatorEngine.setMusicDanceEnabled(this, isChecked)
            }
        )
        val musicDesc = TextView(this).apply {
            text = "When music plays on your phone, the hamster dances in place while the wheel stays still. Charging always takes priority."
            setTextColor(Color.parseColor("#8E8E93"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setLineSpacing(0f, 1.15f)
            setPadding(0, dp(10), 0, dp(16))
        }
        musicCard.addView(musicDesc)

        musicCard.addView(
            createDotMatrixSwitchRow(
                dotLabel = "IGNORE YOUTUBE",
                initialChecked = HamsterAnimatorEngine.ignoreYoutubeEnabled
            ) { isChecked ->
                HamsterAnimatorEngine.setIgnoreYoutubeEnabled(this, isChecked)
            }
        )

        excludedCountText = TextView(this).apply {
            setTextColor(Color.parseColor("#8E8E93"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
            setLineSpacing(0f, 1.15f)
            setPadding(0, dp(8), 0, dp(14))
        }
        musicCard.addView(excludedCountText)
        updateExcludedAppsSummary()

        val exclusionBtnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        grantNotifListenerBtn = Button(this).apply {
            text = "Grant media access"
            isAllCaps = false
            setTextColor(Color.parseColor("#FFD60A"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            background = pillBg("#2B2612", "#665718")
            setPadding(dp(14), dp(6), dp(14), dp(6))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dp(38)
            ).apply {
                rightMargin = dp(8)
            }
            setOnClickListener {
                try {
                    startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                } catch (_: Throwable) {
                    startActivity(Intent(Settings.ACTION_SETTINGS))
                }
            }
        }
        exclusionBtnRow.addView(grantNotifListenerBtn)
        updateYoutubePermissionVisibility()

        val manageExclusionsBtn = Button(this).apply {
            text = "Excluded apps (+)"
            isAllCaps = false
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
            background = pillBg("#2C2C30", "#3A3A40")
            setPadding(dp(16), dp(6), dp(16), dp(6))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dp(38)
            )
            setOnClickListener { showExcludedAppsDialog() }
        }
        exclusionBtnRow.addView(manageExclusionsBtn)
        musicCard.addView(exclusionBtnRow)
        root.addView(musicCard)

        // 6. Card 4: SCREEN-ON & SILENT MODE (No annoying notifications!)
        val systemCard = createNothingCard()
        systemCard.addView(
            createDotMatrixSwitchRow(
                dotLabel = "KEEP ON WITH SCREEN",
                initialChecked = HamsterAnimatorEngine.keepScreenOnActive
            ) { isChecked ->
                HamsterAnimatorEngine.setKeepScreenOnActive(this, isChecked)
            }
        )
        val screenDesc = TextView(this).apply {
            text = "Keeps the Glyph Matrix animation active even when your screen is unlocked. Turn off if you want standard AOD-only behavior."
            setTextColor(Color.parseColor("#8E8E93"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setLineSpacing(0f, 1.15f)
            setPadding(0, dp(10), 0, dp(16))
        }
        systemCard.addView(screenDesc)

        systemCard.addView(
            createDotMatrixSwitchRow(
                dotLabel = "STATUS NOTIFICATION",
                initialChecked = HamsterAnimatorEngine.foregroundNotificationEnabled
            ) { isChecked ->
                if (isChecked) requestNotificationPermissionIfNeeded()
                HamsterAnimatorEngine.setForegroundNotificationEnabled(this, isChecked)
            }
        )
        val notifDesc = TextView(this).apply {
            text = "Off by default so no notification sits in your shade. While selected as your active Glyph Toy in Nothing OS, animations work without any notification."
            setTextColor(Color.parseColor("#8E8E93"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
            setLineSpacing(0f, 1.15f)
            setPadding(0, dp(10), 0, 0)
        }
        systemCard.addView(notifDesc)
        root.addView(systemCard)

        // 7. Card 5: QUICK PREVIEW & WIDGET TOOLS
        val toolsCard = createNothingCard()
        val toolsHeader = DotMatrixTextView(this).apply {
            text = "PREVIEW AND WIDGET"
            dotColor = Color.WHITE
            dotDiameterPx = dpF(2.1f)
            dotGapPx = dpF(0.85f)
            setPadding(0, 0, 0, dp(14))
        }
        toolsCard.addView(toolsHeader)

        val simRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(10)
            }
        }

        simulateChargeBtn = Button(this).apply {
            text = "Test charging run"
            isAllCaps = false
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            background = pillBg("#2C2C30", "#3A3A40")
            layoutParams = LinearLayout.LayoutParams(0, dp(40), 1f).apply {
                rightMargin = dp(6)
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
        simRow.addView(simulateChargeBtn)

        simulateMusicBtn = Button(this).apply {
            text = "Test music dance"
            isAllCaps = false
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            background = pillBg("#2C2C30", "#3A3A40")
            layoutParams = LinearLayout.LayoutParams(0, dp(40), 1f).apply {
                leftMargin = dp(6)
            }
            setOnClickListener {
                val active = HamsterAnimatorEngine.manualMusicOverride
                    ?: HamsterAnimatorEngine.isMusicPlayingNow
                HamsterAnimatorEngine.setManualMusicOverride(!active)
            }
        }
        simRow.addView(simulateMusicBtn)
        toolsCard.addView(simRow)

        val bottomToolsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val resetAutoBtn = Button(this).apply {
            text = "Reset to live sensors"
            isAllCaps = false
            setTextColor(Color.parseColor("#9A9AA2"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            background = pillBg("#202024", "#2E2E34")
            layoutParams = LinearLayout.LayoutParams(0, dp(40), 1f).apply {
                rightMargin = dp(6)
            }
            setOnClickListener {
                HamsterAnimatorEngine.setManualOverride(null)
                HamsterAnimatorEngine.setManualMusicOverride(null)
                HamsterAnimatorEngine.syncBatteryChargingState(this@MainActivity)
                HamsterAnimatorEngine.pollMusicStateSilently(this@MainActivity)
                Toast.makeText(this@MainActivity, "Synced with live USB & media state", Toast.LENGTH_SHORT).show()
            }
        }
        bottomToolsRow.addView(resetAutoBtn)

        val pinWidgetBtn = Button(this).apply {
            text = "Add Home Widget"
            isAllCaps = false
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            background = pillBg("#2C2C30", "#3A3A40")
            layoutParams = LinearLayout.LayoutParams(0, dp(40), 1f).apply {
                leftMargin = dp(6)
            }
            setOnClickListener { requestPinHamsterWidget() }
        }
        bottomToolsRow.addView(pinWidgetBtn)
        toolsCard.addView(bottomToolsRow)
        root.addView(toolsCard)

        scroll.addView(root)
        return scroll
    }

    private fun createNothingCard(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#161618"))
                cornerRadius = dp(24).toFloat()
                setStroke(dp(1), Color.parseColor("#232327"))
            }
            val p = dp(20)
            setPadding(p, p, p, p)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(14)
            }
        }
    }

    private fun createDotMatrixSwitchRow(
        dotLabel: String,
        initialChecked: Boolean,
        onToggle: (Boolean) -> Unit
    ): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val labelView = DotMatrixTextView(this).apply {
            text = dotLabel
            dotColor = Color.WHITE
            dotDiameterPx = dpF(2.1f)
            dotGapPx = dpF(0.85f)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        row.addView(labelView)

        val sw = Switch(this).apply {
            isChecked = initialChecked
            setOnCheckedChangeListener { _, isChecked -> onToggle(isChecked) }
        }
        row.addView(sw)
        return row
    }

    /**
     * Shows a dialog where the user can pick any installed app on their phone (or type a package name)
     * to exclude it from triggering the dancing hamster animation.
     */
    private fun showExcludedAppsDialog() {
        val pm = packageManager
        val installedApps = try {
            pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter { app ->
                    // Show user-installed apps and media/launchable system apps
                    (app.flags and ApplicationInfo.FLAG_SYSTEM) == 0 ||
                            pm.getLaunchIntentForPackage(app.packageName) != null
                }
                .map { app ->
                    val label = pm.getApplicationLabel(app).toString()
                    Pair(label, app.packageName.lowercase())
                }
                .distinctBy { it.second }
                .sortedBy { it.first.lowercase() }
        } catch (_: Throwable) {
            emptyList()
        }

        val currentExcluded = MediaSessionMonitorService.getCustomIgnoredPackages(this).toMutableSet()

        if (installedApps.isNotEmpty()) {
            val labels = installedApps.map { "${it.first} (${it.second})" }.toTypedArray()
            val checked = BooleanArray(installedApps.size) { idx ->
                currentExcluded.contains(installedApps[idx].second)
            }

            AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("Исключения для танца под музыку")
                .setMultiChoiceItems(labels, checked) { _, which, isChecked ->
                    val pkg = installedApps[which].second
                    if (isChecked) {
                        currentExcluded.add(pkg)
                    } else {
                        currentExcluded.remove(pkg)
                    }
                }
                .setPositiveButton("Сохранить") { _, _ ->
                    MediaSessionMonitorService.setCustomIgnoredPackages(this, currentExcluded)
                    HamsterAnimatorEngine.pollMusicStateSilently(this)
                    updateExcludedAppsSummary()
                    Toast.makeText(this, "Список исключений сохранен", Toast.LENGTH_SHORT).show()
                }
                .setNeutralButton("Ввести пакет вручную") { _, _ ->
                    showManualPackageInputDialog()
                }
                .setNegativeButton("Отмена", null)
                .show()
        } else {
            showManualPackageInputDialog()
        }
    }

    private fun showManualPackageInputDialog() {
        val input = EditText(this).apply {
            hint = "Например: org.telegram.messenger"
            inputType = InputType.TYPE_CLASS_TEXT
            setText(MediaSessionMonitorService.getCustomIgnoredPackages(this@MainActivity).joinToString(", "))
        }
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Пакеты-исключения (через запятую)")
            .setView(input)
            .setPositiveButton("Сохранить") { _, _ ->
                val raw = input.text.toString()
                val pkgs = raw.split(",", ";", " ", "\n")
                    .map { it.trim().lowercase() }
                    .filter { it.isNotEmpty() }
                    .toSet()
                MediaSessionMonitorService.setCustomIgnoredPackages(this, pkgs)
                HamsterAnimatorEngine.pollMusicStateSilently(this)
                updateExcludedAppsSummary()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun requestPinHamsterWidget() {
        val mgr = AppWidgetManager.getInstance(this)
        val provider = ComponentName(this, HamsterWidgetProvider::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && mgr.isRequestPinAppWidgetSupported) {
            mgr.requestPinAppWidget(provider, null, null)
        } else {
            Toast.makeText(
                this,
                "Long-press on home screen → Widgets → Charging Hamster 13×13",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun pillBg(fillHex: String, strokeHex: String): GradientDrawable {
        return GradientDrawable().apply {
            setColor(Color.parseColor(fillHex))
            cornerRadius = dp(22).toFloat()
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

    private fun dpF(value: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value,
            resources.displayMetrics
        )
    }
}
