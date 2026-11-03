package astral.loader

import org.json.JSONObject
import java.util.Locale

object LibraryMetadata {

    fun gameLabel(library: JSONObject): String {
        val packageName = library.optString("GAME").trim()
        val type = library.optString("TYPE").trim()
        return when {
            packageName.contains("mobile.legends", ignoreCase = true) -> "Mobile Legends"
            packageName.contains("codm", ignoreCase = true) -> "Mobile Legends"
            type.isNotBlank() -> type
            packageName.isNotBlank() -> packageName
            else -> "Game unavailable"
        }
    }

    fun versionLabel(library: JSONObject): String {
        val rawVersion = sequenceOf("VERSION", "APP_VERSION", "VERSION_NAME", "APP_VERSION_NAME")
            .map { library.optString(it).trim() }
            .firstOrNull { it.isNotBlank() }
            ?: return ""
        return if (rawVersion.startsWith("v", ignoreCase = true)) rawVersion else "v$rawVersion"
    }

    fun status(library: JSONObject): String {
        val maintenanceValue = library.opt("MAINTENANCE")
        val maintenance = when (maintenanceValue) {
            is Boolean -> maintenanceValue
            is String -> maintenanceValue.equals("true", ignoreCase = true)
            else -> false
        }
        if (maintenance) return "MAINTENANCE"

        val rawStatus = library.optString("STATUS").trim()
        if (rawStatus.isBlank()) return "ACTIVE"
        return rawStatus.uppercase(Locale.US).replace('_', ' ')
    }

    fun isActive(library: JSONObject): Boolean = status(library) == "ACTIVE"
}
