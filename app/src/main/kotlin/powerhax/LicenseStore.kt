package astral.loader

import android.content.SharedPreferences

class LicenseStore(private val preferences: SharedPreferences) {

    fun saveForLibrary(libraryName: String, licenseKey: String) {
        val entries = preferences
            .getStringSet(KEY_LICENSES, emptySet())
            ?.toMutableSet()
            ?: mutableSetOf()

        entries.removeAll { it.startsWith("$libraryName|") }
        entries += "$libraryName|$licenseKey"
        preferences.edit().putStringSet(KEY_LICENSES, entries).apply()
    }

    fun getForLibrary(libraryName: String): String? {
        return preferences
            .getStringSet(KEY_LICENSES, emptySet())
            ?.firstOrNull { it.startsWith("$libraryName|") }
            ?.substringAfter('|')
    }

    fun saveSelectedLibrary(libraryName: String) {
        preferences.edit().putString(KEY_SELECTED_LIBRARY, libraryName).apply()
    }

    fun getSelectedLibrary(): String? {
        return preferences.getString(KEY_SELECTED_LIBRARY, null)
    }

    private companion object {
        const val KEY_LICENSES = "license_keys_map"
        const val KEY_SELECTED_LIBRARY = "selected_server"
    }
}
