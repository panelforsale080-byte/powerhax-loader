package astral.loader

import android.content.res.AssetManager
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class AssetExtractor(
    private val assets: AssetManager,
    private val cacheDirectory: File
) {

    fun extractAll() {
        try {
            extractPath("")
        } catch (_: Exception) {
        }
    }

    private fun extractPath(path: String) {
        try {
            val files = assets.list(path) ?: return
            for (fileName in files) {
                val assetPath = if (path.isEmpty()) fileName else "$path/$fileName"
                if (assets.list(assetPath)?.isNotEmpty() == true) {
                    extractPath(assetPath)
                } else {
                    copyAsset(assetPath)
                }
            }
        } catch (_: IOException) {
        }
    }

    private fun copyAsset(assetPath: String) {
        val outputFile = File(cacheDirectory, assetPath)
        if (outputFile.exists() && outputFile.length() > 0) return

        try {
            outputFile.parentFile?.mkdirs()
            assets.open(assetPath).use { inputStream ->
                FileOutputStream(outputFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            if (assetPath.startsWith("lib")) {
                outputFile.setReadable(true, false)
                outputFile.setWritable(true, false)
                outputFile.setExecutable(true, false)
            }
        } catch (_: IOException) {
        }
    }
}
