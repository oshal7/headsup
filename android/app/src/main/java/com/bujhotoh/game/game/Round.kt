package com.bujhotoh.game.game

import com.bujhotoh.game.data.Card
import kotlin.random.Random

data class Answer(val text: String, val correct: Boolean, val timedOut: Boolean = false)

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
        _answers += Answer(c.text, correct)
        current = null
    }

    /** Time ran out: the card on screen counts as missed. */
    fun timeUp() {
        val c = current ?: return
        _answers += Answer(c.text, correct = false, timedOut = true)
        current = null
    }

    companion object { const val MIN_FRESH = 15 }
}

/** Funny result headline by score. */
fun resultTitle(score: Int, random: Random = Random.Default): String = when {
    score >= 12 -> listOf("Ekdum Jhakaas! 🔥", "Kya baat hai! 🔥", "Superstar! 🌟")
    score >= 8 -> listOf("Shabaash! 🎉", "Waah! 🎉", "Zabardast! 🎉")
    score >= 4 -> listOf("Not bad! 👍", "Accha hai! 👍", "Chalega! 👍")
    else -> listOf("Arre yaar! 😅", "Koi baat nahi 😅", "Agli baar pakka! 😅")
}.random(random)
