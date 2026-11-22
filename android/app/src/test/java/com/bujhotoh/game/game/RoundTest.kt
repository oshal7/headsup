package com.bujhotoh.game.game

import com.bujhotoh.game.data.Card
import com.bujhotoh.game.data.ClueMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class RoundTest {
    private val cards = (1..40).map { Card("Word $it", ClueMode.SAY) }

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
        assertEquals(Answer(a, correct = false, timedOut = true), r.answers.single())
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

    @Test fun `result tiers scale with score`() {
        assertEquals(listOf(0, 0, 1, 1, 2, 3), listOf(0, 3, 4, 7, 8, 12).map(::resultTier))
    }

    @Test fun `undo brings the last card back and puts the next one back in the queue`() {
        val r = Round(cards, emptySet(), Random(7))
        val a = r.next(); r.answer(false)       // a pass fired while the phone was being put down
        val b = r.next()                        // the next card was already on screen
        val undone = r.undoLast()
        assertEquals(a.text, undone?.text)
        assertEquals(a, r.current)
        assertTrue(r.answers.isEmpty())
        r.answer(true)
        assertEquals(b, r.next())               // nothing skipped
        assertEquals(1, r.score)
    }

    @Test fun `undo with nothing answered does nothing`() {
        val r = Round(cards, emptySet(), Random(8))
        r.next()
        assertNull(r.undoLast())
    }
}
