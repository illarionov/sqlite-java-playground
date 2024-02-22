# Sqlite java Playground

Here I'm trying to build and run web-assembly version of the [Sqlite][] on JVM using [GraalVM][] or [Chicory][].

Main Modules:
* chicory
  * Code to run sqlite request on Chicory (`./gradlew :chicory:run`)
* graalvm
  * Code to run sqlite request on GraalVM (`./gradlew graalvm:run`)
* sqlite-wasm
  * Compiled WASM Sqlite binaries in resources and some common code
* host
  * Common code: common implementation of the Filesystem, wasi_snapshot_preview1 and Emscripten runtime environment bindings

[Sqlite]: https://sqlite.org/wasm/doc/trunk/index.md
[GraalVM]: https://www.graalvm.org/latest/reference-manual/wasm/
[Chicory]: https://github.com/dylibso/chicory

## License

[CC0-1.0](./LICENSE).




