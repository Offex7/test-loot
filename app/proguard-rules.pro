# StreamHub
# R8 is enabled for release. Retrofit/Gson and Compose Navigation are not used by app code,
# so no model keep rules are necessary for those libraries.
-keep,allowoptimization,allowobfuscation class androidx.media3.** { *; }
-keep,allowoptimization,allowobfuscation class androidx.datastore.** { *; }
-keep,allowoptimization,allowobfuscation class com.offex7.streamhub.BootReceiver { *; }
-keep,allowoptimization,allowobfuscation class com.offex7.streamhub.RadioPlaybackService { *; }
-keep,allowoptimization,allowobfuscation class com.offex7.streamhub.PipActionReceiver { *; }

# Keep the integrated remote protocols and DLNA entry points stable under release R8.
-keep,allowoptimization class com.radiotv.control.core.** { *; }
-keep,allowoptimization class com.radiotv.control.core.proto.** { *; }
-keep,allowoptimization class com.radiotv.control.cast.** { *; }
-keep,allowoptimization class com.tananaev.adblib.** { *; }
-keep,allowoptimization class fi.iki.elonen.** { *; }
-keep,allowoptimization class org.bouncycastle.** { *; }
