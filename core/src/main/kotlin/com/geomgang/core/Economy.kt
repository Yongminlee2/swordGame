package com.geomgang.core

import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * 돈의 흐름.
 *
 * 수입은 검 판매와 조각 조합, 지출은 강화 비용과 아이템 구매 두 갈래씩이다.
 * 판매가 지수가 비용 지수보다 크다는 것이 이 게임 경제의 핵심 불변식이며,
 * 그 덕분에 고단계 도전이 무모한 짓이 아니라 계산이 서는 도박이 된다.
 */
object Economy {

    /** 상점에서 파는 +0 검의 가격. */
    const val BASE_SWORD_PRICE: Long = 100

    const val PREVENT_TICKET_PRICE: Long = 800
    const val BLESSING_SCROLL_PRICE: Long = 1_200
    const val LUCK_CHARM_PRICE: Long = 2_000

    /** 파산 구제가 채워 주는 골드. */
    const val BAILOUT_GOLD: Long = 300

    private const val COST_BASE = 30.0
    private const val COST_GROWTH = 1.45

    private const val PRICE_BASE = 60.0

    /**
     * 판매가 증가율.
     *
     * 비용 증가율(1.45)과 비교해 정할 값이 아니다. 하락 구간에서 한 단계를 올리는 **기대** 비용은
     * 재시도(1/p)와 떨어졌다가 되돌아오는 비용까지 곱해져 1.45보다 훨씬 빠르게 커진다.
     * 시뮬레이션으로 재 보면 +0→+10 기대 비용이 약 11,850골드인데,
     * 1.6 배율일 때 +10 판매가는 6,597골드라 한 바퀴마다 5,000골드씩 손해였다.
     * 자본을 모을 수 없으니 플레이어가 영구 파산 상태로 저단계만 맴돌았다.
     */
    private const val PRICE_GROWTH = 1.80

    /** [currentLevel] 검을 한 단계 올리는 데 드는 비용. */
    fun upgradeCost(currentLevel: Int): Long {
        require(currentLevel >= 0) { "currentLevel must be >= 0, was $currentLevel" }
        return (COST_BASE * COST_GROWTH.pow(currentLevel.toDouble())).roundToLong()
    }

    /** [level] 검을 팔았을 때 받는 골드. */
    fun sellPrice(level: Int): Long {
        require(level >= 0) { "level must be >= 0, was $level" }
        return (PRICE_BASE * PRICE_GROWTH.pow(level.toDouble())).roundToLong()
    }

    fun priceOf(item: Item): Long = when (item) {
        Item.PREVENT_TICKET -> PREVENT_TICKET_PRICE
        Item.BLESSING_SCROLL -> BLESSING_SCROLL_PRICE
        Item.LUCK_CHARM -> LUCK_CHARM_PRICE
    }

    /**
     * 실제로 내는 값은 [GoldShop.itemPrice] 가 정한다. 여기 [priceOf] 는 **바닥값**이다.
     *
     * 단계에 곧바로 연동해 봤다가 [com.geomgang.core.sim.BalanceSimulation] 이 거부한 적이
     * 있다 — 무한 도달이 21단계에서 16단계로 주저앉았다. 아이템을 못 사면 파괴가 자금을
     * 통째로 지우고, 그러면 고단계에 도전할 밑천이 안 생긴다.
     *
     * 지금은 바닥값을 깔고 **누진만** 얹는다. 처음 몇 개는 예전 값 그대로라 초반이
     * 그대로고, 쟁여 둘수록 값이 오른다.
     */
    fun canBuyItem(state: GameState, item: Item): Boolean =
        GoldShop.canBuyItem(state, item)

    fun buyItem(state: GameState, item: Item): GameState {
        check(canBuyItem(state, item)) { "not enough gold for $item" }
        return GoldShop.buyItem(state, item)
    }

    fun canBuySword(state: GameState): Boolean =
        state.sword == null && state.pendingDestroy == null && state.gold >= BASE_SWORD_PRICE

    fun buySword(state: GameState, family: WeaponFamily): GameState {
        check(canBuySword(state)) { "cannot buy a sword in this state" }
        return state.copy(
            gold = state.gold - BASE_SWORD_PRICE,
            sword = Sword(family, 0),
        )
    }

    /**
     * 검을 **보관함으로 바로** 살 수 있는지.
     *
     * 손에 든 검이 있어도 된다. 고단계 강화는 재료 검을 요구하는데,
     * 그때마다 들고 있던 검을 팔거나 넣었다 빼야 한다면 준비가 일이 된다.
     */
    fun canBuyToStorage(state: GameState): Boolean =
        state.pendingDestroy == null &&
            state.gold >= BASE_SWORD_PRICE &&
            !Storage.isFull(state)

    fun buyToStorage(state: GameState, family: WeaponFamily): GameState {
        check(canBuyToStorage(state)) { "cannot buy into storage in this state" }
        return state.copy(
            gold = state.gold - BASE_SWORD_PRICE,
            storage = state.storage + Sword(family, 0),
        )
    }

    fun canSellSword(state: GameState): Boolean =
        state.sword != null && state.pendingDestroy == null

    fun sellSword(state: GameState): GameState {
        val sword = state.sword
        check(canSellSword(state) && sword != null) { "no sword to sell" }
        return state.copy(gold = state.gold + sellPrice(sword.level), sword = null)
    }

    /**
     * 아무것도 할 수 없는 상태인지.
     *
     * 검이 없고, 검을 살 골드도 없고, +5 검으로 바꿀 조각도 없을 때만 참이다.
     * 파괴 연출이 진행 중(pendingDestroy)이면 아직 결과가 확정된 게 아니므로 판정하지 않는다.
     */
    fun needsBailout(state: GameState): Boolean =
        state.sword == null &&
            state.pendingDestroy == null &&
            state.gold < BASE_SWORD_PRICE &&
            state.shards < Recipes.SWORD5_SHARD_COST

    /**
     * 파산 상태면 골드를 [BAILOUT_GOLD]로 채운다. 검은 주지 않는다.
     *
     * 검이 아니라 골드를 주는 이유: 검만 주면 골드가 0이라 강화 비용조차 못 내고,
     * 골드로 주면 플레이어가 원하는 계열을 골라 살 수 있다.
     * 사고 되파는 순환은 한 바퀴에 40골드씩 손해라 이 장치로 골드를 벌 수는 없다.
     */
    fun applyBailoutIfNeeded(state: GameState): GameState =
        if (needsBailout(state)) state.copy(gold = BAILOUT_GOLD) else state
}
