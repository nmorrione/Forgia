package io.github.nmorrione.forgia.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.nmorrione.forgia.data.Goal
import io.github.nmorrione.forgia.logic.CycleReport
import io.github.nmorrione.forgia.logic.CycleReports
import io.github.nmorrione.forgia.logic.It
import io.github.nmorrione.forgia.logic.NextCycle
import io.github.nmorrione.forgia.logic.NextCycleOptions
import io.github.nmorrione.forgia.logic.capitalize
import io.github.nmorrione.forgia.logic.fmt1
import io.github.nmorrione.forgia.logic.fmtInt
import io.github.nmorrione.forgia.logic.signed1
import io.github.nmorrione.forgia.ui.AppViewModel
import io.github.nmorrione.forgia.ui.Route
import io.github.nmorrione.forgia.ui.components.Btn
import io.github.nmorrione.forgia.ui.components.CardBox
import io.github.nmorrione.forgia.ui.components.H2
import io.github.nmorrione.forgia.ui.components.Muted
import io.github.nmorrione.forgia.ui.edit.ConfirmDialog
import io.github.nmorrione.forgia.ui.edit.EditorScaffold
import io.github.nmorrione.forgia.ui.edit.MissingItem
import io.github.nmorrione.forgia.ui.edit.Stepper
import io.github.nmorrione.forgia.ui.edit.SwitchRow
import io.github.nmorrione.forgia.ui.edit.TextIn
import io.github.nmorrione.forgia.ui.theme.Body
import io.github.nmorrione.forgia.ui.theme.Pal
import io.github.nmorrione.forgia.ui.theme.Small
import io.github.nmorrione.forgia.ui.theme.Tiny
import io.github.nmorrione.forgia.ui.theme.cond
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.roundToInt

private fun goalDescription(g: Goal) = when (g) {
    Goal.BULK -> "Continua a costruire: un piccolo surplus sopra il mantenimento e carichi che salgono."
    Goal.CUT -> "4–6 settimane sotto il mantenimento per asciugare l’addome, tenendo i carichi."
    Goal.MAINTENANCE -> "Calorie al mantenimento: per un periodo pieno o per consolidare."
}

private fun dateRange(r: CycleReport) =
    "${It.shortDate(r.start)} ${r.start.year} – ${It.shortDate(r.end)} ${r.end.year}"

/* ---------- Riepilogo ---------- */

/** Riepilogo del ciclo in corso ([archiveIndex] null) o di uno dell'archivio. */
@Composable
fun CycleSummaryScreen(vm: AppViewModel, archiveIndex: Int?) {
    val d = vm.data
    val today = remember { LocalDate.now() }
    val rec = archiveIndex?.let { d.cycles.getOrNull(it) ?: return MissingItem(vm) }
    val program = rec?.program ?: d.program
    val report = remember(d.log, program, rec, d.nutrition.train, d.nutrition.rest, d.nutrition.adjustments) {
        if (rec != null) {
            // Fino al giorno in cui è stato chiuso: dopo comincia il ciclo successivo.
            CycleReports.build(rec.program, rec.train, rec.rest, rec.adjustments, d.log, LocalDate.parse(rec.endedOn))
        } else {
            CycleReports.build(program, d.nutrition.train, d.nutrition.rest, d.nutrition.adjustments, d.log, today)
        }
    }

    EditorScaffold(program.name, vm::pop) {
        Muted("${program.goal.label} · ${dateRange(report)}", Modifier.padding(top = 4.dp))
        if (rec == null && !report.finished) {
            Muted(
                if (report.elapsedDays == 0) "Il ciclo non è ancora iniziato: qui vedrai i dati man mano che li segni."
                else "Ciclo in corso: dati dei primi ${report.elapsedDays} giorni.",
                Modifier.padding(top = 2.dp), Small,
            )
        }
        ReportBody(report)

        if (rec == null) {
            H2("Il prossimo ciclo")
            CardBox {
                Text("Consiglio: ${report.recommended.label.lowercase()}", style = cond(20))
                Text(report.reason, style = Small, modifier = Modifier.padding(top = 4.dp))
                Btn("Prepara il prossimo ciclo", Modifier.padding(top = 12.dp).fillMaxWidth(), filled = true) {
                    vm.push(Route.NextCycle)
                }
            }
        }
    }
}

@Composable
private fun ReportBody(r: CycleReport) {
    val p = Pal
    H2("Peso e misure")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Tile(
            r.weightDelta?.let { signed1(it) + " kg" } ?: "–",
            if (r.startWeight != null && r.endWeight != null) "Peso: da ${fmt1(r.startWeight)} a ${fmt1(r.endWeight)} kg" else "Peso: servono più pesate",
            Modifier.weight(1f),
        )
        Tile(
            r.weeklyRate?.let { signed1(it) } ?: "–",
            "kg a settimana",
            Modifier.weight(1f),
        )
    }
    Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Tile(
            r.waistDelta?.let { signed1(it) + " cm" } ?: "–",
            if (r.waistStart != null && r.waistEnd != null) "Vita: da ${fmt1(r.waistStart)} a ${fmt1(r.waistEnd)} cm" else "Vita: servono due misure",
            Modifier.weight(1f),
        )
        Tile("${r.done}/${r.planned}", "Sedute fatte, ${r.foodOk} giorni di cibo in linea", Modifier.weight(1f))
    }

    H2("Mantenimento stimato")
    CardBox {
        if (r.maintenance != null) {
            Text("≈ ${fmtInt(r.maintenance.roundToInt())} kcal al giorno", style = cond(26, FontWeight.Bold))
            Muted(
                "Il piano prevedeva in media ${fmtInt(r.avgKcal.roundToInt())} kcal al giorno e il peso è cambiato di ${signed1(r.weeklyRate ?: 0.0)} kg a settimana. " +
                    "Ogni kg corrisponde a circa 7.700 kcal: la differenza dice quanto bruci davvero. È una stima, più precisa se ti pesi spesso e segui il piano.",
                Modifier.padding(top = 6.dp), Tiny,
            )
        } else {
            Text("Non ancora calcolabile", style = cond(20))
            Muted(
                "Servono almeno 3 settimane con il peso segnato (ora ${r.weeksWithWeight}). Calorie medie del piano finora: ${fmtInt(r.avgKcal.roundToInt())} al giorno.",
                Modifier.padding(top = 4.dp), Tiny,
            )
        }
    }

    H2("Carichi")
    if (r.exercises.isEmpty()) {
        Muted("Nessun carico segnato in questo ciclo.", style = Body)
    } else {
        CardBox(padding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)) {
            r.exercises.forEachIndexed { i, ex ->
                if (i > 0) HorizontalDivider(color = p.line)
                Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(ex.name, style = Body.copy(fontWeight = FontWeight.SemiBold))
                        Text(
                            if (ex.firstText == ex.bestText) ex.bestText else "${ex.firstText} → ${ex.bestText}",
                            style = Small, color = p.muted,
                        )
                    }
                    val pct = ex.pct
                    Text(
                        if (pct.roundToInt() == 0) "=" else "${if (pct > 0) "+" else "−"}${kotlin.math.abs(pct).roundToInt()}%",
                        style = cond(20),
                        color = if (pct > 0.5) p.good else if (pct < -0.5) p.bad else p.muted,
                    )
                }
            }
        }
        Muted("La percentuale è la forza stimata (formula di Epley) della serie migliore, rispetto alla prima seduta del ciclo.", Modifier.padding(top = 6.dp), Tiny)
    }
}

@Composable
private fun Tile(value: String, label: String, modifier: Modifier) {
    CardBox(modifier, padding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)) {
        Text(value, style = cond(28, FontWeight.Bold, 1.0))
        Text(label, style = Tiny, color = Pal.muted)
    }
}

/* ---------- Prossimo ciclo ---------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NextCycleScreen(vm: AppViewModel) {
    val d = vm.data
    val p = Pal
    val today = remember { LocalDate.now() }
    val report = remember(d.log, d.program, d.nutrition) {
        CycleReports.build(d.program, d.nutrition.train, d.nutrition.rest, d.nutrition.adjustments, d.log, today)
    }
    var goal by remember { mutableStateOf(report.recommended) }
    var name by remember { mutableStateOf(NextCycle.defaultName(report.recommended, d.program, d.cycles.size)) }
    var nameEdited by remember { mutableStateOf(false) }
    var weeks by remember { mutableStateOf(NextCycle.defaultWeeks(report.recommended)) }
    var start by remember { mutableStateOf(NextCycle.defaultStart(d.program, today)) }
    var deloadFirst by remember { mutableStateOf(NextCycle.defaultDeloadFirst(d.program)) }
    var showDate by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf<Boolean?>(null) } // true = poi apri la scheda

    val weight = report.endWeight ?: d.nutrition.bodyWeightKg
    val (train, rest) = NextCycle.targets(goal, d.nutrition, d.program, report.maintenance, weight)
    val plan = NextCycle.plan(goal, weeks, deloadFirst, d.program.blocks.size)

    EditorScaffold("Prossimo ciclo", vm::pop) {
        Muted(report.reason, Modifier.padding(top = 4.dp), Small)

        H2("Obiettivo", top = 20.dp)
        Goal.entries.forEach { g ->
            val on = g == goal
            Column(
                Modifier
                    .padding(bottom = 8.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(p.surface)
                    .border(if (on) 2.dp else 1.dp, if (on) p.ink else p.line, RoundedCornerShape(14.dp))
                    .clickable {
                        goal = g
                        weeks = NextCycle.defaultWeeks(g)
                        if (!nameEdited) name = NextCycle.defaultName(g, d.program, d.cycles.size)
                    }
                    .padding(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(g.label, style = cond(21), modifier = Modifier.weight(1f))
                    if (g == report.recommended) Text("Consigliato", style = Tiny.copy(fontWeight = FontWeight.SemiBold), color = p.good)
                }
                Text(goalDescription(g), style = Small, color = p.muted)
            }
        }

        H2("Date", top = 20.dp)
        TextIn("Nome del ciclo", name, { name = it; nameEdited = true })
        CardBox(Modifier.padding(top = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Inizio", style = Body.copy(fontWeight = FontWeight.SemiBold))
                    Muted("${capitalize(It.dayTitle(start))} ${start.year}")
                }
                Btn("Cambia") { showDate = true }
            }
            Stepper("Settimane", weeks, 1, 52) { weeks = it }
            SwitchRow(
                "Settimana di scarico all’inizio", deloadFirst,
                help = "Consigliata dopo un ciclo che finisce con le settimane più dure.",
            ) { deloadFirst = it }
            Muted(
                "Fine: ${It.dayTitle(start.plusDays(weeks * 7L - 1))} ${start.plusDays(weeks * 7L - 1).year}. " +
                    plan.mapIndexed { i, ph -> "${i + 1}: " + if (ph.deload) "scarico" else "RIR ${ph.rir}" }.joinToString(" · "),
                Modifier.padding(top = 4.dp), Tiny,
            )
        }

        H2("Alimentazione", top = 20.dp)
        CardBox {
            Text("Allenamento: ${fmtInt(train.kcal)} kcal, ${train.protein} g di proteine", style = Body.copy(fontWeight = FontWeight.SemiBold))
            Text("Riposo: ${fmtInt(rest.kcal)} kcal, ${rest.protein} g di proteine", style = Body.copy(fontWeight = FontWeight.SemiBold))
            Muted(
                (report.maintenance?.let { "Dal mantenimento stimato di circa ${fmtInt(it.roundToInt())} kcal. " }
                    ?: "Il mantenimento non si è potuto stimare: si parte dagli obiettivi attuali. ") +
                    "Peso di riferimento ${fmt1(weight)} kg. Le quantità dei pasti si ricalcolano; tutto resta modificabile da Cibo.",
                Modifier.padding(top = 6.dp), Tiny,
            )
        }

        Muted(
            "Il ciclo attuale va nell’archivio con il suo riepilogo. Diario, carichi, pasti e alimenti restano: lo storico «ultima volta» continua.",
            Modifier.padding(top = 14.dp), Small,
        )
        Btn("Avvia il nuovo ciclo", Modifier.padding(top = 12.dp).fillMaxWidth(), filled = true) { confirm = false }
        Btn("Avvia e modifica la scheda", Modifier.padding(top = 8.dp).fillMaxWidth()) { confirm = true }
    }

    if (showDate) {
        val state = rememberDatePickerState(initialSelectedDateMillis = start.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { start = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                    showDate = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text("Annulla") } },
        ) { DatePicker(state) }
    }

    confirm?.let { thenEdit ->
        ConfirmDialog(
            "Avviare «${name.ifBlank { goal.label }}»?",
            "Parte ${It.dayTitle(start)} ${start.year}, per $weeks settimane. «${d.program.name}» va nell’archivio.",
            "Avvia",
            onConfirm = {
                vm.update { NextCycle.apply(it, NextCycleOptions(goal, name, start, weeks, deloadFirst), report, today) }
                vm.stack.clear()
                vm.selectedDay = null
                if (thenEdit) vm.push(Route.ProgramEdit)
            },
            onDismiss = { confirm = null },
        )
    }
}
