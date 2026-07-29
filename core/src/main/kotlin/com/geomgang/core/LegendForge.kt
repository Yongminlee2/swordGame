package com.geomgang.core

/**
 * 전설검 등급.
 *
 * 계열은 +20 에서 끝난다. 그 위는 강화로 가지 않고 **조합으로만** 간다.
 * 확률표가 이미 [RateTable.MAX_FINITE_LEVEL] 20 이고 아트도 계열 +0~+20 / 전설 공용이라
 * 구조는 이미 여기에 맞춰져 있다.
 */
object LegendForge {

    /** 전설검이 시작되는 단계. */
    const val LEVEL: Int = RateTable.MAX_FINITE_LEVEL + 1

    /** 재료가 갖춰야 하는 단계. */
    const val MATERIAL_LEVEL: Int = RateTable.MAX_FINITE_LEVEL

    /** 해금 뒤 다시 벼리는 조각 값. */
    const val RECRAFT_SHARDS: Int = 500

    /**
     * 재료 넷.
     *
     * 조합 나무의 3층 두 갈래(용검·정령검)와 폭넓음(합검), 그리고 허검이다.
     * 성격이 다 달라서 넷을 모았다는 것은 **전부 통달했다**는 뜻이 된다.
     */
    val MATERIALS: List<WeaponFamily> = listOf(
        WeaponFamily.DRAGON,
        WeaponFamily.SPIRIT,
        WeaponFamily.FUSED,
        WeaponFamily.VOID,
    )

    /** 이 검을 더 강화할 수 있는지. **계열은 +20 에서 멈춘다.** */
    fun canForge(sword: Sword): Boolean =
        sword.isLegend() || sword.level < MATERIAL_LEVEL

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

    /** 재료 넷을 태우고 전설검을 손에 쥔다. */
    fun craft(state: GameState, progress: ProgressState): Pair<GameState, ProgressState> {
        check(canCraft(state, progress)) { "cannot craft a legend in this state" }
        // 재료마다 한 자루씩만 태운다. 같은 계열이 여러 자루면 나머지는 남는다.
        val toBurn = MATERIALS.toMutableList()
        val left = state.storage.filterNot { sword ->
            usable(sword) && toBurn.remove(sword.family)
        }
        val next = state.copy(
            sword = Sword(MATERIALS.first(), LEVEL),
            storage = left,
            bestLevel = maxOf(state.bestLevel, LEVEL),
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
        sword = Sword(MATERIALS.first(), LEVEL),
        shards = state.shards - RECRAFT_SHARDS,
        bestLevel = maxOf(state.bestLevel, LEVEL),
    )

    /** 도감에 바친 검이 전설검이면 해금이 남는다. */
    fun onOffered(progress: ProgressState, sword: Sword): ProgressState =
        if (sword.isLegend()) progress.copy(legendUnlocked = true) else progress
}
