
plugins {
    id("org.jetbrains.kotlin.jvm")
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

testing {
    suites {
        // Configure the built-in test suite
        val test by getting(JvmTestSuite::class) {
            // Use JUnit Jupiter test framework
            useJUnitJupiter("5.10.0")
        }
    }
}

//java {
//    toolchain {
//        languageVersion = JavaLanguageVersion.of(21)
//    }
//}
