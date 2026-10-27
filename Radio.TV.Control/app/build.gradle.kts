plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val enableGoogleScreenshotTests = providers.gradleProperty("enableGoogleScreenshotTests").orNull == "true"
if (enableGoogleScreenshotTests) {
    // Apply only for the official snapshot task so the APK and emulator fallback build stay independent.
    apply(plugin = "com.android.compose.screenshot")
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
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    if (enableGoogleScreenshotTests) {
        experimentalProperties["android.experimental.enableScreenshotTest"] = true
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
    implementation(project(":tvremote-companion"))
    implementation(platform("androidx.compose:compose-bom:2025.10.01"))
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    if (enableGoogleScreenshotTests) {
        add("screenshotTestImplementation", platform("androidx.compose:compose-bom:2025.10.01"))
        add("screenshotTestImplementation", "com.android.tools.screenshot:screenshot-validation-api:0.0.1-alpha16")
        add("screenshotTestImplementation", "androidx.compose.ui:ui-tooling")
    }
    add("androidTestImplementation", platform("androidx.compose:compose-bom:2025.10.01"))
    add("androidTestImplementation", "androidx.compose.ui:ui-test-junit4")
    add("androidTestImplementation", "androidx.test.ext:junit:1.2.1")
    add("androidTestImplementation", "androidx.test:runner:1.6.2")
    add("debugImplementation", "androidx.compose.ui:ui-test-manifest")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation("androidx.datastore:datastore-preferences:1.1.7")
    implementation("io.insert-koin:koin-android:4.1.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
