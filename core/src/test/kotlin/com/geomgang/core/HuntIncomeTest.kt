package com.geomgang.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 사냥 수입은 **어느 구역에서나 같은 무게**여야 한다.
 *
 * 예전에는 한 바퀴가 초원에서 강화 37번치, 끝의 문에서 259번치였다. 단계당 사냥 골드가
 * 1.52배씩 자라는데 강화 비용은 1.45배씩 자라서, 그 미세한 차이가 38단계 쌓여 7배가 됐다.
 * 후반에 골드가 뜻을 잃은 진짜 이유다.
 *
 * 지금은 모든 구역이 「한 바퀴 = 강화 25번치」 근처에 있다. 이 테스트가 그 정렬을 지킨다 —
 * 구역을 새로 추가할 때 골드를 감으로 적으면 여기서 걸린다.
 */
class HuntIncomeTest {

    private companion object {
        /** 한 구역을 한 바퀴 도는 데 잡는 잡몹 수. */
        const val MOBS = Zone.MONSTERS_BEFORE_BOSS

        /** 목표 구간. 25 를 노리되 반올림과 구역 성격의 흔들림을 허용한다. */
        const val MIN_RATIO = 15.0
        const val MAX_RATIO = 40.0
    }

    /** 한 바퀴에서 나오는 골드. 잡몹은 종류별 배수의 평균을 쓴다. */
    private fun runGold(zone: Zone): Double {
        val avgFactor = zone.monsters.map { it.goldFactor }.average()
        return zone.baseGold * MOBS * avgFactor + zone.bossGold
    }

    @Test
    fun `모든 구역이 한 바퀴에 비슷한 무게를 준다`() {
        for (zone in Zone.entries) {
            val ratio = runGold(zone) / Economy.upgradeCost(zone.recommendedLevel)
            assertTrue(
                "${zone.displayName}(권장 +${zone.recommendedLevel}) 한 바퀴 = 강화 %.1f 번치".format(ratio),
                ratio in MIN_RATIO..MAX_RATIO,
            )
        }
    }

    /** 뒤 구역이 앞 구역보다 많이 준다. 안 그러면 앞 구역에 눌러앉는 게 이득이 된다. */
    @Test
    fun `구역이 깊어질수록 수입이 는다`() {
        val sorted = Zone.entries.sortedBy { it.recommendedLevel }
        for (i in 1 until sorted.size) {
            assertTrue(
                "${sorted[i - 1].displayName} -> ${sorted[i].displayName}",
                runGold(sorted[i]) > runGold(sorted[i - 1]),
            )
        }
    }

    /**
     * 검을 파는 것이 사냥을 압도하면 안 된다.
     *
     * 사냥이 열리는 것은 용검 뒤다([Unlocks.huntOpen]). 그때부터는 두 수입이 나란히
     * 서야 하는데, 판매가가 한없이 부풀면 사냥터가 장식이 된다.
     */
    @Test
    fun `무한 구간 판매가가 사냥을 압도하지 않는다`() {
        val topRun = runGold(Zone.entries.maxBy { it.recommendedLevel })
        for (level in 30..50) {
            val runs = Economy.sellPrice(level) / topRun
            assertTrue(
                "+$level 판매가 = 사냥 %.0f 바퀴치".format(runs),
                runs < 200,
            )
        }
    }

    /**
     * +10 까지는 1.80 곡선 모양 그대로다 — 바닥값만 올렸다.
     *
     * 이 구간은 파산 나선을 막으려고 시뮬레이션으로 맞춘 구간이라 손대지 않는다.
     * +11 부터는 1.50 으로 완만해진다(v2.3) — +10 위 판매가가 너무 높았다.
     */
    @Test
    fun `+10까지는 바닥값만 올라간 같은 곡선이다`() {
        for (level in 0..10) {
            val expected = Math.round(110.0 * Math.pow(1.80, level.toDouble()))
            assertEquals("+$level", expected, Economy.sellPrice(level))
        }
    }

    /** +11 부터는 1.80 곡선을 이어 갔을 때보다 반드시 싸야 한다. */
    @Test
    fun `+11 위 판매가는 옛 곡선보다 싸다`() {
        for (level in 11..50) {
            val straight = 110.0 * Math.pow(1.80, level.toDouble())
            assertTrue("+$level", Economy.sellPrice(level) < straight)
        }
    }
}
