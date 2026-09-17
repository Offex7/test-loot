plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.offex7.streamhub"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.offex7.streamhub"
        minSdk = 29
        targetSdk = 34
        versionCode = 4
        versionName = "4.0"
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug { isMinifyEnabled = false }
    }
    packaging { resources.excludes += setOf("META-INF/DEPENDENCIES", "META-INF/LICENSE", "META-INF/LICENSE.txt", "META-INF/NOTICE") }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    buildFeatures { compose = true; buildConfig = true }
    sourceSets["main"].res.srcDir(layout.buildDirectory.dir("generated/v4res").get().asFile)
}

kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.04.01")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.18.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.datastore:datastore-preferences:1.2.1")
    implementation("androidx.media3:media3-exoplayer:1.11.0")
    implementation("androidx.media3:media3-exoplayer-hls:1.11.0")
    implementation("androidx.media3:media3-ui:1.11.0")
    implementation("androidx.media3:media3-session:1.11.0")
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("io.coil-kt.coil3:coil-compose:3.5.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.5.0")
}

val v4Assets = mapOf(
    "start_tv" to "https://avatars.mds.yandex.net/i?id=124c90a0cfd3b3341d7e7592f8eda057e77081cd-4926719-images-thumbs&n=13",
    "start_radio" to "https://static.vecteezy.com/system/resources/previews/001/207/003/non_2x/music-icon-radio-png.png",
    "logo_record" to "https://sun9-58.vkuserphoto.ru/s/v1/ig2/IiddqILI9W20xtBjGASd1Wc2qaE8CtlNMcM4HP7_rOxeHWqZHsTZQrxChaHjZF90iod1cWtN-YKmKEhzRcRW4WNu.jpg?quality=96&cs=640x0",
    "logo_chocolate" to "https://avatars.mds.yandex.net/i?id=1e60272039bd4313b6db31715603bcbd_l-5207916-images-thumbs&n=13",
    "logo_energy" to "https://www.energyfm.ru/favicon.ico",
    "logo_ultra" to "https://is1-ssl.mzstatic.com/image/thumb/Purple221/v4/38/a2/31/38a23119-5f63-f992-f243-410ebdbe2e92/AppIcon-1x_U007epad-0-1-85-220-0.png/1200x630wa.jpg",
    "logo_kalyan" to "https://dfm.ru/b/d/a9C4t_hQkezt5PerPjcva6o4JuuCN9MOEI0j77hVMcD9iKP0DLhWWxZENyFfMZsgTgVNeCjIRm2cyeWlz8QjcNK6FJXYlqOLWg=z2mWS9XkQNQR-Eyvb39g-w.webp",
    "logo_pirate" to "https://avatars.mds.yandex.net/i?id=716a7e3af7b104f3440332533174c9675b6caf11-7549373-images-thumbs&n=13",
    "logo_vocal" to "https://avatars.mds.yandex.net/i?id=32e06202154c0631649a6d0b54f5f6d0_l-5287214-images-thumbs&n=13",
    "logo_chill" to "https://avatars.mds.yandex.net/i?id=cdb7307dc06845c2738e64bc70cb8042385cadb1-3986577-images-thumbs&n=13",
    "logo_psy" to "https://dfm.ru/b/d/a9C4t_hQkezt4_erPjcva6o4JuuCN9MOEI0j77hVMcD9iKP0DLhWWxZENyFfMZsgTgVNeCjIRm2cyeWlz8QjcNK6FJXYlqOLWg=cz1cYtES_SBOB1fZ0h832A.webp",
    "logo_metalcore" to "https://lh3.ggpht.com/3F-VojAzJppXcdFDGvjZ_55ONQyBo4mlpEqbIS9n5w-kG-W4NxT2MqdQU5qcwsXJ7g=s180",
    "logo_yug" to "https://yug-radio.ru/writable/uploads/grafskiy-photos/________________________-mobile.jpg",
    "logo_relax" to "https://avatars.mds.yandex.net/i?id=03476032ca5ccd22a23ac6b876142cd3_l-9291097-images-thumbs&n=13",
    "logo_fallback" to "https://avatars.mds.yandex.net/i?id=0a9808b1a359810ad95a4407bb71fa8b_l-5146492-images-thumbs&n=13",
    "logo_comedy" to "https://mediaplano.ru/wp-content/uploads/2019/11/comedy-radio-logo.png",
    "logo_autoradio" to "https://commons.wikimedia.org/wiki/Special:Redirect/file/Avtoradio4.png"
)

val downloadV4Assets by tasks.registering {
    val outDir = layout.buildDirectory.dir("generated/v4res/drawable")
    outputs.dir(outDir)
    doLast {
        val dir = outDir.get().asFile
        dir.mkdirs()
        fun run(vararg args: String): Boolean = runCatching { ProcessBuilder(*args).inheritIO().start().waitFor() == 0 }.getOrDefault(false)
        for ((name, url) in v4Assets) {
            val raw = File(dir, "${name}.source")
            val png = File(dir, "${name}.png")
            if (!png.exists()) {
                val downloaded = run("curl", "-L", "--fail", "--silent", "--show-error", "--retry", "2", "--max-time", "30", "-A", "TV-Radio-Online/4.0", "-o", raw.absolutePath, url)
                if (downloaded) run("convert", raw.absolutePath, "-resize", "512x512^", "-gravity", "center", "-extent", "512x512", png.absolutePath)
            }
            raw.delete()
            if (!png.exists()) run("convert", "-size", "512x512", "xc:#2A2A2A", "-fill", "#9E9E9E", "-gravity", "center", "-pointsize", "42", "-annotate", "0", "NO Image", png.absolutePath)
        }
        val tv = File(dir, "start_tv.png")
        if (tv.exists()) run("convert", tv.absolutePath, "-fuzz", "8%", "-transparent", "white", tv.absolutePath)
    }
}

tasks.named("preBuild") { dependsOn(downloadV4Assets) }
tasks.matching { it.name == "generateDebugResources" || it.name == "processDebugResources" }.configureEach { dependsOn(downloadV4Assets) }
