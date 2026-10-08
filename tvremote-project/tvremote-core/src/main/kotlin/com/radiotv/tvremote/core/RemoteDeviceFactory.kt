package com.radiotv.tvremote.core
import android.content.Context
object RemoteDeviceFactory {
    fun create(context: Context, info: RemoteDeviceInfo): RemoteDevice = when (info.protocol) {
        DeviceProtocol.ANDROID_TV_V2 -> AndroidTvRemoteV2(context, info)
        DeviceProtocol.SAMSUNG_TIZEN -> SamsungTizenRemote(info.host, info.name, info.port)
        DeviceProtocol.LG_WEBOS -> LgWebOsRemote(context, info.host, info.name, info.port)
        DeviceProtocol.ROKU_ECP -> RokuEcpRemote(info.host, info.name)
    }
}
