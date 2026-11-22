package com.bujhotoh.game

import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.bujhotoh.game.ui.components.FestiveBackground
import com.bujhotoh.game.ui.components.LocalUiHapticsEnabled
import com.bujhotoh.game.ui.screens.HomeScreen
import com.bujhotoh.game.ui.screens.PlayScreen
import com.bujhotoh.game.ui.screens.ReadyScreen
import com.bujhotoh.game.ui.screens.ResultsScreen
import com.bujhotoh.game.ui.screens.Sheets
import com.bujhotoh.game.ui.theme.BujhoTheme

class MainActivity : ComponentActivity() {
    private val vm: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        volumeControlStream = AudioManager.STREAM_MUSIC // volume keys control the game, not the ringer
        setContent { BujhoTheme { BujhoApp(vm, this) } }
    }

    override fun onResume() {
        super.onResume()
        vm.onResume(this)
    }

    override fun onPause() {
        vm.onPause()
        super.onPause()
    }

    /** Landscape + screen kept on + system bars hidden while a round is being set up or played. */
    fun applyGameMode(inGame: Boolean) {
        requestedOrientation =
            if (inGame) ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        if (inGame) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val ctl = WindowCompat.getInsetsController(window, window.decorView)
        if (inGame) {
            ctl.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            ctl.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            ctl.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}

@Composable
fun BujhoApp(vm: GameViewModel, activity: MainActivity? = LocalContext.current as? MainActivity) {
    val screen = vm.screen
    LaunchedEffect(screen) { activity?.applyGameMode(screen == Screen.READY || screen == Screen.PLAY) }

    BackHandler(enabled = screen != Screen.HOME) {
        when (screen) {
            Screen.PLAY -> vm.requestQuit()
            else -> vm.goHome()
        }
    }

    CompositionLocalProvider(LocalUiHapticsEnabled provides vm.settings.vibration) { FestiveBackground {
        AnimatedContent(
            targetState = screen,
            transitionSpec = { (fadeIn(tween(300)) + scaleIn(tween(300), initialScale = .97f)) togetherWith fadeOut(tween(150)) },
            label = "screen",
        ) { s ->
            when (s) {
                Screen.HOME -> HomeScreen(vm)
                Screen.READY -> ReadyScreen(vm)
                Screen.PLAY -> PlayScreen(vm)
                Screen.RESULTS -> ResultsScreen(vm)
            }
        }
        Sheets(vm)
    } }
}
