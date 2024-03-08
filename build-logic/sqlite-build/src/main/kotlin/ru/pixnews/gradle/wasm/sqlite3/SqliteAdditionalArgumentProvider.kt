package ru.pixnews.gradle.wasm.sqlite3

import java.io.File
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.process.CommandLineArgumentProvider

class SqliteAdditionalArgumentProvider(
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    val sqliteCFile: Provider<File>
) : CommandLineArgumentProvider {
    override fun asArguments(): MutableIterable<String> {
        return mutableListOf<String>().apply {
            addAll(codeGenerationOptions)
            addAll(codeOptimizationOptionsO2)
            addAll(emscriptenConfigurationOptions)
            addAll(exportedFunctionsConfiguration)
            addAll(SqliteConfiguration.wasmConfig)
            add("-DSQLITE_C=${sqliteCFile.get()}")
        }
    }

    companion object {
        val codeGenerationOptions = listOf(
            "-g3",
            "-fPIC",
            "--minify", "0",
            "--no-entry",
            "-Wno-limited-postlink-optimizations",
        )

        val codeOptimizationOptionsO2 = listOf(
            "-O2",
            "-flto",
        )

        val emscriptenConfigurationOptions = listOf(
            "-sALLOW_MEMORY_GROWTH",
            "-sALLOW_TABLE_GROWTH",
            "-sDYNAMIC_EXECUTION=0",
            "-sENVIRONMENT=node",
            "-sERROR_ON_UNDEFINED_SYMBOLS=1",
            "-sEXPORTED_RUNTIME_METHODS=wasmMemory",
            "-sEXPORT_NAME=sqlite3InitModule",
            "-sGLOBAL_BASE=4096",
            "-sIMPORTED_MEMORY",
            "-sINITIAL_MEMORY=16777216",
            "-sLLD_REPORT_UNDEFINED",
            "-sMODULARIZE",
            "-sNO_POLYFILL",
            "-sSTACK_SIZE=512KB",
            "-sSTANDALONE_WASM=0",
            "-sSTRICT_JS=0",
            "-sUSE_CLOSURE_COMPILER=0",
            "-sWASM_BIGINT=1",
        )

        val exportedFunctionsConfiguration = listOf(
            //"-sEXPORTED_FUNCTIONS=${ExportedFunctions.defaultWasm.joinToString(",")}",
            "-sEXPORTED_FUNCTIONS=${ExportedFunctions.openHelper.joinToString(",")}",
        )
    }
}