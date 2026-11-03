package astral.loader

import android.content.ContentResolver
import android.provider.Settings
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class PanelLicenseValidator(private val contentResolver: ContentResolver) {

    fun validate(licenseKey: String, endpoint: String, game: String): String {
        val endpointUrl = endpoint.trim()
        if (endpointUrl.isBlank()) return "Panel endpoint unavailable"

        return try {
            val serial = (Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: "android")
                .replace(Regex("[^A-Za-z0-9_-]"), "_")
            val form = listOf("game" to game, "user_key" to licenseKey, "serial" to serial)
                .joinToString("&") { (name, value) ->
                    "${URLEncoder.encode(name, "UTF-8")}=${URLEncoder.encode(value, "UTF-8")}"
                }
            val connection = (URL(endpointUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                doInput = true
                useCaches = false
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            }

            try {
                connection.outputStream.use { it.write(form.toByteArray(Charsets.UTF_8)) }
                val responseCode = connection.responseCode
                val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
                val response = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                    ?: return "No validation response (HTTP $responseCode)"
                val result = runCatching { JSONObject(response.trim().removePrefix("\uFEFF")) }
                    .getOrElse { return "Invalid panel response (HTTP $responseCode)" }
                if (result.optBoolean("status")) SUCCESS_TOKEN else result.optString("reason", "Invalid license key")
            } finally {
                connection.disconnect()
            }
        } catch (error: Exception) {
            "Validation request failed: ${error.message ?: error::class.simpleName}"
        }
    }

    fun isSuccess(response: String): Boolean = response == SUCCESS_TOKEN

    private companion object {
        const val TIMEOUT_MS = 15_000
        val SUCCESS_TOKEN = ObfuscatedTextDecoder.decode(
            "5226ec94c5746d67161e56b92005078d7b65b6c43e38da9cf84c65c2837e1499f1840cca9aa7b90480fdbe17995178bf7ccba86ab5d8e6d4ff9e6e9b5e8bc6d2130525f00e95"
        )
    }
}
