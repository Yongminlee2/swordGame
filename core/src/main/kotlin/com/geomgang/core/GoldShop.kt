package com.geomgang.core

import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * 골드로 사는 것들의 가격.
 *
 * 사냥 수입은 초원 잡몹 14골드에서 끝의 문 1.4억골드까지 천만 배가 되는데
 * 상점 값은 처음 그대로였다(방지권 800, 검 100). 그래서 후반에 골드는 쌓이기만 하고
 * 정작 발목을 잡는 것은 재료였다. 남는 것과 모자란 것을 잇는다.
 *
 * **가격 뼈대**
 * ```
 * 가격 = (지금 강화 비용 × 배수) × GROWTH^(이 구간에서 산 개수)
 * ```
 * 두 항이 각각 다른 실패를 막는다.
 * - 누진이 없으면 한 번에 몰아 사서 사냥터를 통째로 건너뛴다
 * - 누진만 있으면 값이 영원히 올라 결국 아무것도 못 사게 되고, 골드는 다시 쓸 데를 잃는다
 *
 * 그래서 **최고 단계가 오르면 누진이 풀린다**([rebase]). 한 단계 올릴 때마다 값이
 * 되돌아오므로, 골드는 늘 "다음 한 단계"에 묶여 있다.
 */
object GoldShop {

    /** 하나 살 때마다 붙는 배수. */
    const val GROWTH = 1.18

    /**
     * 강화석 기준가 = 지금 강화 비용 × 이 값.
     *
     * 사냥 골드를 「한 바퀴 = 강화 25번치」로 통일하면서 후반 구역 수입이 10배 줄었다.
     * 이 값을 그대로 두면 **같은 한 레벨에 사냥을 10배 더 돌아야 한다.** 수입과 함께
     * 내려야 갈리는 속도가 그대로다 — 줄이려던 것은 골드의 자릿수지 플레이 시간이 아니다.
     */
    const val STONE_MULT = 0.077

    /**
     * 재료 검 기준가. **강화석보다 싸야 한다.**
     *
     * 사냥에서 검이 떨어지지 않게 된 뒤로 보관함을 채우는 길은 여기뿐이다.
     * 강화석보다 비싸지면 조합이 통째로 잠긴다.
     *
     * [STONE_MULT] 와 같은 비율(0.44배)을 유지한다 — 둘을 따로 만지면 어느 쪽이
     * 비싼지가 우연에 맡겨진다.
     */
    const val SWORD_MULT = 0.034

    /**
     * 누진이 멈추는 개수.
     *
     * [rebase] 는 **최고 단계가 올라야** 누진을 푸는데, 계열이 +20 에서 끝나면서
     * 최고 단계가 그 자리에 오래 머무는 구간이 생겼다 — 전설검 재료 네 자루를
     * 각각 +20 까지 올리는 동안이 전부 그렇다. 그 구간에서 값이 끝없이 오르면
     * **골드가 다시 쓸 데를 잃는다.** 이 파일이 처음부터 막으려던 실패다.
     *
     * 1.18^30 ≈ 143배. 여기서 멈춰도 "몰아 사서 사냥터를 건너뛰는" 길은 막힌다.
     */
    const val GROWTH_CAP = 30

    private fun curve(state: GameState, mult: Double, bought: Int): Long {
        val base = Economy.upgradeCost(state.bestLevel) * mult
        val steps = bought.coerceAtMost(GROWTH_CAP)
        return (base * GROWTH.pow(steps.toDouble())).roundToLong().coerceAtLeast(1)
    }

    fun stonePrice(state: GameState): Long = curve(state, STONE_MULT, state.stonesBought)

    fun materialSwordPrice(state: GameState): Long =
        curve(state, SWORD_MULT, state.swordsBought).coerceAtLeast(Economy.BASE_SWORD_PRICE)

    /** 소모품 하나를 살 때 붙는 배수. */
    const val ITEM_GROWTH = 1.18

    /**
     * 소모품 값이 움직이기 시작하는 최고 단계.
     *
     * 유한 구간(+20 이하)은 **예전 고정가 그대로**다. 이유가 둘이다.
     *
     * 하나, 거기서는 고정가가 이미 제 몫을 한다 — 골드가 귀해서 방지권 800이 고민거리다.
     *
     * 둘, [com.geomgang.core.sim.BalanceSimulation] 이 도는 구간이 여기인데 그 모형에는
     * **사냥이 없다.** 후반 골드의 출처가 통째로 빠져 있어서, 진행 연동 가격을 넣으면
     * 무엇을 넣든 거부한다. 실제로 v1.7에 한 번, 여기서 또 한 번 무한 도달이 19까지
     * 주저앉았다. 시뮬레이터가 보지 못하는 것을 시뮬레이터로 재려 하면 안 된다.
     */
    const val ITEM_BAND_LEVEL = RateTable.MAX_FINITE_LEVEL + 1

    /** 무한 구간 기준가 = 지금 강화 비용 × 이 값. 셋의 상대 순서는 고정가와 같다. */
    private fun endlessMultOf(item: Item): Double = when (item) {
        Item.PREVENT_TICKET -> 0.15
        Item.BLESSING_SCROLL -> 0.25
        Item.LUCK_CHARM -> 0.40
    }

    /**
     * 소모품 값.
     *
     * 고정가만 두면 후반에 공짜나 다름없어진다 — 골드가 465억인데 방지권이 800골드였다.
     * 무한 구간부터는 강화 비용에 연동하고 누진을 얹는다.
     *
     * 세 소모품이 **카운터 하나를 함께 쓴다.** 그래야 "이번 구간에 무엇을 쟁일까" 가
     * 선택이 된다 — 따로 세면 셋 다 쟁이는 것이 늘 최선이 되어 고를 것이 없어진다.
     * 누진은 한 단계 올리면 [rebase] 가 푼다.
     *
     * 값이 올라도 진행이 막히지 않는 이유: **조각 교환은 그대로다.** 골드로 못 사도
     * 파괴에서 주운 조각으로 방지권·축복서·부적을 바꿀 수 있다([Recipes]).
     */
    fun itemPrice(state: GameState, item: Item): Long {
        val floor = Economy.priceOf(item)
        if (state.bestLevel < ITEM_BAND_LEVEL) return floor

        val base = Economy.upgradeCost(state.bestLevel) * endlessMultOf(item)
        return (base * ITEM_GROWTH.pow(state.itemsBought.toDouble()))
            .roundToLong()
            .coerceAtLeast(floor)
    }

    fun canBuyItem(state: GameState, item: Item): Boolean =
        state.gold >= itemPrice(state, item)

    fun buyItem(state: GameState, item: Item): GameState {
        check(canBuyItem(state, item)) { "cannot buy $item in this state" }
        return state.copy(
            gold = state.gold - itemPrice(state, item),
            inventory = state.inventory.plus(item, 1),
            itemsBought = state.itemsBought + 1,
        )
    }

    fun canBuyStone(state: GameState): Boolean =
        state.pendingDestroy == null && state.gold >= stonePrice(state)

    fun buyStone(state: GameState): GameState {
        check(canBuyStone(state)) { "cannot buy a stone in this state" }
        return state.copy(
            gold = state.gold - stonePrice(state),
            forgeStones = state.forgeStones + 1,
            stonesBought = state.stonesBought + 1,
        )
    }

    /**
     * 최고 단계가 오르면 누진을 푼다.
     *
     * 부르는 곳은 **강화 성공 한 군데뿐**이다. 여러 곳에서 리셋하면 카운터가 어긋나고,
     * 어긋난 카운터는 "왜 갑자기 싸졌지"로 나타나 규칙을 읽을 수 없게 만든다.
     */
    fun rebase(state: GameState): GameState =
        if (state.bestLevel > state.priceBandLevel) {
            state.copy(
                stonesBought = 0,
                swordsBought = 0,
                itemsBought = 0,
                priceBandLevel = state.bestLevel,
            )
        } else {
            state
        }
}
