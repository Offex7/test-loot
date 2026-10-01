# UniMote v1.4.1 — corrected install failure diagnosis

The APK shown in the user's Package Installer screenshot is the previous build with SHA-256:
dc922efd78ed029321e688aeeefa94299870cc4ae35f5b7601fc12fa42edff2b

The screenshot itself contains only Android's generic "Не удалось установить приложение / Возникла проблема с файлом приложения", so there is no device error code to quote. Direct binary inspection identifies the cause.

Original resources.arsc:
- length 596600
- root chunk size 596600
- global string-pool size 139140
- package chunk starts at 139152 (4-byte aligned)

Previous broken resources.arsc:
- length 598301
- root chunk size 598301
- global string-pool size 140841 (NOT 4-byte aligned)
- package chunk starts at 140853 (NOT 4-byte aligned)
- the package payload after that point is byte-for-byte identical to the original package payload

The previous patch appended 1701 bytes to the global resource string pool but did not add the required 3-byte padding. This made the following ResTable_package chunk begin at an invalid unaligned offset. This is the direct malformed-resources condition behind the Package Installer rejection.

The previous build's V1/JAR, V2 and V3 signatures were cryptographically valid in local verification, so signature absence was not the cause of the supplied APK. Its V3 SDK range was also changed from the original; the repair uses minSDK 28 and maxSDK 2147483647.

Corrected APK:
- SHA-256: f700331f72842af7aa357c88b78cb65e081095df42f8a9eaf6b2cff6d39fe623
- resources.arsc length: 598304
- global string-pool size: 140844
- package start: 140856 (4-byte aligned)
- package payload: byte-for-byte identical to original
- resources.arsc is ZIP_STORED
- V1 PASS, V2 PASS, V3 PASS
- V3 minSDK=28, maxSDK=2147483647
- all stored ZIP data offsets are 4-byte aligned
- ZIP test PASS; 1108 entries; no duplicate names

Runtime caveat: no adb/device/emulator is available in the execution environment, so a physical adb install and logcat cannot be truthfully claimed.
