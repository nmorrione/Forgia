package io.github.nmorrione.forgia.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.nmorrione.forgia.data.DayLog
import io.github.nmorrione.forgia.data.SessionType
import io.github.nmorrione.forgia.logic.Advice
import io.github.nmorrione.forgia.logic.It
import io.github.nmorrione.forgia.logic.NutritionCalc
import io.github.nmorrione.forgia.logic.Schedule
import io.github.nmorrione.forgia.logic.Tracker
import io.github.nmorrione.forgia.logic.fmt1
import io.github.nmorrione.forgia.logic.signed1
import io.github.nmorrione.forgia.logic.signed2
import io.github.nmorrione.forgia.ui.AppViewModel
import io.github.nmorrione.forgia.ui.Route
import io.github.nmorrione.forgia.ui.Tab
import io.github.nmorrione.forgia.ui.components.LinkBtn
import io.github.nmorrione.forgia.ui.components.Btn
import io.github.nmorrione.forgia.ui.components.CardBox
import io.github.nmorrione.forgia.ui.components.Dot
import io.github.nmorrione.forgia.ui.components.H2
import io.github.nmorrione.forgia.ui.components.Muted
import io.github.nmorrione.forgia.ui.components.Note
import io.github.nmorrione.forgia.ui.components.NumberInput
import io.github.nmorrione.forgia.ui.components.ScreenColumn
import io.github.nmorrione.forgia.ui.components.foodColor
import io.github.nmorrione.forgia.ui.theme.Pal
import io.github.nmorrione.forgia.ui.theme.Small
import io.github.nmorrione.forgia.ui.theme.Tiny
import io.github.nmorrione.forgia.ui.theme.cond
import io.github.nmorrione.forgia.ui.theme.sessionColor
import java.time.LocalDate
import kotlin.math.abs

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CalendarScreen(vm: AppViewModel) {
    val data = vm.data
    val s = remember(data.program) { Schedule(data.program) }
    val t = remember(s, data.log) { Tracker(s, data.log) }
    val today = remember { LocalDate.now() }
    val todayIdx = s.indexOf(today)
    val sel = s.clampDay(vm.selectedDay ?: todayIdx)
    val p = Pal

    val st = t.stats()
    val avgs = (0 until s.weeks).map { t.weekAvg(it) }
    val firstW = avgs.indexOfFirst { it != null }
    val lastW = avgs.indexOfLast { it != null }
    val delta = if (firstW >= 0 && lastW > firstW) avgs[lastW]!! - avgs[firstW]!! else null

    ScreenColumn {
        // Fine ciclo: riepilogo e prossimo ciclo
        if (s.isFinished(today)) {
            CardBox(Modifier.padding(top = 12.dp)) {
                Text(if (todayIdx == s.days - 1) "Ultimo giorno del ciclo" else "Ciclo finito", style = cond(20))
                Muted("Guarda com’è andata e prepara il prossimo: massa, definizione o mantenimento.", Modifier.padding(top = 2.dp))
                Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Btn("Riepilogo", Modifier.weight(1f)) { vm.push(Route.CycleSummary) }
                    Btn("Prossimo ciclo", Modifier.weight(1f), filled = true) { vm.push(Route.NextCycle) }
                }
            }
        }
        // Preparazione: quello che hai segnato prima dell'inizio
        val prep = t.prepSummary()
        if (prep != null || todayIdx < 0) {
            CardBox(Modifier.padding(top = 12.dp)) {
                Text("Preparazione", style = cond(20))
                Text(
                    if (todayIdx < 0) "Il programma parte ${It.dayTitle(s.start)}: mancano ${-todayIdx} giorni."
                    else "Prima dell’inizio, dal ${It.shortDate(s.dateAt(s.firstDay(today, data.log)))}.",
                    style = Small, color = p.muted,
                )
                if (prep != null) {
                    FlowRow(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(numLabel("Allenamenti ", "${prep.workouts}"), style = Small)
                        Text(numLabel("Giorni segnati ", "${prep.daysLogged}"), style = Small)
                        prep.avgWeight?.let {
                            Text(numLabel("Peso di partenza ", fmt1(it) + " kg"), style = Small)
                        }
                    }
                    if (prep.avgWeight != null) {
                        Muted(
                            "Media delle ultime ${prep.weighIns} pesate prima dell’inizio: confrontala con le medie delle settimane.",
                            Modifier.padding(top = 4.dp), Tiny,
                        )
                    }
                }
                if (todayIdx < 0) {
                    Btn("Segna oggi", Modifier.padding(top = 10.dp).fillMaxWidth()) {
                        vm.selectedDay = null
                        vm.tab = Tab.OGGI
                    }
                }
            }
        }
        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Stat("${st.done}/${st.planned}", "Sedute fatte", Modifier.weight(1f))
            Stat("${st.foodOk}", "Giorni di cibo in linea", Modifier.weight(1f))
        }
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Stat(
                if (lastW >= 0) fmt1(avgs[lastW]!!) else "–",
                if (lastW >= 0) "Peso medio, settimana ${lastW + 1}" else "Peso medio",
                Modifier.weight(1f),
            )
            Stat(delta?.let(::signed1) ?: "–", "kg dalla prima media", Modifier.weight(1f))
        }

        // Griglia dei giorni
        Row(Modifier.padding(top = 18.dp, bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            It.weekdayInitials.forEach {
                Text(it, style = cond(14), color = p.muted, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
        val lead = s.start.dayOfWeek.value - 1
        val rows = (lead + s.days + 6) / 7
        for (r in 0 until rows) {
            Row(Modifier.padding(bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (c in 0 until 7) {
                    val i = r * 7 + c - lead
                    if (i < 0 || i >= s.days) {
                        Spacer(Modifier.weight(1f))
                    } else {
                        DayCell(
                            date = s.dateAt(i),
                            sess = s.sessionAt(i, data.log),
                            e = t.day(i),
                            showMonth = i == 0 || s.dateAt(i).dayOfMonth == 1,
                            isToday = i == todayIdx,
                            isSel = i == sel,
                            modifier = Modifier.weight(1f),
                        ) {
                            vm.selectedDay = i
                            vm.tab = Tab.OGGI
                        }
                    }
                }
            }
        }
        FlowRow(
            Modifier.padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            data.program.sessions.forEach { ss ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(14.dp, 5.dp).background(sessionColor(ss.color), RoundedCornerShape(2.dp)))
                    Text(ss.name, style = Tiny, color = p.muted, modifier = Modifier.padding(start = 5.dp))
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Dot(p.good, 8.dp)
                Text("cibo in linea", style = Tiny, color = p.muted, modifier = Modifier.padding(start = 5.dp))
            }
        }
        Muted("Tocca un giorno per aprirlo e compilarlo.", Modifier.padding(top = 6.dp), Tiny)

        H2("Andamento del peso")
        CardBox(padding = PaddingValues(start = 10.dp, end = 10.dp, top = 12.dp, bottom = 6.dp)) {
            WeightChart(s, t, avgs)
        }

        // Correzione delle calorie secondo le regole della Guida
        val advice = t.calorieAdvice(today, data.nutrition.adjust, data.nutrition.adjustments)
        if (advice != null) {
            H2("Calorie")
            CardBox {
                Text(advice.text, style = Small)
                if (advice.kind == Advice.UP || advice.kind == Advice.DOWN) {
                    val d = advice.deltaKcal
                    Btn(
                        "Applica ${if (d > 0) "+" else "−"}${abs(d)} kcal",
                        Modifier.padding(top = 10.dp).fillMaxWidth(),
                        filled = true,
                    ) { vm.updateNutrition { NutritionCalc.applyCalorieChange(it, d, today) } }
                    Muted("Gli obiettivi cambiano per entrambi i tipi di giornata e le quantità dei pasti si ricalcolano: le trovi in Cibo.", Modifier.padding(top = 8.dp), Tiny)
                }
            }
        }

        H2("Settimane")
        for (w in 0 until s.weeks) {
            val (dn, pl) = t.weekSessions(w)
            val a = avgs[w]
            val pa = if (w > 0) avgs[w - 1] else null
            val dd = if (a != null && pa != null) a - pa else null
            val mKey = s.key(s.measureDayOfWeek(w))
            CardBox(Modifier.padding(bottom = 8.dp), padding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Settimana ${w + 1}", style = cond(19))
                        Text("${s.weekRange(w)}, ${s.phaseShort(w)}", style = Tiny, color = p.muted)
                    }
                    NumberInput(
                        data.log.days[mKey]?.waist,
                        { v -> vm.updateDay(mKey) { it.copy(waist = v) } },
                        key = "$mKey/waist",
                        modifier = Modifier.width(76.dp),
                        placeholder = "vita",
                        style = cond(17),
                    )
                    Text("cm", style = Small, color = p.muted, modifier = Modifier.padding(start = 6.dp))
                }
                FlowRow(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(numLabel("Sedute ", "$dn/$pl"), style = Small)
                    Text(numLabel("Media ", a?.let { fmt1(it) + " kg" } ?: "–"), style = Small)
                    if (dd != null) {
                        Text(
                            numLabel("Variazione ", signed1(dd) + " kg", if (abs(dd) <= 0.4) p.good else p.bad),
                            style = Small,
                        )
                    }
                }
            }
        }
        val rule = data.nutrition.adjust
        Note("Ritmo previsto per questo ciclo (${data.program.goal.label.lowercase()}): da ${signed2(rule.minWeeklyGain)} a ${signed2(rule.maxWeeklyGain)} kg a settimana. Qui sopra compare la correzione delle calorie quando serve.")
        if (!s.isFinished(today) && todayIdx >= 0) {
            LinkBtn("Riepilogo del ciclo finora", Modifier.padding(top = 6.dp)) { vm.push(Route.CycleSummary) }
        }

        if (data.cycles.isNotEmpty()) {
            H2("Cicli precedenti")
            data.cycles.asReversed().forEachIndexed { k, rec ->
                val index = data.cycles.lastIndex - k
                val rs = Schedule(rec.program)
                CardBox(Modifier.padding(bottom = 8.dp), onClick = { vm.push(Route.ArchivedCycle(index)) }) {
                    Text(rec.program.name, style = cond(19))
                    Text(
                        "${rec.program.goal.label} · ${It.shortDate(rs.start)} ${rs.start.year} – ${It.shortDate(rs.end)} ${rs.end.year}",
                        style = Tiny, color = p.muted,
                    )
                }
            }
        }
    }
}

@Composable
private fun numLabel(label: String, value: String, color: Color? = null) = buildAnnotatedString {
    append(label)
    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = color ?: Color.Unspecified)) { append(value) }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier = Modifier) {
    CardBox(modifier, padding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)) {
        Text(value, style = cond(30, FontWeight.Bold, 1.0))
        Text(label, style = Tiny, color = Pal.muted)
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    sess: SessionType,
    e: DayLog,
    showMonth: Boolean,
    isToday: Boolean,
    isSel: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val p = Pal
    val shape = RoundedCornerShape(9.dp)
    Box(
        modifier
            .heightIn(min = 68.dp)
            .clip(shape)
            .background(p.surface)
            .border(
                if (isSel) 2.dp else 1.dp,
                when {
                    isSel -> p.accent
                    isToday -> p.ink
                    else -> Color.Transparent
                },
                shape,
            )
            .clickable(onClick = onClick),
    ) {
        Box(Modifier.fillMaxWidth().height(5.dp).background(sessionColor(sess.color)))
        Column(
            Modifier.fillMaxWidth().padding(top = 9.dp, bottom = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("${date.dayOfMonth}", style = cond(17), maxLines = 1)
                if (showMonth) {
                    Text(It.monthsShort[date.monthValue - 1], style = Tiny.copy(fontSize = 10.sp), color = p.muted, maxLines = 1, modifier = Modifier.padding(start = 1.dp))
                }
            }
            Text(sess.short, style = cond(12), color = p.muted, maxLines = 1)
            Row(Modifier.heightIn(min = 12.dp), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                if (e.done) Text("✓", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = p.good)
                e.food?.let { Dot(foodColor(it), 8.dp) }
            }
            e.weight?.let { Text(fmt1(it), fontSize = 10.5.sp, color = p.muted, maxLines = 1) }
        }
    }
}

@Composable
private fun WeightChart(s: Schedule, t: Tracker, avgs: List<Double?>) {
    val p = Pal
    val pts = (0 until s.days).mapNotNull { i -> t.day(i).weight?.let { i to it } }
    if (pts.size < 2) {
        Muted(
            "Segna il peso al mattino nella vista Oggi: qui comparirà l’andamento giorno per giorno e la media settimanale.",
            Modifier.padding(horizontal = 6.dp, vertical = 16.dp),
            Small.copy(fontSize = 15.sp),
        )
        return
    }
    val tm = rememberTextMeasurer()
    val label = Tiny.copy(fontSize = 11.sp, color = p.muted)
    var lo = pts.minOf { it.second }
    var hi = pts.maxOf { it.second }
    if (hi - lo < 1.2) {
        val m = (hi + lo) / 2
        lo = m - 0.6
        hi = m + 0.6
    }
    lo -= 0.2
    hi += 0.2
    val step = if (s.weeks > 12) 2 else 1
    Canvas(Modifier.fillMaxWidth().aspectRatio(340f / 170f)) {
        val pl = 34.dp.toPx()
        val pr = 8.dp.toPx()
        val pt = 10.dp.toPx()
        val pb = 24.dp.toPx()
        val w = size.width
        val h = size.height
        fun x(i: Double) = pl + (i / (s.days - 1)).toFloat() * (w - pl - pr)
        fun y(v: Double) = pt + (1 - ((v - lo) / (hi - lo))).toFloat() * (h - pt - pb)
        for (k in 0..3) {
            val v = lo + (hi - lo) * k / 3
            val yy = y(v)
            drawLine(p.line, Offset(pl, yy), Offset(w - pr, yy), 1.dp.toPx())
            val lay = tm.measure(fmt1(v), label)
            drawText(lay, topLeft = Offset(pl - 5.dp.toPx() - lay.size.width, yy - lay.size.height / 2f))
        }
        for (wk in 0 until s.weeks step step) {
            val lay = tm.measure("S${wk + 1}", label)
            drawText(lay, topLeft = Offset(x(wk * 7 + 3.0) - lay.size.width / 2f, h - lay.size.height - 2.dp.toPx()))
        }
        pts.forEach { (i, v) -> drawCircle(p.muted.copy(alpha = 0.55f), 2.6.dp.toPx(), Offset(x(i.toDouble()), y(v))) }
        val ap = avgs.mapIndexedNotNull { wk, a -> a?.let { Offset(x(wk * 7 + 3.0), y(it)) } }
        if (ap.size > 1) {
            val path = Path().apply {
                moveTo(ap[0].x, ap[0].y)
                ap.drop(1).forEach { lineTo(it.x, it.y) }
            }
            drawPath(path, p.accent, style = Stroke(2.5.dp.toPx(), join = StrokeJoin.Round))
        }
        ap.forEach { drawCircle(p.accent, 3.5.dp.toPx(), it) }
    }
    Muted("Punti grigi: pesate giornaliere. Linea blu: media settimanale.", Modifier.padding(start = 4.dp, end = 4.dp, top = 2.dp, bottom = 6.dp), Tiny.copy(fontSize = 12.sp))
}
