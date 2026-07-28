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

    /** +5 검 교환가. [Economy.needsBailout]이 파산 판정 기준으로 함께 쓴다. */
    const val SWORD5_SHARD_COST: Int = 120

    /** 조각을 강화석으로 바꾸는 값. 강화석은 고단계 강화의 화폐다. */
    const val STONE_SHARD_COST: Int = 20

    val ALL: List<Recipe> = listOf(
        Recipe("stone", "강화석", STONE_SHARD_COST, RecipeReward.GrantStone(1)),
        Recipe("prevent", "방지권", 10, RecipeReward.GrantItem(Item.PREVENT_TICKET, 1)),
        Recipe("blessing", "축복서", 30, RecipeReward.GrantItem(Item.BLESSING_SCROLL, 1)),
        Recipe("luck", "행운부적", 60, RecipeReward.GrantItem(Item.LUCK_CHARM, 1)),
        Recipe("sword5", "+5 검", SWORD5_SHARD_COST, RecipeReward.GrantSword(5)),
        Recipe("sword10", "+10 검", 400, RecipeReward.GrantSword(10)),
    )

    fun byId(id: String): Recipe =
        ALL.firstOrNull { it.id == id }
            ?: throw IllegalArgumentException("unknown recipe id: $id")

    /**
     * 교환으로 받는 검의 계열.
     *
     * 예전에는 화면이 직접 고르게 했다. 그런데 계열은 확률에도 경제에도 전혀
     * 관여하지 않아 **무엇을 고르든 결과가 같다.** 결과가 바뀌지 않는 선택은
     * 손만 한 번 더 가게 한다.
     *
     * 그래서 고르는 대신 [incomplete] — 도감이 덜 찬 계열 — 을 먼저 준다.
     * 남은 게 없으면 열린 계열 전부에서 고른다. 무작위지만 [roll] 을 밖에서
     * 넣으므로 테스트가 결과를 고정할 수 있다.
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
        if (state.shards < recipe.shardCost) return false
        // 검을 주는 교환은 빈손일 때만 가능하다. 들고 있는 검을 덮어쓰지 않는다.
        if (recipe.reward is RecipeReward.GrantSword && state.sword != null) return false
        return true
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
