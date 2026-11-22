package com.bujhotoh.game.game

import com.bujhotoh.game.data.Card
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class RoundTest {
    private val cards = (1..40).map { Card("Word $it", "") }

    @Test fun `scores correct answers and records passes`() {
        val r = Round(cards, emptySet(), Random(1))
        val a = r.next(); r.answer(true)
        val b = r.next(); r.answer(false)
        val c = r.next(); r.answer(true)
        assertEquals(2, r.score)
        assertEquals(listOf(a.text, b.text, c.text), r.answers.map { it.text })
        assertEquals(listOf(true, false, true), r.answers.map { it.correct })
    }

    @Test fun `time up marks the card on screen as missed`() {
        val r = Round(cards, emptySet(), Random(2))
        val a = r.next()
        r.timeUp()
        assertEquals(Answer(a.text, correct = false, timedOut = true), r.answers.single())
        assertNull(r.current)
    }

    @Test fun `answering twice for one card only counts once`() {
        val r = Round(cards, emptySet(), Random(3))
        r.next(); r.answer(true); r.answer(true)
        assertEquals(1, r.answers.size)
    }

    @Test fun `skips cards seen in earlier rounds`() {
        val seen = cards.take(20).map { it.text }.toSet()
        val r = Round(cards, seen, Random(4))
        val shown = List(20) { r.next().text.also { r.answer(false) } }
        assertTrue(shown.none { it in seen })
        assertFalse(r.deckReset)
    }

    @Test fun `starts over when almost every card has been seen`() {
        val seen = cards.take(30).map { it.text }.toSet() // only 10 fresh < 15
        val r = Round(cards, seen, Random(5))
        assertTrue(r.deckReset)
    }

    @Test fun `never runs out of cards`() {
        val r = Round(cards.take(5), emptySet(), Random(6))
        repeat(50) { r.next(); r.answer(false) }
        assertEquals(50, r.answers.size)
    }

    @Test fun `result titles scale with score`() {
        assertTrue(resultTitle(0, Random(1)).contains("😅"))
        assertTrue(resultTitle(12, Random(1)).let { it.contains("🔥") || it.contains("🌟") })
    }
}
