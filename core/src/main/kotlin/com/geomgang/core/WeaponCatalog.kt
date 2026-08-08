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

/**
 * 도감의 칸 하나. **그림 한 장에 칸 하나다.**
 *
 * 예전에는 티어가 칸이었다. 그런데 그림은 강화 한 단계마다 바뀌는데 티어는 열한 구간뿐이라
 * 「녹슨 검」 한 칸이 +0·+1·+2 세 그림을 덮고 「이름 없는 검」 한 칸이 +31 위 전부를 덮었다.
 * 그림 절반이 도감에 자리가 없었던 셈이다.
 *
 * [family] 가 null 이면 전설 그림이다 — [WeaponCatalog.FAMILY_MAX_LEVEL] 위는 계열과
 * 무관하게 같은 그림을 쓰므로 칸도 계열마다 두지 않는다.
 */
data class CodexEntry(val family: WeaponFamily?, val level: Int)

/** 무기 외형 정의와 도감 구성. 확률·경제와는 무관하다. */
object WeaponCatalog {

    /** 이 단계부터 검에 오라 레이어를 그린다. */
    const val AURA_MIN_LEVEL: Int = 15

    /** 계열 고유 그림이 덮는 마지막 단계. 그 위는 전설 그림이다. */
    const val FAMILY_MAX_LEVEL: Int = 20

    /**
     * 전설 그림이 덮는 마지막 단계. 그 위는 마지막 그림을 계속 쓴다.
     *
     * +21~+39 는 시트의 전설 칸, +40~+50 은 단계마다 낱장 그림이다
     * ([com.geomgang.game.ui.LegendArt]). 도감은 그 둘을 구분하지 않는다 —
     * **그림 한 장에 칸 하나**라는 규칙만 지킨다.
     */
    const val LEGEND_MAX_LEVEL: Int = 50

    /** 해당 강화 단계의 외형 티어. 이름과 오라가 이 값을 쓴다. */
    fun tierFor(level: Int): WeaponTier {
        require(level >= 0) { "level must be >= 0, was $level" }
        return WeaponTier.entries.first { level >= it.minLevel && level <= it.maxLevel }
    }

    /** 이 티어를 획득할 수 있는 난이도들. */
    fun difficultiesFor(tier: WeaponTier): List<Difficulty> =
        if (tier.endlessOnly) listOf(Difficulty.ENDLESS) else Difficulty.entries.toList()

    /** 이 단계가 계열 고유 그림을 쓰는지. */
    fun isFamilyArt(level: Int): Boolean = level <= FAMILY_MAX_LEVEL

    /**
     * 이 검이 채우는 도감 칸.
     *
     * +41 이상은 그림이 더 없으므로 마지막 전설 칸으로 모인다 — 상한 없는 구간을
     * 칸으로 좇을 수는 없다.
     */
    fun slotFor(family: WeaponFamily, level: Int): CodexEntry {
        require(level >= 0) { "level must be >= 0, was $level" }
        return if (isFamilyArt(level)) {
            CodexEntry(family, level)
        } else {
            CodexEntry(null, level.coerceAtMost(LEGEND_MAX_LEVEL))
        }
    }

    /**
     * 이 계열이 도감에 가지는 단계 칸.
     *
     * 조합검(마검·성검·용검)은 **+1 이 시작이다** — 상점에 나오지 않고 조합으로만
     * 나오는데 [Refinery]·[LegendForge] 둘 다 +1 을 내놓는다. 하락으로도 못 닿는다:
     * +1~+5 는 안전구간([RateTable.SAFE_BAND_END])이라 떨어지지 않고, 하락이 시작되는
     * +6 에서 미끄러져도 +5 가 바닥이다. 그래서 +0 칸을 두면 **영원히 못 채우는 칸**이 된다.
     */
    fun levelsFor(family: WeaponFamily): IntRange =
        if (family in LegendForge.REFINED_FAMILIES) 1..FAMILY_MAX_LEVEL else 0..FAMILY_MAX_LEVEL

    /** 전설 칸이 덮는 단계. */
    val LEGEND_LEVELS: IntRange = (FAMILY_MAX_LEVEL + 1)..LEGEND_MAX_LEVEL

    /**
     * 도감 전체 칸.
     *
     * 기본 4계열 × 21(+0~+20) = 84, 조합검 3계열 × 20(+1~+20) = 60, 전설 30(+21~+50).
     * 합쳐서 174 다. 숨긴 계열의 칸은 여기 없다 — 옛 세이브가 이미 채운 칸은
     * 지우지 않되 세지도 않는다.
     */
    val ENTRIES: List<CodexEntry> =
        WeaponFamily.CODEX_FAMILIES.flatMap { family ->
            levelsFor(family).map { level -> CodexEntry(family, level) }
        } + LEGEND_LEVELS.map { level -> CodexEntry(null, level) }
}
