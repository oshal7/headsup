package com.bujhotoh.game.audio

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/** Distinct vibrations so the guesser can *feel* what happened with the phone on their forehead. */
class Haptics(context: Context) {
    private val vibrator: Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

    var enabled = true

    /** One firm pulse: got it. */
    fun correct() = vibrate(longArrayOf(0, 60), intArrayOf(0, 255))
    /** Two light taps: skipped. */
    fun pass() = vibrate(longArrayOf(0, 25, 70, 25), intArrayOf(0, 120, 0, 120))
    /** Short tick for each countdown number. */
    fun tick() = vibrate(longArrayOf(0, 15), intArrayOf(0, 90))
    /** Long-short-long: time's up, take the phone off your head. */
    fun timeUp() = vibrate(longArrayOf(0, 180, 90, 80, 90, 280), intArrayOf(0, 255, 0, 200, 0, 255))

    private fun vibrate(timings: LongArray, amplitudes: IntArray) {
        val v = vibrator ?: return
        if (!enabled || !v.hasVibrator()) return
        val effect = if (v.hasAmplitudeControl()) VibrationEffect.createWaveform(timings, amplitudes, -1)
        else VibrationEffect.createWaveform(timings, -1)
        runCatching { v.vibrate(effect) }
    }
}
