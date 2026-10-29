package com.radiotv.control.core

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface VoiceInputStatus {
    data object Off : VoiceInputStatus
    data object Starting : VoiceInputStatus
    data object Recording : VoiceInputStatus
    data class Error(val message: String) : VoiceInputStatus
}

/** PCM16 mono/8 kHz recorder + Android TV Remote v2 voice-stream transport. */
class AndroidTvVoiceInputController(
    context: Context,
    private val remote: AndroidTvRemoteV2Transport
) : AutoCloseable {
    private val appContext = context.applicationContext
    private val stateMutable = MutableStateFlow<VoiceInputStatus>(VoiceInputStatus.Off)
    val status: StateFlow<VoiceInputStatus> = stateMutable.asStateFlow()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile private var recorder: AudioRecord? = null
    @Volatile private var readerJob: Job? = null
    @Volatile private var activeSessionId: Int? = null

    suspend fun start(): String = withContext(Dispatchers.IO) {
        check(stateMutable.value !is VoiceInputStatus.Recording &&
            stateMutable.value !is VoiceInputStatus.Starting) { "Голосовой ввод уже активен." }
        check(appContext.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            "Разрешите доступ к микрофону для голосового ввода."
        }
        check(remote.status.value is RemoteStatus.Connected) {
            "Для голосового ввода сначала подключитесь к Android TV по Wi-Fi."
        }
        stateMutable.value = VoiceInputStatus.Starting

        var audio: AudioRecord? = null
        var sessionId: Int? = null
        try {
            val minBuffer = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            check(minBuffer > 0) { "Android не поддерживает запись PCM 16-bit mono 8 kHz на этом устройстве." }
            val bufferSize = maxOf(minBuffer, VOICE_PACKET_BYTES * 2)
            audio = AudioRecord.Builder()
                .setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .build()
            check(audio.state == AudioRecord.STATE_INITIALIZED) { "Не удалось инициализировать микрофон." }
            sessionId = remote.startVoiceSession()
            audio.startRecording()
            recorder = audio
            activeSessionId = sessionId
            stateMutable.value = VoiceInputStatus.Recording
            val activeAudio = audio
            val activeId = sessionId
            readerJob = scope.launch {
                val packet = ByteArray(VOICE_PACKET_BYTES)
                var filled = 0
                try {
                    while (currentCoroutineContext().isActive) {
                        val count = activeAudio.read(
                            packet,
                            filled,
                            packet.size - filled,
                            AudioRecord.READ_BLOCKING
                        )
                        if (count < 0) error("Ошибка чтения микрофона: $count")
                        if (count == 0) continue
                        filled += count
                        if (filled == packet.size) {
                            remote.sendVoiceChunk(activeId, packet)
                            filled = 0
                        }
                    }
                } catch (cancel: CancellationException) {
                    throw cancel
                } catch (error: Exception) {
                    stateMutable.value = VoiceInputStatus.Error(error.message ?: "Ошибка передачи голосового аудио.")
                    runCatching { remote.endVoiceSession(activeId) }
                    runCatching { if (activeAudio.recordingState == AudioRecord.RECORDSTATE_RECORDING) activeAudio.stop() }
                    activeAudio.release()
                    if (recorder === activeAudio) recorder = null
                    activeSessionId = null
                }
            }
            "Голосовой ввод включён. Говорите; повторное нажатие остановит запись."
        } catch (error: Exception) {
            sessionId?.let { runCatching { remote.endVoiceSession(it) } }
            runCatching { if (audio?.recordingState == AudioRecord.RECORDSTATE_RECORDING) audio.stop() }
            runCatching { audio?.release() }
            recorder = null
            activeSessionId = null
            stateMutable.value = VoiceInputStatus.Error(error.message ?: "Не удалось начать голосовой ввод.")
            throw error
        }
    }

    suspend fun stop(): String = withContext(Dispatchers.IO) {
        val audio = recorder
        val session = activeSessionId
        runCatching {
            if (audio?.recordingState == AudioRecord.RECORDSTATE_RECORDING) audio.stop()
        }
        readerJob?.cancelAndJoin()
        readerJob = null
        runCatching { audio?.release() }
        recorder = null
        activeSessionId = null
        if (session != null) runCatching { remote.endVoiceSession(session) }
        if (stateMutable.value !is VoiceInputStatus.Error) stateMutable.value = VoiceInputStatus.Off
        "Голосовая запись остановлена."
    }

    suspend fun toggle(): String =
        if (stateMutable.value is VoiceInputStatus.Recording || stateMutable.value is VoiceInputStatus.Starting) stop()
        else start()

    override fun close() {
        runCatching { if (recorder?.recordingState == AudioRecord.RECORDSTATE_RECORDING) recorder?.stop() }
        readerJob?.cancel()
        runCatching { recorder?.release() }
        recorder = null
        val session = activeSessionId
        activeSessionId = null
        if (session != null) scope.launch { runCatching { remote.endVoiceSession(session) } }
        scope.cancel()
        stateMutable.value = VoiceInputStatus.Off
    }

    private companion object {
        const val SAMPLE_RATE = 8_000
        const val VOICE_PACKET_BYTES = 8 * 1024
    }
}
