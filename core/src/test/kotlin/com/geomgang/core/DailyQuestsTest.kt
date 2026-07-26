package com.geomgang.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class DailyQuestsTest {

    /** nextInt 만 순서대로 돌려주는 난수. 퀘스트 추첨은 nextInt 만 쓴다. */
    private class IntRandom(private vararg val ints: Int) : Random() {
        private var i = 0
        override fun nextBits(bitCount: Int): Int = 0
        override fun nextInt(until: Int): Int =
            if (i < ints.size) ints[i++] % until else 0
    }

    private val stats = Stats(monsterKills = 100, attempts = 50)

    private fun fresh(dateKey: String = "20260727", weekKey: String = "2026-31") =
        DailyQuests.refresh(QuestState(), stats, dateKey, weekKey, IntRandom(0, 1, 2))

    // --- 배정 ---

    @Test
    fun `날짜가 바뀌면 일일 3개를 새로 뽑는다`() {
        val state = fresh()
        assertEquals("20260727", state.dateKey)
        assertEquals(DailyQuests.DAILY_COUNT, state.daily.size)
    }

    @Test
    fun `일일 퀘스트의 종류는 겹치지 않는다`() {
        val kinds = fresh().daily.map { it.kind }
        assertEquals(kinds.size, kinds.toSet().size)
    }

    @Test
    fun `같은 날짜면 그대로 둔다`() {
        val first = fresh()
        val again = DailyQuests.refresh(first, stats, "20260727", "2026-31", IntRandom(5, 6))
        assertEquals(first, again)
    }

    @Test
    fun `주간은 주 키가 바뀔 때만 새로 뽑는다`() {
        val first = fresh()
        assertNotNull(first.weekly)
        val nextDay = DailyQuests.refresh(first, stats, "20260728", "2026-31", IntRandom(0, 1, 2))
        assertEquals(first.weekly, nextDay.weekly)
        val nextWeek = DailyQuests.refresh(nextDay, stats, "20260803", "2026-32", IntRandom(0, 1, 2))
        assertTrue(first.weekly !== nextWeek.weekly)
    }

    @Test
    fun `배정 시점 카운터가 기준선이 된다`() {
        val state = fresh()
        for (q in state.daily) {
            assertEquals(DailyQuests.counterOf(q.kind, stats), q.baseline)
        }
    }

    @Test
    fun `풀에서 뺀 종류는 나오지 않는다`() {
        val pool = QuestKind.entries.filter { it != QuestKind.STAR }
        val state = DailyQuests.refresh(
            QuestState(), stats, "20260727", "2026-31", IntRandom(0, 1, 2), pool,
        )
        assertTrue(state.daily.none { it.kind == QuestKind.STAR })
    }

    // --- 진행도 ---

    @Test
    fun `진행도는 기준선 차분이고 목표를 넘지 않는다`() {
        val q = QuestInstance(QuestKind.KILL, target = 10, baseline = 100)
        assertEquals(0, DailyQuests.progressOf(q, stats))
        val later = stats.copy(monsterKills = 107)
        assertEquals(7, DailyQuests.progressOf(q, later))
        val over = stats.copy(monsterKills = 200)
        assertEquals(10, DailyQuests.progressOf(q, over))
        assertTrue(DailyQuests.isDone(q, over))
    }

    // --- 수령 ---

    @Test
    fun `완료한 일일 퀘스트를 수령하면 골드와 조각이 들어온다`() {
        val q = QuestInstance(QuestKind.KILL, target = 5, baseline = 100)
        val game = GameState(
            difficulty = Difficulty.ENDLESS,
            quests = QuestState(dateKey = "20260727", daily = listOf(q)),
        )
        val done = stats.copy(monsterKills = 105)
        val claimed = DailyQuests.claim(game, done, 0)
        assertEquals(DailyQuests.dailyGold(QuestKind.KILL), claimed.gold)
        assertEquals(DailyQuests.DAILY_SHARDS, claimed.shards)
        assertTrue(claimed.quests.daily[0].claimed)
    }

    @Test
    fun `미완료 퀘스트는 수령할 수 없다`() {
        val q = QuestInstance(QuestKind.KILL, target = 5, baseline = 100)
        val game = GameState(
            difficulty = Difficulty.ENDLESS,
            quests = QuestState(dateKey = "20260727", daily = listOf(q)),
        )
        var thrown = false
        try {
            DailyQuests.claim(game, stats, 0)
        } catch (e: IllegalStateException) {
            thrown = true
        }
        assertTrue(thrown)
    }

    @Test
    fun `이중 수령은 막힌다`() {
        val q = QuestInstance(QuestKind.KILL, target = 5, baseline = 100, claimed = true)
        val game = GameState(
            difficulty = Difficulty.ENDLESS,
            quests = QuestState(dateKey = "20260727", daily = listOf(q)),
        )
        val done = stats.copy(monsterKills = 105)
        var thrown = false
        try {
            DailyQuests.claim(game, done, 0)
        } catch (e: IllegalStateException) {
            thrown = true
        }
        assertTrue(thrown)
    }

    @Test
    fun `주간 수령은 인덱스 -1 이고 조각이 더 크다`() {
        val weekly = QuestInstance(QuestKind.KILL, target = 5, baseline = 100)
        val game = GameState(
            difficulty = Difficulty.ENDLESS,
            quests = QuestState(weekKey = "2026-31", weekly = weekly),
        )
        val done = stats.copy(monsterKills = 105)
        val claimed = DailyQuests.claim(game, done, -1)
        assertEquals(DailyQuests.WEEKLY_SHARDS, claimed.shards)
        assertTrue(claimed.quests.weekly!!.claimed)
        assertFalse(DailyQuests.WEEKLY_SHARDS <= DailyQuests.DAILY_SHARDS)
    }
}
