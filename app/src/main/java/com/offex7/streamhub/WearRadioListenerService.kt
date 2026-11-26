package com.offex7.streamhub

import android.content.ComponentName
import android.os.Handler
import android.os.Looper
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

private const val WEAR_COMMAND_PATH = "/radio/command"
private const val WEAR_STATE_REQUEST_PATH = "/radio/state/request"
private const val WEAR_STATE_PATH = "/radio/state"

/** Receives Wear OS commands and controls the existing phone MediaSession. */
class WearRadioListenerService : com.google.android.gms.wearable.WearableListenerService() {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val queuedActions = mutableListOf<(MediaController) -> Unit>()
    private var mediaController: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)
        val nodeId = messageEvent.sourceNodeId
        val message = messageEvent.data.toString(Charsets.UTF_8)
        when (messageEvent.path) {
            WEAR_COMMAND_PATH -> mainHandler.post { handleCommand(nodeId, message) }
            WEAR_STATE_REQUEST_PATH -> mainHandler.post { sendState(nodeId) }
        }
    }

    private fun handleCommand(nodeId: String, command: String) {
        withController { controller ->
            when {
                command == "toggle" -> {
                    if (controller.isPlaying || controller.playWhenReady) {
                        controller.pause()
                    } else {
                        startPlayback(controller)
                    }
                }
                command == "next" -> moveStation(controller, 1)
                command == "previous" -> moveStation(controller, -1)
                command.startsWith("play:") -> {
                    val index = command.substringAfter("play:").toIntOrNull()
                    if (index != null && index in 0 until minOf(controller.mediaItemCount, RADIO_STATIONS.size)) {
                        controller.seekTo(index, 0L)
                        startPlayback(controller)
                    }
                }
            }
            mainHandler.postDelayed({ sendState(nodeId) }, 350L)
        }
    }

    private fun moveStation(controller: MediaController, direction: Int) {
        val count = minOf(controller.mediaItemCount, RADIO_STATIONS.size)
        if (count <= 0) return
        val current = controller.currentMediaItemIndex.takeIf { it in 0 until count } ?: 0
        controller.seekTo((current + direction + count) % count, 0L)
        startPlayback(controller)
    }

    private fun startPlayback(controller: MediaController) {
        if (controller.playbackState == Player.STATE_IDLE) controller.prepare()
        controller.play()
    }

    private fun withController(action: (MediaController) -> Unit) {
        mediaController?.let { active ->
            action(active)
            return
        }
        queuedActions += action
        if (controllerFuture != null) return

        val future = MediaController.Builder(
            this,
            SessionToken(this, ComponentName(this, RadioPlaybackService::class.java))
        ).buildAsync()
        controllerFuture = future
        future.addListener({
            controllerFuture = null
            val connected = runCatching { future.get() }.getOrElse { error ->
                LogExporter.log("Wear OS MediaSession connection failed: " + (error.message ?: error.javaClass.simpleName))
                null
            }
            if (connected == null) {
                queuedActions.clear()
                return@addListener
            }
            mediaController = connected
            val pending = queuedActions.toList()
            queuedActions.clear()
            pending.forEach { callback -> runCatching { callback(connected) } }
        }, mainExecutor)
    }

    private fun sendState(nodeId: String) {
        withController { controller ->
            val stationName = RADIO_STATIONS.getOrNull(controller.currentMediaItemIndex)?.name
                ?: controller.mediaMetadata.title?.toString()?.takeIf { it.isNotBlank() }
                ?: "Ничего не играет"
            val playing = controller.isPlaying || controller.playWhenReady

            ioScope.launch {
                val favoriteIds = runCatching {
                    SettingsStore(applicationContext).favorites(Section.RADIO)
                }.getOrDefault(emptySet())
                val favoriteArray = JSONArray()
                RADIO_STATIONS.forEachIndexed { index, station ->
                    if (station.key in favoriteIds) {
                        favoriteArray.put(
                            JSONObject().put("name", station.name).put("index", index)
                        )
                    }
                }
                val payload = JSONObject()
                    .put("station", stationName)
                    .put("playing", playing)
                    .put("favorites", favoriteArray)
                    .toString()
                    .toByteArray(Charsets.UTF_8)

                runCatching {
                    Wearable.getMessageClient(applicationContext)
                        .sendMessage(nodeId, WEAR_STATE_PATH, payload)
                        .addOnFailureListener { error ->
                            LogExporter.log("Wear OS state send failed: " + (error.message ?: error.javaClass.simpleName))
                        }
                }.onFailure { error ->
                    LogExporter.log("Wear OS state response failed: " + (error.message ?: error.javaClass.simpleName))
                }
            }
        }
    }

    override fun onDestroy() {
        controllerFuture?.cancel(false)
        controllerFuture = null
        mediaController?.release()
        mediaController = null
        ioScope.cancel()
        mainHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
