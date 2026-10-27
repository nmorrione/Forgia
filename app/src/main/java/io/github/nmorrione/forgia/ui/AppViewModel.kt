package io.github.nmorrione.forgia.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.nmorrione.forgia.data.AppData
import io.github.nmorrione.forgia.data.DayLog
import io.github.nmorrione.forgia.data.DayType
import io.github.nmorrione.forgia.data.Nutrition
import io.github.nmorrione.forgia.data.Program
import io.github.nmorrione.forgia.data.Store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

enum class Tab(val label: String) {
    OGGI("Oggi"), CALENDARIO("Calendario"), SCHEDE("Schede"), TIMER("Timer"), CIBO("Cibo"), GUIDA("Guida")
}

enum class SectionKind(val title: String) { GUIDE("Testi della guida"), PROGRAM_INFO("Testi delle schede"), NUTRITION("Testi dell’alimentazione") }

/** Schermate di modifica, sopra le cinque sezioni principali. */
sealed interface Route {
    data object ProgramEdit : Route
    data class SessionEdit(val id: String) : Route
    data class ExerciseEdit(val sessionId: String, val exerciseId: String) : Route
    data object TargetsEdit : Route
    data class MealEdit(val day: DayType, val mealId: String) : Route
    data object Foods : Route
    data class FoodEdit(val id: String) : Route
    data object SubstitutionsEdit : Route
    data class SectionsEdit(val kind: SectionKind) : Route
}

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val store = Store(File(app.filesDir, "forgia.json"))

    /** Stato Compose sincrono: i campi di testo vedono subito ogni modifica. */
    var data by mutableStateOf(store.load())
        private set

    var tab by mutableStateOf(Tab.OGGI)
    /** Giorno selezionato nella vista Oggi; null = oggi. */
    var selectedDay by mutableStateOf<Int?>(null)
    var planTab by mutableStateOf<String?>(null)
    var foodDay by mutableStateOf(DayType.TRAIN)
    var timerIntervals by mutableStateOf(false)
    val stack = mutableStateListOf<Route>()

    private var saveJob: Job? = null

    fun update(f: (AppData) -> AppData) {
        data = f(data)
        val snapshot = data
        saveJob?.cancel()
        saveJob = viewModelScope.launch(Dispatchers.IO) {
            delay(400)
            store.save(snapshot)
        }
    }

    fun updateProgram(f: (Program) -> Program) = update { it.copy(program = f(it.program)) }
    fun updateNutrition(f: (Nutrition) -> Nutrition) = update { it.copy(nutrition = f(it.nutrition)) }

    fun updateDay(key: String, f: (DayLog) -> DayLog) = update { d ->
        val cur = d.log.days[key] ?: DayLog()
        val next = f(cur)
        val days = if (next == DayLog()) d.log.days - key else d.log.days + (key to next)
        d.copy(log = d.log.copy(days = days))
    }

    fun replaceAll(newData: AppData) = update { newData }

    fun saveNow() {
        saveJob?.cancel()
        store.save(data)
    }

    fun push(r: Route) { stack.add(r) }
    fun pop() { if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex) }

    override fun onCleared() {
        saveNow()
        super.onCleared()
    }
}
