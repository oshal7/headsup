package com.bujhotoh.game.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DecksTest {
    private val builtIn = Decks.parse(File("src/main/assets/decks.json").readText())

    @Test fun `bundled decks load with cards and colours`() {
        assertEquals(16, builtIn.size)
        assertTrue(builtIn.all { it.words.size >= 40 && it.name.isNotBlank() && it.emoji.isNotBlank() })
        assertTrue(builtIn.sumOf { it.words.size } > 1000)
        assertTrue(builtIn.all { it.words.size == it.words.toSet().size })
    }

    @Test fun `act and hum decks carry their clue hint`() {
        val charades = builtIn.first { it.id == "act-it-out" }
        assertEquals(ClueMode.ACT, charades.mode)
        assertTrue(Decks.cardsFor(charades, builtIn).all { it.hint.contains("Act") })
        assertEquals(ClueMode.HUM, builtIn.first { it.id == "songs" }.mode)
    }

    @Test fun `mix masala contains every card tagged with its deck`() {
        val mix = Decks.mixOf(builtIn)
        val cards = Decks.cardsFor(mix, builtIn)
        assertEquals(builtIn.sumOf { it.words.size }, cards.size)
        assertTrue(cards.first { it.text == "Sholay" }.hint.contains("Bollywood"))
    }

    @Test fun `custom decks survive a save and load`() {
        val d = Deck("custom-1", "Gupta Family", "", "🏠", Decks.parseColor("#ee0979"), Decks.parseColor("#ff6a00"),
            "Your very own deck.", ClueMode.ACT, listOf("Bua ji", "Papa's scooter", "Goa trip", "Rajma", "Diwali 2019"), custom = true)
        val back = Decks.customFromJson(Decks.customToJson(listOf(d)))
        assertEquals(listOf(d), back)
    }

    @Test fun `bad custom deck data is ignored instead of crashing`() {
        assertEquals(emptyList<Deck>(), Decks.customFromJson("{not json"))
    }

    @Test fun `colour parsing`() {
        assertEquals(0xFFFF3D6E.toInt(), Decks.parseColor("#ff3d6e"))
    }
}
