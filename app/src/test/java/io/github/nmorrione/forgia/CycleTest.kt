package io.github.nmorrione.forgia

import io.github.nmorrione.forgia.data.DayLog
import io.github.nmorrione.forgia.data.Defaults
import io.github.nmorrione.forgia.data.Goal
import io.github.nmorrione.forgia.data.SetEntry
import io.github.nmorrione.forgia.data.TrainingLog
import io.github.nmorrione.forgia.logic.CycleReports
import io.github.nmorrione.forgia.logic.NextCycle
import io.github.nmorrione.forgia.logic.NextCycleOptions
import io.github.nmorrione.forgia.logic.Schedule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import kotlin.math.abs

class CycleTest {
    private val base = Defaults.appData(Defaults.ORIGINAL_START)
    private val s = Schedule(base.program)
    private val end = LocalDate.of(2027, 1, 17)

    /** 8 settimane a +0,25 kg/settimana, vita +1 cm, panca da 60×7 a 70×6. */
    private fun log(waistEnd: Double = 77.0): TrainingLog {
        val days = mutableMapOf<String, DayLog>()
        for (i in 0 until 56) {
            val w = i / 7
            days[s.key(i)] = DayLog(weight = 70.0 + 0.25 * w, done = !s.sessionAt(i, TrainingLog()).rest)
        }
        days[s.key(6)] = days.getValue(s.key(6)).copy(waist = 76.0)
        days[s.key(55)] = days.getValue(s.key(55)).copy(waist = waistEnd)
        days[s.key(1)] = days.getValue(s.key(1)).copy(sets = mapOf("ua1" to listOf(SetEntry(60.0, 7), SetEntry(60.0, 6))))
        days[s.key(50)] = days.getValue(s.key(50)).copy(sets = mapOf("ua1" to listOf(SetEntry(70.0, 6))))
        return TrainingLog(days)
    }

    private fun report(l: TrainingLog) = CycleReports.build(
        base.program, base.nutrition.train, base.nutrition.rest, emptyList(), l, end,
    )

    @Test fun reportAndMaintenance() {
        val r = report(log())
        assertTrue(r.finished)
        assertEquals(1.75, r.weightDelta!!, 1e-9)
        assertEquals(0.25, r.weeklyRate!!, 1e-9)
        assertEquals(1.0, r.waistDelta!!, 1e-9)
        assertEquals(40, r.done)
        // Piano: 5 giorni a 2.850 e 2 a 2.500 → 2.750; surplus 0,25 × 7700 / 7 = 275
        assertEquals(2750.0, r.avgKcal, 1e-6)
        assertEquals(2475.0, r.maintenance!!, 1e-6)
        val bench = r.exercises.first { it.name == "Panca piana con bilanciere" }
        assertEquals("60 kg × 7", bench.firstText)
        assertEquals("70 kg × 6", bench.bestText)
        assertTrue(bench.pct > 10)
        assertEquals(Goal.BULK, r.recommended)
    }

    @Test fun waistGrowingTooFastSuggestsCut() {
        assertEquals(Goal.CUT, report(log(waistEnd = 79.5)).recommended)
    }

    @Test fun planWithDeloadFirst() {
        val plan = NextCycle.plan(Goal.BULK, 8, deloadFirst = true, blocks = 2)
        assertEquals(8, plan.size)
        assertTrue(plan[0].deload)
        assertEquals(listOf("3", "2", "1"), plan.subList(1, 4).map { it.rir })
        assertTrue(plan[4].deload)
        assertEquals(1, plan[5].block)
        assertEquals(6, NextCycle.plan(Goal.CUT, 6, deloadFirst = false, blocks = 2).size)
    }

    @Test fun startsTheMondayAfterTheEnd() {
        assertEquals(LocalDate.of(2027, 1, 18), NextCycle.defaultStart(base.program, end))
        assertEquals(LocalDate.of(2027, 1, 25), NextCycle.defaultStart(base.program, LocalDate.of(2027, 1, 20)))
    }

    @Test fun nextCycleArchivesAndRetargets() {
        val data = base.copy(log = log())
        val r = report(data.log)
        val next = NextCycle.apply(data, NextCycleOptions(Goal.BULK, "Massa, ciclo 2", LocalDate.of(2027, 1, 18), 8, true), r, end)
        assertEquals(1, next.cycles.size)
        assertEquals(base.program, next.cycles[0].program)
        assertEquals("2027-01-18", next.program.startDate)
        assertEquals(Goal.BULK, next.program.goal)
        // Mantenimento 2.475 + 250 = 2.725 di media: allenamento 2.825 (arrotondato a 2.830), riposo 350 in meno
        assertEquals(2830, next.nutrition.train.kcal)
        assertEquals(2480, next.nutrition.rest.kcal)
        assertEquals(71.8, next.nutrition.bodyWeightKg, 1e-9)
        assertTrue(next.nutrition.adjustments.isEmpty())
        // Il diario non si tocca
        assertEquals(data.log, next.log)
        // I pasti seguono il nuovo obiettivo
        val day = io.github.nmorrione.forgia.logic.NutritionCalc.dayMacros(next.nutrition.trainMeals, next.nutrition)
        assertTrue("kcal ${day.kcal}", abs(day.kcal - 2830) < 60)
    }

    @Test fun cutRaisesProteinAndLowersCalories() {
        val data = base.copy(log = log())
        val r = report(data.log)
        val next = NextCycle.apply(data, NextCycleOptions(Goal.CUT, "Definizione", LocalDate.of(2027, 1, 18), 6, false), r, end)
        assertTrue(next.nutrition.train.kcal < base.nutrition.train.kcal - 300)
        assertTrue(next.nutrition.train.protein >= 170)
        assertTrue(next.nutrition.adjust.maxWeeklyGain < 0)
    }

    @Test fun afterTheEndYouCanStillLog() {
        val after = LocalDate.of(2027, 1, 25)
        assertEquals(s.indexOf(after), s.lastDay(after, TrainingLog()))
        assertEquals("Dopo la fine del ciclo", s.phaseLabel(s.weekOf(s.indexOf(after))))
        assertNotNull(s.sessionAt(s.indexOf(after), TrainingLog()))
    }
}
