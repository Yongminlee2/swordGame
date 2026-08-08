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

    /** 레벨 하나가 주는 몫. 0.002 는 0.2%p 다. 성공률과 하락방지가 같은 크기다. */
    const val PER_LEVEL: Double = 0.002

    /**
     * 첫 레벨 값. **고정이다.**
     *
     * 한때 `강화 비용(최고 단계) × 5` 였다 — 검이 오를수록 같은 스킬 한 칸이
     * 비싸졌다. 미루면 손해라 "지금 사야 하나"를 늘 계산해야 했고, 무엇보다
     * **값이 왜 바뀌는지 화면이 설명하지 못했다.**
     *
     * 스킬은 영구 성장이라 값이 흔들릴 이유가 없다. 사다리는 레벨만 보고 오른다.
     *
     * 2,000 이었다가 올렸다(v2.4) — 영구 성장인데 너무 쌌다.
     */
    private const val BASE_PRICE = 5_000.0

    /** 레벨마다 붙는 배수. Lv0 5,000 → Lv14 약 360만, 다 올리면 960만쯤이다(v2.4, 1.5→1.6). */
    private const val GROWTH = 1.6

    /** [level] 에서 다음 레벨로 올리는 값. **검 단계와 무관하다.** */
    fun priceOf(level: Int): Long =
        (BASE_PRICE * GROWTH.pow(level.toDouble())).roundToLong().coerceAtLeast(1)

    /** 상태를 받는 옛 모양. 값은 [level] 만 본다. */
    fun priceOf(state: GameState, level: Int): Long = priceOf(level)

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
