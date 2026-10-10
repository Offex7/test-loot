# Radio.TV.Control

Standalone Kotlin + Jetpack Compose TV-remote module, structured for later integration as a tab in Radio.TV. The branch contains the demo host and reusable modules; CI checks buildability but cannot certify compatibility with every TV firmware.

## Modules

- `:app` — standalone demo, discovery, connection and settings tabs.
- `:tvremote-core` — transport contracts, Android TV Remote v2 voice/key sessions, Bluetooth HID, gyro air-mouse controller and other TV protocol adapters.
- `:tvremote-ui` — reusable `TvRemotePad` Compose UI.
- `:tvremote-cast` — DLNA/UPnP discovery, AVTransport commands and an on-demand local HTTP server for a user-selected media URI.

**Application ID:** `com.radiotv.control`  
**Minimum Android:** 12 / API 31  
**Compile SDK / target SDK:** API 37  
**UI palette:** black/red with neutral white/gray  
**Orientation and layout:** portrait requested in the manifest; UI also caps width in code for large-screen/landscape configurations.

## Build and install

Requirements: JDK 17, Android SDK platform 37, Build Tools 37.0.0, Gradle 9.5.0.

```bash
cd Radio.TV.Control
gradle --no-daemon :app:assembleDebug :tvremote-core:test
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

R8 minification and resource shrinking are enabled for both debug and release builds. The CI APK is debug-signed, for testing rather than Play Store distribution.

## UI, insets, and large screens

The root content in `app/src/main/java/com/radiotv/control/MainActivity.kt` is wrapped in `WindowInsets.safeDrawing`. Compose's safe drawing insets reserve space for the display cutout and system bars, including the bottom navigation area. The bottom app navigation is placed inside this inset-aware column.

The root uses `BoxWithConstraints` to detect landscape dimensions and centers a width-capped black panel: up to 520 dp in portrait and 460 dp in landscape. The reusable remote's height and button sizes also shrink when the available height is short. This is a programmatic content letterbox; the app does not depend solely on `android:screenOrientation="portrait"`, which a large-screen Android version may disregard. The result should still be visually checked on the actual target tablet/foldable and its system-bar configuration.

Palette colors used in `tvremote-ui/.../TvRemotePad.kt`:

| Hex | Use |
| --- | --- |
| `#000000` | Root / letterbox background |
| `#0A0A0A` | Surface |
| `#141414` | Reserved older raised tone (not an accent) |
| `#252525` | Raised controls / button background |
| `#E53935` | Active red / primary accent / D-pad outline |
| `#D32F2F` | Deep red for active containers |
| `#FFFFFF` | Main text and icons |
| `#B0B0B0` | Secondary text and icons |
| `#383838` | Neutral button borders |
| `#651B1B` | Dark red outline variant |
| `#FF5252` | Error state |

The Material 3 color scheme explicitly assigns primary, secondary, tertiary, background, surface, container, outline and error roles to this red/black/white/gray palette. Some operating-system dialogs (such as the system document picker, permission prompts and Bluetooth pairing screens) remain Android system UI and may use the device's system theme.

## Remote actions and status feedback

- **Microphone / Android TV voice:** tapping the voice action checks the Wi-Fi Android TV Remote v2 connection, requests `RECORD_AUDIO` when needed, starts a PCM 16-bit mono / 8 kHz `AudioRecord` stream and opens a Remote v2 voice session. The red active state indicates recording. Voice streaming is specific to Android TV Remote v2; a successful compile does not prove compatibility with every TV firmware.
- **Air mouse:** the action checks that the phone exposes a gyroscope and Bluetooth HID is connected. When HID is not connected, the app starts the Bluetooth discovery/advertising flow and explains that the TV must select the phone from its Bluetooth settings. Active mode is shown red; missing sensor/connection errors are reported in the control tab. The HID Device role depends on phone vendor firmware.
- **Touchpad:** swipes, tap, and long press are forwarded as relative Bluetooth HID mouse reports when the TV is connected as an HID host.
- **Media casting:** the system document picker can select photos/videos, audio or any file. The cast module reads the selected `content://` URI with Android's `ContentResolver`, serves it through an on-demand LAN HTTP endpoint (including byte-range requests), and sends that absolute URL to a selected DLNA/UPnP AVTransport renderer using `SetAVTransportURI` / `Play`. The phone and TV must be on the same LAN and the router must allow clients to communicate. Play/Pause/Stop and best-effort relative seek commands are exposed; renderer support varies. DRM-protected streams and HLS/DASH playlists are not supported by this local-file path. This is **DLNA casting**, not Android's system screen mirroring; MediaProjection/Cast receiver integration is not included.

## TV discovery and protocols

Discovery uses Android NSD/mDNS, SSDP/M-SEARCH and bounded TCP probing. The subnet scan is capped at /24. Results are best-effort device-type guesses, not a guarantee that the matching protocol can control the device.

- Android TV Remote v2: TLS pairing, saved host/certificate reconnection, key input, text/IME and voice stream foundations.
- Bluetooth HID: keyboard/media/mouse reports and gyroscope-driven relative cursor.
- Samsung Tizen / LG webOS / Roku ECP: adapter implementations are present in `:tvremote-core`; they still need real-device validation across models and firmware revisions.
- DLNA / UPnP: renderer discovery and AVTransport play/pause/stop/seek commands.
- Google Cast and AirPlay: service discovery exists, but full control/cast adapters are not implemented by this prototype.
- External IR hardware is not implemented.

Do not assume any protocol has been physically validated solely because its classes are present in the APK. The CI workflow builds, runs core unit tests and verifies the package, manifest, selected DEX classes, signature, hash and size. The optional emulator screenshots show the UI only and do not test pairing with a physical TV.

## Public Compose API

The reusable public Composable is `TvRemotePad` in package `com.radiotv.control.ui`:

```kotlin
@Composable
fun TvRemoteTab(
    remoteIsConnected: Boolean,
    onRemoteKey: (RemoteKey) -> Unit,
    onRemoteFeature: (RemoteFeatureAction) -> Unit,
    modifier: Modifier = Modifier
) {
    TvRemotePad(
        enabled = remoteIsConnected,
        onKey = onRemoteKey,
        onFeatureAction = onRemoteFeature,
        modifier = modifier,
        showFeatureActions = true
    )
}
```

To integrate it in Radio.TV, include the modules in the host Gradle project:

```kotlin
implementation(project(":tvremote-core"))
implementation(project(":tvremote-ui"))
implementation(project(":tvremote-cast"))
```

Then render it from the host's tab navigation:

```kotlin
when (selectedRadioTvTab) {
    RadioTvTab.TV_REMOTE -> TvRemoteTab(
        remoteIsConnected = radioTvRemoteConnected,
        onRemoteKey = { key ->
            lifecycleScope.launch { radioTvTransport.sendKey(key) }
        },
        onRemoteFeature = { action ->
            // Dispatch to the host-owned microphone / keyboard / air-mouse /
            // touchpad / casting and permission flows for that tab.
            handleTvRemoteFeature(action)
        },
        modifier = Modifier.fillMaxSize()
    )
    else -> ExistingRadioTvTabContent()
}
```

The UI module emits `RemoteKey` and `RemoteFeatureAction` callbacks; it does not own a standalone Activity or connection singleton. The Radio.TV host must provide its live connection state and action dispatch. For local media handoff, use `LocalMediaHttpServer` together with `DlnaCastController` and a selected renderer.

## Safe-area and device-validation checklist

- [ ] Test a phone with a camera cutout and gestures navigation.
- [ ] Test a phone with three-button navigation.
- [ ] Test a tablet / foldable in portrait and landscape at smallest-width >= 600 dp.
- [ ] Test permission-denial/retry flows for microphone and Bluetooth.
- [ ] Test local media cast against a real DLNA renderer and check seeking support.

The code uses safe drawing insets and width/height constraints for these cases, but the checks above require the actual form factor or an appropriately configured emulator before calling device-specific behavior verified.
