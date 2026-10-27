package io.github.nmorrione.forgia.ui.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.nmorrione.forgia.data.Exercise
import io.github.nmorrione.forgia.data.Goal
import io.github.nmorrione.forgia.logic.NextCycle
import io.github.nmorrione.forgia.data.Habit
import io.github.nmorrione.forgia.data.Prescription
import io.github.nmorrione.forgia.data.Program
import io.github.nmorrione.forgia.data.SessionType
import io.github.nmorrione.forgia.data.Tip
import io.github.nmorrione.forgia.data.WeekPhase
import io.github.nmorrione.forgia.logic.It
import io.github.nmorrione.forgia.logic.Schedule
import io.github.nmorrione.forgia.logic.capitalize
import io.github.nmorrione.forgia.ui.AppViewModel
import io.github.nmorrione.forgia.ui.Route
import io.github.nmorrione.forgia.ui.components.Btn
import io.github.nmorrione.forgia.ui.components.CardBox
import io.github.nmorrione.forgia.ui.components.Dot
import io.github.nmorrione.forgia.ui.components.LinkBtn
import io.github.nmorrione.forgia.ui.components.Muted
import io.github.nmorrione.forgia.ui.theme.Body
import io.github.nmorrione.forgia.ui.theme.Pal
import io.github.nmorrione.forgia.ui.theme.SessionColors
import io.github.nmorrione.forgia.ui.theme.Tiny
import io.github.nmorrione.forgia.ui.theme.cond
import io.github.nmorrione.forgia.ui.theme.sessionColor
import java.time.Instant
import java.time.ZoneOffset
import kotlin.math.roundToInt

/* ---------- Programma ---------- */

private fun resizeWeeks(p: Program, n: Int): Program {
    val plan = p.weekPlan.toMutableList()
    while (plan.size < n) plan += (plan.lastOrNull { !it.deload } ?: WeekPhase()).copy(note = "")
    // Le settimane in più restano salvate: se allunghi di nuovo il programma ritornano.
    return p.copy(weeks = n, weekPlan = plan)
}

private fun addBlock(p: Program): Program = p.copy(
    blocks = p.blocks + "Blocco ${p.blocks.size + 1}",
    sessions = p.sessions.map { s ->
        s.copy(exercises = s.exercises.map { e ->
            val base = e.blocks.getOrNull(p.blocks.size) ?: e.blocks.lastOrNull() ?: Prescription()
            val list = e.blocks.take(p.blocks.size).toMutableList()
            while (list.size < p.blocks.size) list += base
            e.copy(blocks = list + base)
        })
    },
)

private fun removeBlock(p: Program, b: Int): Program = p.copy(
    blocks = p.blocks.without(b),
    weekPlan = p.weekPlan.map { w ->
        when {
            w.block == b -> w.copy(block = (b - 1).coerceAtLeast(0))
            w.block > b -> w.copy(block = w.block - 1)
            else -> w
        }
    },
    sessions = p.sessions.map { s ->
        s.copy(exercises = s.exercises.map { e -> if (e.blocks.size > b && e.blocks.size > 1) e.copy(blocks = e.blocks.without(b)) else e })
    },
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgramEditor(vm: AppViewModel) {
    val prog = vm.data.program
    val s = remember(prog) { Schedule(prog) }
    var showDate by remember { mutableStateOf(false) }
    val p = Pal

    fun setPhase(w: Int, ph: WeekPhase) = vm.updateProgram { pr ->
        val plan = pr.weekPlan.toMutableList()
        while (plan.size <= w) plan += plan.lastOrNull() ?: WeekPhase()
        plan[w] = ph
        pr.copy(weekPlan = plan)
    }

    EditorScaffold("Programma", vm::pop) {
        TextIn("Nome del programma", prog.name, { v -> vm.updateProgram { it.copy(name = v) } })
        Picker(
            "Obiettivo del ciclo", Goal.entries.map { it to it.label }, prog.goal,
            onSelect = { g ->
                // Con l'obiettivo cambia anche il ritmo del peso previsto per le correzioni delle calorie.
                vm.update { d ->
                    d.copy(
                        program = d.program.copy(goal = g),
                        nutrition = d.nutrition.copy(adjust = NextCycle.rule(g, d.nutrition.adjust)),
                    )
                }
            },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 10.dp)) {
            Btn("Riepilogo del ciclo", Modifier.weight(1f)) { vm.push(Route.CycleSummary) }
            Btn("Prossimo ciclo", Modifier.weight(1f)) { vm.push(Route.NextCycle) }
        }

        EditorSection("Date")
        CardBox {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Data di inizio", style = Body.copy(fontWeight = FontWeight.SemiBold))
                    Muted("${capitalize(It.dayTitle(s.start))} ${s.start.year}")
                }
                Btn("Cambia") { showDate = true }
            }
            Stepper("Durata in settimane", prog.weeks, 1, 52) { n -> vm.updateProgram { resizeWeeks(it, n) } }
            Muted("Fine: ${It.dayTitle(s.end)} ${s.end.year}.", style = Tiny)
            Muted(
                "Il diario resta legato ai giorni del calendario: se sposti l’inizio o cambi la durata, quello che hai già segnato non si perde.",
                Modifier.padding(top = 6.dp), Tiny,
            )
        }

        EditorSection("Settimana tipo")
        val sessionOptions = prog.sessions.map { it.id to it.name }
        for (d in 0..6) {
            Picker(capitalize(It.weekdays[d]), sessionOptions, prog.weeklyTemplate.getOrElse(d) { "" }, onSelect = { id ->
                vm.updateProgram { pr ->
                    val t = pr.weeklyTemplate.toMutableList()
                    while (t.size < 7) t += pr.sessions.firstOrNull { it.rest }?.id ?: pr.sessions.first().id
                    t[d] = id
                    pr.copy(weeklyTemplate = t)
                }
            })
        }
        Picker("Giorno della misura della vita", (1..7).map { it to capitalize(It.weekday(it)) }, prog.measureWeekday, onSelect = { v ->
            vm.updateProgram { it.copy(measureWeekday = v) }
        })

        EditorSection("Sessioni")
        prog.sessions.forEach { st ->
            key(st.id) {
                CardBox(Modifier.padding(bottom = 8.dp), onClick = { vm.push(Route.SessionEdit(st.id)) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Dot(sessionColor(st.color), 12.dp)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(st.name, style = cond(19))
                            Muted(if (st.rest) "Riposo" else "${st.exercises.size} esercizi", style = Tiny)
                        }
                        Text("›", style = cond(24), color = p.muted)
                    }
                }
            }
        }
        LinkBtn("+ Nuova sessione") {
            val id = newId("s")
            vm.updateProgram { it.copy(sessions = it.sessions + SessionType(id, "Nuova sessione", "N", color = 5)) }
            vm.push(Route.SessionEdit(id))
        }

        EditorSection("Blocchi")
        prog.blocks.forEachIndexed { b, name ->
            key(b) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextIn("Blocco ${b + 1}", name, { v -> vm.updateProgram { it.copy(blocks = it.blocks.replaceAt(b, v)) } }, Modifier.weight(1f))
                    if (prog.blocks.size > 1) LinkBtn("Elimina", color = p.bad) { vm.updateProgram { removeBlock(it, b) } }
                }
            }
        }
        LinkBtn("+ Aggiungi blocco") { vm.updateProgram { addBlock(it) } }
        Muted("Ogni esercizio ha una prescrizione per blocco: la imposti aprendo l’esercizio.", style = Tiny)

        EditorSection("Settimana per settimana")
        for (w in 0 until prog.weeks.coerceIn(1, 52)) {
            val ph = prog.weekPlan.getOrNull(w) ?: s.phase(w)
            key(w) {
                CardBox(Modifier.padding(bottom = 8.dp)) {
                    Text("Settimana ${w + 1} · ${s.weekRange(w)}", style = cond(18))
                    SwitchRow("Settimana di scarico", ph.deload) { v -> setPhase(w, ph.copy(deload = v)) }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Picker(
                            if (ph.deload) "Blocco di riferimento" else "Blocco",
                            prog.blocks.mapIndexed { i, n -> i to n },
                            ph.block.coerceIn(0, prog.blocks.size - 1),
                            { v -> setPhase(w, ph.copy(block = v)) },
                            Modifier.weight(1f),
                        )
                        if (!ph.deload) TextIn("RIR", ph.rir, { v -> setPhase(w, ph.copy(rir = v)) }, Modifier.width(96.dp))
                    }
                    TextIn("Nota", ph.note, { v -> setPhase(w, ph.copy(note = v)) }, singleLine = false)
                }
            }
        }

        EditorSection("Scarico")
        IntIn(
            "Serie da tenere (%)", (prog.deload.setsFactor * 100).roundToInt(),
            { v -> vm.updateProgram { it.copy(deload = it.deload.copy(setsFactor = v.coerceIn(10, 100) / 100.0)) } },
            key = "deload-sets",
        )
        IntIn(
            "Variazione del carico (%)", prog.deload.loadChangePct,
            { v -> vm.updateProgram { it.copy(deload = it.deload.copy(loadChangePct = v)) } },
            key = "deload-load", signed = true,
        )
        TextIn("RIR dello scarico", prog.deload.rir, { v -> vm.updateProgram { it.copy(deload = it.deload.copy(rir = v)) } })

        EditorSection("Riscaldamento")
        TextIn("Testo sopra gli esercizi", prog.warmup, { v -> vm.updateProgram { it.copy(warmup = v) } }, singleLine = false, minLines = 2)

        EditorSection("Abitudini da spuntare")
        prog.habits.forEachIndexed { i, h ->
            key(h.id) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextIn("Abitudine ${i + 1}", h.label, { v -> vm.updateProgram { it.copy(habits = it.habits.replaceAt(i, h.copy(label = v))) } }, Modifier.weight(1f))
                    LinkBtn("Elimina", color = p.bad) { vm.updateProgram { it.copy(habits = it.habits.without(i)) } }
                }
            }
        }
        LinkBtn("+ Aggiungi abitudine") { vm.updateProgram { it.copy(habits = it.habits + Habit(newId("h"), "Nuova abitudine")) } }
    }

    if (showDate) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = s.start.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { ms ->
                        val d = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()
                        vm.updateProgram { it.copy(startDate = d.toString()) }
                    }
                    showDate = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text("Annulla") } },
        ) { DatePicker(state) }
    }
}

/* ---------- Sessione ---------- */

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SessionEditor(vm: AppViewModel, id: String) {
    val prog = vm.data.program
    val st = prog.session(id) ?: return MissingItem(vm)
    val s = remember(prog) { Schedule(prog) }
    val p = Pal
    var confirmDelete by remember { mutableStateOf(false) }

    fun upd(f: (SessionType) -> SessionType) =
        vm.updateProgram { pr -> pr.copy(sessions = pr.sessions.map { if (it.id == id) f(it) else it }) }

    EditorScaffold(st.name.ifBlank { "Sessione" }, vm::pop) {
        TextIn("Nome", st.name, { v -> upd { it.copy(name = v) } })
        TextIn("Sigla nel calendario", st.short, { v -> upd { it.copy(short = v.take(3)) } })
        TextIn("Descrizione", st.description, { v -> upd { it.copy(description = v) } }, singleLine = false)
        TextIn("Durata indicativa", st.duration, { v -> upd { it.copy(duration = v) } })
        SwitchRow(
            "Giorno di riposo", st.rest,
            help = "Niente spunta «fatto», obiettivi alimentari da giorno di riposo.",
        ) { v -> upd { it.copy(rest = v) } }
        Text("Colore", style = Body.copy(fontWeight = FontWeight.SemiBold), modifier = Modifier.padding(top = 10.dp, bottom = 8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SessionColors.indices.forEach { i ->
                Box(
                    Modifier
                        .size(36.dp)
                        .background(sessionColor(i), CircleShape)
                        .border(if (i == st.color) 3.dp else 0.dp, p.ink, CircleShape)
                        .clickable { upd { it.copy(color = i) } },
                )
            }
        }

        EditorSection("Esercizi")
        st.exercises.forEachIndexed { i, ex ->
            key(ex.id) {
                CardBox(Modifier.padding(bottom = 8.dp), onClick = { vm.push(Route.ExerciseEdit(id, ex.id)) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("${i + 1}. ${ex.name}", style = cond(18))
                            Muted(prog.blocks.indices.joinToString(" · ") { b -> Schedule.prescText(s.prescriptionFor(ex, b)) }, style = Tiny)
                        }
                        if (i > 0) LinkBtn("↑") { upd { it.copy(exercises = it.exercises.move(i, -1)) } }
                        if (i < st.exercises.lastIndex) LinkBtn("↓") { upd { it.copy(exercises = it.exercises.move(i, 1)) } }
                    }
                }
            }
        }
        LinkBtn("+ Aggiungi esercizio") {
            val eid = newId("e")
            upd { it.copy(exercises = it.exercises + Exercise(eid, "Nuovo esercizio", blocks = prog.blocks.map { Prescription() })) }
            vm.push(Route.ExerciseEdit(id, eid))
        }

        EditorSection("Consigli nella vista Oggi")
        TextIn("Titolo della sezione", st.tipsTitle, { v -> upd { it.copy(tipsTitle = v) } })
        st.tips.forEachIndexed { i, tip ->
            key(i) {
                CardBox(Modifier.padding(top = 8.dp)) {
                    TextIn("Consiglio ${i + 1}", tip.text, { v -> upd { it.copy(tips = it.tips.replaceAt(i, tip.copy(text = v))) } }, singleLine = false)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Picker(
                            "Quando",
                            listOf<Pair<Int?, String>>(null to "Ogni volta") + (1..7).map { Pair<Int?, String>(it, "Solo di ${It.weekday(it)}") },
                            tip.weekday,
                            { v -> upd { it.copy(tips = it.tips.replaceAt(i, tip.copy(weekday = v))) } },
                            Modifier.weight(1f),
                        )
                        LinkBtn("Elimina", color = p.bad) { upd { it.copy(tips = it.tips.without(i)) } }
                    }
                }
            }
        }
        LinkBtn("+ Aggiungi consiglio") { upd { it.copy(tips = it.tips + Tip("")) } }
        Muted("Scrivi {domani} per inserire il nome della sessione del giorno dopo.", style = Tiny)

        EditorSection("Note nella scheda")
        TextIn(
            "Una nota per riga", st.planNotes.joinToString("\n"), { v -> upd { it.copy(planNotes = v.lines()) } },
            singleLine = false, minLines = 3, help = "Compaiono nella scheda quando la sessione non ha esercizi.",
        )

        EditorSection("Elimina")
        if (prog.sessions.size > 1) {
            Btn("Elimina la sessione", color = p.bad) { confirmDelete = true }
        } else {
            Muted("È l’unica sessione: non si può eliminare.")
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            "Eliminare «${st.name}»?",
            "Nella settimana tipo verrà sostituita da un giorno di riposo. Il diario non cambia.",
            "Elimina",
            onConfirm = {
                vm.pop()
                vm.updateProgram { pr ->
                    val others = pr.sessions.filter { it.id != id }
                    val repl = others.firstOrNull { it.rest }?.id ?: others.first().id
                    pr.copy(sessions = others, weeklyTemplate = pr.weeklyTemplate.map { if (it == id) repl else it })
                }
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

/* ---------- Esercizio ---------- */

@Composable
fun ExerciseEditor(vm: AppViewModel, sessionId: String, exerciseId: String) {
    val prog = vm.data.program
    val ex = prog.session(sessionId)?.exercises?.firstOrNull { it.id == exerciseId } ?: return MissingItem(vm)
    val p = Pal
    var confirmDelete by remember { mutableStateOf(false) }

    fun upd(f: (Exercise) -> Exercise) = vm.updateProgram { pr ->
        pr.copy(sessions = pr.sessions.map { s ->
            if (s.id != sessionId) s else s.copy(exercises = s.exercises.map { if (it.id == exerciseId) f(it) else it })
        })
    }

    EditorScaffold(ex.name.ifBlank { "Esercizio" }, vm::pop) {
        TextIn("Nome", ex.name, { v -> upd { it.copy(name = v) } })
        TextIn("Esecuzione e consigli", ex.cue, { v -> upd { it.copy(cue = v) } }, singleLine = false, minLines = 2)
        TextIn("Recupero", ex.rest, { v -> upd { it.copy(rest = v) } })
        TextIn(
            "Di quanto aumentare", ex.increment, { v -> upd { it.copy(increment = v) } },
            help = "Compare quando chiudi il range di ripetizioni in tutte le serie.",
        )

        prog.blocks.forEachIndexed { b, bn ->
            val pr = ex.blocks.getOrNull(b) ?: ex.blocks.lastOrNull() ?: Prescription()
            fun setP(np: Prescription) = upd { e ->
                val list = e.blocks.toMutableList()
                while (list.size <= b) list += list.lastOrNull() ?: Prescription()
                list[b] = np
                e.copy(blocks = list)
            }
            EditorSection(bn)
            CardBox {
                Stepper("Serie", pr.sets, 1, 15) { setP(pr.copy(sets = it)) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IntIn("Ripetizioni min", pr.repMin, { setP(pr.copy(repMin = it.coerceAtLeast(1))) }, key = "$exerciseId/$b/min", modifier = Modifier.weight(1f))
                    IntIn("Ripetizioni max", pr.repMax, { setP(pr.copy(repMax = it.coerceAtLeast(1))) }, key = "$exerciseId/$b/max", modifier = Modifier.weight(1f))
                }
                TextIn("Dopo le ripetizioni (es. per lato)", pr.suffix, { setP(pr.copy(suffix = it)) })
                TextIn(
                    "Variante in questo blocco (facoltativa)", pr.variant ?: "", { v -> setP(pr.copy(variant = v.ifBlank { null })) },
                    help = "Un nome diverso ha uno storico dei carichi separato.",
                )
                if (pr.variant != null) {
                    TextIn("Consigli per la variante", pr.variantCue ?: "", { v -> setP(pr.copy(variantCue = v.ifBlank { null })) }, singleLine = false)
                }
                Muted("Anteprima: ${Schedule.prescText(pr)}", Modifier.padding(top = 8.dp), Tiny)
            }
        }

        EditorSection("Elimina")
        Btn("Elimina l’esercizio", color = p.bad) { confirmDelete = true }
    }

    if (confirmDelete) {
        ConfirmDialog(
            "Eliminare «${ex.name}»?",
            "Lo storico dei carichi già segnati resta nel diario.",
            "Elimina",
            onConfirm = {
                vm.pop()
                vm.updateProgram { pr ->
                    pr.copy(sessions = pr.sessions.map { s ->
                        if (s.id != sessionId) s else s.copy(exercises = s.exercises.filter { it.id != exerciseId })
                    })
                }
            },
            onDismiss = { confirmDelete = false },
        )
    }
}
