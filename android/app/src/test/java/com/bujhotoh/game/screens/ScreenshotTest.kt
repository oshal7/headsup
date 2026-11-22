package com.bujhotoh.game.screens

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Looper
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.bujhotoh.game.GameViewModel
import com.bujhotoh.game.MainActivity
import com.bujhotoh.game.Screen
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowSensor
import java.time.Duration

/**
 * Renders the real app (MainActivity + GameViewModel) on the JVM and saves screenshots,
 * walking through a full round. Output: PNG files in app/build/screenshots
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class ScreenshotTest {
    private val out = "build/screenshots"

    private fun idle(ms: Long) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))

    private fun addGravitySensor() {
        val sm = ApplicationProvider.getApplicationContext<Context>().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        shadowOf(sm).addSensor(ShadowSensor.newInstance(Sensor.TYPE_GRAVITY))
    }

    private fun shoot(scenario: ActivityScenario<MainActivity>, name: String) {
        idle(400)
        scenario.onActivity { it.window.decorView.captureRoboImage("$out/$name.png") }
    }

    private fun vm(scenario: ActivityScenario<MainActivity>): GameViewModel {
        lateinit var vm: GameViewModel
        scenario.onActivity { vm = ViewModelProvider(it)[GameViewModel::class.java] }
        return vm
    }

    @Test
    @Config(qualifiers = "w411dp-h731dp-xxhdpi") // 9:16, a valid Play Store screenshot size
    fun homeAndSheets() {
        ActivityScenario.launch(MainActivity::class.java).use { sc ->
            shoot(sc, "01-home")
            val vm = vm(sc)
            sc.onActivity { vm.openDeck(vm.allDecks[1]) }
            idle(800); shoot(sc, "02-deck-sheet")
            sc.onActivity { vm.closeSheet(); vm.openSettings() }
            idle(800); shoot(sc, "03-settings")
        }
    }

    @Test
    @Config(qualifiers = "w731dp-h411dp-land-xxhdpi") // 16:9
    fun tiltRound() {
        addGravitySensor()
        ActivityScenario.launch(MainActivity::class.java).use { sc ->
            val vm = vm(sc)
            sc.onActivity { vm.openDeck(vm.allDecks[1]); vm.play() }
            assertEquals(Screen.READY, vm.screen)
            shoot(sc, "04-ready-tilt")
            sc.onActivity { vm.startCountdown() }
            idle(300); shoot(sc, "05-countdown")
            idle(3400); shoot(sc, "06-play")
            sc.onActivity { vm.answer(true) }
            idle(150); shoot(sc, "07-correct")
            idle(800)
            sc.onActivity { vm.answer(false) }
            idle(150); shoot(sc, "08-pass")
            idle(800)
            sc.onActivity { vm.answer(true) }
            idle(900)
            assertEquals(2, vm.score)
            sc.onActivity { vm.finishRoundNow() } // skip waiting out the clock
            idle(2_000)
            assertEquals(Screen.RESULTS, vm.screen)
            shoot(sc, "09-results")
        }
    }

    @Test
    @Config(qualifiers = "w731dp-h411dp-land-xxhdpi") // 16:9
    fun tapModeWithLongCard() {
        ActivityScenario.launch(MainActivity::class.java).use { sc ->
            val vm = vm(sc)
            val dialogues = vm.allDecks.first { it.id == "dialogues" }
            sc.onActivity { vm.openDeck(dialogues); vm.play() }
            shoot(sc, "10-ready-tap")
            sc.onActivity { vm.startCountdown() }
            idle(3500); shoot(sc, "11-play-tap-mode")
        }
    }
}
