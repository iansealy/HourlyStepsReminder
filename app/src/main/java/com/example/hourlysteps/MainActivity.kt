package com.example.hourlysteps

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.text.format.DateFormat
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import java.text.NumberFormat
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Date

class MainActivity : Activity() {
    private lateinit var store: Store
    private lateinit var hourRange: TextView
    private lateinit var stepCount: TextView
    private lateinit var stepTarget: TextView
    private lateinit var hourMeter: ProgressBar
    private lateinit var hourStatus: TextView
    private lateinit var dayCount: TextView
    private lateinit var dayMeter: ProgressBar
    private lateinit var dayStatus: TextView
    private lateinit var trackingNotice: LinearLayout
    private lateinit var trackingMessage: TextView
    private lateinit var allowActivity: View
    private lateinit var history: List<HistoryRow>

    private val numbers = NumberFormat.getIntegerInstance()
    private val dateFormatter = DateTimeFormatter.ofPattern("EEE, d MMM")
    private val totalsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == Store.HOURS_KEY || key == null) updateTotals()
    }
    private val clockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = updateTotals()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = Store(this)
        showDashboard()
        if (savedInstanceState == null) requestPermissionsIfNeeded()
        else if (hasActivityPermission()) startMonitoring()
    }

    private fun showDashboard() {
        val content = screen(R.id.dashboard_scroll)
        content.section(row().apply {
            stretch(label(getString(R.string.dashboard_title), 28f, weight = 750).apply {
                setPadding(0, 0, dp(10), 0)
                isAccessibilityHeading = true
            })
            addView(action(getString(R.string.settings_title)) {
                startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
            })
        })

        trackingMessage = label("", 15f, Palette.muted)
        allowActivity = action(getString(R.string.allow_activity)) {
            requestPermissions(arrayOf(Manifest.permission.ACTIVITY_RECOGNITION), PERMISSIONS_REQUEST)
        }
        trackingNotice = card().apply {
            section(trackingMessage)
            section(allowActivity, 8)
        }
        content.section(trackingNotice, 16)

        val hour = currentHourCard()
        val day = todayCard()
        if (resources.configuration.screenWidthDp >= 600 && resources.configuration.fontScale <= 1.3f) {
            content.section(row().apply {
                gravity = android.view.Gravity.TOP
                stretch(hour)
                addView(View(this@MainActivity), dp(14), 1)
                stretch(day)
            }, 16)
        } else {
            content.section(hour, 16)
            content.section(day, 12)
        }

        content.section(label(getString(R.string.history_title), 23f, weight = 750).apply {
            isAccessibilityHeading = true
        }, 23)
        content.section(label(getString(R.string.history_summary), 14f, Palette.muted), 2)

        val historyCard = card(0)
        history = List(15) { index ->
            if (index > 0) historyCard.divider()
            addHistoryRow(historyCard)
        }
        content.section(historyCard, 12)
        updateTotals()
    }

    private fun currentHourCard() = column().apply {
        background = GradientDrawable(
            GradientDrawable.Orientation.TL_BR, intArrayOf(Palette.navy, Palette.navyLight)
        ).apply { cornerRadius = dp(24).toFloat() }
        setPadding(dp(20), dp(17), dp(20), dp(17))
        section(row().apply {
            stretch(label(getString(R.string.current_hour), 16f, Palette.heroText, 700).apply {
                isAccessibilityHeading = true
            })
            hourRange = label("", 14f, Palette.heroText, 500)
            addView(hourRange)
        })

        stepCount = label("", 52f, Color.WHITE, 750).apply {
            includeFontPadding = false
            fontFeatureSettings = "tnum"
        }
        stepTarget = label("", 16f, Palette.heroText, 500)
        section(stepCount, 12)
        section(stepTarget, 2)

        hourMeter = meter(Palette.lime, 0x466C8CC8)
        addMeter(hourMeter, 8, 15)
        hourStatus = label("", 14f, Palette.lime, 650)
        section(hourStatus, 8)
    }

    private fun todayCard() = card().apply {
        section(label(getString(R.string.today), 16f, weight = 700).apply {
            isAccessibilityHeading = true
        })
        dayCount = label("", 27f, weight = 750)
        dayStatus = label("", 14f, Palette.muted)
        dayMeter = meter(Palette.blue, Palette.paleBlue)
        section(dayCount, 2)
        addMeter(dayMeter, 6, 7)
        section(dayStatus, 6)
    }

    private fun addHistoryRow(card: LinearLayout): HistoryRow {
        val date = label("", 15f, weight = 600)
        val hours = label("", 14f, Palette.muted, 600).apply {
            setPadding(dp(8), 0, 0, 0)
        }
        val goal = label(getString(R.string.history_goal_met), 12f, Palette.green, 700)
        card.section(row().apply {
            minimumHeight = dp(48)
            setPadding(dp(17), dp(8), dp(17), dp(8))
            stretch(column().apply {
                section(date)
                section(goal)
            })
            addView(hours)
        })
        return HistoryRow(date, hours, goal)
    }

    private fun updateTotals() {
        val now = ZonedDateTime.now()
        val progress = store.progress(now)
        val date = now.toLocalDate()
        val hourlyTarget = store.hourlyTarget
        val dailyTarget = store.dailyTarget
        val steps = progress.currentHourSteps
        val stepsLeft = hourlyTarget - steps
        val successfulHours = progress.successfulHours[date.toString()] ?: 0

        val start = now.truncatedTo(ChronoUnit.HOURS)
        val clock = DateFormat.getTimeFormat(this)
        hourRange.text = getString(
            R.string.hour_range,
            clock.format(Date.from(start.toInstant())),
            clock.format(Date.from(start.plusHours(1).toInstant()))
        )
        stepCount.text = numbers.format(steps)
        stepTarget.text = getString(R.string.step_target, numbers.format(hourlyTarget))
        hourMeter.max = hourlyTarget
        hourMeter.progress = steps
        hourStatus.text = if (stepsLeft <= 0) getString(R.string.hourly_goal_reached)
            else resources.getQuantityString(R.plurals.steps_left, stepsLeft, numbers.format(stepsLeft))

        dayCount.text = getString(
            R.string.today_progress, numbers.format(successfulHours), numbers.format(dailyTarget)
        )
        dayMeter.max = dailyTarget
        dayMeter.progress = successfulHours
        val dailyGoalMet = successfulHours >= dailyTarget
        dayStatus.text = if (dailyGoalMet) getString(R.string.daily_goal_reached)
            else getString(R.string.today_summary, numbers.format(hourlyTarget))
        dayStatus.setTextColor(if (dailyGoalMet) Palette.green else Palette.muted)

        history.forEachIndexed { index, row ->
            val day = date.minusDays(index.toLong())
            val count = progress.successfulHours[day.toString()] ?: 0
            val formattedDate = day.format(dateFormatter)
            row.date.text = if (index == 0) getString(R.string.history_today, formattedDate) else formattedDate
            row.hours.text = resources.getQuantityString(R.plurals.history_hours, count, numbers.format(count))
            row.goal.visibility = if (count >= dailyTarget) View.VISIBLE else View.GONE
        }
    }

    private fun updateTrackingNotice() {
        val hasSensor = getSystemService(SensorManager::class.java)
            .getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null
        val needsPermission = !hasActivityPermission()
        trackingNotice.visibility = if (!hasSensor || needsPermission) View.VISIBLE else View.GONE
        trackingMessage.setText(if (hasSensor) R.string.activity_permission_needed else R.string.sensor_unavailable)
        allowActivity.visibility = if (hasSensor && needsPermission) View.VISIBLE else View.GONE
    }

    private fun hasActivityPermission() =
        checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED

    private fun requestPermissionsIfNeeded() {
        val permissions = mutableListOf<String>()
        if (!hasActivityPermission()) permissions.add(Manifest.permission.ACTIVITY_RECOGNITION)
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (permissions.isEmpty()) startMonitoring()
        else requestPermissions(permissions.toTypedArray(), PERMISSIONS_REQUEST)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST) {
            if (hasActivityPermission()) startMonitoring()
            updateTrackingNotice()
        }
    }

    private fun startMonitoring() {
        try {
            startForegroundService(Intent(this, StepService::class.java))
        } catch (_: IllegalStateException) {
            Toast.makeText(this, R.string.monitoring_failed, Toast.LENGTH_LONG).show()
        } catch (_: SecurityException) {
            Toast.makeText(this, R.string.monitoring_failed, Toast.LENGTH_LONG).show()
        }
    }

    override fun onStart() {
        super.onStart()
        store.registerListener(totalsListener)
        val clockChanges = IntentFilter(Intent.ACTION_TIME_TICK).apply {
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(clockReceiver, clockChanges, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(clockReceiver, clockChanges)
        }
    }

    override fun onResume() {
        super.onResume()
        updateTotals()
        updateTrackingNotice()
        ReminderReceiver.schedule(this)
    }

    override fun onStop() {
        unregisterReceiver(clockReceiver)
        store.unregisterListener(totalsListener)
        super.onStop()
    }

    private data class HistoryRow(val date: TextView, val hours: TextView, val goal: TextView)

    private companion object {
        const val PERMISSIONS_REQUEST = 50
    }
}
