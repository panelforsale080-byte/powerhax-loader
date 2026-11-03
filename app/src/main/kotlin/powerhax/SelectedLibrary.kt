package astral.loader

import android.os.Build
import org.json.JSONObject

data class SelectedLibrary(
    val name: String = "",
    val downloadUrl: String = "",
    val connectUrl: String = "",
    val architecture: String = "",
    val jniKey: String = "",
    val gamePackage: String = "",
    val type: String = ""
) {
    companion object {
        fun fromJson(json: JSONObject): SelectedLibrary {
            val rawGame = json.optString("GAME")
            val gamePackage = normalizeGamePackage(rawGame)
            return SelectedLibrary(
                name = json.optString("NAME"),
                downloadUrl = json.optString("DOWNLOAD"),
                connectUrl = json.optString("URL"),
                architecture = json.optString("ARCH"),
                jniKey = json.optString("KEY"),
                gamePackage = gamePackage,
                type = json.optString("TYPE")
            )
        }

        fun normalizeGamePackage(raw: String): String {
            val mlbb = "com.mobile.legends"
            if (raw.isBlank()) return mlbb
            val lower = raw.lowercase()
            if (lower.contains("cod") || lower.contains("duty") || lower.contains("activision")) {
                return mlbb
            }
            return raw
        }

        fun resolveArchitecture(catalogArch: String): String {
            if (catalogArch.equals("32bit", ignoreCase = true)) return "32bit"
            if (catalogArch.equals("64bit", ignoreCase = true)) return "64bit"
            val primary = Build.SUPPORTED_ABIS.firstOrNull().orEmpty()
            return if (primary.contains("64", ignoreCase = true)) "64bit" else "32bit"
        }
    }
}
