package io.github.nmorrione.forgia.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import io.github.nmorrione.forgia.logic.Schedule
import io.github.nmorrione.forgia.ui.AppViewModel
import io.github.nmorrione.forgia.ui.Tab
import io.github.nmorrione.forgia.ui.Route
import io.github.nmorrione.forgia.ui.SectionKind
import io.github.nmorrione.forgia.ui.components.CardBox
import io.github.nmorrione.forgia.ui.components.ExpandCard
import io.github.nmorrione.forgia.ui.components.H2
import io.github.nmorrione.forgia.ui.components.H3
import io.github.nmorrione.forgia.ui.components.LinkBtn
import io.github.nmorrione.forgia.ui.components.Muted
import io.github.nmorrione.forgia.ui.components.RichText
import io.github.nmorrione.forgia.ui.components.ScreenColumn
import io.github.nmorrione.forgia.ui.components.TipsCard
import io.github.nmorrione.forgia.ui.theme.Body
import io.github.nmorrione.forgia.ui.theme.Condensed
import io.github.nmorrione.forgia.ui.theme.Pal
import io.github.nmorrione.forgia.ui.theme.Small
import io.github.nmorrione.forgia.ui.theme.Tiny
import io.github.nmorrione.forgia.ui.theme.cond
import io.github.nmorrione.forgia.ui.theme.sessionColor

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlansScreen(vm: AppViewModel) {
    val prog = vm.data.program
    val s = remember(prog) { Schedule(prog) }
    val tabs = prog.sessions.filter { !it.rest }
    val cur = tabs.firstOrNull { it.id == vm.planTab } ?: tabs.firstOrNull()
    val p = Pal

    ScreenColumn(scroll = vm.scroll(Tab.SCHEDE)) {
        if (tabs.isNotEmpty()) {
            BoxWithConstraints(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                val gap = 6.dp
                val each = max((maxWidth - gap * (tabs.size - 1)) / tabs.size, 84.dp)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(gap)) {
                    tabs.forEach { st ->
                        val on = st.id == cur?.id
                        val c = sessionColor(st.color)
                        Box(
                            Modifier
                                .width(each)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (on) p.btn else p.surface)
                                .drawBehind {
                                    val bh = 5.dp.toPx()
                                    drawRect(c, topLeft = Offset(0f, size.height - bh), size = Size(size.width, bh))
                                }
                                .clickable { vm.planTab = st.id }
                                .padding(top = 12.dp, bottom = 15.dp, start = 4.dp, end = 4.dp),
                            contentAlignment = Alignment.Center,
                        ) { Text(st.name, style = cond(17), color = if (on) p.btnInk else p.ink, maxLines = 1) }
                    }
                }
            }
        }

        if (cur != null) {
            Column(Modifier.padding(top = 18.dp, bottom = 4.dp)) {
                Text(cur.name, style = cond(44, FontWeight.SemiBold, 0.95))
                val days = s.weekdaysOf(cur.id).ifBlank { "Non è nella settimana tipo" }
                Muted(listOf("$days.", cur.description, cur.duration).filter { it.isNotBlank() }.joinToString(" "), Modifier.padding(top = 6.dp), Body)
            }
            LinkBtn("Modifica la scheda") { vm.push(Route.SessionEdit(cur.id)) }
            if (cur.exercises.isNotEmpty()) {
                cur.exercises.forEachIndexed { idx, ex ->
                    CardBox(Modifier.padding(bottom = 10.dp), onClick = { vm.push(Route.ExerciseEdit(cur.id, ex.id)) }) {
                        Row(verticalAlignment = Alignment.Top) {
                            Box(
                                Modifier.padding(top = 1.dp).size(22.dp).background(p.surface2, CircleShape).border(1.dp, p.line, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) { Text("${idx + 1}", style = Tiny.copy(fontWeight = FontWeight.SemiBold), color = p.muted) }
                            Spacer(Modifier.width(8.dp))
                            H3(ex.name, Modifier.weight(1f))
                        }
                        FlowRow(Modifier.padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            prog.blocks.forEachIndexed { b, bn ->
                                val pr = s.prescriptionFor(ex, b)
                                Text(
                                    buildAnnotatedString {
                                        append("$bn ")
                                        withStyle(SpanStyle(fontFamily = Condensed, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)) {
                                            pr.variant?.let { append("$it, ") }
                                            append(Schedule.prescText(pr))
                                        }
                                    },
                                    style = Small,
                                )
                            }
                            if (ex.rest.isNotBlank()) {
                                Text(
                                    buildAnnotatedString {
                                        append("Recupero ")
                                        withStyle(SpanStyle(fontFamily = Condensed, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)) { append(ex.rest) }
                                    },
                                    style = Small,
                                )
                            }
                        }
                        if (ex.cue.isNotBlank()) Muted(ex.cue)
                    }
                }
            } else if (cur.planNotes.any { it.isNotBlank() }) {
                TipsCard(cur.planNotes.filter { it.isNotBlank() })
            }
        } else {
            Muted("Non ci sono sessioni di allenamento: aggiungine una da Programma.", Modifier.padding(top = 16.dp), Body)
        }

        H2("Come funziona")
        ExpandCard("Le ${s.weeks} settimane", initiallyOpen = true) {
            for (k in 0 until s.weeks) {
                val ph = s.phase(k)
                Row(Modifier.padding(bottom = 10.dp)) {
                    Text("${k + 1}", style = cond(22, FontWeight.Bold), color = p.muted, modifier = Modifier.width(32.dp))
                    Column(Modifier.weight(1f)) {
                        Text("${s.weekRange(k)}: ${s.phaseShort(k)}", style = Body.copy(fontWeight = FontWeight.SemiBold))
                        if (ph.note.isNotBlank()) Muted(ph.note)
                    }
                }
            }
        }
        prog.info.forEach { sec ->
            ExpandCard(sec.title, sec.open) { RichText(sec.body) }
        }
        Row {
            LinkBtn("Modifica programma") { vm.push(Route.ProgramEdit) }
            LinkBtn("Modifica testi") { vm.push(Route.SectionsEdit(SectionKind.PROGRAM_INFO)) }
        }
    }
}
