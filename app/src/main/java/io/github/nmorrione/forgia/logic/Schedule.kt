package io.github.nmorrione.forgia.logic

import io.github.nmorrione.forgia.data.Exercise
import io.github.nmorrione.forgia.data.Prescription
import io.github.nmorrione.forgia.data.Program
import io.github.nmorrione.forgia.data.SessionType
import io.github.nmorrione.forgia.data.TrainingLog
import io.github.nmorrione.forgia.data.WeekPhase
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.ceil

/** Esercizio già risolto per una settimana: blocco, variante e scarico applicati. */
data class ResolvedExercise(
    val exercise: Exercise,
    /** Chiave dello storico: le varianti hanno uno storico separato. */
    val logKey: String,
    val name: String,
    val cue: String,
    val sets: Int,
    val repMin: Int,
    val repMax: Int,
    val text: String,
    val rest: String,
    val deload: Boolean,
)

/** Calcola date, fasi e prescrizioni a partire dal programma. */
class Schedule(val program: Program) {
    val start: LocalDate = runCatching { LocalDate.parse(program.startDate) }.getOrElse { LocalDate.now() }
    val weeks: Int = program.weeks.coerceIn(1, 52)
    val days: Int = weeks * 7
    val end: LocalDate = start.plusDays(days - 1L)

    private val fallbackSession = SessionType("rest", "Riposo", "–", rest = true, color = 4)

    fun dateAt(i: Int): LocalDate = start.plusDays(i.toLong())
    fun indexOf(d: LocalDate): Int = ChronoUnit.DAYS.between(start, d).toInt()
    fun key(i: Int): String = dateAt(i).toString()
    fun weekOf(i: Int): Int = Math.floorDiv(i, 7)
    fun clampDay(i: Int): Int = i.coerceIn(0, days - 1)

    /**
     * Prima dell'inizio (settimane negative) vale la prima settimana: è la preparazione,
     * in cui si provano i carichi del primo blocco.
     */
    fun phase(week: Int): WeekPhase =
        program.weekPlan.getOrNull(week.coerceAtLeast(0)) ?: program.weekPlan.lastOrNull() ?: WeekPhase()

    fun blockName(b: Int): String = program.blocks.getOrNull(b) ?: "Blocco ${b + 1}"

    /** "Blocco 1 (RIR 3)", "Settimana di scarico" o, prima dell'inizio, "Preparazione (RIR 3)". */
    fun phaseLabel(week: Int): String {
        val ph = phase(week)
        return when {
            week < 0 -> "Preparazione (RIR ${ph.rir})"
            week >= weeks -> "Dopo la fine del ciclo"
            ph.deload -> "Settimana di scarico"
            else -> "${blockName(ph.block)} (RIR ${ph.rir})"
        }
    }

    /**
     * Il primo giorno raggiungibile nella vista Oggi: oggi o il primo giorno segnato,
     * se vengono prima dell'inizio, fino a 120 giorni prima.
     */
    fun firstDay(today: LocalDate, log: TrainingLog): Int {
        val firstLogged = log.days.keys.minOrNull()?.let { runCatching { indexOf(LocalDate.parse(it)) }.getOrNull() }
        return listOfNotNull(0, indexOf(today), firstLogged).min().coerceAtLeast(-120)
    }

    /**
     * L'ultimo giorno raggiungibile: dopo la fine del ciclo si continua a segnare
     * (oggi o l'ultimo giorno segnato), fino a 120 giorni dopo.
     */
    fun lastDay(today: LocalDate, log: TrainingLog): Int {
        val lastLogged = log.days.keys.maxOrNull()?.let { runCatching { indexOf(LocalDate.parse(it)) }.getOrNull() }
        return listOfNotNull(days - 1, indexOf(today), lastLogged).max().coerceAtMost(days - 1 + 120)
    }

    fun isFinished(today: LocalDate): Boolean = indexOf(today) >= days - 1

    /** "Blocco 1, RIR 3" o "Scarico". */
    fun phaseShort(week: Int): String {
        val ph = phase(week)
        return if (ph.deload) "Scarico, RIR ${program.deload.rir}" else "${blockName(ph.block)}, RIR ${ph.rir}"
    }

    fun weekRange(week: Int): String = It.range(dateAt(week * 7), dateAt(week * 7 + 6))

    fun plannedSessionId(d: LocalDate): String =
        program.weeklyTemplate.getOrNull(d.dayOfWeek.value - 1) ?: program.sessions.firstOrNull()?.id ?: "rest"

    fun sessionOn(d: LocalDate, log: TrainingLog): SessionType {
        val override = log.days[d.toString()]?.session
        return program.session(override)
            ?: program.session(plannedSessionId(d))
            ?: program.sessions.firstOrNull()
            ?: fallbackSession
    }

    fun sessionAt(i: Int, log: TrainingLog): SessionType = sessionOn(dateAt(i), log)

    /** In quali giorni della settimana tipo cade la sessione: "Lunedì e giovedì". */
    fun weekdaysOf(sessionId: String): String {
        val names = program.weeklyTemplate.mapIndexedNotNull { idx, id -> if (id == sessionId) It.weekdays[idx] else null }
        return capitalize(It.joinAnd(names))
    }

    /** Il giorno della settimana [week] in cui si misura la vita. */
    fun measureDayOfWeek(week: Int): Int {
        for (k in 0 until 7) {
            val i = week * 7 + k
            if (dateAt(i).dayOfWeek.value == program.measureWeekday) return i
        }
        return week * 7 + 6
    }

    fun prescriptionFor(ex: Exercise, block: Int): Prescription =
        ex.blocks.getOrNull(block) ?: ex.blocks.lastOrNull() ?: Prescription()

    fun resolve(ex: Exercise, week: Int): ResolvedExercise {
        val ph = phase(week)
        val p = prescriptionFor(ex, ph.block)
        val key = if (p.variant != null) "${ex.id}~b${ph.block}" else ex.id
        val sets = if (ph.deload) ceil(p.sets * program.deload.setsFactor).toInt().coerceAtLeast(1) else p.sets
        var text = prescText(p.copy(sets = sets))
        if (ph.deload && program.deload.loadChangePct != 0) {
            val pct = program.deload.loadChangePct
            text += ", carico ${if (pct < 0) "−" else "+"}${abs(pct)}%"
        }
        return ResolvedExercise(
            exercise = ex,
            logKey = key,
            name = p.variant ?: ex.name,
            cue = p.variantCue ?: ex.cue,
            sets = sets,
            repMin = p.repMin,
            repMax = p.repMax,
            text = text,
            rest = ex.rest,
            deload = ph.deload,
        )
    }

    companion object {
        fun repsText(p: Prescription): String =
            if (p.repMin == p.repMax) "${p.repMin}" else "${p.repMin}–${p.repMax}"

        fun prescText(p: Prescription): String =
            "${p.sets} × ${repsText(p)}" + if (p.suffix.isNotBlank()) " ${p.suffix}" else ""
    }
}
