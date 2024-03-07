package ru.pixnews.gradle.wasm.emscripten

import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations

abstract class WasmStripTask @Inject constructor(
    private val execOperations: ExecOperations,
) : DefaultTask() {

    @get:InputFile
    abstract val source: RegularFileProperty

    @get:OutputFile
    abstract val destination: RegularFileProperty

    @get:Input
    @get:Optional
    abstract val wasmStripLocation: Property<String>

    @TaskAction
    fun strip() {
        val wasmStrip = if (wasmStripLocation.isPresent) {
            wasmStripLocation.get()
        } else {
            "wasm-strip"
        }

        try {
            execOperations.exec {
                commandLine = listOf(
                    wasmStrip,
                    "-o",
                    destination.get().toString(),
                    source.get().toString()
                )
            }.rethrowFailure()
        } catch (ioException: Exception) {
            throw GradleException(
                "Failed to execute `wasm-strip`. Make sure WABT (The WebAssembly Binary Toolkit) is installed.",
                ioException
            )
        }
    }
}