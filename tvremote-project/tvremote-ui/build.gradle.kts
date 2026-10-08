plugins {
    id("com.android.library")
}

android {
    namespace = "com.radiotv.tvremote.ui"
    compileSdk = 36
    defaultConfig { minSdk = 26 }
}

dependencies {
    api(project(":tvremote-core"))
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.fragment:fragment-ktx:1.8.9")
    implementation("androidx.appcompat:appcompat:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
}
