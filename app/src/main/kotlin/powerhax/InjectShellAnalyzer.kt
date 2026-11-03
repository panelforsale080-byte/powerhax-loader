package astral.loader

import java.io.File

object InjectShellAnalyzer {

    data class Analysis(
        val processTerminated: Boolean,
        val ptraceLost: Boolean,
        val userHints: List<String>
    )

    fun analyze(shellOutput: String, unityPid: Int): Analysis {
        val out = shellOutput.lowercase()
        val terminated = out.contains("target process terminated") ||
            out.contains("process terminated") ||
            out.contains("signal 9") ||
            out.contains("sigkill")
        val ptraceLost = out.contains("not attached") ||
            out.contains("couldn't open status file") ||
            out.contains("ptrace_setregs failed")
        val procGone = unityPid > 0 && !File("/proc/$unityPid").exists()

        val hints = mutableListOf<String>()
        if (terminated || procGone) {
            hints += "MLBB Unity process died during inject — do NOT re-inject immediately"
            hints += "Reopen game → stay in lobby 30s → inject ONCE"
            hints += "Use libTool-inject.so (anti-crash build) when available"
        }
        if (ptraceLost && (terminated || procGone)) {
            hints += "Ptrace errors are normal after the game already crashed"
        }

        return Analysis(
            processTerminated = terminated || procGone,
            ptraceLost = ptraceLost,
            userHints = hints
        )
    }
}
