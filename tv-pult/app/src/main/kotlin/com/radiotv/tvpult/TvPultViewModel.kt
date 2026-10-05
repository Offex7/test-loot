package com.radiotv.tvpult

import android.app.Application
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.radiotv.tvremote.androidtv.AndroidTvIdentity
import com.radiotv.tvremote.androidtv.AndroidTvRemoteDevice
import com.radiotv.tvremote.androidtv.protocol.PairingSession
import com.radiotv.tvremote.core.RemoteConnectionState
import com.radiotv.tvremote.core.RemoteDevice
import com.radiotv.tvremote.core.RemoteKey
import com.radiotv.tvremote.core.RemoteVolume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class FoundTv(val name: String, val host: String, val port: Int)

sealed interface TvScreen {
    data object Connect : TvScreen
    data class Pairing(val host: String, val name: String) : TvScreen
    data class Remote(val host: String, val name: String) : TvScreen
}

class TvPultViewModel(app: Application) : AndroidViewModel(app) {
    private val nsd = app.getSystemService(NsdManager::class.java)
    private val foundMap = linkedMapOf<String, FoundTv>()

    private val _screen = MutableStateFlow<TvScreen>(TvScreen.Connect)
    val screen: StateFlow<TvScreen> = _screen.asStateFlow()

    private val _found = MutableStateFlow<List<FoundTv>>(emptyList())
    val found: StateFlow<List<FoundTv>> = _found.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _connection = MutableStateFlow<RemoteConnectionState>(RemoteConnectionState.Disconnected)
    val connection: StateFlow<RemoteConnectionState> = _connection.asStateFlow()

    private val _volume = MutableStateFlow<RemoteVolume?>(null)
    val volume: StateFlow<RemoteVolume?> = _volume.asStateFlow()

    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var pairing: PairingSession? = null
    private var remote: RemoteDevice? = null
    private var remoteJob: Job? = null

    fun discover() {
        if (discoveryListener != null) return
        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) = Unit
            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                if (!serviceInfo.serviceType.contains("_androidtvremote2._tcp")) return
                runCatching {
                    nsd.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) = Unit
                        override fun onServiceResolved(info: NsdServiceInfo) {
                            val host = info.host?.hostAddress ?: return
                            foundMap[host] = FoundTv(info.serviceName ?: host, host, info.port)
                            _found.value = foundMap.values.sortedBy { it.name }
                        }
                    })
                }
            }
            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                val name = serviceInfo.serviceName ?: return
                foundMap.entries.removeIf { it.value.name == name }
                _found.value = foundMap.values.sortedBy { it.name }
            }
            override fun onDiscoveryStopped(serviceType: String) = Unit
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                _message.value = "Автопоиск недоступен. Введите IP телевизора вручную."
                discoveryListener = null
            }
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) = Unit
        }
        discoveryListener = listener
        runCatching {
            nsd.discoverServices("_androidtvremote2._tcp", NsdManager.PROTOCOL_DNS_SD, listener)
        }.onFailure {
            discoveryListener = null
            _message.value = "Автопоиск недоступен. Введите IP вручную."
        }
    }

    fun stopDiscovery() {
        val listener = discoveryListener ?: return
        runCatching { nsd.stopServiceDiscovery(listener) }
        discoveryListener = null
    }

    fun beginPairing(host: String, name: String) {
        val clean = host.trim()
        if (clean.isBlank()) {
            _message.value = "Введите IP-адрес телевизора."
            return
        }
        _busy.value = true
        _message.value = null
        viewModelScope.launch {
            closePairing()
            try {
                val keyStore = withContext(Dispatchers.IO) { AndroidTvIdentity.keyStore(getApplication()) }
                val session = PairingSession(
                    host = clean,
                    keyStore = keyStore,
                    password = AndroidTvIdentity.password,
                    clientName = "TV пульт",
                )
                pairing = session
                val reportedName = session.begin()
                _screen.value = TvScreen.Pairing(clean, reportedName?.takeIf { it.isNotBlank() } ?: name.ifBlank { clean })
            } catch (t: Throwable) {
                _message.value = t.message ?: "Не удалось начать сопряжение."
                closePairing()
            } finally {
                _busy.value = false
            }
        }
    }

    fun submitPairingCode(code: String) {
        val session = pairing ?: return
        val target = _screen.value as? TvScreen.Pairing ?: return
        val normalized = code.trim().uppercase()
        if (normalized.length != 6) {
            _message.value = "Код должен содержать 6 шестнадцатеричных символов."
            return
        }
        _busy.value = true
        viewModelScope.launch {
            try {
                session.complete(normalized)
                closePairing()
                openRemote(target.host, target.name)
            } catch (t: Throwable) {
                _message.value = t.message ?: "Телевизор отклонил код."
            } finally {
                _busy.value = false
            }
        }
    }

    fun cancelPairing() {
        closePairing()
        _screen.value = TvScreen.Connect
        _busy.value = false
    }

    fun openRemote(host: String, name: String) {
        _screen.value = TvScreen.Remote(host, name)
        connectRemote(host)
    }

    fun backToConnect() {
        teardownRemote()
        _screen.value = TvScreen.Connect
    }

    fun send(key: RemoteKey) {
        runCatching { remote?.send(key) }.onFailure {
            _message.value = "Команда не отправлена: " + (it.message ?: "ошибка")
        }
    }

    fun sendText(text: String) {
        if (text.isBlank()) return
        runCatching { remote?.sendText(text) }.onFailure {
            _message.value = "Телевизор не принял текст."
        }
    }

    fun clearMessage() { _message.value = null }

    fun showInfo(message: String) { _message.value = message }

    private fun connectRemote(host: String) {
        teardownRemote()
        remoteJob = viewModelScope.launch {
            try {
                val ks = withContext(Dispatchers.IO) { AndroidTvIdentity.keyStore(getApplication()) }
                val device = AndroidTvRemoteDevice(host, ks, AndroidTvIdentity.password)
                remote = device
                launch { device.state.collect { _connection.value = it } }
                launch { device.volume.collect { _volume.value = it } }
                device.connect()
            } catch (t: Throwable) {
                _connection.value = RemoteConnectionState.Failed(t.message ?: "Ошибка соединения")
            }
        }
    }

    private fun closePairing() {
        pairing?.let { runCatching { it.close() } }
        pairing = null
    }

    private fun teardownRemote() {
        remoteJob?.cancel()
        remoteJob = null
        remote?.disconnect()
        remote = null
        _connection.value = RemoteConnectionState.Disconnected
        _volume.value = null
    }

    override fun onCleared() {
        stopDiscovery()
        closePairing()
        teardownRemote()
        super.onCleared()
    }
}
