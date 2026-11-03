package astral.loader

import java.io.File

object InjectionCommandBuilder {

    fun build(
        cacheDirectory: File,
        library: SelectedLibrary,
        libraryFileName: String,
        injectionMethod: String,
        hasRootAccess: Boolean
    ): String {
        val rootArgument = when {
            injectionMethod == "2" -> ""
            injectionMethod == "3" -> ObfuscatedTextDecoder.decode(FORCED_ROOT_TOKEN)
            hasRootAccess -> ObfuscatedTextDecoder.decode(ROOT_TOKEN)
            else -> ""
        }

        val packageTarget = ObfuscatedTextDecoder.decode(PACKAGE_TARGET_TOKEN)
        val libraryPath = "${ObfuscatedTextDecoder.decode(LIBRARY_PATH_PREFIX_TOKEN)}${cacheDirectory.absolutePath}/$libraryFileName"
        val keyEntry = ObfuscatedTextDecoder.decode(KEY_ENTRY_TOKEN)
        val loaderName = when (library.architecture.lowercase()) {
            "32bit" -> ObfuscatedTextDecoder.decode(LOADER_32_TOKEN)
            else -> ObfuscatedTextDecoder.decode(LOADER_64_TOKEN)
        }
        val loaderPath = "${cacheDirectory.absolutePath}/$loaderName"

        return "$loaderPath $packageTarget ${library.gamePackage} $libraryPath $keyEntry ${library.jniKey} $rootArgument"
    }

    private const val ROOT_TOKEN = "995458a788"
    private const val FORCED_ROOT_TOKEN = "995458a788274bc8175051c9917764dc9aca3eccef6f4f36be47ae4e44"
    private const val PACKAGE_TARGET_TOKEN = "99c25a54c9b18c"
    private const val LIBRARY_PATH_PREFIX_TOKEN = "990aa2649286417c15"
    private const val KEY_ENTRY_TOKEN = "99d37e3d"
    private const val LOADER_32_TOKEN = "b465a3faf3249a3ed6875dad7abf"
    private const val LOADER_64_TOKEN = "b465a3faf3249a3ed60ea3ad7a4d"
}
