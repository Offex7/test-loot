package com.radiotv.control

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import com.radiotv.control.cast.DlnaCastController
import com.radiotv.control.core.AndroidTvRemoteV2Transport
import com.radiotv.control.core.AndroidTvVoiceInputController
import com.radiotv.control.core.BluetoothHidController
import com.radiotv.control.core.GyroAirMouseController
import com.radiotv.control.core.OtherTvRemoteController
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w393dp-h873dp")
class ScreenshotTests {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun screenshotRemote() = captureScreen(screen = "remote", fileName = "remote.png")

    @Test
    fun screenshotControl() = captureScreen(screen = "digital", fileName = "digital-panel.png")

    @Test
    fun screenshotSettings() = captureScreen(screen = "settings", fileName = "settings.png")

    private fun captureScreen(screen: String, fileName: String) {
        ApplicationProvider.getApplicationContext<Context>()
        val koin = GlobalContext.get()
        val output = File("build/screenshots", fileName).absoluteFile
        output.parentFile?.mkdirs()

        composeRule.setContent {
            RadioTvControlScreen(
                remote = koin.get<AndroidTvRemoteV2Transport>(),
                bluetoothHid = koin.get<BluetoothHidController>(),
                airMouse = koin.get<GyroAirMouseController>(),
                voiceInput = koin.get<AndroidTvVoiceInputController>(),
                otherTvRemote = koin.get<OtherTvRemoteController>(),
                dlnaCast = koin.get<DlnaCastController>(),
                initialUiScreen = screen
            )
        }
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage(output.absolutePath)

        val bitmap = BitmapFactory.decodeFile(output.absolutePath)
            ?: throw AssertionError("Не удалось прочитать PNG: $output")
        try {
            var sum = 0L
            var samples = 0L
            val stepX = maxOf(1, bitmap.width / 160)
            val stepY = maxOf(1, bitmap.height / 160)
            for (y in 0 until bitmap.height step stepY) {
                for (x in 0 until bitmap.width step stepX) {
                    val pixel = bitmap.getPixel(x, y)
                    sum += android.graphics.Color.red(pixel)
                    sum += android.graphics.Color.green(pixel)
                    sum += android.graphics.Color.blue(pixel)
                    samples += 3
                }
            }
            val meanRgb = if (samples == 0L) 0.0 else sum.toDouble() / samples
            assertTrue("PNG чёрный ($fileName, mean RGB=$meanRgb)", meanRgb > 0.0)
        } finally {
            bitmap.recycle()
        }
    }
}
