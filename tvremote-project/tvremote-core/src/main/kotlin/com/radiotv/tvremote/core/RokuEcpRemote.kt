package com.radiotv.tvremote.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class RokuEcpRemote(
    host: String,
    name: String = "Roku"
) : RemoteDevice {
    override val info = RemoteDeviceInfo("roku:" + host, name, host, 8060, DeviceProtocol.ROKU_ECP)
    private val stateFlow = kotlinx.coroutines.flow.MutableStateFlow(ConnectionState.DISCONNECTED)
    override val state = stateFlow
    override val capabilities = setOf(
        Capability.NAVIGATION, Capability.VOLUME, Capability.CHANNEL, Capability.NUMERIC,
        Capability.POWER, Capability.MUTE, Capability.INPUT, Capability.MEDIA, Capability.TEXT
    )

    private val map = mapOf(
        TvKey.HOME to "Home", TvKey.BACK to "Back", TvKey.UP to "Up", TvKey.DOWN to "Down",
        TvKey.LEFT to "Left", TvKey.RIGHT to "Right", TvKey.OK to "Select",
        TvKey.PLAY_PAUSE to "Play", TvKey.REWIND to "Rev", TvKey.FAST_FORWARD to "Fwd",
        TvKey.NEXT to "Fwd", TvKey.PREVIOUS to "Rev", TvKey.SEARCH to "Search",
        TvKey.VOLUME_UP to "VolumeUp", TvKey.VOLUME_DOWN to "VolumeDown",
        TvKey.MUTE to "VolumeMute", TvKey.POWER to "PowerOff",
        TvKey.CHANNEL_UP to "ChannelUp", TvKey.CHANNEL_DOWN to "ChannelDown",
        TvKey.INPUT to "InputTuner",
        TvKey.DIGIT_0 to "Lit_0", TvKey.DIGIT_1 to "Lit_1", TvKey.DIGIT_2 to "Lit_2",
        TvKey.DIGIT_3 to "Lit_3", TvKey.DIGIT_4 to "Lit_4", TvKey.DIGIT_5 to "Lit_5",
        TvKey.DIGIT_6 to "Lit_6", TvKey.DIGIT_7 to "Lit_7", TvKey.DIGIT_8 to "Lit_8",
        TvKey.DIGIT_9 to "Lit_9"
    )

    override suspend fun connect() = withContext(Dispatchers.IO) {
        stateFlow.value = ConnectionState.CONNECTING
        val code = request("query/device-info")
        if (code !in 200..299) {
            stateFlow.value = ConnectionState.FAILED
            throw RemoteException("Roku недоступен: HTTP " + code)
        }
        stateFlow.value = ConnectionState.READY
    }

    override suspend fun send(command: RemoteCommand) = withContext(Dispatchers.IO) {
        when (command) {
            is RemoteCommand.Key -> post("keypress/" + (map[command.code] ?: throw RemoteException("Roku: " + command.code)))
            is RemoteCommand.Text -> command.value.forEach { ch -> post("keypress/Lit_" + URLEncoder.encode(ch.toString(), "UTF-8")) }
            is RemoteCommand.PointerMove -> throw RemoteException("Roku ECP pointer не включён")
            RemoteCommand.PointerClick -> post("keypress/Select")
            RemoteCommand.PointerRightClick -> post("keypress/Back")
        }
    }

    override suspend fun disconnect() { stateFlow.value = ConnectionState.DISCONNECTED }

    private fun post(path: String): Int {
        val c = URL("http://" + info.host + ":8060/" + path).openConnection() as HttpURLConnection
        c.requestMethod = "POST"
        c.connectTimeout = 3500
        c.readTimeout = 3500
        c.doOutput = true
        c.outputStream.use { it.write(0) }
        return c.responseCode
    }

    private fun request(path: String): Int {
        val c = URL("http://" + info.host + ":8060/" + path).openConnection() as HttpURLConnection
        c.requestMethod = "GET"
        c.connectTimeout = 3500
        c.readTimeout = 3500
        return c.responseCode
    }
}
