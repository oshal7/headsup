package com.bujhotoh.game

import android.app.Application
import android.content.Context
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bujhotoh.game.audio.Haptics
import com.bujhotoh.game.audio.MusicRole
import com.bujhotoh.game.audio.Sfx
import com.bujhotoh.game.audio.SoundManager
import com.bujhotoh.game.data.Card
import com.bujhotoh.game.data.Category
import com.bujhotoh.game.data.ClueMode
import com.bujhotoh.game.data.Content
import com.bujhotoh.game.data.Deck
import com.bujhotoh.game.data.Decks
import com.bujhotoh.game.data.Prefs
import com.bujhotoh.game.data.Region
import com.bujhotoh.game.data.Settings
import com.bujhotoh.game.game.Answer
import com.bujhotoh.game.game.Round
import com.bujhotoh.game.game.TiltEvent
import com.bujhotoh.game.game.TiltLogic
import com.bujhotoh.game.game.TiltSensor
import com.bujhotoh.game.game.resultTier
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class Screen { HOME, READY, PLAY, RESULTS }

sealed interface Sheet {
    data object None : Sheet
    data object Welcome : Sheet
    /** A built-in category: pick a country (if it has several) and play. */
    data class CategoryPick(val category: Category) : Sheet
    /** Mix or a custom deck. */
    data class DeckInfo(val deck: Deck) : Sheet
    data object Settings : Sheet
    data object HowTo : Sheet
    data class CustomEditor(val editing: Deck?) : Sheet
}

enum class Flash { CORRECT, PASS, TIME_UP }

/** Why a round is paused. */
enum class PauseReason { APP, OFF_FOREHEAD }

data class ResultsUi(
    val deck: Deck,
    val score: Int,
    /** 0..3, see [resultTier]. */
    val tier: Int,
    val isBest: Boolean,
    val previousBest: Int,
    val answers: List<Answer>,
)

class GameViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = Prefs(app)
    val sounds = SoundManager(app)
    private val haptics = Haptics(app)
    val tilt = TiltLogic()
    private val tiltSensor = TiltSensor(app, tilt)

    val content: Content = Content.parse(app.assets.open("content.json").bufferedReader().use { it.readText() })

    var settings by mutableStateOf(prefs.loadSettings()); private set
    var customDecks by mutableStateOf(prefs.loadCustomDecks()); private set
    var best by mutableStateOf(prefs.loadBest()); private set
    private var regionChoices by mutableStateOf(prefs.loadRegionChoices())
    private val seen = prefs.loadSeen().mapValues { it.value.toMutableSet() }.toMutableMap()

    val hasSensor: Boolean get() = tiltSensor.isAvailable
    val sensorNameRes: Int get() = tiltSensor.sourceNameRes

    var screen by mutableStateOf(Screen.HOME); private set
    var sheet by mutableStateOf<Sheet>(Sheet.None); private set
    var deck by mutableStateOf<Deck?>(null); private set

    /** 0..1 while the phone is held still on the forehead, before the round auto-starts. */
    var readyProgress by mutableStateOf(0f); private set
    var tiltTest by mutableStateOf(false); private set

    // Play screen
    var countdown by mutableStateOf<Int?>(null); private set   // 3, 2, 1, 0 (= "Go!"), null when not counting
    var card by mutableStateOf<Card?>(null); private set
    var cardKey by mutableIntStateOf(0); private set            // changes on every new card (drives the pop-in animation)
    var score by mutableIntStateOf(0); private set
    var remainingMs by mutableLongStateOf(0L); private set
    var totalMs by mutableLongStateOf(60_000L); private set
    var flash by mutableStateOf<Flash?>(null); private set
    var pauseReason by mutableStateOf<PauseReason?>(null); private set
    val paused: Boolean get() = pauseReason != null
    var confirmQuit by mutableStateOf(false); private set
    /** Incremented to fire a confetti burst. */
    var celebrate by mutableIntStateOf(0); private set

    var results by mutableStateOf<ResultsUi?>(null); private set

    private var round: Round? = null
    private var running = false
    private var locked = true
    private var endAt = 0L
    private var lastSecond = -1
    private var lastAnswerAt = 0L
    private var lastAnswerCorrect = false
    private var resumed = false
    private var sensorContext: Context? = null
    private var readyJob: Job? = null
    private var gameJob: Job? = null
    private var answerJob: Job? = null

    init {
        applySettings(settings)
        tiltSensor.onEvent = { ev -> answer(ev == TiltEvent.CORRECT) }
        sounds.onInterrupted = { pauseRound(PauseReason.APP) }
        sounds.playMusic(MusicRole.MENU)
        if (settings.homeRegion == null) sheet = Sheet.Welcome
    }

    private fun str(id: Int, vararg args: Any): String = getApplication<Application>().getString(id, *args)

    /* ---------------- Settings ---------------- */

    fun updateSettings(transform: (Settings) -> Settings) {
        val s = transform(settings)
        settings = s
        prefs.saveSettings(s)
        applySettings(s)
    }

    private fun applySettings(s: Settings) {
        sounds.sfxEnabled = s.sfx
        sounds.musicEnabled = s.music
        sounds.style = s.musicStyle
        haptics.enabled = s.vibration
        tilt.setSensitivity(s.sensitivity)
    }

    fun setHomeRegion(id: String) {
        tap()
        updateSettings { it.copy(homeRegion = id) }
        if (sheet == Sheet.Welcome) sheet = Sheet.None
    }

    fun resetSeen() { seen.clear(); prefs.saveSeen(emptyMap()) }

    /* ---------------- Decks & countries ---------------- */

    val homeRegion: String? get() = settings.homeRegion

    /** Country packs sorted with the player's home country first. */
    fun regionsFor(c: Category): List<Region> =
        content.regionsFor(c).sortedBy { r -> when { r.isWorld -> 0; r.id == homeRegion -> 1; else -> 2 } }

    fun selectedRegion(c: Category): Region = content.defaultRegion(c, homeRegion, regionChoices[c.id])

    fun chooseRegion(c: Category, r: Region) {
        tap()
        regionChoices = regionChoices + (c.id to r.id)
        prefs.saveRegionChoices(regionChoices)
    }

    fun mixDeck(): Deck = content.mixDeck(homeRegion, str(R.string.mix_name), str(R.string.mix_desc))

    fun bestFor(c: Category): Int? = c.variants.keys.mapNotNull { best["${c.id}:$it"] }.maxOrNull()

    /* ---------------- Navigation ---------------- */

    fun tap() = sounds.play(Sfx.TAP)

    fun openCategory(c: Category) { tap(); sheet = Sheet.CategoryPick(c) }
    fun openDeck(d: Deck) { tap(); deck = d; sheet = Sheet.DeckInfo(d) }
    fun openSettings() { tap(); sheet = Sheet.Settings }
    fun openHowTo() { tap(); sheet = Sheet.HowTo }
    fun openEditor(editing: Deck?) { tap(); sheet = Sheet.CustomEditor(editing) }
    fun closeSheet() {
        if (sheet == Sheet.Welcome && homeRegion == null) updateSettings { it.copy(homeRegion = Region.WORLD) }
        sheet = Sheet.None
        enableTiltTest(false)
    }

    fun goHome() {
        stopRound()
        readyJob?.cancel()
        screen = Screen.HOME
        sheet = Sheet.None
        sounds.playMusic(MusicRole.MENU)
        updateSensor()
    }

    /** "Play" pressed for a category in the chosen country. */
    fun playCategory(c: Category, r: Region) {
        deck = content.deckFor(c, r)
        play()
    }

    /** "Play" pressed for Mix or a custom deck (already in [deck]). */
    fun play() {
        tap()
        sheet = Sheet.None
        enterReady()
    }

    fun playAgain() { tap(); enterReady() }

    private fun enterReady() {
        stopRound()
        screen = Screen.READY
        sounds.stopMusic()
        updateSensor()
        readyJob?.cancel()
        readyProgress = 0f
        if (hasSensor) readyJob = viewModelScope.launch {
            // Auto-start once the phone has been held still on a forehead for ~0.9 s.
            var since = 0L
            while (isActive && screen == Screen.READY) {
                if (resumed && tilt.isOnForehead()) {
                    if (since == 0L) { since = SystemClock.elapsedRealtime(); haptics.ready() }
                    val held = SystemClock.elapsedRealtime() - since
                    readyProgress = (held / 900f).coerceIn(0f, 1f)
                    if (held > 900) { readyProgress = 0f; startCountdown(); break }
                } else { since = 0L; readyProgress = 0f }
                delay(50)
            }
        }
    }

    /* ---------------- Round ---------------- */

    fun startCountdown() {
        val d = deck ?: return
        if (!hasSensor) return
        readyJob?.cancel()
        val r = Round(content.cardsFor(d, homeRegion), seen[d.id].orEmpty())
        if (r.deckReset) seen[d.id]?.clear()
        round = r
        score = 0; card = null; flash = null; pauseReason = null; confirmQuit = false
        totalMs = settings.roundSeconds * 1000L
        remainingMs = totalMs
        screen = Screen.PLAY
        updateSensor()
        gameJob?.cancel()
        gameJob = viewModelScope.launch {
            for (n in 3 downTo 1) {
                countdown = n; sounds.play(Sfx.COUNT); haptics.tick()
                // Phone taken off the forehead during 3-2-1: back to "get ready".
                repeat(9) { delay(100); if (tilt.isOffForehead()) { enterReady(); return@launch } }
            }
            countdown = 0; sounds.play(Sfx.GO); delay(650)
            countdown = null
            startRunning()
        }
    }

    private fun startRunning() {
        val r = round ?: return
        running = true; locked = false
        endAt = SystemClock.elapsedRealtime() + remainingMs
        lastSecond = -1
        tilt.enable()
        sounds.playMusic(MusicRole.PLAY)
        showNext(r)
        gameJob = viewModelScope.launch {
            var offSince = 0L
            var onSince = 0L
            while (isActive && running) {
                val now = SystemClock.elapsedRealtime()
                if (pauseReason == null) {
                    remainingMs = (endAt - now).coerceAtLeast(0)
                    val sec = ((remainingMs + 999) / 1000).toInt()
                    if (sec != lastSecond) {
                        lastSecond = sec
                        if (sec in 1..10) sounds.play(if (sec <= 5) Sfx.TICK_URGENT else Sfx.TICK)
                    }
                    if (remainingMs <= 0) { timeUp(); break }
                    // Phone on a bed / table, or held like a normal phone, for a moment: pause.
                    if (tilt.isOffForehead()) {
                        if (offSince == 0L) offSince = now
                        if (now - offSince > OFF_FOREHEAD_MS) { pauseOffForehead(offSince); offSince = 0L }
                    } else offSince = 0L
                    onSince = 0L
                } else {
                    // Paused: carry on by itself once the phone is back on a forehead and held still.
                    if (resumed && tilt.isOnForehead()) {
                        if (onSince == 0L) onSince = now
                        if (now - onSince > BACK_ON_FOREHEAD_MS) { onSince = 0L; resumeFromPause() }
                    } else onSince = 0L
                }
                delay(50)
            }
        }
    }

    private fun showNext(r: Round) {
        card = r.next()
        cardKey++
    }

    /** Correct (true) or pass (false), from a tilt or a hardware key. */
    fun answer(correct: Boolean) {
        val r = round ?: return
        if (!running || locked || paused) return
        locked = true
        lastAnswerAt = SystemClock.elapsedRealtime()
        lastAnswerCorrect = correct
        r.answer(correct)
        seen.getOrPut(deck!!.id) { mutableSetOf() }.add(card?.text ?: "")
        if (correct) {
            score = r.score
            sounds.play(Sfx.CORRECT); haptics.correct(); celebrate++
            flash = Flash.CORRECT
        } else {
            sounds.play(Sfx.PASS); haptics.pass()
            flash = Flash.PASS
        }
        answerJob = viewModelScope.launch {
            delay(700)
            flash = null
            if (running) { showNext(r); locked = false }
        }
    }

    private fun pauseOffForehead(offSince: Long) {
        val r = round ?: return
        pauseRound(PauseReason.OFF_FOREHEAD)
        // Laying the phone down screen-up tips it past the "pass" angle on the way (screen-down:
        // past "correct"). If that answer fired just before it went flat, it wasn't real: take it back.
        val flatSide = tilt.lyingFlatSide()
        val matchesFall = (flatSide == 1 && !lastAnswerCorrect) || (flatSide == -1 && lastAnswerCorrect)
        if (matchesFall && lastAnswerAt > 0 && lastAnswerAt >= offSince - UNDO_WINDOW_MS) {
            answerJob?.cancel()
            r.undoLast()?.let { a -> seen[deck?.id]?.remove(a.text) }
            score = r.score
            flash = null
            card = r.current; cardKey++
            locked = false
            lastAnswerAt = 0
        }
        haptics.pause()
    }

    private fun timeUp() {
        val r = round ?: return
        running = false; locked = true
        tilt.disable()
        r.timeUp()
        sounds.stopMusic()
        sounds.play(Sfx.TIME_UP); haptics.timeUp()
        flash = Flash.TIME_UP
        viewModelScope.launch {
            delay(1600)
            flash = null
            showResults(r)
        }
    }

    private fun showResults(r: Round) {
        val d = deck ?: return
        prefs.saveSeen(seen)
        val prev = best[d.id] ?: 0
        val isBest = r.score > prev && r.score > 0
        if (isBest) { best = best + (d.id to r.score); prefs.saveBest(best) }
        results = ResultsUi(d, r.score, resultTier(r.score), isBest, prev, r.answers)
        screen = Screen.RESULTS
        updateSensor()
        sounds.play(if (r.score >= 4) Sfx.WIN else Sfx.LOSE)
        if (r.score >= 4) celebrate++
        viewModelScope.launch { delay(2500); if (screen == Screen.RESULTS) sounds.playMusic(MusicRole.MENU) }
    }

    fun requestQuit() { if (running || countdown != null) confirmQuit = true else goHome() }
    fun dismissQuit() { confirmQuit = false }

    fun resumeFromPause() {
        if (!paused) return
        endAt = SystemClock.elapsedRealtime() + remainingMs
        pauseReason = null
        tilt.enable()
        haptics.ready()
        sounds.play(Sfx.GO)
        sounds.playMusic(MusicRole.PLAY)
    }

    private fun stopRound() {
        running = false; locked = true
        gameJob?.cancel(); gameJob = null
        answerJob?.cancel()
        countdown = null; flash = null; pauseReason = null; confirmQuit = false
        tilt.disable()
    }

    /** Freeze the clock (app left, phone call, or phone taken off the forehead). */
    private fun pauseRound(reason: PauseReason) {
        if (countdown != null) {
            // Left during 3-2-1: go back to the "get ready" screen rather than starting unseen.
            enterReady()
        } else if (running && !paused) {
            remainingMs = (endAt - SystemClock.elapsedRealtime()).coerceAtLeast(0)
            pauseReason = reason
            tilt.disable()
            sounds.pauseMusic()
        }
    }

    /* ---------------- Custom decks ---------------- */

    /** Returns an error message, or null when saved. */
    fun saveCustomDeck(editing: Deck?, name: String, emoji: String, mode: ClueMode, wordsText: String): String? {
        val n = name.trim()
        val words = wordsText.lines().map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        if (n.isEmpty()) return str(R.string.err_name)
        if (words.size < 5) return str(R.string.err_cards, words.size)
        customDecks = if (editing != null) {
            customDecks.map { if (it.id == editing.id) it.copy(name = n, emoji = emoji, mode = mode, words = words) else it }
        } else {
            val (c1, c2) = Decks.CUSTOM_PALETTES.random()
            customDecks + Deck(
                id = "custom-${System.currentTimeMillis()}", name = n, emoji = emoji,
                colorStart = Decks.parseColor(c1), colorEnd = Decks.parseColor(c2),
                desc = str(R.string.custom_desc), mode = mode, words = words, custom = true,
            )
        }
        prefs.saveCustomDecks(customDecks)
        sheet = Sheet.None
        celebrate++
        return null
    }

    fun deleteDeck(d: Deck) {
        customDecks = customDecks.filterNot { it.id == d.id }
        prefs.saveCustomDecks(customDecks)
        sheet = Sheet.None
    }

    fun shareText(): String {
        val r = results ?: return ""
        val link = "https://play.google.com/store/apps/details?id=${BuildConfig.APPLICATION_ID.removeSuffix(".debug")}"
        return str(R.string.share_text, r.score, r.deck.title, link)
    }

    /* ---------------- Lifecycle ---------------- */

    fun onResume(activity: Context) {
        resumed = true
        sensorContext = activity
        sounds.resume()
        updateSensor()
    }

    fun onPause() {
        resumed = false
        pauseRound(PauseReason.APP)
        sounds.pause()
        tiltSensor.stop()
    }

    fun enableTiltTest(on: Boolean) { tiltTest = on; updateSensor() }

    private fun updateSensor() {
        val ctx = sensorContext
        val inGame = screen == Screen.READY || screen == Screen.PLAY
        if (resumed && ctx != null && (inGame || tiltTest)) tiltSensor.start(ctx)
        else tiltSensor.stop()
    }

    override fun onCleared() {
        tiltSensor.stop()
        sounds.release()
    }

    companion object {
        /** Phone must look "off the forehead" this long before the round pauses. */
        const val OFF_FOREHEAD_MS = 1200L
        /** ...and be back on a forehead, held still, this long before it carries on. */
        const val BACK_ON_FOREHEAD_MS = 800L
        /** An answer this soon before the phone went flat is treated as accidental and undone. */
        const val UNDO_WINDOW_MS = 1500L
    }
}
