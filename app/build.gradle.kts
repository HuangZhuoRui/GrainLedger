plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.vincent.grainledger"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.vincent.grainledger"
        minSdk = 33
        targetSdk = 37
        versionCode = 121
        versionName = "1.2.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val customKeystorePath = System.getenv("KEYSTORE_FILE_PATH")
                ?: (if (file("/Users/vincent/Desktop/SUSE-APP-Key/APP-Key.jks").exists()) "/Users/vincent/Desktop/SUSE-APP-Key/APP-Key.jks" else null)

            if (customKeystorePath != null && file(customKeystorePath).exists()) {
                storeFile = file(customKeystorePath)
                storePassword = System.getenv("KEYSTORE_PASSWORD") ?: "LinuxisUbuntu18"
                keyAlias = System.getenv("KEY_ALIAS") ?: "suse-app-key"
                keyPassword = System.getenv("KEY_PASSWORD") ?: "LinuxisUbuntu18"
            }
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("release")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.material.icons.extended)

    // MIUIX UI 组件库
    implementation(libs.miuix.ui)
    implementation(libs.miuix.preference)
    implementation(libs.miuix.icons)
    implementation(libs.miuix.squircle)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}