package ru.pixnews.gradle.wasm.sqlite3

import java.io.Serializable
import javax.inject.Inject
import org.gradle.api.Named
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory

public open class Sqlite3WasmBuildSpec @Inject internal constructor(
    objects: ObjectFactory,
    providers: ProviderFactory,
    private val name: String,
) : Named, Serializable {
    override fun getName(): String = name

    public val sqliteVersion: Property<String> = objects.property(String::class.java)
        .convention("3450100")

    public val sqlite3Source: ConfigurableFileCollection = objects.fileCollection()

    public val wasmBaseFileName: Property<String> = objects.property(String::class.java)
        .convention("sqlite3")

    public val wasmUnstrippedFileName: Property<String> = objects.property(String::class.java)
        .convention(providers.provider {
            "${wasmBaseFileName.get()}-${name}-${sqliteVersion.get()}-unstripped.wasm"
        })

    public val wasmFileName: Property<String> = objects.property(String::class.java)
        .convention(providers.provider {
            "${wasmBaseFileName.get()}-${name}-${sqliteVersion.get()}.wasm"
        })


    public companion object {
        private const val serialVersionUID: Long = -1
    }
}
