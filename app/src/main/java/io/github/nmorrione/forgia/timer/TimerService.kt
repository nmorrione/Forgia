package io.github.nmorrione.forgia.timer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import io.github.nmorrione.forgia.MainActivity
import io.github.nmorrione.forgia.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Servizio in primo piano: tiene vivo il processo e la CPU mentre il timer gira,
 * e mostra una notifica con il tempo che scorre e i comandi.
 */
class TimerService : Service() {

    companion object {
        const val ACTION_PAUSE = "forgia.timer.PAUSE"
        const val ACTION_RESUME = "forgia.timer.RESUME"
        const val ACTION_SKIP = "forgia.timer.SKIP"
        const val ACTION_STOP = "forgia.timer.STOP"
        private const val CHANNEL = "timer"
        private const val NOTIFICATION_ID = 1
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var watcher: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Timer", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Il timer in corso"
                setShowBadge(false)
            },
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PAUSE -> TimerEngine.pause()
            ACTION_RESUME -> TimerEngine.resume()
            ACTION_SKIP -> TimerEngine.skip()
            ACTION_STOP -> TimerEngine.stop()
        }
        ServiceCompat.startForeground(
            this, NOTIFICATION_ID, build(TimerEngine.state.value),
            if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0,
        )
        if (wakeLock == null) {
            wakeLock = getSystemService(PowerManager::class.java)
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "forgia:timer")
                .apply { acquire(4 * 60 * 60 * 1000L) }
        }
        if (watcher == null) {
            watcher = scope.launch {
                // La notifica cambia solo quando cambia il tratto: il conto alla rovescia lo fa il cronometro.
                TimerEngine.state
                    .map { it.copy(remainingMs = 0, totalRemainingMs = 0) }
                    .distinctUntilChanged()
                    .collectLatest { st ->
                        when {
                            !st.active -> finishService()
                            st.phase == Phase.DONE -> {
                                notifySafe(build(TimerEngine.state.value))
                                delay(5000)
                                finishService()
                            }
                            else -> notifySafe(build(TimerEngine.state.value))
                        }
                    }
            }
        }
        return START_NOT_STICKY
    }

    private fun finishService() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        scope.cancel()
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
        super.onDestroy()
    }

    private fun notifySafe(n: android.app.Notification) {
        runCatching { NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, n) }
    }

    private fun action(action: String, code: Int): PendingIntent = PendingIntent.getService(
        this, code, Intent(this, TimerService::class.java).setAction(action),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun build(st: TimerState): android.app.Notification {
        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_OPEN_TIMER, true)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val title = buildString {
            append(st.phase.label.ifBlank { "Timer" })
            st.exercise?.let { append(" · ").append(it) }
        }
        val detail = buildList {
            if (st.kind == TimerKind.INTERVAL && st.rounds > 0 && st.phase != Phase.DONE) {
                add("Ripetizione ${st.round} di ${st.rounds}")
                if (st.sets > 1) add("serie ${st.set} di ${st.sets}")
            }
            st.next?.let { add("poi $it") }
            if (st.paused) add("in pausa, ${fmtClock(st.remainingMs)}")
        }.joinToString(", ")

        val b = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle(title)
            .setContentText(detail.ifBlank { st.title })
            .setContentIntent(open)
            .setOngoing(st.phase != Phase.DONE)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(if (st.phase == Phase.RINGING) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_STOPWATCH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        if (st.running && !st.paused) {
            b.setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setShowWhen(true)
                .setWhen(System.currentTimeMillis() + st.remainingMs)
        }
        when {
            st.phase == Phase.RINGING -> b.addAction(0, "Ferma", action(ACTION_STOP, 4))
            st.phase == Phase.DONE -> b.addAction(0, "Chiudi", action(ACTION_STOP, 4))
            else -> {
                if (st.paused) b.addAction(0, "Riprendi", action(ACTION_RESUME, 2))
                else b.addAction(0, "Pausa", action(ACTION_PAUSE, 1))
                if (st.kind == TimerKind.INTERVAL) b.addAction(0, "Salta", action(ACTION_SKIP, 3))
                b.addAction(0, "Stop", action(ACTION_STOP, 4))
            }
        }
        return b.build()
    }
}
