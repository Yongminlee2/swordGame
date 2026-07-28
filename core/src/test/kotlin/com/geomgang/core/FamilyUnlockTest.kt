package com.geomgang.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 계열 해금 — 시작은 직검 하나뿐이고, 나머지는 서로 다른 활동으로 열린다.
 * 조합 전용 10계열은 상점·드롭에 절대 나오지 않는다.
 */
class FamilyUnlockTest {

    private val fresh = ProgressState()

    @Test
    fun `처음에는 직검만 열려 있다`() {
        assertEquals(listOf(WeaponFamily.STRAIGHT), Progress.unlockedFamilies(fresh))
    }

    @Test
    fun `10단계를 달성하면 곡도가 열린다`() {
        val before = fresh.copy(stats = Stats(bestLevelEver = 9))
        assertFalse(WeaponFamily.CURVED in Progress.unlockedFamilies(before))
        val after = fresh.copy(stats = Stats(bestLevelEver = Progress.CURVED_UNLOCK_LEVEL))
        assertTrue(WeaponFamily.CURVED in Progress.unlockedFamilies(after))
    }

    @Test
    fun `구역 셋을 깨면 대검이 열린다`() {
        var p = fresh
        p = Progress.onZoneCleared(p, "meadow")
        p = Progress.onZoneCleared(p, "forest")
        assertFalse(WeaponFamily.GREAT in Progress.unlockedFamilies(p))
        p = Progress.onZoneCleared(p, "cave")
        assertTrue(WeaponFamily.GREAT in Progress.unlockedFamilies(p))
    }

    @Test
    fun `같은 구역을 다시 깨도 하나로 센다`() {
        var p = fresh
        repeat(5) { p = Progress.onZoneCleared(p, "meadow") }
        assertEquals(1, p.clearedZones.size)
    }

    @Test
    fun `고유검을 발견하면 세검이 열린다`() {
        assertFalse(WeaponFamily.RAPIER in Progress.unlockedFamilies(fresh))
        val found = Progress.onUniqueFound(fresh, "trinity")
        assertTrue(WeaponFamily.RAPIER in Progress.unlockedFamilies(found))
    }

    @Test
    fun `조합 전용 계열은 어떤 진행도에서도 상점에 나오지 않는다`() {
        val maxed = ProgressState(
            achievements = Achievement.entries.toSet(),
            stats = Stats(bestLevelEver = 99),
            uniqueFound = UniqueSwords.RECIPES.map { it.id }.toSet(),
            clearedZones = Zone.entries.map { it.id }.toSet(),
        )
        val unlocked = Progress.unlockedFamilies(maxed)
        assertEquals(WeaponFamily.BASICS, unlocked)
        for (family in WeaponFamily.entries) {
            if (family in WeaponFamily.BASICS) continue
            assertFalse("$family 가 상점에 나온다", family in unlocked)
        }
    }

    @Test
    fun `잠긴 계열은 조건 설명을 준다`() {
        assertNotNull(Progress.basicFamilyHint(fresh, WeaponFamily.CURVED))
        assertNotNull(Progress.basicFamilyHint(fresh, WeaponFamily.GREAT))
        assertNotNull(Progress.basicFamilyHint(fresh, WeaponFamily.RAPIER))
        assertNull(Progress.basicFamilyHint(fresh, WeaponFamily.STRAIGHT))
    }

    @Test
    fun `조합이 조합표대로 계열을 만든다`() {
        // 직검 둘 -> 쌍검
        val twin = Fusion.resultOf(
            listOf(Sword(WeaponFamily.STRAIGHT, 3), Sword(WeaponFamily.STRAIGHT, 5)),
        )
        assertEquals(WeaponFamily.TWIN, twin.family)
        // 최고 5 + (2-1) + 같은 계열 1 = 7
        assertEquals(7, twin.level)

        // 대검 + 세검 -> 성검
        val holy = Fusion.resultOf(
            listOf(Sword(WeaponFamily.GREAT, 8), Sword(WeaponFamily.RAPIER, 6)),
        )
        assertEquals(WeaponFamily.HOLY, holy.family)
    }

    @Test
    fun `고유검 레시피가 조합표보다 먼저다`() {
        // 성검 셋 +10 이상은 삼위일체(고유검)다. 조합표에는 {성검} 항목이 없지만
        // 있더라도 고유검이 이긴다는 것을 확인한다.
        val result = Fusion.resultOf(
            listOf(
                Sword(WeaponFamily.HOLY, 10),
                Sword(WeaponFamily.HOLY, 11),
                Sword(WeaponFamily.HOLY, 12),
            ),
        )
        assertEquals("trinity", result.uniqueId)
    }
}
