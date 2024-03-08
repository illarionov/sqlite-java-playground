package ru.pixnews.gradle.wasm.emscripten

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.listProperty
import org.gradle.process.CommandLineArgumentProvider
import org.gradle.process.ExecOperations
import ru.pixnews.gradle.wasm.sqlite3.BuildDirPath.COMPILE_WORK_DIR
import ru.pixnews.gradle.wasm.sqlite3.BuildDirPath.compileUnstrippedResultDir

@CacheableTask
abstract class EmscriptenBuildTask @Inject constructor(
    private val execOperations: ExecOperations,
    objects: ObjectFactory,
    layout: ProjectLayout,
    providers: ProviderFactory,
) : DefaultTask() {
    @get:InputFiles
    @get:SkipWhenEmpty
    @get:IgnoreEmptyDirectories
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val source: ConfigurableFileCollection = objects.fileCollection()

    @get:Input
    @Optional
    val emscriptenRoot: Property<File> = objects.property(File::class.java).convention(
        providers
            .environmentVariable("EMSDK")
            .orElse(providers.gradleProperty("emsdkRoot"))
            .map { File(it) }
    )

    @get:Internal
    val emccExecutablePath: Property<String> = objects.property(String::class.java).convention(
        "upstream/emscripten/emcc"
    )

    @get:Internal
    val emccExecutable: Property<File> = objects.property(File::class.java).convention(
        emscriptenRoot.zip<String?, File>(emccExecutablePath) { root, fileName -> File(root, fileName) }
    )

    @get:Input
    @get:Optional
    val emccVersion: Property<String> = objects.property(String::class.java).convention("3.1.55")

    @get:Input
    val outputFileName: Property<String> = objects.property(String::class.java)

    @get:OutputDirectory
    @Optional
    val outputDirectory: DirectoryProperty = objects.directoryProperty().convention(
        layout.buildDirectory.dir(compileUnstrippedResultDir("Main"))
    )

    @get:Input
    @Optional
    val workingDir: Property<File> = objects.property(File::class.java).convention(
        layout.buildDirectory.dir(COMPILE_WORK_DIR).map { it.asFile }
    )

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:Optional
    val includes: ConfigurableFileCollection = objects.fileCollection()

    @get:Internal
    val outputFile: RegularFileProperty = objects.fileProperty().convention(
        outputDirectory.zip<String, RegularFile>(outputFileName, Directory::file)
    )

    @get:InputFiles
    @Optional
    @PathSensitive(PathSensitivity.NONE)
    val emscriptenConfigFile: ConfigurableFileCollection = objects.fileCollection()

    @get:Nested
    val additionalArgumentProviders: ListProperty<CommandLineArgumentProvider> = objects.listProperty()

    @get:Internal
    val emscriptenCacheDir: DirectoryProperty = objects.directoryProperty()

    @TaskAction
    fun build() {
        checkEmsdkVersion()

        val workingDir = this@EmscriptenBuildTask.workingDir.get()
        workingDir.mkdirs()

        val cmdLine = buildCommandLine()

        execOperations.exec {
            this.commandLine = cmdLine
            this.workingDir = workingDir
            this.environment = getEmsdkEnvironment()
        }.rethrowFailure().assertNormalExitValue()
    }

    private fun buildCommandLine(): List<String> = buildList {
        val emcc = getEmccExecutableOrThrow()
        val workDir = this@EmscriptenBuildTask.workingDir.get()

        add(emcc.toString())

        add("-o")
        add(outputFile.get().toString())

        // Do not depend on ~/.emscripten
        add("--em-config")
        add(getEmscriptenConfigFile().toString())

        if (emscriptenCacheDir.isPresent) {
            val cacheDir = emscriptenCacheDir.get()
            add("--cache")
            add(cacheDir.toString())
        }

        includes.forEach { includePath ->
            val relativePath = includePath.relativeToOrSelf(workDir)
            add("-I${relativePath}")
        }

        additionalArgumentProviders.get().forEach { argumentProvider ->
            addAll(argumentProvider.asArguments())
        }

        source.forEach { sourcePath ->
            val relativePath = sourcePath.relativeToOrSelf(workDir)
            add(relativePath.toString())
        }
    }

    private fun checkEmsdkVersion() {
        if (!emccVersion.isPresent) return

        val emcc = getEmccExecutableOrThrow().toString()
        val requiredVersion = emccVersion.get()

        val stdErr = ByteArrayOutputStream()
        execOperations.exec {
            commandLine = listOf(emcc, "-v")
            errorOutput = stdErr
            environment = getEmsdkEnvironment()
        }.rethrowFailure().assertNormalExitValue()

        val firstLine: String = ByteArrayInputStream(stdErr.toByteArray()).bufferedReader().use {
            it.readLine()
        } ?: error("Can not read Emscripten SDK version")

        val version = EMCC_VERSION_REGEX.matchEntire(firstLine)?.groups?.get(1)?.value

        if (requiredVersion != version) {
            throw IllegalStateException("The installed version of Emscripten SDK `${version}` differs from the required version `$requiredVersion`")
        }
    }

    private fun getEmsdkEnvironment(): Map<String, Any> = buildMap {
        put("EMSDK", emscriptenRoot.get().toString())
    }

    private fun getEmccExecutableOrThrow(): File {
        val path = emccExecutable.orNull ?: error(
            "Can not find Emscripten SDK installation directory. EMSDK environment variable should be defined"
        )
        check(path.isFile) {
            "Can not find Emscripten Compiler (emcc) executable. `$path` is not a file"
        }
        return path
    }

    private fun getEmscriptenConfigFile(): File {
        val files = emscriptenConfigFile.files
        return if (files.isNotEmpty()) {
            files.first()
        } else {
            emscriptenRoot.get().resolve(".emscripten")
        }
    }

    private companion object {
        private val EMCC_VERSION_REGEX = """emcc\s+\(Emscripten.+\)\s+(\S+)\s+.*""".toRegex()
    }
}