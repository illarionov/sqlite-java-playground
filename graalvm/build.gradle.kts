@file:Suppress("UnstableApiUsage")

import java.nio.file.Paths
import kotlin.io.path.exists

plugins {
    id("buildlogic.kotlin-application-conventions")
}

val isGraalVm = providers.gradleProperty("GRAALVM")
    .map(String::toBoolean)
    .orElse(providers
        .systemProperty("java.home")
        .map { javaHome ->
            Paths.get("$javaHome/lib/graalvm").exists()
        }
    )
    .orElse(false)

configurations {
    dependencyScope("graalvmCompiler") {
        defaultDependencies {
            add(libs.graalvm.compiler.get())
        }
    }
    resolvable("graalvmCompilerClasspath") {
        extendsFrom(configurations["graalvmCompiler"])
    }
}

class GraalvmCompilerJvmArgumentsProvider(
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val gralvmClasspath: FileCollection,
    val isGralvm: Provider<Boolean>
) : CommandLineArgumentProvider {
    override fun asArguments(): Iterable<String> {
        return if (!isGralvm.get()) {
            listOf(
                "-XX:+UnlockExperimentalVMOptions",
                "-XX:+EnableJVMCI",
                "--upgrade-module-path=${gralvmClasspath.asPath}"
            )
        } else {
            emptyList()
        }
    }
}

val graalVmJvmArgsProvider = GraalvmCompilerJvmArgumentsProvider(
    configurations["graalvmCompilerClasspath"],
    isGraalVm,
)

application {
    mainClass = "org.example.app.AppKt"
}

tasks.withType<JavaExec>().configureEach {
    jvmArgumentProviders += graalVmJvmArgsProvider
}

dependencies {
    implementation(libs.graalvm.polyglot.polyglot)
    implementation(libs.graalvm.polyglot.wasm)
    compileOnly(libs.graalvm.wasm.language)
    implementation(project(":sqlite3-wasm"))
    implementation(project(":host"))
}
dependencies {
    testImplementation("junit:junit:4.13.2")
}

testing {
    suites {
        getByName<JvmTestSuite>("test") {
            targets.all {
                testTask.configure {
                    jvmArgumentProviders += graalVmJvmArgsProvider
                }
            }
        }
    }
}
