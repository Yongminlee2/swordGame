package com.geomgang.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 고유검은 한 번 발견한 사실 자체가 보너스다. 들고 있지 않아도 된다.
 *
 * 발견은 이미 영구 기록(`uniqueFound`)이라 새 저장 필드가 필요 없다.
 */
class UniqueBonusTest {

    @Test
    fun `하나도 없으면 0이다`() {
        assertEquals(0.0, UniqueSwords.holdingBonus(ProgressState()).successRate, 1e-9)
    }

    @Test
    fun `한 종류당 같은 몫이 붙는다`() {
        val three = ProgressState(uniqueFound = UniqueSwords.RECIPES.take(3).map { it.id }.toSet())
        assertEquals(UniqueSwords.PER_UNIQUE * 3, UniqueSwords.holdingBonus(three).successRate, 1e-9)
    }

    @Test
    fun `전부 모으면 상한이다`() {
        val all = ProgressState(uniqueFound = UniqueSwords.RECIPES.map { it.id }.toSet())
        val expected = UniqueSwords.PER_UNIQUE * UniqueSwords.RECIPES.size
        assertEquals(expected, UniqueSwords.holdingBonus(all).successRate, 1e-9)
        assertEquals(expected, UniqueSwords.holdingBonus(all).destroyGuard, 1e-9)
    }

    /** 모르는 id 가 섞여 있어도 실제 고유검만 센다. */
    @Test
    fun `모르는 id 는 세지 않는다`() {
        val junk = ProgressState(uniqueFound = setOf("없는것", "이상한것"))
        assertEquals(0.0, UniqueSwords.holdingBonus(junk).successRate, 1e-9)
    }

    @Test
    fun `고유검이 강화 보너스 출처에 들어간다`() {
        val some = ProgressState(uniqueFound = UniqueSwords.RECIPES.take(2).map { it.id }.toSet())
        val fresh = GameState(Difficulty.ENDLESS)
        assertTrue(ForgeBonuses.of(fresh, some).successRate > 0.0)
        assertTrue(ForgeBonuses.sourcesOf(fresh, some).any { it.label == "고유검" })
    }
}
