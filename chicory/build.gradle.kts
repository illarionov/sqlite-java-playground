@file:Suppress("UnstableApiUsage")

plugins {
    id("buildlogic.kotlin-application-conventions")
}

application {
    mainClass = "ru.pixnews.wasm.sqlite3.chicory.AppKt"
}

dependencies {
    implementation(libs.chicory)
    implementation(project(":sqlite3-wasm"))
}
dependencies {
    testImplementation("junit:junit:4.13.2")
}

testing {
    suites {
        getByName<JvmTestSuite>("test") {
            targets.all {
                testTask.configure {
                }
            }
        }
    }
}
