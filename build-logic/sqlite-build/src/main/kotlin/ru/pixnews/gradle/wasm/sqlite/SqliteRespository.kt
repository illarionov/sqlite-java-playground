package ru.pixnews.gradle.wasm.sqlite

import java.net.URI
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.ArtifactRepository

fun RepositoryHandler.sqliteRepository(): ArtifactRepository = ivy {
    url = URI("https://www.sqlite.org/")
    patternLayout {
        artifact("2024/sqlite-amalgamation-[revision].[ext]")
    }
    metadataSources {
        artifact()
    }
    content {
        includeModule("sqlite", "amalgamation")
    }
}

