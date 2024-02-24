plugins {
    id("com.android.library")
    kotlin("android")
}

android {
    namespace = "ru.pixnews.sqlite.android"
    compileSdk = 34
    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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

repositories {
    mavenLocal {
        mavenContent {
            includeGroupAndSubgroups("com.dylibso.chicory")
        }
    }
    mavenCentral()
    google()
}


dependencies {
    project(":graalvm")
    api("androidx.core:core:1.12.0")
    api("androidx.sqlite:sqlite:2.4.0")
}

