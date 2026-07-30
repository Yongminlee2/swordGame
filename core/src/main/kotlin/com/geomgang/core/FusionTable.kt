package com.geomgang.core

/**
 * 조합표의 한 줄.
 *
 * @param materials 필요한 재료의 **계열 집합**
 * @param result 나오는 계열
 * @param hint 조합소에 보여 줄 설명. 조합표는 숨기지 않는다 - 계열을 모으는 길이
 *   보여야 조합소에 갈 이유가 생긴다
 */
data class FusionEntry(
    val materials: Set<WeaponFamily>,
    val result: WeaponFamily,
    val hint: String,
)

/**
 * 조합표 - 계열의 유일한 출처.
 *
 * v2.1에서 둘로 줄었다. 기본 4계열(직검·곡도·대검·세검)이 상점에 나오고,
 * 조합으로 마검·성검을 만들고, 그 둘 +20 을 조합소 전설 칸에서 합치면 용검(+21)이다.
 * 사다리 전체가 한 방향으로 흐른다: 기본 4 → 마검·성검 → 용검.
 *
 * **용검은 이 표에 없다.** 전설 칸([LegendForge]) 전용이라, 마검+성검을 일반
 * 조합으로 섞으면 불사조(고유검) 레시피만 판정된다 - 두 길이 겹치지 않는다.
 *
 * 숨긴 계열의 옛 항목(쌍검·낫검·창검·도끼검·정령검·합검·허검)은 git 이력에 있다.
 * 되살릴 때 여기 다시 넣으면 된다.
 */
object FusionTable {

    val ALL: List<FusionEntry> = listOf(
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
    )

    private val byMaterials: Map<Set<WeaponFamily>, WeaponFamily> =
        ALL.associate { it.materials to it.result }

    /** 이 계열 집합으로 만들어지는 계열. 표에 없으면 null - 조합 불가다. */
    fun resultFor(families: Set<WeaponFamily>): WeaponFamily? = byMaterials[families]

    /** 이 계열을 만드는 조합. 없으면 null (기본 계열이거나 전설). */
    fun recipeFor(family: WeaponFamily): FusionEntry? = ALL.firstOrNull { it.result == family }
}
