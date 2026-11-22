package com.bujhotoh.game.data

import org.json.JSONArray
import org.json.JSONObject

/** How clue-givers should give clues for a deck. */
enum class ClueMode(val key: String, val hint: String, val badge: String) {
    SAY("say", "", ""),
    ACT("act", "🎭 Act it out — no talking!", "🎭"),
    HUM("hum", "🎤 Hum it — no lyrics!", "🎤");

    companion object {
        fun from(key: String?): ClueMode = entries.firstOrNull { it.key == key } ?: SAY
    }
}

data class Deck(
    val id: String,
    val name: String,
    val hindi: String,
    val emoji: String,
    /** Gradient colours as ARGB ints. */
    val colorStart: Int,
    val colorEnd: Int,
    val desc: String,
    val mode: ClueMode,
    val words: List<String>,
    val custom: Boolean = false,
    val mix: Boolean = false,
)

/** One card shown during a round, with the small hint line under the word. */
data class Card(val text: String, val hint: String)

object Decks {
    const val MIX_ID = "mix"

    /** Parses assets/decks.json (generated from the web game's js/data.js by tools/build-decks.js). */
    fun parse(json: String): List<Deck> {
        val arr = JSONObject(json).getJSONArray("decks")
        return (0 until arr.length()).map { i -> deckFromJson(arr.getJSONObject(i), custom = false) }
    }

    fun mixOf(builtIn: List<Deck>): Deck = Deck(
        id = MIX_ID,
        name = "Mix Masala",
        hindi = "मिक्स मसाला",
        emoji = "🌶️",
        colorStart = parseColor("#ff0844"),
        colorEnd = parseColor("#ffb199"),
        desc = "A spicy mix from every deck. Anything can come up!",
        mode = ClueMode.SAY,
        words = builtIn.flatMap { it.words },
        mix = true,
    )

    /** The cards for a deck. Mix Masala tags each card with the deck it came from. */
    fun cardsFor(deck: Deck, builtIn: List<Deck>): List<Card> =
        if (deck.mix) {
            builtIn.flatMap { d ->
                val how = when (d.mode) {
                    ClueMode.ACT -> " · Act it"
                    ClueMode.HUM -> " · Hum it"
                    ClueMode.SAY -> ""
                }
                d.words.map { Card(it, "${d.emoji} ${d.name}$how") }
            }
        } else {
            deck.words.map { Card(it, deck.mode.hint) }
        }

    fun customToJson(decks: List<Deck>): String = JSONArray().apply {
        decks.forEach { d ->
            put(JSONObject().apply {
                put("id", d.id); put("name", d.name); put("emoji", d.emoji)
                put("colors", JSONArray().put(toHex(d.colorStart)).put(toHex(d.colorEnd)))
                put("mode", d.mode.key); put("desc", d.desc)
                put("words", JSONArray(d.words))
            })
        }
    }.toString()

    fun customFromJson(json: String?): List<Deck> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(json)
            (0 until arr.length()).map { deckFromJson(arr.getJSONObject(it), custom = true) }
        }.getOrDefault(emptyList())
    }

    private fun deckFromJson(o: JSONObject, custom: Boolean): Deck {
        val colors = o.getJSONArray("colors")
        val words = o.getJSONArray("words")
        return Deck(
            id = o.getString("id"),
            name = o.getString("name"),
            hindi = o.optString("hindi", ""),
            emoji = o.optString("emoji", "🎉"),
            colorStart = parseColor(colors.getString(0)),
            colorEnd = parseColor(colors.getString(1)),
            desc = o.optString("desc", ""),
            mode = ClueMode.from(o.optString("mode")),
            words = (0 until words.length()).map { words.getString(it) }.distinct(),
            custom = custom,
        )
    }

    /** "#rrggbb" -> opaque ARGB int (pure Kotlin so it works in unit tests). */
    fun parseColor(hex: String): Int = (0xFF000000L or hex.removePrefix("#").toLong(16)).toInt()

    private fun toHex(argb: Int) = "#%06x".format(argb and 0xFFFFFF)

    val CUSTOM_PALETTES = listOf(
        "#ee0979" to "#ff6a00", "#4776e6" to "#8e54e9", "#00c6ff" to "#0072ff",
        "#f7797d" to "#fbd786", "#43cea2" to "#185a9d",
    )
    val CUSTOM_EMOJIS = listOf("🎉", "👨‍👩‍👧", "🏢", "🎓", "🏖️", "🐶", "🍕", "💃", "🤪", "❤️", "🎮", "🏠")
}
