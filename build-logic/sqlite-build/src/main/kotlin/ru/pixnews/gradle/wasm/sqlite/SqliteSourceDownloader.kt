@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")

package ru.pixnews.gradle.wasm.sqlite

import java.lang.Boolean
import org.gradle.api.Project
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.attributes.Attribute
import org.gradle.api.file.FileCollection
import org.gradle.kotlin.dsl.dependencies

val EXTRACTED_SQLITE_ATTRIBUTE: Attribute<Boolean> = Attribute.of("extracted-sqlite", Boolean::class.java)
private val EXTRACTED_SQLITE_TRUE = Boolean.TRUE as Boolean
private val EXTRACTED_SQLITE_FALSE = Boolean.FALSE as Boolean

internal fun Project.setupSqliteSource(
    sqliteVersion: String
): FileCollection {
    val sqliteConfiguration = configurations.detachedConfiguration(
        dependencyFactory.create("sqlite", "amalgamation", sqliteVersion, null, "zip")
    ).attributes {
        attribute(EXTRACTED_SQLITE_ATTRIBUTE, EXTRACTED_SQLITE_FALSE)
    }

    project.dependencies {
        attributesSchema.attribute(EXTRACTED_SQLITE_ATTRIBUTE)
        artifactTypes.maybeCreate("zip").attributes.attribute(EXTRACTED_SQLITE_ATTRIBUTE, EXTRACTED_SQLITE_FALSE)
        registerTransform(UnpackSqliteAmalgamationTransform::class.java) {
            from.attribute(EXTRACTED_SQLITE_ATTRIBUTE, EXTRACTED_SQLITE_FALSE)
            to.attribute(EXTRACTED_SQLITE_ATTRIBUTE, EXTRACTED_SQLITE_TRUE)
        }
    }

    val unpackedSqliteSrc = sqliteConfiguration
        .incoming
        .artifactView {
            attributes {
                attribute(EXTRACTED_SQLITE_ATTRIBUTE, EXTRACTED_SQLITE_TRUE)
                attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, ArtifactTypeDefinition.ZIP_TYPE)
            }
        }.files.asFileTree

    val sqlite3c = unpackedSqliteSrc.filter { it.isFile && it.name == "sqlite3.c" }

    return sqlite3c
}

