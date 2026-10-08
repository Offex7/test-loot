package com.radiotv.tvremote.core

enum class DeviceProtocol { ANDROID_TV_V2, SAMSUNG_TIZEN, LG_WEBOS, ROKU_ECP }
enum class ConnectionState { DISCONNECTED, CONNECTING, READY, PAIRING, FAILED }
enum class Capability {
    NAVIGATION, VOLUME, CHANNEL, NUMERIC, POWER, MUTE, INPUT, MEDIA,
    TEXT, VOICE, POINTER, AIR_MOUSE
}
sealed class RemoteCommand {
    data class Key(val code: TvKey, val longPress: Boolean = false) : RemoteCommand()
    data class Text(val value: String) : RemoteCommand()
    data class PointerMove(val dx: Int, val dy: Int) : RemoteCommand()
    data object PointerClick : RemoteCommand()
    data object PointerRightClick : RemoteCommand()
}
enum class TvKey(val androidKeyCode: Int) {
    POWER(26), MUTE(164), VOLUME_UP(24), VOLUME_DOWN(25),
    CHANNEL_UP(166), CHANNEL_DOWN(167),
    UP(19), DOWN(20), LEFT(21), RIGHT(22), OK(23),
    BACK(4), HOME(3), MENU(82), INPUT(178),
    PLAY_PAUSE(85), STOP(86), REWIND(89), FAST_FORWARD(90),
    NEXT(87), PREVIOUS(88), SEARCH(84), ENTER(66), DELETE(67),
    GUIDE(172), INFO(165), SETTINGS(176),
    RED(183), GREEN(184), YELLOW(185), BLUE(186),
    DIGIT_0(7), DIGIT_1(8), DIGIT_2(9), DIGIT_3(10), DIGIT_4(11),
    DIGIT_5(12), DIGIT_6(13), DIGIT_7(14), DIGIT_8(15), DIGIT_9(16)
}
data class RemoteDeviceInfo(
    val id: String,
    val name: String,
    val host: String,
    val port: Int,
    val protocol: DeviceProtocol,
    val model: String? = null,
    val vendor: String? = null
)
data class VolumeInfo(val level: Int, val max: Int, val muted: Boolean)
interface VoiceSession {
    suspend fun sendPcm(chunk: ByteArray)
    suspend fun finish()
}
interface RemoteDevice {
    val info: RemoteDeviceInfo
    val state: kotlinx.coroutines.flow.StateFlow<ConnectionState>
    val capabilities: Set<Capability>
    suspend fun pair(code: String) {}
    suspend fun connect()
    suspend fun disconnect()
    suspend fun send(command: RemoteCommand)
    fun supports(capability: Capability): Boolean = capability in capabilities
}
