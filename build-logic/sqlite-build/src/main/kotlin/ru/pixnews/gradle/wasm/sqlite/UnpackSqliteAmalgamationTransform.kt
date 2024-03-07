package ru.pixnews.gradle.wasm.sqlite

import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import kotlin.io.path.Path
import kotlin.io.path.name
import org.gradle.api.artifacts.transform.InputArtifact
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "Not worth caching")
abstract class UnpackSqliteAmalgamationTransform : TransformAction<TransformParameters.None> {
    @get:InputArtifact
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val inputZipFile: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        val zipFile = inputZipFile.get().asFile
        val unzipDir = outputs.dir(zipFile.nameWithoutExtension).toPath()

        ZipFile(zipFile).use { inputZip: ZipFile ->
            for (entry: ZipEntry in inputZip.entries()) {
                if (entry.isDirectory) continue
                val fname = Path(entry.name).name
                if (fname !in SQLITE_EXTRACTED_FILES) continue
                inputZip.getInputStream(entry).use {
                    Files.copy(it, unzipDir.resolve(fname))
                }
            }
        }
    }

    internal companion object {
        val SQLITE_EXTRACTED_FILES = listOf("sqlite3.c", "sqlite3.h")
    }
}