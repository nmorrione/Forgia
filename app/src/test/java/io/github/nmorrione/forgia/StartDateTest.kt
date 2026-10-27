package io.github.nmorrione.forgia

import io.github.nmorrione.forgia.data.DayLog
import io.github.nmorrione.forgia.data.Defaults
import io.github.nmorrione.forgia.data.TrainingLog
import io.github.nmorrione.forgia.logic.Schedule
import io.github.nmorrione.forgia.logic.Tracker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class StartDateTest {

    @Test fun freshInstallStartsOnFirstLaunch() {
        val day = LocalDate.of(2027, 3, 10) // un mercoledì
        val d = Defaults.appData(day)
        assertEquals("2027-03-10", d.program.startDate)
        // Senza il 23 novembre non si parla di Natale
        assertFalse(d.guide.any { it.title == "Le feste" })
        assertFalse(d.program.weekPlan.any { it.note.contains("Natale") })
        // Il primo giorno segue la settimana tipo: mercoledì è riposo
        assertEquals("rest", Schedule(d.program).sessionAt(0, TrainingLog()).id)
    }

    @Test fun originalStartKeepsTheHolidayTexts() {
        val d = Defaults.appData(Defaults.ORIGINAL_START)
        assertTrue(d.guide.any { it.title == "Le feste" })
        assertTrue(d.program.weekPlan[4].note.contains("Natale"))
    }

    @Test fun preparationDaysBeforeTheStart() {
        val p = Defaults.program(Defaults.ORIGINAL_START)
        val s = Schedule(p)
        val today = LocalDate.of(2026, 10, 27)
        assertEquals(-27, s.indexOf(today))
        assertEquals(-27, s.firstDay(today, TrainingLog()))
        assertEquals("Preparazione (RIR 3)", s.phaseLabel(s.weekOf(-27)))
        // In preparazione valgono i carichi della prima settimana
        val bench = p.session("ua")!!.exercises[0]
        assertEquals("4 × 5–7", s.resolve(bench, s.weekOf(-27)).text)
        // 27 ottobre 2026 è martedì: Upper A
        assertEquals("ua", s.sessionAt(-27, TrainingLog()).id)
    }

    @Test fun preparationSummaryAndHistory() {
        val s = Schedule(Defaults.program(Defaults.ORIGINAL_START))
        val log = TrainingLog(
            mapOf(
                "2026-10-27" to DayLog(done = true, weight = 70.0),
                "2026-10-28" to DayLog(weight = 70.4),
                "2026-11-24" to DayLog(weight = 71.0),
            ),
        )
        val t = Tracker(s, log)
        val prep = t.prepSummary()!!
        assertEquals(1, prep.workouts)
        assertEquals(2, prep.daysLogged)
        assertEquals(70.2, prep.avgWeight!!, 1e-9)
        // Dopo l'inizio si può ancora tornare al primo giorno segnato
        assertEquals(-27, s.firstDay(LocalDate.of(2026, 12, 1), log))
    }
}
