# Radio.TV TV Remote

Модульный Android-пульт для телевизоров и ТВ-приставок, подготовленный для последующего встраивания в Radio.TV.

## Состав

- app/ — самостоятельное demo-приложение, которое запускает экран пульта.
- tvremote-core/ — модели, абстракция RemoteDevice, discovery, Android TV Remote v2, Samsung Tizen, LG webOS, Roku ECP, Bluetooth HID/Air Mouse.
- tvremote-ui/ — UI пульта и TVRemoteFragment.
- tvremote-cast/ — независимый DLNA/UPnP SSDP + AVTransport слой.

Проект находится в каталоге `tvremote-project/`, чтобы не менять рабочий корень Radio.TV. Изменения сделаны в ветке `feature/tvremote-v1`; ветка `main` не трогается.

## Сборка

Требуется JDK 17 и Android SDK. Локально:

```text
cd tvremote-project
gradle :tvremote-core:test :app:assembleDebug
```

GitHub Actions запускает unit-тесты, debug APK и отдельную проверку APK при push в исходные каталоги ветки `feature/tvremote-v1`.

## Последняя подтверждённая сборка

Последняя подтверждённая зелёная сборка для актуального кода выполнена GitHub Actions run №12 на коммите `74556a23d1e92b26d2ba066ae8653d69f77d3215`:

- CI run: https://github.com/Offex7/test-loot/actions/runs/37846960808
- Artifact: https://github.com/Offex7/test-loot/actions/runs/37846960808/artifacts/11579204979
- Artifact name: `TVRemote-debug`
- Artifact ZIP size: 7,842,334 bytes
- Artifact ZIP SHA-256: `a307b95ad852e5f7dd7339e9e30ed6abad41031780ed8487a7fd45e698ab8cc1`
- APK size: 8,149,630 bytes
- APK SHA-256: `0f72033bab005be22c5bd57846ea22385d822465bfe920a939ec19ca2f82ce34`

Лог run №12 подтверждает:
- `:tvremote-core:test` — success;
- `:app:assembleDebug` — success;
- `BUILD SUCCESSFUL in 1m 35s`;
- APK artifact успешно загружен.

Run №12 не был фактически зависшим: на момент повторной проверки GitHub Actions job уже имел `completed/success`. Поэтому признаков OOM, R8, protobuf codegen или падения Gradle в финальном логе нет. В начале run был виден запрос по 7 из 8 SDK license agreements, после чего сборка продолжилась штатно.

## CI hardening после run №12

После анализа run №12 workflow был усилен в коммитах:

- `7c4b648b342703ed9dd188e0d768755bec011535` — разделение unit-test и assembleDebug, APK verification, уникальное имя artifact;
- `81d252825ca05faa5219b8d2e79a93242f0d9a82` — Gradle `--max-workers=2`, `timeout 10m`, строгие проверки manifest-флагов;
- добавлен `concurrency` с `cancel-in-progress: true`, чтобы несколько push в одну ветку не создавали конкурирующие сборки;
- trigger path ограничен исходниками/Gradle-файлами, поэтому изменение только README не запускает новый build.

Поскольку доступный GitHub connector не предоставляет список push-triggered workflow runs, отдельный post-fix run после коммита `81d2528...` здесь нельзя достоверно идентифицировать по run ID. Поэтому этот README не выдаёт непроверенный run как зелёный.

## Android TV / Google TV Remote v2

Реализовано:

- mDNS discovery: `_androidtvremote2._tcp.`
- pairing через TLS на 6467 с шестисимвольным HEX-кодом;
- live session на 6466;
- D-Pad, OK, Back, Home, Menu;
- Power, Mute, Volume, Channel, numeric keys, Input;
- media: Play/Pause/Stop/Rewind/Fast Forward;
- IME inject text;
- потоковый voice: PCM 16-bit mono 8 kHz;
- постоянный локальный RSA-2048 client identity в PKCS12.

В основу wire schema положены публичное описание Google TV Remote v2 и открытые референсные реализации. Генерируемый protobuf-код в проект не копируется из чужих репозиториев.

## Samsung Tizen

Локальный Samsung remote WebSocket профиль:

- TCP 8001;
- ручная конфигурация 8002 для secure endpoint;
- navigation, volume, channel, power, mute, input, media и numeric keys.

SmartThings cloud API в первой версии не используется: облачная авторизация добавила бы сильную зависимость от аккаунта/токенов и не улучшила базовый локальный пульт.

## LG webOS

Реализовано:

- WebSocket registration/pairing на 3000;
- хранение client-key;
- базовые клавиши;
- volume/channel/media/power;
- text через webOS IME;
- получение pointer socket и отправка move/click/button событий.

На отдельных поколениях webOS secure socket/permissions могут отличаться, поэтому LG pointer и secure registration должны быть проверены на нескольких реальных моделях.

## Roku ECP

Реализовано:

- discovery/доступ через ECP TCP 8060;
- navigation, Back/Home, Play, Rev/Fwd;
- volume/mute/channel/input/power, если модель предоставляет эти key endpoints;
- numeric keys;
- text через Lit_ UTF-8.

На Roku необходимо разрешить управление мобильными приложениями в настройках устройства. Доступность отдельных ТВ-команд зависит от модели.

## DLNA / UPnP

`tvremote-cast` выполняет:

- SSDP M-SEARCH для MediaRenderer;
- чтение device description;
- поиск AVTransport controlURL;
- SetAVTransportURI;
- Play / Stop;
- DIDL-Lite metadata;
- отдельный audioItem/videoItem.

Важно: DLNA renderer должен уметь напрямую получить переданный URL и поддерживать MIME/формат потока. Универсального transcoder/proxy в первой версии нет, поэтому HLS/DRM/нестандартные Radio.TV URL могут быть несовместимы с конкретным телевизором.

## Touch / Air Mouse

TouchPad:

- свайп;
- тап = OK для обычных пультов;
- тап = pointer click для устройств с pointer capability;
- долгий тап = Back либо right click.

Gyroscope Air Mouse:

- работает при наличии TYPE_GYROSCOPE;
- graceful degradation без гироскопа;
- для устройств с Wi-Fi pointer capability используется network pointer;
- повторное нажатие Air Mouse теперь корректно останавливает активный режим;
- добавлен отдельный BluetoothHidDevice controller для будущего/экспериментального HID-сценария.

Bluetooth HID требует отдельного pairing/разрешений Android и не обещается как универсальный способ для каждого TV.

## UI и ориентация

Пульт построен как вертикальный fixed-width viewport:

- предпочтительная ширина 420dp;
- при более широком окне контент центрируется;
- фон вокруг viewport чёрный, поэтому при широкой области появляются letterbox-поля;
- Activity принудительно portrait;
- WindowInsetsCompat учитывает system bars и display cutout;
- нижняя область имеет запас под gesture/button navigation;
- внутренний ScrollView позволяет прокручивать полный fixed-width viewport на компактных дисплеях.

Это намеренно сделано без landscape-layout и без растягивания пульта на всю ширину.

## Интеграция в Radio.TV

В `settings.gradle.kts`:

```kotlin
include(":tvremote-core", ":tvremote-ui", ":tvremote-cast")
```

В модуле Radio.TV:

```kotlin
implementation(project(":tvremote-core"))
implementation(project(":tvremote-ui"))
implementation(project(":tvremote-cast"))
```

Показ экрана:

```kotlin
val remoteFragment = TVRemoteFragment.newInstance().apply {
    setCastController(DlnaCastController())

    setMediaProvider(object : RadioTvMediaProvider {
        override fun currentMedia(): CastMedia? {
            // Вернуть текущий URL + MIME + title из проигрывателя Radio.TV.
            return null
        }
    })
}

childFragmentManager.beginTransaction()
    .replace(R.id.remote_container, remoteFragment)
    .commit()
```

UI не знает о DI-фреймворке Radio.TV. Поэтому проект можно подключить к существующему Hilt/Koin-контейнеру без изменения UI API. В production лучше отдавать `RemoteDeviceFactory`, `CastController` и media provider через DI, а экземпляры TV remote lifecycle-ить вместе с вкладкой.

## Ограничения текущей версии

Реальное сопряжение/команды должны быть подтверждены на физических моделях. В этой среде нет подключённых Android TV, Samsung, LG или Roku, поэтому hardware-level verification здесь не подменяется статическими заявлениями.

Не гарантируется:

- Samsung text input на всех поколениях;
- LG secure pointer socket на всех поколениях;
- Roku voice audio streaming через локальный ECP;
- DLNA воспроизведение HLS/DRM на каждом renderer;
- Bluetooth HID как универсальный mouse transport для любого TV;
- Android TV Remote v2 pairing/voice на каждом OEM firmware без проверки на реальном устройстве.

## Проверка актуального APK

Для run №12 APK дополнительно проверен как ZIP-архив и через разбор бинарного AndroidManifest/DEX в среде выполнения:

- в скомпилированном Manifest `usesCleartextTraffic=true`;
- `MainActivity` имеет `screenOrientation=portrait`;
- DEX содержит `OVER_SCROLL_IF_CONTENT_SCROLLS`, `Air Mouse включена` и `Air Mouse выключена`, что подтверждает наличие двух последних UI-изменений в собранном APK.

Локального `apksigner`/Android Build Tools в текущей среде нет, поэтому криптографическую команду `apksigner verify --verbose` для run №12 локально здесь не выдаю как выполненную. Новый CI workflow теперь выполняет эту проверку на GitHub runner.

## Открытые ссылки

Android TV Remote v2:
- https://github.com/tronikos/androidtvremote2
- https://github.com/theharisshah/androidremote
- https://github.com/dakopopelman/AndroidTVRemoteControl

Samsung:
- https://github.com/ReyNeill/samsung-tv-remote

LG:
- https://github.com/merdok/lgtv2
- https://github.com/hobbyquaker/lgtv2
- https://github.com/ConnectSDK/Connect-SDK-Android

DLNA:
- https://github.com/yinnho/UPnPCast

Bluetooth reference:
- https://github.com/ahmedamoharram/bluetooth-remote

Roku ECP:
- https://developer.roku.com/docs/developer-program/dev-tools/external-control-api.md
