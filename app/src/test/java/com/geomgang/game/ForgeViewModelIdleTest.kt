package com.geomgang.game

import com.geomgang.core.AdventureState
import com.geomgang.core.Difficulty
import com.geomgang.core.GameState
import com.geomgang.core.IdleRewards
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.random.Random

/**
 * 자리비움 보상 배선.
 *
 * 시계를 직접 쥐고 돌린다 — 실제로 8시간을 기다릴 수는 없고,
 * 여기서 지키려는 것은 "같은 시간을 두 번 받지 않는다" 이기 때문이다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ForgeViewModelIdleTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    /** 원하는 시각을 돌려주는 시계. */
    private class FakeClock(var millis: Long) : () -> Long {
        override fun invoke(): Long = millis

        fun advanceHours(hours: Long) {
            millis += hours * 60 * 60 * 1000
        }
    }

    private fun save(store: SaveStore, gold: Long = 1_000, cleared: Set<String> = emptySet()) {
        store.saveGame(
            GameState(
                difficulty = Difficulty.ENDLESS,
                gold = gold,
                sword = Sword(WeaponFamily.STRAIGHT, 0),
                adventure = AdventureState(clearedZoneIds = cleared),
            ),
        )
    }

    private fun vm(store: SaveStore, clock: FakeClock) =
        ForgeViewModel(store, Difficulty.ENDLESS, Random(1), now = clock)

    @Test
    fun `시각이 없던 옛 세이브에는 보상을 주지 않는다`() = runTest(dispatcher) {
        val store = SaveStore(tmp.root)
        save(store)
        // 방금 저장한 세이브의 lastSeenMillis 는 0 이다 - 얼마나 비웠는지 알 수 없다
        val vm = vm(store, FakeClock(1_000_000_000_000))
        assertNull(vm.ui.value.idleReward)
    }

    @Test
    fun `자리를 비운 만큼 골드와 강화석이 들어온다`() = runTest(dispatcher) {
        val store = SaveStore(tmp.root)
        val clock = FakeClock(1_000_000_000_000)
        save(store, gold = 1_000)

        // 한 번 켜서 시각을 찍는다
        val first = vm(store, clock)
        assertNull(first.ui.value.idleReward)
        val goldBefore = first.ui.value.gold

        clock.advanceHours(3)
        val second = vm(store, clock)

        val reward = second.ui.value.idleReward
        assertNotNull("3시간을 비웠으면 보상이 있어야 한다", reward)
        assertEquals(3 * IdleRewards.STONES_PER_HOUR, reward!!.stones)
        assertEquals(Zone.MEADOW, reward.zone)
        assertEquals(goldBefore + reward.gold, second.ui.value.gold)
        assertEquals(reward.stones, second.ui.value.forgeStones)
    }

    @Test
    fun `같은 시간을 두 번 받지 않는다`() = runTest(dispatcher) {
        val store = SaveStore(tmp.root)
        val clock = FakeClock(1_000_000_000_000)
        save(store)
        vm(store, clock)

        clock.advanceHours(3)
        val second = vm(store, clock)
        val goldAfterFirstClaim = second.ui.value.gold

        // 시계를 그대로 두고 다시 켜면 비운 시간이 0 이다
        val third = vm(store, clock)
        assertNull(third.ui.value.idleReward)
        assertEquals(goldAfterFirstClaim, third.ui.value.gold)
    }

    @Test
    fun `상한을 넘겨 비워도 상한까지만 들어온다`() = runTest(dispatcher) {
        val store = SaveStore(tmp.root)
        val clock = FakeClock(1_000_000_000_000)
        save(store)
        vm(store, clock)

        clock.advanceHours(100)
        val reward = vm(store, clock).ui.value.idleReward
        assertNotNull(reward)
        assertEquals(IdleRewards.MAX_SECONDS, reward!!.seconds)
    }

    @Test
    fun `깬 구역이 깊을수록 많이 들어온다`() = runTest(dispatcher) {
        val shallow = SaveStore(tmp.newFolder("shallow"))
        val deep = SaveStore(tmp.newFolder("deep"))
        val clock = FakeClock(1_000_000_000_000)

        save(shallow, cleared = setOf(Zone.MEADOW.id))
        save(deep, cleared = setOf(Zone.MEADOW.id, Zone.FOREST.id, Zone.CAVE.id))
        vm(shallow, clock)
        vm(deep, clock)

        clock.advanceHours(2)
        val shallowGold = vm(shallow, clock).ui.value.idleReward!!.gold
        val deepGold = vm(deep, clock).ui.value.idleReward!!.gold
        assertTrue("$shallowGold < $deepGold", shallowGold < deepGold)
    }

    @Test
    fun `안내를 닫아도 보상은 남는다`() = runTest(dispatcher) {
        val store = SaveStore(tmp.root)
        val clock = FakeClock(1_000_000_000_000)
        save(store)
        vm(store, clock)

        clock.advanceHours(3)
        val vm = vm(store, clock)
        val gold = vm.ui.value.gold

        vm.dismissIdleReward()
        assertNull(vm.ui.value.idleReward)
        assertEquals(gold, vm.ui.value.gold)
    }
}
