package com.geomgang.game.i18n

import java.util.Locale

/** 삐약 시리즈와 같은 13개 표시 언어. */
enum class GameLanguage(
    val tag: String,
    val nativeName: String,
    val assetCode: String,
) {
    KOREAN("ko", "한국어", "ko"),
    ENGLISH("en", "English", "en"),
    JAPANESE("ja", "日本語", "ja"),
    CHINESE_SIMPLIFIED("zh-Hans", "简体中文", "zh"),
    CHINESE_TRADITIONAL("zh-Hant", "繁體中文", "zh_Hant"),
    SPANISH("es", "Español", "es"),
    FRENCH("fr", "Français", "fr"),
    GERMAN("de", "Deutsch", "de"),
    PORTUGUESE("pt", "Português", "pt"),
    RUSSIAN("ru", "Русский", "ru"),
    THAI("th", "ไทย", "th"),
    VIETNAMESE("vi", "Tiếng Việt", "vi"),
    INDONESIAN("id", "Bahasa Indonesia", "id"),
    ;

    companion object {
        val selectable: List<GameLanguage> = entries

        fun fromTag(tag: String?): GameLanguage? {
            if (tag.isNullOrBlank()) return null
            val normalized = tag.replace('_', '-').lowercase(Locale.ROOT)
            if (normalized.startsWith("zh")) {
                val traditional = normalized.contains("hant") ||
                    normalized.endsWith("-tw") || normalized.endsWith("-hk") ||
                    normalized.endsWith("-mo")
                return if (traditional) CHINESE_TRADITIONAL else CHINESE_SIMPLIFIED
            }
            val language = normalized.substringBefore('-')
            return entries.firstOrNull {
                it.tag.substringBefore('-').equals(language, ignoreCase = true)
            }
        }

        fun fromLocale(locale: Locale): GameLanguage? {
            if (locale.language == "zh") {
                val script = locale.script
                val country = locale.country.uppercase(Locale.ROOT)
                return if (script.equals("Hant", ignoreCase = true) || country in setOf("TW", "HK", "MO")) {
                    CHINESE_TRADITIONAL
                } else {
                    CHINESE_SIMPLIFIED
                }
            }
            return fromTag(locale.language)
        }

        /** 직접 고른 언어 → 기기 언어 → 영어 순서로 결정한다. */
        fun resolve(savedTag: String?, preferredLocales: List<Locale>): GameLanguage =
            fromTag(savedTag)
                ?: preferredLocales.firstNotNullOfOrNull(::fromLocale)
                ?: ENGLISH
    }
}
