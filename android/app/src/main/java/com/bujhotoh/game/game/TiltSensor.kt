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

/**
 * Reads gravity from the phone and feeds [TiltLogic] in screen coordinates.
 * Prefers the fused GRAVITY sensor; falls back to a low-passed accelerometer.
 */
class TiltSensor(context: Context, val logic: TiltLogic) : SensorEventListener {
    private val appContext = context.applicationContext
    private val sm = appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val gravity: Sensor? = sm.getDefaultSensor(Sensor.TYPE_GRAVITY)
    private val accel: Sensor? = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val sensor: Sensor? = gravity ?: accel
    private val lowPass = FloatArray(3)
    private var running = false
    private var display: Display? = null

    /** Called on the main thread when a tilt fires. */
    var onEvent: ((TiltEvent) -> Unit)? = null

    val isAvailable: Boolean get() = sensor != null

    fun start(context: Context) {
        if (running || sensor == null) return
        display = displayOf(context)
        running = sm.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
    }

    fun stop() {
        if (!running) return
        sm.unregisterListener(this)
        running = false
    }

    override fun onSensorChanged(e: SensorEvent) {
        var x = e.values[0]; var y = e.values[1]; val z: Float
        if (e.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            // isolate gravity from hand shake
            val a = 0.8f
            lowPass[0] = a * lowPass[0] + (1 - a) * x
            lowPass[1] = a * lowPass[1] + (1 - a) * y
            lowPass[2] = a * lowPass[2] + (1 - a) * e.values[2]
            x = lowPass[0]; y = lowPass[1]; z = lowPass[2]
        } else {
            z = e.values[2]
        }
        // Device axes -> screen axes for the current display rotation.
        val (sx, sy) = when (display?.rotation ?: Surface.ROTATION_0) {
            Surface.ROTATION_90 -> -y to x
            Surface.ROTATION_180 -> -x to -y
            Surface.ROTATION_270 -> y to -x
            else -> x to y
        }
        logic.onGravity(sx, sy, z)?.let { ev -> onEvent?.invoke(ev) }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    @Suppress("DEPRECATION")
    private fun displayOf(context: Context): Display? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) context.display
        else (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
}
