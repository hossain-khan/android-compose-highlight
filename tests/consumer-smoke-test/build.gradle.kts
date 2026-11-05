plugins {
    id("com.android.application") version "__AGP_VERSION__"
    id("org.jetbrains.kotlin.plugin.compose") version "__KOTLIN_VERSION__"
}

android {
    namespace = "dev.hossain.highlight.consumer"
    compileSdk = 37

    defaultConfig {
        applicationId = "dev.hossain.highlight.consumer"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    // Intentionally declare ONLY the published library artifact to verify that all
    // ABI-exposed types (Compose UI, Foundation, Coroutines StateFlow) are provided transitively.
    implementation("dev.hossain:compose-highlight:__LIBRARY_VERSION__")
}
