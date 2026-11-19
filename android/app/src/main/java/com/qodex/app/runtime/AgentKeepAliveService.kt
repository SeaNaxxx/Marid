package com.qodex.app.runtime

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat

// Kotlin host duty #1 from the architecture directive: keep the engine process
// alive through Doze/OEM kills while an agent task runs; the partial WakeLock
// also guards the engine's sockets from being frozen when the screen locks.
class AgentKeepAliveService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        // Notification channels exist only on API 26+; minSdk is 24 so the
        // pre-O path uses NotificationCompat without a channel.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Agent runtime",
                    NotificationManager.IMPORTANCE_LOW,
                ).apply { description = "Shown while the Qodex engine is running" },
            )
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(applicationInfo.icon)
            .setContentTitle("Qodex engine running")
            .setContentText("Agent tasks stay alive in the background")
            .setOngoing(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        // WakeLock is re-acquired idempotently so repeated starts never leak.
        wakeLock?.takeIf { it.isHeld }?.release()
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "qodex:agent-engine").apply {
            setReferenceCounted(false)
            acquire(WAKELOCK_TIMEOUT_MS)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = null

    companion object {
        private const val CHANNEL_ID = "qodex-agent-runtime"
        private const val NOTIFICATION_ID = 0x51

        // 6h upper bound keeps a forgotten service from draining battery forever;
        // a new start while tasks run re-arms it.
        private const val WAKELOCK_TIMEOUT_MS = 6 * 60 * 60 * 1000L

        fun start(ctx: Context) {
            val intent = Intent(ctx, AgentKeepAliveService::class.java)
            // startService throws IllegalStateException when the app is in the
            // background on API 26+ (boot() runs before the activity resumes);
            // startForegroundService + the onCreate promote covers that path.
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                ctx.startForegroundService(intent)
            } else {
                ctx.startService(intent)
            }
        }
    }
}
