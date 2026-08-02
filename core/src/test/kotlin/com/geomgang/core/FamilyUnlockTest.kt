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

    /**
     * 예전 조건은 "구역 3곳 클리어" 였다. 사냥터가 시즌2(용검 뒤)로 밀리면서
     * 대검 → 성검 → 용검 사슬이 데드락이 됐다 — 시즌1 활동(파괴)으로 바꿨다.
     */
    @Test
    fun `파괴를 세 번 겪으면 대검이 열린다`() {
        val two = ProgressState(stats = Stats(destroys = 2))
        assertFalse(WeaponFamily.GREAT in Progress.unlockedFamilies(two))
        val three = ProgressState(stats = Stats(destroys = Progress.GREAT_UNLOCK_DESTROYS.toLong()))
        assertTrue(WeaponFamily.GREAT in Progress.unlockedFamilies(three))
    }

    @Test
    fun `같은 구역을 다시 깨도 하나로 센다`() {
        var p = fresh
        repeat(5) { p = Progress.onZoneCleared(p, "meadow") }
        assertEquals(1, p.clearedZones.size)
    }

    /**
     * 예전 조건은 "고유검 1개 발견" 이었다. 고유검은 힌트만 있는 숨은 레시피인데
     * 세검이 없으면 창검도 허검도 전설검도 없다 — 숨은 것 하나가 주 진행선을 잡고 있었다.
     * v2.3에서 일반 조합이 사라져 "조합 1회" 도 못 쓰게 됐다. 지금은 강화 단계다.
     */
    @Test
    fun `깊이 강화하면 세검이 열린다`() {
        assertFalse(WeaponFamily.RAPIER in Progress.unlockedFamilies(fresh))
        val reached = ProgressState(
            stats = Stats(bestLevelEver = Progress.RAPIER_UNLOCK_LEVEL),
        )
        assertTrue(WeaponFamily.RAPIER in Progress.unlockedFamilies(reached))
    }

    @Test
    fun `조합 전용 계열은 어떤 진행도에서도 상점에 나오지 않는다`() {
        val maxed = ProgressState(
            achievements = Achievement.entries.toSet(),
            stats = Stats(bestLevelEver = 99, fusions = 999, destroys = 999),
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

    /**
     * v2.3부터 계열 조합은 [Refinery]다 — +20 두 자루가 새 계열 +1 이 된다.
     * 아무 단계나 섞어 평균을 내던 옛 조합은 없다.
     */
    @Test
    fun `계열 조합표가 마검과 성검을 만든다`() {
        val results = Refinery.RECIPES.associate { it.materials.toSet() to it }

        val demon = results[setOf(WeaponFamily.STRAIGHT, WeaponFamily.CURVED)]
        assertEquals(WeaponFamily.DEMON, demon?.result)
        assertEquals(1, demon?.resultLevel)

        val holy = results[setOf(WeaponFamily.GREAT, WeaponFamily.RAPIER)]
        assertEquals(WeaponFamily.HOLY, holy?.result)
        assertEquals(1, holy?.resultLevel)
    }

    /** 낮은 단계 두 자루는 아무것도 되지 않는다 — 계열 조합은 +20 의식이다. */
    @Test
    fun `평균 조합은 사라졌다`() {
        val result = Fusion.resultOrNull(
            listOf(Sword(WeaponFamily.STRAIGHT, 3), Sword(WeaponFamily.CURVED, 5)),
        )
        assertNull(result)
    }

    @Test
    fun `고유검 레시피는 조합으로 남아 있다`() {
        // 성검 둘 +10 이상은 삼위일체(고유검)다.
        val result = Fusion.resultOrNull(
            listOf(
                Sword(WeaponFamily.HOLY, 10),
                Sword(WeaponFamily.HOLY, 11),
            ),
        )
        assertEquals("trinity", result?.uniqueId)
    }
}
