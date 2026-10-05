plugins {
    id("com.android.library")
}
android {
    namespace = "com.radiotv.tvremote.cast"
    compileSdk = 37
    compileSdkMinor = 1
    defaultConfig { minSdk = 29 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
kotlin { jvmToolchain(17) }
