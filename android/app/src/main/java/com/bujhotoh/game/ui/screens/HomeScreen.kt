package com.bujhotoh.game.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bujhotoh.game.GameViewModel
import com.bujhotoh.game.R
import com.bujhotoh.game.data.Category
import com.bujhotoh.game.data.ClueMode
import com.bujhotoh.game.data.Deck
import com.bujhotoh.game.ui.components.DeckCard
import com.bujhotoh.game.ui.components.NewDeckCard
import com.bujhotoh.game.ui.components.RoundArtButton
import com.bujhotoh.game.ui.components.rememberUiHaptic
import com.bujhotoh.game.ui.components.sunburst
import com.bujhotoh.game.ui.theme.Baloo
import com.bujhotoh.game.ui.theme.BujhoColors

/**
 * Home: a game-lobby layout. Header with the mascot, a big "Quick Play" banner, then one
 * horizontally scrolling shelf of deck cards per group, and finally the player's own decks.
 */
@Composable
fun HomeScreen(vm: GameViewModel) {
    val insets = WindowInsets.safeDrawing.asPaddingValues()
    val mix = vm.mixDeck()
    val mixCount = vm.content.mixCards(vm.homeRegion).size
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = insets.calculateTopPadding() + 8.dp, bottom = insets.calculateBottomPadding() + 28.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        item(key = "header") { Header(onHelp = vm::openHowTo, onSettings = vm::openSettings) }
        item(key = "hero") { QuickPlay(mixCount, onPlay = { vm.openDeck(mix) }) }

        vm.content.groups.forEach { g ->
            val cats = vm.content.categoriesIn(g)
            if (cats.isEmpty()) return@forEach
            item(key = "g:${g.id}") {
                Shelf(g.name, g.desc, stringResource(R.string.decks_count, cats.size)) {
                    items(cats, key = { it.id }) { c -> CategoryCard(vm, c) }
                }
            }
        }

        item(key = "yours") {
            Shelf(stringResource(R.string.section_your_decks), stringResource(R.string.make_deck_sub), null) {
                item(key = "add") { NewDeckCard(stringResource(R.string.make_deck), stringResource(R.string.make_deck_card_sub), onClick = { vm.openEditor(null) }) }
                items(vm.customDecks, key = { it.id }) { d -> CustomCard(vm, d) }
            }
        }
        item(key = "footer") {
            Text(
                stringResource(R.string.footer), color = BujhoColors.Muted.copy(alpha = .7f), fontSize = 13.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
            )
        }
    }
}

@Composable
private fun CategoryCard(vm: GameViewModel, c: Category) {
    // The card shows no flags: the country is picked after tapping, inside the deck sheet.
    val region = vm.selectedRegion(c)
    DeckCard(
        id = c.id, name = c.name, emoji = c.emoji,
        info = stringResource(R.string.cards_count, c.variants[region.id].orEmpty().size),
        tag = modeTag(c.mode), best = vm.bestFor(c),
        colors = Color(c.colorStart) to Color(c.colorEnd),
        onClick = { vm.openCategory(c) },
    )
}

@Composable
private fun CustomCard(vm: GameViewModel, d: Deck) {
    DeckCard(
        id = d.id, name = d.name, emoji = d.emoji,
        info = stringResource(R.string.cards_count, d.words.size),
        tag = modeTag(d.mode), best = vm.best[d.id],
        colors = Color(d.colorStart) to Color(d.colorEnd),
        onClick = { vm.openDeck(d) },
    )
}

@Composable
private fun modeTag(m: ClueMode): String? = when (m) {
    ClueMode.ACT -> stringResource(R.string.tag_act)
    ClueMode.HUM -> stringResource(R.string.tag_hum)
    ClueMode.SOUND -> stringResource(R.string.tag_sound)
    ClueMode.SAY -> null
}

/** A titled, horizontally scrolling row of cards. */
@Composable
private fun Shelf(title: String, sub: String, count: String?, content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 14.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 22.sp)
                if (sub.isNotEmpty()) Text(sub, fontSize = 13.sp, color = BujhoColors.Muted, lineHeight = 15.sp)
            }
            if (count != null) Text(
                count, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BujhoColors.Gold,
                modifier = Modifier.padding(start = 8.dp, bottom = 2.dp),
            )
        }
        LazyRow(
            Modifier.fillMaxWidth().padding(top = 10.dp),
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
private fun Header(onHelp: () -> Unit, onSettings: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(R.drawable.mascot), contentDescription = null, modifier = Modifier.size(52.dp))
        Text(
            stringResource(R.string.app_name),
            style = TextStyle(
                fontFamily = Baloo, fontWeight = FontWeight.ExtraBold, fontSize = 34.sp, lineHeight = 34.sp,
                brush = Brush.verticalGradient(listOf(Color(0xFFFFF6C2), BujhoColors.Gold, BujhoColors.Saffron)),
                shadow = Shadow(BujhoColors.Maroon, Offset(0f, 5f), 0f),
            ),
            modifier = Modifier.padding(start = 6.dp),
        )
        Spacer(Modifier.weight(1f))
        RoundArtButton(R.drawable.art_help, stringResource(R.string.how_to_play), onHelp)
        Spacer(Modifier.size(10.dp))
        RoundArtButton(R.drawable.art_gear, stringResource(R.string.settings), onSettings)
    }
}

/** Big featured banner: Mix It Up, with the mascot nodding and tilting like a player. */
@Composable
private fun QuickPlay(cards: Int, onPlay: () -> Unit) {
    val t = rememberInfiniteTransition(label = "hero")
    val spin by t.animateFloat(0f, 360f, infiniteRepeatable(tween(40_000, easing = LinearEasing)), label = "rays")
    val nod by t.animateFloat(-1f, 1f, infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "nod")
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val sink by animateFloatAsState(if (pressed) 1f else 0f, label = "sink")
    val haptic = rememberUiHaptic()
    val shape = RoundedCornerShape(28.dp)
    Box(
        Modifier
            .padding(horizontal = 16.dp)
            .padding(top = 10.dp, bottom = 8.dp)
            .fillMaxWidth()
            .widthIn(max = 640.dp)
            .clickable(interactionSource = src, indication = null, role = Role.Button) { haptic(HapticFeedbackType.Confirm); onPlay() },
    ) {
        Box(Modifier.matchParentSize().offset(y = 7.dp).clip(shape).background(Color(0xFF8A1040)))
        Box(
            Modifier
                .fillMaxWidth()
                .height(176.dp)
                .offset(y = (sink * 5).dp)
                .clip(shape)
                .background(Brush.linearGradient(listOf(Color(0xFFFF4F8B), Color(0xFFFF8A3D))))
                .border(2.dp, Color.White.copy(alpha = .3f), shape),
        ) {
            Canvas(Modifier.matchParentSize()) {
                sunburst(Offset(size.width * .78f, size.height * .55f), size.maxDimension, spin, rays = 16, color = Color.White.copy(alpha = .13f))
            }
            Image(
                painterResource(R.drawable.mascot), contentDescription = null,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 6.dp)
                    .size(168.dp)
                    .graphicsLayer { rotationZ = nod * 9f; translationY = nod * 6f },
            )
            Column(Modifier.align(Alignment.CenterStart).padding(start = 20.dp, end = 150.dp)) {
                Text(
                    stringResource(R.string.quick_play).uppercase(), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.6.sp, color = Color.White.copy(alpha = .9f),
                )
                Text(
                    stringResource(R.string.mix_name), fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 32.sp,
                    style = TextStyle(shadow = Shadow(Color(0x66000000), Offset(0f, 4f), 0f)),
                )
                Text(stringResource(R.string.mix_sub, java.text.NumberFormat.getIntegerInstance().format(cards)), fontSize = 13.sp, color = Color.White.copy(alpha = .9f), lineHeight = 15.sp)
                Box(
                    Modifier.padding(top = 10.dp).clip(CircleShape).background(Color.White).padding(horizontal = 20.dp, vertical = 6.dp),
                ) {
                    Text(stringResource(R.string.play), fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFB3002D))
                }
            }
        }
    }
}
