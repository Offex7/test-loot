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
    keyboardActive: Boolean = false,
    featureMessage: String? = null,
    showFeatureActions: Boolean = false,
    deviceName: String = "Телевизор не подключён",
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
        keyboardActive = keyboardActive,
        featureMessage = featureMessage,
        showFeatureActions = showFeatureActions,
        deviceName = deviceName,
        onSettings = onSettings,
        onBack = onBack,
        onStartSearch = onStartSearch
    )
}
