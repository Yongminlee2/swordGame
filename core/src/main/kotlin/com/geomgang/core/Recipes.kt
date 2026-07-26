package com.geomgang.core

/** 조합소 교환의 보상. */
sealed interface RecipeReward {
    data class GrantItem(val item: Item, val count: Int) : RecipeReward
    data class GrantSword(val level: Int) : RecipeReward
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

    val ALL: List<Recipe> = listOf(
        Recipe("prevent", "방지권", 10, RecipeReward.GrantItem(Item.PREVENT_TICKET, 1)),
        Recipe("blessing", "축복서", 30, RecipeReward.GrantItem(Item.BLESSING_SCROLL, 1)),
        Recipe("luck", "행운부적", 60, RecipeReward.GrantItem(Item.LUCK_CHARM, 1)),
        Recipe("sword5", "+5 검", SWORD5_SHARD_COST, RecipeReward.GrantSword(5)),
        Recipe("sword10", "+10 검", 400, RecipeReward.GrantSword(10)),
    )

    fun byId(id: String): Recipe =
        ALL.firstOrNull { it.id == id }
            ?: throw IllegalArgumentException("unknown recipe id: $id")

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
        }
    }
}
