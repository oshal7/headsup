package com.bujhotoh.game.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bujhotoh.game.GameViewModel
import com.bujhotoh.game.R
import com.bujhotoh.game.Sheet
import com.bujhotoh.game.data.Category
import com.bujhotoh.game.data.ClueMode
import com.bujhotoh.game.data.Deck
import com.bujhotoh.game.data.Decks
import com.bujhotoh.game.data.MusicStyle
import com.bujhotoh.game.data.Region
import com.bujhotoh.game.data.Sensitivity
import com.bujhotoh.game.ui.components.BigButton
import com.bujhotoh.game.ui.components.ChoiceChips
import com.bujhotoh.game.ui.components.GhostButton
import com.bujhotoh.game.ui.components.SectionLabel
import com.bujhotoh.game.ui.components.rememberUiHaptic
import com.bujhotoh.game.ui.theme.BujhoColors
import kotlinx.coroutines.delay

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
                Sheet.Welcome -> Welcome(vm)
                is Sheet.CategoryPick -> CategoryPick(vm, sheet.category)
                is Sheet.DeckInfo -> DeckInfo(vm, sheet.deck)
                Sheet.Settings -> SettingsContent(vm)
                Sheet.HowTo -> HowToContent()
                is Sheet.CustomEditor -> CustomEditor(vm, sheet.editing)
                Sheet.None -> Unit
            }
        }
    }
}

/* ---------------- Country picking ---------------- */

/** Big tappable flags. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlagPicker(regions: List<Region>, selected: String?, onPick: (Region) -> Unit, worldLabel: String? = null) {
    val haptic = rememberUiHaptic()
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        regions.forEach { r ->
            val on = r.id == selected
            val s by animateFloatAsState(if (on) 1.06f else 1f, label = "flag")
            Column(
                Modifier
                    .width(92.dp)
                    .scale(s)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (on) BujhoColors.Gold else BujhoColors.Card)
                    .border(BorderStroke(2.dp, if (on) BujhoColors.Gold else BujhoColors.CardLine), RoundedCornerShape(18.dp))
                    .clickable(role = Role.RadioButton) { haptic(HapticFeedbackType.SegmentTick); onPick(r) }
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(r.flag, fontSize = 40.sp, lineHeight = 44.sp)
                Text(
                    if (r.isWorld && worldLabel != null) worldLabel else r.name,
                    color = if (on) BujhoColors.OnGold else BujhoColors.Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                    textAlign = TextAlign.Center, maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.Welcome(vm: GameViewModel) {
    var picked by rememberSaveable { mutableStateOf<String?>(null) }
    // Countries first, then "Somewhere else" (World).
    val options = vm.content.regions.filterNot { it.isWorld } + vm.content.regions.filter { it.isWorld }
    Text("🌍", fontSize = 56.sp)
    Text(stringResource(R.string.welcome_title), fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, lineHeight = 28.sp)
    Text(stringResource(R.string.welcome_body), color = BujhoColors.Muted, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 10.dp))
    FlagPicker(options, picked, { picked = it.id }, worldLabel = stringResource(R.string.somewhere_else))
    Spacer(Modifier.height(18.dp))
    BigButton(stringResource(R.string.lets_go), onClick = { vm.setHomeRegion(picked ?: Region.WORLD) })
}

@Composable
private fun ColumnScope.CategoryPick(vm: GameViewModel, c: Category) {
    val regions = vm.regionsFor(c)
    val region = vm.selectedRegion(c)
    TitleBlock(c.emoji, c.name, c.desc)
    if (regions.size > 1) {
        SectionLabel(stringResource(R.string.which_country))
        FlagPicker(regions, region.id, { vm.chooseRegion(c, it) })
    }
    val deckId = "${c.id}:${region.id}"
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(top = 10.dp)) {
        Text("🃏 " + stringResource(R.string.cards_count, c.variants[region.id].orEmpty().size), fontSize = 14.sp)
        vm.best[deckId]?.let { Text(stringResource(R.string.best_score, it), fontSize = 14.sp) }
    }
    RoundTimeAndPlay(vm) { vm.playCategory(c, region) }
}

@Composable
private fun ColumnScope.DeckInfo(vm: GameViewModel, deck: Deck) {
    var confirmDelete by remember { mutableStateOf(false) }
    TitleBlock(deck.emoji, deck.name, deck.desc)
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(top = 6.dp)) {
        val n = if (deck.mix) vm.content.mixCards(vm.homeRegion).size else deck.words.size
        Text("🃏 " + stringResource(R.string.cards_count, n), fontSize = 14.sp)
        vm.best[deck.id]?.let { Text(stringResource(R.string.best_score, it), fontSize = 14.sp) }
    }
    RoundTimeAndPlay(vm, vm::play)
    if (deck.custom) {
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GhostButton(stringResource(R.string.edit_deck), { vm.openEditor(deck) }, Modifier.weight(1f))
            GhostButton(stringResource(R.string.delete), { confirmDelete = true }, Modifier.weight(1f), color = BujhoColors.Blush)
        }
    }
    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text(stringResource(R.string.delete_title, deck.name)) },
        text = { Text(stringResource(R.string.delete_body)) },
        confirmButton = { TextButton({ confirmDelete = false; vm.deleteDeck(deck) }) { Text(stringResource(R.string.delete_confirm)) } },
        dismissButton = { TextButton({ confirmDelete = false }) { Text(stringResource(R.string.cancel)) } },
        containerColor = BujhoColors.SheetBottom,
    )
}

@Composable
private fun ColumnScope.TitleBlock(emoji: String, name: String, desc: String) {
    val pop = remember { Animatable(0f) }
    LaunchedEffect(name) { pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy)) }
    Text(emoji, fontSize = 64.sp, modifier = Modifier.scale(pop.value))
    Text(name, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
    if (desc.isNotEmpty()) Text(desc, color = BujhoColors.Muted, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 6.dp))
}

@Composable
private fun RoundTimeAndPlay(vm: GameViewModel, onPlay: () -> Unit) {
    SectionLabel(stringResource(R.string.round_time))
    val twoMin = stringResource(R.string.two_min)
    val secs = listOf(30, 60, 90).associateWith { stringResource(R.string.seconds_short, it) }
    ChoiceChips(
        options = listOf(30, 60, 90, 120), selected = vm.settings.roundSeconds,
        label = { if (it == 120) twoMin else secs.getValue(it) },
        onSelect = { s -> vm.tap(); vm.updateSettings { it.copy(roundSeconds = s) } },
    )
    Spacer(Modifier.height(18.dp))
    BigButton(stringResource(R.string.play), onClick = onPlay)
}

/* ---------------- Settings ---------------- */

@Composable
private fun SettingsContent(vm: GameViewModel) {
    val s = vm.settings
    Text(stringResource(R.string.settings), fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
    ToggleRow(stringResource(R.string.music), s.music) { on -> vm.updateSettings { it.copy(music = on) } }
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(stringResource(R.string.music_style), fontSize = 17.sp)
        Spacer(Modifier.height(8.dp))
        val names = mapOf(
            MusicStyle.CHILL to stringResource(R.string.style_chill),
            MusicStyle.CLASSIC to stringResource(R.string.style_classic),
            MusicStyle.PARTY to stringResource(R.string.style_party),
        )
        ChoiceChips(MusicStyle.entries, s.musicStyle, { "${it.emoji} ${names.getValue(it)}" }, { st -> vm.updateSettings { it.copy(musicStyle = st, music = true) } })
    }
    HorizontalDivider(color = BujhoColors.CardLine)
    ToggleRow(stringResource(R.string.sound_effects), s.sfx) { on -> vm.updateSettings { it.copy(sfx = on) }; vm.tap() }
    ToggleRow(stringResource(R.string.vibration), s.vibration) { on -> vm.updateSettings { it.copy(vibration = on) } }
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(stringResource(R.string.home_country), fontSize = 17.sp)
        Spacer(Modifier.height(8.dp))
        val options = vm.content.regions.filterNot { it.isWorld } + vm.content.regions.filter { it.isWorld }
        FlagPicker(options, s.homeRegion, { r -> vm.setHomeRegion(r.id) }, worldLabel = stringResource(R.string.somewhere_else))
    }
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(stringResource(R.string.tilt_sensitivity), fontSize = 17.sp)
        Spacer(Modifier.height(8.dp))
        val names = mapOf(
            Sensitivity.LOW to stringResource(R.string.sens_low),
            Sensitivity.MEDIUM to stringResource(R.string.sens_medium),
            Sensitivity.HIGH to stringResource(R.string.sens_high),
        )
        ChoiceChips(Sensitivity.entries, s.sensitivity, { names.getValue(it) }, { l -> vm.tap(); vm.updateSettings { it.copy(sensitivity = l) } })
    }
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(stringResource(R.string.tilt_sensor), fontSize = 17.sp)
        Spacer(Modifier.height(8.dp))
        if (vm.tiltTest) TiltTester(vm) else GhostButton(stringResource(R.string.test_tilt), { vm.enableTiltTest(true) })
    }
    var resetDone by remember { mutableStateOf(false) }
    Spacer(Modifier.height(8.dp))
    GhostButton(stringResource(if (resetDone) R.string.reset_done else R.string.reset_seen), { vm.resetSeen(); resetDone = true })
}

/** Live view of the tilt sensor: hold the phone like on a forehead and nod / tilt back. */
@Composable
private fun TiltTester(vm: GameViewModel) {
    var up by remember { mutableFloatStateOf(0f) }
    var steady by remember { mutableStateOf(true) }
    var off by remember { mutableStateOf(false) }
    var has by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            val t = vm.tilt
            up = t.up; steady = t.isSteady(); off = t.isOffForehead(); has = t.hasReading
            delay(50)
        }
    }
    val th = vm.tilt.threshold
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(BujhoColors.Card).padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.using_sensor, stringResource(vm.sensorNameRes)), fontSize = 13.sp, color = BujhoColors.Muted)
        if (!has) {
            Text(stringResource(R.string.waiting_sensor), modifier = Modifier.padding(8.dp))
        } else {
            // Meter: left = screen to the floor (correct), right = screen to the ceiling (pass)
            Box(Modifier.fillMaxWidth().padding(vertical = 10.dp).height(22.dp)) {
                Row(Modifier.fillMaxWidth().height(22.dp).clip(RoundedCornerShape(99.dp))) {
                    val side = (1f - th) / 2f
                    Spacer(Modifier.weight(side).height(22.dp).background(BujhoColors.Green))
                    Spacer(Modifier.weight(th).height(22.dp).background(BujhoColors.CardLine))
                    Spacer(Modifier.weight(side).height(22.dp).background(BujhoColors.Orange))
                }
                BoxWithConstraints(Modifier.fillMaxWidth().height(22.dp)) {
                    val x = maxWidth * ((up + 1f) / 2f).coerceIn(0f, 1f)
                    Spacer(Modifier.padding(start = (x - 3.dp).coerceAtLeast(0.dp)).size(6.dp, 22.dp).background(Color.White))
                }
            }
            Text(
                stringResource(
                    when { off -> R.string.zone_off; up < -th -> R.string.zone_correct; up > th -> R.string.zone_pass; else -> R.string.zone_neutral },
                ),
                fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
            )
            Text(stringResource(if (steady) R.string.held_steady else R.string.moving), fontSize = 13.sp, color = BujhoColors.Muted)
        }
        Spacer(Modifier.height(8.dp))
        GhostButton(stringResource(R.string.stop_test), { vm.enableTiltTest(false) })
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val haptic = rememberUiHaptic()
    val change = { v: Boolean -> haptic(if (v) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff); onChange(v) }
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable(role = Role.Switch) { change(!checked) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 17.sp, modifier = Modifier.weight(1f))
        Switch(
            checked = checked, onCheckedChange = change,
            colors = SwitchDefaults.colors(checkedTrackColor = BujhoColors.Green, checkedThumbColor = BujhoColors.Ink),
        )
    }
    HorizontalDivider(color = BujhoColors.CardLine)
}

/* ---------------- How to play ---------------- */

@Composable
private fun ColumnScope.HowToContent() {
    Text(stringResource(R.string.how_title), fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
    val steps = listOf(
        R.string.how_1_t to R.string.how_1_b, R.string.how_2_t to R.string.how_2_b, R.string.how_3_t to R.string.how_3_b,
        R.string.how_4_t to R.string.how_4_b, R.string.how_5_t to R.string.how_5_b, R.string.how_6_t to R.string.how_6_b,
    )
    steps.forEachIndexed { i, (title, body) ->
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            Text("${i + 1}.", fontWeight = FontWeight.ExtraBold, color = BujhoColors.Gold, modifier = Modifier.padding(end = 10.dp))
            Column {
                Text(stringResource(title), fontWeight = FontWeight.Bold)
                Text(stringResource(body), color = BujhoColors.Muted, fontSize = 14.sp)
            }
        }
    }
    Text(stringResource(R.string.how_note), color = BujhoColors.Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
}

/* ---------------- Custom deck editor ---------------- */

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
    Text(stringResource(if (editing != null) R.string.custom_title_edit else R.string.custom_title_new), fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
    Text(stringResource(R.string.custom_body), color = BujhoColors.Muted, fontSize = 14.sp, textAlign = TextAlign.Center)
    OutlinedTextField(name, { name = it.take(32) }, label = { Text(stringResource(R.string.custom_name)) }, singleLine = true,
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
    OutlinedTextField(words, { words = it }, label = { Text(stringResource(R.string.custom_cards)) }, minLines = 6,
        colors = fieldColors, modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(10.dp))
    val modeNames = mapOf(
        ClueMode.SAY to stringResource(R.string.mode_say), ClueMode.ACT to stringResource(R.string.mode_act), ClueMode.HUM to stringResource(R.string.mode_hum),
    )
    ChoiceChips(ClueMode.entries, mode, { modeNames.getValue(it) }, { mode = it })
    Spacer(Modifier.height(14.dp))
    BigButton(stringResource(R.string.save_deck), onClick = { error = vm.saveCustomDeck(editing, name, emoji, mode, words) ?: "" })
    if (error.isNotEmpty()) Text(error, color = BujhoColors.Blush, modifier = Modifier.padding(top = 8.dp))
}

