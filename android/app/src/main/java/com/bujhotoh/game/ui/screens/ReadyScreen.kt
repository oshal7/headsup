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
import androidx.compose.ui.res.stringResource
import com.bujhotoh.game.GameViewModel
import com.bujhotoh.game.R
import com.bujhotoh.game.ui.components.BigButton
import com.bujhotoh.game.ui.components.RoundIconButton
import com.bujhotoh.game.ui.theme.BujhoColors

@Composable
fun ReadyScreen(vm: GameViewModel) {
    Box(Modifier.fillMaxSize().safeDrawingPadding()) {
        RoundIconButton("←", stringResource(R.string.back), vm::goHome, Modifier.padding(12.dp))
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
                if (deck != null) Text(deck.title, color = BujhoColors.Muted, fontSize = 15.sp)
                if (vm.hasSensor) {
                    Text(stringResource(R.string.ready_title), fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, lineHeight = 30.sp)
                    Text(stringResource(R.string.ready_body), color = BujhoColors.Muted, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
                    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Legend(stringResource(R.string.legend_correct), stringResource(R.string.legend_correct_sub), BujhoColors.GreenLight, Modifier.weight(1f))
                        Legend(stringResource(R.string.legend_pass), stringResource(R.string.legend_pass_sub), BujhoColors.OrangeLight, Modifier.weight(1f))
                    }
                    HoldStill(vm.readyProgress)
                    BigButton(stringResource(R.string.start_now), onClick = vm::startCountdown)
                    Text(stringResource(R.string.sensor_in_use, stringResource(vm.sensorNameRes)), color = BujhoColors.Muted.copy(alpha = .7f), fontSize = 12.sp,
                        modifier = Modifier.padding(top = 6.dp))
                } else {
                    Text(stringResource(R.string.no_sensor_title), fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                    Text(stringResource(R.string.no_sensor_body), color = BujhoColors.Muted, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 8.dp))
                }
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
            stringResource(if (progress > 0f) R.string.hold_still else R.string.waiting_forehead),
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

/** Little animation: the Headzy buddy nods down (correct), then tilts back (pass). */
@Composable
private fun PhoneOnHead(modifier: Modifier) {
    val t = rememberInfiniteTransition(label = "head")
    val p by t.animateFloat(0f, 1f, infiniteRepeatable(tween(3200, easing = LinearEasing)), label = "p")
    Box(modifier, contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Image(
            androidx.compose.ui.res.painterResource(com.bujhotoh.game.R.drawable.mascot), contentDescription = null,
            modifier = Modifier.size(150.dp).graphicsLayer {
                // .15-.35 nod forward (correct), .55-.75 tilt back (pass)
                val nod = when {
                    p in .15f..0.35f -> kotlin.math.sin((p - .15f) / .2f * Math.PI).toFloat()
                    p in .55f..0.75f -> -kotlin.math.sin((p - .55f) / .2f * Math.PI).toFloat()
                    else -> 0f
                }
                rotationX = nod * 35f
                translationY = nod * 10f * density
            },
        )
    }
}
