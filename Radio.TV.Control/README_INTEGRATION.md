# Radio.TV.Control — интеграция в Radio.TV

Документ описывает подключение существующих Android-модулей пульта как вкладки в основное приложение Radio.TV. Исходники проверены в ветке `feature/radiotv-control-v1`.

## 1. Модули и границы ответственности

| Gradle-модуль | Что содержит | Зависимость от других модулей проекта |
|---|---|---|
| `:tvremote-ui` | Compose-пульт и точка входа `TvRemotePadScreen(...)` | `:tvremote-core` через `api(project(...))` |
| `:tvremote-core` | Модель клавиш/состояний, Android TV Remote v2, ADB-over-TCP, Bluetooth HID и транспортные абстракции | Нет зависимостей на соседние Gradle-модули |
| `:tvremote-companion` | WebSocket-клиент телефона, WebSocket-сервер для ТВ, mDNS/NSD для `_radiotvcontrol._tcp.` | Не зависит от `:app`, `:tvremote-ui` или `:tvremote-core` |
| `:tvremote-cast` | DLNA/SSDP discovery, локальная сеть для cast и HTTP для передачи медиа | `:tvremote-core` |
| `:app` | Текущая демо-оболочка: навигация, настройки, DI, permission flow и связывание транспорта с UI | Использует UI, core, cast и companion |

Все перечисленные Gradle-модули — Android library-модули, кроме `:app`. Граф зависимостей направленный: `:tvremote-ui → tvremote-core`, `:tvremote-cast → tvremote-core`, а приложение соединяет модули. Циклических зависимостей между этими модулями в Gradle-файлах нет.

**Важное уточнение по TV-серверу:** reusable-классы `RadioTvCompanionClient` и `RadioTvCompanionServer` находятся в `:tvremote-companion`. Текущий foreground-service `RadioTvCompanionService`, который связывает сервер с ADB loopback, уведомлением и действиями оболочки, пока находится в `:app`. При переносе в Radio.TV перенесите этот service/glue в приложение-хост и настройте его уведомление и обработку события ADB под собственную навигацию; не копируйте его как часть `:tvremote-ui`.

## 2. Подключение модулей к Radio.TV

Скопируйте каталоги `tvremote-core/`, `tvremote-ui/`, `tvremote-companion/` и при необходимости `tvremote-cast/` из каталога `Radio.TV.Control/` в корень основного проекта Radio.TV. Не копируйте демонстрационный `:app` поверх основного приложения.

### `settings.gradle.kts`

Добавьте модули к существующим `include(...)` — не заменяйте настройки основного проекта целиком:

```kotlin
include(
    ":tvremote-core",
    ":tvremote-ui",
    ":tvremote-companion",
    ":tvremote-cast"
)
```

Убедитесь, что в `pluginManagement` есть Google Maven, Maven Central и Gradle Plugin Portal, а в `dependencyResolutionManagement.repositories` — Google Maven и Maven Central.

### Корневой `build.gradle.kts`

Добавьте плагины, если их ещё нет. Версии должны совпадать с совместимыми версиями, уже принятыми в основном Radio.TV:

```kotlin
plugins {
    id("com.android.library") version "9.3.1" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.21" apply false
    id("com.google.protobuf") version "0.10.0" apply false
}
```

Если основной проект уже объявляет эти плагины, не объявляйте их повторно — используйте текущие версии. `:tvremote-core` применяет Protobuf Gradle Plugin для генерации lite-классов протокола.

### `app/build.gradle.kts` основного Radio.TV

Минимальный набор для вкладки UI с моделью клавиш:

```kotlin
dependencies {
    implementation(project(":tvremote-ui"))
    implementation(project(":tvremote-core"))
}
```

Если приложение использует соответствующие функции, добавьте также:

```kotlin
dependencies {
    implementation(project(":tvremote-companion"))
    implementation(project(":tvremote-cast"))
}
```

`tvremote-ui` публично экспортирует `:tvremote-core` через `api(project(":tvremote-core"))`, однако явная зависимость приложения на core полезна, если app напрямую создаёт контроллеры или обращается к моделям протоколов.

Текущие настройки модулей: `minSdk = 31`, `compileSdk = 37`, Kotlin/JVM target 17. Сохраните совместимость с SDK и Kotlin, которые используются основным Radio.TV.

## 3. Вызов пульта внутри вкладки Compose

Публичная точка входа UI — `com.radiotv.control.ui.TvRemotePadScreen`. Она принимает состояние и callback-и, но не создаёт `MainActivity`, не создаёт сетевые контроллеры и не хранит соединение самостоятельно.

```kotlin
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.radiotv.control.core.RemoteKey
import com.radiotv.control.ui.TvRemotePadScreen
import com.radiotv.control.ui.RemoteFeatureAction

@Composable
fun RadioTvRemoteTab(
    connected: Boolean,
    tvName: String,
    onKey: (RemoteKey) -> Unit,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onSearch: () -> Unit,
    onFeature: (RemoteFeatureAction) -> Unit
) {
    TvRemotePadScreen(
        modifier = Modifier,
        enabled = connected,
        deviceName = tvName,
        onKey = onKey,
        onBack = onBack,
        onSettings = onOpenSettings,
        onStartSearch = onSearch,
        onFeatureAction = onFeature
    )
}
```

Вставьте `RadioTvRemoteTab(...)` в content выбранной вкладки Radio.TV. Хост-приложение отвечает за свой `NavController`/роутер и System Back через `BackHandler`. Callback `onBack` относится к кнопке Back на самом пульте; он не вызывает навигацию приложения автоматически.

Важные публичные параметры `TvRemotePadScreen`:

- `enabled` — есть ли активное соединение, влияющее на состояние UI.
- `deviceName` — текст статуса/имя ТВ в верхней панели.
- `onKey(RemoteKey)` — передача физической команды выбранному транспорту.
- `onFeatureAction(RemoteFeatureAction)` — действия микрофона, клавиатуры, тачпада и Cast; хост открывает соответствующий диалог/сервис.
- `touchpadActive`, `airMouseActive`, `muteActive`, `voiceActive`, `keyboardActive` — состояния активных функций, если хост ими управляет.
- `featureMessage` и `showFeatureActions` — необязательное сообщение и дополнительные элементы UI.

Не помещайте сетевой ввод-вывод в composable: callback-и должны передавать намерение в контроллеры уровня приложения/ ViewModel.

## 4. Companion (mDNS + WebSocket)

### Телефонная сторона — клиент

Клиент сам выполняет NSD/mDNS-поиск сервиса `_radiotvcontrol._tcp.`, открывает WebSocket и публикует `StateFlow<CompanionConnectionStatus>`. Создавайте его с application context, запускайте вместе с владельцем вкладки/ViewModel и обязательно закрывайте:

```kotlin
val companionClient = RadioTvCompanionClient(applicationContext)
companionClient.start()

// При уничтожении владельца:
companionClient.close()
```

В Compose можно наблюдать `companionClient.status` через `collectAsStateWithLifecycle()` (добавьте Lifecycle Compose dependency в основном проект, если её ещё нет). Команды клиента — suspend-функции:

```kotlin
companionClient.sendKeyCode(androidKeyCode)
companionClient.sendText(text)
companionClient.launchApp(packageName)
```

Оборачивайте вызовы в coroutine scope приложения и обновляйте `enabled/deviceName` по состоянию `CompanionConnectionStatus.Connected`. В `Searching`/ `Unavailable` выбирайте fallback-подключение, если оно доступно.

`RemoteKey` — общая модель клавиш, а `sendKeyCode` принимает Android keycode. Для одного и того же сопоставления в Companion и ADB используйте публичное расширение из `:tvremote-core`:

```kotlin
import com.radiotv.control.core.toAndroidKeyCode

scope.launch {
    companionClient.sendKeyCode(RemoteKey.OK.toAndroidKeyCode())
}
```

Это отображение используется и внутри `AdbRemoteDevice.sendKey(RemoteKey)`, поэтому host-приложению не нужно поддерживать вторую таблицу keycode.

### TV-сторона — сервер и ADB loopback

`RadioTvCompanionServer(context) { json -> ... }` публикует `_radiotvcontrol._tcp.` на TCP-порту **8089**. Он принимает структурированные JSON-команды `key`, `text`, `launch`; произвольная shell-команда через WebSocket не предоставляется.

Для поведения, совпадающего с демо-приложением, на ТВ нужен foreground service, который:
1. запускает `RadioTvCompanionServer`;
2. создаёт `AdbRemoteDevice(applicationContext)` и пробует локальный ADB `127.0.0.1:5555`;
3. валидирует JSON и преобразует только поддержанные типизированные команды в key/text/launch;
4. отображает пользователю инструкцию включить Developer options/ADB, если loopback ADB выключен;
5. закрывает сервер, ADB и scope в `onDestroy()`.

В текущем репозитории эту связку реализует `RadioTvCompanionService` в `:app`, потому что уведомление и prompt связаны с оболочкой приложения. Переносите этот service-glue в Radio.TV отдельным шагом; одного подключения `:tvremote-companion` достаточно для reusable client/server-классов, но не для автоматического переноса UI-специфичных broadcast/notification действий.

## 5. ADB fallback и поиск устройств

Создавайте контроллер в хосте, используя application context:

```kotlin
val adbRemote = AdbRemoteDevice(applicationContext)

// После того как пользователь выбрал ТВ и включил ADB:
adbRemote.connect(hostIp, 5555)
adbRemote.sendKey(RemoteKey.OK)
adbRemote.sendText("Текст для телевизора")

// При освобождении контроллера:
adbRemote.close()
```

Первое соединение может потребовать подтверждения RSA-ключа на телевизоре. Наличие порта 5555 само по себе не означает, что ADB включён/авторизован. Устройства и другие протоколы можно обнаруживать через `LocalNetworkDeviceDiscovery(context).discover()` из `:tvremote-cast`; этот вызов — suspend-функция, её следует выполнять из coroutine scope.

Не открывайте порт ADB в интернет и не добавляйте произвольную shell-команду к WebSocket API. Для Kivi используйте ADB только на локальном адресе выбранного ТВ, с подтверждением ключа.

## 6. Permissions манифеста

Добавляйте только разрешения функций, которые реально включены в основной Radio.TV. Для полного набора возможностей текущего демо-список следующий:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.CHANGE_WIFI_MULTICAST_STATE" />
<uses-permission android:name="android.permission.ACCESS_LOCAL_NETWORK" />
<uses-permission android:name="android.permission.VIBRATE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.BLUETOOTH_ADVERTISE" />
<uses-permission
    android:name="android.permission.BLUETOOTH_SCAN"
    android:usesPermissionFlags="neverForLocation" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
```

Примечания:

- Для Bluetooth на Android 12+ запросите runtime-разрешения `BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN` и/или `BLUETOOTH_ADVERTISE` в соответствии с выбранным режимом HID. Одного объявления в manifest недостаточно.
- Для локальной сети на версиях Android, где `ACCESS_LOCAL_NETWORK` предоставляется как runtime permission, обработайте запрос и отказ. Доступность/поведение разрешения зависит от версии Android.
- `RECORD_AUDIO` нужно только при включении голосового ввода; это runtime-разрешение.
- Для сервера на ТВ добавьте service declaration и корректный foreground service lifecycle. В текущем демонстрационном manifest service называется `com.radiotv.control.RadioTvCompanionService`; при переносе пакета замените имя на собственный класс.
- `android:usesCleartextTraffic="true"` присутствует в демо-приложении для локальных протоколов. Не включайте его глобально без оценки транспортов и политики безопасности основного Radio.TV.

## 7. Что проверить перед слиянием

1. Подключить модули в отдельной ветке Radio.TV и собрать app для minSdk 31+.
2. Открыть вкладку, проверить все key callback-и, верхнюю панель, Back и переход в настройки.
3. На двух устройствах в одной Wi-Fi сети запустить TV Companion server/service и убедиться, что клиент показывает `Connected`, затем проверить key/text/launch.
4. Отключить Companion и проверить fallback Android TV Remote v2 / ADB только для устройств с включённым и авторизованным соответствующим протоколом.
5. Проверить сценарии отказа permissions, потерю сети, закрытие ViewModel/service и возврат из диалогов.
6. Запустить проектные unit-тесты и Compose screenshot tests основного Radio.TV. Скриншоты этой репозитории проверяют визуальное состояние; они не заменяют ручное тестирование на реальном ТВ.

## 8. Совместимость и статус

Документ отражает фактическое разбиение модулей этой ветки. UI-слой изолирован от `:app`/Activity. Перед продакшен-релизом интегрирующий проект должен прогнать сборку и аппаратные тесты под собственными applicationId, permissions, app lifecycle и policy.

- Публичная UI-точка входа: `com.radiotv.control.ui.TvRemotePadScreen`
- Минимальная версия Android модулей: API 31.
- Рекомендуемый порядок: подключить UI/core; затем добавить Companion и ADB/cast по требуемым функциям.
