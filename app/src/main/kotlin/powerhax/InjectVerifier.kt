package astral.loader

import java.io.File

object InjectVerifier {

    private val LIB_NEEDLES = listOf("libTool.so", "libtool.so", "/files/inject/libTool")

    fun isLibLoaded(pid: Int): Boolean {
        if (pid <= 0) return false
        val maps = File("/proc/$pid/maps")
        if (!maps.canRead()) return false
        return try {
            maps.useLines { lines ->
                lines.any { line ->
                    LIB_NEEDLES.any { needle -> line.contains(needle, ignoreCase = true) }
                }
            }
        } catch (_: Exception) {
            false
        }
    }
}
