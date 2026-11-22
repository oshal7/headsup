package com.bujhotoh.game.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bujhotoh.game.GameViewModel
import com.bujhotoh.game.ui.components.BigButton
import com.bujhotoh.game.ui.components.RoundIconButton
import com.bujhotoh.game.ui.theme.BujhoColors

@Composable
fun ReadyScreen(vm: GameViewModel) {
    Box(Modifier.fillMaxSize().safeDrawingPadding()) {
        RoundIconButton("←", "Back", vm::goHome, Modifier.padding(12.dp))
        Row(
            Modifier.fillMaxSize().padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterHorizontally),
        ) {
            PhoneOnHead(Modifier.size(170.dp))
            Column(
                Modifier.widthIn(max = 460.dp).verticalScroll(rememberScrollState()).padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val deck = vm.deck
                if (deck != null) Text("${deck.emoji} ${deck.name}", color = BujhoColors.Muted, fontSize = 15.sp)
                if (vm.useTilt) {
                    Text("Phone ko maathe pe rakho!", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, lineHeight = 30.sp)
                    Text("Hold the phone on your forehead, screen facing your friends. The game starts by itself.",
                        color = BujhoColors.Muted, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
                    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Legend("⬇ Nod down", "Sahi! (correct)", BujhoColors.GreenLight, Modifier.weight(1f))
                        Legend("⬆ Tilt back", "Pass", BujhoColors.OrangeLight, Modifier.weight(1f))
                    }
                    HoldStill(vm.readyProgress)
                    BigButton("Start now", onClick = vm::startCountdown)
                    Text("Sensor: ${vm.sensorName}", color = BujhoColors.Muted.copy(alpha = .7f), fontSize = 12.sp,
                        modifier = Modifier.padding(top = 6.dp))
                } else {
                    Text("Tap mode", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                    Text("A friend holds the phone and taps (or swipes) when the guesser gets it. Swipe right = correct, left = pass.",
                        color = BujhoColors.Muted, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 8.dp))
                    BigButton("Start! ▶", onClick = vm::startCountdown)
                }
                if (vm.readyHint.isNotEmpty()) Text(vm.readyHint, color = BujhoColors.Blush, fontSize = 14.sp,
                    textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

/** Shows the phone has been recognised on the forehead and the round is about to start. */
@Composable
private fun HoldStill(progress: Float) {
    Column(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            if (progress > 0f) "Perfect, hold still… 🙂" else "Waiting for the phone on your forehead…",
            color = if (progress > 0f) BujhoColors.GreenLight else BujhoColors.Muted, fontSize = 15.sp, fontWeight = FontWeight.Bold,
        )
        Box(Modifier.fillMaxWidth().height(8.dp).padding(top = 2.dp).clip(RoundedCornerShape(99.dp)).background(BujhoColors.Card)) {
            Box(Modifier.fillMaxWidth(progress).height(8.dp).background(BujhoColors.GreenLight))
        }
    }
}

@Composable
private fun Legend(title: String, sub: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(BujhoColors.Card)
            .border(1.dp, BujhoColors.CardLine, RoundedCornerShape(16.dp)).padding(10.dp),
    ) {
        Text(title, color = color, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
        Text(sub, color = BujhoColors.Muted, fontSize = 13.sp)
    }
}

/** Little animation: a phone lands on a smiling head, then nods down and tilts back. */
@Composable
private fun PhoneOnHead(modifier: Modifier) {
    val t = rememberInfiniteTransition(label = "head")
    val p by t.animateFloat(0f, 1f, infiniteRepeatable(tween(3200, easing = LinearEasing)), label = "p")
    Box(modifier, contentAlignment = Alignment.BottomCenter) {
        Text("🙂", fontSize = 110.sp, lineHeight = 110.sp)
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .size(96.dp, 50.dp)
                .graphicsLayer {
                    // 0-.25 drop in, .25-.45 rest, .45-.6 nod (correct), .6-.75 rest, .75-.9 tilt back (pass)
                    translationY = when { p < .25f -> (-60 + p / .25f * 78) * density; else -> 18 * density }
                    alpha = if (p < .25f) p / .25f else 1f
                    rotationX = when {
                        p in .45f..0.6f -> kotlin.math.sin((p - .45f) / .15f * Math.PI).toFloat() * 50f
                        p in .75f..0.9f -> -kotlin.math.sin((p - .75f) / .15f * Math.PI).toFloat() * 50f
                        else -> 0f
                    }
                }
                .clip(RoundedCornerShape(10.dp))
                .background(Brush.linearGradient(listOf(BujhoColors.Pink, BujhoColors.Saffron)))
                .border(3.dp, BujhoColors.Deep, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) { Text("?", fontWeight = FontWeight.ExtraBold, fontSize = 26.sp) }
    }
}
