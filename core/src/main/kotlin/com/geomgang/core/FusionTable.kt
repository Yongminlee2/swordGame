package com.geomgang.core

/**
 * 조합표의 한 줄.
 *
 * @param materials 필요한 재료의 **계열 집합**. 자루 수는 결과 단계에만 영향을 준다
 * @param result 나오는 계열
 * @param hint 도감에 보여 줄 설명. 조합표는 숨기지 않는다 — 계열을 모으는 길이 보여야
 *   조합소에 갈 이유가 생긴다
 */
data class FusionEntry(
    val materials: Set<WeaponFamily>,
    val result: WeaponFamily,
    val hint: String,
)

/**
 * 조합표 — 계열의 유일한 출처.
 *
 * 기본 4계열(직검·곡도·대검·세검)만 상점·드롭에 나오고, 나머지 10계열은
 * 조합이나 회랑 보상으로만 얻는다. 조합이 "더 높은 단계의 같은 검"을 만드는
 * 부수적 장치가 아니라 **계열을 넓히는 주된 수단**이 된다.
 *
 * 조회는 재료의 계열 집합으로 한다. 표에 없으면 [Fusion] 의 일반 규칙으로 넘어간다.
 * 우선순위: 고유검 레시피 → 이 표 → 일반 규칙.
 */
object FusionTable {

    val ALL: List<FusionEntry> = listOf(
        FusionEntry(setOf(WeaponFamily.STRAIGHT), WeaponFamily.TWIN, "직검 + 직검"),
        FusionEntry(
            setOf(WeaponFamily.STRAIGHT, WeaponFamily.GREAT),
            WeaponFamily.AXE,
            "직검 + 대검",
        ),
        FusionEntry(setOf(WeaponFamily.CURVED), WeaponFamily.SCYTHE, "곡도 + 곡도"),
        FusionEntry(setOf(WeaponFamily.RAPIER), WeaponFamily.SPEAR, "세검 + 세검"),
        FusionEntry(
            setOf(WeaponFamily.STRAIGHT, WeaponFamily.CURVED),
            WeaponFamily.DEMON,
            "직검 + 곡도",
        ),
        FusionEntry(
            setOf(WeaponFamily.GREAT, WeaponFamily.RAPIER),
            WeaponFamily.HOLY,
            "대검 + 세검",
        ),
        FusionEntry(
            setOf(WeaponFamily.DEMON, WeaponFamily.HOLY),
            WeaponFamily.DRAGON,
            "마검 + 성검",
        ),
        FusionEntry(
            setOf(WeaponFamily.SCYTHE, WeaponFamily.SPEAR),
            WeaponFamily.SPIRIT,
            "낫검 + 창검",
        ),
        // 허검은 무한 회랑 10층 돌파로도 얻지만 여기 조합으로도 얻는다.
        // 강화 게임인데 회랑 진도에 강제로 묶이면 전설검 재료로 쓸 수 없다.
        FusionEntry(
            setOf(WeaponFamily.AXE, WeaponFamily.SPEAR),
            WeaponFamily.VOID,
            "도끼검 + 창검",
        ),
    )

    private val byMaterials: Map<Set<WeaponFamily>, WeaponFamily> =
        ALL.associate { it.materials to it.result }

    /** 서로 다른 계열 넷을 한 번에 녹이면 합검이 된다. 개별 조합보다 뒤에 본다. */
    const val FUSED_DISTINCT_COUNT = 4

    /**
     * 재료 계열 집합으로 결과 계열을 찾는다. 표에 없으면 null.
     */
    fun resultFor(families: Set<WeaponFamily>): WeaponFamily? {
        byMaterials[families]?.let { return it }
        if (families.size >= FUSED_DISTINCT_COUNT) return WeaponFamily.FUSED
        return null
    }

    /** 이 계열을 만드는 조합. 없으면 null (기본 계열이거나 허검). */
    fun recipeFor(family: WeaponFamily): FusionEntry? = ALL.firstOrNull { it.result == family }
}
