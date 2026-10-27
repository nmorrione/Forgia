package io.github.nmorrione.forgia.timer

import io.github.nmorrione.forgia.data.IntervalConfig

enum class TimerKind { NONE, COUNTDOWN, INTERVAL }

enum class Phase(val label: String) {
    IDLE(""),
    PREPARE("Preparati"),
    WORK("Lavoro"),
    REST("Recupero"),
    SET_REST("Recupero tra le serie"),
    COUNTDOWN("Timer"),
    RINGING("Tempo!"),
    DONE("Finito!"),
}

/** Un tratto del timer: preparazione, lavoro, recupero… */
data class Step(
    val phase: Phase,
    val ms: Long,
    val round: Int = 0,
    val set: Int = 0,
    val exercise: String? = null,
    val next: String? = null,
)

data class TimerState(
    val kind: TimerKind = TimerKind.NONE,
    val phase: Phase = Phase.IDLE,
    val remainingMs: Long = 0,
    val stepMs: Long = 0,
    val totalRemainingMs: Long = 0,
    val paused: Boolean = false,
    val round: Int = 0,
    val rounds: Int = 0,
    val set: Int = 0,
    val sets: Int = 0,
    val exercise: String? = null,
    val next: String? = null,
    val title: String = "",
) {
    val active: Boolean get() = phase != Phase.IDLE
    val running: Boolean get() = phase in setOf(Phase.PREPARE, Phase.WORK, Phase.REST, Phase.SET_REST, Phase.COUNTDOWN)
}

object Intervals {
    /**
     * Preparazione, poi per ogni serie: lavoro e recupero alternati (senza recupero dopo l'ultima
     * ripetizione), e il recupero tra le serie.
     */
    fun steps(c: IntervalConfig): List<Step> {
        val names = c.exercises.map { it.trim() }.filter { it.isNotEmpty() }
        fun ex(i: Int): String? = if (names.isEmpty()) null else names[i % names.size]
        val rounds = c.rounds.coerceAtLeast(1)
        val sets = c.sets.coerceAtLeast(1)
        val out = mutableListOf<Step>()
        if (c.prepareSec > 0) out += Step(Phase.PREPARE, c.prepareSec * 1000L, 1, 1, next = ex(0))
        for (s in 1..sets) {
            for (r in 1..rounds) {
                out += Step(Phase.WORK, c.workSec.coerceAtLeast(1) * 1000L, r, s, exercise = ex(r - 1), next = ex(r).takeIf { r < rounds })
                if (r < rounds && c.restSec > 0) out += Step(Phase.REST, c.restSec * 1000L, r, s, next = ex(r))
            }
            if (s < sets && c.setRestSec > 0) out += Step(Phase.SET_REST, c.setRestSec * 1000L, rounds, s, next = ex(0))
        }
        return out
    }

    fun totalMs(c: IntervalConfig): Long = steps(c).sumOf { it.ms }
}

/** 90 000 ms → "1:30"; arrotonda per eccesso, come un timer. */
fun fmtClock(ms: Long): String {
    val total = ((ms.coerceAtLeast(0) + 999) / 1000)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
