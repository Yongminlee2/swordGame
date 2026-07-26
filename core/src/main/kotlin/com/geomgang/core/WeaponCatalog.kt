package com.geomgang.core

/**
 * 강화 단계에 따라 결정되는 검의 외형 등급.
 *
 * 구간은 [minLevel]..[maxLevel] 이며 빈틈 없이 이어진다.
 * 마지막 티어의 [maxLevel]은 [Int.MAX_VALUE]로 열려 있다.
 */
enum class WeaponTier(
    val id: String,
    val displayName: String,
    val minLevel: Int,
    val maxLevel: Int,
    val endlessOnly: Boolean,
) {
    RUSTY("rusty", "녹슨 검", 0, 2, false),
    STEEL("steel", "강철검", 3, 5, false),
    SILVER("silver", "은장검", 6, 8, false),
    RUNE("rune", "룬검", 9, 11, false),
    FLAME("flame", "화염검", 12, 14, false),
    THUNDER("thunder", "뇌전검", 15, 16, false),
    DAWN("dawn", "여명의 성검", 17, 18, false),
    BLACK_DRAGON("black_dragon", "흑룡참", 19, 20, false),
    DRAGON_SCALE("dragon_scale", "용린참", 21, 25, true),
    ABYSS("abyss", "심연검", 26, 30, true),
    NAMELESS("nameless", "이름 없는 검", 31, Int.MAX_VALUE, true),
}

/** 도감의 칸 하나. 계열 × 티어 조합이다. */
data class CodexEntry(val family: WeaponFamily, val tier: WeaponTier)

/** 무기 외형 정의와 도감 구성. 확률·경제와는 무관하다. */
object WeaponCatalog {

    /** 이 단계부터 검에 오라 레이어를 그린다. */
    const val AURA_MIN_LEVEL: Int = 15

    /** 해당 강화 단계의 외형 티어. */
    fun tierFor(level: Int): WeaponTier {
        require(level >= 0) { "level must be >= 0, was $level" }
        return WeaponTier.entries.first { level >= it.minLevel && level <= it.maxLevel }
    }

    /** 이 티어를 획득할 수 있는 난이도들. */
    fun difficultiesFor(tier: WeaponTier): List<Difficulty> =
        if (tier.endlessOnly) listOf(Difficulty.ENDLESS) else Difficulty.entries.toList()

    /** 도감 전체 칸. 계열 8종 × 티어 11종 = 88칸. */
    val ENTRIES: List<CodexEntry> =
        WeaponFamily.entries.flatMap { family ->
            WeaponTier.entries.map { tier -> CodexEntry(family, tier) }
        }
}
