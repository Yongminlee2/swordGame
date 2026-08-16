package com.geomgang.game.i18n

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalizedTextCoverageTest {
    @Test
    fun `모든 Compose 화면은 공통 번역 텍스트를 사용한다`() {
        val sourceRoot = sequenceOf(
            File("src/main/java/com/geomgang/game"),
            File("app/src/main/java/com/geomgang/game"),
        ).firstOrNull(File::isDirectory) ?: error("cannot find app sources")

        val violations = sourceRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" && it.name != "LocalizedText.kt" }
            .mapNotNull { file ->
                val source = file.readText()
                val rawText = Regex("(?<![A-Za-z0-9_])Text\\(").containsMatchIn(source)
                val rawImport = source.contains("import androidx.compose.material3.Text\n") ||
                    source.contains("import androidx.compose.material3.Text\r\n")
                if (rawText || rawImport) file.relativeTo(sourceRoot).path else null
            }
            .toList()

        assertTrue("unlocalized Material Text calls: $violations", violations.isEmpty())
    }

    @Test
    fun `Android 앱 언어는 중국어 지역을 포함해 14개 로케일을 선언한다`() {
        val res = sequenceOf(File("src/main/res"), File("app/src/main/res"))
            .firstOrNull(File::isDirectory) ?: error("cannot find resources")
        val config = File(res, "xml/locales_config.xml").readText()
        assertEquals(14, Regex("<locale android:name=").findAll(config).count())

        val translatedDirectories = setOf(
            "values-ko", "values-ja", "values-zh", "values-zh-rTW", "values-zh-rHK",
            "values-es", "values-fr", "values-de", "values-pt", "values-ru",
            "values-th", "values-vi", "values-in",
        )
        translatedDirectories.forEach { directory ->
            assertTrue("missing launcher label: $directory", File(res, "$directory/strings.xml").isFile)
        }
        assertTrue(File(res, "values/strings.xml").readText().contains("Sword Forge"))
        assertTrue(File(res, "values-ko/strings.xml").readText().contains("검 강화"))
    }
}
