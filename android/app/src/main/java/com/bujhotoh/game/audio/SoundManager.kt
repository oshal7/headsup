package com.bujhotoh.game.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Handler
import android.os.Looper
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

class SoundManager(context: Context) {
    private val appContext = context.applicationContext
    private val attrs = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()
    private val pool = SoundPool.Builder().setMaxStreams(6).setAudioAttributes(attrs).build()
    private val ids = Sfx.entries.associateWith { pool.load(appContext, it.res, 1) }
    private val main = Handler(Looper.getMainLooper())

    var sfxEnabled = true
    var musicEnabled = true
        set(value) { field = value; if (value) resumeMusic() else stopMusic() }
    var style = MusicStyle.CHILL
        set(value) { val changed = field != value; field = value; if (changed) role?.let { r -> playMusic(r, force = true) } }

    private var player: MediaPlayer? = null
    private var playingRes = 0
    private var role: MusicRole? = null
    private var paused = false

    fun play(sfx: Sfx) {
        if (!sfxEnabled) return
        val id = ids[sfx] ?: return
        pool.play(id, sfx.volume, sfx.volume, 1, 0, 1f)
    }

    fun playMusic(role: MusicRole, force: Boolean = false) {
        this.role = role
        if (!musicEnabled || paused) return
        val res = trackFor(style, role)
        if (!force && res == playingRes && player != null) return
        stopMusic(keepRole = true)
        val mp = MediaPlayer.create(appContext, res, attrs, 0) ?: return
        mp.isLooping = true
        mp.setVolume(0f, 0f)
        mp.start()
        player = mp; playingRes = res
        fade(mp, 0f, MUSIC_VOLUME, 600)
    }

    fun stopMusic(keepRole: Boolean = false) {
        if (!keepRole) role = null
        player?.let { mp -> runCatching { mp.stop() }; mp.release() }
        player = null; playingRes = 0
    }

    /** App went to background. */
    fun pause() {
        paused = true
        player?.let { runCatching { it.pause() } }
    }

    /** App came back. */
    fun resume() {
        paused = false
        resumeMusic()
    }

    private fun resumeMusic() {
        val r = role ?: return
        val mp = player
        if (mp != null && musicEnabled && !paused) runCatching { mp.start() } else playMusic(r, force = true)
    }

    fun release() {
        stopMusic(); pool.release()
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
