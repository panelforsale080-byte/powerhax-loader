package astral.loader

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class InjectUiLog(
    private val onLine: (String) -> Unit
) {
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)

    fun line(message: String) {
        val stamp = timeFormat.format(Date())
        onLine("[$stamp] $message")
    }

    fun blank() {
        onLine("")
    }
}
