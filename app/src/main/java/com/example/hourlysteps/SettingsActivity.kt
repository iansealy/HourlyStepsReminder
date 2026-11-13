package com.example.hourlysteps

import android.app.Activity
import android.app.AlarmManager
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.text.NumberFormat

class SettingsActivity : Activity() {
    private lateinit var store: Store
    private lateinit var preciseStatus: TextView
    private val rows = mutableMapOf<Setting, Pair<View, TextView>>()
    private var openSetting: Setting? = null
    private var editor: EditText? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = Store(this)
        showSettings()
        savedInstanceState?.getString("openSetting")?.let { name ->
            val setting = Setting.valueOf(name)
            edit(setting, savedInstanceState.getString("draft"))
        }
    }

    private fun showSettings() {
        val content = screen(R.id.settings_scroll)
        content.section(row().apply {
            addView(action(getString(R.string.back), ::finish))
            stretch(label(getString(R.string.settings_title), 28f, weight = 750).apply {
                setPaddingRelative(dp(14), 0, 0, 0)
                isAccessibilityHeading = true
            })
        })

        addHeading(content, R.string.goals_section)
        content.section(card(0).apply {
            addSetting(Setting.HOURLY_GOAL)
            divider()
            addSetting(Setting.DAILY_GOAL)
        }, 9)

        addHeading(content, R.string.reminders_section)
        content.section(label(getString(R.string.reminders_summary), 14f, Palette.muted), 4)
        content.section(card(0).apply {
            addSetting(Setting.LEAD_TIME)
            divider()
            addSetting(Setting.START_HOUR)
            divider()
            addSetting(Setting.END_HOUR)
            section(label(getString(R.string.reminder_window_hint), 13f, Palette.muted).apply {
                setPadding(dp(17), dp(2), dp(17), dp(14))
            })
        }, 11)

        if (Build.VERSION.SDK_INT >= 31) {
            content.section(card().apply {
                section(label(getString(R.string.precise_reminders), 16f, weight = 700))
                preciseStatus = label("", 14f, Palette.muted)
                section(preciseStatus, 4)
            }.apply {
                foreground = ripple(Color.TRANSPARENT, 22)
                isFocusable = true
                setOnClickListener { openPreciseReminderSettings() }
            }, 12)
        }
        content.section(label(getString(R.string.tracking_note), 13f, Palette.muted), 17)
    }

    private fun addHeading(content: LinearLayout, title: Int) {
        content.section(label(getString(title), 22f, weight = 750).apply {
            isAccessibilityHeading = true
        }, 23)
    }

    private fun LinearLayout.addSetting(setting: Setting) {
        val name = getString(setting.title)
        val value = label("", 16f, Palette.blue, 700).apply {
            setPaddingRelative(dp(12), 0, 0, 0)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        section(row().apply {
            minimumHeight = dp(58)
            setPadding(dp(17), dp(9), dp(17), dp(9))
            foreground = ripple(Color.TRANSPARENT, 16)
            stretch(label(name, 15f, weight = 600).apply {
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            })
            addView(value)
            isFocusable = true
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
            setOnClickListener { edit(setting) }
            rows[setting] = this to value
            updateSettingRow(setting)
        })
    }

    private fun updateSettingRow(setting: Setting) {
        val (row, value) = rows.getValue(setting)
        value.text = formattedValue(setting)
        row.contentDescription = getString(R.string.edit_setting_description, getString(setting.title), value.text)
    }

    private fun formattedValue(setting: Setting): String =
        if (setting.isClockHour) getString(R.string.whole_hour, setting.read(store))
        else NumberFormat.getIntegerInstance().format(setting.read(store))

    private fun edit(setting: Setting, draft: String? = null) {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setSingleLine()
            hint = getString(setting.title)
            setText(draft ?: setting.read(store).toString())
            if (draft == null) selectAll() else setSelection(length())
        }
        val container = column().apply {
            setPadding(dp(24), dp(6), dp(24), dp(4))
            section(input)
        }
        val hint = if (setting.isClockHour) R.string.hour_number_hint else R.string.allowed_range
        val dialog = AlertDialog.Builder(this)
            .setTitle(setting.title)
            .setMessage(getString(hint, setting.min, setting.max))
            .setView(container)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.save, null)
            .create()

        openSetting = setting
        editor = input
        dialog.setOnDismissListener {
            openSetting = null
            editor = null
        }
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val number = input.text.toString().trim().toIntOrNull()
                if (number == null || number !in setting.min..setting.max) {
                    input.error = getString(hint, setting.min, setting.max)
                } else {
                    setting.write(store, number)
                    ReminderReceiver.schedule(this)
                    dialog.dismiss()
                    updateSettingRow(setting)
                }
            }
        }
        input.requestFocus()
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        dialog.show()
    }

    private fun openPreciseReminderSettings() {
        if (Build.VERSION.SDK_INT < 31) return
        try {
            startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$packageName")))
        } catch (_: android.content.ActivityNotFoundException) {
            Toast.makeText(this, R.string.precise_unavailable, Toast.LENGTH_LONG).show()
        }
    }

    private fun updatePreciseReminderStatus() {
        if (Build.VERSION.SDK_INT >= 31) {
            val allowed = getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
            preciseStatus.setText(if (allowed) R.string.precise_allowed else R.string.precise_delayed)
            preciseStatus.setTextColor(if (allowed) Palette.green else Palette.blue)
        }
    }

    override fun onResume() {
        super.onResume()
        updatePreciseReminderStatus()
        ReminderReceiver.schedule(this)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        openSetting?.let { outState.putString("openSetting", it.name) }
        editor?.let { outState.putString("draft", it.text.toString()) }
    }

    private enum class Setting(
        val title: Int,
        val min: Int,
        val max: Int,
        val read: (Store) -> Int,
        val write: (Store, Int) -> Unit,
        val isClockHour: Boolean = false
    ) {
        HOURLY_GOAL(R.string.hourly_goal_setting, 1, 10000, { it.hourlyTarget }, { s, v -> s.hourlyTarget = v }),
        DAILY_GOAL(R.string.daily_goal_setting, 1, 24, { it.dailyTarget }, { s, v -> s.dailyTarget = v }),
        LEAD_TIME(R.string.reminder_lead_setting, 1, 59, { it.reminderMinutes }, { s, v -> s.reminderMinutes = v }),
        START_HOUR(R.string.reminder_start_setting, 0, 23, { it.startHour }, { s, v -> s.startHour = v }, true),
        END_HOUR(R.string.reminder_end_setting, 0, 24, { it.endHour }, { s, v -> s.endHour = v }, true)
    }
}
