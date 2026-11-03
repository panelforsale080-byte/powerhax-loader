package astral.loader

import android.util.Log

class CommandRunner {

    private companion object {
        const val TAG = "AstralCommandRunner"
    }

    fun hasRootAccess(): Boolean {
        return try {
            val process = ProcessBuilder("su", "-c", "echo root")
                .redirectErrorStream(true)
                .start()

            process.inputStream.bufferedReader().use { it.readLine() }
                ?.contains("root") == true
        } catch (_: Exception) {
            false
        }
    }

    data class ShellResult(val exitCode: Int, val output: String)

    fun run(command: String) {
        Thread { runSync(command) }.start()
    }

    fun runSync(command: String): ShellResult {
        val shell = if (hasRootAccess()) "su" else "sh"
        return try {
            val process = ProcessBuilder(shell, "-c", command)
                .redirectErrorStream(true)
                .start()

            val out = process.inputStream.bufferedReader().use { it.readText() }
            val code = process.waitFor()
            if (out.isNotBlank()) {
                Log.i(TAG, "shell out: $out")
            }
            Log.i(TAG, "shell exit=$code cmd=${command.take(200)}")
            ShellResult(code, out)
        } catch (e: Exception) {
            Log.e(TAG, "shell failed: ${e.message}")
            ShellResult(-1, e.message.orEmpty())
        }
    }
}
