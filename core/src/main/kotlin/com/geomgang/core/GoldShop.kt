package com.geomgang.core

import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * 골드 상점 가격 규칙.
 *
 * 강화석과 소모품은 진행도·연속 구매와 관계없이 고정가를 유지한다.
 * 재료 검만 진행도와 해당 구간 구매량에 따라 값이 오른다.
 */
object GoldShop {

    /** 재료 검을 하나 살 때마다 붙는 배수. */
    const val GROWTH = 1.18

    /** 용검 구간 초입 가격을 사용자가 읽기 쉽게 반올림한 고정가. */
    const val STONE_PRICE: Long = 4_000

    /** 재료 검은 강화석보다 싸게 유지해 조합 진행을 막지 않는다. */
    const val SWORD_MULT = 0.034

    /** 1.18^30 약 143배에서 누진을 멈춰 오랜 재료 제작 구간의 폭주를 막는다. */
    const val GROWTH_CAP = 30

    private fun curve(state: GameState, mult: Double, bought: Int): Long {
        val base = Economy.upgradeCost(state.bestLevel) * mult
        val steps = bought.coerceAtMost(GROWTH_CAP)
        return (base * GROWTH.pow(steps.toDouble())).roundToLong().coerceAtLeast(1)
    }

    fun stonePrice(@Suppress("UNUSED_PARAMETER") state: GameState): Long = STONE_PRICE

    fun materialSwordPrice(state: GameState): Long =
        curve(state, SWORD_MULT, state.swordsBought).coerceAtLeast(Economy.BASE_SWORD_PRICE)

    /** 소모품은 시즌과 연속 구매 횟수에 상관없이 처음 정한 가격을 쓴다. */
    fun itemPrice(@Suppress("UNUSED_PARAMETER") state: GameState, item: Item): Long =
        Economy.priceOf(item)

    fun canBuyItem(state: GameState, item: Item): Boolean =
        state.gold >= itemPrice(state, item)

    fun buyItem(state: GameState, item: Item): GameState {
        check(canBuyItem(state, item)) { "cannot buy $item in this state" }
        return state.copy(
            gold = state.gold - itemPrice(state, item),
            inventory = state.inventory.plus(item, 1),
        )
    }

    fun canBuyStone(state: GameState): Boolean =
        state.pendingDestroy == null && state.gold >= stonePrice(state)

    fun buyStone(state: GameState): GameState {
        check(canBuyStone(state)) { "cannot buy a stone in this state" }
        return state.copy(
            gold = state.gold - stonePrice(state),
            forgeStones = state.forgeStones + 1,
        )
    }

    /** 최고 단계가 오르면 재료 검 누진을 풀어 새 구간을 시작한다. */
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
