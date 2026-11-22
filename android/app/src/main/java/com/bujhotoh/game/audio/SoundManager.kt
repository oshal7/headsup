package com.bujhotoh.game.audio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import com.bujhotoh.game.R
import com.bujhotoh.game.data.MusicStyle

/**
 * Game sounds, each chosen to match what just happened:
 *  - CORRECT: temple bell + harmonium "ta-daa" (clearly positive)
 *  - PASS:    soft whoosh + low tabla (neutral, never an "error" buzzer)
 *  - TICK / TICK_URGENT: tabla taps for the last 10 / 5 seconds
 *  - COUNT / GO: dhol hits for 3-2-1 and "Shuru!"
 *  - TIME_UP: shankh (conch), the traditional signal that something has ended
 *  - WIN / LOSE: shehnai phrase, festive or gentle depending on the score
 *  - TAP: light tabla tap for buttons
 * All of them were rendered from the web game's synth engine (tools/render-sounds.js).
 */
enum class Sfx(val res: Int, val volume: Float) {
    TAP(R.raw.sfx_tap, 0.30f),
    CORRECT(R.raw.sfx_correct, 0.85f),
    PASS(R.raw.sfx_pass, 0.65f),
    TICK(R.raw.sfx_tick, 0.40f),
    TICK_URGENT(R.raw.sfx_tick_urgent, 0.70f),
    COUNT(R.raw.sfx_count, 0.75f),
    GO(R.raw.sfx_go, 0.85f),
    TIME_UP(R.raw.sfx_timeup, 0.85f),
    WIN(R.raw.sfx_win, 0.75f),
    LOSE(R.raw.sfx_lose, 0.55f),
}

enum class MusicRole { MENU, PLAY }

/**
 * Plays effects (SoundPool) and looping background music (MediaPlayer), and behaves like a
 * good Android citizen:
 *  - asks for audio focus, so other apps' music pauses while ours plays and resumes after;
 *  - pauses music for phone calls, alarms and other apps, and ducks under navigation prompts;
 *  - stops music when headphones are unplugged (so it doesn't suddenly blare from the speaker);
 *  - uses the media stream, so the phone's volume keys control it.
 */
class SoundManager(context: Context) {
    private val appContext = context.applicationContext
    private val sfxAttrs = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()
    private val musicAttrs = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()
    private val pool = SoundPool.Builder().setMaxStreams(6).setAudioAttributes(sfxAttrs).build()
    private val ids = Sfx.entries.associateWith { pool.load(appContext, it.res, 1) }
    private val main = Handler(Looper.getMainLooper())
    private val audio = appContext.getSystemService(AudioManager::class.java)

    /** Called when something else needs the audio (incoming call, alarm, another app): the round should pause. */
    var onInterrupted: (() -> Unit)? = null

    var sfxEnabled = true
    var musicEnabled = true
        set(value) { field = value; if (value) resumeMusic() else stopMusic() }
    var style = MusicStyle.CHILL
        set(value) { val changed = field != value; field = value; if (changed) role?.let { r -> playMusic(r, force = true) } }

    private var player: MediaPlayer? = null
    private var playingRes = 0
    private var role: MusicRole? = null
    private var paused = false
    private var hasFocus = false
    private var heldByOthers = false   // focus temporarily lost; resume when it comes back

    private val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(musicAttrs)
        .setWillPauseWhenDucked(false)
        .setOnAudioFocusChangeListener({ change -> onFocusChange(change) }, main)
        .build()

    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            if (i.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                player?.let { runCatching { it.pause() } }
                heldByOthers = true
            }
        }
    }
    private var noisyRegistered = false

    fun play(sfx: Sfx) {
        if (!sfxEnabled) return
        val id = ids[sfx] ?: return
        pool.play(id, sfx.volume, sfx.volume, 1, 0, 1f)
    }

    fun playMusic(role: MusicRole, force: Boolean = false) {
        this.role = role
        if (!musicEnabled || paused) return
        val res = trackFor(style, role)
        val current = player
        if (!force && res == playingRes && current != null) {
            // Same track: make sure it's actually playing (e.g. after a phone call paused it).
            if (requestFocus() && !current.isPlaying) runCatching { current.start() }
            heldByOthers = false
            return
        }
        stopMusic(keepRole = true)
        if (!requestFocus()) return
        val mp = MediaPlayer.create(appContext, res, musicAttrs, audio.generateAudioSessionId()) ?: return
        mp.isLooping = true
        mp.setVolume(0f, 0f)
        mp.start()
        player = mp; playingRes = res; heldByOthers = false
        fade(mp, 0f, MUSIC_VOLUME, 600)
    }

    fun stopMusic(keepRole: Boolean = false) {
        if (!keepRole) { role = null; abandonFocus() }
        player?.let { mp -> runCatching { mp.stop() }; mp.release() }
        player = null; playingRes = 0
    }

    /** Hold the music (round paused); [playMusic] with the same role carries on from here. */
    fun pauseMusic() {
        player?.let { runCatching { if (it.isPlaying) it.pause() } }
    }

    /** App went to background: pause and let other apps have the audio. */
    fun pause() {
        paused = true
        player?.let { runCatching { it.pause() } }
        abandonFocus()
        if (noisyRegistered) { runCatching { appContext.unregisterReceiver(noisyReceiver) }; noisyRegistered = false }
    }

    /** App came back. */
    fun resume() {
        paused = false
        if (!noisyRegistered) {
            ContextCompat.registerReceiver(appContext, noisyReceiver, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY),
                ContextCompat.RECEIVER_NOT_EXPORTED)
            noisyRegistered = true
        }
        resumeMusic()
    }

    private fun resumeMusic() {
        val r = role ?: return
        val mp = player
        if (mp != null && musicEnabled && !paused && requestFocus()) { runCatching { mp.start() }; heldByOthers = false }
        else if (mp == null) playMusic(r, force = true)
    }

    private fun onFocusChange(change: Int) {
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                hasFocus = false; heldByOthers = true
                player?.let { runCatching { it.pause() } }
                onInterrupted?.invoke()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {   // phone call, alarm, voice assistant
                heldByOthers = true
                player?.let { runCatching { it.pause() } }
                onInterrupted?.invoke()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> player?.setVolume(MUSIC_VOLUME * 0.25f, MUSIC_VOLUME * 0.25f)
            AudioManager.AUDIOFOCUS_GAIN -> {
                hasFocus = true
                player?.let { mp ->
                    mp.setVolume(MUSIC_VOLUME, MUSIC_VOLUME)
                    if (heldByOthers && !paused && musicEnabled) runCatching { mp.start() }
                }
                heldByOthers = false
            }
        }
    }

    private fun requestFocus(): Boolean {
        if (hasFocus) return true
        hasFocus = audio.requestAudioFocus(focusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        return hasFocus
    }

    private fun abandonFocus() {
        if (!hasFocus) return
        audio.abandonAudioFocusRequest(focusRequest)
        hasFocus = false
    }

    fun release() {
        pause(); stopMusic(); pool.release()
    }

    private fun fade(mp: MediaPlayer, from: Float, to: Float, ms: Long) {
        val steps = 12
        for (i in 1..steps) main.postDelayed({
            if (player === mp) runCatching { val v = from + (to - from) * i / steps; mp.setVolume(v, v) }
        }, ms * i / steps)
    }

    companion object {
        private const val MUSIC_VOLUME = 0.42f

        fun trackFor(style: MusicStyle, role: MusicRole): Int = when (style) {
            MusicStyle.CHILL -> if (role == MusicRole.MENU) R.raw.music_chill else R.raw.music_chill_play
            MusicStyle.CLASSIC -> R.raw.music_classic
            MusicStyle.PARTY -> if (role == MusicRole.MENU) R.raw.music_classic else R.raw.music_party
        }
    }
}
