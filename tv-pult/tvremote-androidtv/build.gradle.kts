plugins {
    id("com.android.library")
id("com.google.protobuf")
}
android {
    namespace = "com.radiotv.tvremote.androidtv"
    compileSdk = 35
    defaultConfig { minSdk = 29 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
kotlin { jvmToolchain(17) }
protobuf {
    protoc { artifact = "com.google.protobuf:protoc:4.36.0" }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins { maybeCreate("java").option("lite") }
        }
    }
}
dependencies {
    api(project(":tvremote-core"))
    api("com.google.protobuf:protobuf-javalite:4.36.0")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
    implementation("org.bouncycastle:bcprov-jdk18on:1.85")
    implementation("org.bouncycastle:bcpkix-jdk18on:1.85")
}
