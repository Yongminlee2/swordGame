package com.geomgang.game

import com.geomgang.core.DailyQuests
import com.geomgang.core.Difficulty
import com.geomgang.core.GameState
import com.geomgang.core.QuestKind
import com.geomgang.core.SaveStore
import com.geomgang.core.Sword
import com.geomgang.core.WeaponFamily
import com.geomgang.core.Zone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class ForgeViewModelQuestTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private class QueueRandom : Random() {
        override fun nextBits(bitCount: Int): Int = 0
        override fun nextInt(until: Int): Int = 0
        override fun nextDouble(): Double = 1.0
    }

    private fun vm(sword: Sword? = Sword(WeaponFamily.STRAIGHT, 3)): ForgeViewModel {
        val store = SaveStore(tmp.root)
        store.saveGame(GameState(difficulty = Difficulty.ENDLESS, gold = 0, sword = sword))
        return ForgeViewModel(store, Difficulty.ENDLESS, QueueRandom())
    }

    @Test
    fun `첫 로드에 일일 3개와 주간 1개가 배정된다`() = runTest(dispatcher) {
        val ui = vm().ui.value
        assertEquals(DailyQuests.DAILY_COUNT, ui.quests.daily.size)
        assertTrue(ui.quests.weekly != null)
        assertTrue(ui.quests.dateKey.isNotEmpty())
    }

    @Test
    fun `별 강화 미해금이면 별 퀘스트가 나오지 않는다`() = runTest(dispatcher) {
        val ui = vm().ui.value
        assertTrue(ui.quests.daily.none { it.kind == QuestKind.STAR })
    }

    @Test
    fun `잡몹 처치가 처치 퀘스트 진행도를 올린다`() = runTest(dispatcher) {
        val v = vm()
        v.enterZone(Zone.MEADOW)
        v.tapTarget() // +3 직검이 들쥐(체력 16)를 한 방에 잡는다
        val ui = v.ui.value
        v.leaveHunt()
        val killIndex = ui.quests.daily.indexOfFirst { it.kind == QuestKind.KILL }
        if (killIndex >= 0) {
            assertEquals(1, ui.questProgress[killIndex])
        }
        // QueueRandom.nextInt=0 이면 일일 첫 퀘스트가 항상 KILL 이므로 실제로는 항상 존재한다
        assertTrue(killIndex >= 0)
    }

    @Test
    fun `완료한 퀘스트는 수령할 수 있고 이중 수령은 안 된다`() = runTest(dispatcher) {
        // 탭 연타 가드(실제 시각 기준)를 피해서, 이미 완료된 퀘스트를 세이브에 심어 둔다.
        val cal = java.util.Calendar.getInstance()
        val dateKey = "%04d%02d%02d".format(
            cal.get(java.util.Calendar.YEAR),
            cal.get(java.util.Calendar.MONTH) + 1,
            cal.get(java.util.Calendar.DAY_OF_MONTH),
        )
        val weekKey = "%04d-%02d".format(
            cal.get(java.util.Calendar.YEAR),
            cal.get(java.util.Calendar.WEEK_OF_YEAR),
        )
        val store = SaveStore(tmp.root)
        store.saveProgress(
            com.geomgang.core.ProgressState(
                stats = com.geomgang.core.Stats(monsterKills = 100),
            ),
        )
        store.saveGame(
            GameState(
                difficulty = Difficulty.ENDLESS,
                gold = 0,
                sword = Sword(WeaponFamily.STRAIGHT, 3),
                quests = com.geomgang.core.QuestState(
                    dateKey = dateKey,
                    weekKey = weekKey,
                    daily = listOf(
                        com.geomgang.core.QuestInstance(QuestKind.KILL, target = 5, baseline = 95),
                    ),
                ),
            ),
        )
        val v = ForgeViewModel(store, Difficulty.ENDLESS, QueueRandom())

        val before = v.ui.value.gold
        v.claimQuest(0)
        val afterClaim = v.ui.value
        v.claimQuest(0) // 이중 수령 시도 - 조용히 무시돼야 한다
        val afterSecond = v.ui.value

        assertTrue(afterClaim.quests.daily[0].claimed)
        assertEquals(before + DailyQuests.dailyGold(QuestKind.KILL), afterClaim.gold)
        assertEquals(afterClaim.gold, afterSecond.gold)
        assertEquals(
            DailyQuests.DAILY_SHARDS,
            afterClaim.shards,
        )
    }

    @Test
    fun `미완료 퀘스트 수령은 무시된다`() = runTest(dispatcher) {
        val v = vm()
        val before = v.ui.value.gold
        v.claimQuest(0)
        assertEquals(before, v.ui.value.gold)
        assertFalse(v.ui.value.quests.daily[0].claimed)
    }
}
