plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.hermes.browser"
    compileSdk = 36

    defaultConfig {
        // CUTOVER: gecko is now the real browser (replaces the WebView build, Deck-integrated).
        applicationId = "com.hermes.browser"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        // GeckoView migration: arm64 for the phone (add x86_64 for the emulator; ABI splits at cutover).
        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
        }
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.material)

    // GeckoView (Firefox engine) — replaces system WebView. See GECKO_MIGRATION_PLAN.md.
    implementation("org.mozilla.geckoview:geckoview:151.0.20260608154138")

    // Pull-to-refresh: wraps the GeckoView; gated to the top of the page via the ScrollDelegate.
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // Phase 5 favicon-color theming: Palette extracts the favicon's dominant color, material-kolor
    // seeds the whole M3 scheme from it (search bar, popup menu, panels all tint harmoniously).
    implementation("androidx.palette:palette-ktx:1.0.0")
    implementation("com.materialkolor:material-kolor:1.4.4")
}
