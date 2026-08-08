package com.geomgang.core

import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.ln
import kotlin.random.Random

/**
 * 담금질이 무한 구간의 속도를 얼마나 바꿨는지 숫자로 못 박는다.
 *
 * 실기기 세이브가 +44 였다. 담금질 전에는 +45 한 단계에 기댓값 200번이었고
 * 실패가 아무것도 남기지 않았다. 이 테스트가 그 구간을 지킨다.
 */
class TemperTempoTest {

    private companion object {
        const val START_LEVEL = 44
        const val RUNS = 2_000

        /**
         * 한 단계를 올리는 데 걸리는 시도 수의 목표 구간(중앙값).
         *
         * +44 는 전설검 구간이라 전설 특성(성공률 +3%p, 파괴방지 +3%)이 포함된 값이다.
         * 어렵게 얻은 검이 실제로 더 잘 붙는지를 여기서 잰다.
         */
        const val MIN_MEDIAN = 5
        const val MAX_MEDIAN = 30
    }

    /** 검이 부서지면 방지권으로 되살린다. 재도전 문턱이 아니라 속도를 재는 시험이다. */
    private fun attemptsToAdvance(rng: Random): Int {
        var state = GameState(
            difficulty = Difficulty.ENDLESS,
            gold = 1_000_000_000_000_000L,
            sword = Sword(WeaponFamily.STRAIGHT, START_LEVEL),
            inventory = Inventory(preventTickets = 9_999),
            forgeStones = 9_999,
        )
        var attempts = 0
        while (attempts < 5_000) {
            // 골드와 방지권은 이 시험의 관심사가 아니다. 매번 채운다.
            state = state.copy(
                gold = 1_000_000_000_000_000L,
                inventory = state.inventory.copy(preventTickets = 9_999),
            )
            val result = ForgeEngine.attempt(state, UsedItems.NONE, rng)
            attempts++
            state = result.state
            if (result is ForgeResult.Success) return attempts
            if (result is ForgeResult.Destroyed) {
                state = ForgeEngine.applyPrevent(state)
            }
            // 부서졌지만 검이 남은 경우(v2.5)에도 파괴창이 열려 pendingDestroy 가 남는다.
            // 실제 플레이에서는 파편을 줍거나 시간이 지나면 풀린다 — 여기서는 흘려보낸다.
            state = ForgeEngine.confirmDestroy(state)
        }
        return attempts
    }

    /** 튜닝용. 실패하지 않고 숫자만 뽑는다. */
    @Test
    fun `담금질 리포트를 출력한다`() {
        val rng = Random(20_260_729L)
        val counts = List(RUNS) { attemptsToAdvance(rng) }.sorted()
        val flat = RateTable.successRate(Difficulty.ENDLESS, START_LEVEL + 1)
        println(
            """
            [담금질] +$START_LEVEL -> +${START_LEVEL + 1}, $RUNS 판
              중앙값     : ${counts[counts.size / 2]}
              p90 / p99  : ${counts[(counts.size * 90) / 100]} / ${counts[(counts.size * 99) / 100]}
              최대       : ${counts.last()}
              평균       : %.1f
              담금질 없음 : %.1f (고정 %.3f%%)
            """.trimIndent().format(
                counts.average(),
                ln(0.5) / ln(1.0 - flat),
                flat * 100,
            ),
        )
    }

    @Test
    fun `담금질이 붙으면 한 단계가 목표 구간 안에서 끝난다`() {
        val rng = Random(20_260_729L)
        val counts = List(RUNS) { attemptsToAdvance(rng) }.sorted()
        val median = counts[counts.size / 2]

        assertTrue(
            "중앙값=$median (목표 $MIN_MEDIAN..$MAX_MEDIAN), " +
                "p90=${counts[(counts.size * 90) / 100]}, 최대=${counts.last()}",
            median in MIN_MEDIAN..MAX_MEDIAN,
        )
    }

    /**
     * 담금질이 없었다면 얼마나 걸렸는지.
     *
     * 확률이 고정이면 시도 수는 기하분포이고 중앙값은 ln(0.5)/ln(1-p) 다.
     * 이 값이 크다는 것이 담금질을 넣은 이유다 - 숫자로 남겨 둔다.
     */
    @Test
    fun `담금질이 없으면 백 번을 넘긴다`() {
        val flat = RateTable.successRate(Difficulty.ENDLESS, START_LEVEL + 1, temperFails = 0)
        val median = ln(0.5) / ln(1.0 - flat)
        assertTrue("담금질 없는 중앙값=%.1f (p=%.4f)".format(median, flat), median > 100.0)
    }

    /** 상한이 있어야 무한 구간이 끝없이 이어질 이유가 남는다. */
    @Test
    fun `담금질만으로는 상한을 넘지 못한다`() {
        val rate = RateTable.successRate(Difficulty.ENDLESS, 45, temperFails = 1_000_000)
        assertTrue("rate=$rate", rate <= Tempering.MAX_RATE + 1e-9)
    }
}
