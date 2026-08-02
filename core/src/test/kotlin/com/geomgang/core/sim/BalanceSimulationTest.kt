package com.geomgang.core.sim

import com.geomgang.core.Difficulty
import com.geomgang.core.RateTable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 밸런스 회귀 방지선.
 *
 * 구체적 수치가 아니라 **목표 구간**으로 단언한다. 확률표나 가격을 미세 조정할 때마다
 * 테스트가 깨지면 안 되지만, 밸런스가 크게 무너지면 반드시 걸려야 하기 때문이다.
 */
class BalanceSimulationTest {

    /** 튜닝용. 실패하지 않고 숫자만 뽑는다. */
    @Test
    fun `밸런스 리포트를 출력한다`() {
        for (difficulty in Difficulty.entries) {
            println(report(difficulty))
        }
    }

    @Test
    fun `일반 모드 평균 최고 단계가 목표 구간 안에 있다`() {
        val r = report(Difficulty.NORMAL)
        // v2.3에서 +10 위 판매가를 깎고(1.80 → 1.50 → 1.20) 소모품 값을 10배 올렸다.
        // 시즌1은 +20 여섯 자루의 여정이라 한 세션의 평균이 낮아진 것이 맞다 -
        // 대신 파산율이 0이어야 한다.
        assertTrue("평균 최고 단계=${r.averageBestLevel}", r.averageBestLevel in 14.0..18.0)
    }

    /**
     * **+20 은 맨손으로도 닿을 수 있되, 거저는 아니어야 한다.**
     *
     * 시즌1은 +20 을 여섯 번(기본 4계열 + 마검 + 성검) 넘는 여정이다(v2.3).
     * 시뮬레이터는 보너스 없는 맨손 플레이어의 한 세션(2,000회)만 본다 —
     * 실제 플레이어는 스킬·도감·고유검 보너스를 영구히 쌓아 회차마다 빨라지고,
     * 세션도 한 번으로 끝나지 않는다.
     *
     * 그래서 지키는 선은 둘이다.
     *
     * **길이 있다** — 최고 단계가 [RateTable.MAX_FINITE_LEVEL] 에 닿는다. 도달률이
     * 아니라 최고 단계로 재는 이유: 도달률은 소모품 값 한 번에 0.17% ↔ 0.01% 로
     * 흔들리는 꼬리 통계라 회귀를 알려 주지 못한다. **최고 단계가 20 아래로 내려가면
     * 그건 어려워진 것이 아니라 길이 막힌 것이다** — 소모품 값을 125배로 올렸을 때
     * 실제로 17에서 멈췄고, 그러면 용검에 영영 못 간다.
     *
     * **거저가 아니다** — 도달률 5% 미만. 이 선이 무너지면 시즌1이 다시 짧아진 것이다.
     *
     * **막다른 길이 아니라는 증거는 파산율 0%** 다 — 「경제가 돌아가서…」 테스트다.
     */
    @Test
    fun `상한에 닿되 거저 닿지는 않는다`() {
        val r = report(Difficulty.NORMAL)
        assertEquals("최고 도달=${r.maxBestLevel}", RateTable.MAX_FINITE_LEVEL, r.maxBestLevel)
        assertTrue("상한 도달률=${r.capRate}", r.capRate < 0.05)
        assertTrue("평균 시도 수=${r.averageAttempts}", r.averageAttempts > 500)
    }

    /**
     * 확률표가 살아 있다는 증거.
     *
     * 판매가나 재료 요구를 손대도 지옥 난이도는 여전히 벽이어야 한다. 여기가 무너지면
     * 경제를 만진 것이 아니라 **확률표를 망가뜨린 것**이다.
     */
    @Test
    fun `지옥 난이도는 여전히 벽이다`() {
        val r = report(Difficulty.HARD)
        assertTrue("지옥 상한 도달률=${r.capRate}", r.capRate < 0.05)
    }

    @Test
    fun `난이도 순서가 평균 최고 단계로 드러난다`() {
        val easy = report(Difficulty.EASY)
        val normal = report(Difficulty.NORMAL)
        val hard = report(Difficulty.HARD)
        assertTrue(
            "hard=${hard.averageBestLevel} normal=${normal.averageBestLevel}",
            hard.averageBestLevel < normal.averageBestLevel,
        )
        assertTrue(
            "normal=${normal.averageBestLevel} easy=${easy.averageBestLevel}",
            normal.averageBestLevel < easy.averageBestLevel,
        )
    }

    @Test
    fun `경제가 돌아가서 파산이 예외적인 일이 된다`() {
        // 파산 구제는 안전망이지 일상이 아니어야 한다.
        // 이 값이 1.0 에 붙으면 자본을 모을 수 없다는 뜻이고, 그때 게임은 저단계에서 멈춘다.
        val r = report(Difficulty.NORMAL)
        assertTrue("파산 발생률=${r.bankruptcyRate}", r.bankruptcyRate < 0.5)
    }

    /**
     * 무한 모드에는 난이도 상한이 없지만, **계열 상한 +20 은 있다.**
     *
     * +21 부터는 전설검 등급이고 강화가 아니라 조합으로만 넘어간다([LegendForge]).
     * 시뮬레이터는 사냥도 조합도 모형화하지 않으므로 그 벽을 넘을 길이 없다 —
     * 이 숫자가 21 이 되면 오히려 **강화만으로 벽이 뚫렸다는 뜻**이라 잘못이다.
     * 벽 너머는 `LegendForgeTest` 가 지킨다.
     */
    @Test
    fun `무한 모드는 난이도 상한 없이 계열 상한까지 올라간다`() {
        val r = report(Difficulty.ENDLESS)
        assertEquals("무한 최고 도달=${r.maxBestLevel}", 20, r.maxBestLevel)
        assertEquals(0.0, r.capRate, 1e-9)
    }

    @Test
    fun `쉬움 모드는 상한을 볼 수 있는 길을 열어 준다`() {
        // +20 도달과 흑룡참 도감이 아무에게도 닿지 않으면 죽은 콘텐츠가 된다.
        val r = report(Difficulty.EASY)
        assertTrue("쉬움 상한 도달률=${r.capRate}", r.capRate > 0.001)
    }

    companion object {
        // 판 수보다 한 판의 길이가 중요하다. 400회로는 +20 에 도달할 구조적 여유가 없어
        // 상한 도달률이 항상 0 이 나온다. 판당 2,000회는 끈질긴 플레이어 한 세션에 해당한다.
        const val RUNS = 20_000
        const val MAX_ATTEMPTS = 2_000
        const val SEED = 20260726L

        // 테스트마다 2만 판을 다시 돌리면 느려서 결과를 재사용한다.
        private val cache = mutableMapOf<Difficulty, SimReport>()

        fun report(difficulty: Difficulty): SimReport = cache.getOrPut(difficulty) {
            BalanceSimulation.run(difficulty, RUNS, MAX_ATTEMPTS, SEED)
        }
    }
}
