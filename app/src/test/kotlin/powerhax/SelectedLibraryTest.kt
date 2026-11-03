package astral.loader

import org.junit.Assert.assertEquals
import org.junit.Test

class SelectedLibraryTest {

    @Test
    fun storesCatalogContractFieldsInRuntimeConfiguration() {
        val selected = SelectedLibrary(
            name = "POWERHAX",
            downloadUrl = "https://example.test/lib.so",
            connectUrl = "https://example.test/connect",
            architecture = "64bit",
            jniKey = "test-key",
            gamePackage = "com.example.game",
            type = "CODMGR"
        )

        assertEquals("POWERHAX", selected.name)
        assertEquals("https://example.test/lib.so", selected.downloadUrl)
        assertEquals("https://example.test/connect", selected.connectUrl)
        assertEquals("64bit", selected.architecture)
        assertEquals("test-key", selected.jniKey)
        assertEquals("com.example.game", selected.gamePackage)
        assertEquals("CODM", selected.type)
    }
}
