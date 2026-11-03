package astral.loader

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
            return SelectedLibrary(
                name = json.optString("NAME"),
                downloadUrl = json.optString("DOWNLOAD"),
                connectUrl = json.optString("URL"),
                architecture = json.optString("ARCH"),
                jniKey = json.optString("KEY"),
                gamePackage = json.optString("GAME"),
                type = json.optString("TYPE")
            )
        }
    }
}
