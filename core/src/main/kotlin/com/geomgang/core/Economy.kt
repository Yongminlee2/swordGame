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

    /**
     * 상점에서 파는 +0 검의 가격.
     *
     * **+0 판매가([PRICE_BASE])보다 반드시 비싸야 한다.** 싸지면 사서 되파는 것만으로
     * 골드가 불어나 강화할 이유가 사라진다. v2.2에서 판매가 바닥을 110 으로 올리면서
     * 100 이던 이 값이 그 아래로 내려갔고, 파산 구제까지 겹쳐 무한 순환이 생겼다.
     * `EconomyTest` 의 「사서 되파는 것으로 골드가 늘지 않는다」가 이 선을 지킨다.
     */
    const val BASE_SWORD_PRICE: Long = 160

    /**
     * 소모품 고정가 — 시즌1(+20 이하)에서 실제로 내는 값이다([GoldShop.itemPrice]).
     *
     * v2.3에서 10배 올렸다(800/1,200/2,000 → 지금 값). 예전 값은 +5 검 한 자루 값이라
     * **고민거리가 아니라 그냥 늘 채워 두는 것**이었다. 실제로
     * [com.geomgang.core.sim.BalanceSimulation] 은 방지권을 다섯 장까지 상시 비축한다 —
     * 살 수 있으면 무조건 산다. 파괴가 나도 매번 되살리니 파괴 구간이 벽이 아니었다.
     * **파괴 구간을 다시 벽으로 만드는 손잡이가 여기다.**
     *
     * 얼마나 올릴지는 시뮬레이션이 정했다. 100,000/160,000/250,000 으로 잡아 봤더니
     * 20,000판 중 +20 도달이 **0명**이 되고 최고 단계 자체가 17에서 멈췄다 — 그건
     * 어려워진 것이 아니라 길이 막힌 것이다. 10배가 **천장(+20)이 남아 있는 가장 비싼
     * 값**이었다. 여기를 만지면 반드시 시뮬레이션부터 돌리고, 최고 단계가
     * [RateTable.MAX_FINITE_LEVEL] 인지부터 볼 것.
     */
    const val PREVENT_TICKET_PRICE: Long = 8_000
    const val BLESSING_SCROLL_PRICE: Long = 12_000
    const val LUCK_CHARM_PRICE: Long = 20_000

    /** 파산 구제가 채워 주는 골드. */
    const val BAILOUT_GOLD: Long = 300

    private const val COST_BASE = 30.0
    private const val COST_GROWTH = 1.45

    /**
     * 판매가의 바닥값.
     *
     * v2.2에서 60 → 110. 용검 이전에는 **검을 파는 것이 유일한 수입**이 됐다
     * ([Unlocks]) — 사냥터가 잠기고 강화석도 안 먹으니 골드 한 축으로만 돈다.
     *
     * 증가율(1.80)이 아니라 바닥값을 올렸다. 증가율은 +20 에서 12.7배로 부풀어
     * 후반 골드가 뜻을 잃는 진짜 출처가 된다. 바닥값은 곡선 모양을 그대로 두고
     * 한 바퀴의 벌이만 키운다.
     */
    private const val PRICE_BASE = 110.0

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

    /**
     * 무한 구간(+21 위)의 판매가 증가율.
     *
     * 1.80 은 **사냥이 없던 시절** 값이다. 그때는 검을 파는 것이 유일한 수입이라
     * 비용(1.45)을 크게 앞서야 자본이 모였다. 지금은 사냥이 그 몫을 한다.
     *
     * 1.80 을 끝까지 끌고 가면 +44 검 한 자루가 강화 27,000번치가 된다 — 골드가
     * 뜻을 잃는 진짜 출처다. 유한 구간은 건드리지 않는다(시뮬레이션이 지키는 구간이다).
     */
    private const val ENDLESS_PRICE_GROWTH = 1.35

    /** [currentLevel] 검을 한 단계 올리는 데 드는 비용. */
    fun upgradeCost(currentLevel: Int): Long {
        require(currentLevel >= 0) { "currentLevel must be >= 0, was $currentLevel" }
        return (COST_BASE * COST_GROWTH.pow(currentLevel.toDouble())).roundToLong()
    }

    /**
     * [level] 검을 팔았을 때 받는 골드.
     *
     * [RateTable.MAX_FINITE_LEVEL] 까지는 예전 곡선 그대로고, 그 위는 완만해진다.
     */
    /**
     * +11~+14 의 판매가 증가율.
     *
     * v2.3에서 1.80 → 1.50. +10 위에서 1.80 을 끌고 가면 +20 한 자루가 1,400만이라
     * 후반 골드가 뜻을 잃는다. +10 아래는 그대로다 — 거긴 파산 나선을 막으려고
     * 시뮬레이션으로 맞춘 구간이라 손대지 않는다.
     */
    private const val PRICE_GROWTH_MID = 1.50

    /**
     * +15~+20 의 판매가 증가율.
     *
     * 여기는 **비용 증가율(1.45)보다 낮다.** 의도한 것이다 — 이 구간은 돈을 버는
     * 구간이 아니라 조합 재료(+20)를 만드는 구간이고, 벌이는 +14 아래의 회전이 맡는다.
     * 1.50 을 끝까지 끌고 가면 +20 한 자루가 226만이라 그 한 자루로 시즌1이 끝난다.
     *
     * 1.20 → 1.35 (v2.3). 1.20 은 반대로 너무 짰다 — +15~20 을 만드는 값이
     * 그 검을 팔아 돌아오는 값과 너무 벌어져 이 구간이 순수한 손해 구간으로 읽혔다.
     * +20 이 59만에서 120만이 된다. 1.50(226만) 으로는 돌아가지 않는다.
     *
     * 한계 증가율이 뒤집혔으므로 **경제가 도는지는 시뮬레이션이 지킨다**
     * (`BalanceSimulationTest` 의 파산율 0%). 이 값을 만지면 반드시 그것부터 볼 것.
     */
    private const val PRICE_GROWTH_TOP = 1.35

    /** 1.80 곡선이 끝나는 단계. 여기까지가 파산 나선 방지 구간이다. */
    private const val LOW_BAND_END = 10

    /** 1.50 곡선이 끝나는 단계. 이 위는 완만해진다. */
    private const val MID_BAND_END = 14

    fun sellPrice(level: Int): Long {
        require(level >= 0) { "level must be >= 0, was $level" }
        val low = level.coerceAtMost(LOW_BAND_END)
        val mid = (level.coerceAtMost(MID_BAND_END) - LOW_BAND_END).coerceAtLeast(0)
        val top = (level.coerceAtMost(RateTable.MAX_FINITE_LEVEL) - MID_BAND_END)
            .coerceAtLeast(0)
        val endless = (level - RateTable.MAX_FINITE_LEVEL).coerceAtLeast(0)
        val value = PRICE_BASE *
            PRICE_GROWTH.pow(low.toDouble()) *
            PRICE_GROWTH_MID.pow(mid.toDouble()) *
            PRICE_GROWTH_TOP.pow(top.toDouble()) *
            ENDLESS_PRICE_GROWTH.pow(endless.toDouble())
        return value.roundToLong()
    }

    /**
     * 계열 판매 배수 — **계열이 가격에 나타나는 유일한 자리.**
     *
     * 같은 단계면 계열 무관 같은 값이었다. 조합검(마검·성검)을 만들어도 판매가가
     * 그대로라 조합의 값어치가 가격에 없었다. 기본 4계열은 해금 조건이 어려운
     * 순으로 조금씩, 조합검은 재료 두 자루와 조합비의 값을 쳐서 크게 오른다.
     *
     * **불변식 둘. 깨뜨리면 화폐가 무너진다.**
     * - 기본 4계열 +0 판매가 < 구매가([BASE_SWORD_PRICE]) — 사서 되팔면 손해여야 한다.
     * - 조합해 팔기 < 재료 둘을 그냥 팔기 — 1.8 곡선의 볼록성과 평균 단계가 지킨다.
     *   조합은 가격표가 아니라 계보를 위한 것이고, 프리미엄은 손해를 덜어 줄 뿐이다.
     *
     * 직검이 1.0 인 것은 시뮬레이터가 직검으로만 돌기 때문이다 — 기준이 움직이면
     * 그 모형이 재려던 것이 흐려진다. 숨긴 계열도 값을 갖는다(옛 세이브 보유분).
     */
    fun familyMult(family: WeaponFamily): Double = when (family) {
        WeaponFamily.STRAIGHT -> 1.00
        WeaponFamily.CURVED -> 1.05
        WeaponFamily.GREAT -> 1.10
        WeaponFamily.RAPIER -> 1.15
        WeaponFamily.DEMON -> 1.50
        WeaponFamily.HOLY -> 1.65
        WeaponFamily.DRAGON -> 2.00
        WeaponFamily.TWIN, WeaponFamily.SCYTHE -> 1.20
        WeaponFamily.AXE, WeaponFamily.SPEAR -> 1.25
        WeaponFamily.SPIRIT -> 1.60
        WeaponFamily.FUSED -> 1.70
        WeaponFamily.VOID -> 1.80
    }

    /** 이 검을 팔았을 때 받는 골드. 단계 곡선에 계열 배수를 얹는다. */
    fun sellPrice(sword: Sword): Long =
        (sellPrice(sword.level) * familyMult(sword.family)).roundToLong()

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
        return state.copy(gold = state.gold + sellPrice(sword), sword = null)
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
