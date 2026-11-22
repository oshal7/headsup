package com.bujhotoh.game.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bujhotoh.game.Flash
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import com.bujhotoh.game.GameViewModel
import com.bujhotoh.game.PauseReason
import com.bujhotoh.game.R
import com.bujhotoh.game.data.ClueMode
import com.bujhotoh.game.ui.components.BigButton
import com.bujhotoh.game.ui.components.Confetti
import com.bujhotoh.game.ui.components.GhostButton
import com.bujhotoh.game.ui.theme.Baloo
import com.bujhotoh.game.ui.theme.BujhoColors

@Composable
fun PlayScreen(vm: GameViewModel) {
    val deck = vm.deck ?: return
    val c1 = Color(deck.colorStart); val c2 = Color(deck.colorEnd)
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(c1, c2)))
            .focusRequester(focus)
            .focusable()
            .onKeyEvent { e ->
                // Hardware keyboards / Chromebooks: → ↓ Space Enter = correct, ← ↑ P = pass
                if (e.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (e.key) {
                    Key.DirectionRight, Key.DirectionDown, Key.Spacebar, Key.Enter -> { vm.answer(true); true }
                    Key.DirectionLeft, Key.DirectionUp, Key.P -> { vm.answer(false); true }
                    else -> false
                }
            },
    ) {
        SunRays()
        Column(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 16.dp, vertical = 8.dp)) {
            TopBar(vm, deck.title)
            WordArea(vm, Modifier.weight(1f).fillMaxWidth())
            TimerBar(if (vm.totalMs > 0) vm.remainingMs / vm.totalMs.toFloat() else 1f)
        }
        Confetti(vm.celebrate)
        FlashOverlay(vm.flash)
        CountdownOverlay(vm.countdown)
        if (vm.paused) PausedOverlay(vm)
    }

    if (vm.confirmQuit) AlertDialog(
        onDismissRequest = vm::dismissQuit,
        title = { Text(stringResource(R.string.end_round_title)) },
        text = { Text(stringResource(R.string.end_round_body)) },
        confirmButton = { TextButton(vm::goHome) { Text(stringResource(R.string.end_round)) } },
        dismissButton = { TextButton(vm::dismissQuit) { Text(stringResource(R.string.keep_playing)) } },
        containerColor = BujhoColors.SheetBottom,
    )
}

@Composable
private fun TopBar(vm: GameViewModel, title: String) {
    val sec = ((vm.remainingMs + 999) / 1000).toInt()
    val urgent = sec in 1..10 && vm.countdown == null
    val t = rememberInfiniteTransition(label = "urgent")
    val throb by t.animateFloat(1f, 1.18f, infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "throb")
    val bump = remember { Animatable(1f) }
    LaunchedEffect(vm.score) { if (vm.score > 0) { bump.snapTo(1.5f); bump.animateTo(1f, spring(Spring.DampingRatioMediumBouncy)) } }

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            title, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 260.dp).clip(CircleShape).background(Color.Black.copy(alpha = .22f)).padding(horizontal = 12.dp, vertical = 4.dp),
        )
        Spacer(Modifier.weight(1f))
        Text(
            "$sec", fontSize = 40.sp, fontWeight = FontWeight.ExtraBold,
            color = if (urgent) BujhoColors.GoldLight else Color.White,
            style = TextStyle(shadow = Shadow(Color.Black.copy(alpha = .25f), Offset(0f, 6f))),
            modifier = Modifier.scale(if (urgent) throb else 1f),
        )
        Spacer(Modifier.weight(1f))
        Text(
            "${vm.score}", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.scale(bump.value).clip(CircleShape).background(Color.White.copy(alpha = .2f)).padding(horizontal = 14.dp),
        )
        Spacer(Modifier.size(10.dp))
        Box(
            Modifier.size(38.dp).clip(CircleShape).background(Color.Black.copy(alpha = .25f))
                .clickable(role = Role.Button, onClickLabel = stringResource(R.string.end_round), onClick = vm::requestQuit),
            contentAlignment = Alignment.Center,
        ) { Text("✕", fontSize = 18.sp) }
    }
}

@Composable
private fun WordArea(vm: GameViewModel, modifier: Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        val card = vm.card
        if (card != null) key(vm.cardKey) {
            val pop = remember { Animatable(0.4f) }
            LaunchedEffect(Unit) { pop.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow)) }
            val how = when (card.mode) {
                ClueMode.ACT -> stringResource(R.string.hint_act)
                ClueMode.HUM -> stringResource(R.string.hint_hum)
                ClueMode.SOUND -> stringResource(R.string.hint_sound)
                ClueMode.SAY -> ""
            }
            val hint = listOf(card.source, how).filter { it.isNotEmpty() }.joinToString(" · ")
            Column(
                Modifier.fillMaxSize().padding(vertical = 6.dp).graphicsLayer {
                    scaleX = pop.value.coerceAtMost(1.04f); scaleY = scaleX
                    alpha = ((pop.value - .4f) / .6f).coerceIn(0f, 1f)
                    rotationZ = (1f - pop.value) * -10f
                },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                BasicText(
                    card.text,
                    modifier = Modifier.weight(1f, fill = false).fillMaxWidth(),
                    style = TextStyle(
                        fontFamily = Baloo, fontWeight = FontWeight.ExtraBold, color = Color.White, textAlign = TextAlign.Center,
                        lineHeight = 1.05.em(), shadow = Shadow(Color.Black.copy(alpha = .25f), Offset(0f, 8f), 12f),
                    ),
                    autoSize = TextAutoSize.StepBased(minFontSize = 20.sp, maxFontSize = 180.sp, stepSize = 2.sp),
                    maxLines = 3,
                )
                if (hint.isNotEmpty()) Text(
                    hint, fontSize = 16.sp,
                    modifier = Modifier.padding(top = 6.dp).clip(CircleShape).background(Color.Black.copy(alpha = .2f)).padding(horizontal = 14.dp, vertical = 2.dp),
                )
            }
        }
    }
}

private fun Double.em() = androidx.compose.ui.unit.TextUnit(this.toFloat(), androidx.compose.ui.unit.TextUnitType.Em)

@Composable
private fun TimerBar(fraction: Float) {
    Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(Color.Black.copy(alpha = .22f))) {
        Box(
            Modifier.fillMaxHeight().fillMaxWidth(fraction.coerceIn(0f, 1f))
                .background(Brush.horizontalGradient(listOf(Color(0xFFFFF6C2), BujhoColors.Gold))),
        )
    }
}

/** Slowly turning sun-ray pattern behind the card. */
@Composable
private fun SunRays() {
    val t = rememberInfiniteTransition(label = "rays")
    val a by t.animateFloat(0f, 360f, infiniteRepeatable(tween(60_000, easing = LinearEasing)), label = "a")
    Canvas(Modifier.fillMaxSize()) {
        val r = size.maxDimension
        val c = Offset(size.width / 2, size.height / 2)
        rotate(a, c) {
            for (i in 0 until 20) drawArc(
                Color.White.copy(alpha = .09f), startAngle = i * 18f, sweepAngle = 6f, useCenter = true,
                topLeft = Offset(c.x - r, c.y - r), size = Size(r * 2, r * 2),
            )
        }
    }
}

@Composable
private fun FlashOverlay(flash: Flash?) {
    AnimatedVisibility(flash != null, enter = fadeIn(tween(80)), exit = fadeOut(tween(160))) {
        val f = flash ?: Flash.TIME_UP
        val (inner, mid, outer) = when (f) {
            Flash.CORRECT -> Triple(Color(0xFF3DFF9A), BujhoColors.Green, Color(0xFF0A7A3F))
            Flash.PASS, Flash.TIME_UP -> Triple(Color(0xFFFFC56B), BujhoColors.Orange, Color(0xFFB54800))
        }
        val stamp = remember { Animatable(2.2f) }
        LaunchedEffect(f) { stamp.snapTo(2.2f); stamp.animateTo(1f, spring(dampingRatio = 0.45f)) }
        val praise = stringArrayResource(R.array.flash_correct)
        val text = when (f) {
            Flash.CORRECT -> remember { praise.random() }
            Flash.PASS -> stringResource(R.string.flash_pass)
            Flash.TIME_UP -> stringResource(R.string.flash_time_up)
        }
        Box(
            Modifier.fillMaxSize().background(Brush.radialGradient(0f to inner, .6f to mid, 1f to outer)),
            contentAlignment = Alignment.Center,
        ) {
            Text(text, fontSize = 96.sp, fontWeight = FontWeight.ExtraBold, color = Color.White,
                style = TextStyle(shadow = Shadow(Color.Black.copy(alpha = .2f), Offset(0f, 10f))),
                modifier = Modifier.graphicsLayer { scaleX = stamp.value; scaleY = stamp.value; rotationZ = (stamp.value - 1f) * -10f })
        }
    }
}

@Composable
private fun CountdownOverlay(n: Int?) {
    if (n == null) return
    Box(
        Modifier.fillMaxSize().background(Brush.radialGradient(listOf(BujhoColors.Night.copy(alpha = .75f), BujhoColors.Deep.copy(alpha = .96f)))),
        contentAlignment = Alignment.Center,
    ) {
        key(n) {
            val s = remember { Animatable(2.4f) }
            LaunchedEffect(Unit) { s.animateTo(1f, tween(280)); s.animateTo(.85f, tween(600)) }
            Text(
                if (n == 0) stringResource(R.string.go) else "$n",
                fontSize = if (n == 0) 110.sp else 200.sp, fontWeight = FontWeight.ExtraBold, color = BujhoColors.Gold,
                style = TextStyle(shadow = Shadow(BujhoColors.Maroon, Offset(0f, 14f))),
                modifier = Modifier.graphicsLayer { scaleX = s.value; scaleY = s.value; alpha = (2.4f - s.value).coerceIn(0f, 1f) },
            )
        }
    }
}

@Composable
private fun PausedOverlay(vm: GameViewModel) {
    val offForehead = vm.pauseReason == PauseReason.OFF_FOREHEAD
    Box(Modifier.fillMaxSize().background(BujhoColors.Deep.copy(alpha = .94f)).safeDrawingPadding(), contentAlignment = Alignment.Center) {
        Column(Modifier.widthIn(max = 460.dp).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (offForehead) {
                // Gentle bob so it reads as "waiting", not "stuck".
                val t = rememberInfiniteTransition(label = "bob")
                val bob by t.animateFloat(0f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "b")
                Text("📱", fontSize = 56.sp, modifier = Modifier.graphicsLayer { translationY = -bob * 18f; rotationZ = (bob - .5f) * 12f })
                Text(stringResource(R.string.off_forehead_title), fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = BujhoColors.Gold,
                    textAlign = TextAlign.Center, lineHeight = 36.sp)
                Text(stringResource(R.string.off_forehead_body), color = BujhoColors.Muted, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
            } else {
                Text(stringResource(R.string.paused), fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, color = BujhoColors.Gold)
            }
            Text(stringResource(R.string.seconds_left, ((vm.remainingMs + 999) / 1000).toInt()), color = BujhoColors.Muted, modifier = Modifier.padding(top = 6.dp))
            Spacer(Modifier.heightIn(min = 16.dp))
            BigButton(stringResource(R.string.resume), vm::resumeFromPause)
            Spacer(Modifier.height(10.dp))
            GhostButton(stringResource(R.string.end_round), vm::goHome)
        }
    }
}
