dependencyResolutionManagement {
    // Reuse version catalog from the main build.
    versionCatalogs {
        create("libs", { from(files("../gradle/libs.versions.toml")) })
    }
}

rootProject.name = "sqlite-java-playground-build-logic"
include("project", "sqlite-build")