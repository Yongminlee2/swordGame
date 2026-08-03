package com.geomgang.core

/** 조합소 교환의 보상. */
sealed interface RecipeReward {
    data class GrantItem(val item: Item, val count: Int) : RecipeReward
    data class GrantSword(val level: Int) : RecipeReward
    data class GrantStone(val count: Int) : RecipeReward
}

/** 조각으로 무언가를 바꾸는 교환식 하나. */
data class Recipe(
    val id: String,
    val displayName: String,
    val shardCost: Int,
    val reward: RecipeReward,
)

/**
 * 조합소.
 *
 * 조각은 골드와 분리된 화폐다. 골드가 바닥나도 주워 모은 조각으로 재기할 수 있어야
 * 파괴가 곧 게임 종료가 되지 않는다.
 */
object Recipes {

    /** +5 워프권 교환가. [Economy.needsBailout]이 파산 판정 기준으로 함께 쓴다. */
    const val SWORD5_SHARD_COST: Int = 50

    /** 조각을 강화석으로 바꾸는 값. 강화석은 고단계 강화의 화폐다. */
    const val STONE_SHARD_COST: Int = 20

    /**
     * 워프권 값 — **파괴 몇 번치인가**로 읽는다.
     *
     * 줍기 회수량은 단계×3([ForgeEngine.SALVAGE_MULTIPLIER])이라 +10~15 파괴 한 번이
     * 30~45조각이다. 그러니 +5 는 파괴 두 번, +10 은 다섯 번, +15 는 열 번쯤이다.
     *
     * 120/400/850 이었는데 **너무 비쌌다.** 파괴는 +10~15 에서 10~12.5% 로만 나므로
     * 파괴 한 번을 겪는 데도 시도 수백 번이 든다 — 850조각은 워프권을 재기의 도구가
     * 아니라 거의 못 사는 사치품으로 만들었다.
     *
     * **아래로도 선이 있다.** 어떤 워프권도 그 단계 파괴 한 번의 조각보다 싸면 안 된다
     * (+10 파괴 최대 39조각 < +5 워프 50). 싸지면 파괴→줍기→워프가 순환 이득이 된다.
     *
     * 시즌2 소모품(방지권 10·축복서 30·부적 60)과도 자릿수를 맞췄다 —
     * 워프 +10(150)이 부적 두 장 반쯤이다. 한쪽만 헐값이면 조각의 뜻이 흐려진다.
     */
    val ALL: List<Recipe> = listOf(
        Recipe("stone", "강화석", STONE_SHARD_COST, RecipeReward.GrantStone(1)),
        Recipe("prevent", "방지권", 10, RecipeReward.GrantItem(Item.PREVENT_TICKET, 1)),
        Recipe("blessing", "축복서", 30, RecipeReward.GrantItem(Item.BLESSING_SCROLL, 1)),
        Recipe("luck", "행운부적", 60, RecipeReward.GrantItem(Item.LUCK_CHARM, 1)),
        Recipe("sword5", "워프권 +5", SWORD5_SHARD_COST, RecipeReward.GrantSword(5)),
        Recipe("sword10", "워프권 +10", 150, RecipeReward.GrantSword(10)),
        Recipe("sword15", "워프권 +15", 250, RecipeReward.GrantSword(15)),
    )

    fun byId(id: String): Recipe =
        ALL.firstOrNull { it.id == id }
            ?: throw IllegalArgumentException("unknown recipe id: $id")

    /**
     * 지금 국면에서 열려 있는 교환. 시즌1(용검 이전)은 **워프권뿐이다.**
     *
     * 강화석은 시즌2 재료라 당연히 잠그고, 소모품(방지권 10조각 등)도 잠근다 —
     * 열어 두면 파괴 한 번(조각 30~60)이 방지권 서너 장이 되어, 소모품 골드값을
     * 10배 올려 세운 「파괴 구간은 벽」이 조각 뒷문으로 무너진다.
     */
    fun availableIn(deep: Boolean): List<Recipe> =
        if (deep) ALL else ALL.filter { it.reward is RecipeReward.GrantSword }

    fun availableIn(state: GameState): List<Recipe> =
        availableIn(Unlocks.legendReached(state))

    /**
     * 워프권을 **안 고르고** 샀을 때의 계열.
     *
     * 한때 이 함수가 유일한 길이었다. 그때는 계열이 확률에도 경제에도 관여하지 않아
     * 무엇을 고르든 결과가 같았고, 바뀌지 않는 선택은 손만 한 번 더 가게 했다.
     *
     * 지금은 계열마다 판매가([Economy.familyMult])·강화 특성([FamilyForge])·도감 칸이
     * 달라서 화면이 직접 고른다. 이 함수는 안 골랐을 때의 기본값으로 남는다 —
     * [incomplete](도감이 덜 찬 계열)를 먼저 주고, 없으면 열린 계열 전부에서 고른다.
     * 무작위지만 [roll] 을 밖에서 넣으므로 테스트가 결과를 고정할 수 있다.
     */
    fun familyFor(
        unlocked: List<WeaponFamily>,
        incomplete: Set<WeaponFamily>,
        roll: Int,
    ): WeaponFamily {
        require(unlocked.isNotEmpty()) { "no unlocked family to grant" }
        val pool = unlocked.filter { it in incomplete }.ifEmpty { unlocked }
        return pool[roll.mod(pool.size)]
    }

    fun canCraft(state: GameState, recipe: Recipe): Boolean {
        // 시즌 게이트는 여기서 지킨다 - UI만 가리면 시뮬레이터·저장 복구 경로가 새 나간다.
        if (recipe !in availableIn(state)) return false
        if (state.shards < recipe.shardCost) return false
        // 검을 주는 교환은 빈손일 때만 가능하다. 들고 있는 검을 덮어쓰지 않는다.
        if (recipe.reward is RecipeReward.GrantSword && state.sword != null) return false
        return true
    }

    /**
     * 한 번에 바꿀 수 있는 최대 개수.
     *
     * 강화석은 고단계에서 한 번에 열댓 개씩 든다. 한 번에 하나씩 누르게 두면
     * **바꾸는 일이 노가다가 된다** — 고민할 거리가 아니라 손가락 문제다.
     *
     * 검은 예외로 언제나 하나다. 손은 하나뿐이라 두 자루를 동시에 받을 수 없다.
     */
    fun maxCraftable(state: GameState, recipe: Recipe): Int {
        if (!canCraft(state, recipe)) return 0
        if (recipe.reward is RecipeReward.GrantSword) return 1
        return state.shards / recipe.shardCost
    }

    /**
     * 여러 개를 한 번에 바꾼다.
     *
     * [count] 가 [maxCraftable] 을 넘으면 살 수 있는 만큼만 바꾼다 — 화면이 계산을
     * 다시 하지 않아도 되게 여기서 잘라 준다.
     */
    fun craftMany(
        state: GameState,
        recipe: Recipe,
        count: Int,
        family: WeaponFamily,
    ): GameState {
        val times = count.coerceAtMost(maxCraftable(state, recipe))
        if (times <= 0) return state
        var next = state
        repeat(times) { next = craft(next, recipe, family) }
        return next
    }

    fun craft(state: GameState, recipe: Recipe, family: WeaponFamily): GameState {
        check(canCraft(state, recipe)) { "cannot craft ${recipe.id} in this state" }
        val paid = state.copy(shards = state.shards - recipe.shardCost)
        return when (val reward = recipe.reward) {
            is RecipeReward.GrantItem ->
                paid.copy(inventory = paid.inventory.plus(reward.item, reward.count))

            is RecipeReward.GrantSword ->
                paid.copy(
                    sword = Sword(family, reward.level),
                    bestLevel = maxOf(paid.bestLevel, reward.level),
                )

            is RecipeReward.GrantStone ->
                paid.copy(forgeStones = paid.forgeStones + reward.count)
        }
    }
}
