package com.geomgang.core

import kotlin.math.pow

/** 강화 실패 시 어떤 처리를 받는 구간인지. */
enum class FailureBand {
    /** 단계 유지. */
    STAY,

    /** 1단계 하락. */
    DROP,

    /** [RateTable.destroyChance] 확률로 파괴, 나머지는 하락. */
    DESTROY_OR_DROP,
}

/**
 * 강화 확률표. 게임 밸런스의 단일 출처다.
 *
 * 모든 함수의 `targetLevel`은 "이번 시도로 도달하려는 단계"다.
 * +7 검을 +8로 올리려는 시도의 targetLevel 은 8 이다.
 */
object RateTable {

    /** 난이도 배수와 축복서를 모두 적용한 뒤에도 넘을 수 없는 성공률 상한. */
    const val MAX_SUCCESS_RATE: Double = 0.98

    /** 축복서 1장이 더해 주는 성공률(%p). 난이도 배수를 적용한 뒤에 가산한다. */
    const val BLESSING_BONUS: Double = 0.10

    /** 무한 모드 +21 이상에서 한 단계마다 곱해지는 감쇠 계수. */
    const val ENDLESS_DECAY: Double = 0.85

    /** 무한 모드 성공률의 하한. 이보다 낮아지지 않는다. */
    const val ENDLESS_FLOOR: Double = 0.005

    /** 유한 모드(쉬움·일반·지옥)의 최대 단계. */
    const val MAX_FINITE_LEVEL: Int = 20

    /** 실패해도 단계가 유지되는 마지막 단계. */
    const val SAFE_BAND_END: Int = 5

    /**
     * 실패 시 1단계 하락하는 마지막 단계. 이 위는 파괴 가능 구간이다.
     *
     * v2.3에서 12 → 9. 파괴가 조각을 남기고 조각이 워프권이 되면서(죽음도 밑천),
     * 파괴는 더 일찍 시작해도 된다 — 잃는 것이 아니라 다른 화폐로 바뀌는 것이다.
     */
    const val DROP_BAND_END: Int = 9

    /**
     * 인덱스 = targetLevel(1..20). 0번 자리는 사용하지 않는 자리채움이다.
     *
     * v2.1에서 후반 낙폭을 완만하게 올렸다. 예전 표는 +16~+20 이 12→9→6→4→2% 로
     * 떨어져 "단계마다 반토막" 으로 읽혔다 — 실기기에서 차이가 너무 크다는 피드백.
     * 이제 +20 목표가 5% 다. 값은 `BalanceSimulationTest` 구간과 함께 움직인다.
     */
    private val BASE = doubleArrayOf(
        0.00,
        0.95, 0.90, 0.85, 0.80, 0.75,
        0.70, 0.64, 0.58, 0.52, 0.46,
        0.40, 0.35, 0.30, 0.26, 0.22,
        0.18, 0.14, 0.11, 0.08, 0.05,
    )

    /** 난이도 보정 전 기준 성공률. 무한 구간(21+)은 감쇠식으로 계산한다. */
    fun baseSuccessRate(targetLevel: Int): Double {
        require(targetLevel >= 1) { "targetLevel must be >= 1, was $targetLevel" }
        if (targetLevel <= MAX_FINITE_LEVEL) return BASE[targetLevel]
        val steps = (targetLevel - MAX_FINITE_LEVEL).toDouble()
        val decayed = BASE[MAX_FINITE_LEVEL] * ENDLESS_DECAY.pow(steps)
        return maxOf(decayed, ENDLESS_FLOOR)
    }

    /**
     * 난이도 배수·담금질·축복서를 반영한 최종 성공률.
     *
     * 순서가 뜻을 만든다. 담금질은 누적된 몫이라 난이도 배수 **뒤**에 붙고,
     * 축복서는 "이번 판만 얹는 것" 이라 누적분 **위**에 얹힌다.
     *
     * @param temperFails 이 목표 단계에 쌓인 실패 수. 0 이면 예전과 같은 값이다.
     * @param bonus [ForgeBonuses] 가 모은 성공률 가산. 플레이어가 쌓아 온 몫이라
     *   맨 마지막에 더한다. 0 이면 예전과 같은 값이다.
     */
    fun successRate(
        difficulty: Difficulty,
        targetLevel: Int,
        blessing: Boolean = false,
        temperFails: Int = 0,
        bonus: Double = 0.0,
        temperCapBonus: Double = 0.0,
        blessingMult: Double = 1.0,
    ): Double {
        val scaled = baseSuccessRate(targetLevel) * difficulty.multiplier
        val tempered = Tempering.rateFor(scaled, targetLevel, temperFails, temperCapBonus)
        val boosted = if (blessing) tempered + BLESSING_BONUS * blessingMult else tempered
        return minOf(boosted + bonus, MAX_SUCCESS_RATE)
    }

    /** 실패했을 때 어떤 처리를 받는 구간인지. */
    fun failureBand(targetLevel: Int): FailureBand {
        require(targetLevel >= 1) { "targetLevel must be >= 1, was $targetLevel" }
        return when {
            targetLevel <= SAFE_BAND_END -> FailureBand.STAY
            targetLevel <= DROP_BAND_END -> FailureBand.DROP
            else -> FailureBand.DESTROY_OR_DROP
        }
    }

    /** 실패했을 때 파괴로 이어질 확률. 유지·하락 구간이면 0.0. */
    fun destroyChance(targetLevel: Int): Double {
        require(targetLevel >= 1) { "targetLevel must be >= 1, was $targetLevel" }
        // v2.3에서 유한 구간을 두 번 낮췄다(0.40/0.60/0.80 → 0.25/0.40/0.55 → 다시 절반).
        // 시즌1이 길어진 것은 조합(계열마다 +20)이 맡는다 - 파괴까지 잦으면 길이가 아니라 고문이 된다.
        // 계열 넷을 전부 +20까지 올려야 하는 지금, 파괴 한 번은 한 계열을 통째로 되돌린다.
        // +10~12 는 파괴가 시작되는 인사다 - 낮게 두어 조각 맛을 보게 한다.
        return when {
            targetLevel <= DROP_BAND_END -> 0.00
            targetLevel <= 12 -> 0.10
            targetLevel <= 15 -> 0.125
            targetLevel <= 18 -> 0.20
            targetLevel <= MAX_FINITE_LEVEL -> 0.275
            else -> 1.00
        }
    }
}
