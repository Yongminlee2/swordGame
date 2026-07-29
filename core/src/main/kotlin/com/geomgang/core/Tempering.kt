package com.geomgang.core

/**
 * 담금질 — 무한 구간에서 실패가 다음 성공률을 올린다.
 *
 * 예전에는 [RateTable.ENDLESS_FLOOR] 0.5% 에 붙은 채 실패가 **아무것도 남기지 않았다.**
 * 200번을 실패해도 201번째가 여전히 0.5% 라면 그건 긴장이 아니라 대기다.
 *
 * 실패가 쌓이면 확률이 오르고, 성공하면 0 으로 돌아간다. 그래서 게이지가 차오를수록
 * "이번엔 되나?" 가 진짜 질문이 된다.
 */
object Tempering {

    /**
     * 담금질이 붙는 첫 단계.
     *
     * 유한 구간(+20 이하)은 확률이 이미 충분해 필요 없고, 건드리면
     * 밸런스 시뮬레이션이 잡아 둔 곡선이 무너진다.
     */
    const val MIN_LEVEL: Int = RateTable.MAX_FINITE_LEVEL + 1

    /**
     * 실패 한 번이 더해 주는 몫. **기준 성공률에 대한 비율**이다.
     *
     * 고정 %p 로 두면 +21(1.7%)에서는 미미하고 +45(0.5%)에서는 과하거나 그 반대가 된다.
     * 비율로 두면 어느 단계에서나 "실패 두 번이면 확률이 두 배" 라는 같은 감각이 나온다.
     */
    const val STEP_RATIO: Double = 0.5

    /**
     * 담금질만으로 넘을 수 없는 성공률 상한.
     *
     * 여기가 없으면 무한 구간이 결국 공짜가 되고, 끝없이 이어질 이유가 사라진다.
     */
    const val MAX_RATE: Double = 0.50

    fun applies(targetLevel: Int): Boolean = targetLevel >= MIN_LEVEL

    /**
     * 담금질을 반영한 성공률.
     *
     * 붙지 않는 구간이거나 쌓인 실패가 없으면 [baseRate] 를 그대로 돌려준다.
     * 담금질은 **올려 주기만 한다** — 기준값이 이미 [MAX_RATE] 보다 높아도 낮추지 않는다.
     *
     * @param capBonus 계열 특성이 올려 주는 상한 가산. 정령검·전설검이 쓴다
     */
    fun rateFor(baseRate: Double, targetLevel: Int, fails: Int, capBonus: Double = 0.0): Double {
        if (!applies(targetLevel) || fails <= 0) return baseRate
        val raised = baseRate + baseRate * STEP_RATIO * fails
        return maxOf(baseRate, minOf(raised, MAX_RATE + capBonus))
    }

    /** 이 목표 단계에 유효한 누적 실패 수. 다른 단계에 쌓인 것은 세지 않는다. */
    fun failsFor(state: GameState, targetLevel: Int): Int =
        if (state.temperLevel == targetLevel) state.temperFails else 0
}
