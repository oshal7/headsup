package com.bujhotoh.game.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bujhotoh.game.GameViewModel
import com.bujhotoh.game.Sheet
import com.bujhotoh.game.data.ClueMode
import com.bujhotoh.game.data.Controls
import com.bujhotoh.game.data.Deck
import com.bujhotoh.game.data.Decks
import com.bujhotoh.game.data.MusicStyle
import com.bujhotoh.game.data.Sensitivity
import com.bujhotoh.game.ui.components.BigButton
import com.bujhotoh.game.ui.components.ChoiceChips
import com.bujhotoh.game.ui.components.GhostButton
import com.bujhotoh.game.ui.components.SectionLabel
import com.bujhotoh.game.ui.theme.BujhoColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Sheets(vm: GameViewModel) {
    val sheet = vm.sheet
    if (sheet == Sheet.None) return
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = vm::closeSheet,
        sheetState = state,
        containerColor = BujhoColors.SheetTop,
        contentColor = BujhoColors.Ink,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (sheet) {
                is Sheet.DeckInfo -> DeckInfo(vm, sheet.deck)
                Sheet.Settings -> SettingsContent(vm)
                Sheet.HowTo -> HowToContent()
                is Sheet.CustomEditor -> CustomEditor(vm, sheet.editing)
                Sheet.None -> Unit
            }
        }
    }
}

@Composable
private fun ColumnScope.DeckInfo(vm: GameViewModel, deck: Deck) {
    val pop = remember { Animatable(0f) }
    LaunchedEffect(deck.id) { pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy)) }
    var confirmDelete by remember { mutableStateOf(false) }
    Text(deck.emoji, fontSize = 64.sp, modifier = Modifier.scale(pop.value))
    Text(deck.name, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
    if (deck.hindi.isNotEmpty()) Text(deck.hindi, color = BujhoColors.Gold, fontSize = 18.sp)
    Text(deck.desc, color = BujhoColors.Muted, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("🃏 ${vm.cardCount(deck)} cards", fontSize = 14.sp)
        vm.best[deck.id]?.let { Text("🏆 Best: $it", fontSize = 14.sp) }
    }
    SectionLabel("Round time")
    ChoiceChips(
        options = listOf(30, 60, 90, 120), selected = vm.settings.roundSeconds,
        label = { if (it == 120) "2 min" else "${it}s" },
        onSelect = { s -> vm.tap(); vm.updateSettings { it.copy(roundSeconds = s) } },
    )
    Spacer(Modifier.height(18.dp))
    BigButton("Khelo! ▶", onClick = vm::play)
    if (deck.custom) {
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GhostButton("✏️ Edit deck", { vm.openEditor(deck) }, Modifier.weight(1f))
            GhostButton("🗑 Delete", { confirmDelete = true }, Modifier.weight(1f), color = BujhoColors.Blush)
        }
    }
    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text("Delete \"${deck.name}\"?") },
        text = { Text("This can't be undone.") },
        confirmButton = { TextButton({ confirmDelete = false; vm.deleteDeck(deck) }) { Text("Delete") } },
        dismissButton = { TextButton({ confirmDelete = false }) { Text("Cancel") } },
        containerColor = BujhoColors.SheetBottom,
    )
}

@Composable
private fun SettingsContent(vm: GameViewModel) {
    val s = vm.settings
    Text("Settings", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
    ToggleRow("🎵 Music", s.music) { on -> vm.updateSettings { it.copy(music = on) } }
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text("🎶 Music style", fontSize = 17.sp)
        Spacer(Modifier.height(8.dp))
        ChoiceChips(MusicStyle.entries, s.musicStyle, { it.label }, { st -> vm.updateSettings { it.copy(musicStyle = st, music = true) } })
    }
    HorizontalDivider(color = BujhoColors.CardLine)
    ToggleRow("🔔 Sound effects", s.sfx) { on -> vm.updateSettings { it.copy(sfx = on) }; vm.tap() }
    ToggleRow("📳 Vibration", s.vibration) { on -> vm.updateSettings { it.copy(vibration = on) } }
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text("🎮 Controls", fontSize = 17.sp)
        Spacer(Modifier.height(8.dp))
        ChoiceChips(Controls.entries, s.controls, { it.label }, { c -> vm.tap(); vm.updateSettings { it.copy(controls = c) } })
    }
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text("📐 Tilt sensitivity", fontSize = 17.sp)
        Spacer(Modifier.height(8.dp))
        ChoiceChips(Sensitivity.entries, s.sensitivity, { it.label }, { l -> vm.tap(); vm.updateSettings { it.copy(sensitivity = l) } })
    }
    var resetDone by remember { mutableStateOf(false) }
    Spacer(Modifier.height(8.dp))
    GhostButton(if (resetDone) "Done — all cards are fresh again ✨" else "Reset \"already seen\" cards", { vm.resetSeen(); resetDone = true })
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable { onChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 17.sp, modifier = Modifier.weight(1f))
        Switch(
            checked = checked, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = BujhoColors.Green, checkedThumbColor = BujhoColors.Ink),
        )
    }
    HorizontalDivider(color = BujhoColors.CardLine)
}

@Composable
private fun ColumnScope.HowToContent() {
    Text("Kaise khelein?", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
    val steps = listOf(
        "Pick a deck" to "Bollywood, festivals, food, brands, cricket…",
        "Phone on your forehead" to "Sideways, screen facing your friends. Don't peek! 🙈",
        "Friends give clues" to "Talk, act or hum — each deck tells them how.",
        "Guessed it? Nod down" to "Tip the screen towards the floor. ✅",
        "Stuck? Tilt your head back" to "Screen towards the ceiling to pass. ⏭",
        "Most correct answers wins" to "Pass the phone and take turns!",
    )
    steps.forEachIndexed { i, (title, body) ->
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            Text("${i + 1}.", fontWeight = FontWeight.ExtraBold, color = BujhoColors.Gold, modifier = Modifier.padding(end = 10.dp))
            Column { Text(title, fontWeight = FontWeight.Bold); Text(body, color = BujhoColors.Muted, fontSize = 14.sp) }
        }
    }
    Text(
        "No motion sensor, or a friend wants to hold the phone? Choose Tap buttons in Settings, then tap or swipe: right = correct, left = pass.",
        color = BujhoColors.Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColumnScope.CustomEditor(vm: GameViewModel, editing: Deck?) {
    var name by rememberSaveable { mutableStateOf(editing?.name ?: "") }
    var words by rememberSaveable { mutableStateOf(editing?.words?.joinToString("\n") ?: "") }
    var emoji by rememberSaveable { mutableStateOf(editing?.emoji ?: Decks.CUSTOM_EMOJIS.first()) }
    var mode by rememberSaveable { mutableStateOf(editing?.mode ?: ClueMode.SAY) }
    var error by remember { mutableStateOf("") }
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = BujhoColors.Gold, unfocusedBorderColor = BujhoColors.CardLine,
        focusedTextColor = BujhoColors.Ink, unfocusedTextColor = BujhoColors.Ink, cursorColor = BujhoColors.Gold,
        focusedLabelColor = BujhoColors.Gold, unfocusedLabelColor = BujhoColors.Muted,
    )
    Text(if (editing != null) "Edit deck" else "Apna Deck banao", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
    Text("Inside jokes, family names, office stuff — anything! One card per line (at least 5).",
        color = BujhoColors.Muted, fontSize = 14.sp, textAlign = TextAlign.Center)
    OutlinedTextField(name, { name = it.take(32) }, label = { Text("Deck name") }, singleLine = true,
        colors = fieldColors, modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
    FlowRow(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Decks.CUSTOM_EMOJIS.forEach { e ->
            Text(e, fontSize = 24.sp, textAlign = TextAlign.Center,
                modifier = Modifier.size(42.dp).clip(RoundedCornerShape(10.dp))
                    .background(if (e == emoji) BujhoColors.Gold else BujhoColors.Card)
                    .clickable { emoji = e }.padding(top = 4.dp))
        }
    }
    OutlinedTextField(words, { words = it }, label = { Text("Cards, one per line") }, minLines = 6,
        colors = fieldColors, modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(10.dp))
    ChoiceChips(ClueMode.entries, mode, { when (it) { ClueMode.SAY -> "🗣 Talk"; ClueMode.ACT -> "🎭 Act"; ClueMode.HUM -> "🎤 Hum" } }, { mode = it })
    Spacer(Modifier.height(14.dp))
    BigButton("Save deck", onClick = { error = vm.saveCustomDeck(editing, name, emoji, mode, words) ?: "" })
    if (error.isNotEmpty()) Text(error, color = BujhoColors.Blush, modifier = Modifier.padding(top = 8.dp))
}
