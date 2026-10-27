package io.github.nmorrione.forgia.logic

import io.github.nmorrione.forgia.data.AdjustRule
import io.github.nmorrione.forgia.data.Adjustment
import io.github.nmorrione.forgia.data.DayLog
import io.github.nmorrione.forgia.data.SetEntry
import io.github.nmorrione.forgia.data.TrainingLog
import java.time.LocalDate

data class PrevSets(val date: LocalDate, val sets: List<SetEntry>)

data class Stats(val planned: Int, val done: Int, val foodOk: Int)

/** Quello che hai segnato prima dell'inizio del programma. */
data class PrepSummary(val daysLogged: Int, val workouts: Int, val avgWeight: Double?, val weighIns: Int)

enum class Advice { UP, DOWN, HOLD, WAIT }

data class CalorieAdvice(val kind: Advice, val text: String, val deltaKcal: Int = 0)

/** Calcoli sul diario: medie, storico degli esercizi, statistiche e suggerimenti. */
class Tracker(private val s: Schedule, private val log: TrainingLog) {

    fun day(i: Int): DayLog = log.days[s.key(i)] ?: DayLog()

    fun weekAvg(w: Int): Double? {
        val ws = (w * 7 until w * 7 + 7).mapNotNull { day(it).weight }
        return if (ws.isEmpty()) null else ws.average()
    }

    /** L'ultima misura della vita segnata nella settimana. */
    fun weekWaist(w: Int): Double? = (w * 7 until w * 7 + 7).reversed().firstNotNullOfOrNull { day(it).waist }

    fun weekSessions(w: Int): Pair<Int, Int> {
        var planned = 0
        var done = 0
        for (i in w * 7 until w * 7 + 7) {
            if (!s.sessionAt(i, log).rest) {
                planned++
                if (day(i).done) done++
            }
        }
        return done to planned
    }

    fun stats(): Stats {
        var planned = 0
        var done = 0
        var foodOk = 0
        for (i in 0 until s.days) {
            val d = day(i)
            if (!s.sessionAt(i, log).rest) {
                planned++
                if (d.done) done++
            }
            if (d.food == 1) foodOk++
        }
        return Stats(planned, done, foodOk)
    }

    /** Preparazione: allenamenti segnati e media delle ultime 7 pesate prima dell'inizio. */
    fun prepSummary(): PrepSummary? {
        val startKey = s.start.toString()
        val pre = log.days.entries.filter { it.key < startKey }.sortedBy { it.key }
        if (pre.isEmpty()) return null
        val weights = pre.mapNotNull { it.value.weight }.takeLast(7)
        return PrepSummary(
            daysLogged = pre.size,
            workouts = pre.count { it.value.done },
            avgWeight = if (weights.isEmpty()) null else weights.average(),
            weighIns = weights.size,
        )
    }

    /** L'ultima volta che hai segnato [logKey] prima di [before], anche in programmi precedenti. */
    fun lastSets(logKey: String, before: LocalDate): PrevSets? {
        val b = before.toString()
        return log.days.entries
            .filter { it.key < b && it.value.sets[logKey]?.any { e -> !e.isEmpty } == true }
            .maxByOrNull { it.key }
            ?.let { PrevSets(LocalDate.parse(it.key), it.value.sets.getValue(logKey).filter { e -> !e.isEmpty }) }
    }

    /**
     * Le regole della Guida: ogni [AdjustRule.everyWeeks] settimane confronta la media del peso
     * e la vita con quelle di [AdjustRule.everyWeeks] settimane prima.
     */
    fun calorieAdvice(today: LocalDate, rule: AdjustRule, adjustments: List<Adjustment>): CalorieAdvice? {
        val every = rule.everyWeeks.coerceAtLeast(1)
        val w = (s.weekOf(s.indexOf(today)) - 1).coerceAtMost(s.weeks - 1)
        if (w < every) return null
        val a = weekAvg(w) ?: return null
        val b = weekAvg(w - every) ?: return null
        val rate = (a - b) / every
        val waistA = weekWaist(w)
        val waistB = weekWaist(w - every)
        val waistDelta = if (waistA != null && waistB != null) waistA - waistB else null

        val windowStart = s.dateAt((w - every + 1) * 7).toString()
        val recent = adjustments.lastOrNull { it.date >= windowStart }
        val facts = "Media settimana ${w + 1}: ${fmt1(a)} kg, ${signed1(rate)} kg a settimana" +
            (waistDelta?.let { ", vita ${signed1(it)} cm" } ?: "") + "."
        if (recent != null) {
            return CalorieAdvice(
                Advice.WAIT,
                "$facts Hai già corretto le calorie il ${It.shortDate(LocalDate.parse(recent.date))}: aspetta altre $every settimane di dati.",
            )
        }
        return when {
            rate > rule.maxWeeklyGain || (waistDelta != null && waistDelta >= rule.waistAlarmCm) -> CalorieAdvice(
                Advice.DOWN, "$facts Stai salendo troppo in fretta: togli ${rule.stepKcal} kcal al giorno.", -rule.stepKcal,
            )
            rate < rule.minWeeklyGain && (waistDelta == null || waistDelta <= 0.0) -> CalorieAdvice(
                Advice.UP, "$facts Il peso è quasi fermo: aggiungi ${rule.stepKcal} kcal al giorno.", rule.stepKcal,
            )
            else -> CalorieAdvice(Advice.HOLD, "$facts Sei nel range: non toccare niente.")
        }
    }

    companion object {
        /** "60 kg × 7, 7, 6, 6" se il carico è lo stesso, altrimenti "60 × 7, 62,5 × 6". */
        fun formatSets(sets: List<SetEntry>): String {
            val filled = sets.filter { !it.isEmpty }
            if (filled.isEmpty()) return ""
            val kgs = filled.map { it.kg }.distinct()
            val reps = filled.joinToString(", ") { it.reps?.toString() ?: "?" }
            return when {
                kgs.size == 1 && kgs[0] == null -> "$reps ripetizioni"
                kgs.size == 1 -> "${fmtNum(kgs[0]!!)} kg × $reps"
                else -> filled.joinToString(", ") { "${it.kg?.let(::fmtNum) ?: "–"} × ${it.reps ?: "?"}" }
            }
        }

        /** Doppia progressione: range chiuso in tutte le serie → aumenta il carico. */
        fun progressionHint(prev: List<SetEntry>, r: ResolvedExercise): String? {
            val withReps = prev.filter { it.reps != null }
            if (withReps.isEmpty() || r.deload) return null
            val closed = withReps.size >= r.sets && withReps.take(r.sets).all { it.reps!! >= r.repMax }
            return if (closed) {
                "Range chiuso: aumenta il carico" + if (r.exercise.increment.isNotBlank()) " (${r.exercise.increment})." else "."
            } else {
                "Stesso carico: punta a ${r.repMax} ripetizioni in tutte le serie."
            }
        }
    }
}
