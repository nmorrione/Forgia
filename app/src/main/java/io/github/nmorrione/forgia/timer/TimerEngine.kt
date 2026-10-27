package io.github.nmorrione.forgia.timer

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.core.content.ContextCompat
import io.github.nmorrione.forgia.data.IntervalConfig
import io.github.nmorrione.forgia.data.TimerSettings
import io.github.nmorrione.forgia.data.TimerSound
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

/**
 * Il timer vive nel processo dell'app, non nella schermata: continua a contare (e a suonare)
 * anche a schermo spento, tenuto in vita da [TimerService].
 * Tutte le operazioni girano su un unico thread, quindi lo stato non ha bisogno di lock.
 */
object TimerEngine {
    private val scope = CoroutineScope(
        SupervisorJob() + Executors.newSingleThreadExecutor { r -> Thread(r, "forgia-timer") }.asCoroutineDispatcher(),
    )
    private val _state = MutableStateFlow(TimerState())
    val state: StateFlow<TimerState> = _state.asStateFlow()

    private lateinit var app: Context
    private var kind = TimerKind.NONE
    private var title = ""
    private var steps: List<Step> = emptyList()
    /** Millisecondi che restano dopo ogni passo, per il tempo totale. */
    private var after: LongArray = LongArray(0)
    private var rounds = 0
    private var sets = 0
    private var idx = 0
    private var stepEnd = 0L
    private var pausedRemaining = -1L
    private var lastTick = 0
    private var loop: Job? = null
    private var aux: Job? = null
    private var beeper: Beeper? = null
    private var alarm: AlarmPlayer? = null
    private var buzzer: Buzzer? = null
    private var vibrate = true

    private fun now() = SystemClock.elapsedRealtime()

    fun startCountdown(ctx: Context, seconds: Int, s: TimerSettings) = start(ctx, s) {
        kind = TimerKind.COUNTDOWN
        title = "Timer"
        rounds = 0
        sets = 0
        listOf(Step(Phase.COUNTDOWN, seconds.coerceAtLeast(1) * 1000L))
    }

    fun startInterval(ctx: Context, c: IntervalConfig, s: TimerSettings) = start(ctx, s) {
        kind = TimerKind.INTERVAL
        title = c.name
        rounds = c.rounds.coerceAtLeast(1)
        sets = c.sets.coerceAtLeast(1)
        Intervals.steps(c)
    }

    /** Fa sentire i suoni, per regolare il volume. */
    fun preview(ctx: Context, s: TimerSettings) {
        app = ctx.applicationContext
        scope.launch {
            if (state.value.active) return@launch
            val b = Beeper(s.sound == TimerSound.ALARM)
            listOf(Cue.TICK, Cue.TICK, Cue.TICK, Cue.WORK).forEach { b.play(it); delay(1000) }
            b.play(Cue.REST)
            delay(1200)
            b.play(Cue.FINISH)
            delay(3000)
            b.release()
        }
    }

    private fun start(ctx: Context, s: TimerSettings, build: () -> List<Step>) {
        app = ctx.applicationContext
        scope.launch {
            shutdown()
            val built = build()
            if (built.isEmpty()) return@launch
            steps = built
            after = LongArray(built.size).also { a ->
                var acc = 0L
                for (i in built.indices.reversed()) {
                    a[i] = acc
                    acc += built[i].ms
                }
            }
            vibrate = s.vibrate
            beeper = Beeper(s.sound == TimerSound.ALARM)
            buzzer = Buzzer(app)
            pausedRemaining = -1
            enter(0, now(), cue = steps[0].phase == Phase.WORK)
            publish(steps[0].ms)
            runLoop()
            ContextCompat.startForegroundService(app, Intent(app, TimerService::class.java))
        }
    }

    fun pause() {
        scope.launch {
            if (!state.value.running || pausedRemaining >= 0) return@launch
            pausedRemaining = (stepEnd - now()).coerceAtLeast(0)
            loop?.cancel()
            publish(pausedRemaining)
        }
    }

    fun resume() {
        scope.launch {
            if (pausedRemaining < 0) return@launch
            stepEnd = now() + pausedRemaining
            pausedRemaining = -1
            publish(stepEnd - now())
            runLoop()
        }
    }

    /** Passa subito al tratto successivo del circuito. */
    fun skip() {
        scope.launch {
            if (kind != TimerKind.INTERVAL || !state.value.running) return@launch
            if (idx + 1 >= steps.size) {
                finish()
                return@launch
            }
            val paused = pausedRemaining >= 0
            enter(idx + 1, now(), cue = !paused)
            if (paused) pausedRemaining = steps[idx].ms
            publish(if (paused) pausedRemaining else stepEnd - now())
        }
    }

    /** Ferma tutto e torna alla schermata di impostazione. */
    fun stop() {
        scope.launch {
            shutdown()
            _state.value = TimerState()
        }
    }

    private fun shutdown() {
        loop?.cancel()
        aux?.cancel()
        alarm?.stop()
        alarm = null
        buzzer?.cancel()
        beeper?.release()
        beeper = null
        pausedRemaining = -1
    }

    private fun runLoop() {
        loop?.cancel()
        loop = scope.launch {
            while (isActive) {
                tick()
                delay(50)
            }
        }
    }

    private fun enter(i: Int, start: Long, cue: Boolean) {
        idx = i
        lastTick = 0
        stepEnd = start + steps[i].ms
        if (cue) {
            beeper?.play(if (steps[i].phase == Phase.WORK) Cue.WORK else Cue.REST)
            if (vibrate) buzzer?.once(if (steps[i].phase == Phase.WORK) 350 else 150)
        }
    }

    private fun tick() {
        val rem = stepEnd - now()
        if (rem <= 0) {
            next()
            return
        }
        // Tre beep negli ultimi tre secondi di ogni tratto del circuito
        if (steps[idx].phase != Phase.COUNTDOWN) {
            val sec = ((rem + 999) / 1000).toInt()
            if (sec in 1..3 && sec != lastTick) {
                lastTick = sec
                beeper?.play(Cue.TICK)
            }
        }
        publish(rem)
    }

    private fun next() {
        if (steps[idx].phase == Phase.COUNTDOWN) {
            ring()
            return
        }
        if (idx + 1 >= steps.size) {
            finish()
            return
        }
        // Il tratto nuovo parte dalla fine esatta del precedente: nessuna deriva.
        enter(idx + 1, stepEnd, cue = true)
        publish(stepEnd - now())
    }

    private fun finish() {
        loop?.cancel()
        _state.value = _state.value.copy(phase = Phase.DONE, remainingMs = 0, totalRemainingMs = 0, paused = false, next = null)
        beeper?.play(Cue.FINISH)
        if (vibrate) buzzer?.pattern(longArrayOf(0, 400, 150, 400, 150, 900), repeat = false)
        val b = beeper
        beeper = null
        // Non legato ad aux: va chiuso anche se nel frattempo parte un altro timer.
        scope.launch {
            delay(4000)
            b?.release()
        }
    }

    private fun ring() {
        loop?.cancel()
        _state.value = _state.value.copy(phase = Phase.RINGING, remainingMs = 0, totalRemainingMs = 0, paused = false)
        val player = AlarmPlayer(app)
        alarm = player
        val ok = player.start()
        if (vibrate) buzzer?.pattern(longArrayOf(0, 600, 400), repeat = true)
        aux = scope.launch {
            // Se la sveglia di sistema non parte, suoniamo i nostri beep.
            if (!ok) {
                repeat(20) {
                    beeper?.play(Cue.FINISH)
                    delay(3000)
                }
            } else {
                delay(60_000)
            }
            // Dopo un minuto smette da sola.
            alarm?.stop()
            alarm = null
            buzzer?.cancel()
            beeper?.release()
            beeper = null
            _state.value = _state.value.copy(phase = Phase.DONE)
        }
    }

    private fun publish(rem: Long) {
        val st = steps[idx]
        _state.value = TimerState(
            kind = kind,
            phase = st.phase,
            remainingMs = rem,
            stepMs = st.ms,
            totalRemainingMs = rem + after[idx],
            paused = pausedRemaining >= 0,
            round = st.round,
            rounds = rounds,
            set = st.set,
            sets = sets,
            exercise = st.exercise,
            next = st.next,
            title = title,
        )
    }
}
