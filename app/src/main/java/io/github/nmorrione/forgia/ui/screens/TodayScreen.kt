package io.github.nmorrione.forgia.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import io.github.nmorrione.forgia.data.DayType
import io.github.nmorrione.forgia.logic.parseRestSeconds
import io.github.nmorrione.forgia.timer.TimerEngine
import io.github.nmorrione.forgia.timer.TimerKind
import io.github.nmorrione.forgia.timer.fmtClock
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import io.github.nmorrione.forgia.data.SetEntry
import io.github.nmorrione.forgia.logic.It
import io.github.nmorrione.forgia.logic.PrevSets
import io.github.nmorrione.forgia.logic.ResolvedExercise
import io.github.nmorrione.forgia.logic.Schedule
import io.github.nmorrione.forgia.logic.Tracker
import io.github.nmorrione.forgia.logic.capitalize
import io.github.nmorrione.forgia.logic.fmtInt
import io.github.nmorrione.forgia.logic.fmtNum
import io.github.nmorrione.forgia.ui.AppViewModel
import io.github.nmorrione.forgia.ui.Tab
import io.github.nmorrione.forgia.ui.Route
import io.github.nmorrione.forgia.ui.components.Banner
import io.github.nmorrione.forgia.ui.components.Btn
import io.github.nmorrione.forgia.ui.components.CardBox
import io.github.nmorrione.forgia.ui.components.Chip
import io.github.nmorrione.forgia.ui.components.ChipRow
import io.github.nmorrione.forgia.ui.components.Dot
import io.github.nmorrione.forgia.ui.components.H2
import io.github.nmorrione.forgia.ui.components.H3
import io.github.nmorrione.forgia.ui.components.LinkBtn
import io.github.nmorrione.forgia.ui.components.Muted
import io.github.nmorrione.forgia.ui.components.NumberInput
import io.github.nmorrione.forgia.ui.components.RoundIcon
import io.github.nmorrione.forgia.ui.components.ScreenColumn
import io.github.nmorrione.forgia.ui.components.SmallField
import io.github.nmorrione.forgia.ui.components.TipsCard
import io.github.nmorrione.forgia.ui.components.foodColor
import io.github.nmorrione.forgia.ui.theme.Body
import io.github.nmorrione.forgia.ui.theme.Pal
import io.github.nmorrione.forgia.ui.theme.Small
import io.github.nmorrione.forgia.ui.theme.Tiny
import io.github.nmorrione.forgia.ui.theme.cond
import io.github.nmorrione.forgia.ui.theme.sessionColor
import java.time.LocalDate
import kotlin.math.max

@Composable
fun TodayScreen(vm: AppViewModel) {
    val data = vm.data
    val s = remember(data.program) { Schedule(data.program) }
    val t = remember(s, data.log) { Tracker(s, data.log) }
    val today = remember { LocalDate.now() }
    val todayIdx = s.indexOf(today)
    // Prima dell'inizio si può già segnare: è la preparazione (giorni con indice negativo).
    val first = s.firstDay(today, data.log)
    // Dopo la fine si continua a segnare finché non parte il ciclo successivo.
    val last = s.lastDay(today, data.log)
    val home = todayIdx.coerceIn(first, last)
    val sel = (vm.selectedDay ?: todayIdx).coerceIn(first, last)
    val date = s.dateAt(sel)
    val key = date.toString()
    val e = t.day(sel)
    val sess = s.sessionAt(sel, data.log)
    val w = s.weekOf(sel)
    val p = Pal
    val c = sessionColor(sess.color)

    val ctx = LocalContext.current
    // Il recupero non parte se c'è un circuito a intervalli in corso: lo interromperebbe.
    val intervalRunning by remember {
        TimerEngine.state.map { it.active && it.kind == TimerKind.INTERVAL }.distinctUntilChanged()
    }.collectAsState(initial = false)

    // Cambiando sezione la posizione resta; cambiando giorno si riparte dall'inizio.
    LaunchedEffect(sel) {
        if (vm.todayScrollDay != null && vm.todayScrollDay != sel) vm.scroll(Tab.OGGI).scrollTo(0)
        vm.todayScrollDay = sel
    }

    ScreenColumn(scroll = vm.scroll(Tab.OGGI)) {
        // Navigazione tra i giorni
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundIcon("‹", enabled = sel > first) { vm.selectedDay = sel - 1 }
            Column(Modifier.weight(1f).padding(horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(capitalize(It.dayTitle(date)), style = cond(22), textAlign = TextAlign.Center)
                Text(
                    if (sel < 0) {
                        "${s.phaseLabel(w)}, ${if (sel == -1) "manca 1 giorno" else "mancano ${-sel} giorni"} all’inizio"
                    } else if (sel >= s.days) {
                        "Dopo la fine del ciclo (${It.shortDate(s.end)})"
                    } else {
                        "Settimana ${w + 1} di ${s.weeks}, ${s.phaseLabel(w)}"
                    },
                    style = Small, color = p.muted, textAlign = TextAlign.Center,
                )
            }
            RoundIcon("›", enabled = sel < last) { vm.selectedDay = sel + 1 }
        }
        if (todayIdx < 0 && sel == todayIdx) {
            val n = -todayIdx
            Banner(
                "Il programma parte ${It.dayTitle(s.start)}, tra $n ${if (n == 1) "giorno" else "giorni"}. " +
                    "Fino ad allora sei in preparazione: segui la settimana tipo con i carichi del primo blocco e segna peso e serie. " +
                    "Dal primo giorno vedrai sotto ogni esercizio cosa hai fatto l’ultima volta.",
            )
        }
        if (todayIdx == 0 && sel == 0 && data.log.days.isEmpty()) {
            Banner("Il programma parte oggi. Se preferisci un altro giorno, cambia la data di inizio da Programma, in alto a destra.")
        }
        if (s.isFinished(today) && sel >= s.days - 1) {
            CardBox(Modifier.padding(top = 12.dp)) {
                Text(
                    if (todayIdx == s.days - 1) "Oggi è l’ultimo giorno del ciclo." else "Il ciclo è finito il ${It.shortDate(s.end)}.",
                    style = Body.copy(fontWeight = FontWeight.SemiBold),
                )
                Muted(
                    "Guarda il riepilogo e scegli il prossimo ciclo. Intanto puoi continuare a segnare peso e allenamenti qui.",
                    Modifier.padding(top = 4.dp),
                )
                Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Btn("Riepilogo", Modifier.weight(1f)) { vm.push(Route.CycleSummary) }
                    Btn("Prossimo ciclo", Modifier.weight(1f), filled = true) { vm.push(Route.NextCycle) }
                }
            }
        }
        if (sel != home) {
            val label = when {
                todayIdx in first..last -> "Torna a oggi"
                todayIdx < first -> "Vai al primo giorno"
                else -> "Vai all’ultimo giorno"
            }
            Box(Modifier.fillMaxWidth().padding(top = 4.dp), contentAlignment = Alignment.Center) {
                LinkBtn(label) { vm.selectedDay = null }
            }
        }

        // Sessione del giorno: il bordo è il colore della sessione
        var swapOpen by rememberSaveable(key) { mutableStateOf(false) }
        Column(
            Modifier
                .padding(top = 14.dp, bottom = 6.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 5.dp, bottomStart = 5.dp, topEnd = 16.dp, bottomEnd = 16.dp))
                .background(p.surface)
                .drawBehind { drawRect(c, size = Size(14.dp.toPx(), size.height)) }
                .padding(start = 32.dp, end = 18.dp, top = 18.dp, bottom = 16.dp),
        ) {
            Text(sess.name, style = cond(52, FontWeight.Bold, 0.92))
            if (sess.description.isNotBlank()) Muted(sess.description, Modifier.padding(top = 8.dp), Body)
            if (!sess.rest) {
                Row(
                    Modifier
                        .padding(top = 10.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { vm.updateDay(key) { it.copy(done = !it.done) } },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = e.done,
                        onCheckedChange = { v -> vm.updateDay(key) { it.copy(done = v) } },
                        colors = CheckboxDefaults.colors(checkedColor = c, uncheckedColor = p.muted),
                    )
                    Text("Allenamento fatto", style = Body.copy(fontWeight = FontWeight.SemiBold), modifier = Modifier.padding(end = 8.dp))
                }
            }
            Text(
                "Cambia la sessione di questo giorno",
                style = Small, color = p.muted,
                modifier = Modifier.padding(top = 10.dp).clickable { swapOpen = !swapOpen }.padding(vertical = 4.dp),
            )
            if (swapOpen) {
                ChipRow {
                    data.program.sessions.forEach { st ->
                        Chip(st.name, st.id == sess.id, sessionColor(st.color)) {
                            vm.updateDay(key) { d ->
                                d.copy(
                                    session = if (st.id == s.plannedSessionId(date)) null else st.id,
                                    done = if (st.rest) false else d.done,
                                )
                            }
                        }
                    }
                }
            }
        }

        // Esercizi
        if (sess.exercises.isNotEmpty()) {
            H2("Esercizi")
            if (data.program.warmup.isNotBlank()) Muted(data.program.warmup, Modifier.padding(bottom = 10.dp))
            sess.exercises.forEachIndexed { idx, ex ->
                val r = s.resolve(ex, w)
                ExerciseLogCard(
                    n = idx + 1,
                    r = r,
                    entries = e.sets[r.logKey].orEmpty(),
                    prev = t.lastSets(r.logKey, date),
                    fieldKey = "$key/${r.logKey}",
                    onRest = if (intervalRunning) null else { sec ->
                        TimerEngine.startCountdown(ctx, sec, data.timer, label = "Recupero · ${r.name}")
                    },
                ) { list ->
                    vm.updateDay(key) { d ->
                        val empty = list.all { it.isEmpty } && list.size <= r.sets
                        d.copy(sets = if (empty) d.sets - r.logKey else d.sets + (r.logKey to list))
                    }
                }
                // Tra un esercizio e il successivo: il recupero per cambiare esercizio
                if (idx < sess.exercises.lastIndex) {
                    val next = s.resolve(sess.exercises[idx + 1], w).name
                    val sec = data.program.changeRest(sess)
                    ChangeRestRow(next, sec, enabled = !intervalRunning && sec > 0) {
                        TimerEngine.startCountdown(ctx, sec, data.timer, label = "Cambio · poi $next")
                    }
                }
            }
        }

        // Consigli della sessione
        val tomorrow = s.sessionOn(date.plusDays(1), data.log).name
        val tips = sess.tips
            .filter { it.weekday == null || it.weekday == date.dayOfWeek.value }
            .map { it.text.replace("{domani}", tomorrow) }
        if (tips.isNotEmpty()) {
            H2(sess.tipsTitle.ifBlank { "Note" })
            TipsCard(tips)
        }

        // Alimentazione
        val target = data.nutrition.target(if (sess.rest) DayType.REST else DayType.TRAIN)
        H2("Alimentazione")
        Text(
            buildAnnotatedString {
                append("Obiettivo di oggi: ")
                withStyle(SpanStyle(color = p.ink, fontWeight = FontWeight.SemiBold)) { append("≈ ${fmtInt(target.kcal)} kcal") }
                append(", ")
                withStyle(SpanStyle(color = p.ink, fontWeight = FontWeight.SemiBold)) { append("${target.protein} g di proteine") }
                append(".")
            },
            style = Small.copy(fontSize = 15.sp), color = p.muted, modifier = Modifier.padding(bottom = 12.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(1 to "In linea", 2 to "Quasi", 3 to "Fuori").forEach { (v, label) ->
                val on = e.food == v
                Row(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(p.surface)
                        .border(if (on) 2.dp else 1.dp, if (on) p.ink else p.line, RoundedCornerShape(12.dp))
                        .clickable { vm.updateDay(key) { it.copy(food = if (it.food == v) null else v) } }
                        .padding(vertical = 12.dp, horizontal = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Dot(foodColor(v), 11.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(label, style = Body.copy(fontWeight = FontWeight.SemiBold), maxLines = 1)
                }
            }
        }
        if (data.program.habits.isNotEmpty()) {
            ChipRow {
                data.program.habits.forEach { h ->
                    Chip(h.label, h.id in e.habits) {
                        vm.updateDay(key) { d -> d.copy(habits = if (h.id in d.habits) d.habits - h.id else d.habits + h.id) }
                    }
                }
            }
        }

        // Misure
        H2("Misure")
        MeasureField(
            label = "Peso al mattino", unit = "kg", value = e.weight, placeholder = "70,0", fieldKey = "$key/weight",
            help = "A digiuno, dopo il bagno. Conta la media della settimana, non il singolo giorno.",
        ) { v -> vm.updateDay(key) { it.copy(weight = v) } }
        if (date.dayOfWeek.value == data.program.measureWeekday) {
            Spacer(Modifier.size(8.dp))
            MeasureField(
                label = if (sel < 0) "Circonferenza vita" else "Circonferenza vita (settimana ${w + 1})",
                unit = "cm", value = e.waist, placeholder = "76,0",
                fieldKey = "$key/waist", help = "All’altezza dell’ombelico, pancia rilassata, dopo il bagno.",
            ) { v -> vm.updateDay(key) { it.copy(waist = v) } }
        }

        H2("Note")
        SmallField(
            value = e.note,
            onValueChange = { v -> vm.updateDay(key) { it.copy(note = v) } },
            modifier = Modifier.fillMaxWidth(),
            placeholder = "Sensazioni, sonno, cosa hai mangiato fuori programma…",
            style = Body,
            center = false,
            singleLine = false,
            minLines = 3,
        )
    }
}

@Composable
private fun MeasureField(
    label: String, unit: String, value: Double?, placeholder: String, fieldKey: String, help: String,
    onValue: (Double?) -> Unit,
) {
    CardBox {
        Text(label, style = Body.copy(fontWeight = FontWeight.SemiBold), modifier = Modifier.padding(bottom = 6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            NumberInput(value, onValue, key = fieldKey, modifier = Modifier.width(130.dp), placeholder = placeholder, style = cond(22))
            Text(unit, style = cond(20), color = Pal.muted, modifier = Modifier.padding(start = 10.dp))
        }
        Muted(help, Modifier.padding(top = 8.dp), Tiny)
    }
}

/** Tra due esercizi: pulsante per il recupero di cambio, con l'esercizio che viene dopo. */
@Composable
private fun ChangeRestRow(next: String, seconds: Int, enabled: Boolean, onStart: () -> Unit) {
    val p = Pal
    val shape = RoundedCornerShape(999.dp)
    Row(
        Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f).height(1.dp).background(p.line))
        Row(
            Modifier
                .padding(horizontal = 8.dp)
                .widthIn(max = 320.dp)
                .clip(shape)
                .background(p.surface2)
                .border(1.dp, p.line, shape)
                .clickable(enabled = enabled, onClick = onStart)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (seconds > 0) "⏱  Cambio esercizio ${fmtClock(seconds * 1000L)}" else "Cambio esercizio",
                style = Small.copy(fontWeight = FontWeight.SemiBold),
                color = if (enabled) p.ink else p.muted,
                maxLines = 1,
            )
        }
        Box(Modifier.weight(1f).height(1.dp).background(p.line))
    }
    Text(
        "Poi: $next",
        style = Tiny, color = p.muted, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis,
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
    )
}

private fun setAt(list: List<SetEntry>, i: Int, v: SetEntry): List<SetEntry> {
    val m = list.toMutableList()
    while (m.size <= i) m += SetEntry()
    m[i] = v
    return m
}

@Composable
private fun ExerciseLogCard(
    n: Int,
    r: ResolvedExercise,
    entries: List<SetEntry>,
    prev: PrevSets?,
    fieldKey: String,
    /** Fa partire il timer di recupero; null se non si può (circuito a intervalli in corso). */
    onRest: ((Int) -> Unit)?,
    onChange: (List<SetEntry>) -> Unit,
) {
    val p = Pal
    CardBox(Modifier.padding(bottom = 10.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                Modifier.padding(top = 1.dp).size(22.dp).background(p.surface2, CircleShape).border(1.dp, p.line, CircleShape),
                contentAlignment = Alignment.Center,
            ) { Text("$n", style = Tiny.copy(fontWeight = FontWeight.SemiBold), color = p.muted) }
            Spacer(Modifier.width(8.dp))
            H3(r.name, Modifier.weight(1f))
        }
        Text(
            buildAnnotatedString {
                append(r.text)
                if (r.rest.isNotBlank()) {
                    withStyle(SpanStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 14.sp, color = p.muted)) {
                        append("   recupero ${r.rest}")
                    }
                }
            },
            style = cond(19),
            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
        )
        if (r.cue.isNotBlank()) Muted(r.cue, Modifier.padding(top = 4.dp, bottom = 6.dp))

        val rows = max(r.sets, entries.size)
        for (i in 0 until rows) {
            val cur = entries.getOrNull(i) ?: SetEntry()
            val pv = prev?.sets?.getOrNull(i)
            Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${i + 1}", style = cond(17), color = p.muted, modifier = Modifier.width(24.dp))
                NumberInput(
                    cur.kg, { v -> onChange(setAt(entries, i, cur.copy(kg = v))) },
                    key = "$fieldKey/$i/kg", modifier = Modifier.weight(1f),
                    placeholder = pv?.kg?.let { fmtNum(it) } ?: "kg",
                )
                Text("kg  ×", style = Small, color = p.muted, modifier = Modifier.padding(horizontal = 8.dp))
                NumberInput(
                    cur.reps?.toDouble(), { v -> onChange(setAt(entries, i, cur.copy(reps = v?.toInt()))) },
                    key = "$fieldKey/$i/reps", modifier = Modifier.weight(1f), decimals = false,
                    placeholder = pv?.reps?.toString() ?: "${r.repMin}–${r.repMax}",
                )
                Text("rip", style = Small, color = p.muted, modifier = Modifier.padding(start = 8.dp))
            }
        }
        Row(Modifier.padding(top = 2.dp)) {
            LinkBtn("+ serie") {
                val padded = entries.toMutableList()
                while (padded.size < rows) padded += SetEntry()
                onChange(padded + SetEntry())
            }
            if (entries.size > r.sets) LinkBtn("− serie") { onChange(entries.dropLast(1)) }
            if (prev != null && entries.all { it.isEmpty }) LinkBtn("Copia l’ultima volta") { onChange(prev.sets) }
        }
        // Recupero tra le serie: parte il timer classico con il tempo della scheda
        val restSec = parseRestSeconds(r.rest)
        if (restSec != null && onRest != null) {
            Btn("⏱  Avvia recupero ${fmtClock(restSec * 1000L)}", Modifier.padding(top = 6.dp, bottom = 4.dp).fillMaxWidth()) {
                onRest(restSec)
            }
        }
        Text(
            buildAnnotatedString {
                if (prev != null) {
                    append("Ultima volta (${It.shortDate(prev.date)}): ")
                    withStyle(SpanStyle(color = p.ink, fontWeight = FontWeight.SemiBold)) { append(Tracker.formatSets(prev.sets)) }
                } else {
                    append("Prima volta: scegli un carico che rispetti il RIR della settimana.")
                }
            },
            style = Tiny, color = p.muted, modifier = Modifier.padding(top = 2.dp),
        )
        prev?.let { Tracker.progressionHint(it.sets, r) }?.let { hint ->
            Text(
                hint, style = Tiny.copy(fontWeight = FontWeight.SemiBold),
                color = if (hint.startsWith("Range")) p.good else p.muted, modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
