package io.github.nmorrione.forgia.logic

import io.github.nmorrione.forgia.data.AdjustRule
import io.github.nmorrione.forgia.data.AppData
import io.github.nmorrione.forgia.data.CycleRecord
import io.github.nmorrione.forgia.data.DayTarget
import io.github.nmorrione.forgia.data.DayType
import io.github.nmorrione.forgia.data.Goal
import io.github.nmorrione.forgia.data.Nutrition
import io.github.nmorrione.forgia.data.Program
import io.github.nmorrione.forgia.data.WeekPhase
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.math.max
import kotlin.math.roundToInt

data class NextCycleOptions(
    val goal: Goal,
    val name: String,
    val start: LocalDate,
    val weeks: Int,
    val deloadFirst: Boolean,
)

/** Costruisce il ciclo successivo: fasi, obiettivi calorici e regole, poi archivia quello attuale. */
object NextCycle {

    fun defaultWeeks(goal: Goal) = when (goal) {
        Goal.BULK -> 8
        Goal.CUT -> 6
        Goal.MAINTENANCE -> 4
    }

    fun defaultName(goal: Goal, current: Program, cycles: Int): String = when (goal) {
        Goal.BULK -> "Massa, ciclo ${cycles + 2}"
        Goal.CUT -> "Definizione"
        Goal.MAINTENANCE -> "Mantenimento"
    }

    /** Il lunedì dopo la fine del ciclo, o dopo oggi se la fine è già passata. */
    fun defaultStart(program: Program, today: LocalDate): LocalDate {
        val s = Schedule(program)
        var d = maxOf(s.end.plusDays(1), today)
        while (d.dayOfWeek != DayOfWeek.MONDAY) d = d.plusDays(1)
        return d
    }

    /** Se l'ultima settimana del ciclo non era di scarico, si riparte con una settimana leggera. */
    fun defaultDeloadFirst(program: Program): Boolean =
        program.weekPlan.getOrNull(program.weeks - 1)?.deload != true

    /** Ritmo del peso previsto per ogni obiettivo, in kg a settimana. */
    fun rule(goal: Goal, current: AdjustRule): AdjustRule = when (goal) {
        Goal.BULK -> current.copy(minWeeklyGain = 0.1, maxWeeklyGain = 0.4, waistAlarmCm = 1.0)
        Goal.CUT -> current.copy(minWeeklyGain = -0.75, maxWeeklyGain = -0.25, waistAlarmCm = 1.0)
        Goal.MAINTENANCE -> current.copy(minWeeklyGain = -0.15, maxWeeklyGain = 0.15, waistAlarmCm = 1.0)
    }

    /**
     * Le settimane del ciclo: eventuale scarico iniziale, poi uno o due blocchi
     * con il RIR che scende, separati da uno scarico se il ciclo è abbastanza lungo.
     */
    fun plan(goal: Goal, weeks: Int, deloadFirst: Boolean, blocks: Int): List<WeekPhase> {
        val out = mutableListOf<WeekPhase>()
        if (deloadFirst) {
            out += WeekPhase(0, "4", deload = true, note = "Settimana di transizione: metà serie e carichi leggeri, per ripartire riposato.")
        }
        val work = weeks - out.size
        if (work <= 0) return out.take(max(weeks, 1))
        if (blocks >= 2 && work >= 6) {
            val a = (work - 1) / 2
            out += block(goal, 0, a)
            out += WeekPhase(0, "4", deload = true, note = "Scarico a metà ciclo: metà serie, carichi −10%.")
            out += block(goal, 1, work - 1 - a)
        } else {
            out += block(goal, 0, work)
        }
        return out
    }

    private fun block(goal: Goal, b: Int, k: Int): List<WeekPhase> = (0 until k).map { i ->
        val f = if (k == 1) 0.5 else i.toDouble() / (k - 1)
        val rir = when (goal) {
            // In definizione si resta un po' più lontani dal cedimento
            Goal.CUT -> if (f < 0.67) "2" else "1–2"
            else -> when {
                k == 1 -> "2"
                f < 0.34 -> if (b == 0) "3" else "2"
                f < 0.67 -> "2"
                else -> "1"
            }
        }
        val note = when {
            i == 0 && goal == Goal.CUT -> "In definizione l’obiettivo è tenere i carichi, non aumentarli a tutti i costi."
            i == 0 && goal == Goal.MAINTENANCE -> "Mantieni i carichi e cura la tecnica."
            i == 0 && b == 0 -> "Riparti dai carichi dell’ultimo ciclo, con un po’ di margine."
            i == 0 -> "Meno ripetizioni, più carico."
            i == k - 1 && k > 1 -> "La settimana più dura del blocco."
            else -> ""
        }
        WeekPhase(b, rir, note = note)
    }

    /**
     * Obiettivi per il nuovo ciclo. Con il mantenimento misurato: massa +250 kcal, definizione −400,
     * mantenimento pari. Senza, si parte dagli obiettivi attuali. Proteine e grassi seguono il peso.
     */
    fun targets(goal: Goal, n: Nutrition, program: Program, maintenance: Double?, newWeight: Double): Pair<DayTarget, DayTarget> {
        val trainDays = program.weeklyTemplate.count { program.session(it)?.rest == false }.coerceIn(0, 7)
        val diff = n.train.kcal - n.rest.kcal
        val currentAvg = (trainDays * n.train.kcal + (7 - trainDays) * n.rest.kcal) / 7.0
        val avg = when (goal) {
            Goal.BULK -> maintenance?.plus(250) ?: currentAvg
            Goal.CUT -> maintenance?.minus(400) ?: (currentAvg - 500)
            Goal.MAINTENANCE -> maintenance ?: (currentAvg - 250)
        }
        val train = round10(avg + diff * (7 - trainDays) / 7.0)
        val rest = train - diff
        val ratio = newWeight / n.bodyWeightKg.coerceAtLeast(1.0)
        var pTrain = round5(n.train.protein * ratio)
        var pRest = round5(n.rest.protein * ratio)
        var fat = round5(n.train.fat * ratio)
        if (goal == Goal.CUT) {
            // Più proteine per difendere il muscolo, grassi non sotto 0,8 g/kg
            pTrain = max(pTrain, round5(2.4 * newWeight))
            pRest = max(pRest, round5(2.3 * newWeight))
            fat = max(fat, round5(0.8 * newWeight))
        }
        return DayTarget(train, pTrain, fat) to DayTarget(rest, pRest, fat)
    }

    private fun round10(v: Double) = (v / 10).roundToInt() * 10
    private fun round5(v: Double) = (v / 5).roundToInt() * 5

    /** Archivia il ciclo attuale e prepara il nuovo; diario, alimenti e pasti restano (le quantità si ricalcolano). */
    fun apply(data: AppData, o: NextCycleOptions, report: CycleReport, today: LocalDate): AppData {
        val n = data.nutrition
        val record = CycleRecord(
            program = data.program,
            train = n.train,
            rest = n.rest,
            adjustments = n.adjustments,
            endedOn = minOf(today, Schedule(data.program).end).toString(),
        )
        val program = data.program.copy(
            name = o.name.ifBlank { o.goal.label },
            startDate = o.start.toString(),
            weeks = o.weeks.coerceIn(1, 52),
            weekPlan = plan(o.goal, o.weeks, o.deloadFirst, data.program.blocks.size),
            goal = o.goal,
        )
        val weight = report.endWeight ?: n.bodyWeightKg
        val (train, rest) = targets(o.goal, n, data.program, report.maintenance, weight)
        var nut = n.copy(
            bodyWeightKg = (weight * 10).roundToInt() / 10.0,
            train = train,
            rest = rest,
            adjust = rule(o.goal, n.adjust),
            adjustments = emptyList(),
            targetNote = targetNote(o.goal, report),
        )
        nut = NutritionCalc.rescaleDay(NutritionCalc.rescaleDay(nut, DayType.TRAIN), DayType.REST)
        return data.copy(program = program, nutrition = nut, cycles = data.cycles + record)
    }

    private fun targetNote(goal: Goal, r: CycleReport): String {
        val base = r.maintenance?.let { "Mantenimento stimato dal ciclo precedente: circa ${fmtInt(it.roundToInt())} kcal al giorno. " }
            ?: "Obiettivi ripresi dal ciclo precedente: il mantenimento non si è potuto stimare. "
        return base + when (goal) {
            Goal.BULK -> "In media circa 250 kcal sopra, per salire di 0,1–0,4 kg a settimana."
            Goal.CUT -> "In media circa 400 kcal sotto, per scendere di 0,25–0,75 kg a settimana senza perdere forza."
            Goal.MAINTENANCE -> "In media al mantenimento: il peso dovrebbe restare entro ±0,15 kg a settimana."
        }
    }
}
