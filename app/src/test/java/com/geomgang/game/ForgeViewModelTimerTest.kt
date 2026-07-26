package com.geomgang.game

import com.geomgang.core.Difficulty
import com.geomgang.core.GameState
import com.geomgang.core.Inventory
import com.geomgang.core.SaveStore
import com.geomgang.core.Sword
import com.geomgang.core.Timing
import com.geomgang.core.WeaponFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
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

@OptIn(ExperimentalCoroutinesApi::class)
class ForgeViewModelTimerTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    /**
     * 반드시 실패하고 반드시 파괴되는 난수.
     *
     * ForgeEngine 의 난수 소비 순서 계약(1: 성공 판정, 2: 파괴 판정)에 기댄다.
     * 홀수 번째는 0.99 로 성공 판정을 떨어뜨리고, 짝수 번째는 0.0 으로 파괴를 고른다.
     */
    private fun alwaysDestroy() = object : Random() {
        private var i = 0
        override fun nextBits(bitCount: Int): Int = 0
        override fun nextDouble(): Double = if (i++ % 2 == 0) 0.99 else 0.0
    }

    private fun vm(tickets: Int): ForgeViewModel {
        val store = SaveStore(tmp.root)
        store.saveGame(
            GameState(
                difficulty = Difficulty.NORMAL,
                gold = 1_000_000,
                sword = Sword(WeaponFamily.STRAIGHT, 19),
                inventory = Inventory(preventTickets = tickets),
                bestLevel = 19,
            ),
        )
        return ForgeViewModel(store, Difficulty.NORMAL, alwaysDestroy())
    }

    @Test
    fun `방지권이 있으면 파괴 직후 방지권 창이 열린다`() = runTest(dispatcher) {
        val vm = vm(tickets = 1)
        vm.forge()
        val phase = vm.ui.value.destroyPhase
        assertTrue("phase=$phase", phase is DestroyPhase.Prevent)
        assertEquals(Timing.PREVENT_WINDOW_MILLIS, (phase as DestroyPhase.Prevent).totalMillis)
    }

    @Test
    fun `방지권이 없으면 곧바로 줍기 창이 열린다`() = runTest(dispatcher) {
        val vm = vm(tickets = 0)
        vm.forge()
        assertTrue(vm.ui.value.destroyPhase is DestroyPhase.Salvage)
    }

    @Test
    fun `제한 시간 안에 누르면 검이 되살아난다`() = runTest(dispatcher) {
        val vm = vm(tickets = 1)
        vm.forge()
        advanceTimeBy(1_000)
        vm.usePrevent()
        assertEquals(DestroyPhase.None, vm.ui.value.destroyPhase)
        assertNotNull(vm.ui.value.sword)
        assertEquals(19, vm.ui.value.sword?.level)
        assertEquals(0, vm.ui.value.preventTickets)
    }

    @Test
    fun `제한 시간이 지나면 방지권 창이 닫히고 줍기 창으로 넘어간다`() = runTest(dispatcher) {
        val vm = vm(tickets = 1)
        vm.forge()
        advanceTimeBy(Timing.PREVENT_WINDOW_MILLIS + Timing.TICK_MILLIS * 2)
        assertTrue(
            "phase=${vm.ui.value.destroyPhase}",
            vm.ui.value.destroyPhase is DestroyPhase.Salvage,
        )
        // 놓쳤을 뿐 방지권이 소모되지는 않는다
        assertEquals(1, vm.ui.value.preventTickets)
        assertNull(vm.ui.value.sword)
    }

    @Test
    fun `남은 시간이 줄어든다`() = runTest(dispatcher) {
        val vm = vm(tickets = 1)
        vm.forge()
        val first = (vm.ui.value.destroyPhase as DestroyPhase.Prevent).remainingMillis
        advanceTimeBy(500)
        val later = (vm.ui.value.destroyPhase as DestroyPhase.Prevent).remainingMillis
        assertTrue("$first -> $later", later < first)
    }

    @Test
    fun `줍기를 제한 시간 안에 누르면 조각을 얻는다`() = runTest(dispatcher) {
        val vm = vm(tickets = 0)
        vm.forge()
        advanceTimeBy(500)
        vm.salvage()
        assertEquals(DestroyPhase.None, vm.ui.value.destroyPhase)
        assertTrue("shards=${vm.ui.value.shards}", vm.ui.value.shards > 0)
    }

    @Test
    fun `줍기를 놓치면 아무것도 얻지 못하고 창이 닫힌다`() = runTest(dispatcher) {
        val vm = vm(tickets = 0)
        vm.forge()
        advanceTimeBy(Timing.SALVAGE_WINDOW_MILLIS + Timing.TICK_MILLIS * 2)
        assertEquals(DestroyPhase.None, vm.ui.value.destroyPhase)
        assertEquals(0, vm.ui.value.shards)
        assertNull(vm.ui.value.sword)
    }

    @Test
    fun `창이 닫히면 잠금이 풀린다`() = runTest(dispatcher) {
        val vm = vm(tickets = 0)
        vm.forge()
        advanceTimeBy(Timing.SALVAGE_WINDOW_MILLIS + Timing.TICK_MILLIS * 2)
        assertEquals(false, vm.ui.value.busy)
        assertTrue(vm.ui.value.canBuySword)
    }

    @Test
    fun `창이 열려 있는 동안에는 강화 버튼이 잠긴다`() = runTest(dispatcher) {
        val vm = vm(tickets = 1)
        vm.forge()
        assertTrue(vm.ui.value.busy)
        assertEquals(false, vm.ui.value.canForge)
    }

    @Test
    fun `진행 비율은 0과 1 사이다`() = runTest(dispatcher) {
        val vm = vm(tickets = 1)
        vm.forge()
        repeat(10) {
            val p = vm.ui.value.destroyPhase.progress
            assertTrue("progress=$p", p in 0f..1f)
            advanceTimeBy(200)
        }
    }
}
