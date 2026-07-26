package com.tamreeni.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat

class WorkoutForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "tamreeni_workout_channel"
        const val NOTIF_ID = 5501
        const val ACTION_START_WORKOUT = "com.tamreeni.app.action.START_WORKOUT"
        const val ACTION_RESUME_WORKOUT = "com.tamreeni.app.action.RESUME_WORKOUT"
        const val ACTION_START_REST = "com.tamreeni.app.action.START_REST"
        const val ACTION_STOP = "com.tamreeni.app.action.STOP"
        const val EXTRA_SECONDS = "seconds"
    }

    private val handler = Handler(Looper.getMainLooper())
    private var tickRunnable: Runnable? = null
    private var mode = "workout"
    private var workoutStart = 0L
    private var restEnd = 0L
    private var restRang = false
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "tamreeni:workoutTimer")
            wakeLock?.setReferenceCounted(false)
        } catch (e: Exception) { /* ignore */ }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_WORKOUT -> {
                mode = "workout"
                workoutStart = System.currentTimeMillis()
                startForeground(NOTIF_ID, buildNotification("⏱ تمرينتي شغالة", "مدة التمرين: 0:00", false))
                acquireWakeLock()
                startTicking()
            }
            ACTION_RESUME_WORKOUT -> {
                mode = "workout"
                if (workoutStart == 0L) workoutStart = System.currentTimeMillis()
                acquireWakeLock()
                startTicking()
            }
            ACTION_START_REST -> {
                val seconds = intent.getIntExtra(EXTRA_SECONDS, 90)
                mode = "rest"
                restEnd = System.currentTimeMillis() + seconds * 1000L
                restRang = false
                acquireWakeLock()
                startTicking()
            }
            ACTION_STOP -> {
                stopTicking()
                releaseWakeLock()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun acquireWakeLock() {
        try {
            if (wakeLock?.isHeld == false) wakeLock?.acquire(6 * 60 * 60 * 1000L) // حد أقصى 6 ساعات أمان
        } catch (e: Exception) { /* ignore */ }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) wakeLock?.release()
        } catch (e: Exception) { /* ignore */ }
    }

    private fun startTicking() {
        stopTicking()
        val r = object : Runnable {
            override fun run() {
                try {
                    tick()
                } catch (e: Exception) {
                    // منمنعش الحلقة توقف حتى لو حصل استثناء غير متوقع
                }
                handler.postDelayed(this, 1000)
            }
        }
        tickRunnable = r
        handler.post(r)
    }

    private fun stopTicking() {
        tickRunnable?.let { handler.removeCallbacks(it) }
        tickRunnable = null
    }

    private fun fmt(totalSeconds: Int): String {
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        val s = totalSeconds % 60
        return if (h > 0) String.format("%d:%02d:%02d", h, m, s) else String.format("%d:%02d", m, s)
    }

    private fun tick() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (mode == "rest") {
            val remMs = restEnd - System.currentTimeMillis()
            val rem = Math.ceil(remMs / 1000.0).toInt()
            if (rem <= 0) {
                if (!restRang) {
                    restRang = true
                    vibrate()
                    nm.notify(NOTIF_ID, buildNotification("✅ خلصت الراحة!", "ابدأ السيت الجديد", true))
                }
                handler.postDelayed({ if (mode == "rest") mode = "workout" }, 4000)
                return
            }
            nm.notify(NOTIF_ID, buildNotification("🔥 الراحة شغالة", "متبقي " + fmt(rem), false))
        } else {
            val elapsed = ((System.currentTimeMillis() - workoutStart) / 1000).toInt().coerceAtLeast(0)
            nm.notify(NOTIF_ID, buildNotification("⏱ تمرينتي شغالة", "مدة التمرين: " + fmt(elapsed), false))
        }
    }

    private fun vibrate() {
        try {
            val pattern = longArrayOf(0, 250, 90, 250, 90, 250)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                val v = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                v.vibrate(VibrationEffect.createWaveform(pattern, -1))
            }
        } catch (e: Exception) { /* ignore */ }
    }

    private fun buildNotification(title: String, text: String, alert: Boolean): Notification {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(applicationInfo.icon)
            .setOngoing(true)
            .setOnlyAlertOnce(!alert)
            .setPriority(if (alert) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_LOW)
        if (alert) {
            builder.setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
        }
        return builder.build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "تمرينتي — تايمر التمرين",
                    NotificationManager.IMPORTANCE_HIGH
                )
                channel.description = "إشعار مدة التمرين وعداد الراحة"
                channel.enableVibration(true)
                nm.createNotificationChannel(channel)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopTicking()
        releaseWakeLock()
        super.onDestroy()
    }
}
