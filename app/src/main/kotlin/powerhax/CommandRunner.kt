package astral.loader

class CommandRunner {

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

                process.inputStream
                    .bufferedReader()
                    .use { it.readText() }
                process.waitFor()
            } catch (_: Exception) {
            }
        }.start()
    }
}
