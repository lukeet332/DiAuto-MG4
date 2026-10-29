plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    compileSdk = 36
    namespace = "com.allgo.app.androidauto"

    defaultConfig {
        // Installs as an update over the MG4's AllGo Android Auto app (versionCode 1), so the stock
        // AllGo hub reports DiAuto sessions to the car. Uninstalling the update restores AllGo.
        applicationId = "com.allgo.app.androidauto"
        minSdk = 28
        targetSdk = 28
        versionCode = 4
        versionName = "diauto-bridge-0.3.0"
    }

    // The AOSP test platform key (public) that the MG4's AllGo apps are signed with; an update
    // over com.allgo.app.androidauto must match it.
    signingConfigs {
        create("platform") {
            storeFile = rootProject.file("tools/platform.p12")
            storeType = "PKCS12"
            storePassword = "android"
            keyAlias = "platform"
            keyPassword = "android"
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("platform")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    // Car-only system app on Android 9, never published to Play.
    lint {
        disable += "ExpiredTargetSdkVersion"
    }

    testOptions {
        unitTests.isIncludeAndroidResources = false
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.17")
}
