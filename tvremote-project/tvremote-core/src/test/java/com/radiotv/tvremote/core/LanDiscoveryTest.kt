package com.radiotv.tvremote.core
import org.junit.Test
class LanDiscoveryTest {
    @Test fun modelLayerLoads() {
        assert(DeviceProtocol.values().contains(DeviceProtocol.ANDROID_TV_V2))
        assert(DeviceProtocol.values().contains(DeviceProtocol.SAMSUNG_TIZEN))
        assert(DeviceProtocol.values().contains(DeviceProtocol.LG_WEBOS))
        assert(DeviceProtocol.values().contains(DeviceProtocol.ROKU_ECP))
    }
}
