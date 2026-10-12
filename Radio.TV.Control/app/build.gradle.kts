plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val ciRunNumber = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull()?.coerceAtLeast(1) ?: 1
// Reuse the repository's existing stable project key across both variants and CI runs.
val sharedRadioTvKeystore = rootProject.projectDir.parentFile.resolve("app/signing/radio-tv-v5-debug.keystore")

android {
    namespace = "com.radiotv.control"
    compileSdk = 37
    defaultConfig {
        applicationId = "com.radiotv.control"
        minSdk = 31
        targetSdk = 37
        versionCode = ciRunNumber
        versionName = "0.1.$ciRunNumber"
    }
    signingConfigs {
        create("sharedRadioTv") {
            storeFile = sharedRadioTvKeystore
            storePassword = "android"
            keyAlias = "AndroidDebugKey"
            keyPassword = "android"
        }
    }
    buildTypes {
        debug {
            applicationIdSuffix = ""
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("sharedRadioTv")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("sharedRadioTv")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true }
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}
kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}
dependencies {
    implementation(project(":tvremote-core"))
    implementation(project(":tvremote-ui"))
    implementation(project(":tvremote-cast"))
    implementation(platform("androidx.compose:compose-bom:2025.10.01"))
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // JVM-only screenshot tests: native Robolectric graphics + Compose + Roborazzi (no emulator/ADB).
    testImplementation(platform("androidx.compose:compose-bom:2025.10.01"))
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.13")
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("androidx.test.espresso:espresso-core:3.6.1")
    testImplementation("io.github.takahirom.roborazzi:roborazzi:1.26.0")
    testImplementation("io.github.takahirom.roborazzi:roborazzi-compose:1.26.0")
    testImplementation("androidx.compose.ui:ui-test-manifest")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation("androidx.datastore:datastore-preferences:1.1.7")
    implementation("io.insert-koin:koin-android:4.1.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
