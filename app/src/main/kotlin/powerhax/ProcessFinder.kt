package astral.loader

import android.util.Log
import java.io.File

object ProcessFinder {

    private const val TAG = "AstralProcessFinder"

    data class Target(val pid: Int, val label: String)

    fun findMlbb(): Target? {
        val proc = File("/proc")
        val dirs = proc.listFiles() ?: return null
        for (d in dirs) {
            if (!d.isDirectory) continue
            val name = d.name
            if (name.isEmpty() || !name.all { it.isDigit() }) continue
            try {
                val cmdBytes = File(d, "cmdline").readBytes()
                val cmdline = String(cmdBytes, Charsets.UTF_8).substringBefore('\u0000')
                val match = cmdline.contains("UnityKillsMe", ignoreCase = true) ||
                    cmdline.contains("UnityKills", ignoreCase = true) ||
                    cmdline.contains("com.mobile.legends", ignoreCase = true)
                if (match) {
                    val label = cmdline.ifBlank { "UnityKillsMe" }
                    Log.i(TAG, "Process found pid=$name cmdline=$label")
                    return Target(name.toInt(), label)
                }
            } catch (e: Throwable) {
                Log.d(TAG, "Skip pid $name: ${e.message}")
            }
        }
        Log.w(TAG, "No MLBB process (UnityKillsMe / com.mobile.legends) in /proc")
        return null
    }
}
