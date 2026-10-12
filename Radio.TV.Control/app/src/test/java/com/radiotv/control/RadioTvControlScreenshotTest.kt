package com.radiotv.control

import android.content.Intent
import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import com.github.takahirom.roborazzi.captureRoboImage
import java.io.File
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Captures the real MainActivity on the JVM. No emulator, ADB, or hand-drawn substitute UI.
 * Each screen uses the app's existing initial-screen intent so all captured pixels come from
 * the production Compose tree for that tab.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w393dp-h873dp")
class RadioTvControlScreenshotTest {

    @Test
    fun captureRemoteControlAndSettings() {
        val outputDir = File(
            System.getProperty("radio.tv.screenshot.outputDir")
                ?: "build/outputs/roborazzi/radio-tv-screenshots"
        ).apply { mkdirs() }

        captureScreen("remote", File(outputDir, "remote.png"))
        captureScreen("digital", File(outputDir, "digital-panel.png"))
        captureScreen("settings", File(outputDir, "settings.png"))

        listOf("remote.png", "digital-panel.png", "settings.png").forEach { name ->
            check(File(outputDir, name).length() > 0L) { "Roborazzi did not create $name" }
        }
    }

    private fun captureScreen(screen: String, file: File) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val launchIntent = Intent(context, MainActivity::class.java)
            .putExtra("ui-screen", screen)
        file.parentFile?.mkdirs()
        ActivityScenario.launch<MainActivity>(launchIntent).use {
            onView(isRoot()).captureRoboImage(file.absolutePath)
        }
    }
}
