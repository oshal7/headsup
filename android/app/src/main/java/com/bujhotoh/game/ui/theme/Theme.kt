package com.bujhotoh.game.ui.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.bujhotoh.game.R

/** Baloo 2 (SIL OFL) covers Latin and Devanagari, so Hindi subtitles match the rest of the UI. */
val Baloo = FontFamily(
    Font(R.font.baloo2_medium, FontWeight.Medium),
    Font(R.font.baloo2_bold, FontWeight.Bold),
    Font(R.font.baloo2_extrabold, FontWeight.ExtraBold),
)

object BujhoColors {
    val Night = Color(0xFF2A0845)
    val Violet = Color(0xFF7B2FF7)
    val Deep = Color(0xFF12001F)
    val Gold = Color(0xFFFFD200)
    val GoldLight = Color(0xFFFFE066)
    val Saffron = Color(0xFFFF9A3D)
    val SaffronDark = Color(0xFFC25400)
    val Pink = Color(0xFFFF3D6E)
    val Maroon = Color(0xFFB3002D)
    val Green = Color(0xFF18C46B)
    val GreenLight = Color(0xFF5DFFA6)
    val Orange = Color(0xFFFF8A00)
    val OrangeLight = Color(0xFFFFBE5C)
    val Ink = Color(0xFFFFF8EC)
    val Muted = Color(0xFFE6D6FF)
    val Blush = Color(0xFFFFC9DA)
    val Card = Color.White.copy(alpha = 0.09f)
    val CardLine = Color.White.copy(alpha = 0.16f)
    val SheetTop = Color(0xFF3C1170)
    val SheetBottom = Color(0xFF24063F)
    val OnGold = Color(0xFF3D1600)
}

private fun TextStyle.baloo() = copy(fontFamily = Baloo)

private val BujhoTypography = Typography().run {
    Typography(
        displayLarge = displayLarge.baloo(), displayMedium = displayMedium.baloo(), displaySmall = displaySmall.baloo(),
        headlineLarge = headlineLarge.baloo(), headlineMedium = headlineMedium.baloo(), headlineSmall = headlineSmall.baloo(),
        titleLarge = titleLarge.baloo(), titleMedium = titleMedium.baloo(), titleSmall = titleSmall.baloo(),
        bodyLarge = bodyLarge.baloo(), bodyMedium = bodyMedium.baloo(), bodySmall = bodySmall.baloo(),
        labelLarge = labelLarge.baloo(), labelMedium = labelMedium.baloo(), labelSmall = labelSmall.baloo(),
    )
}

@Composable
fun BujhoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = BujhoColors.Gold,
            onPrimary = BujhoColors.OnGold,
            secondary = BujhoColors.Pink,
            background = BujhoColors.Night,
            onBackground = BujhoColors.Ink,
            surface = BujhoColors.SheetBottom,
            onSurface = BujhoColors.Ink,
            surfaceContainerLow = BujhoColors.SheetBottom,
        ),
        typography = BujhoTypography,
    ) {
        CompositionLocalProvider(LocalContentColor provides BujhoColors.Ink, content = content)
    }
}
