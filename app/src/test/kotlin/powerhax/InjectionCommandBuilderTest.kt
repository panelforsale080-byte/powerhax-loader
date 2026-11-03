package astral.loader

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class InjectionCommandBuilderTest {

    @Test
    fun buildsAnInjectionCommandFromSelectedLibrary() {
        val library = SelectedLibrary(
            name = "POWERHAX",
            architecture = "64bit",
            jniKey = "test-key",
            gamePackage = "com.example.game"
        )

        val command = InjectionCommandBuilder.build(
            cacheDirectory = File("cache"),
            library = library,
            libraryFileName = "libTool.so",
            injectionMethod = "1",
            hasRootAccess = false
        )

        assertTrue(command.contains("com.example.game"))
        assertTrue(command.contains("libTool.so"))
        assertTrue(command.contains("test-key"))
    }
}
