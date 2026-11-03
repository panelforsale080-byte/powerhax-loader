package astral.loader

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class LibraryCatalogClient(private val endpoint: String) {

    suspend fun load(): Result<JSONObject> = withContext(Dispatchers.IO) {
        runCatching {
            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                requestMethod = "GET"
                useCaches = false
            }

            try {
                val encoded = connection.inputStream
                    .bufferedReader(Charsets.UTF_8)
                    .use { it.readText() }
                    .trim()

                if (encoded.isBlank()) {
                    throw IOException("Empty server response")
                }

                val decoded = Base64.decode(encoded, Base64.DEFAULT)
                if (decoded.isEmpty()) {
                    throw IOException("Empty decoded server response")
                }

                val json = String(decoded, Charsets.UTF_8).trim()
                if (json.isBlank()) {
                    throw IOException("Empty server JSON")
                }

                JSONObject(json)
            } finally {
                connection.disconnect()
            }
        }
    }

    private companion object {
        const val TIMEOUT_MS = 15_000
    }
}
