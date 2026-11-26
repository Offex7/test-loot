plugins {
    id("com.android.library")
}

android {
    namespace = "com.offex7.streamhub.auto"
    compileSdk = 37
    compileSdkMinor = 1

    defaultConfig {
        minSdk = 31
    }

    buildToolsVersion = "37.0.0"

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation("androidx.car.app:app:1.8.0-rc01")
    implementation("androidx.media3:media3-session:1.11.1")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
}
