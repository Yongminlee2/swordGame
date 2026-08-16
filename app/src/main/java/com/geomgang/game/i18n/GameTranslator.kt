package com.geomgang.game.i18n

import android.content.res.AssetManager
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * 완성된 화면 문장을 번역한다.
 *
 * 도메인 모델의 이름·설명은 순수 Kotlin 모듈에 남겨 저장 형식을 깨뜨리지 않고,
 * Compose가 텍스트를 그리기 직전에 지정한 언어로 바꾼다. 숫자·강화 단계처럼 실행 중
 * 붙는 값은 그대로 두고, 앞뒤의 고정 문장 조각만 긴 것부터 교체한다.
 */
class GameTranslator private constructor(
    val language: GameLanguage,
    translations: Map<String, String>,
) {
    private val exact = translations
    private val fragmentsByFirstChar = translations.entries
        .asSequence()
        .filter { (source, target) -> source != target && source.isNotEmpty() }
        .map { it.key to it.value }
        .groupBy { (source) -> source.first() }
        .mapValues { (_, values) -> values.sortedByDescending { (source) -> source.length } }

    private val cache = object : LinkedHashMap<String, String>(256, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?): Boolean =
            size > CACHE_LIMIT
    }

    fun translate(source: String): String {
        if (language == GameLanguage.KOREAN || !HANGUL.containsMatchIn(source)) return source
        synchronized(cache) { cache[source]?.let { return it } }

        val translated = exact[source] ?: run {
            val candidates = source.asSequence()
                .distinct()
                .flatMap { fragmentsByFirstChar[it].orEmpty().asSequence() }
                .filter { (from) -> from.length <= source.length }
                .distinct()
                .sortedByDescending { (from) -> from.length }
            candidates.fold(source) { text, (from, to) ->
                if (text.contains(from)) text.replace(from, to) else text
            }
        }
        synchronized(cache) { cache[source] = translated }
        return translated
    }

    companion object {
        private const val CACHE_LIMIT = 2_048
        private val HANGUL = Regex("[\\uAC00-\\uD7A3]")

        val korean = GameTranslator(GameLanguage.KOREAN, emptyMap())

        internal fun fromTranslations(
            language: GameLanguage,
            translations: Map<String, String>,
        ): GameTranslator = GameTranslator(language, translations)

        fun load(assets: AssetManager, language: GameLanguage): GameTranslator {
            if (language == GameLanguage.KOREAN) return korean
            val path = "i18n/${language.assetCode}.json"
            val root = assets.open(path).bufferedReader(Charsets.UTF_8).use { reader ->
                Json.parseToJsonElement(reader.readText()).jsonObject
            }
            val values = root.mapValues { (_, value) -> value.jsonPrimitive.content }
            return GameTranslator(language, values)
        }
    }
}
