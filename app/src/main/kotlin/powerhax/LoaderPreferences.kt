package astral.loader

import android.content.Context

class LoaderPreferences(context: Context) {

    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    val licenses = LicenseStore(preferences)

    fun getMode(): String = preferences.getString(KEY_MODE, MODE_AUTO) ?: MODE_AUTO

    fun saveMode(mode: String) {
        preferences.edit().putString(KEY_MODE, mode).apply()
    }

    fun getDelaySeconds(): Int = preferences.getInt(KEY_DELAY, 0)

    fun saveDelaySeconds(seconds: Int) {
        preferences.edit().putInt(KEY_DELAY, seconds).apply()
    }

    fun getInjectionMethod(): String = preferences.getString(KEY_METHOD, DEFAULT_METHOD) ?: DEFAULT_METHOD

    fun saveInjectionMethod(method: String) {
        preferences.edit().putString(KEY_METHOD, method).apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "loaderPrefs"
        const val KEY_MODE = "injection_mode"
        const val KEY_DELAY = "injection_delay"
        const val KEY_METHOD = "injection_method"
        const val MODE_AUTO = "auto"
        const val DEFAULT_METHOD = "1"
    }
}
