plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.qodex.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.qodex.app"
        // minSdk 24 / targetSdk 28 are deliberate (Zcode-Android device-proven):
        // Android 10+ W^X policy blocks exec() of binaries in app-writable
        // storage for apps targeting >= 29; targeting 28 keeps the embedded
        // Bionic node executable on Android 10+ (Termux/proot-distro precedent).
        minSdk = 24
        targetSdk = 28
        versionCode = 1
        versionName = "0.1.0-qodex"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    androidResources {
        // AAPT2 otherwise decompresses/re-compresses these at build time and
        // AssetManager.open() then yields re-compressed bytes; runtime
        // extraction streams raw tar/deb bytes and must not pay that cost.
        noCompress += listOf("tgz", "gz", "deb", "xz")
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.02")
    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")

    // RPC to the on-device engine (ws://127.0.0.1:3030) — OkHttp WebSocket
    // speaks the same binary framing as packages/client websocket.ts.
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Coroutines + JSON payloads (RpcValue.Obj uses JsonElement trees).
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")

    // Runtime provisioning: .deb extraction (ar + tar.xz|zst) for the Termux
    // Bionic closure and .tgz for the engine dist — both at runtime.
    implementation("org.apache.commons:commons-compress:1.27.1")
    // commons-compress's XZCompressorInputStream resolves org.tukaani.xz classes at runtime; without this jar data.tar.xz throws NoClassDefFoundError.
    implementation("org.tukaani:xz:1.9")
    // commons-compress's zstd support requires the zstd-jni native AAR.
    implementation("com.github.luben:zstd-jni:1.5.5-11@aar")

    // JVM unit tests: golden vectors from the real TypeScript serializer +
    // a full in-process ChannelClient <-> fake-ChannelServer round trip.
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("org.json:json:20240303")
}
