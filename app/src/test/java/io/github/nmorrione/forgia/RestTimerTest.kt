package io.github.nmorrione.forgia

import io.github.nmorrione.forgia.data.Defaults
import io.github.nmorrione.forgia.data.EndSound
import io.github.nmorrione.forgia.data.Store
import io.github.nmorrione.forgia.logic.parseRestSeconds
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RestTimerTest {

    @Test fun restTimesFromThePlan() {
        assertEquals(180, parseRestSeconds("3 min"))
        assertEquals(120, parseRestSeconds("2–3 min"))
        assertEquals(120, parseRestSeconds("2 min"))
        assertEquals(90, parseRestSeconds("90 s"))
        assertEquals(60, parseRestSeconds("60–90 s"))
        assertEquals(60, parseRestSeconds("60 s"))
        assertEquals(90, parseRestSeconds("90 s dopo la coppia"))
        assertEquals(90, parseRestSeconds("1:30"))
        assertEquals(90, parseRestSeconds("1,5 min"))
        assertNull(parseRestSeconds("superserie"))
        assertNull(parseRestSeconds(""))
    }

    @Test fun everyExerciseInTheDefaultPlanHasARestOrIsASuperset() {
        Defaults.program(Defaults.ORIGINAL_START).sessions.flatMap { it.exercises }.forEach { ex ->
            if (ex.rest != "superserie") {
                assert(parseRestSeconds(ex.rest) != null) { "${ex.name}: ${ex.rest}" }
            }
        }
    }

    @Test fun timerSettingsFromVersion040GetTheBeeper() {
        // Un file della 0.4.0 non ha endSound: si legge con il cicalino
        val obj = Json.parseToJsonElement(Store.encode(Defaults.appData(Defaults.ORIGINAL_START))).jsonObject
        val timer = JsonObject(obj.getValue("timer").jsonObject.filterKeys { it != "endSound" })
        val old = JsonObject(obj + ("timer" to timer))
        assertEquals(EndSound.BEEPER, Store.decode(old.toString()).timer.endSound)
    }
}
