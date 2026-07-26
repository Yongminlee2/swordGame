package com.geomgang.core.sim

import com.geomgang.core.Difficulty
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
        assertTrue("평균 최고 단계=${r.averageBestLevel}", r.averageBestLevel in 9.0..15.0)
    }

    @Test
    fun `일반 모드에서 상한 도달이 가능하되 흔하지는 않다`() {
        val r = report(Difficulty.NORMAL)
        assertTrue("상한 도달률=${r.capRate}", r.capRate > 0.0)
        assertTrue("상한 도달률=${r.capRate}", r.capRate < 0.15)
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

    @Test
    fun `무한 모드는 상한 없이 21단계 이상을 만들어 낸다`() {
        // 용린참 이상 티어가 도감에서 죽은 칸이 되지 않으려면 이게 성립해야 한다.
        val r = report(Difficulty.ENDLESS)
        assertTrue("무한 최고 도달=${r.maxBestLevel}", r.maxBestLevel > 20)
        assertEquals(0.0, r.capRate, 1e-9)
    }

    @Test
    fun `쉬움 모드는 상한을 볼 수 있는 길을 열어 준다`() {
        // +20 도달과 흑룡참 도감이 아무에게도 닿지 않으면 죽은 콘텐츠가 된다.
        val r = report(Difficulty.EASY)
        assertTrue("쉬움 상한 도달률=${r.capRate}", r.capRate > 0.05)
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
