plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlinter)
    alias(libs.plugins.kotlinx.serialization)
}

android {
    namespace = "dev.hossain.highlight.sample"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.hossain.highlight.sample"
        minSdk = 24
        targetSdk = 36
        versionCode = 26
        versionName = "0.22.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        // Debug keystore used to sign release builds in CI. This is intentionally
        // hardcoded because this is a sample app, not a production release.
        // Never hardcode credentials for production keystores - use secrets instead.
        create("debugKeystore") {
            storeFile = rootProject.file("keystore/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debugKeystore")
        }
        create("benchmark") {
            initWith(getByName("release"))
            signingConfig = getByName("debug").signingConfig
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            matchingFallbacks += listOf("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    lint {
        // ComponentActivity extends Activity, but lint can't resolve it in release builds
        disable += "Instantiatable"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    // Available in maven central https://central.sonatype.com/artifact/dev.hossain/compose-highlight
    // implementation("dev.hossain:compose-highlight:<version>")
    implementation(project(":compose-highlight"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.leakcanary.android)

    // LLM chat streaming dependencies
    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.kotlinx.serialization.json)
}
