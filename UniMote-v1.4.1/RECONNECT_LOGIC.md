# Reconnect logic parity

The original Settings item mapped to the visible "Переподключиться" action at click position 1.

Its original flow is preserved in the added Duplication click branch:
1. If Fire TV, call FireTVControl.closeADB().
2. If Samsung, call SamsungControl.disconnect().
3. If Android TV, call AndroidTVManager.disconnect().
4. If Roku, call RokuControl.deinitRetrofit().
5. If a device is connected through StreamingManager, call StreamingManager.disconnect().
6. Start the same PremiumActivity intent as the original Settings branch.

The existing CastFragment$4.onClick behavior for all other button IDs is not replaced; the new row ID is handled first and all non-matching IDs fall through to the original body.

The row uses the original settings_i_connect drawable and the existing reconnect string resource.
