package com.bujhotoh.game.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ContentTest {
    private val content = Content.parse(File("src/main/assets/content.json").readText())
    private fun cat(id: String) = content.categories.first { it.id == id }

    @Test fun `content loads with regions and plenty of cards`() {
        assertEquals(listOf("world", "in", "us", "gb", "cn"), content.regions.map { it.id })
        assertTrue(content.categories.size >= 15)
        val total = content.categories.sumOf { c -> c.variants.values.sumOf { it.size } }
        assertTrue("only $total cards", total > 2000)
        content.categories.forEach { c ->
            c.variants.forEach { (r, words) ->
                assertTrue("${c.id}/$r too small", words.size >= 20)
                assertEquals("${c.id}/$r has duplicates", words.size, words.toSet().size)
            }
        }
    }

    @Test fun `every category sits on a home-screen shelf`() {
        assertEquals(listOf("party", "act", "pop", "family", "culture"), content.groups.map { it.id })
        content.categories.forEach { c -> assertTrue("${c.id} has no shelf", content.groups.any { it.id == c.group }) }
        assertEquals(content.categories.size, content.groups.sumOf { content.categoriesIn(it).size })
        assertEquals(ClueMode.SOUND, cat("sounds").mode)
        assertTrue(cat("famous").isUniversal)
    }

    @Test fun `animals are universal, movies need a country`() {
        assertTrue(cat("animals").isUniversal)
        assertFalse(cat("movies").isUniversal)
        assertEquals(setOf("in", "us", "gb", "cn"), cat("movies").variants.keys)
        assertTrue("Sholay" in cat("movies").variants.getValue("in"))
        assertTrue("Titanic" in cat("movies").variants.getValue("us"))
    }

    @Test fun `default country prefers last choice, then home, then World`() {
        val movies = cat("movies"); val food = cat("food")
        assertEquals("us", content.defaultRegion(movies, home = "in", lastChoice = "us").id)
        assertEquals("in", content.defaultRegion(movies, home = "in", lastChoice = null).id)
        assertEquals("world", content.defaultRegion(food, home = "fr", lastChoice = null).id)
        assertEquals("in", content.defaultRegion(movies, home = "world", lastChoice = null).id) // no World movies: first country
    }

    @Test fun `deck for a category and country`() {
        val d = content.deckFor(cat("movies"), content.region("gb")!!)
        assertEquals("movies:gb", d.id)
        assertEquals("Movies 🇬🇧", d.title)
        assertTrue("Notting Hill" in d.words)
    }

    @Test fun `mix has every universal card plus only the home country`() {
        val mixIndia = content.mixCards("in").map { it.text }.toSet()
        assertTrue("Elephant" in mixIndia)
        assertTrue("Sholay" in mixIndia)
        assertFalse("Titanic" in mixIndia)
        val mixWorld = content.mixCards("world").map { it.text }.toSet()
        assertFalse("Sholay" in mixWorld)
        assertTrue(content.mixCards("us").first { it.text == "Titanic" }.source.contains("🇺🇸"))
    }

    @Test fun `act and hum categories carry their clue mode`() {
        assertEquals(ClueMode.ACT, cat("act").mode)
        assertEquals(ClueMode.HUM, cat("songs").mode)
        assertTrue(content.cardsFor(content.deckFor(cat("songs"), content.region("us")!!), "us").all { it.mode == ClueMode.HUM })
    }

    @Test fun `custom decks survive a save and load`() {
        val d = Deck("custom-1", "Family", "🏠", Decks.parseColor("#ee0979"), Decks.parseColor("#ff6a00"),
            "Your very own deck.", ClueMode.ACT, listOf("Grandma's soup", "Dad's car", "Beach trip", "Pizza night", "The cat"), custom = true)
        assertEquals(listOf(d), Decks.customFromJson(Decks.customToJson(listOf(d))))
    }

    @Test fun `bad custom deck data is ignored instead of crashing`() {
        assertEquals(emptyList<Deck>(), Decks.customFromJson("{not json"))
    }

    @Test fun `colour parsing`() {
        assertEquals(0xFFFF3D6E.toInt(), Decks.parseColor("#ff3d6e"))
    }
}
