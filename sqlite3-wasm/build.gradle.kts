
plugins {
    id("buildlogic.kotlin-library-conventions")
}

configurations {
    dependencyScope("wasmLibraries")
    resolvable("wasmLibrariesClasspath") {
        extendsFrom(configurations["wasmLibraries"])
        attributes {
            attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
            attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named("wasm-library"))
        }
    }
}

val wasmResourcesDir = layout.buildDirectory.dir("wasmLibraries")

val copyResourcesTask = tasks.register<Copy>("copyWasmLibrariesToResources") {
    from(configurations["wasmLibrariesClasspath"])
    into(wasmResourcesDir.map { it.dir("ru/pixnews/sqlite3/wasm/") })
    include("*.wasm")
}

kotlin {
    sourceSets {
        named("main") {
            resources.srcDir(files(wasmResourcesDir).builtBy(copyResourcesTask))
        }
    }
}

dependencies {
    "wasmLibraries"(project(":sqlite3-wasm-src"))
}
