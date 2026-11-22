package com.bujhotoh.game.game

import com.bujhotoh.game.data.Card
import kotlin.random.Random

data class Answer(val card: Card, val correct: Boolean, val timedOut: Boolean = false) {
    val text: String get() = card.text
}

/**
 * One round's card queue and answers. Pure logic, no Android.
 *
 * Cards already seen in earlier rounds are skipped until fewer than [MIN_FRESH]
 * remain; then the deck starts over (and [deckReset] is true so the caller can clear "seen").
 */
class Round(
    private val cards: List<Card>,
    seen: Set<String>,
    private val random: Random = Random.Default,
) {
    private val queue = ArrayDeque<Card>()
    private val _answers = mutableListOf<Answer>()
    val answers: List<Answer> get() = _answers
    val score: Int get() = _answers.count { it.correct }
    var current: Card? = null; private set
    var deckReset = false; private set

    init { refill(seen) }

    private fun refill(seen: Set<String>) {
        var fresh = cards.filter { it.text !in seen }
        if (fresh.size < MIN_FRESH) { fresh = cards; deckReset = true }
        queue.addAll(fresh.shuffled(random))
    }

    /** Shows the next card (refilling from the whole deck if a very long round runs out). */
    fun next(): Card {
        if (queue.isEmpty()) refill(emptySet())
        return queue.removeFirst().also { current = it }
    }

    fun answer(correct: Boolean) {
        val c = current ?: return
        _answers += Answer(c, correct)
        current = null
    }

    /**
     * Takes back the last answer (e.g. the "pass" fired while the phone was being put down on a bed).
     * Its card becomes current again; any card already shown after it goes back on top of the queue.
     */
    fun undoLast(): Answer? {
        val a = _answers.removeLastOrNull() ?: return null
        current?.let { queue.addFirst(it) }
        current = a.card
        return a
    }

    /** Time ran out: the card on screen counts as missed. */
    fun timeUp() {
        val c = current ?: return
        _answers += Answer(c, correct = false, timedOut = true)
        current = null
    }

    companion object { const val MIN_FRESH = 15 }
}

/** 0 = warm-up, 1 = nice, 2 = brilliant, 3 = superstar. The UI picks a fun title for each tier. */
fun resultTier(score: Int): Int = when {
    score >= 12 -> 3
    score >= 8 -> 2
    score >= 4 -> 1
    else -> 0
}
