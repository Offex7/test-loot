plugins {
    id("com.android.library")
}

android {
    namespace = "com.radiotv.tvremote.core"
    compileSdk = 36
    defaultConfig { minSdk = 26; consumerProguardFiles("consumer-rules.pro") }
}

dependencies {
    api("androidx.annotation:annotation:1.9.1")
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.bouncycastle:bcprov-jdk18on:1.80")
    implementation("org.bouncycastle:bcpkix-jdk18on:1.80")
    testImplementation("junit:junit:4.13.2")
}
