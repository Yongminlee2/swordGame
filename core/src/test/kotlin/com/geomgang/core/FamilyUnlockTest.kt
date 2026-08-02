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
     */
    @Test
    fun `조합을 몇 번 하면 세검이 열린다`() {
        assertFalse(WeaponFamily.RAPIER in Progress.unlockedFamilies(fresh))
        val fused = ProgressState(
            stats = Stats(fusions = Progress.RAPIER_UNLOCK_FUSIONS.toLong()),
        )
        assertTrue(WeaponFamily.RAPIER in Progress.unlockedFamilies(fused))
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

    @Test
    fun `조합이 조합표대로 계열을 만든다`() {
        // 직검 + 곡도 -> 마검, 단계는 평균 내림
        val demon = Fusion.resultOrNull(
            listOf(Sword(WeaponFamily.STRAIGHT, 3), Sword(WeaponFamily.CURVED, 5)),
        )
        assertEquals(WeaponFamily.DEMON, demon?.family)
        assertEquals(4, demon?.level)

        // 대검 + 세검 -> 성검
        val holy = Fusion.resultOrNull(
            listOf(Sword(WeaponFamily.GREAT, 8), Sword(WeaponFamily.RAPIER, 6)),
        )
        assertEquals(WeaponFamily.HOLY, holy?.family)
    }

    @Test
    fun `고유검 레시피가 조합표보다 먼저다`() {
        // 성검 둘 +10 이상은 삼위일체(고유검)다. 조합표에는 {성검} 항목이 없지만
        // 있더라도 고유검이 이긴다는 것을 확인한다.
        val result = Fusion.resultOrNull(
            listOf(
                Sword(WeaponFamily.HOLY, 10),
                Sword(WeaponFamily.HOLY, 11),
            ),
        )
        assertEquals("trinity", result?.uniqueId)
    }
}
