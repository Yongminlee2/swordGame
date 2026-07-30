package com.geomgang.core

/**
 * 고유검 레시피 하나.
 *
 * @param needs    재료 조건 목록: (계열, 최소 단계, 자루 수). 계열 null = 아무 검.
 * @param essences 구역 정수 요구량 (Zone id → 개수)
 * @param hint     도감의 미발견 힌트 한 줄
 */
data class UniqueRecipe(
    val id: String,
    val name: String,
    val hint: String,
    val resultFamily: WeaponFamily,
    val needs: List<Triple<WeaponFamily?, Int, Int>>,
    val essences: Map<String, Int> = emptyMap(),
    val blurb: String,
)

/**
 * 고유검.
 *
 * 조합(Fusion)의 확장이다. 재료 구성이 레시피와 맞으면 일반 결과 대신 고유검이 나온다.
 * 레시피는 화면에 보이지 않는다 - 도감의 힌트를 읽고 **발견**하는 재미가 목적이다.
 *
 * 패시브는 별도의 버프 저장 없이, 전투·강화 계산이 그때그때 이 객체에 물어본다.
 * 검이 곧 상태다.
 */
object UniqueSwords {

    /** 불사조가 파괴를 대신 받을 때 잃는 단계. */
    const val REVIVE_LEVEL_LOSS = 3

    /**
     * 레시피 6종. 전부 **재료 두 자루**다 — 조합이 두 자루라서.
     *
     * **정의 순서가 우선순위다.** 조건이 좁은 것(단계 하한·정수)이 앞이다.
     * 심연(마검+16 둘)이 탐식자(마검 아무 둘)보다 앞에 있어야 +16 둘이 탐식자로 새지 않는다.
     *
     * v2.1에서 숨긴 계열을 쓰는 4종(용왕의 송곳니·절단자·행운아·개화)을 내렸다.
     * git 이력에 있다 — 계열을 되살릴 때 함께 되살린다.
     */
    val RECIPES: List<UniqueRecipe> = listOf(
        UniqueRecipe(
            id = "abyss_eater", name = "심연을 삼킨 검",
            hint = "가장 어두운 것 둘을 깊이 벼려, 심연의 정수에 담그면…",
            resultFamily = WeaponFamily.DEMON,
            needs = listOf(Triple(WeaponFamily.DEMON, 16, 2)),
            essences = mapOf("abyss" to 5),
            blurb = "적 최대체력의 2%를 추가로 벤다",
        ),
        UniqueRecipe(
            id = "phoenix", name = "불사조",
            hint = "성과 마가 화산의 불길 속에서 하나가 되면…",
            resultFamily = WeaponFamily.HOLY,
            needs = listOf(
                Triple(WeaponFamily.HOLY, 12, 1),
                Triple(WeaponFamily.DEMON, 12, 1),
            ),
            essences = mapOf("volcano" to 3),
            blurb = "파괴될 때 한 번, 재가 되는 대신 되살아난다",
        ),
        UniqueRecipe(
            id = "trinity", name = "삼위일체",
            hint = "성스러운 것 둘이 한 곳에 모이면…",
            resultFamily = WeaponFamily.HOLY,
            needs = listOf(Triple(WeaponFamily.HOLY, 10, 2)),
            blurb = "보스에게 40% 더 아프다",
        ),
        UniqueRecipe(
            id = "tempest", name = "폭풍우",
            hint = "가장 빠른 것 둘과 설원의 정수…",
            resultFamily = WeaponFamily.RAPIER,
            needs = listOf(Triple(WeaponFamily.RAPIER, 0, 2)),
            essences = mapOf("snowfield" to 2),
            blurb = "손이 30% 더 빨라진다",
        ),
        UniqueRecipe(
            id = "origin", name = "시작의 검",
            hint = "처음의 검 둘이 처음으로 돌아가면…",
            resultFamily = WeaponFamily.STRAIGHT,
            needs = listOf(Triple(WeaponFamily.STRAIGHT, 0, 2)),
            blurb = "장착 중 강화 성공률 +3%p",
        ),
        // 탐식자는 마검 아무 둘 - 심연(+16 하한)보다 뒤라야 +16 둘이 여기로 새지 않는다
        UniqueRecipe(
            id = "glutton", name = "탐식자",
            hint = "굶주린 마검 둘이 서로를 집어삼키면…",
            resultFamily = WeaponFamily.DEMON,
            needs = listOf(Triple(WeaponFamily.DEMON, 0, 2)),
            blurb = "조각을 2배로 챙긴다",
        ),
    )

    private val byId = RECIPES.associateBy { it.id }

    fun of(sword: Sword?): UniqueRecipe? = sword?.uniqueId?.let { byId[it] }

    fun byId(id: String): UniqueRecipe? = byId[id]

    /**
     * 재료·정수가 어느 레시피와 맞는지. 첫 번째로 맞는 레시피가 답이다(정의 순서 = 우선순위).
     *
     * 매칭 규칙: 구체 계열 조건부터 재료를 배정하고, 남은 재료가 "아무 검" 조건을 채운다.
     * 재료 수는 정확히 조건 합과 같아야 한다 - 다섯 자루로 네 자루 레시피를 만들 수 없다.
     */
    fun match(materials: List<Sword>, essences: Map<String, Int>): UniqueRecipe? {
        for (recipe in RECIPES) {
            if (matches(recipe, materials, essences)) return recipe
        }
        return null
    }

    private fun matches(
        recipe: UniqueRecipe,
        materials: List<Sword>,
        essences: Map<String, Int>,
    ): Boolean {
        // 고유검은 재료가 될 수 없다
        if (materials.any { it.uniqueId != null }) return false
        if (materials.size != recipe.needs.sumOf { it.third }) return false
        for ((zoneId, count) in recipe.essences) {
            if ((essences[zoneId] ?: 0) < count) return false
        }

        val remaining = materials.toMutableList()
        // 구체 계열 조건 먼저
        for ((family, minLevel, count) in recipe.needs.filter { it.first != null }) {
            repeat(count) {
                val idx = remaining.indexOfFirst { it.family == family && it.level >= minLevel }
                if (idx < 0) return false
                remaining.removeAt(idx)
            }
        }
        // 남은 재료가 "아무 검" 조건을 채운다
        for ((_, minLevel, count) in recipe.needs.filter { it.first == null }) {
            repeat(count) {
                val idx = remaining.indexOfFirst { it.level >= minLevel }
                if (idx < 0) return false
                remaining.removeAt(idx)
            }
        }
        return remaining.isEmpty()
    }

    // --- 패시브. 고유검이 아니면 전부 중립값이다. ---

    fun bossBonusOf(sword: Sword?): Double = if (sword?.uniqueId == "trinity") 1.4 else 1.0

    fun shardMultOf(sword: Sword?): Double = if (sword?.uniqueId == "glutton") 2.0 else 1.0

    fun burnMultOf(sword: Sword?): Double = if (sword?.uniqueId == "dragon_fang") 3.0 else 1.0

    fun dropMultOf(sword: Sword?): Double = if (sword?.uniqueId == "lucky") 2.0 else 1.0

    fun critBonusOf(sword: Sword?): Double = if (sword?.uniqueId == "cleaver") 0.10 else 0.0

    fun tapIntervalMultOf(sword: Sword?): Double =
        if (sword?.uniqueId == "tempest") 0.7 else 1.0

    fun maxHpRatioOf(sword: Sword?): Double =
        if (sword?.uniqueId == "abyss_eater") 0.02 else 0.0

    fun goldMultOf(sword: Sword?): Double = if (sword?.uniqueId == "bloom") 1.5 else 1.0

    fun forgeBonusOf(sword: Sword?): Double = if (sword?.uniqueId == "origin") 0.03 else 0.0

    fun canRevive(sword: Sword?): Boolean = sword?.uniqueId == "phoenix"

    /**
     * 고유검 한 종류를 발견한 값. 0.005 는 0.5%p 다.
     *
     * v2.1에서 10종→6종이 되면서 0.3%p 그대로면 만점이 1.8%p 로 줄어든다.
     * 한 종의 값을 올려 만점 +3.0%p 를 유지한다.
     */
    const val PER_UNIQUE: Double = 0.005

    /**
     * 발견한 고유검 수로 정해지는 보너스.
     *
     * **들고 있을 필요가 없다** — 한 번 만들어 봤다는 사실 자체가 대장장이의 실력이다.
     * 모르는 id 는 세지 않는다(옛 세이브나 손댄 세이브 대비).
     */
    fun holdingBonus(progress: ProgressState): ForgeBonus {
        val known = progress.uniqueFound.count { byId(it) != null }
        val value = PER_UNIQUE * known
        return ForgeBonus(successRate = value, destroyGuard = value)
    }
}
