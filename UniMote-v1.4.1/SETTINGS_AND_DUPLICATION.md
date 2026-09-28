# Settings / Duplication

The bottom navigation entry `Настройки` remains in the original position and uses the original resource/menu identifiers.

Settings content was emptied by changing `SettingsAdapter.getItemCount()` from 7 to 0. The fragment and navigation entry remain intact.

The reconnect card was moved conceptually to the Duplication screen by adding a fifth card to `fragment_mirrroring.xml` under the existing four mirroring cards. The new card reuses the existing reconnect icon/text/card styling.

All four original duplication click handlers remain present and untouched.

In this APK the original Settings position-5 callback ends in `SettingsFragment.shareAction()`, and that method is an empty `return-void`. The new reconnect card therefore returns immediately for its own ID while preserving the original audio-card handler for its original ID. This avoids rewriting the four working duplication actions.
