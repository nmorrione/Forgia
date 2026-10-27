package io.github.nmorrione.forgia

import io.github.nmorrione.forgia.data.AppData
import io.github.nmorrione.forgia.data.DayLog
import io.github.nmorrione.forgia.data.DayType
import io.github.nmorrione.forgia.data.Defaults
import io.github.nmorrione.forgia.data.SetEntry
import io.github.nmorrione.forgia.data.Store
import io.github.nmorrione.forgia.data.TrainingLog
import io.github.nmorrione.forgia.logic.Advice
import io.github.nmorrione.forgia.logic.NutritionCalc
import io.github.nmorrione.forgia.logic.Schedule
import io.github.nmorrione.forgia.logic.Tracker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import kotlin.math.abs

class LogicTest {
    private val data = Defaults.appData(Defaults.ORIGINAL_START)
    private val s = Schedule(data.program)

    @Test fun scheduleMatchesHtml() {
        assertEquals(LocalDate.of(2026, 11, 23), s.start)
        assertEquals(56, s.days)
        assertEquals("pk", s.sessionAt(0, TrainingLog()).id)
        assertEquals("ua", s.sessionAt(1, TrainingLog()).id)
        assertEquals("lg", s.sessionAt(5, TrainingLog()).id)
        assertEquals("23–29 nov", s.weekRange(0))
        assertEquals("30 nov – 6 dic", s.weekRange(1))
        assertEquals("Lunedì e giovedì", s.weekdaysOf("pk"))
        assertEquals("Blocco 1 (RIR 3)", s.phaseLabel(0))
        assertEquals("Settimana di scarico", s.phaseLabel(4))
    }

    @Test fun prescriptionsPerBlockAndDeload() {
        val bench = data.program.session("ua")!!.exercises[0]
        assertEquals("4 × 5–7", s.resolve(bench, 0).text)
        assertEquals("2 × 5–7, carico −10%", s.resolve(bench, 4).text)
        assertEquals("5 × 4–6", s.resolve(bench, 5).text)
        val incline = data.program.session("ub")!!.exercises[0]
        val r = s.resolve(incline, 6)
        assertEquals("Panca inclinata con bilanciere", r.name)
        assertEquals("ub1~b1", r.logKey)
        assertEquals("ub1", s.resolve(incline, 1).logKey)
    }

    @Test fun defaultMealsMatchTargets() {
        val n = data.nutrition
        val train = NutritionCalc.dayMacros(n.trainMeals, n)
        val rest = NutritionCalc.dayMacros(n.restMeals, n)
        assertTrue("train kcal ${train.kcal}", abs(train.kcal - 2850) < 60)
        assertTrue("train protein ${train.protein}", abs(train.protein - 160) < 8)
        assertTrue("rest kcal ${rest.kcal}", abs(rest.kcal - 2500) < 60)
    }

    @Test fun scalingFollowsTargets() {
        val n = data.nutrition
        val target = n.train.copy(kcal = n.train.kcal + 300)
        val r = NutritionCalc.scaleMeals(n.trainMeals, n, target)
        assertTrue("after ${r.after.kcal}", abs(r.after.kcal - target.kcal) < 40)
        assertTrue(abs(r.after.protein - target.protein) < 8)
        val same = NutritionCalc.scaleMeals(n.trainMeals, n, n.train)
        assertTrue(abs(same.after.kcal - same.before.kcal) < 40)
    }

    @Test fun substitutionsAreComputed() {
        val n = data.nutrition
        val prot = NutritionCalc.substitutionRows(n.substitutions[0], n).associate { it.first.id to it.second }
        assertEquals(130.0, prot["pollo"]!!, 0.1)
        assertEquals(120.0, prot["tonno"]!!, 0.1)
        assertEquals(300.0, prot["yogurt"]!!, 0.1)
        val carbs = NutritionCalc.substitutionRows(n.substitutions[1], n).associate { it.first.id to it.second }
        assertEquals(80.0, carbs["riso"]!!, 0.1)
        assertEquals(75.0, carbs["gallette"]!!, 0.1)
    }

    @Test fun progressionAndHistory() {
        val bench = data.program.session("ua")!!.exercises[0]
        val log = TrainingLog(
            mapOf(
                "2026-11-24" to DayLog(sets = mapOf("ua1" to List(4) { SetEntry(60.0, 7) })),
            ),
        )
        val t = Tracker(s, log)
        val prev = t.lastSets("ua1", LocalDate.of(2026, 12, 1))!!
        assertEquals("60 kg × 7, 7, 7, 7", Tracker.formatSets(prev.sets))
        val hint = Tracker.progressionHint(prev.sets, s.resolve(bench, 1))!!
        assertTrue(hint.startsWith("Range chiuso"))
    }

    @Test fun calorieAdviceUpWhenFlat() {
        val days = mutableMapOf<String, DayLog>()
        for (i in 0 until 21) days[s.key(i)] = DayLog(weight = 70.0)
        val t = Tracker(s, TrainingLog(days))
        val a = t.calorieAdvice(s.dateAt(21), data.nutrition.adjust, emptyList())!!
        assertEquals(Advice.UP, a.kind)
        val n2 = NutritionCalc.applyCalorieChange(data.nutrition, a.deltaKcal, s.dateAt(21))
        assertEquals(3000, n2.train.kcal)
        val after = NutritionCalc.dayMacros(n2.meals(DayType.TRAIN), n2)
        assertTrue("after ${after.kcal}", abs(after.kcal - 3000) < 50)
    }

    @Test fun jsonRoundTrip() {
        val text = Store.encode(data)
        val back: AppData = Store.decode(text)
        assertEquals(data, back)
    }
}
