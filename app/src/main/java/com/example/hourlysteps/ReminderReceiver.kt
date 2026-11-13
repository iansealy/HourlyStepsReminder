package com.example.hourlysteps

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import java.time.ZonedDateTime

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val store = Store(context)
        val now = ZonedDateTime.now()
        if (store.inNotificationHours(now.hour) && store.count(store.currentKey()) < store.hourlyTarget) {
            val nm = context.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(NotificationChannel("reminders", "Hourly reminders", NotificationManager.IMPORTANCE_HIGH))
            if (Build.VERSION.SDK_INT < 33 || context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                val open = PendingIntent.getActivity(context, 1, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
                nm.notify(2, Notification.Builder(context, "reminders").setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle("Time for some steps")
                    .setContentText("${store.count(store.currentKey())} of ${store.hourlyTarget} steps this hour. ${store.reminderMinutes} minutes left.")
                    .setContentIntent(open).setAutoCancel(true).build())
            }
        }
        schedule(context)
    }
    companion object {
        fun schedule(context: Context) {
            val store = Store(context)
            val now = ZonedDateTime.now()
            var next = now.withMinute(60 - store.reminderMinutes).withSecond(0).withNano(0)
            if (!next.isAfter(now)) next = next.plusHours(1)
            val pending = PendingIntent.getBroadcast(context, 10, Intent(context, ReminderReceiver::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            val alarm = context.getSystemService(AlarmManager::class.java)
            val at = next.toInstant().toEpochMilli()
            if (Build.VERSION.SDK_INT >= 31 && !alarm.canScheduleExactAlarms()) alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
            else alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
        }
    }
}
