plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.radiotv.tvremote.cast"
    compileSdk = 37
    defaultConfig { minSdk = 26 }
}

dependencies {
    api(project(":tvremote-core"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
}
