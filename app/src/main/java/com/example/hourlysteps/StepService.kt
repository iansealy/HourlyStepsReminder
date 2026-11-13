package com.example.hourlysteps

import android.app.*
import android.content.*
import android.hardware.*
import android.os.*

class StepService : Service(), SensorEventListener {
    private lateinit var manager: SensorManager
    private lateinit var store: Store
    private val channel = "monitoring"
    override fun onCreate() {
        super.onCreate()
        store = Store(this)
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(channel, "Step monitoring", NotificationManager.IMPORTANCE_LOW))
        val intent = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notification = Notification.Builder(this, channel).setSmallIcon(android.R.drawable.ic_menu_directions)
            .setContentTitle("Hourly Steps Reminder").setContentText("Monitoring phone steps throughout the day")
            .setContentIntent(intent).setOngoing(true).build()
        if (Build.VERSION.SDK_INT >= 34) startForeground(1, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH)
        else startForeground(1, notification)
        manager = getSystemService(SENSOR_SERVICE) as SensorManager
        manager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)?.let { manager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        ReminderReceiver.schedule(this)
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY
    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_STEP_COUNTER) return
        val now = System.currentTimeMillis()
        // elapsedRealtime allows detection of a new boot even if the sensor value is reset.
        val boot = (now - SystemClock.elapsedRealtime()) / 60_000L
        store.record(event.values[0].toLong(), now, boot)
    }
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    override fun onDestroy() { manager.unregisterListener(this); super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null
}
