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
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
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

/** Whether UI touch haptics are on (follows the in-game Vibration setting). */
val LocalUiHapticsEnabled = staticCompositionLocalOf { true }

/**
 * System touch feedback for taps. Uses the phone's own haptic patterns, so it also respects
 * the system "Touch feedback" setting, plus the game's Vibration switch.
 */
@Composable
fun rememberUiHaptic(): (HapticFeedbackType) -> Unit {
    val haptic = LocalHapticFeedback.current
    val on = LocalUiHapticsEnabled.current
    return remember(haptic, on) { { type -> if (on) haptic.performHapticFeedback(type) } }
}

/** Deep violet backdrop with slowly turning light rays and drifting sparkles (game-lobby style). */
@Composable
fun FestiveBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val t = rememberInfiniteTransition(label = "bg")
    val spin by t.animateFloat(0f, 360f, infiniteRepeatable(tween(120_000, easing = LinearEasing)), label = "spin")
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
        Canvas(Modifier.fillMaxSize().alpha(0.07f)) {
            sunburst(Offset(size.width / 2, size.height * 0.08f), size.maxDimension * 1.2f, spin)
        }
        Sparkles()
        content()
    }
}

/** Light rays fanning out from [center]. */
fun DrawScope.sunburst(center: Offset, radius: Float, angle: Float, rays: Int = 18, color: Color = Color.White) {
    val step = 360f / rays
    rotate(angle, center) {
        for (i in 0 until rays) {
            rotate(i * step, center) {
                val half = Math.toRadians(step / 4.0)
                val p = Path().apply {
                    moveTo(center.x, center.y)
                    lineTo(center.x + radius * kotlin.math.sin(-half).toFloat(), center.y - radius * kotlin.math.cos(half).toFloat())
                    lineTo(center.x + radius * kotlin.math.sin(half).toFloat(), center.y - radius * kotlin.math.cos(half).toFloat())
                    close()
                }
                drawPath(p, color)
            }
        }
    }
}

/** Four-point sparkle star. */
fun DrawScope.sparkle(center: Offset, r: Float, color: Color) {
    val k = r * 0.22f
    val p = Path().apply {
        moveTo(center.x, center.y - r)
        quadraticTo(center.x + k, center.y - k, center.x + r, center.y)
        quadraticTo(center.x + k, center.y + k, center.x, center.y + r)
        quadraticTo(center.x - k, center.y + k, center.x - r, center.y)
        quadraticTo(center.x - k, center.y - k, center.x, center.y - r)
        close()
    }
    drawPath(p, color)
}

@Composable
private fun Sparkles() {
    val items = remember { List(10) { Triple(Random.nextFloat(), 16_000 + Random.nextInt(12_000), 5f + Random.nextFloat() * 9f) } }
    val colors = listOf(BujhoColors.Gold, BujhoColors.Pink, Color.White, BujhoColors.GreenLight)
    val t = rememberInfiniteTransition(label = "sparkles")
    val progress = items.mapIndexed { i, (_, dur, _) ->
        t.animateFloat(0f, 1f, infiniteRepeatable(tween(dur, easing = LinearEasing)), label = "s$i")
    }
    Canvas(Modifier.fillMaxSize()) {
        items.forEachIndexed { i, (x, _, r) ->
            val phase = (progress[i].value + i * 0.13f) % 1f
            val a = (if (phase < .1f) phase * 10 else 1f - phase) * 0.45f
            sparkle(
                Offset(x * size.width, size.height - phase * (size.height + 60f)),
                r.dp.toPx(), colors[i % colors.size].copy(alpha = a),
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
    val haptic = rememberUiHaptic()
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
            .clickable(interactionSource = src, indication = null, role = Role.Button) { haptic(HapticFeedbackType.Confirm); onClick() }
            .padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = textColor, fontSize = fontSize, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
    }
}

/** Translucent secondary button. */
@Composable
fun GhostButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = BujhoColors.Ink) {
    val haptic = rememberUiHaptic()
    Box(
        modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(BujhoColors.Card)
            .border(1.dp, BujhoColors.CardLine, RoundedCornerShape(16.dp))
            .clickable(role = Role.Button) { haptic(HapticFeedbackType.ContextClick); onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = color, fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) }
}

/** Round icon button with a 3D illustration. */
@Composable
fun RoundArtButton(@androidx.annotation.DrawableRes art: Int, contentDescription: String, onClick: () -> Unit, modifier: Modifier = Modifier, size: Dp = 46.dp) {
    val haptic = rememberUiHaptic()
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(BujhoColors.Card)
            .border(1.dp, BujhoColors.CardLine, CircleShape)
            .clickable(role = Role.Button, onClickLabel = contentDescription) { haptic(HapticFeedbackType.ContextClick); onClick() },
        contentAlignment = Alignment.Center,
    ) { ArtIcon(art, size * 0.56f) }
}

/** Round icon button (help, settings, back). */
@Composable
fun RoundIconButton(label: String, contentDescription: String, onClick: () -> Unit, modifier: Modifier = Modifier, size: Dp = 46.dp) {
    val haptic = rememberUiHaptic()
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(BujhoColors.Card)
            .border(1.dp, BujhoColors.CardLine, CircleShape)
            .clickable(role = Role.Button, onClickLabel = contentDescription) { haptic(HapticFeedbackType.ContextClick); onClick() },
        contentAlignment = Alignment.Center,
    ) { Text(label, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold) }
}

/** Single-choice chips. */
@Composable
fun <T> ChoiceChips(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    val haptic = rememberUiHaptic()
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
                    .clickable(role = Role.RadioButton) { haptic(HapticFeedbackType.SegmentTick); onSelect(o) }
                    .padding(vertical = 10.dp, horizontal = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label(o), color = if (on) BujhoColors.OnGold else BujhoColors.Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1)
            }
        }
    }
}

/**
 * A deck on the home screen, styled like a collectible game card: a coloured portrait card with a
 * glowing 3D illustration, a chunky bottom edge and a label strip.
 */
@Composable
fun DeckCard(
    id: String, name: String, emoji: String, info: String, tag: String?, best: Int?,
    colors: Pair<Color, Color>, onClick: () -> Unit, modifier: Modifier = Modifier,
    width: Dp = 136.dp, height: Dp = 184.dp,
) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed) 1f else 0f, label = "press")
    val haptic = rememberUiHaptic()
    val shape = RoundedCornerShape(22.dp)
    val edge = lerpColor(colors.second, Color.Black, 0.45f)
    Box(
        modifier
            .size(width, height + 6.dp)
            .clickable(interactionSource = src, indication = null, role = Role.Button, onClickLabel = name) {
                haptic(HapticFeedbackType.ContextClick); onClick()
            },
    ) {
        // Chunky edge under the card; the card sinks onto it when pressed.
        Box(Modifier.offset(y = 6.dp).size(width, height).clip(shape).background(edge))
        Column(
            Modifier
                .offset(y = (press * 4).dp)
                .size(width, height)
                .clip(shape)
                .background(Brush.verticalGradient(listOf(colors.first, colors.second)))
                .border(2.dp, Color.White.copy(alpha = .28f), shape),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    drawCircle(
                        Brush.radialGradient(listOf(Color.White.copy(alpha = .55f), Color.White.copy(alpha = 0f)), center = center, radius = size.minDimension * .5f),
                        radius = size.minDimension * .5f,
                    )
                }
                DeckIcon(id, emoji, 80.dp, Modifier.graphicsLayer { rotationZ = press * -8f; scaleX = 1f + press * .08f; scaleY = scaleX })
                if (best != null) {
                    Row(
                        Modifier.align(Alignment.TopStart).padding(8.dp).clip(CircleShape).background(Color.Black.copy(alpha = .3f))
                            .padding(start = 4.dp, end = 8.dp, top = 1.dp, bottom = 1.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ArtIcon(com.bujhotoh.game.R.drawable.art_trophy, 14.dp)
                        Text(" $best", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 12.sp)
                    }
                }
                if (tag != null) {
                    Text(
                        tag.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = .8.sp, lineHeight = 10.sp,
                        color = BujhoColors.OnGold,
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).clip(CircleShape).background(BujhoColors.Gold)
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                    )
                }
            }
            Column(
                Modifier.fillMaxWidth().background(Color.Black.copy(alpha = .22f)).padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    name, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 16.sp, maxLines = 2,
                    textAlign = TextAlign.Center, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
                if (info.isNotEmpty()) Text(info, fontSize = 11.sp, color = Color.White.copy(alpha = .8f), lineHeight = 12.sp, maxLines = 1)
            }
        }
    }
}

/** The "make your own deck" card at the start of the Your Decks shelf. */
@Composable
fun NewDeckCard(title: String, sub: String, onClick: () -> Unit, width: Dp = 136.dp, height: Dp = 184.dp) {
    val haptic = rememberUiHaptic()
    val shape = RoundedCornerShape(22.dp)
    Column(
        Modifier
            .size(width, height)
            .clip(shape)
            .background(Color.White.copy(alpha = .05f))
            .drawBehind {
                drawRoundRect(
                    Color.White.copy(alpha = .4f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(22.dp.toPx()),
                    style = Stroke(2.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(14f, 10f))),
                )
            }
            .clickable(role = Role.Button) { haptic(HapticFeedbackType.ContextClick); onClick() }
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.size(56.dp).clip(CircleShape).background(BujhoColors.Gold), contentAlignment = Alignment.Center) {
            Text("+", fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = BujhoColors.OnGold, lineHeight = 36.sp)
        }
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, lineHeight = 16.sp, modifier = Modifier.padding(top = 10.dp))
        Text(sub, fontSize = 11.sp, color = BujhoColors.Muted, textAlign = TextAlign.Center, lineHeight = 13.sp)
    }
}

private fun lerpColor(a: Color, b: Color, t: Float) = Color(
    a.red + (b.red - a.red) * t, a.green + (b.green - a.green) * t, a.blue + (b.blue - a.blue) * t, 1f,
)

@Composable
fun SectionLabel(text: String) {
    Text(text.uppercase(), fontSize = 13.sp, color = BujhoColors.Muted, letterSpacing = 1.5.sp, modifier = Modifier.padding(top = 14.dp, bottom = 6.dp))
}
