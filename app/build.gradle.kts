import java.net.URL
import java.io.FileOutputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.nothing4apro.hamsterwheel"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.nothing4apro.hamsterwheel"
        minSdk = 33
        targetSdk = 34
        versionCode = 4
        versionName = "1.3.0"
    }

    signingConfigs {
        getByName("debug") {
            storeFile = file("fixed-debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

val downloadGlyphSdk by tasks.registering {
    val sdkDir = file("libs")
    val sdkFile = file("libs/glyph-matrix-sdk-2.0.aar")
    outputs.file(sdkFile)
    doLast {
        if (!sdkFile.exists()) {
            sdkDir.mkdirs()
            val url = URL("https://raw.githubusercontent.com/Nothing-Developer-Programme/GlyphMatrix-Developer-Kit/main/glyph-matrix-sdk-2.0.aar")
            println("Downloading Nothing Glyph Matrix SDK 2.0 from official repo...")
            url.openStream().use { input ->
                FileOutputStream(sdkFile).use { output ->
                    input.copyTo(output)
                }
            }
            println("Saved to ${sdkFile.absolutePath}")
        }
    }
}

tasks.named("preBuild") {
    dependsOn(downloadGlyphSdk)
}

dependencies {
    implementation(files("libs/glyph-matrix-sdk-2.0.aar"))
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
}
