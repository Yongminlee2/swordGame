package com.geomgang.game

import com.geomgang.core.Difficulty
import com.geomgang.core.GameState
import com.geomgang.core.Inventory
import com.geomgang.core.SaveStore
import com.geomgang.core.Sword
import com.geomgang.core.WeaponFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class ForgeViewModelAutoTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun alwaysSucceed() = object : Random() {
        override fun nextBits(bitCount: Int): Int = 0
        override fun nextDouble(): Double = 0.0
    }

    /** 항상 실패하고 항상 파괴되는 난수. */
    private fun alwaysDestroy() = object : Random() {
        private var i = 0
        override fun nextBits(bitCount: Int): Int = 0
        override fun nextDouble(): Double = if (i++ % 2 == 0) 0.99 else 0.0
    }

    private fun store() = SaveStore(tmp.root)

    private fun vm(
        level: Int = 0,
        gold: Long = 1_000_000,
        tickets: Int = 0,
        rng: Random = alwaysSucceed(),
        store: SaveStore = store(),
    ): ForgeViewModel {
        store.saveGame(
            GameState(
                difficulty = Difficulty.NORMAL,
                gold = gold,
                sword = Sword(WeaponFamily.STRAIGHT, level),
                inventory = Inventory(preventTickets = tickets),
                bestLevel = level,
            ),
        )
        return ForgeViewModel(store, Difficulty.NORMAL, rng)
    }

    // --- 자동강화 ---

    @Test
    fun `자동강화가 목표 단계에서 멈춘다`() = runTest(dispatcher) {
        val vm = vm(level = 0)
        vm.startAutoForge(targetLevel = 4)
        advanceUntilIdle()
        assertEquals(4, vm.ui.value.sword?.level)
        assertFalse(vm.ui.value.autoForging)
    }

    @Test
    fun `자동강화는 안전구간을 벗어나면 스스로 멈춘다`() = runTest(dispatcher) {
        // 목표를 +20 으로 걸어도 안전구간 끝(+5)에서 멈춰야 한다
        val vm = vm(level = 0)
        vm.startAutoForge(targetLevel = 20)
        advanceUntilIdle()
        assertEquals(5, vm.ui.value.sword?.level)
        assertFalse(vm.ui.value.autoForging)
    }

    @Test
    fun `골드가 부족하면 자동강화가 멈춘다`() = runTest(dispatcher) {
        // +0 강화 비용이 30, +1 은 44 다. 70골드로는 목표 +5 에 닿을 수 없다.
        val vm = vm(level = 0, gold = 70)
        vm.startAutoForge(targetLevel = 5)
        advanceUntilIdle()

        assertFalse(vm.ui.value.autoForging)
        assertTrue("목표에 닿지 못하고 멈춰야 한다", (vm.ui.value.sword?.level ?: 0) < 5)
        // 다음 시도 비용을 못 내는 상태로 멈춰 있어야 한다
        assertTrue(
            "gold=${vm.ui.value.gold} cost=${vm.ui.value.upgradeCost}",
            vm.ui.value.gold < vm.ui.value.upgradeCost,
        )
    }

    @Test
    fun `이미 안전구간을 벗어나 있으면 자동강화를 켤 수 없다`() = runTest(dispatcher) {
        val vm = vm(level = 8)
        assertFalse(vm.ui.value.canAutoForge)
        vm.startAutoForge(targetLevel = 12)
        advanceUntilIdle()
        assertEquals(8, vm.ui.value.sword?.level)
    }

    @Test
    fun `자동강화를 멈출 수 있다`() = runTest(dispatcher) {
        val vm = vm(level = 0)
        vm.startAutoForge(targetLevel = 5)
        advanceTimeBy(250)
        vm.stopAutoForge()
        val stopped = vm.ui.value.sword?.level ?: 0
        advanceUntilIdle()
        assertFalse(vm.ui.value.autoForging)
        assertEquals(stopped, vm.ui.value.sword?.level)
    }

    @Test
    fun `자동강화 중에는 손으로 강화할 수 없다`() = runTest(dispatcher) {
        val vm = vm(level = 0)
        vm.startAutoForge(targetLevel = 5)
        val before = vm.ui.value.sword?.level
        vm.forge()
        assertEquals(before, vm.ui.value.sword?.level)
    }

    // --- 방지권 자동사용 ---

    @Test
    fun `자동사용이 켜져 있으면 제한 시간 창을 열지 않고 되살린다`() = runTest(dispatcher) {
        val store = store()
        store.saveSettings(com.geomgang.core.Settings(autoPrevent = true))
        val vm = vm(level = 19, tickets = 1, rng = alwaysDestroy(), store = store)
        vm.forge()
        assertEquals(DestroyPhase.None, vm.ui.value.destroyPhase)
        assertNotNull(vm.ui.value.sword)
        assertEquals(19, vm.ui.value.sword?.level)
        assertEquals(0, vm.ui.value.preventTickets)
    }

    @Test
    fun `자동사용이 꺼져 있으면 창이 열린다`() = runTest(dispatcher) {
        val vm = vm(level = 19, tickets = 1, rng = alwaysDestroy())
        vm.forge()
        assertTrue(vm.ui.value.destroyPhase is DestroyPhase.Prevent)
    }

    // --- 설정 ---

    @Test
    fun `설정이 저장되고 다시 켜도 유지된다`() = runTest(dispatcher) {
        val store = store()
        val first = vm(store = store)
        first.setAutoPrevent(true)
        assertTrue(first.ui.value.settings.autoPrevent)

        val second = ForgeViewModel(store, Difficulty.NORMAL, alwaysSucceed())
        assertTrue(second.ui.value.settings.autoPrevent)
    }

    @Test
    fun `설정은 모드 초기화의 영향을 받지 않는다`() = runTest(dispatcher) {
        val store = store()
        vm(store = store).setAutoPrevent(true)
        store.resetGame(Difficulty.NORMAL)
        assertTrue(store.loadSettings().autoPrevent)
    }

    // --- 칭호 ---

    @Test
    fun `달성한 업적의 칭호를 고를 수 있다`() = runTest(dispatcher) {
        val vm = vm(level = 4)
        vm.startAutoForge(targetLevel = 5)
        advanceUntilIdle()
        val earned = vm.ui.value.progress.achievements.first()
        vm.selectTitle(earned)
        assertEquals(earned, vm.ui.value.progress.selectedTitle)
    }

    @Test
    fun `진행도가 화면 상태에 노출된다`() = runTest(dispatcher) {
        val vm = vm(level = 0)
        vm.startAutoForge(targetLevel = 3)
        advanceUntilIdle()
        assertTrue(vm.ui.value.progress.stats.attempts > 0)
        assertTrue(vm.ui.value.progress.codex.isNotEmpty())
    }
}
