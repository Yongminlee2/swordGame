package com.geomgang.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 해금 조건은 **화면을 보고 밟을 수 있어야** 한다.
 *
 * 세검 조건이 "고유검 1개 발견" 이었다. 고유검은 힌트만 있는 숨은 레시피인데,
 * 창검은 세검 둘 · 허검은 도끼검+창검 · 전설검은 허검이 있어야 한다.
 * **숨은 것 하나가 주 진행선 전체를 인질로 잡고 있었다.**
 */
class FamilyUnlockReachTest {

    @Test
    fun `세검은 조합 횟수로 열린다`() {
        val none = ProgressState()
        assertFalse(Progress.basicFamilyUnlocked(none, WeaponFamily.RAPIER))

        val fused = ProgressState(
            stats = Stats(fusions = Progress.RAPIER_UNLOCK_FUSIONS.toLong()),
        )
        assertTrue(Progress.basicFamilyUnlocked(fused, WeaponFamily.RAPIER))
    }

    /** 고유검을 하나도 못 찾아도 세검은 열려야 한다. */
    @Test
    fun `세검은 고유검과 무관하다`() {
        val fused = ProgressState(
            stats = Stats(fusions = Progress.RAPIER_UNLOCK_FUSIONS.toLong()),
            uniqueFound = emptySet(),
        )
        assertTrue(Progress.basicFamilyUnlocked(fused, WeaponFamily.RAPIER))
        assertNull(Progress.basicFamilyHint(fused, WeaponFamily.RAPIER))
    }

    /** 잠긴 동안에는 몇 번 남았는지가 보여야 한다. */
    @Test
    fun `세검 힌트가 진행도를 담는다`() {
        val hint = Progress.basicFamilyHint(ProgressState(), WeaponFamily.RAPIER)
        assertNotNull(hint)
        assertTrue("힌트=$hint", hint!!.contains("0/${Progress.RAPIER_UNLOCK_FUSIONS}"))
    }

    /**
     * 용검(전설)까지 가는 길에 숨은 조건이 없어야 한다.
     *
     * 용검 ← 마검+성검 ← 기본 4계열. 이 사슬의 어느 고리도 고유검을 요구하면 안 된다.
     */
    @Test
    fun `전설 재료로 가는 길에 숨은 조건이 없다`() {
        // 상점에서 살 수 있는 기본 계열은 전부 드러난 조건으로 열린다
        // 전부 시즌1 활동이다 - 사냥(구역)을 요구하는 고리가 하나라도 있으면 데드락이다
        val reachable = ProgressState(
            stats = Stats(
                bestLevelEver = Progress.CURVED_UNLOCK_LEVEL,
                fusions = Progress.RAPIER_UNLOCK_FUSIONS.toLong(),
                destroys = Progress.GREAT_UNLOCK_DESTROYS.toLong(),
            ),
        )
        for (family in WeaponFamily.BASICS) {
            assertTrue("$family 가 안 열렸다", Progress.basicFamilyUnlocked(reachable, family))
        }

        // 그 넷로 전설 재료 둘(마검·성검)이 전부 만들어진다
        for (material in LegendForge.MATERIALS) {
            assertTrue("$material", FusionTable.ALL.any { it.result == material })
        }
    }
}
