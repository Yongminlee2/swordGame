package com.geomgang.game.i18n

import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalizationCatalogTest {
    private val hangul = Regex("[\\uAC00-\\uD7A3]")

    @Test
    fun `12개 번역 카탈로그가 같은 키를 모두 갖는다`() {
        val catalogs = loadCatalogs()
        val expectedCodes = GameLanguage.selectable
            .filterNot { it == GameLanguage.KOREAN }
            .map { it.assetCode }
            .toSet()
        assertEquals(expectedCodes, catalogs.keys)

        val englishKeys = catalogs.getValue("en").keys
        assertTrue("catalog is unexpectedly small", englishKeys.size >= 1_000)
        catalogs.forEach { (code, values) ->
            assertEquals("missing or extra key in $code", englishKeys, values.keys)
            val leaks = values.filterValues(hangul::containsMatchIn)
            assertTrue("Hangul leaked into $code: ${leaks.entries.take(3)}", leaks.isEmpty())
        }
    }

    @Test
    fun `실행 중 숫자와 이름을 붙인 문장도 한글이 남지 않는다`() {
        val catalogs = loadCatalogs()
        val samples = listOf(
            "골드 1,000 · 강화석 3",
            "용검 +20 도전",
            "정수력 10로 새기기",
            "심연의 왕 격파 · 조각 12 획득",
            "전설 파괴 방지권 ×3",
        )
        GameLanguage.selectable.filterNot { it == GameLanguage.KOREAN }.forEach { language ->
            val translator = GameTranslator.fromTranslations(
                language,
                catalogs.getValue(language.assetCode),
            )
            samples.forEach { source ->
                val translated = translator.translate(source)
                assertFalse("${language.tag}: $translated", hangul.containsMatchIn(translated))
            }
        }
    }

    @Test
    fun `영어 용어집이 게임 뜻을 유지한다`() {
        val english = loadCatalogs().getValue("en")
        assertEquals("Enhance", english["강화"])
        assertEquals("Shards", english["조각"])
        assertEquals("Essence Power ", english["정수력 "])
        assertEquals("Upgrade Stone", english["강화석"])
        assertEquals("Collection", english["도감"])
    }

    private fun loadCatalogs(): Map<String, Map<String, String>> {
        val directory = sequenceOf(
            File("src/main/assets/i18n"),
            File("app/src/main/assets/i18n"),
        ).firstOrNull(File::isDirectory) ?: error("cannot find localization assets")

        return directory.listFiles { file -> file.extension == "json" }
            .orEmpty()
            .associate { file ->
                val values = Json.parseToJsonElement(file.readText()).jsonObject
                    .mapValues { (_, value) -> value.jsonPrimitive.content }
                file.nameWithoutExtension to values
            }
    }
}
