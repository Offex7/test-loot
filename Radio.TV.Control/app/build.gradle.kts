plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.android.compose.screenshot")
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
    experimentalProperties["android.experimental.enableScreenshotTest"] = true
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

    screenshotTestImplementation(platform("androidx.compose:compose-bom:2025.10.01"))
    screenshotTestImplementation("com.android.tools.screenshot:screenshot-validation-api:0.0.1-alpha16")
    screenshotTestImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation("androidx.datastore:datastore-preferences:1.1.7")
    implementation("io.insert-koin:koin-android:4.1.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
