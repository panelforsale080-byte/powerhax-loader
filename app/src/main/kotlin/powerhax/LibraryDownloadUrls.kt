package astral.loader

import java.net.HttpURLConnection
import java.net.URL

object LibraryDownloadUrls {

    private const val BASE =
        "https://raw.githubusercontent.com/panelforsale080-byte/lib.so-loader/main"

    /** Crash-safe build (no instant hack_thread on remote dlopen). */
    const val INJECT_PAYLOAD = "$BASE/libTool-inject.so"

    const val LEGACY_FULL = "$BASE/libTool.so"

    fun resolvePrimary(): String {
        return if (urlExists(INJECT_PAYLOAD)) INJECT_PAYLOAD else LEGACY_FULL
    }

    private fun urlExists(url: String): Boolean {
        return try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.requestMethod = "HEAD"
            conn.connectTimeout = 12_000
            conn.readTimeout = 12_000
            conn.connect()
            val ok = conn.responseCode == HttpURLConnection.HTTP_OK
            conn.disconnect()
            ok
        } catch (_: Exception) {
            false
        }
    }
}
