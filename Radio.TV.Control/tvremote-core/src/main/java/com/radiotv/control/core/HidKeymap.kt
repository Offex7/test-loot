package com.radiotv.control.core

internal sealed interface HidInputCommand {
    data class Keyboard(val usage: Int, val modifier: Int = 0) : HidInputCommand
    data class Consumer(val usage: Int) : HidInputCommand
}

internal fun hidInputFor(key: RemoteKey): HidInputCommand = when (key) {
    RemoteKey.UP -> HidInputCommand.Keyboard(0x52)
    RemoteKey.DOWN -> HidInputCommand.Keyboard(0x51)
    RemoteKey.LEFT -> HidInputCommand.Keyboard(0x50)
    RemoteKey.RIGHT -> HidInputCommand.Keyboard(0x4F)
    RemoteKey.OK -> HidInputCommand.Keyboard(0x28)
    RemoteKey.BACK -> HidInputCommand.Keyboard(0x29)
    RemoteKey.HOME -> HidInputCommand.Keyboard(0x4A)
    RemoteKey.MENU -> HidInputCommand.Keyboard(0x65)
    RemoteKey.POWER -> HidInputCommand.Consumer(0x0030)
    RemoteKey.MUTE -> HidInputCommand.Consumer(0x00E2)
    RemoteKey.VOLUME_UP -> HidInputCommand.Consumer(0x00E9)
    RemoteKey.VOLUME_DOWN -> HidInputCommand.Consumer(0x00EA)
    RemoteKey.CHANNEL_UP -> HidInputCommand.Consumer(0x009C)
    RemoteKey.CHANNEL_DOWN -> HidInputCommand.Consumer(0x009D)
    RemoteKey.SOURCE -> HidInputCommand.Consumer(0x00B8)
    RemoteKey.PLAY_PAUSE -> HidInputCommand.Consumer(0x00CD)
    RemoteKey.STOP -> HidInputCommand.Consumer(0x00B7)
    RemoteKey.REWIND -> HidInputCommand.Consumer(0x00B4)
    RemoteKey.FAST_FORWARD -> HidInputCommand.Consumer(0x00B3)
    RemoteKey.NUMBER_0 -> HidInputCommand.Keyboard(0x27)
    RemoteKey.NUMBER_1 -> HidInputCommand.Keyboard(0x1E)
    RemoteKey.NUMBER_2 -> HidInputCommand.Keyboard(0x1F)
    RemoteKey.NUMBER_3 -> HidInputCommand.Keyboard(0x20)
    RemoteKey.NUMBER_4 -> HidInputCommand.Keyboard(0x21)
    RemoteKey.NUMBER_5 -> HidInputCommand.Keyboard(0x22)
    RemoteKey.NUMBER_6 -> HidInputCommand.Keyboard(0x23)
    RemoteKey.NUMBER_7 -> HidInputCommand.Keyboard(0x24)
    RemoteKey.NUMBER_8 -> HidInputCommand.Keyboard(0x25)
    RemoteKey.NUMBER_9 -> HidInputCommand.Keyboard(0x26)
}

internal fun asciiHidUsage(char: Char): Pair<Int, Int>? {
    val shifted = char.isUpperCase()
    val c = char.lowercaseChar()
    return when (c) {
        in 'a'..'z' -> (0x04 + c.code - 'a'.code) to if (shifted) 0x02 else 0
        in '1'..'9' -> (0x1E + c.code - '1'.code) to 0
        '0' -> 0x27 to 0
        ' ' -> 0x2C to 0
        '\n', '\r' -> 0x28 to 0
        '\t' -> 0x2B to 0
        '!' -> 0x1E to 0x02
        '@' -> 0x1F to 0x02
        '#' -> 0x20 to 0x02
        '$' -> 0x21 to 0x02
        '%' -> 0x22 to 0x02
        '^' -> 0x23 to 0x02
        '&' -> 0x24 to 0x02
        '*' -> 0x25 to 0x02
        '(' -> 0x26 to 0x02
        ')' -> 0x27 to 0x02
        '-' -> 0x2D to 0
        '_' -> 0x2D to 0x02
        '=' -> 0x2E to 0
        '+' -> 0x2E to 0x02
        '[' -> 0x2F to 0
        ']' -> 0x30 to 0
        ';' -> 0x33 to 0
        ':' -> 0x33 to 0x02
        ',' -> 0x36 to 0
        '.' -> 0x37 to 0
        '/' -> 0x38 to 0
        '?' -> 0x38 to 0x02
        else -> null
    }
}
