package com.bujhotoh.game.game

import com.bujhotoh.game.data.Sensitivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

class TiltLogicTest {
    private val g = 9.81f

    /**
     * Gravity in screen coordinates for a phone held on the forehead (landscape, content upright),
     * tipped forward by [deg] degrees: positive = screen towards the floor (nod), negative = towards the ceiling.
     */
    private fun head(deg: Double): FloatArray {
        val r = Math.toRadians(deg)
        return floatArrayOf(0f, (g * cos(r)).toFloat(), (-g * sin(r)).toFloat())
    }

    private fun TiltLogic.feed(deg: Double, times: Int = 6): List<TiltEvent> {
        val out = mutableListOf<TiltEvent>()
        repeat(times) { val v = head(deg); onGravity(v[0], v[1], v[2])?.let(out::add) }
        return out
    }

    @Test fun `phone upright on forehead is detected`() {
        val t = TiltLogic()
        t.feed(0.0)
        assertTrue(t.isOnForehead())
    }

    @Test fun `phone lying flat is not on a forehead`() {
        val t = TiltLogic()
        repeat(5) { t.onGravity(0f, 0f, g) }
        assertFalse(t.isOnForehead())
    }

    @Test fun `nothing fires before the round is enabled`() {
        val t = TiltLogic()
        assertTrue(t.feed(70.0).isEmpty())
    }

    @Test fun `nod down is correct, tilt back is pass`() {
        val t = TiltLogic()
        t.feed(0.0); t.enable()
        assertEquals(listOf(TiltEvent.CORRECT), t.feed(60.0))
        t.feed(0.0)
        assertEquals(listOf(TiltEvent.PASS), t.feed(-60.0))
    }

    @Test fun `one long nod counts once until the phone comes back to neutral`() {
        val t = TiltLogic()
        t.feed(0.0); t.enable()
        assertEquals(1, t.feed(70.0, times = 40).size)
        assertTrue(t.feed(25.0).isEmpty())      // still tipped: not re-armed
        t.feed(0.0)
        assertEquals(listOf(TiltEvent.CORRECT), t.feed(70.0))
    }

    @Test fun `small head movements do not trigger`() {
        val t = TiltLogic()
        t.feed(0.0); t.enable()
        assertTrue(t.feed(20.0).isEmpty())
        assertTrue(t.feed(-20.0).isEmpty())
    }

    @Test fun `high sensitivity fires on smaller tilts than low`() {
        val high = TiltLogic(Sensitivity.HIGH).apply { feed(0.0); enable() }
        val low = TiltLogic(Sensitivity.LOW).apply { feed(0.0); enable() }
        assertEquals(listOf(TiltEvent.CORRECT), high.feed(30.0))
        assertTrue(low.feed(30.0).isEmpty())
    }

    @Test fun `calibrates to a slightly tilted resting position`() {
        val t = TiltLogic()
        t.feed(-15.0); t.enable()                 // head resting slightly back
        assertTrue(t.feed(-30.0).isEmpty())      // 15° more back: within neutral band
        assertEquals(listOf(TiltEvent.PASS), t.feed(-60.0))
    }

    @Test fun `zero gravity reading is ignored`() {
        assertNull(TiltLogic().onGravity(0f, 0f, 0f))
    }

    /** Feeds a head movement from [from]° to [to]° over [ms] milliseconds at 50 Hz, with timestamps. */
    private fun TiltLogic.move(from: Double, to: Double, ms: Long, startNanos: Long = 1_000_000_000L): List<TiltEvent> {
        val out = mutableListOf<TiltEvent>()
        val steps = (ms / 20).toInt().coerceAtLeast(1)
        for (i in 0..steps) {
            val v = head(from + (to - from) * i / steps)
            onGravity(v[0], v[1], v[2], startNanos + i * 20_000_000L)?.let(out::add)
        }
        return out
    }

    @Test fun `a quick nod counts before reaching the full angle`() {
        val t = TiltLogic()
        t.feed(0.0); t.enable()
        // 27° is below the medium threshold (~33°) but done in 150 ms: a natural snappy nod
        assertEquals(listOf(TiltEvent.CORRECT), t.move(0.0, 27.0, ms = 150))
    }

    @Test fun `a slow drift to the same angle does not count`() {
        val t = TiltLogic()
        t.feed(0.0); t.enable()
        assertTrue(t.move(0.0, 27.0, ms = 2_000).isEmpty())
    }

    @Test fun `a quick head-back flick passes`() {
        val t = TiltLogic()
        t.feed(0.0); t.enable()
        assertEquals(listOf(TiltEvent.PASS), t.move(0.0, -27.0, ms = 150))
    }

    @Test fun `does not count as on the forehead while the phone is moving`() {
        val t = TiltLogic()
        t.feed(0.0)
        repeat(20) { t.onRotationSpeed(2.5f, 0.4f, 0.1f) }   // being waved around
        assertFalse(t.isOnForehead())
        repeat(40) { t.onRotationSpeed(0.05f, 0.02f, 0.01f) } // now held still
        assertTrue(t.isOnForehead())
    }

    @Test fun `phones without a gyroscope only need the position`() {
        val t = TiltLogic()
        t.feed(0.0)
        assertTrue(t.isSteady())
        assertTrue(t.isOnForehead())
    }
}
