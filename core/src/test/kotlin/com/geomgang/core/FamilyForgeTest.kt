package com.geomgang.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 계열마다 강화 특성이 다르다. 여기서 계열이 처음으로 성격을 갖는다.
 *
 * 전투 특성([FamilyStyle])과 한 enum 에 섞지 않는다 — 서로 다른 이유로 바뀌는 값이다.
 */
class FamilyForgeTest {

    @Test
    fun `단계가 전설 여부를 정한다`() {
        assertFalse(Sword(WeaponFamily.STRAIGHT, 20).isLegend())
        assertTrue(Sword(WeaponFamily.STRAIGHT, 21).isLegend())
    }

    /** 전설검은 계열을 무시한다. +21 위면 무엇이든 전설이다. */
    @Test
    fun `전설검은 계열을 무시한다`() {
        assertEquals(FamilyForge.LEGEND, FamilyForge.of(Sword(WeaponFamily.STRAIGHT, 30)))
        assertEquals(FamilyForge.LEGEND, FamilyForge.of(Sword(WeaponFamily.VOID, 30)))
    }

    @Test
    fun `계열마다 다른 특성을 갖는다`() {
        assertEquals(FamilyForge.STRAIGHT, FamilyForge.of(Sword(WeaponFamily.STRAIGHT, 5)))
        assertEquals(FamilyForge.RAPIER, FamilyForge.of(Sword(WeaponFamily.RAPIER, 5)))
    }

    @Test
    fun `검이 없으면 아무 특성도 없다`() {
        val none = FamilyForge.of(null)
        assertEquals(0.0, none.successBonus, 1e-9)
        assertEquals(1.0, none.temperMult, 1e-9)
        assertEquals(1.0, none.costMult, 1e-9)
    }

    /** 전설검이 모든 계열보다 강해야 한다. 가장 어려운 길의 보상이다. */
    @Test
    fun `전설검이 가장 강하다`() {
        val others = FamilyForge.entries
            .filter { it != FamilyForge.LEGEND && it != FamilyForge.NONE }
        assertTrue(others.all { FamilyForge.LEGEND.successBonus >= it.successBonus })
        assertTrue(others.all { FamilyForge.LEGEND.destroyGuard >= it.destroyGuard })
    }

    @Test
    fun `세검은 담금질이 두 배로 쌓인다`() {
        assertEquals(2.0, FamilyForge.of(Sword(WeaponFamily.RAPIER, 5)).temperMult, 1e-9)
    }

    @Test
    fun `도끼검은 강화 비용이 싸다`() {
        assertTrue(FamilyForge.of(Sword(WeaponFamily.AXE, 5)).costMult < 1.0)
    }

    @Test
    fun `열네 계열이 모두 특성을 갖는다`() {
        for (family in WeaponFamily.entries) {
            val forge = FamilyForge.of(Sword(family, 5))
            assertTrue("$family", forge != FamilyForge.NONE)
            assertTrue("$family", forge.blurb.isNotBlank())
        }
    }

    /** 든 검의 몫이 보너스 출처에 들어간다. */
    @Test
    fun `든 검이 보너스 출처에 들어간다`() {
        val holding = GameState(Difficulty.ENDLESS, sword = Sword(WeaponFamily.GREAT, 5))
        assertTrue(ForgeBonuses.sourcesOf(holding, ProgressState()).any { it.label == "계열" })
        assertTrue(ForgeBonuses.of(holding, ProgressState()).destroyGuard > 0.0)
    }

    @Test
    fun `전설검을 들면 출처 이름이 바뀐다`() {
        val holding = GameState(Difficulty.ENDLESS, sword = Sword(WeaponFamily.GREAT, 30))
        assertTrue(ForgeBonuses.sourcesOf(holding, ProgressState()).any { it.label == "전설검" })
    }
}
