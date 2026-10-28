package io.github.nmorrione.forgia.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.nmorrione.forgia.timer.Phase
import io.github.nmorrione.forgia.timer.TimerEngine
import io.github.nmorrione.forgia.timer.TimerKind
import io.github.nmorrione.forgia.timer.TimerState
import io.github.nmorrione.forgia.timer.fmtClock
import io.github.nmorrione.forgia.ui.theme.Body
import io.github.nmorrione.forgia.ui.theme.Pal
import io.github.nmorrione.forgia.ui.theme.Small
import io.github.nmorrione.forgia.ui.theme.cond

/** Altezza riservata in fondo alle schermate quando la barra del timer è visibile. */
val TimerBarSpace: Dp = 104.dp

/**
 * Il timer in corso, in fondo a ogni schermata: tempo, pausa, ±15 s e stop.
 * Quando suona diventa rossa con un grande "Ferma".
 */
@Composable
fun TimerBar(onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val st by TimerEngine.state.collectAsState()
    if (!st.active) return
    val p = Pal
    val color = phaseColor(st)
    val ringing = st.phase == Phase.RINGING
    val done = st.phase == Phase.DONE
    val shape = RoundedCornerShape(16.dp)
    Box(modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), contentAlignment = Alignment.BottomCenter) {
        Column(
            Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .shadow(8.dp, shape)
                .clip(shape)
                .background(if (ringing) p.bad else p.surface)
                .border(2.dp, color, shape)
                .clickable(onClick = onOpen),
        ) {
            Row(
                Modifier.padding(start = 16.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    val label = when {
                        ringing -> "Tempo!"
                        done && st.kind == TimerKind.INTERVAL -> "Circuito finito"
                        done -> "Tempo scaduto"
                        st.kind == TimerKind.INTERVAL -> st.phase.label + (st.exercise?.let { " · $it" } ?: "")
                        else -> st.title
                    }
                    Text(
                        label, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        style = if (ringing) cond(26, FontWeight.Bold) else Small.copy(fontWeight = FontWeight.SemiBold),
                        color = if (ringing) Color.White else color,
                    )
                    if (ringing) {
                        Text(st.title, style = Small, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    } else if (!done) {
                        Text(
                            fmtClock(st.remainingMs) + if (st.paused) "  in pausa" else "",
                            style = cond(30, FontWeight.Bold, 1.0),
                            color = if (st.paused) p.muted else p.ink,
                        )
                    }
                }
                when {
                    ringing -> BarButton("Ferma", filled = true, inverted = true) { TimerEngine.stop() }
                    done -> BarButton("Chiudi", filled = true) { TimerEngine.stop() }
                    else -> Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (st.kind == TimerKind.COUNTDOWN) {
                            BarButton("−15") { TimerEngine.addTime(-15_000) }
                            BarButton("+15") { TimerEngine.addTime(15_000) }
                        } else {
                            BarButton("Salta") { TimerEngine.skip() }
                        }
                        BarButton(if (st.paused) "▶" else "❚❚", filled = true) {
                            if (st.paused) TimerEngine.resume() else TimerEngine.pause()
                        }
                        BarButton("✕") { TimerEngine.stop() }
                    }
                }
            }
            // Avanzamento del tratto in corso
            if (!ringing && !done && st.stepMs > 0) {
                val progress = (1f - st.remainingMs.toFloat() / st.stepMs).coerceIn(0f, 1f)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(p.line)
                        .drawBehind { drawRect(color, size = Size(size.width * progress, size.height)) },
                )
            }
        }
    }
}

@Composable
private fun phaseColor(st: TimerState): Color {
    val p = Pal
    return when (st.phase) {
        Phase.WORK -> p.good
        Phase.REST, Phase.SET_REST -> p.accent
        Phase.PREPARE -> p.warn
        Phase.RINGING -> p.bad
        Phase.DONE -> p.good
        else -> p.ink
    }
}

@Composable
private fun BarButton(text: String, filled: Boolean = false, inverted: Boolean = false, onClick: () -> Unit) {
    val p = Pal
    val bg = when {
        inverted -> Color.White
        filled -> p.btn
        else -> p.surface2
    }
    val fg = when {
        inverted -> p.bad
        filled -> p.btnInk
        else -> p.ink
    }
    val wide = text.length > 2
    Box(
        Modifier
            .then(if (wide) Modifier.height(44.dp) else Modifier.size(44.dp))
            .clip(if (wide) RoundedCornerShape(22.dp) else CircleShape)
            .background(bg)
            .border(1.dp, if (filled || inverted) bg else p.line, if (wide) RoundedCornerShape(22.dp) else CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = if (wide) 14.dp else 0.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = Body.copy(fontWeight = FontWeight.Bold), color = fg)
    }
}
