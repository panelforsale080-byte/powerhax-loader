package astral.loader

import android.util.Log
import java.io.File

object ProcessFinder {

    private const val TAG = "AstralProcessFinder"

    data class Target(val pid: Int, val label: String) {
        val isUnityKills: Boolean
            get() = label.contains("UnityKills", ignoreCase = true)
    }

    /** All MLBB-related processes (for on-screen debug). */
    fun listMlbbCandidates(): List<Target> {
        val out = mutableListOf<Target>()
        scanProc { pid, cmdline ->
            if (isMlbbRelated(cmdline)) {
                out += Target(pid, cmdline.ifBlank { "pid=$pid" })
            }
        }
        return out.sortedByDescending { processScore(it.label) }
    }

    /** Unity game process — menu must inject here, not the launcher/main APK process. */
    fun findUnityKills(): Target? {
        var best: Target? = null
        var bestScore = Int.MIN_VALUE
        scanProc { pid, cmdline ->
            if (!cmdline.contains("UnityKills", ignoreCase = true)) return@scanProc
            if (!isMlbbRelated(cmdline)) return@scanProc
            val score = processScore(cmdline) + 200
            if (score > bestScore) {
                bestScore = score
                best = Target(pid, cmdline)
            }
        }
        best?.let { Log.i(TAG, "UnityKills target pid=${it.pid} cmdline=${it.label}") }
        return best
    }

    fun findMlbb(): Target? = findUnityKills() ?: findMainMlbb()

    private fun findMainMlbb(): Target? {
        var best: Target? = null
        var bestScore = Int.MIN_VALUE
        scanProc { pid, cmdline ->
            if (!isMlbbRelated(cmdline)) return@scanProc
            if (cmdline.contains("UnityKills", ignoreCase = true)) return@scanProc
            val score = processScore(cmdline)
            if (score > bestScore) {
                bestScore = score
                best = Target(pid, cmdline)
            }
        }
        return best
    }

    private fun scanProc(block: (pid: Int, cmdline: String) -> Unit) {
        val proc = File("/proc")
        val dirs = proc.listFiles() ?: return
        for (d in dirs) {
            if (!d.isDirectory) continue
            val name = d.name
            if (name.isEmpty() || !name.all { it.isDigit() }) continue
            try {
                val cmdBytes = File(d, "cmdline").readBytes()
                val cmdline = String(cmdBytes, Charsets.UTF_8).trim('\u0000')
                block(name.toInt(), cmdline)
            } catch (e: Throwable) {
                Log.d(TAG, "Skip pid $name: ${e.message}")
            }
        }
    }

    private fun isMlbbRelated(cmdline: String): Boolean {
        if (cmdline.isBlank()) return false
        return cmdline.contains("UnityKillsMe", ignoreCase = true) ||
            cmdline.contains("UnityKills", ignoreCase = true) ||
            cmdline.contains("com.mobile.legends", ignoreCase = true) ||
            cmdline.contains("mobile.legends", ignoreCase = true)
    }

    private fun processScore(cmdline: String): Int {
        var s = 0
        if (cmdline.contains("UnityKillsMe", ignoreCase = true)) s += 100
        else if (cmdline.contains("UnityKills", ignoreCase = true)) s += 80
        if (cmdline.contains("com.mobile.legends", ignoreCase = true)) s += 40
        if (cmdline.contains(":")) s += 10
        return s
    }
}
