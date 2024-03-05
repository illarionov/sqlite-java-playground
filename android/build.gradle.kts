import org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
import org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED
import org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED
import org.gradle.api.tasks.testing.logging.TestLogEvent.STANDARD_ERROR
import org.gradle.api.tasks.testing.logging.TestLogEvent.STANDARD_OUT

plugins {
    id("com.android.library")
    alias(libs.plugins.room)
    alias(libs.plugins.ksp)
    kotlin("android")
}

android {
    namespace = "ru.pixnews.sqlite.android"
    compileSdk = 34
    defaultConfig {
        minSdk = 26
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
    testOptions {
        unitTests {
            isReturnDefaultValues = false
            isIncludeAndroidResources = true
            all {
                it.useJUnitPlatform()
                it.maxHeapSize = "1G"
                it.testLogging {
                    events = setOf(FAILED, PASSED, SKIPPED, STANDARD_ERROR, STANDARD_OUT)
                }
            }
        }
    }
}

room {
    schemaDirectory("$projectDir/schemas/")
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
    implementation(project(":graalvm"))
    implementation("co.touchlab:kermit:2.0.3")
    implementation(libs.androidx.room.runtime)
    annotationProcessor(libs.androidx.room.compiler)
    ksp(libs.androidx.room.compiler)
    api("androidx.core:core:1.12.0")
    api("androidx.sqlite:sqlite:2.4.0")
    testImplementation(libs.androidx.room.testing)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.params)
    testImplementation(libs.androidx.test.core)
    testRuntimeOnly(libs.junit.jupiter.engine)
}

