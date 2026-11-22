package com.bujhotoh.game.game

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.view.Display
import android.view.Surface
import android.view.WindowManager
import com.bujhotoh.game.R

/**
 * Reads the phone's orientation and motion and feeds [TiltLogic] in screen coordinates.
 *
 * Orientation, best first:
 *  1. GAME_ROTATION_VECTOR: gyroscope + accelerometer fused by Android. Smooth and
 *     reacts instantly to head movement, unaffected by magnets or metal nearby.
 *  2. GRAVITY: fused gravity estimate (phones with a gyroscope but no rotation vector).
 *  3. ACCELEROMETER: low-pass filtered, for phones without a gyroscope.
 * The raw GYROSCOPE is also read, to know whether the phone is being held still.
 */
class TiltSensor(context: Context, val logic: TiltLogic) : SensorEventListener {
    private val sm = context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationVector: Sensor? = sm.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
    private val gravity: Sensor? = sm.getDefaultSensor(Sensor.TYPE_GRAVITY)
    private val accel: Sensor? = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyro: Sensor? = sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val orientation: Sensor? = rotationVector ?: gravity ?: accel

    private val rot = FloatArray(9)
    private val lowPass = FloatArray(3)
    private var running = false
    private var display: Display? = null

    /** Called on the main thread when a tilt fires. */
    var onEvent: ((TiltEvent) -> Unit)? = null

    val isAvailable: Boolean get() = orientation != null

    /** String resource naming what's being used, shown on the Ready screen and in Settings. */
    val sourceNameRes: Int
        get() = when {
            rotationVector != null -> R.string.sensor_fused
            gravity != null && gyro != null -> R.string.sensor_gyro_gravity
            gravity != null -> R.string.sensor_gravity
            accel != null -> R.string.sensor_accel
            else -> R.string.sensor_none
        }

    fun start(context: Context) {
        val o = orientation ?: return
        display = displayOf(context)
        if (running) return
        running = sm.registerListener(this, o, SensorManager.SENSOR_DELAY_GAME)
        gyro?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
    }

    fun stop() {
        if (!running) return
        sm.unregisterListener(this)
        running = false
    }

    override fun onSensorChanged(e: SensorEvent) {
        val v = e.values
        when (e.sensor.type) {
            Sensor.TYPE_GYROSCOPE -> { logic.onRotationSpeed(v[0], v[1], v[2]); return }
            Sensor.TYPE_GAME_ROTATION_VECTOR -> {
                // Rotation matrix maps device -> world; its last row is world "up" in device axes.
                SensorManager.getRotationMatrixFromVector(rot, v)
                feed(rot[6], rot[7], rot[8], e.timestamp)
            }
            Sensor.TYPE_GRAVITY -> feed(v[0], v[1], v[2], e.timestamp)
            Sensor.TYPE_ACCELEROMETER -> {
                val a = 0.8f // isolate gravity from hand shake
                for (i in 0..2) lowPass[i] = a * lowPass[i] + (1 - a) * v[i]
                feed(lowPass[0], lowPass[1], lowPass[2], e.timestamp)
            }
        }
    }

    /** Device axes -> screen axes for the current display rotation, then into the logic. */
    private fun feed(x: Float, y: Float, z: Float, timestamp: Long) {
        val (sx, sy) = when (display?.rotation ?: Surface.ROTATION_0) {
            Surface.ROTATION_90 -> -y to x
            Surface.ROTATION_180 -> -x to -y
            Surface.ROTATION_270 -> y to -x
            else -> x to y
        }
        logic.onGravity(sx, sy, z, timestamp)?.let { ev -> onEvent?.invoke(ev) }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    @Suppress("DEPRECATION")
    private fun displayOf(context: Context): Display? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) context.display
        else (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
}
