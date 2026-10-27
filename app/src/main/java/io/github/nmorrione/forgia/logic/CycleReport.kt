package io.github.nmorrione.forgia.logic

import io.github.nmorrione.forgia.data.Adjustment
import io.github.nmorrione.forgia.data.DayTarget
import io.github.nmorrione.forgia.data.Goal
import io.github.nmorrione.forgia.data.Program
import io.github.nmorrione.forgia.data.SetEntry
import io.github.nmorrione.forgia.data.TrainingLog
import java.time.LocalDate
import kotlin.math.max

/** Kcal in 1 kg di peso corporeo: la stima classica usata per il mantenimento. */
const val KCAL_PER_KG = 7700.0

data class ExerciseProgress(
    val name: String,
    val firstText: String,
    val bestText: String,
    val firstE1rm: Double,
    val bestE1rm: Double,
) {
    /** Variazione della forza stimata, in percentuale. */
    val pct: Double get() = if (firstE1rm > 0) (bestE1rm / firstE1rm - 1) * 100 else 0.0
}

data class CycleReport(
    val start: LocalDate,
    val end: LocalDate,
    /** Giorni del ciclo già passati (tutti, se è finito). */
    val elapsedDays: Int,
    val finished: Boolean,
    val startWeight: Double?,
    val endWeight: Double?,
    /** kg a settimana, dalla retta che passa per le medie settimanali. */
    val weeklyRate: Double?,
    val waistStart: Double?,
    val waistEnd: Double?,
    val planned: Int,
    val done: Int,
    val foodOk: Int,
    val exercises: List<ExerciseProgress>,
    /** Calorie medie al giorno previste dal piano, tenendo conto delle correzioni. */
    val avgKcal: Double,
    /** Stima del mantenimento: calorie medie meno il surplus che spiega il ritmo del peso. */
    val maintenance: Double?,
    val weeksWithWeight: Int,
    val recommended: Goal,
    val reason: String,
) {
    val weightDelta: Double? get() = if (startWeight != null && endWeight != null) endWeight - startWeight else null
    val waistDelta: Double? get() = if (waistStart != null && waistEnd != null) waistEnd - waistStart else null
}

object CycleReports {

    /** Forza stimata (formula di Epley); senza carico non si calcola. */
    fun e1rm(e: SetEntry): Double? {
        val kg = e.kg ?: return null
        val reps = e.reps ?: return null
        if (kg <= 0 || reps <= 0) return null
        return kg * (1 + reps / 30.0)
    }

    /** Pendenza della retta dei minimi quadrati. */
    fun slope(points: List<Pair<Double, Double>>): Double? {
        if (points.size < 2) return null
        val mx = points.sumOf { it.first } / points.size
        val my = points.sumOf { it.second } / points.size
        val den = points.sumOf { (it.first - mx) * (it.first - mx) }
        if (den == 0.0) return null
        return points.sumOf { (it.first - mx) * (it.second - my) } / den
    }

    fun build(
        program: Program,
        train: DayTarget,
        rest: DayTarget,
        adjustments: List<Adjustment>,
        log: TrainingLog,
        today: LocalDate,
    ): CycleReport {
        val s = Schedule(program)
        val t = Tracker(s, log)
        val lastIdx = minOf(s.days - 1, s.indexOf(today))
        val finished = s.indexOf(today) >= s.days - 1

        // Peso: medie settimanali, più il peso di partenza della preparazione
        val lastWeek = if (lastIdx < 0) -1 else s.weekOf(lastIdx)
        val weekly = (0..lastWeek).mapNotNull { w -> t.weekAvg(w)?.let { w to it } }
        val prep = t.prepSummary()?.avgWeight
        val startWeight = prep ?: weekly.firstOrNull()?.second
        val endWeight = weekly.lastOrNull()?.second
        val rate = slope(weekly.map { it.first.toDouble() to it.second })

        // Vita: la prima misura (anche in preparazione) e l'ultima del ciclo
        val startKey = s.start.toString()
        val preWaist = log.days.entries.filter { it.key < startKey && it.value.waist != null }.maxByOrNull { it.key }?.value?.waist
        val waists = (0..lastWeek).mapNotNull { t.weekWaist(it) }
        val waistStart = preWaist ?: waists.firstOrNull()
        val waistEnd = waists.lastOrNull()

        var planned = 0
        var done = 0
        var foodOk = 0
        var kcalSum = 0.0
        for (i in 0..maxOf(lastIdx, -1)) {
            val restDay = s.sessionAt(i, log).rest
            val d = t.day(i)
            if (!restDay) {
                planned++
                if (d.done) done++
            }
            if (d.food == 1) foodOk++
            // Le correzioni applicate dopo questo giorno non c'erano ancora
            val key = s.key(i)
            val later = adjustments.filter { it.date > key }.sumOf { it.deltaKcal }
            kcalSum += (if (restDay) rest.kcal else train.kcal) - later
        }
        val avgKcal = if (lastIdx >= 0) kcalSum / (lastIdx + 1) else {
            val trainDays = program.weeklyTemplate.count { program.session(it)?.rest == false }
            (trainDays * train.kcal + (7 - trainDays) * rest.kcal) / 7.0
        }
        val maintenance = if (rate != null && weekly.size >= 3) avgKcal - rate * KCAL_PER_KG / 7 else null

        val exercises = if (lastIdx < 0) emptyList() else progress(program, log, s.start, s.dateAt(lastIdx))
        val (goal, reason) = recommend(program.goal, startWeight, endWeight, waistStart, waistEnd, weekly.size)

        return CycleReport(
            start = s.start, end = s.end, elapsedDays = maxOf(lastIdx + 1, 0), finished = finished,
            startWeight = startWeight, endWeight = endWeight, weeklyRate = rate,
            waistStart = waistStart, waistEnd = waistEnd,
            planned = planned, done = done, foodOk = foodOk, exercises = exercises,
            avgKcal = avgKcal, maintenance = maintenance, weeksWithWeight = weekly.size,
            recommended = goal, reason = reason,
        )
    }

    /** Per ogni esercizio (e variante): la serie migliore della prima seduta e la migliore del ciclo. */
    private fun progress(program: Program, log: TrainingLog, from: LocalDate, to: LocalDate): List<ExerciseProgress> {
        val names = linkedMapOf<String, String>()
        program.sessions.filter { !it.rest }.forEach { st ->
            st.exercises.forEach { ex ->
                program.blocks.indices.forEach { b ->
                    val p = ex.blocks.getOrNull(b) ?: ex.blocks.lastOrNull() ?: return@forEach
                    val key = if (p.variant != null) "${ex.id}~b$b" else ex.id
                    names.putIfAbsent(key, p.variant ?: ex.name)
                }
            }
        }
        val a = from.toString()
        val z = to.toString()
        val days = log.days.entries.filter { it.key in a..z }.sortedBy { it.key }
        return names.mapNotNull { (key, name) ->
            val sessions = days.mapNotNull { d ->
                d.value.sets[key]?.mapNotNull { e -> e1rm(e)?.let { e to it } }?.maxByOrNull { it.second }
            }
            if (sessions.isEmpty()) return@mapNotNull null
            val first = sessions.first()
            val best = sessions.maxBy { it.second }
            ExerciseProgress(name, setText(first.first), setText(best.first), first.second, best.second)
        }
    }

    private fun setText(e: SetEntry) = "${fmtNum(e.kg ?: 0.0)} kg × ${e.reps}"

    /** Il consiglio per il prossimo ciclo, con la spiegazione. */
    fun recommend(
        goal: Goal, startWeight: Double?, endWeight: Double?, waistStart: Double?, waistEnd: Double?, weeksWithWeight: Int,
    ): Pair<Goal, String> {
        if (startWeight == null || endWeight == null || weeksWithWeight < 2) {
            return goal to "Ci sono pochi dati sul peso per un consiglio: ripeti lo stesso obiettivo e pesati più spesso."
        }
        val dw = endWeight - startWeight
        val dv = if (waistStart != null && waistEnd != null) waistEnd - waistStart else null
        return when (goal) {
            Goal.BULK -> when {
                dv != null && (dv >= 3.0 || (dw > 0 && dv / max(dw, 0.5) > 1.5)) ->
                    Goal.CUT to "La vita è cresciuta di ${fmt1(dv)} cm per ${signed1(dw)} kg: qualche settimana di definizione, poi riprendi la massa."
                dw < 0.3 ->
                    Goal.BULK to "Il peso è cambiato poco (${signed1(dw)} kg): continua la massa, con le calorie ricalcolate sul tuo mantenimento."
                else ->
                    Goal.BULK to "Peso${if (dv != null) " e vita" else ""} sono cresciuti in modo equilibrato (${signed1(dw)} kg${dv?.let { ", vita ${signed1(it)} cm" } ?: ""}): continua la massa."
            }
            Goal.CUT -> if (dw > -0.5) {
                Goal.CUT to "Il peso è sceso poco (${signed1(dw)} kg): qualche settimana in più di definizione."
            } else {
                Goal.BULK to "Definizione fatta (${signed1(dw)} kg${dv?.let { ", vita ${signed1(it)} cm" } ?: ""}): torna a costruire, dal mantenimento più un piccolo surplus."
            }
            Goal.MAINTENANCE -> Goal.BULK to "Dopo il mantenimento puoi riprendere la massa, partendo dal mantenimento che hai misurato."
        }
    }
}
