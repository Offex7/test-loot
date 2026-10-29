package com.radiotv.control.core

import com.radiotv.control.core.proto.RemoteMessageProto

enum class RemoteKey(val wireCode: RemoteMessageProto.RemoteKeyCode) {
    UP(RemoteMessageProto.RemoteKeyCode.KEYCODE_DPAD_UP),
    DOWN(RemoteMessageProto.RemoteKeyCode.KEYCODE_DPAD_DOWN),
    LEFT(RemoteMessageProto.RemoteKeyCode.KEYCODE_DPAD_LEFT),
    RIGHT(RemoteMessageProto.RemoteKeyCode.KEYCODE_DPAD_RIGHT),
    OK(RemoteMessageProto.RemoteKeyCode.KEYCODE_DPAD_CENTER),
    BACK(RemoteMessageProto.RemoteKeyCode.KEYCODE_BACK),
    EXIT(RemoteMessageProto.RemoteKeyCode.KEYCODE_ESCAPE),
    HOME(RemoteMessageProto.RemoteKeyCode.KEYCODE_HOME),
    MENU(RemoteMessageProto.RemoteKeyCode.KEYCODE_MENU),
    POWER(RemoteMessageProto.RemoteKeyCode.KEYCODE_POWER),
    MUTE(RemoteMessageProto.RemoteKeyCode.KEYCODE_VOLUME_MUTE),
    VOLUME_UP(RemoteMessageProto.RemoteKeyCode.KEYCODE_VOLUME_UP),
    VOLUME_DOWN(RemoteMessageProto.RemoteKeyCode.KEYCODE_VOLUME_DOWN),
    CHANNEL_UP(RemoteMessageProto.RemoteKeyCode.KEYCODE_CHANNEL_UP),
    CHANNEL_DOWN(RemoteMessageProto.RemoteKeyCode.KEYCODE_CHANNEL_DOWN),
    SOURCE(RemoteMessageProto.RemoteKeyCode.KEYCODE_TV_INPUT),
    PLAY_PAUSE(RemoteMessageProto.RemoteKeyCode.KEYCODE_MEDIA_PLAY_PAUSE),
    PLAY(RemoteMessageProto.RemoteKeyCode.KEYCODE_MEDIA_PLAY),
    PAUSE(RemoteMessageProto.RemoteKeyCode.KEYCODE_MEDIA_PAUSE),
    STOP(RemoteMessageProto.RemoteKeyCode.KEYCODE_MEDIA_STOP),
    REWIND(RemoteMessageProto.RemoteKeyCode.KEYCODE_MEDIA_REWIND),
    FAST_FORWARD(RemoteMessageProto.RemoteKeyCode.KEYCODE_MEDIA_FAST_FORWARD),
    NUMBER_0(RemoteMessageProto.RemoteKeyCode.KEYCODE_0),
    NUMBER_1(RemoteMessageProto.RemoteKeyCode.KEYCODE_1),
    NUMBER_2(RemoteMessageProto.RemoteKeyCode.KEYCODE_2),
    NUMBER_3(RemoteMessageProto.RemoteKeyCode.KEYCODE_3),
    NUMBER_4(RemoteMessageProto.RemoteKeyCode.KEYCODE_4),
    NUMBER_5(RemoteMessageProto.RemoteKeyCode.KEYCODE_5),
    NUMBER_6(RemoteMessageProto.RemoteKeyCode.KEYCODE_6),
    NUMBER_7(RemoteMessageProto.RemoteKeyCode.KEYCODE_7),
    NUMBER_8(RemoteMessageProto.RemoteKeyCode.KEYCODE_8),
    NUMBER_9(RemoteMessageProto.RemoteKeyCode.KEYCODE_9)
}

/**
 * Converts the shared remote-key model to Android's input keycode.
 * Useful for Companion JSON commands and other transports that accept Android key events.
 */
fun RemoteKey.toAndroidKeyCode(): Int = when (this) {
    RemoteKey.UP -> 19
    RemoteKey.DOWN -> 20
    RemoteKey.LEFT -> 21
    RemoteKey.RIGHT -> 22
    RemoteKey.OK -> 23
    RemoteKey.BACK -> 4
    RemoteKey.EXIT -> 111
    RemoteKey.HOME -> 3
    RemoteKey.MENU -> 82
    RemoteKey.POWER -> 26
    RemoteKey.MUTE -> 164
    RemoteKey.VOLUME_UP -> 24
    RemoteKey.VOLUME_DOWN -> 25
    RemoteKey.CHANNEL_UP -> 166
    RemoteKey.CHANNEL_DOWN -> 167
    RemoteKey.SOURCE -> 178
    RemoteKey.PLAY_PAUSE -> 85
    RemoteKey.PLAY -> 126
    RemoteKey.PAUSE -> 127
    RemoteKey.STOP -> 86
    RemoteKey.REWIND -> 89
    RemoteKey.FAST_FORWARD -> 90
    RemoteKey.NUMBER_0 -> 7
    RemoteKey.NUMBER_1 -> 8
    RemoteKey.NUMBER_2 -> 9
    RemoteKey.NUMBER_3 -> 10
    RemoteKey.NUMBER_4 -> 11
    RemoteKey.NUMBER_5 -> 12
    RemoteKey.NUMBER_6 -> 13
    RemoteKey.NUMBER_7 -> 14
    RemoteKey.NUMBER_8 -> 15
    RemoteKey.NUMBER_9 -> 16
}

sealed interface RemoteStatus {
    data object Disconnected : RemoteStatus
    data class Connecting(val host: String) : RemoteStatus
    data class AwaitingCode(val host: String) : RemoteStatus
    data class Connected(val host: String) : RemoteStatus
    data class Error(val message: String) : RemoteStatus
}
