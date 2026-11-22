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
import com.bujhotoh.game.data.ClueMode
import com.bujhotoh.game.data.Controls
import com.bujhotoh.game.data.Deck
import com.bujhotoh.game.data.Decks
import com.bujhotoh.game.data.Prefs
import com.bujhotoh.game.data.Settings
import com.bujhotoh.game.game.Answer
import com.bujhotoh.game.game.Round
import com.bujhotoh.game.game.TiltEvent
import com.bujhotoh.game.game.TiltLogic
import com.bujhotoh.game.game.TiltSensor
import com.bujhotoh.game.game.resultTitle
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class Screen { HOME, READY, PLAY, RESULTS }

sealed interface Sheet {
    data object None : Sheet
    data class DeckInfo(val deck: Deck) : Sheet
    data object Settings : Sheet
    data object HowTo : Sheet
    data class CustomEditor(val editing: Deck?) : Sheet
}

enum class Flash { CORRECT, PASS, TIME_UP }

data class ResultsUi(
    val deck: Deck,
    val score: Int,
    val title: String,
    val subtitle: String,
    val answers: List<Answer>,
)

class GameViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = Prefs(app)
    val sounds = SoundManager(app)
    private val haptics = Haptics(app)
    val tilt = TiltLogic()
    private val tiltSensor = TiltSensor(app, tilt)

    val builtInDecks: List<Deck> = Decks.parse(app.assets.open("decks.json").bufferedReader().use { it.readText() })
    private val mixDeck = Decks.mixOf(builtInDecks)

    var settings by mutableStateOf(prefs.loadSettings()); private set
    var customDecks by mutableStateOf(prefs.loadCustomDecks()); private set
    var best by mutableStateOf(prefs.loadBest()); private set
    private val seen = prefs.loadSeen().mapValues { it.value.toMutableSet() }.toMutableMap()

    val allDecks: List<Deck> get() = listOf(mixDeck) + builtInDecks + customDecks

    var screen by mutableStateOf(Screen.HOME); private set
    var sheet by mutableStateOf<Sheet>(Sheet.None); private set
    var deck by mutableStateOf<Deck?>(null); private set

    // Ready screen
    var useTilt by mutableStateOf(false); private set
    var readyHint by mutableStateOf(""); private set

    // Play screen
    var countdown by mutableStateOf<Int?>(null); private set   // 3, 2, 1, 0 (= "Shuru!"), null when not counting
    var card by mutableStateOf<Card?>(null); private set
    var cardKey by mutableIntStateOf(0); private set            // changes on every new card (drives the pop-in animation)
    var score by mutableIntStateOf(0); private set
    var remainingMs by mutableLongStateOf(0L); private set
    var totalMs by mutableLongStateOf(60_000L); private set
    var flash by mutableStateOf<Flash?>(null); private set
    var paused by mutableStateOf(false); private set
    var confirmQuit by mutableStateOf(false); private set
    /** Incremented to fire a confetti burst. */
    var celebrate by mutableIntStateOf(0); private set

    var results by mutableStateOf<ResultsUi?>(null); private set

    private var round: Round? = null
    private var running = false
    private var locked = true
    private var endAt = 0L
    private var lastSecond = -1
    private var resumed = false
    private var sensorContext: Context? = null
    private var readyJob: Job? = null
    private var gameJob: Job? = null

    init {
        applySettings(settings)
        tiltSensor.onEvent = { ev -> answer(ev == TiltEvent.CORRECT) }
        sounds.playMusic(MusicRole.MENU)
    }

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

    fun resetSeen() { seen.clear(); prefs.saveSeen(emptyMap()) }

    /* ---------------- Navigation ---------------- */

    fun tap() = sounds.play(Sfx.TAP)

    fun openDeck(d: Deck) { tap(); deck = d; sheet = Sheet.DeckInfo(d) }
    fun openSettings() { tap(); sheet = Sheet.Settings }
    fun openHowTo() { tap(); sheet = Sheet.HowTo }
    fun openEditor(editing: Deck?) { tap(); sheet = Sheet.CustomEditor(editing) }
    fun closeSheet() { sheet = Sheet.None }

    fun cardCount(d: Deck): Int = if (d.mix) builtInDecks.sumOf { it.words.size } else d.words.size

    fun goHome() {
        stopRound()
        readyJob?.cancel()
        screen = Screen.HOME
        sheet = Sheet.None
        sounds.playMusic(MusicRole.MENU)
        updateSensor()
    }

    /** "Khelo!" pressed on a deck. */
    fun play() {
        tap()
        sheet = Sheet.None
        val wantTilt = settings.controls != Controls.TAP
        useTilt = wantTilt && tiltSensor.isAvailable
        readyHint = if (settings.controls == Controls.TILT && !tiltSensor.isAvailable)
            "This phone has no motion sensor, so we'll use tap buttons." else ""
        enterReady()
    }

    fun playAgain() { tap(); enterReady() }

    private fun enterReady() {
        stopRound()
        screen = Screen.READY
        sounds.stopMusic()
        updateSensor()
        readyJob?.cancel()
        if (useTilt) readyJob = viewModelScope.launch {
            // Auto-start once the phone has been held on a forehead for ~0.9 s.
            var since = 0L
            while (isActive && screen == Screen.READY) {
                if (resumed && tilt.isOnForehead()) {
                    if (since == 0L) since = SystemClock.elapsedRealtime()
                    if (SystemClock.elapsedRealtime() - since > 900) { startCountdown(); break }
                } else since = 0L
                delay(100)
            }
        }
    }

    /* ---------------- Round ---------------- */

    fun startCountdown() {
        val d = deck ?: return
        readyJob?.cancel()
        val r = Round(Decks.cardsFor(d, builtInDecks), seen[d.id].orEmpty())
        if (r.deckReset) seen[d.id]?.clear()
        round = r
        score = 0; card = null; flash = null; paused = false; confirmQuit = false
        totalMs = settings.roundSeconds * 1000L
        remainingMs = totalMs
        screen = Screen.PLAY
        updateSensor()
        gameJob?.cancel()
        gameJob = viewModelScope.launch {
            for (n in 3 downTo 1) {
                countdown = n; sounds.play(Sfx.COUNT); haptics.tick(); delay(900)
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
        if (useTilt) tilt.enable()
        sounds.playMusic(MusicRole.PLAY)
        showNext(r)
        gameJob = viewModelScope.launch {
            while (isActive && running) {
                if (!paused) {
                    remainingMs = (endAt - SystemClock.elapsedRealtime()).coerceAtLeast(0)
                    val sec = ((remainingMs + 999) / 1000).toInt()
                    if (sec != lastSecond) {
                        lastSecond = sec
                        if (sec in 1..10) sounds.play(if (sec <= 5) Sfx.TICK_URGENT else Sfx.TICK)
                    }
                    if (remainingMs <= 0) { timeUp(); break }
                }
                delay(50)
            }
        }
    }

    private fun showNext(r: Round) {
        card = r.next()
        cardKey++
    }

    /** Correct (true) or pass (false), from a tilt, a button, a swipe or a key. */
    fun answer(correct: Boolean) {
        val r = round ?: return
        if (!running || locked || paused) return
        locked = true
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
        viewModelScope.launch {
            delay(700)
            flash = null
            if (running) { showNext(r); locked = false }
        }
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
        results = ResultsUi(
            deck = d,
            score = r.score,
            title = resultTitle(r.score),
            subtitle = when {
                isBest -> "🏆 New best for ${d.name}!"
                prev > 0 -> "Best: $prev"
                else -> ""
            },
            answers = r.answers,
        )
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
        paused = false
        if (useTilt) tilt.enable()
        sounds.playMusic(MusicRole.PLAY)
    }

    private fun stopRound() {
        running = false; locked = true
        gameJob?.cancel(); gameJob = null
        countdown = null; flash = null; paused = false; confirmQuit = false
        tilt.disable()
    }

    /* ---------------- Custom decks ---------------- */

    /** Returns an error message, or null when saved. */
    fun saveCustomDeck(editing: Deck?, name: String, emoji: String, mode: ClueMode, wordsText: String): String? {
        val n = name.trim()
        val words = wordsText.lines().map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        if (n.isEmpty()) return "Give your deck a name 🙂"
        if (words.size < 5) return "Add at least 5 cards (you have ${words.size})."
        customDecks = if (editing != null) {
            customDecks.map { if (it.id == editing.id) it.copy(name = n, emoji = emoji, mode = mode, words = words) else it }
        } else {
            val (c1, c2) = Decks.CUSTOM_PALETTES.random()
            customDecks + Deck(
                id = "custom-${System.currentTimeMillis()}", name = n, hindi = "", emoji = emoji,
                colorStart = Decks.parseColor(c1), colorEnd = Decks.parseColor(c2),
                desc = "Your very own deck.", mode = mode, words = words, custom = true,
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
        return "I got ${r.score} right in \"${r.deck.name}\" on Bujho Toh! 🇮🇳🎉 Can you beat me?\n" +
            "https://play.google.com/store/apps/details?id=${BuildConfig.APPLICATION_ID.removeSuffix(".debug")}"
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
        if (countdown != null) {
            // Left during 3-2-1: go back to the "get ready" screen rather than starting unseen.
            stopRound()
            screen = Screen.READY
            enterReady()
        } else if (running && !paused) {
            // Freeze the clock; the player resumes from a "Paused" screen.
            remainingMs = (endAt - SystemClock.elapsedRealtime()).coerceAtLeast(0)
            paused = true
            tilt.disable()
        }
        sounds.pause()
        tiltSensor.stop()
    }

    private fun updateSensor() {
        val ctx = sensorContext
        if (resumed && ctx != null && useTilt && (screen == Screen.READY || screen == Screen.PLAY)) tiltSensor.start(ctx)
        else tiltSensor.stop()
    }

    override fun onCleared() {
        tiltSensor.stop()
        sounds.release()
    }
}
