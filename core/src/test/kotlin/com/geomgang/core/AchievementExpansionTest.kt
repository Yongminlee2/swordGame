package com.geomgang.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v1.3~v1.4 컨텐츠 업적.
 *
 * 업적이 강화 한 갈래만 보고 있으면 사냥·조합·회랑·수집을 아무리 해도 남는 것이 없다.
 * 늘어난 시스템마다 이정표가 하나씩 있어야 한다.
 */
class AchievementExpansionTest {

    private val empty = ProgressState()

    private fun refreshed(p: ProgressState) = Progress.refresh(p)

    private fun stats(block: Stats.() -> Stats) = empty.copy(stats = empty.stats.block())

    // --- 사냥 ---

    @Test
    fun `잡몹 처치 수가 세 단계 이정표를 연다`() {
        assertFalse(Achievement.HUNT_100 in refreshed(stats { copy(monsterKills = 99) }).achievements)
        assertTrue(Achievement.HUNT_100 in refreshed(stats { copy(monsterKills = 100) }).achievements)
        assertTrue(Achievement.HUNT_1000 in refreshed(stats { copy(monsterKills = 1_000) }).achievements)
        assertTrue(
            Achievement.HUNT_10000 in refreshed(stats { copy(monsterKills = 10_000) }).achievements,
        )
    }

    @Test
    fun `첫 보스를 잡으면 업적이 열린다`() {
        assertTrue(Achievement.BOSS_FIRST in refreshed(stats { copy(bossKills = 1) }).achievements)
    }

    @Test
    fun `모든 구역을 깨면 정복자 칭호가 열린다`() {
        val partial = empty.copy(clearedZones = Zone.entries.drop(1).map { it.id }.toSet())
        assertFalse(Achievement.ZONES_ALL in refreshed(partial).achievements)
        val all = empty.copy(clearedZones = Zone.entries.map { it.id }.toSet())
        assertTrue(Achievement.ZONES_ALL in refreshed(all).achievements)
    }

    @Test
    fun `이벤트 조우가 쌓이면 업적이 열린다`() {
        assertTrue(Achievement.EVENT_50 in refreshed(stats { copy(eventsSeen = 50) }).achievements)
    }

    // --- 조합 ---

    @Test
    fun `조합 횟수가 두 단계 이정표를 연다`() {
        assertTrue(Achievement.FUSE_10 in refreshed(stats { copy(fusions = 10) }).achievements)
        assertFalse(Achievement.FUSE_100 in refreshed(stats { copy(fusions = 99) }).achievements)
        assertTrue(Achievement.FUSE_100 in refreshed(stats { copy(fusions = 100) }).achievements)
    }

    @Test
    fun `고유검 발견이 세 단계 이정표를 연다`() {
        val one = Progress.onUniqueFound(empty, "trinity")
        assertTrue(Achievement.UNIQUE_FIRST in refreshed(one).achievements)
        assertFalse(Achievement.UNIQUE_5 in refreshed(one).achievements)

        var five = empty
        UniqueSwords.RECIPES.take(5).forEach { five = Progress.onUniqueFound(five, it.id) }
        assertTrue(Achievement.UNIQUE_5 in refreshed(five).achievements)
        assertFalse(Achievement.UNIQUE_ALL in refreshed(five).achievements)

        var all = empty
        UniqueSwords.RECIPES.forEach { all = Progress.onUniqueFound(all, it.id) }
        assertTrue(Achievement.UNIQUE_ALL in refreshed(all).achievements)
    }

    // --- 특수강화·재료 ---

    @Test
    fun `별 기록이 남고 업적이 열린다`() {
        var p = Progress.onStars(empty, 3)
        assertEquals(3, p.stats.maxStars)
        assertTrue(Achievement.STAR_3 in refreshed(p).achievements)
        assertFalse(Achievement.STAR_5 in refreshed(p).achievements)

        // 별이 떨어져도 최고 기록은 유지된다
        p = Progress.onStars(p, 1)
        assertEquals(3, p.stats.maxStars)

        p = Progress.onStars(p, StarForce.MAX_STARS)
        assertTrue(Achievement.STAR_5 in refreshed(p).achievements)
    }

    @Test
    fun `강화석 획득이 누적된다`() {
        var p = Progress.onStones(empty, 300)
        p = Progress.onStones(p, 200)
        assertEquals(500L, p.stats.stonesEarned)
        assertTrue(Achievement.STONE_500 in refreshed(p).achievements)
    }

    @Test
    fun `0개 획득은 세지 않는다`() {
        assertEquals(0L, Progress.onStones(empty, 0).stats.stonesEarned)
    }

    @Test
    fun `스킬 발동이 누적된다`() {
        var p = empty
        repeat(100) { p = Progress.onSkill(p) }
        assertEquals(100L, p.stats.skillsTriggered)
        assertTrue(Achievement.SKILL_100 in refreshed(p).achievements)
    }

    // --- 회랑·수집 ---

    @Test
    fun `회랑 최고 층이 세 단계 이정표를 연다`() {
        var p = Progress.onGauntletFloor(empty, 10)
        assertTrue(Achievement.GAUNTLET_10 in refreshed(p).achievements)
        // 낮은 층을 다시 깨도 기록은 안 내려간다
        p = Progress.onGauntletFloor(p, 3)
        assertEquals(10, p.stats.gauntletBestEver)
        p = Progress.onGauntletFloor(p, 50)
        assertTrue(Achievement.GAUNTLET_25 in refreshed(p).achievements)
        assertTrue(Achievement.GAUNTLET_50 in refreshed(p).achievements)
    }

    @Test
    fun `펫 수집이 두 단계 이정표를 연다`() {
        val one = Progress.onPetFound(empty, PetKind.QUOKKA.id)
        assertTrue(Achievement.PET_FIRST in refreshed(one).achievements)
        assertFalse(Achievement.PET_ALL in refreshed(one).achievements)

        var all = empty
        PetKind.entries.forEach { all = Progress.onPetFound(all, it.id) }
        assertTrue(Achievement.PET_ALL in refreshed(all).achievements)
    }

    @Test
    fun `같은 펫을 또 얻어도 수집 기록은 하나다`() {
        var p = empty
        repeat(5) { p = Progress.onPetFound(p, PetKind.QUOKKA.id) }
        assertEquals(1, p.petsFound.size)
    }

    // --- 전체 ---

    @Test
    fun `업적 id와 칭호가 겹치지 않는다`() {
        val ids = Achievement.entries.map { it.id }
        assertEquals("업적 id가 겹친다", ids.size, ids.toSet().size)
        val titles = Achievement.entries.map { it.title }
        assertEquals("칭호가 겹친다", titles.size, titles.toSet().size)
    }

    @Test
    fun `업적이 강화 밖 갈래를 충분히 담는다`() {
        // 강화만으로 얻을 수 없는 업적이 절반 가까이 되어야 컨텐츠가 이정표를 갖는다.
        val huntAndBeyond = setOf(
            Achievement.HUNT_100, Achievement.HUNT_1000, Achievement.HUNT_10000,
            Achievement.BOSS_FIRST, Achievement.ZONES_ALL, Achievement.EVENT_50,
            Achievement.FUSE_10, Achievement.FUSE_100,
            Achievement.UNIQUE_FIRST, Achievement.UNIQUE_5, Achievement.UNIQUE_ALL,
            Achievement.STAR_3, Achievement.STAR_5, Achievement.STONE_500,
            Achievement.SKILL_100,
            Achievement.GAUNTLET_10, Achievement.GAUNTLET_25, Achievement.GAUNTLET_50,
            Achievement.PET_FIRST, Achievement.PET_ALL,
        )
        assertTrue(huntAndBeyond.size * 2 >= Achievement.entries.size)
    }

    @Test
    fun `이미 얻은 업적은 취소되지 않는다`() {
        val earned = refreshed(stats { copy(monsterKills = 100) })
        // 통계가 초기화되어도(있을 수 없는 일이지만) 업적은 남는다
        val reset = earned.copy(stats = Stats())
        assertTrue(Achievement.HUNT_100 in refreshed(reset).achievements)
    }
}
