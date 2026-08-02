package com.geomgang.core

import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * 대장간 스킬.
 *
 * 골드로 올리는 영구 성장이다. 골드는 후반에 쌓이기만 하고 쓸 데가 없었는데,
 * 여기가 오래 묶일 곳이 된다.
 *
 * 값을 **지금 강화 비용에 연동**한다. 고정값으로 두면 초반엔 못 사고 후반엔 공짜가
 * 된다 — 강화석([GoldShop])이 같은 이유로 같은 방식을 쓴다.
 */
object Smithy {

    const val MAX_LEVEL: Int = 15

    /** 레벨 하나가 주는 몫. 0.002 는 0.2%p 다. 성공률과 파괴방지가 같은 크기다. */
    const val PER_LEVEL: Double = 0.002

    /** 첫 레벨 값 = 지금 강화 비용 × 이 값. */
    private const val BASE_MULT = 5.0

    /** 레벨마다 붙는 배수. */
    private const val GROWTH = 1.5

    /** [level] 에서 다음 레벨로 올리는 값. */
    fun priceOf(state: GameState, level: Int): Long {
        val base = Economy.upgradeCost(state.bestLevel) * BASE_MULT
        return (base * GROWTH.pow(level.toDouble())).roundToLong().coerceAtLeast(1)
    }

    fun canUpgrade(state: GameState, progress: ProgressState): Boolean {
        if (progress.smithyLevel >= MAX_LEVEL) return false
        return state.gold >= priceOf(state, progress.smithyLevel)
    }

    /** 한 레벨 올린다. 골드는 게임 상태에서, 레벨은 진행도에서 움직인다. */
    fun upgrade(state: GameState, progress: ProgressState): Pair<GameState, ProgressState> {
        check(canUpgrade(state, progress)) { "cannot upgrade smithy in this state" }
        val price = priceOf(state, progress.smithyLevel)
        return state.copy(gold = state.gold - price) to
            progress.copy(smithyLevel = progress.smithyLevel + 1)
    }

    fun bonusOf(progress: ProgressState): ForgeBonus {
        val value = PER_LEVEL * progress.smithyLevel.coerceIn(0, MAX_LEVEL)
        return ForgeBonus(successRate = value, dropGuard = value)
    }
}
