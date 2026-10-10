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

R8 minification and resource shrinking are enabled for both debug and release builds.

### Versioning and signing

- The package ID remains **`com.radiotv.control`**. Do not add a debug application-ID suffix.
- GitHub Actions assigns **`versionCode = GITHUB_RUN_NUMBER`** and `versionName = 0.1.<run-number>`, so every successful CI run receives a higher version code. Local builds fall back to version code `1`; set `GITHUB_RUN_NUMBER` when building locally if you need an upgrade-compatible APK.
- Debug and release variants are explicitly signed with the same repository keystore at `../app/signing/radio-tv-v5-debug.keystore` (alias `AndroidDebugKey`). Keep that file and its credentials unchanged for later builds.
- Android permits in-place updates only when **application ID and signing certificate match** and the new `versionCode` is higher. The previous run №27 APK's signing certificate has not been compared here with this pinned key. If Android reports a signature mismatch when installing this new line of builds, uninstall the older copy once, then install an APK signed with the pinned key; later APKs using this same key and a higher version code should update in place. Removing the old app also removes its app-local data.
- This repository key is for development/testing builds, not Play Store publishing.

## UI, insets, and large screens

The root content in `app/src/main/java/com/radiotv/control/MainActivity.kt` is wrapped in `WindowInsets.safeDrawing`. Compose's safe drawing insets reserve space for the display cutout and system bars, including the bottom navigation area. The bottom app navigation is placed inside this inset-aware column.

The root uses `BoxWithConstraints` to detect `maxWidth > maxHeight` and centers a width-capped black panel: up to 520 dp in portrait and 460 dp in landscape. This is a **programmatic content letterbox**: on a wide/landscape screen, the vertical UI stays centered and the unused areas remain black; it does not rely only on `android:screenOrientation="portrait"`. The remote's dimensions also compact as available height shrinks. The CI screenshot script tests a landscape viewport with a computed smallest width above 600 dp; this confirms the composed layout can render under that configuration, but it does **not** prove Android 17/API 37's policy on every physical tablet or foldable.

Palette colors used in `tvremote-ui/.../TvRemotePad.kt`:

| Hex | Use |
| --- | --- |
| `#000000` | Root / letterbox background |
| `#0A0A0A` | Surface |
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
- **Media casting:** the system document picker can select photos/videos (`ACTION_OPEN_DOCUMENT`), audio (`ACTION_GET_CONTENT`) or any file (`ACTION_OPEN_DOCUMENT`). The cast module reads the selected `content://` URI with Android's `ContentResolver`, serves it through an on-demand LAN HTTP endpoint (including byte-range requests), and sends that absolute URL to a selected DLNA/UPnP AVTransport renderer using `SetAVTransportURI` / `Play`. The phone and TV must be on the same LAN and the router must allow clients to communicate. Play/Pause/Stop and best-effort relative seek commands are exposed; renderer support varies. HLS/DASH playlist files are rejected with a user-facing explanation. DRM is not supported by this local-file path. This is **DLNA casting**, not Android's system screen mirroring; MediaProjection/Cast receiver integration is not included.

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

The reusable public Composable is `TvRemotePad` in package `com.radiotv.control.ui`. Its required integration surface is `TvRemotePad(enabled, onKey, modifier, onFeatureAction)`; the additional state/display parameters have defaults and can be added when the host wants active-state highlighting or feature status inline. This four-argument API is enough to embed the pad as a Radio.TV tab provided the host dispatches key/action callbacks and supplies real connection state:

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

Implemented in source:
- `MainActivity.kt` calls `enableEdgeToEdge()` and wraps the centered content column in `WindowInsets.safeDrawing`; this reserves safe content space for status/navigation bars and display cutouts and keeps the bottom app tabs inside those insets.
- The same file detects landscape using `BoxWithConstraints`, caps the content width at 460 dp in landscape / 520 dp in portrait, centers the panel, and paints the remaining background black.
- The CI screenshot script captures the main remote, device-search screen, bottom-navigation screen and landscape viewport using `adb exec-out screencap -p`. The API 37 emulator step is allowed to fail without failing APK build, so check that the screenshot artifact is actually present for each run.

Still requires device-level confirmation:
- [ ] A physical phone with a camera cutout and gesture navigation.
- [ ] A phone using three-button navigation.
- [ ] A physical tablet/foldable at smallest-width >= 600 dp with Android 17/API 37 orientation behaviour.
- [ ] Microphone permission denial/retry and Bluetooth pairing on hardware.
- [ ] Casting and seek controls against a real DLNA renderer.

Code and emulator screenshots are implementation evidence, not proof of behaviour on every physical device or TV firmware.
