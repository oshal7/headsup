package com.bujhotoh.game.ui.screens

import android.content.Intent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import com.bujhotoh.game.GameViewModel
import com.bujhotoh.game.R
import com.bujhotoh.game.ui.components.BigButton
import com.bujhotoh.game.ui.components.Confetti
import com.bujhotoh.game.ui.components.GhostButton
import com.bujhotoh.game.ui.theme.BujhoColors
import kotlinx.coroutines.delay

@Composable
fun ResultsScreen(vm: GameViewModel) {
    val r = vm.results ?: return
    val context = LocalContext.current
    val titlePop = remember { Animatable(0f) }
    var shown by remember { mutableIntStateOf(0) }
    LaunchedEffect(r) {
        titlePop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        val step = (600L / maxOf(1, r.score)).coerceAtLeast(40L)
        while (shown < r.score) { delay(step); shown++ }
    }
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 520.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                val titles = stringArrayResource(
                    when (r.tier) { 3 -> R.array.result_tier_3; 2 -> R.array.result_tier_2; 1 -> R.array.result_tier_1; else -> R.array.result_tier_0 },
                )
                val title = remember(r) { titles.random() }
                Text(
                    title, fontSize = 46.sp, fontWeight = FontWeight.ExtraBold, color = BujhoColors.Gold, textAlign = TextAlign.Center,
                    style = TextStyle(shadow = Shadow(BujhoColors.Maroon, Offset(0f, 8f))),
                    modifier = Modifier.graphicsLayer { scaleX = titlePop.value; scaleY = titlePop.value },
                )
                Text("$shown", fontSize = 96.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 96.sp)
                Text("${stringResource(R.string.correct_count)} · ${r.deck.emoji} ${r.deck.title}", color = BujhoColors.Muted)
                val sub = when {
                    r.isBest -> stringResource(R.string.new_best, r.deck.title)
                    r.previousBest > 0 -> stringResource(R.string.previous_best, r.previousBest)
                    else -> ""
                }
                if (sub.isNotEmpty()) Text(sub, color = BujhoColors.Blush, modifier = Modifier.padding(top = 6.dp))
                Spacer(Modifier.height(12.dp))
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.Black.copy(alpha = .2f))) {
                    r.answers.forEach { a ->
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 9.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(a.text, fontSize = 17.sp, color = if (a.correct) Color.White else Color.White.copy(alpha = .6f), modifier = Modifier.weight(1f))
                            Text(
                                when { a.correct -> "✓"; a.timedOut -> "⏰"; else -> "⏭" },
                                fontWeight = FontWeight.ExtraBold,
                                color = if (a.correct) BujhoColors.GreenLight else BujhoColors.OrangeLight,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                BigButton(stringResource(R.string.play_again), vm::playAgain)
                Spacer(Modifier.height(10.dp))
                val shareTitle = stringResource(R.string.share_title)
                GhostButton(stringResource(R.string.share_score), {
                    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, vm.shareText())
                    context.startActivity(Intent.createChooser(send, shareTitle))
                })
                Spacer(Modifier.height(10.dp))
                GhostButton(stringResource(R.string.choose_another), vm::goHome)
            }
        }
        Confetti(vm.celebrate, rain = r.score >= 4)
    }
}
