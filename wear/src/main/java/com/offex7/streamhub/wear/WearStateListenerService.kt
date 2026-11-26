package com.offex7.streamhub.wear

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

private const val WATCH_STATE_PATH = "/radio/state"

data class WearFavoriteStation(val name: String, val index: Int)

data class WearRadioState(
    val stationName: String = "",
    val playing: Boolean = false,
    val favorites: List<WearFavoriteStation> = emptyList()
)

object WearRadioStateHolder {
    private val mutableState = MutableStateFlow(WearRadioState())
    val state = mutableState.asStateFlow()

    fun update(value: WearRadioState) {
        mutableState.value = value
    }
}

class WearStateListenerService : WearableListenerService() {
    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)
        if (messageEvent.path != WATCH_STATE_PATH) return
        runCatching {
            val json = JSONObject(messageEvent.data.toString(Charsets.UTF_8))
            val favoritesJson = json.optJSONArray("favorites")
            val favorites = buildList {
                if (favoritesJson != null) {
                    for (index in 0 until favoritesJson.length()) {
                        val item = favoritesJson.optJSONObject(index) ?: continue
                        val stationIndex = item.optInt("index", -1)
                        val name = item.optString("name").trim()
                        if (stationIndex >= 0 && name.isNotBlank()) add(WearFavoriteStation(name, stationIndex))
                    }
                }
            }
            WearRadioStateHolder.update(
                WearRadioState(
                    stationName = json.optString("station", ""),
                    playing = json.optBoolean("playing", false),
                    favorites = favorites
                )
            )
        }
    }
}
