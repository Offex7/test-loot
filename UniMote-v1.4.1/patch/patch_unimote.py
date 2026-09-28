# Binary patch recipe used for the supplied UniMote_v1.4.1.apk.
#
# The original execution environment did not provide apktool/apksigner, so the
# finished APK was produced with targeted DEX/resource/XML patching.
# See the root project artifacts for the exact original/final SHA-256 values.

PATCH_SUMMARY = {
    "app_name": "TV пульт",
    "settings_getItemCount": {"before": 7, "after": 0},
    "new_duplication_reconnect_card": True,
    "manifest_changed": False,
    "technical_unimote_identifiers_preserved": True,
}

# A byte-for-byte source copy of the runtime patch script is supplied separately
# in the local project artifact because this public GitHub connection supports
# text commits but not binary/repository asset upload for the supplied APK.
