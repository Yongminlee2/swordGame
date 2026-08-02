package com.geomgang.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 이번 강화가 어떻게 끝날 수 있는지.
 *
 * 화면에는 성공률만 있었다. 떨어질 확률과 부서질 확률은 [RateTable] 에 있는데
 * 보이지 않아서, 특히 무한 구간(실패 = 파괴)을 모르고 누르게 됐다.
 */
class ForgeOddsTest {

    private fun odds(level: Int, items: UsedItems = UsedItems.NONE) =
        ForgeOdds.of(Difficulty.ENDLESS, targetLevel = level, items = items)

    @Test
    fun `세 값을 더하면 언제나 1이다`() {
        // 확률이 새면 화면이 거짓말을 한다. 구간 경계마다 확인한다.
        for (level in 1..60) {
            val o = odds(level)
            assertEquals("+$level", 1.0, o.success + o.drop + o.destroy + o.stay, 1e-9)
        }
    }

    @Test
    fun `안전구간은 떨어지지도 부서지지도 않는다`() {
        for (level in 1..RateTable.SAFE_BAND_END) {
            val o = odds(level)
            assertEquals("+$level 하락", 0.0, o.drop, 1e-9)
            assertEquals("+$level 파괴", 0.0, o.destroy, 1e-9)
            assertEquals("+$level 유지", 1.0 - o.success, o.stay, 1e-9)
        }
    }

    @Test
    fun `하락구간은 실패가 전부 하락이다`() {
        val level = RateTable.SAFE_BAND_END + 1
        val o = odds(level)
        assertEquals(0.0, o.destroy, 1e-9)
        assertEquals(0.0, o.stay, 1e-9)
        assertEquals(1.0 - o.success, o.drop, 1e-9)
    }

    @Test
    fun `파괴구간은 실패가 하락과 파괴로 갈린다`() {
        val level = RateTable.DROP_BAND_END + 1
        val o = odds(level)
        val fail = 1.0 - o.success
        assertTrue("파괴가 있어야 한다", o.destroy > 0.0)
        assertTrue("하락도 있어야 한다", o.drop > 0.0)
        assertEquals(fail * RateTable.destroyChance(level), o.destroy, 1e-9)
    }

    @Test
    fun `무한 구간은 실패가 곧 파괴다`() {
        // 이 숫자가 화면에 뜨는 순간이 이 게임에서 손이 가장 떨려야 하는 지점이다.
        val level = RateTable.MAX_FINITE_LEVEL + 1
        val o = odds(level)
        assertEquals(0.0, o.drop, 1e-9)
        assertEquals(1.0 - o.success, o.destroy, 1e-9)
    }

    @Test
    fun `축복서를 켜면 성공률이 오른다`() {
        val level = 14
        assertTrue(odds(level, UsedItems(blessing = true)).success > odds(level).success)
    }

    @Test
    fun `행운부적을 켜면 하락만 사라진다`() {
        // 토글이 무엇을 사는 건지 눈으로 보여야 누를 이유가 생긴다.
        val o = odds(18, UsedItems(luckCharm = true))
        assertEquals(0.0, o.drop, 1e-9)
        assertEquals(1.0 - o.success - o.destroy, o.stay, 1e-9)
    }

    /**
     * **부적을 켜도 파괴 확률은 한 치도 줄지 않는다**(v2.3).
     *
     * 예전에는 부적이 실패의 결과를 통째로 없애 화면의 파괴율이 0%로 바뀌었다.
     * 지금은 하락만 막으므로 그 표시는 거짓말이다 — 파괴를 막는 것은 방지권이다.
     */
    @Test
    fun `부적은 파괴 확률 표시를 건드리지 않는다`() {
        for (level in RateTable.DROP_BAND_END + 1..RateTable.MAX_FINITE_LEVEL) {
            assertEquals(
                "+$level",
                odds(level).destroy,
                odds(level, UsedItems(luckCharm = true)).destroy,
                1e-9,
            )
        }
    }

    @Test
    fun `백분율은 반올림해도 합이 100이다`() {
        // 화면은 정수 %로 쓴다. 62+30+8 이 99나 101이 되면 눈에 띈다.
        for (level in 1..60) {
            val p = odds(level).percents()
            assertEquals("+$level", 100, p.success + p.stay + p.drop + p.destroy)
        }
    }
}
