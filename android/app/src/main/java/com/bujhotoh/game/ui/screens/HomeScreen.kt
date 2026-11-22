package com.bujhotoh.game.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bujhotoh.game.GameViewModel
import com.bujhotoh.game.R
import com.bujhotoh.game.data.Category
import com.bujhotoh.game.data.ClueMode
import com.bujhotoh.game.data.Deck
import com.bujhotoh.game.ui.components.DeckTile
import com.bujhotoh.game.ui.components.RoundIconButton
import com.bujhotoh.game.ui.theme.Baloo
import com.bujhotoh.game.ui.theme.BujhoColors

@Composable
fun HomeScreen(vm: GameViewModel) {
    val insets = WindowInsets.safeDrawing.asPaddingValues()
    val universal = vm.content.categories.filter { it.isUniversal }
    val regional = vm.content.categories.filterNot { it.isUniversal }
    val mix = vm.mixDeck()
    LazyVerticalGrid(
        columns = GridCells.Adaptive(150.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = insets.calculateTopPadding() + 8.dp, bottom = insets.calculateBottomPadding() + 24.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        fullWidth { Header(onHelp = vm::openHowTo, onSettings = vm::openSettings) }

        fullWidth { Section(stringResource(R.string.section_everyone)) }
        item(key = "mix", span = { GridItemSpan(maxLineSpan) }) { DeckItem(vm, mix) }
        items(universal, key = { it.id }) { c -> CategoryItem(vm, c) }

        fullWidth { Section(stringResource(R.string.section_countries)) }
        items(regional, key = { it.id }) { c -> CategoryItem(vm, c) }

        fullWidth { Section(stringResource(R.string.section_your_decks)) }
        items(vm.customDecks, key = { it.id }) { d -> DeckItem(vm, d) }
        item(key = "add") {
            Box(
                Modifier
                    .heightIn(min = 132.dp)
                    .border(2.dp, Color.White.copy(alpha = .35f), RoundedCornerShape(22.dp))
                    .clickable(role = Role.Button) { vm.openEditor(null) }
                    .padding(14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.make_deck), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, textAlign = TextAlign.Center)
                    Text(stringResource(R.string.make_deck_sub), fontSize = 12.sp, color = BujhoColors.Muted, textAlign = TextAlign.Center)
                }
            }
        }
        fullWidth {
            Text(stringResource(R.string.footer), color = BujhoColors.Muted.copy(alpha = .7f), fontSize = 13.sp,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 14.dp))
        }
    }
}

@Composable
private fun CategoryItem(vm: GameViewModel, c: Category) {
    val regions = vm.regionsFor(c)
    DeckTile(
        name = c.name,
        // Country categories show their flags, home country first.
        subtitle = if (c.isUniversal) "" else regions.joinToString(" ") { it.flag },
        emoji = c.emoji,
        cardsLabel = modeBadge(c.mode),
        badge = vm.bestFor(c)?.let { "🏆 $it" },
        colors = Color(c.colorStart) to Color(c.colorEnd),
        onClick = { vm.openCategory(c) },
        shineDelay = (c.id.hashCode() and 0x7) * 400,
    )
}

@Composable
private fun DeckItem(vm: GameViewModel, d: Deck) {
    val best = vm.best[d.id]
    DeckTile(
        name = d.name, subtitle = "", emoji = d.emoji,
        cardsLabel = stringResource(R.string.cards_count, if (d.mix) vm.content.mixCards(vm.homeRegion).size else d.words.size) + modeBadge(d.mode),
        badge = when { d.custom -> stringResource(R.string.badge_yours); best != null -> "🏆 $best"; else -> null },
        colors = Color(d.colorStart) to Color(d.colorEnd),
        onClick = { vm.openDeck(d) },
        shineDelay = (d.id.hashCode() and 0x7) * 400,
    )
}

private fun modeBadge(m: ClueMode) = when (m) { ClueMode.ACT -> " · 🎭"; ClueMode.HUM -> " · 🎤"; ClueMode.SAY -> "" }

private fun LazyGridScope.fullWidth(content: @Composable () -> Unit) =
    item(span = { GridItemSpan(maxLineSpan) }) { content() }

@Composable
private fun Section(title: String) {
    Text(
        title.uppercase(), color = BujhoColors.Gold, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp,
        modifier = Modifier.padding(top = 10.dp),
    )
}

@Composable
private fun Header(onHelp: () -> Unit, onSettings: () -> Unit) {
    val t = rememberInfiniteTransition(label = "logo")
    val bob by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1750), RepeatMode.Reverse), label = "bob")
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth()) {
            RoundIconButton("?", stringResource(R.string.how_to_play), onHelp)
            Spacer(Modifier.weight(1f))
            RoundIconButton("⚙️", stringResource(R.string.settings), onSettings)
        }
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(brush = Brush.verticalGradient(listOf(Color(0xFFFFF6C2), BujhoColors.Gold, BujhoColors.Saffron)))) { append("Bujho ") }
                withStyle(SpanStyle(brush = Brush.verticalGradient(listOf(Color(0xFFFFD1E0), BujhoColors.Pink)))) { append("Toh!") }
            },
            style = TextStyle(
                fontFamily = Baloo, fontWeight = FontWeight.ExtraBold, fontSize = 64.sp, lineHeight = 64.sp,
                shadow = Shadow(BujhoColors.Maroon, Offset(0f, 10f), 0f),
            ),
            modifier = Modifier.padding(top = 8.dp).graphicsLayer { translationY = -bob * 14f; rotationZ = -bob },
        )
        Text(stringResource(R.string.tagline), color = BujhoColors.Muted, fontSize = 17.sp, fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
        Text(stringResource(R.string.tagline_sub), color = BujhoColors.Blush, fontSize = 14.sp,
            textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 520.dp).padding(bottom = 4.dp))
    }
}
