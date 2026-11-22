package com.bujhotoh.game.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private val PETAL_COLORS = listOf(
    Color(0xFFFF9A00), Color(0xFFFFB400), Color(0xFFFFD200), Color(0xFFFF6A00),
    Color(0xFFFF3D6E), Color(0xFFFFFFFF), Color(0xFF38EF7D), Color(0xFFE0218A),
)

private class Petal(
    var x: Float, var y: Float, var vx: Float, var vy: Float,
    val r: Float, var rot: Float, val vr: Float, val color: Color, var life: Float, val round: Boolean,
)

/**
 * Marigold-petal confetti. Every time [trigger] changes, a burst fires from the
 * centre; when [rain] is true petals also fall from the top for a few seconds.
 */
@Composable
fun Confetti(trigger: Int, modifier: Modifier = Modifier, rain: Boolean = false) {
    val petals = remember { mutableListOf<Petal>() }
    var frame by remember { mutableLongStateOf(0L) }
    val area = remember { floatArrayOf(0f, 0f) } // canvas width, height

    LaunchedEffect(trigger) {
        if (trigger == 0) return@LaunchedEffect
        val cx = area[0] / 2; val cy = area[1] / 2.4f
        repeat(70) {
            val a = Random.nextFloat() * 6.283f
            val s = 8f + Random.nextFloat() * 18f
            petals += Petal(cx, cy, cos(a) * s, sin(a) * s - 8f, 9f + Random.nextFloat() * 12f,
                Random.nextFloat() * 6.28f, (Random.nextFloat() - .5f) * .3f, PETAL_COLORS.random(), 1f, Random.nextFloat() < .3f)
        }
        val rainUntil = if (rain) System.nanoTime() + 3_000_000_000L else 0L
        while (petals.isNotEmpty() || System.nanoTime() < rainUntil) {
            withFrameNanos { now ->
                if (now < rainUntil) repeat(3) {
                    petals += Petal(Random.nextFloat() * area[0], -20f, (Random.nextFloat() - .5f) * 3f, 4f + Random.nextFloat() * 5f,
                        10f + Random.nextFloat() * 10f, Random.nextFloat() * 6.28f, (Random.nextFloat() - .5f) * .2f, PETAL_COLORS.random(), 1.6f, false)
                }
                val it = petals.iterator()
                while (it.hasNext()) {
                    val p = it.next()
                    p.vy += 0.35f; p.vx *= 0.99f; p.x += p.vx; p.y += p.vy; p.rot += p.vr; p.life -= 0.008f
                    if (p.life <= 0f || p.y > area[1] + 40) it.remove()
                }
                frame = now
            }
        }
    }

    Canvas(modifier.fillMaxSize()) {
        area[0] = size.width; area[1] = size.height
        @Suppress("UNUSED_EXPRESSION") frame // redraw every frame
        petals.forEach { p ->
            val a = p.life.coerceIn(0f, 1f)
            if (p.round) {
                drawCircle(p.color.copy(alpha = a), p.r * .45f, Offset(p.x, p.y))
            } else rotate(Math.toDegrees(p.rot.toDouble()).toFloat(), Offset(p.x, p.y)) {
                drawOval(p.color.copy(alpha = a), Offset(p.x - p.r, p.y - p.r * .5f), Size(p.r * 2, p.r))
            }
        }
    }
}
