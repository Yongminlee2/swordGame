package com.geomgang.core

/** 시즌3 전용 파괴 복구권 상점 규칙. */
object LegendProtection {
    const val GOLD_PRICE: Long = 100_000_000L
    const val SHARD_PRICE: Int = 10_000

    fun canBuyWithGold(state: GameState): Boolean =
        Unlocks.season(state) == GameSeason.LEGEND &&
            state.pendingDestroy == null &&
            state.gold >= GOLD_PRICE

    fun canBuyWithShards(state: GameState): Boolean =
        Unlocks.season(state) == GameSeason.LEGEND &&
            state.pendingDestroy == null &&
            state.shards >= SHARD_PRICE

    fun buyWithGold(state: GameState): GameState {
        check(canBuyWithGold(state)) { "cannot buy legend protection with gold" }
        return state.copy(
            gold = state.gold - GOLD_PRICE,
            inventory = state.inventory.copy(
                legendPreventTickets = state.inventory.legendPreventTickets + 1,
            ),
        )
    }

    fun buyWithShards(state: GameState): GameState {
        check(canBuyWithShards(state)) { "cannot buy legend protection with shards" }
        return state.copy(
            shards = state.shards - SHARD_PRICE,
            inventory = state.inventory.copy(
                legendPreventTickets = state.inventory.legendPreventTickets + 1,
            ),
        )
    }
}
