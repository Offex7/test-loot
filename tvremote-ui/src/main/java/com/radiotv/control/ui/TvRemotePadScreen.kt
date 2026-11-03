package com.radiotv.control.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.radiotv.control.core.RemoteKey

/**
 * Public, integration-friendly entry point for the Radio.TV remote tab.
 *
 * The host app owns navigation, connection state, protocol selection and command dispatch.
 * This UI module does not depend on an Activity or application module.
 */
@Composable
fun TvRemotePadScreen(
    enabled: Boolean,
    onKey: (RemoteKey) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onFeatureAction: (RemoteFeatureAction) -> Unit = {},
    touchpadActive: Boolean = false,
    airMouseActive: Boolean = false,
    muteActive: Boolean = false,
    voiceActive: Boolean = false,
    voiceTranscript: String? = null,
    keyboardActive: Boolean = false,
    featureMessage: String? = null,
    showFeatureActions: Boolean = false,
    energySaving: Boolean = false,
    hapticsEnabled: Boolean = true,
    deviceName: String = "Телевизор не подключён",
    searching: Boolean = false,
    onSettings: () -> Unit = {},
    onStartSearch: () -> Unit = {}
) {
    TvRemotePad(
        enabled = enabled,
        onKey = onKey,
        modifier = modifier,
        onFeatureAction = onFeatureAction,
        touchpadActive = touchpadActive,
        airMouseActive = airMouseActive,
        muteActive = muteActive,
        voiceActive = voiceActive,
        voiceTranscript = voiceTranscript,
        keyboardActive = keyboardActive,
        featureMessage = featureMessage,
        showFeatureActions = showFeatureActions,
        energySaving = energySaving,
        hapticsEnabled = hapticsEnabled,
        deviceName = deviceName,
        searching = searching,
        onSettings = onSettings,
        onBack = onBack,
        onStartSearch = onStartSearch
    )
}
