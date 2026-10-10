package com.radiotv.control.core

import com.radiotv.control.core.proto.RemoteMessageProto

enum class RemoteKey(val wireCode: RemoteMessageProto.RemoteKeyCode) {
    UP(RemoteMessageProto.RemoteKeyCode.KEYCODE_DPAD_UP),
    DOWN(RemoteMessageProto.RemoteKeyCode.KEYCODE_DPAD_DOWN),
    LEFT(RemoteMessageProto.RemoteKeyCode.KEYCODE_DPAD_LEFT),
    RIGHT(RemoteMessageProto.RemoteKeyCode.KEYCODE_DPAD_RIGHT),
    OK(RemoteMessageProto.RemoteKeyCode.KEYCODE_DPAD_CENTER),
    BACK(RemoteMessageProto.RemoteKeyCode.KEYCODE_BACK),
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
sealed interface RemoteStatus {
    data object Disconnected : RemoteStatus
    data class Connecting(val host: String) : RemoteStatus
    data class AwaitingCode(val host: String) : RemoteStatus
    data class Connected(val host: String) : RemoteStatus
    data class Error(val message: String) : RemoteStatus
}
