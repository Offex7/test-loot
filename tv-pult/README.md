# TV пульт

Модульный Android-пульт, визуально ориентированный на UniMote, но реализованный как чистый проект.

## Первый milestone

- портретная ориентация;
- safe-area для status/navigation bars;
- центрированный интерфейс с ограниченной шириной;
- Android TV / Google TV Remote v2 discovery;
- pairing по 6-символьному коду;
- D-pad, Back/Home/Menu, Power/Mute/Input;
- громкость и каналы;
- Play/Pause/Stop/Rewind/Fast Forward;
- IME text injection;
- отдельные boundaries для Samsung, LG webOS, Roku, Bluetooth HID/Air Mouse и DLNA/UPnP.

Основной Android TV Remote v2 protocol layer основан на открытом проекте:
https://github.com/theharisshah/androidremote
и использует опубликованные протоколы Google / reference implementations.

## Архитектура

`tvremote-core` содержит UI-независимый контракт `RemoteDevice`.
Каждый производитель реализуется отдельным модулем.
`tvremote-ui` содержит общий D-pad/UI.
`tvremote-cast` не имеет сетевой backend-зависимости, пока конкретная DLNA-реализация не протестирована.

## Почему targetSdk 35

Android 17/API 37 вводит обязательное разрешение `ACCESS_LOCAL_NETWORK` для приложений, таргетящих API 37+. Первый milestone таргетит 35, чтобы не добавлять неподготовленную runtime-механику доступа к локальной сети.

## Сборка

```bash
gradle -p tv-pult wrapper --gradle-version 9.7.1
./tv-pult/gradlew -p tv-pult :tvremote-core:test :app:assembleDebug
```

CI запускается из корневого `.github/workflows/build-tv-pult.yml`.

## Статус

Android TV Remote v2 — основной протокол первого milestone.
Samsung/LG/Roku/HID/DLNA подключаются после отдельной реализации и реальных совместимых тестов.
