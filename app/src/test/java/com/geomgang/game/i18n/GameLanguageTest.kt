package com.geomgang.game.i18n

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GameLanguageTest {
    @Test
    fun `삐약 시리즈와 같은 13개 언어를 제공한다`() {
        assertEquals(13, GameLanguage.selectable.size)
        assertEquals(13, GameLanguage.selectable.map { it.assetCode }.toSet().size)
    }

    @Test
    fun `중국어 대만 홍콩 마카오는 번체로 보낸다`() {
        assertEquals(GameLanguage.CHINESE_TRADITIONAL, GameLanguage.fromTag("zh-Hant"))
        assertEquals(GameLanguage.CHINESE_TRADITIONAL, GameLanguage.fromTag("zh-TW"))
        assertEquals(GameLanguage.CHINESE_TRADITIONAL, GameLanguage.fromTag("zh-HK"))
        assertEquals(
            GameLanguage.CHINESE_TRADITIONAL,
            GameLanguage.fromLocale(Locale.Builder().setLanguage("zh").setRegion("MO").build()),
        )
        assertEquals(GameLanguage.CHINESE_SIMPLIFIED, GameLanguage.fromTag("zh-CN"))
    }

    @Test
    fun `지원하지 않는 기기 언어는 영어로 대체한다`() {
        assertNull(GameLanguage.fromTag("ar"))
        assertEquals(
            GameLanguage.ENGLISH,
            GameLanguage.resolve(null, listOf(Locale.forLanguageTag("ar"))),
        )
        assertEquals(
            GameLanguage.KOREAN,
            GameLanguage.resolve(null, listOf(Locale.KOREAN, Locale.ENGLISH)),
        )
    }

    @Test
    fun `직접 고른 언어가 기기 언어보다 우선한다`() {
        assertEquals(
            GameLanguage.GERMAN,
            GameLanguage.resolve("de", listOf(Locale.KOREAN)),
        )
    }
}
