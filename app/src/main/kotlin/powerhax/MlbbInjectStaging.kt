package astral.loader

import android.util.Log
import java.io.File

/**
 * Copies libTool.so into MLBB inject folders (same paths libtoolv2 uses before ptrace dlopen).
 * Loader cache path alone often loads without a visible menu on current MLBB builds.
 */
object MlbbInjectStaging {

    private const val TAG = "AstralInjectStaging"

    fun stagePaths(gamePackage: String, libraryFile: File, inGspace: Boolean): List<String> {
        val pkg = gamePackage.ifBlank { "com.mobile.legends" }
        val paths = mutableListOf<String>()
        if (inGspace) {
            paths += "/data/data/com.gspace.android/virtual/data/user/0/$pkg/files/inject/libTool.so"
            paths += "/data/user/0/$pkg/files/inject/libTool.so"
        }
        paths += "/data/data/$pkg/files/inject/libTool.so"
        paths += "/data/user/0/$pkg/files/inject/libTool.so"
        paths += "/sdcard/libtool_inject/libTool.so"
        paths += "/sdcard/Download/libtool_inject/libTool.so"
        return paths.distinct()
    }

    fun buildRootCopyScript(gamePackage: String, libraryFile: File, inGspace: Boolean): String {
        if (!libraryFile.isFile) return ""
        val src = shellQuote(libraryFile.absolutePath)
        val cmds = stagePaths(gamePackage, libraryFile, inGspace).map { dest ->
            val dir = dest.substringBeforeLast('/')
            "mkdir -p ${shellQuote(dir)} && cp -f $src ${shellQuote(dest)} && chmod 755 ${shellQuote(dest)}"
        }
        return cmds.joinToString(" && ")
    }

    fun detectGspaceFromProc(): Boolean {
        return try {
            File("/proc/self/maps").useLines { lines ->
                lines.any {
                    it.contains("com.gspace.android/virtual", ignoreCase = true) ||
                        it.contains("/virtual/data/user/0/", ignoreCase = true)
                }
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun shellQuote(path: String): String =
        "'" + path.replace("'", "'\\''") + "'"
}
