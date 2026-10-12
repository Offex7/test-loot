package com.radiotv.control

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.github.takahirom.roborazzi.captureRoboImage
import java.io.File
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Captures the real activity on the JVM. This deliberately does not use an emulator or adb.
 * The same activity is kept alive while switching tabs, so each image shows the actual app UI.
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

        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val launchIntent = Intent(context, MainActivity::class.java)
            .putExtra("ui-screen", "remote")

        ActivityScenario.launch<MainActivity>(launchIntent).use {
            capture(File(outputDir, "remote.png"))

            // Navigate through the real bottom bar; these are not hand-drawn mock screens.
            onView(withText("Контроль")).perform(click())
            capture(File(outputDir, "digital-panel.png"))

            onView(withText("Настройки")).perform(click())
            capture(File(outputDir, "settings.png"))
        }

        listOf("remote.png", "digital-panel.png", "settings.png").forEach { name ->
            check(File(outputDir, name).length() > 0L) { "Roborazzi did not create $name" }
        }
    }

    private fun capture(file: File) {
        file.parentFile?.mkdirs()
        onView(isRoot()).captureRoboImage(file.absolutePath)
    }
}
