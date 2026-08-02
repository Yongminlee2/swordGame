package com.geomgang.core

/**
 * 조합 한 줄 — **+20 두 자루를 태워 새 계열의 +1 을 얻는다.**
 *
 * v2.3 전에는 아무 두 자루를 섞어 평균 단계가 나왔다. 그러면 조합이 "남는 검 처리"
 * 가 되어 계열을 넘는 일이 가벼워진다. 이제 조합은 **끝까지 올린 두 자루를 바치는
 * 의식**이다 — 마검을 쥐려면 직검과 곡도를 각각 +20 까지 올려야 하고,
 * 그 마검도 +1 부터 다시 시작한다. 시즌1이 길어야 한다는 요구가 여기 산다.
 */
data class RefineRecipe(
    val materials: List<WeaponFamily>,
    val result: WeaponFamily,
    val resultLevel: Int,
)

object Refinery {

    /** 재료가 갖춰야 하는 단계. 계열의 끝이다. */
    const val MATERIAL_LEVEL: Int = RateTable.MAX_FINITE_LEVEL

    /**
     * 조합표. 용검(+21)은 여기 없다 — [LegendForge] 전용 칸이다.
     * 결과가 +1 인 이유: 새 계열은 새 등반이다. 평균으로 물려받으면 등반이 사라진다.
     */
    val RECIPES: List<RefineRecipe> = listOf(
        RefineRecipe(
            listOf(WeaponFamily.STRAIGHT, WeaponFamily.CURVED),
            WeaponFamily.DEMON,
            resultLevel = 1,
        ),
        RefineRecipe(
            listOf(WeaponFamily.GREAT, WeaponFamily.RAPIER),
            WeaponFamily.HOLY,
            resultLevel = 1,
        ),
    )

    /** 재료로 쓸 수 있는 검인지. 고유검·전설검은 태우지 않는다. */
    private fun usable(sword: Sword): Boolean =
        sword.level >= MATERIAL_LEVEL && sword.uniqueId == null && !sword.isLegend()

    /** 아직 없는 재료. 모자라도 무엇이 필요한지 늘 보여 주기 위한 것이다. */
    fun missingFor(state: GameState, recipe: RefineRecipe): List<WeaponFamily> {
        val have = state.storage.filter(::usable).map { it.family }.toMutableList()
        return recipe.materials.filterNot { have.remove(it) }
    }

    /** 결과는 보관함으로 간다(둘 빠지고 하나 들어오니 자리는 늘 있다). 손은 안 비워도 된다. */
    fun canCraft(state: GameState, recipe: RefineRecipe): Boolean =
        state.pendingDestroy == null && missingFor(state, recipe).isEmpty()

    fun craft(state: GameState, recipe: RefineRecipe): GameState {
        check(canCraft(state, recipe)) { "cannot refine ${recipe.result} in this state" }
        val toBurn = recipe.materials.toMutableList()
        val left = state.storage.filterNot { sword ->
            usable(sword) && toBurn.remove(sword.family)
        }
        return state.copy(storage = left + Sword(recipe.result, recipe.resultLevel))
    }
}
