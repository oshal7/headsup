package com.bujhotoh.game.data

import android.content.Context
import org.json.JSONObject

enum class Sensitivity { LOW, MEDIUM, HIGH }
enum class MusicStyle(val emoji: String) { CHILL("☕"), CLASSIC("🪕"), PARTY("🥁") }

data class Settings(
    val music: Boolean = true,
    val musicStyle: MusicStyle = MusicStyle.CHILL,
    val sfx: Boolean = true,
    val vibration: Boolean = true,
    val sensitivity: Sensitivity = Sensitivity.MEDIUM,
    val roundSeconds: Int = 60,
    /** Region id the player chose on first launch ("world" = somewhere else); null = not asked yet. */
    val homeRegion: String? = null,
)

/** Everything the game remembers, stored locally in SharedPreferences. Nothing leaves the device. */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("bujho", Context.MODE_PRIVATE)

    fun loadSettings(): Settings = Settings(
        music = sp.getBoolean("music", true),
        musicStyle = enumOr(sp.getString("musicStyle", null), MusicStyle.CHILL),
        sfx = sp.getBoolean("sfx", true),
        vibration = sp.getBoolean("vibration", true),
        sensitivity = enumOr(sp.getString("sensitivity", null), Sensitivity.MEDIUM),
        roundSeconds = sp.getInt("roundSeconds", 60),
        homeRegion = sp.getString("homeRegion", null),
    )

    fun saveSettings(s: Settings) = sp.edit()
        .putBoolean("music", s.music)
        .putString("musicStyle", s.musicStyle.name)
        .putBoolean("sfx", s.sfx)
        .putBoolean("vibration", s.vibration)
        .putString("sensitivity", s.sensitivity.name)
        .putInt("roundSeconds", s.roundSeconds)
        .putString("homeRegion", s.homeRegion)
        .apply()

    fun loadBest(): Map<String, Int> = readMap("best") { o, k -> o.getInt(k) }
    fun saveBest(best: Map<String, Int>) = sp.edit().putString("best", JSONObject(best).toString()).apply()

    fun loadSeen(): Map<String, Set<String>> = readMap("seen") { o, k ->
        val a = o.getJSONArray(k); (0 until a.length()).map { a.getString(it) }.toSet()
    }
    fun saveSeen(seen: Map<String, Set<String>>) =
        sp.edit().putString("seen", JSONObject(seen.mapValues { it.value.toList() }).toString()).apply()

    /** Last country picked per category, e.g. movies -> us. */
    fun loadRegionChoices(): Map<String, String> = readMap("regionChoice") { o, k -> o.getString(k) }
    fun saveRegionChoices(m: Map<String, String>) = sp.edit().putString("regionChoice", JSONObject(m).toString()).apply()

    fun loadCustomDecks(): List<Deck> = Decks.customFromJson(sp.getString("custom", null))
    fun saveCustomDecks(decks: List<Deck>) = sp.edit().putString("custom", Decks.customToJson(decks)).apply()

    private fun <T> readMap(key: String, read: (JSONObject, String) -> T): Map<String, T> = runCatching {
        val o = JSONObject(sp.getString(key, null) ?: return emptyMap())
        o.keys().asSequence().associateWith { read(o, it) }
    }.getOrDefault(emptyMap())

    private inline fun <reified E : Enum<E>> enumOr(name: String?, default: E): E =
        enumValues<E>().firstOrNull { it.name == name } ?: default
}
