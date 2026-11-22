package com.bujhotoh.game.game

import com.bujhotoh.game.data.Sensitivity
import kotlin.math.abs
import kotlin.math.sqrt

enum class TiltEvent { CORRECT, PASS }

/**
 * Forehead tilt detection, independent of Android so it can be unit tested.
 *
 * Input is gravity in SCREEN coordinates (x right, y up along the displayed
 * content, z out of the screen). From it we take how much the screen faces the sky:
 *
 *   up = z / |g|   +1 screen faces the ceiling (head tilted back)  -> PASS
 *                   0 screen vertical (phone on the forehead)       -> neutral
 *                  -1 screen faces the floor (nod down)             -> CORRECT
 *
 * After a tilt fires, the phone must come back near neutral before the next one
 * can fire, so a single nod never counts twice.
 */
class TiltLogic(sensitivity: Sensitivity = Sensitivity.MEDIUM) {
    var threshold = 0.55f; private set
    var rearm = 0.3f; private set

    /** Smoothed "screen faces up" value in [-1, 1]. */
    var up = 0f; private set
    /** Gravity along the screen's vertical axis, in [-1, 1]. ±1 means the content is upright (or upside down). */
    var vertical = 0f; private set
    var hasReading = false; private set

    var enabled = false; private set
    private var baseline = 0f
    private var armed = true
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

    /** Feed one gravity reading (screen coordinates). Returns an event when a tilt fires. */
    fun onGravity(x: Float, y: Float, z: Float): TiltEvent? {
        val g = sqrt(x * x + y * y + z * z)
        if (g < 1e-3f) return null
        val rawUp = (z / g).coerceIn(-1f, 1f)
        up = if (hasReading) up * 0.35f + rawUp * 0.65f else rawUp
        vertical = (y / g).coerceIn(-1f, 1f)
        hasReading = true
        recent.addLast(up); if (recent.size > 10) recent.removeFirst()
        if (!enabled) return null

        val d = up - baseline
        if (armed) {
            if (d < -threshold) { armed = false; return TiltEvent.CORRECT }
            if (d > threshold) { armed = false; return TiltEvent.PASS }
        } else if (abs(d) < rearm) {
            armed = true
        }
        return null
    }

    /** Phone held upright in landscape with the screen facing out: i.e. on someone's forehead. */
    fun isOnForehead(): Boolean = hasReading && abs(up) < 0.35f && abs(vertical) > 0.7f

    /** Starts detection, taking the current position as neutral (if it's plausibly on a forehead). */
    fun enable() {
        val avg = if (recent.isEmpty()) 0f else recent.average().toFloat()
        baseline = if (abs(avg) < 0.45f) avg else 0f
        armed = abs(up - baseline) < rearm
        enabled = true
    }

    fun disable() { enabled = false }
}
