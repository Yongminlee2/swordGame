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

    /** 강화석 기준가 = 지금 강화 비용 × 이 값. */
    const val STONE_MULT = 0.8

    /** 재료 검 기준가. 강화석보다 싸야 한다 — 자루는 사냥 드롭으로도 흔하다. */
    const val SWORD_MULT = 0.35

    private fun curve(state: GameState, mult: Double, bought: Int): Long {
        val base = Economy.upgradeCost(state.bestLevel) * mult
        return (base * GROWTH.pow(bought.toDouble())).roundToLong().coerceAtLeast(1)
    }

    fun stonePrice(state: GameState): Long = curve(state, STONE_MULT, state.stonesBought)

    fun materialSwordPrice(state: GameState): Long =
        curve(state, SWORD_MULT, state.swordsBought).coerceAtLeast(Economy.BASE_SWORD_PRICE)

    // 소모품(방지권·축복서·행운부적) 값은 여기서 다루지 않는다.
    // 단계 연동을 시도했다가 밸런스 시뮬레이션이 거부했다 - 사유는 [Economy.canBuyItem] 참고.

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
            state.copy(stonesBought = 0, swordsBought = 0, priceBandLevel = state.bestLevel)
        } else {
            state
        }
}
