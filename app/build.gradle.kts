plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.hermes.browser"
    compileSdk = 36

    defaultConfig {
        // Distinct from the live WebView browser (com.hermes.browser) so the Gecko WIP installs
        // ALONGSIDE it as a separate app during the migration. Reverts to com.hermes.browser at cutover.
        applicationId = "com.hermes.browser.gecko"
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
}
