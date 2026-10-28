package io.github.nmorrione.forgia.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import io.github.nmorrione.forgia.data.EndSound
import io.github.nmorrione.forgia.data.IntervalConfig
import io.github.nmorrione.forgia.data.TimerSettings
import io.github.nmorrione.forgia.data.TimerSound
import io.github.nmorrione.forgia.timer.Intervals
import io.github.nmorrione.forgia.timer.Phase
import io.github.nmorrione.forgia.timer.TimerEngine
import io.github.nmorrione.forgia.timer.TimerKind
import io.github.nmorrione.forgia.timer.TimerState
import io.github.nmorrione.forgia.timer.fmtClock
import io.github.nmorrione.forgia.ui.AppViewModel
import io.github.nmorrione.forgia.ui.Tab
import io.github.nmorrione.forgia.ui.components.Btn
import io.github.nmorrione.forgia.ui.components.CardBox
import io.github.nmorrione.forgia.ui.components.Chip
import io.github.nmorrione.forgia.ui.components.ChipRow
import io.github.nmorrione.forgia.ui.components.H2
import io.github.nmorrione.forgia.ui.components.LinkBtn
import io.github.nmorrione.forgia.ui.components.Muted
import io.github.nmorrione.forgia.ui.components.ScreenColumn
import io.github.nmorrione.forgia.ui.components.SmallField
import io.github.nmorrione.forgia.ui.edit.SwitchRow
import io.github.nmorrione.forgia.ui.edit.replaceAt
import io.github.nmorrione.forgia.ui.edit.without
import io.github.nmorrione.forgia.ui.theme.Body
import io.github.nmorrione.forgia.ui.theme.Pal
import io.github.nmorrione.forgia.ui.theme.Small
import io.github.nmorrione.forgia.ui.theme.Tiny
import io.github.nmorrione.forgia.ui.theme.cond
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun TimerScreen(vm: AppViewModel) {
    val st by TimerEngine.state.collectAsState()
    val ctx = LocalContext.current
    val settings = vm.data.timer

    // Schermo sempre acceso mentre il timer corre e la schermata è aperta
    val view = LocalView.current
    val keepOn = st.running && !st.paused
    DisposableEffect(keepOn) {
        view.keepScreenOn = keepOn
        onDispose { view.keepScreenOn = false }
    }

    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    fun askNotifications() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    fun setTimer(f: (TimerSettings) -> TimerSettings) = vm.update { it.copy(timer = f(it.timer)) }

    ScreenColumn(scroll = vm.scroll(Tab.TIMER)) {
        if (st.active) RunningTimer(st) else TimerSetup(vm, settings, ::setTimer, ::askNotifications)
    }
}

@Composable
private fun TimerSetup(
    vm: AppViewModel,
    settings: TimerSettings,
    setTimer: ((TimerSettings) -> TimerSettings) -> Unit,
    askNotifications: () -> Unit,
) {
    val ctx = LocalContext.current
    Column {
        Row(Modifier.padding(top = 12.dp, bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Btn("Timer", Modifier.weight(1f), filled = !vm.timerIntervals) { vm.timerIntervals = false }
            Btn("Intervalli", Modifier.weight(1f), filled = vm.timerIntervals) { vm.timerIntervals = true }
        }
        if (!vm.timerIntervals) {
            CountdownSetup(settings.countdownSec, { v -> setTimer { it.copy(countdownSec = v.coerceIn(5, 5 * 3600)) } }) {
                askNotifications()
                TimerEngine.startCountdown(ctx, settings.countdownSec, settings)
            }
        } else {
            IntervalSetup(settings, setTimer) {
                askNotifications()
                TimerEngine.startInterval(ctx, settings.interval, settings)
            }
        }

        H2("Suoni")
        CardBox {
            SwitchRow(
                "Suona anche in silenzioso", settings.sound == TimerSound.ALARM,
                help = "Usa il volume della sveglia. Spento, i beep seguono il volume multimediale e si mescolano alla musica.",
            ) { v -> setTimer { it.copy(sound = if (v) TimerSound.ALARM else TimerSound.MEDIA) } }
            SwitchRow("Vibrazione", settings.vibrate) { v -> setTimer { it.copy(vibrate = v) } }
            Text("Fine del timer classico", style = Body.copy(fontWeight = FontWeight.SemiBold), modifier = Modifier.padding(top = 8.dp))
            ChipRow {
                Chip("Cicalino", settings.endSound == EndSound.BEEPER) { setTimer { it.copy(endSound = EndSound.BEEPER) } }
                Chip("Sveglia di sistema", settings.endSound == EndSound.SYSTEM_ALARM) { setTimer { it.copy(endSound = EndSound.SYSTEM_ALARM) } }
            }
            Muted("Suona sempre con il volume della sveglia, finché non lo fermi (al massimo un minuto).", Modifier.padding(top = 6.dp), Tiny)
            LinkBtn("Prova i suoni") { TimerEngine.preview(ctx, settings) }
        }
        Muted(
            "Il timer continua anche a schermo spento o se passi a un’altra app: lo trovi nella notifica, con pausa e stop.",
            Modifier.padding(top = 10.dp), Tiny,
        )
    }
}

/* ---------- Timer in corso ---------- */

@Composable
private fun RunningTimer(st: TimerState) {
    val p = Pal
    val color = when (st.phase) {
        Phase.WORK -> p.good
        Phase.REST, Phase.SET_REST -> p.accent
        Phase.PREPARE -> p.warn
        Phase.RINGING -> p.bad
        Phase.DONE -> p.good
        else -> p.ink
    }
    val finished = st.phase == Phase.DONE || st.phase == Phase.RINGING
    Column(
        Modifier
            .padding(top = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.12f))
            .border(2.dp, color, RoundedCornerShape(20.dp))
            .padding(horizontal = 18.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (st.title.isNotBlank() && st.kind == TimerKind.INTERVAL) Text(st.title, style = cond(18), color = p.muted)
        Text(
            when {
                st.phase == Phase.DONE && st.kind == TimerKind.INTERVAL -> "Circuito finito!"
                st.phase == Phase.DONE -> "Tempo scaduto"
                else -> st.phase.label
            },
            style = cond(34, FontWeight.Bold), color = color, textAlign = TextAlign.Center,
        )
        st.exercise?.takeIf { !finished }?.let { Text(it, style = cond(26), textAlign = TextAlign.Center) }
        Text(
            fmtClock(st.remainingMs),
            style = cond(104, FontWeight.Bold, 1.0),
            color = if (st.paused) p.muted else p.ink,
        )
        if (!finished) {
            val progress = if (st.stepMs > 0) 1f - st.remainingMs.toFloat() / st.stepMs else 0f
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = color,
                trackColor = p.line,
                strokeCap = StrokeCap.Round,
                gapSize = 0.dp,
                drawStopIndicator = {},
            )
        }
        if (st.kind == TimerKind.INTERVAL && !finished) {
            val info = buildList {
                add("Ripetizione ${st.round} di ${st.rounds}")
                if (st.sets > 1) add("serie ${st.set} di ${st.sets}")
            }.joinToString(" · ")
            Text(info, style = Body.copy(fontWeight = FontWeight.SemiBold), modifier = Modifier.padding(top = 14.dp))
            st.next?.let { Text("Poi: $it", style = Body, color = p.muted) }
            Text("Mancano ${fmtClock(st.totalRemainingMs)} in tutto", style = Small, color = p.muted, modifier = Modifier.padding(top = 4.dp))
        }
        if (st.paused) Text("In pausa", style = cond(20), color = p.muted, modifier = Modifier.padding(top = 6.dp))
    }

    Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        when {
            st.phase == Phase.RINGING -> Btn("Ferma", Modifier.weight(1f), filled = true) { TimerEngine.stop() }
            st.phase == Phase.DONE -> Btn("Chiudi", Modifier.weight(1f), filled = true) { TimerEngine.stop() }
            else -> {
                Btn(if (st.paused) "Riprendi" else "Pausa", Modifier.weight(1.4f), filled = true) {
                    if (st.paused) TimerEngine.resume() else TimerEngine.pause()
                }
                if (st.kind == TimerKind.INTERVAL) Btn("Salta", Modifier.weight(1f)) { TimerEngine.skip() }
                Btn("Stop", Modifier.weight(1f), color = p.bad) { TimerEngine.stop() }
            }
        }
    }
}

/* ---------- Timer classico ---------- */

@Composable
private fun CountdownSetup(seconds: Int, onChange: (Int) -> Unit, onStart: () -> Unit) {
    CardBox(Modifier.padding(top = 12.dp)) {
        Text(
            fmtClock(seconds * 1000L),
            style = cond(88, FontWeight.Bold, 1.0),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Btn("−1 min", Modifier.weight(1f)) { onChange(seconds - 60) }
            Btn("−15 s", Modifier.weight(1f)) { onChange(seconds - 15) }
            Btn("+15 s", Modifier.weight(1f)) { onChange(seconds + 15) }
            Btn("+1 min", Modifier.weight(1f)) { onChange(seconds + 60) }
        }
        ChipRow {
            listOf(30, 60, 90, 120, 150, 180, 240, 300, 600).forEach { s ->
                Chip(fmtClock(s * 1000L), s == seconds) { onChange(s) }
            }
        }
    }
    Btn("Avvia", Modifier.padding(top = 12.dp).fillMaxWidth(), filled = true, onClick = onStart)
    Muted(
        "Alla fine suona finché non lo fermi, da qui o dalla barra del timer in fondo a ogni schermata. " +
            "Sotto ogni esercizio, in Oggi, c’è anche il pulsante per far partire il recupero.",
        Modifier.padding(top = 8.dp), Tiny,
    )
}

/* ---------- Timer a intervalli ---------- */

@Composable
private fun IntervalSetup(settings: TimerSettings, setTimer: ((TimerSettings) -> TimerSettings) -> Unit, onStart: () -> Unit) {
    val c = settings.interval
    val p = Pal
    var naming by remember { mutableStateOf(false) }
    fun setC(f: (IntervalConfig) -> IntervalConfig) = setTimer { it.copy(interval = f(it.interval)) }
    val presetIndex = settings.presets.indexOfFirst { it == c }

    if (settings.presets.isNotEmpty()) {
        ChipRow {
            settings.presets.forEach { pr -> Chip(pr.name, pr == c) { setC { pr } } }
        }
    }
    CardBox(Modifier.padding(top = 12.dp)) {
        StepRow("Lavoro", fmtClock(c.workSec * 1000L), c.workSec > 5, { setC { it.copy(workSec = (it.workSec - 5).coerceAtLeast(5)) } }) {
            setC { it.copy(workSec = it.workSec + 5) }
        }
        StepRow("Recupero", fmtClock(c.restSec * 1000L), c.restSec > 0, { setC { it.copy(restSec = (it.restSec - 5).coerceAtLeast(0)) } }) {
            setC { it.copy(restSec = it.restSec + 5) }
        }
        StepRow("Ripetizioni", "${c.rounds}", c.rounds > 1, { setC { it.copy(rounds = (it.rounds - 1).coerceAtLeast(1)) } }) {
            setC { it.copy(rounds = (it.rounds + 1).coerceAtMost(99)) }
        }
        StepRow("Serie", "${c.sets}", c.sets > 1, { setC { it.copy(sets = (it.sets - 1).coerceAtLeast(1)) } }) {
            setC { it.copy(sets = (it.sets + 1).coerceAtMost(20)) }
        }
        if (c.sets > 1) {
            StepRow("Recupero tra le serie", fmtClock(c.setRestSec * 1000L), c.setRestSec > 0, { setC { it.copy(setRestSec = (it.setRestSec - 15).coerceAtLeast(0)) } }) {
                setC { it.copy(setRestSec = it.setRestSec + 15) }
            }
        }
        StepRow("Preparazione", fmtClock(c.prepareSec * 1000L), c.prepareSec > 0, { setC { it.copy(prepareSec = (it.prepareSec - 5).coerceAtLeast(0)) } }) {
            setC { it.copy(prepareSec = it.prepareSec + 5) }
        }
    }

    H2("Esercizi del circuito", top = 20.dp)
    Muted("Facoltativi, uno per riga: durante il lavoro vedi quale fare, nel recupero quale viene dopo. Se sono meno delle ripetizioni ricominciano dal primo.", Modifier.padding(bottom = 8.dp))
    SmallField(
        value = c.exercises.joinToString("\n"),
        onValueChange = { v -> setC { it.copy(exercises = v.lines()) } },
        modifier = Modifier.fillMaxWidth(),
        placeholder = "Burpees\nPiegamenti\nSalti alla corda…",
        style = Body,
        center = false,
        singleLine = false,
        minLines = 3,
    )
    val names = c.exercises.filter { it.isNotBlank() }
    if (names.isNotEmpty() && names.size != c.rounds) {
        LinkBtn("Una ripetizione per esercizio (${names.size})") { setC { it.copy(rounds = names.size) } }
    }

    Text(
        "Durata totale: ${fmtClock(Intervals.totalMs(c))}",
        style = Body.copy(fontWeight = FontWeight.SemiBold),
        modifier = Modifier.padding(top = 14.dp),
    )
    Btn("Avvia", Modifier.padding(top = 10.dp).fillMaxWidth(), filled = true, onClick = onStart)
    Row {
        LinkBtn("Salva come preset") { naming = true }
        if (presetIndex >= 0) {
            LinkBtn("Elimina «${c.name}»", color = p.bad) { setTimer { it.copy(presets = it.presets.without(presetIndex)) } }
        }
    }

    if (naming) {
        var name by remember { mutableStateOf(c.name) }
        AlertDialog(
            onDismissRequest = { naming = false },
            title = { Text("Salva il circuito") },
            text = { OutlinedTextField(name, { name = it }, label = { Text("Nome") }, singleLine = true) },
            confirmButton = {
                TextButton(onClick = {
                    val n = name.trim().ifBlank { "Circuito" }
                    setTimer { t ->
                        val cfg = t.interval.copy(name = n)
                        val i = t.presets.indexOfFirst { it.name == n }
                        t.copy(interval = cfg, presets = if (i >= 0) t.presets.replaceAt(i, cfg) else t.presets + cfg)
                    }
                    naming = false
                }) { Text("Salva") }
            },
            dismissButton = { TextButton(onClick = { naming = false }) { Text("Annulla") } },
        )
    }
}

@Composable
private fun StepRow(label: String, value: String, canDown: Boolean, onDown: () -> Unit, onUp: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = Body.copy(fontWeight = FontWeight.SemiBold), modifier = Modifier.weight(1f))
        RepeatButton("−", canDown, onDown)
        Text(value, style = cond(24), textAlign = TextAlign.Center, modifier = Modifier.width(76.dp))
        RepeatButton("+", true, onUp)
    }
}

/** Tieni premuto per far scorrere il valore. */
@Composable
private fun RepeatButton(text: String, enabled: Boolean, onStep: () -> Unit) {
    val p = Pal
    val scope = rememberCoroutineScope()
    val step by rememberUpdatedState(onStep)
    val on by rememberUpdatedState(enabled)
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(p.surface)
            .border(1.dp, p.line, CircleShape)
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    if (!on) return@detectTapGestures
                    val job = scope.launch {
                        step()
                        delay(450)
                        while (on) {
                            step()
                            delay(90)
                        }
                    }
                    tryAwaitRelease()
                    job.cancel()
                })
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = Body.copy(fontSize = Body.fontSize * 1.4), color = if (enabled) p.ink else p.ink.copy(alpha = 0.3f))
    }
}
