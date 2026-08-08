package com.geomgang.core

/**
 * 전설검 등급.
 *
 * 계열은 +20 에서 끝난다. 그 위는 강화로 가지 않고 **조합으로만** 간다 - 단,
 * 용검(DRAGON) 자신은 예외다([canForge]). 확률표가 이미 [RateTable.MAX_FINITE_LEVEL]
 * 20 이고 아트도 계열마다 +0~+20 한 벌씩 있어 용검도 이미 그 한 벌을 쓴다.
 */
object LegendForge {

    /**
     * 재료를 처음 조합했을 때 손에 쥐는 단계.
     *
     * 마검·성검이 +1 부터 다시 오르듯 용검도 +1 부터 오른다(v2.5) - 전에는 곧장
     * +21 로 떴는데, 갓 만든 검치고 낯선 숫자였다. +1~+20 은 용검도 다른 계열과
     * 똑같이 [FamilyForge.DRAGON]·[FamilyStyle.BURNING]·시트3 아트를 그대로 쓴다 -
     * 여태 아무도 그 단계의 용검을 만들 수 없었을 뿐이다.
     */
    const val CRAFT_LEVEL: Int = 1

    /**
     * 전설 등급이 시작되는 단계 - 파괴돼도 여기로 돌아오고([ForgeEngine]),
     * 담금질도 여기부터 붙는다(별강화는 계열로 갈린다 — [StarForce.starrable]). 다시 벼릴 때도
     * ([recraft]) 곧장 여기로 온다 - 한 번 넘은 벽을 두 번 넘으라 하지 않는다.
     */
    const val LEVEL: Int = RateTable.MAX_FINITE_LEVEL + 1

    /** 재료가 갖춰야 하는 단계. */
    const val MATERIAL_LEVEL: Int = RateTable.MAX_FINITE_LEVEL

    /** 해금 뒤 다시 벼리는 조각 값. */
    const val RECRAFT_SHARDS: Int = 500

    /**
     * 재료 둘 — 마검과 성검, 각각 +20.
     *
     * v2.1에서 넷(용검·정령검·합검·허검)에서 줄었다. 노출 계열이 일곱뿐이라
     * 사다리가 기본 4 → 마검·성검 → 용검(전설)으로 곧게 선다.
     * 어둠과 빛을 각각 끝까지 벼려야 용이 된다.
     */
    val MATERIALS: List<WeaponFamily> = listOf(
        WeaponFamily.DEMON,
        WeaponFamily.HOLY,
    )

    /**
     * 파괴되지 않고 바닥으로 떨어지는 계열 — 마검·성검·용검, 전부 조합으로만 얻는다.
     *
     * [ForgeEngine.survivesDestroy] 와 보관함 확인창이 함께 쓴다 — 두 군데서 각자
     * 계열을 나열하면 하나를 놓치기 쉽다. 용검은 +21 위에서는 [Sword.isLegend] 가
     * 먼저 걸리므로, 여기 있는 건 +20 이하로 오르는 동안의 몫이다.
     */
    val REFINED_FAMILIES: Set<WeaponFamily> = (MATERIALS + WeaponFamily.DRAGON).toSet()

    /**
     * 이 검을 더 강화할 수 있는지. **계열은 +20 에서 멈춘다** - 용검만 예외다.
     *
     * 용검은 조합으로만 얻는 유일한 계열이라, +20 벽 자체가 없다 - +1 부터
     * 곧장 무한 구간까지 이어진다.
     */
    fun canForge(sword: Sword): Boolean =
        sword.family == WeaponFamily.DRAGON || sword.isLegend() || sword.level < MATERIAL_LEVEL

    /** 재료로 쓸 수 있는 검인지. 고유검은 녹이지 않는다. */
    private fun usable(sword: Sword): Boolean =
        sword.level >= MATERIAL_LEVEL && sword.uniqueId == null && !sword.isLegend()

    /**
     * 아직 없는 재료.
     *
     * 모자라도 **무엇이 필요한지 늘 보여 주기 위한 것**이다. 목표가 보여야 모으고 싶어진다.
     */
    fun missingFor(state: GameState): List<WeaponFamily> {
        val have = state.storage.filter(::usable).map { it.family }.toMutableList()
        return MATERIALS.filterNot { have.remove(it) }
    }

    fun canCraft(state: GameState, progress: ProgressState): Boolean =
        state.sword == null && state.pendingDestroy == null && missingFor(state).isEmpty()

    /** 재료 둘을 태우고 용검 +1을 손에 쥔다. 마검·성검과 같은 조합의 연장이다. */
    fun craft(state: GameState, progress: ProgressState): Pair<GameState, ProgressState> {
        check(canCraft(state, progress)) { "cannot craft a legend in this state" }
        // 재료마다 한 자루씩만 태운다. 같은 계열이 여러 자루면 나머지는 남는다.
        val toBurn = MATERIALS.toMutableList()
        val left = state.storage.filterNot { sword ->
            usable(sword) && toBurn.remove(sword.family)
        }
        val next = state.copy(
            // 재료가 무엇이든 나오는 것은 용검 +1 하나다. 전설(+21)까지는 다시 오른다.
            sword = Sword(WeaponFamily.DRAGON, CRAFT_LEVEL),
            storage = left,
        )
        return next to progress
    }

    /**
     * 해금 뒤 조각으로 다시 벼린다.
     *
     * **+21 의 벽은 게임에서 가장 높은 벽인데, 그 벽을 넘은 사람에게 다시 넘으라고 하면
     * 아무도 두 번째 도전을 하지 않는다.**
     */
    fun canRecraft(state: GameState, progress: ProgressState): Boolean =
        progress.legendUnlocked &&
            state.sword == null &&
            state.pendingDestroy == null &&
            state.shards >= RECRAFT_SHARDS

    fun recraft(state: GameState): GameState = state.copy(
        sword = Sword(WeaponFamily.DRAGON, LEVEL),
        shards = state.shards - RECRAFT_SHARDS,
        bestLevel = maxOf(state.bestLevel, LEVEL),
    )

    /** 도감에 바친 검이 전설검이면 해금이 남는다. */
    fun onOffered(progress: ProgressState, sword: Sword): ProgressState =
        if (sword.isLegend()) progress.copy(legendUnlocked = true) else progress
}
