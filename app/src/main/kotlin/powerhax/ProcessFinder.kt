package astral.loader

import android.util.Log
import java.io.File

object ProcessFinder {

    private const val TAG = "AstralProcessFinder"

    data class Target(val pid: Int, val label: String)

    fun findMlbb(): Target? {
        val proc = File("/proc")
        val dirs = proc.listFiles() ?: return null
        var best: Target? = null
        var bestScore = Int.MIN_VALUE
        for (d in dirs) {
            if (!d.isDirectory) continue
            val name = d.name
            if (name.isEmpty() || !name.all { it.isDigit() }) continue
            try {
                val cmdBytes = File(d, "cmdline").readBytes()
                val cmdline = String(cmdBytes, Charsets.UTF_8).substringBefore('\u0000')
                val match = cmdline.contains("UnityKillsMe", ignoreCase = true) ||
                    cmdline.contains("UnityKills", ignoreCase = true) ||
                    cmdline.contains("com.mobile.legends", ignoreCase = true) ||
                    cmdline.contains("mobile.legends", ignoreCase = true)
                if (match) {
                    val label = cmdline.ifBlank { "UnityKillsMe" }
                    val pid = name.toInt()
                    val score = processScore(cmdline)
                    Log.i(TAG, "Process candidate pid=$name score=$score cmdline=$label")
                    if (score > bestScore) {
                        bestScore = score
                        best = Target(pid, label)
                    }
                }
            } catch (e: Throwable) {
                Log.d(TAG, "Skip pid $name: ${e.message}")
            }
        }
        if (best != null) {
            Log.i(TAG, "Process selected pid=${best.pid} cmdline=${best.label}")
            return best
        }
        Log.w(TAG, "No MLBB process (UnityKillsMe / com.mobile.legends) in /proc")
        return null
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
