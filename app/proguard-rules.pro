# StreamHub
# R8 is enabled for release. Retrofit/Gson and Compose Navigation are not used by app code,
# so no model keep rules are necessary for those libraries.
-keep,allowoptimization,allowobfuscation class androidx.media3.** { *; }
-keep,allowoptimization,allowobfuscation class androidx.datastore.** { *; }
-keep,allowoptimization,allowobfuscation class com.offex7.streamhub.BootReceiver { *; }
-keep,allowoptimization,allowobfuscation class com.offex7.streamhub.RadioPlaybackService { *; }
-keep,allowoptimization,allowobfuscation class com.offex7.streamhub.PipActionReceiver { *; }
