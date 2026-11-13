package com.example.hourlysteps

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class Store(context: Context) {
    companion object {
        const val HOURS_KEY = "hours"
        private val hourFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HHXXX")
    }

    data class Progress(val currentHourSteps: Int, val successfulHours: Map<String, Int>)

    private val prefs = context.getSharedPreferences("steps", Context.MODE_PRIVATE)
    var hourlyTarget: Int
        get() = prefs.getInt("hourlyTarget", 250)
        set(v) = prefs.edit().putInt("hourlyTarget", v.coerceIn(1, 10000)).apply()
    var dailyTarget: Int
        get() = prefs.getInt("dailyTarget", 12)
        set(v) = prefs.edit().putInt("dailyTarget", v.coerceIn(1, 24)).apply()
    var reminderMinutes: Int
        get() = prefs.getInt("reminderMinutes", 15)
        set(v) = prefs.edit().putInt("reminderMinutes", v.coerceIn(1, 59)).apply()
    var startHour: Int
        get() = prefs.getInt("startHour", 8)
        set(v) = prefs.edit().putInt("startHour", v.coerceIn(0, 23)).apply()
    var endHour: Int
        get() = prefs.getInt("endHour", 22)
        set(v) = prefs.edit().putInt("endHour", v.coerceIn(0, 24)).apply()
    var lastSensorValue: Long
        get() = prefs.getLong("lastValue", -1)
        set(v) = prefs.edit().putLong("lastValue", v).apply()
    var lastSensorTime: Long
        get() = prefs.getLong("lastTime", 0)
        set(v) = prefs.edit().putLong("lastTime", v).apply()
    var lastBootMarker: Long
        get() = prefs.getLong("lastBoot", -1)
        set(v) = prefs.edit().putLong("lastBoot", v).apply()
    private fun hourKey(time: Long): String = Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault())
        .format(hourFormatter)
    private fun records(): JSONObject = try { JSONObject(prefs.getString(HOURS_KEY, "{}") ?: "{}") } catch (_: Exception) { JSONObject() }

    fun progress(now: ZonedDateTime): Progress {
        val data = records()
        val target = hourlyTarget
        val successfulHours = data.keys().asSequence()
            .filter { data.optInt(it) >= target }
            .groupingBy { it.take(10) }
            .eachCount()
        return Progress(data.optInt(now.format(hourFormatter)), successfulHours)
    }

    fun registerListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }
    @Synchronized fun record(value: Long, now: Long, bootMarker: Long) {
        val previous = lastSensorValue
        val prevTime = lastSensorTime
        val previousBoot = lastBootMarker
        lastSensorValue = value
        lastSensorTime = now
        lastBootMarker = bootMarker
        if (previous < 0 || previousBoot != bootMarker || value < previous) return
        val delta = (value - previous).coerceAtMost(100000).toInt()
        if (delta == 0) return
        // Do not attribute a long unobserved interval to a single hour.
        if (now - prevTime > 120_000L || hourKey(now) != hourKey(prevTime)) return
        val data = records()
        val key = hourKey(now)
        data.put(key, data.optInt(key, 0) + delta)
        val cutoff = java.time.LocalDate.now().minusDays(15).toString()
        val keys = data.keys().asSequence().toList()
        for (k in keys) if (k.take(10) < cutoff) data.remove(k)
        prefs.edit().putString(HOURS_KEY, data.toString()).apply()
    }
    fun inNotificationHours(hour: Int): Boolean = if (startHour == endHour) true else if (startHour < endHour) hour in startHour until endHour else hour >= startHour || hour < endHour
}
