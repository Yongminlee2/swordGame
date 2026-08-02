package com.geomgang.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** 계열 특성이 말로만 있지 않고 실제로 물리는지. */
class FamilyForgeEffectTest {

    private fun alwaysFail(): Random = object : Random() {
        override fun nextBits(bitCount: Int): Int =
            if (bitCount >= 32) -1 else (1 shl bitCount) - 1
    }

    private fun at(family: WeaponFamily, level: Int) = GameState(
        difficulty = Difficulty.ENDLESS,
        gold = 1_000_000_000_000_000L,
        sword = Sword(family, level),
        forgeStones = 999,
    )

    @Test
    fun `창검은 강화석이 두 개 덜 든다`() {
        val plain = ForgeCost.requirementFor(30).stones
        val spear = ForgeCost.requirementFor(30, relief = 2).stones
        assertEquals(plain - 2, spear)
    }

    @Test
    fun `강화석 요구는 0 아래로 내려가지 않는다`() {
        assertEquals(0, ForgeCost.requirementFor(0, relief = 5).stones)
    }

    /**
     * 담금질 특성은 전설검에만 있다.
     *
     * 담금질은 +21 부터 붙는데 계열 검은 +20 에서 끝난다. 계열에 담금질 특성을 주면
     * 영원히 발동하지 않는다 — 이 테스트가 그 실수를 막는다.
     */
    @Test
    fun `담금질 상한 특성은 전설검에만 있다`() {
        val families = WeaponFamily.entries.map { FamilyForge.of(Sword(it, 5)) }
        assertTrue(families.all { it.temperCapBonus == 0.0 })
        assertTrue(FamilyForge.LEGEND.temperCapBonus > 0.0)
    }

    @Test
    fun `전설검은 담금질 상한이 높다`() {
        assertEquals(
            Tempering.MAX_RATE + FamilyForge.LEGEND.temperCapBonus,
            Tempering.rateFor(0.005, 45, 100_000, FamilyForge.LEGEND.temperCapBonus),
            1e-9,
        )
    }

    /** 기준(직검)은 아무 특전도 없어야 한다. 시뮬레이터가 보는 "맨손" 이다. */
    @Test
    fun `직검은 아무 특전도 없다`() {
        val straight = FamilyForge.of(Sword(WeaponFamily.STRAIGHT, 5))
        assertEquals(0.0, straight.successBonus, 1e-9)
        assertEquals(0.0, straight.dropGuard, 1e-9)
        assertEquals(1.0, straight.costMult, 1e-9)
        assertEquals(0, straight.stoneRelief)
    }

    @Test
    fun `도끼검은 강화 비용이 싸다`() {
        val axe = ForgeEngine.attempt(at(WeaponFamily.AXE, 10), UsedItems.NONE, alwaysFail())
        val plain = ForgeEngine.attempt(at(WeaponFamily.STRAIGHT, 10), UsedItems.NONE, alwaysFail())
        assertTrue(
            "axe=${axe.state.gold} plain=${plain.state.gold}",
            axe.state.gold > plain.state.gold,
        )
    }

    /** 성검은 축복서가 더 잘 듣는다. */
    @Test
    fun `성검은 축복서 효과가 크다`() {
        val plain = RateTable.successRate(Difficulty.ENDLESS, 10, blessing = true)
        val holy = RateTable.successRate(Difficulty.ENDLESS, 10, blessing = true, blessingMult = 1.5)
        assertTrue("plain=$plain holy=$holy", holy > plain)
    }

    /**
     * 대검의 방어 특성은 **하락**을 확률적으로 붙든다(v2.3 - 파괴는 방지권만 막는다).
     * 하락 구간에서 여러 번 굴리면 유지로 살아남는 판이 나온다.
     */
    @Test
    fun `대검은 하락을 넘길 때가 있다`() {
        val rng = Random(11)
        var survived = 0
        repeat(400) {
            // +8 -> 목표 +9 는 하락 구간이다
            val r = ForgeEngine.attempt(at(WeaponFamily.GREAT, 8), UsedItems.NONE, rng)
            if (r is ForgeResult.Stay) survived++
        }
        assertTrue("살아남은 판=$survived", survived > 0)
    }
}
