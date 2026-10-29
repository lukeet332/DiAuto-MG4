plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    compileSdk = 36
    namespace = "com.diauto.hubsim"

    defaultConfig {
        applicationId = "com.diauto.hubsim"
        minSdk = 28
        targetSdk = 28
        versionCode = 1
        versionName = "hubsim-1"
    }

    // Platform-signed and system uid like the car's AllGo hub (com.allgo.rui), which binds the bridge.
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
