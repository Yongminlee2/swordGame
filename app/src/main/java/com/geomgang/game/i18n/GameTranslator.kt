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

        val translated = exact[source] ?: byPattern(source) ?: run {
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

    /**
     * 숫자가 문장 **중간에** 끼는 문장을 통째로 번역한다.
     *
     * 화면 문장 상당수는 `"+${상수}까지는 실패해도 유지된다"` 처럼 도메인 값을 끼워
     * 만든다. 그러면 완성된 문장이 사전의 어떤 키와도 정확히 같지 않아 조각 치환으로
     * 떨어지는데, 한국어와 영어는 어순이 달라서 조각을 이어 붙이면 문장이 부서진다
     * (실제로 도움말이 "Success +1. +5It is maintained even if you fail up to." 가 됐다).
     *
     * 그래서 숫자를 [SLOT] 으로 지운 **문형**을 키로 찾는다. 번역문에는 같은 자리가
     * 비어 있으므로 원래 숫자를 순서대로 돌려놓으면 어순이 그 언어의 것으로 남는다.
     * 밸런스 상수가 바뀌어도 문형은 그대로라 사전이 낡지 않는다.
     */
    private fun byPattern(source: String): String? {
        if (!NUMBER.containsMatchIn(source)) return null
        val numbers = NUMBER.findAll(source).map { it.value }.toList()
        val target = exact[NUMBER.replace(source, SLOT)] ?: return null
        var index = 0
        return SLOT_PATTERN.replace(target) { numbers.getOrElse(index++) { "" } }
    }

    companion object {
        private const val CACHE_LIMIT = 2_048
        private val HANGUL = Regex("[\\uAC00-\\uD7A3]")

        /** 문형 키에서 숫자가 있던 자리. */
        const val SLOT = "{}"
        private val SLOT_PATTERN = Regex("\\{\\}")

        /** 자릿수 구분(1,000)과 소수점(1.5)까지 한 덩어리로 본다. */
        private val NUMBER = Regex("\\d[\\d,]*(?:\\.\\d+)?")

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
