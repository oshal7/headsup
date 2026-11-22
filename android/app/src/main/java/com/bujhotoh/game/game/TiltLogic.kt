package com.bujhotoh.game.game

import com.bujhotoh.game.data.Sensitivity
import kotlin.math.abs
import kotlin.math.sqrt

enum class TiltEvent { CORRECT, PASS }

/**
 * Forehead tilt detection, independent of Android so it can be unit tested.
 *
 * Orientation input is the "up" direction in SCREEN coordinates (x right, y up along
 * the displayed content, z out of the screen). On phones with a gyroscope this comes
 * from the fused rotation sensor (gyroscope + accelerometer); otherwise from gravity.
 * From it we take how much the screen faces the sky:
 *
 *   up = z / |g|   +1 screen faces the ceiling (head tilted back)  -> PASS
 *                   0 screen vertical (phone on the forehead)       -> neutral
 *                  -1 screen faces the floor (nod down)             -> CORRECT
 *
 * A tilt fires when the angle passes the threshold, or earlier when the head moves
 * quickly (a natural, snappy nod), using how fast `up` is changing.
 * After a tilt fires, the phone must come back near neutral before the next one
 * can fire, so a single nod never counts twice.
 *
 * The gyroscope's rotation speed also tells us whether the phone is being held
 * steady, which is required before a round auto-starts.
 */
class TiltLogic(sensitivity: Sensitivity = Sensitivity.MEDIUM) {
    var threshold = 0.55f; private set
    var rearm = 0.3f; private set

    /** Smoothed "screen faces up" value in [-1, 1]. */
    var up = 0f; private set
    /** How fast [up] is changing, per second (negative = tipping towards the floor). */
    var upRate = 0f; private set
    /** "Up" along the screen's vertical axis, in [-1, 1]. ±1 means the content is upright (or upside down). */
    var vertical = 0f; private set
    var hasReading = false; private set

    /** Smoothed rotation speed from the gyroscope, rad/s. */
    var motion = 0f; private set
    var hasGyro = false; private set

    var enabled = false; private set
    private var baseline = 0f
    private var armed = true
    private var lastNanos = 0L
    private val recent = ArrayDeque<Float>()

    init { setSensitivity(sensitivity) }

    fun setSensitivity(level: Sensitivity) {
        // thresholds on sin(angle from vertical): low ≈ 40°, medium ≈ 33°, high ≈ 25°
        threshold = when (level) {
            Sensitivity.LOW -> 0.65f
            Sensitivity.MEDIUM -> 0.55f
            Sensitivity.HIGH -> 0.42f
        }
        rearm = threshold * 0.55f
    }

    /**
     * Feed one orientation reading: the up/gravity direction in screen coordinates
     * (any length). [timestampNanos] enables quick-nod detection. Returns an event when a tilt fires.
     */
    fun onGravity(x: Float, y: Float, z: Float, timestampNanos: Long = 0L): TiltEvent? {
        val g = sqrt(x * x + y * y + z * z)
        if (g < 1e-6f) return null
        val rawUp = (z / g).coerceIn(-1f, 1f)
        val prevUp = up
        up = if (hasReading) up * 0.35f + rawUp * 0.65f else rawUp
        vertical = (y / g).coerceIn(-1f, 1f)
        if (timestampNanos > 0 && lastNanos > 0) {
            val dt = (timestampNanos - lastNanos) / 1e9f
            if (dt in 0.001f..0.5f) upRate = upRate * 0.5f + ((up - prevUp) / dt) * 0.5f
        }
        if (timestampNanos > 0) lastNanos = timestampNanos
        hasReading = true
        recent.addLast(up); if (recent.size > 10) recent.removeFirst()
        if (!enabled) return null

        val d = up - baseline
        val flick = threshold * FLICK_FRACTION
        if (armed) {
            if (d < -threshold || (d < -flick && upRate < -FLICK_RATE)) { armed = false; return TiltEvent.CORRECT }
            if (d > threshold || (d > flick && upRate > FLICK_RATE)) { armed = false; return TiltEvent.PASS }
        } else if (abs(d) < rearm) {
            armed = true
        }
        return null
    }

    /** Feed one gyroscope reading (rad/s around the device axes). Only the speed is used. */
    fun onRotationSpeed(wx: Float, wy: Float, wz: Float) {
        val m = sqrt(wx * wx + wy * wy + wz * wz)
        motion = if (hasGyro) motion * 0.8f + m * 0.2f else m
        hasGyro = true
    }

    /** Held still enough to start (always true on phones without a gyroscope). */
    fun isSteady(): Boolean = !hasGyro || motion < STEADY_RAD_PER_SEC

    /** Phone upright in landscape, screen facing out, held still: i.e. resting on someone's forehead. */
    fun isOnForehead(): Boolean = hasReading && abs(up) < 0.35f && abs(vertical) > 0.7f && isSteady()

    /** Starts detection, taking the current position as neutral (if it's plausibly on a forehead). */
    fun enable() {
        val avg = if (recent.isEmpty()) 0f else recent.average().toFloat()
        baseline = if (abs(avg) < 0.45f) avg else 0f
        armed = abs(up - baseline) < rearm
        enabled = true
    }

    fun disable() { enabled = false }

    companion object {
        /** A fast nod fires at this fraction of the normal tilt angle. */
        const val FLICK_FRACTION = 0.7f
        /** "Fast" = `up` changing by more than this per second (~a 40° nod in a quarter second). */
        const val FLICK_RATE = 2.2f
        /** Below this rotation speed (rad/s) the phone counts as held still. */
        const val STEADY_RAD_PER_SEC = 0.8f
    }
}
