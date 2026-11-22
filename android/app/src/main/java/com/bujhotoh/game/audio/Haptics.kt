package com.bujhotoh.game.audio

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.VibrationEffect.Composition
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.RequiresApi

/**
 * Distinct haptics so the guesser can *feel* what happened with the phone on their forehead.
 *
 * On Android 11+ phones with a good vibration motor we use the system's haptic primitives
 * (the same crisp clicks/rises/thuds the OS itself uses); otherwise simple waveforms.
 */
class Haptics(context: Context) {
    private val vibrator: Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

    var enabled = true

    /** Rising buzz into a firm click: got it! */
    fun correct() = play(
        primitives = { quickRise(0.5f).click(1f, delayMs = 10) },
        timings = longArrayOf(0, 60), amplitudes = intArrayOf(0, 255),
    )

    /** Soft falling buzz: skipped (deliberately gentler than "correct"). */
    fun pass() = play(
        primitives = { quickFall(0.7f) },
        timings = longArrayOf(0, 25, 70, 25), amplitudes = intArrayOf(0, 120, 0, 120),
    )

    /** Light tick for each 3-2-1 number. */
    fun tick() = play(
        primitives = { tick(0.6f) },
        timings = longArrayOf(0, 15), amplitudes = intArrayOf(0, 90),
    )

    /** The phone has been recognised on the forehead: a tiny confirming tap. */
    fun ready() = play(
        primitives = { tick(0.9f).tick(0.9f, delayMs = 80) },
        timings = longArrayOf(0, 12, 70, 12), amplitudes = intArrayOf(0, 150, 0, 150),
    )

    /** Long-short-long: time's up, take the phone off your head. */
    fun timeUp() = play(
        primitives = { slowRise(1f).click(1f, delayMs = 60).click(1f, delayMs = 120) },
        timings = longArrayOf(0, 180, 90, 80, 90, 280), amplitudes = intArrayOf(0, 255, 0, 200, 0, 255),
    )

    private fun play(primitives: PrimitiveBuilder.() -> PrimitiveBuilder, timings: LongArray, amplitudes: IntArray) {
        val v = vibrator ?: return
        if (!enabled || !v.hasVibrator()) return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val b = PrimitiveBuilder(v).primitives()
                if (b.supported) { v.vibrate(b.composition.compose()); return }
            }
            val effect = if (v.hasAmplitudeControl()) VibrationEffect.createWaveform(timings, amplitudes, -1)
            else VibrationEffect.createWaveform(timings, -1)
            v.vibrate(effect)
        }
    }

    /** Small fluent wrapper over [Composition] that tracks whether this phone supports every primitive used. */
    private class PrimitiveBuilder(private val v: Vibrator) {
        val composition: Composition by lazy { VibrationEffect.startComposition() }
        var supported = true; private set

        @RequiresApi(Build.VERSION_CODES.R)
        private fun add(p: Int, scale: Float, delayMs: Int): PrimitiveBuilder {
            if (!v.areAllPrimitivesSupported(p)) supported = false
            else composition.addPrimitive(p, scale, delayMs)
            return this
        }

        @RequiresApi(Build.VERSION_CODES.R) fun click(s: Float, delayMs: Int = 0) = add(Composition.PRIMITIVE_CLICK, s, delayMs)
        @RequiresApi(Build.VERSION_CODES.R) fun tick(s: Float, delayMs: Int = 0) = add(Composition.PRIMITIVE_TICK, s, delayMs)
        @RequiresApi(Build.VERSION_CODES.R) fun quickRise(s: Float, delayMs: Int = 0) = add(Composition.PRIMITIVE_QUICK_RISE, s, delayMs)
        @RequiresApi(Build.VERSION_CODES.R) fun quickFall(s: Float, delayMs: Int = 0) = add(Composition.PRIMITIVE_QUICK_FALL, s, delayMs)
        @RequiresApi(Build.VERSION_CODES.R) fun slowRise(s: Float, delayMs: Int = 0) = add(Composition.PRIMITIVE_SLOW_RISE, s, delayMs)
    }
}
