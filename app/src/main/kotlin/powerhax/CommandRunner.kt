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

    fun run(command: String) {
        val shell = if (hasRootAccess()) "su" else "sh"

        Thread {
            try {
                val process = ProcessBuilder(shell, "-c", command)
                    .redirectErrorStream(true)
                    .start()

                val out = process.inputStream
                    .bufferedReader()
                    .use { it.readText() }
                if (out.isNotBlank()) {
                    Log.i(TAG, "shell out: $out")
                }
                val code = process.waitFor()
                Log.i(TAG, "shell exit=$code")
            } catch (_: Exception) {
            }
        }.start()
    }
}
