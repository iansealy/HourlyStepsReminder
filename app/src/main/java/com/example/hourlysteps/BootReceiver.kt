package com.example.hourlysteps

import android.content.*
import android.os.Build

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        ReminderReceiver.schedule(context)
        // On newer Android versions, health foreground services may be blocked at boot.
        // The user can reopen the app to resume sensor monitoring.
        try {
            val service = Intent(context, StepService::class.java)
            if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(service) else context.startService(service)
        } catch (_: Exception) { }
    }
}
