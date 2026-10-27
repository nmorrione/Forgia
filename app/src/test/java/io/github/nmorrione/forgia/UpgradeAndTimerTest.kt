package io.github.nmorrione.forgia

import io.github.nmorrione.forgia.data.DayLog
import io.github.nmorrione.forgia.data.Defaults
import io.github.nmorrione.forgia.data.IntervalConfig
import io.github.nmorrione.forgia.data.SCHEMA_VERSION
import io.github.nmorrione.forgia.data.SetEntry
import io.github.nmorrione.forgia.data.Store
import io.github.nmorrione.forgia.data.TimerSettings
import io.github.nmorrione.forgia.data.TrainingLog
import io.github.nmorrione.forgia.timer.Intervals
import io.github.nmorrione.forgia.timer.Phase
import io.github.nmorrione.forgia.timer.fmtClock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UpgradeAndTimerTest {

    /** Un file salvato dalla 0.1.0: senza timer e con schema 1. */
    private fun v1File(): String {
        val data = Defaults.appData().copy(
            log = TrainingLog(mapOf("2026-11-24" to DayLog(done = true, weight = 70.4, sets = mapOf("ua1" to listOf(SetEntry(60.0, 7)))))),
        )
        val obj = Json.parseToJsonElement(Store.encode(data)).jsonObject
        val old = JsonObject(obj.filterKeys { it != "timer" } + ("schema" to JsonPrimitive(1)))
        return old.toString()
    }

    @Test fun dataFromVersion010IsKept() {
        val loaded = Store.decode(v1File())
        assertEquals(SCHEMA_VERSION, loaded.schema)
        assertEquals(TimerSettings(), loaded.timer)
        val day = loaded.log.days.getValue("2026-11-24")
        assertEquals(70.4, day.weight!!, 0.0)
        assertEquals(listOf(SetEntry(60.0, 7)), day.sets["ua1"])
        assertEquals(Defaults.program(), loaded.program)
    }

    @Test fun unknownFieldsFromNewerVersionsAreIgnored() {
        val obj = Json.parseToJsonElement(Store.encode(Defaults.appData())).jsonObject
        val newer = JsonObject(obj + ("campoFuturo" to JsonPrimitive("x")))
        assertEquals(Defaults.program(), Store.decode(newer.toString()).program)
    }

    @Test fun intervalSteps() {
        val c = IntervalConfig(workSec = 40, restSec = 20, rounds = 3, sets = 2, setRestSec = 60, prepareSec = 10, exercises = listOf("A", "B"))
        val steps = Intervals.steps(c)
        assertEquals(
            listOf(
                Phase.PREPARE,
                Phase.WORK, Phase.REST, Phase.WORK, Phase.REST, Phase.WORK,
                Phase.SET_REST,
                Phase.WORK, Phase.REST, Phase.WORK, Phase.REST, Phase.WORK,
            ),
            steps.map { it.phase },
        )
        assertEquals(390_000L, Intervals.totalMs(c))
        // Esercizi a rotazione, e nel recupero si vede il prossimo
        assertEquals(listOf("A", "B", "A"), steps.filter { it.phase == Phase.WORK }.take(3).map { it.exercise })
        assertEquals("B", steps[2].next)
        assertNull(steps[5].next)
        assertEquals("A", steps[6].next)
    }

    @Test fun noRestMeansBackToBackWork() {
        val steps = Intervals.steps(IntervalConfig(workSec = 30, restSec = 0, rounds = 3, sets = 1, prepareSec = 0))
        assertEquals(listOf(Phase.WORK, Phase.WORK, Phase.WORK), steps.map { it.phase })
    }

    @Test fun clockFormat() {
        assertEquals("1:30", fmtClock(90_000))
        assertEquals("0:03", fmtClock(2_001))
        assertEquals("1:00:00", fmtClock(3_600_000))
    }
}
