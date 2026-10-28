package io.github.nmorrione.forgia.data

import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

/**
 * Versione del formato salvato. Ogni campo nuovo deve avere un valore predefinito,
 * così i file delle versioni precedenti si leggono senza perdere niente.
 * 1: versione 0.1.0. 2: aggiunte le impostazioni del timer. 3: obiettivo del ciclo e archivio dei cicli.
 */
const val SCHEMA_VERSION = 3

/** Tutto ciò che l'app salva: configurazione (programma, alimentazione, testi), timer e diario. */
@Serializable
data class AppData(
    val schema: Int = SCHEMA_VERSION,
    val program: Program,
    val nutrition: Nutrition,
    val guide: List<TextSection> = emptyList(),
    val log: TrainingLog = TrainingLog(),
    val timer: TimerSettings = TimerSettings(),
    /** I cicli conclusi, dal più vecchio. Il diario resta in [log], legato alle date. */
    val cycles: List<CycleRecord> = emptyList(),
)

/* ---------- Cicli ---------- */

@Serializable
enum class Goal(val label: String) {
    BULK("Massa"),
    CUT("Definizione"),
    MAINTENANCE("Mantenimento"),
}

/** Un ciclo concluso: il programma e gli obiettivi com'erano, per rifarne il riepilogo. */
@Serializable
data class CycleRecord(
    val program: Program,
    val train: DayTarget,
    val rest: DayTarget,
    val adjustments: List<Adjustment> = emptyList(),
    val endedOn: String,
)

/* ---------- Timer ---------- */

@Serializable
enum class TimerSound {
    /** Volume dei contenuti multimediali: si mescola alla musica. */
    MEDIA,
    /** Volume della sveglia: suona anche col telefono in silenzioso. */
    ALARM,
}

/** Cosa suona alla fine del timer classico. */
@Serializable
enum class EndSound {
    /** Beep brevi a ripetizione, come il timer dell'Orologio. */
    BEEPER,
    /** La suoneria della sveglia di sistema. */
    SYSTEM_ALARM,
}

@Serializable
data class IntervalConfig(
    val name: String = "Circuito",
    val workSec: Int = 40,
    val restSec: Int = 20,
    /** Ripetizioni di lavoro in ogni serie. */
    val rounds: Int = 6,
    val sets: Int = 1,
    val setRestSec: Int = 120,
    val prepareSec: Int = 10,
    /** Facoltativi: uno per ripetizione, ricominciano dal primo se sono meno. */
    val exercises: List<String> = emptyList(),
)

@Serializable
data class TimerSettings(
    val countdownSec: Int = 90,
    val interval: IntervalConfig = Defaults.intervalPresets().first(),
    val presets: List<IntervalConfig> = Defaults.intervalPresets(),
    val sound: TimerSound = TimerSound.MEDIA,
    val vibrate: Boolean = true,
    /** Campo aggiunto nella 0.5.0: con il valore predefinito i dati vecchi si leggono senza migrazione. */
    val endSound: EndSound = EndSound.BEEPER,
)

/* ---------- Programma di allenamento ---------- */

@Serializable
data class Program(
    val name: String,
    /** Data ISO (yyyy-MM-dd) del primo giorno. */
    val startDate: String,
    val weeks: Int,
    /** Id della sessione per ogni giorno della settimana, da lunedì a domenica. */
    val weeklyTemplate: List<String>,
    val sessions: List<SessionType>,
    /** Nomi dei blocchi; ogni esercizio ha una prescrizione per blocco. */
    val blocks: List<String>,
    /** Una voce per settimana: blocco, RIR, scarico. */
    val weekPlan: List<WeekPhase>,
    val deload: DeloadRule = DeloadRule(),
    val warmup: String = "",
    val habits: List<Habit> = emptyList(),
    /** Giorno della misura della vita: 1 = lunedì … 7 = domenica. */
    val measureWeekday: Int = 7,
    val info: List<TextSection> = emptyList(),
    val goal: Goal = Goal.BULK,
    /** Recupero per cambiare esercizio, in secondi; ogni scheda può avere il suo. */
    val changeRestSec: Int = 180,
) {
    fun session(id: String?): SessionType? = sessions.firstOrNull { it.id == id }

    /** Il recupero tra un esercizio e l'altro della sessione: il suo, altrimenti quello del programma. */
    fun changeRest(s: SessionType): Int = s.changeRestSec ?: changeRestSec
}

@Serializable
data class WeekPhase(
    val block: Int = 0,
    val rir: String = "2",
    val deload: Boolean = false,
    val note: String = "",
)

@Serializable
data class DeloadRule(
    /** Frazione delle serie da tenere (arrotondata per eccesso). */
    val setsFactor: Double = 0.5,
    val loadChangePct: Int = -10,
    val rir: String = "4",
)

@Serializable
data class SessionType(
    val id: String,
    val name: String,
    val short: String,
    val description: String = "",
    /** Indice nella palette delle sessioni. */
    val color: Int = 0,
    val rest: Boolean = false,
    val duration: String = "",
    val exercises: List<Exercise> = emptyList(),
    val tipsTitle: String = "Note",
    /** Consigli mostrati nella vista Oggi; {domani} diventa la sessione del giorno dopo. */
    val tips: List<Tip> = emptyList(),
    /** Note mostrate nella scheda quando la sessione non ha esercizi. */
    val planNotes: List<String> = emptyList(),
    /** Recupero per cambiare esercizio solo per questa scheda; null = quello del programma. */
    val changeRestSec: Int? = null,
)

@Serializable
data class Tip(
    val text: String,
    /** Se impostato, il consiglio compare solo in quel giorno (1 = lunedì … 7 = domenica). */
    val weekday: Int? = null,
)

@Serializable
data class Exercise(
    val id: String,
    val name: String,
    val cue: String = "",
    val rest: String = "",
    /** Come aumentare il carico quando chiudi il range, es. "+2,5 kg". */
    val increment: String = "",
    /** Una prescrizione per blocco; se mancano blocchi vale l'ultima. */
    val blocks: List<Prescription> = listOf(Prescription()),
)

@Serializable
data class Prescription(
    val sets: Int = 3,
    val repMin: Int = 8,
    val repMax: Int = 12,
    /** Es. "per lato", "per gamba". */
    val suffix: String = "",
    /** Variante dell'esercizio in questo blocco (nome diverso, storico separato). */
    val variant: String? = null,
    val variantCue: String? = null,
)

@Serializable
data class Habit(val id: String, val label: String)

@Serializable
data class TextSection(val title: String, val body: String, val open: Boolean = false)

/* ---------- Diario ---------- */

/** Il diario è legato alle date, non al programma: cambiare l'inizio non sposta i dati. */
@Serializable
data class TrainingLog(val days: Map<String, DayLog> = emptyMap())

@Serializable
data class DayLog(
    val done: Boolean = false,
    /** Sessione scelta al posto di quella prevista. */
    val session: String? = null,
    /** 1 = in linea, 2 = quasi, 3 = fuori. */
    val food: Int? = null,
    val habits: Set<String> = emptySet(),
    val weight: Double? = null,
    val waist: Double? = null,
    val note: String = "",
    val sets: Map<String, List<SetEntry>> = emptyMap(),
)

@Serializable
data class SetEntry(val kg: Double? = null, val reps: Int? = null) {
    val isEmpty: Boolean get() = kg == null && reps == null
}

/* ---------- Alimentazione ---------- */

@Serializable
enum class DayType { TRAIN, REST }

@Serializable
data class Nutrition(
    val bodyWeightKg: Double = 70.0,
    val train: DayTarget,
    val rest: DayTarget,
    val targetNote: String = "",
    val foods: List<Food>,
    val trainMeals: List<Meal>,
    val restMeals: List<Meal>,
    val substitutions: List<SubstitutionGroup> = emptyList(),
    val sections: List<TextSection> = emptyList(),
    val footer: String = "",
    val adjust: AdjustRule = AdjustRule(),
    val adjustments: List<Adjustment> = emptyList(),
) {
    fun food(id: String): Food? = foods.firstOrNull { it.id == id }
    fun target(day: DayType): DayTarget = if (day == DayType.TRAIN) train else rest
    fun meals(day: DayType): List<Meal> = if (day == DayType.TRAIN) trainMeals else restMeals

    fun withTarget(day: DayType, t: DayTarget): Nutrition =
        if (day == DayType.TRAIN) copy(train = t) else copy(rest = t)

    fun withMeals(day: DayType, m: List<Meal>): Nutrition =
        if (day == DayType.TRAIN) copy(trainMeals = m) else copy(restMeals = m)
}

/** Obiettivo giornaliero: i carboidrati sono ciò che resta delle calorie. */
@Serializable
data class DayTarget(val kcal: Int, val protein: Int, val fat: Int) {
    val carbs: Int get() = ((kcal - protein * 4 - fat * 9) / 4.0).roundToInt().coerceAtLeast(0)
}

@Serializable
enum class FoodRole { PROTEIN, CARBS, FAT, FIXED }

/** Valori per 100 g (o 100 ml). */
@Serializable
data class Food(
    val id: String,
    val name: String,
    val kcal: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val role: FoodRole = FoodRole.FIXED,
    val unit: String = "g",
    /** Se impostato, le quantità si mostrano a pezzi (es. "1 banana"). */
    val pieceName: String? = null,
    val piecePlural: String? = null,
    val pieceGrams: Double? = null,
    val inSubstitutions: Boolean = true,
)

@Serializable
data class MealItem(val foodId: String, val grams: Double)

@Serializable
data class Meal(
    val id: String,
    val time: String,
    val name: String,
    val items: List<MealItem> = emptyList(),
    val note: String = "",
    val alternativesTitle: String = "Alternative",
    val alternatives: List<String> = emptyList(),
    /** I pasti facoltativi non contano nei totali. */
    val optional: Boolean = false,
    /** Se falso, il ricalcolo delle quantità non tocca questo pasto. */
    val scalable: Boolean = true,
)

@Serializable
enum class SubBasis { PROTEIN, CARBS, FAT, KCAL }

/** Tabella di sostituzioni calcolata: stessi grammi di [basis] di [refGrams] di [refFoodId]. */
@Serializable
data class SubstitutionGroup(
    val title: String,
    val role: FoodRole,
    val basis: SubBasis,
    val refFoodId: String,
    val refGrams: Double,
    val note: String = "",
)

@Serializable
data class AdjustRule(
    val minWeeklyGain: Double = 0.1,
    val maxWeeklyGain: Double = 0.4,
    val waistAlarmCm: Double = 1.0,
    val stepKcal: Int = 150,
    val everyWeeks: Int = 2,
)

@Serializable
data class Adjustment(val date: String, val deltaKcal: Int)
