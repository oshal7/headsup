package com.bujhotoh.game.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bujhotoh.game.ui.theme.BujhoColors
import kotlin.random.Random

/** Night-sky gradient with a slowly turning rangoli and floating diyas. */
@Composable
fun FestiveBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val t = rememberInfiniteTransition(label = "bg")
    val spin by t.animateFloat(0f, 360f, infiniteRepeatable(tween(80_000, easing = LinearEasing)), label = "spin")
    Box(
        modifier
            .fillMaxSize()
            .background(BujhoColors.Night)
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        0f to BujhoColors.Violet, 0.55f to BujhoColors.Night, 1f to BujhoColors.Deep,
                        center = Offset(size.width / 2, 0f), radius = size.maxDimension * 1.1f,
                    )
                )
            }
    ) {
        Canvas(Modifier.fillMaxSize().alpha(0.13f)) {
            rangoli(Offset(size.width * 1.0f, 0f), size.minDimension * 0.62f, spin)
            rangoli(Offset(0f, size.height), size.minDimension * 0.5f, -spin * 0.7f)
        }
        FloatingDiyas()
        content()
    }
}

/** Eight-petal rangoli drawn with paths. */
fun DrawScope.rangoli(center: Offset, radius: Float, angle: Float) {
    rotate(angle, center) {
        drawCircle(BujhoColors.Gold, radius * 0.92f, center, style = Stroke(radius * 0.015f))
        for (i in 0 until 8) {
            rotate(i * 45f, center) {
                val p = Path().apply {
                    moveTo(center.x, center.y)
                    cubicTo(center.x - radius * .15f, center.y - radius * .3f, center.x - radius * .08f, center.y - radius * .7f, center.x, center.y - radius * .88f)
                    cubicTo(center.x + radius * .08f, center.y - radius * .7f, center.x + radius * .15f, center.y - radius * .3f, center.x, center.y)
                    close()
                }
                drawPath(p, BujhoColors.Pink)
                drawCircle(BujhoColors.Green, radius * 0.05f, Offset(center.x, center.y - radius * 0.92f))
            }
        }
        drawCircle(BujhoColors.Saffron, radius * 0.18f, center)
        drawCircle(BujhoColors.Violet, radius * 0.08f, center)
    }
}

@Composable
private fun FloatingDiyas() {
    val items = remember { List(8) { Triple(Random.nextFloat(), 14_000 + Random.nextInt(10_000), Random.nextInt(14, 28)) } }
    val t = rememberInfiniteTransition(label = "diyas")
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val w = constraints.maxWidth.toFloat(); val h = constraints.maxHeight.toFloat()
        items.forEachIndexed { i, (x, dur, sizeSp) ->
            val p by t.animateFloat(0f, 1f, infiniteRepeatable(tween(dur, easing = LinearEasing)), label = "d$i")
            Text(
                if (i % 3 == 0) "🪔" else "✨",
                fontSize = sizeSp.sp,
                modifier = Modifier.graphicsLayer {
                    val phase = (p + i * 0.13f) % 1f
                    translationX = x * w * 0.92f
                    translationY = h - phase * (h + 80f)
                    alpha = (if (phase < .1f) phase * 10 else 1f - phase) * 0.6f
                },
            )
        }
    }
}

/** Big chunky "3D" button (saffron by default). */
@Composable
fun BigButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    top: Color = BujhoColors.GoldLight,
    bottom: Color = BujhoColors.Saffron,
    edge: Color = BujhoColors.SaffronDark,
    textColor: Color = Color(0xFF4A1400),
    fontSize: TextUnit = 22.sp,
) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val drop by animateFloatAsState(if (pressed) 2f else 6f, label = "drop")
    Box(
        modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .offset(y = (6 - drop).dp)
            .drawWithContent {
                drawRoundRect(edge, topLeft = Offset(0f, drop.dp.toPx()), size = size, cornerRadius = androidx.compose.ui.geometry.CornerRadius(18.dp.toPx()))
                drawContent()
            }
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.verticalGradient(listOf(top, bottom)))
            .clickable(interactionSource = src, indication = null, role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = textColor, fontSize = fontSize, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
    }
}

/** Translucent secondary button. */
@Composable
fun GhostButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = BujhoColors.Ink) {
    Box(
        modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(BujhoColors.Card)
            .border(1.dp, BujhoColors.CardLine, RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = color, fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) }
}

/** Round icon button (help, settings, back). */
@Composable
fun RoundIconButton(label: String, contentDescription: String, onClick: () -> Unit, modifier: Modifier = Modifier, size: Dp = 46.dp) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(BujhoColors.Card)
            .border(1.dp, BujhoColors.CardLine, CircleShape)
            .clickable(role = Role.Button, onClickLabel = contentDescription, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold) }
}

/** Single-choice chips. */
@Composable
fun <T> ChoiceChips(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { o ->
            val on = o == selected
            val s by animateFloatAsState(if (on) 1.04f else 1f, label = "chip")
            Box(
                Modifier
                    .weight(1f)
                    .scale(s)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (on) BujhoColors.Gold else BujhoColors.Card)
                    .border(BorderStroke(1.dp, if (on) BujhoColors.Gold else BujhoColors.CardLine), RoundedCornerShape(12.dp))
                    .clickable(role = Role.RadioButton) { onSelect(o) }
                    .padding(vertical = 10.dp, horizontal = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label(o), color = if (on) BujhoColors.OnGold else BujhoColors.Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1)
            }
        }
    }
}

/** A deck tile on the home screen. */
@Composable
fun DeckTile(
    name: String, hindi: String, emoji: String, cardsLabel: String, badge: String?,
    colors: Pair<Color, Color>, onClick: () -> Unit, modifier: Modifier = Modifier, shineDelay: Int = 0,
) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, label = "press")
    val t = rememberInfiniteTransition(label = "shine")
    val shine by t.animateFloat(-1.2f, 1.2f, infiniteRepeatable(tween(6000, delayMillis = shineDelay, easing = LinearEasing), RepeatMode.Restart), label = "s")
    Box(
        modifier
            .heightIn(min = 132.dp)
            .scale(scale)
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(colors.first, colors.second)))
            .drawWithContent {
                drawContent()
                val x = size.width * shine * 1.5f
                drawRect(Brush.linearGradient(listOf(Color.Transparent, Color.White.copy(alpha = .25f), Color.Transparent), start = Offset(x, 0f), end = Offset(x + size.width * .5f, size.height)))
            }
            .clickable(interactionSource = src, indication = null, role = Role.Button, onClick = onClick)
            .padding(14.dp),
    ) {
        if (badge != null) {
            Text(
                badge, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.align(Alignment.TopStart).clip(CircleShape).background(Color.Black.copy(alpha = .28f)).padding(horizontal = 8.dp, vertical = 1.dp),
            )
        }
        Text(emoji, fontSize = 40.sp, modifier = Modifier.align(Alignment.TopEnd).rotate(if (pressed) 10f else 0f))
        Column(Modifier.align(Alignment.BottomStart)) {
            Text(name, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 19.sp)
            if (hindi.isNotEmpty()) Text(hindi, fontSize = 13.sp, color = Color.White.copy(alpha = .9f))
            Text(cardsLabel, fontSize = 12.sp, color = Color.White.copy(alpha = .85f))
        }
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(text.uppercase(), fontSize = 13.sp, color = BujhoColors.Muted, letterSpacing = 1.5.sp, modifier = Modifier.padding(top = 14.dp, bottom = 6.dp))
}
