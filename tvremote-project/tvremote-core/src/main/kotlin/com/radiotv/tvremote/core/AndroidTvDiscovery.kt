package com.radiotv.tvremote.core

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import java.util.concurrent.ConcurrentHashMap

class AndroidTvDiscovery(private val context: Context) {
    suspend fun scan(timeoutMs: Long = 4500): List<RemoteDeviceInfo> {
        val nsd = context.getSystemService(Context.NSD_SERVICE) as NsdManager
        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val results = ConcurrentHashMap<String, RemoteDeviceInfo>()
        val lock = runCatching {
            wifi.createMulticastLock("radiotv-tvremote-mdns").apply {
                setReferenceCounted(false)
                acquire()
            }
        }.getOrNull()

        return try {
            suspendCancellableCoroutine { continuation ->
                val listener = object : NsdManager.DiscoveryListener {
                    override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {
                        if (continuation.isActive) continuation.resume(results.values.toList())
                    }
                    override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {
                        if (continuation.isActive) continuation.resume(results.values.toList())
                    }
                    override fun onDiscoveryStarted(serviceType: String?) = Unit
                    override fun onDiscoveryStopped(serviceType: String?) {
                        if (continuation.isActive) continuation.resume(results.values.toList())
                    }
                    override fun onServiceLost(serviceInfo: NsdServiceInfo?) = Unit
                    override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                        runCatching {
                            nsd.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                                override fun onResolveFailed(info: NsdServiceInfo?, errorCode: Int) = Unit
                                override fun onServiceResolved(info: NsdServiceInfo) {
                                    val host = info.host?.hostAddress ?: return
                                    results[host] = RemoteDeviceInfo(
                                        "atv:" + host,
                                        info.serviceName.ifBlank { "Android TV" },
                                        host,
                                        6466,
                                        DeviceProtocol.ANDROID_TV_V2,
                                        info.attributes["md"]?.toString(Charsets.UTF_8)
                                    )
                                }
                            })
                        }
                    }
                }
                runCatching {
                    nsd.discoverServices("_androidtvremote2._tcp.", NsdManager.PROTOCOL_DNS_SD, listener)
                }.onFailure {
                    if (continuation.isActive) continuation.resume(results.values.toList())
                }
                continuation.invokeOnCancellation { runCatching { nsd.stopServiceDiscovery(listener) } }
                CoroutineScope(Dispatchers.Main).launch {
                    delay(timeoutMs)
                    runCatching { nsd.stopServiceDiscovery(listener) }
                    if (continuation.isActive) continuation.resume(results.values.toList())
                }
            }
        } finally {
            runCatching { lock?.release() }
        }
    }
}
