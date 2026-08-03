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
class ForgeViewModelSettingsTest {

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
                // 고단계 테스트는 재료 검·강화석이 필수다(ForgeCost). 넉넉히 채워 둔다.
                storage = List(4) { Sword(WeaponFamily.STRAIGHT, 1) },
                forgeStones = 100,
            ),
        )
        return ForgeViewModel(store, Difficulty.NORMAL, rng)
    }

    /**
     * 손으로 [times] 번 굴린다.
     *
     * 강화 한 번은 연출 잠금을 건다. 화면이 연출을 끝내면서 그 잠금을 푸는데,
     * 테스트에는 화면이 없으므로 여기서 대신 풀어 준다.
     */
    private fun ForgeViewModel.forgeTimes(times: Int) {
        repeat(times) {
            forge()
            onAnimationFinished()
        }
    }

    // --- 연출 잠금 ---

    @Test
    fun `연출이 끝나기 전에는 다음 강화가 먹히지 않는다`() = runTest(dispatcher) {
        val vm = vm(level = 0)
        vm.forge()
        val locked = vm.ui.value.sword?.level
        vm.forge()
        assertEquals("연출 중에는 잠겨 있어야 한다", locked, vm.ui.value.sword?.level)

        vm.onAnimationFinished()
        vm.forge()
        assertEquals((locked ?: 0) + 1, vm.ui.value.sword?.level)
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
        assertTrue(vm.ui.value.destroyPhase is DestroyPhase.Choice)
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
        vm.forgeTimes(1)
        val earned = vm.ui.value.progress.achievements.first()
        vm.selectTitle(earned)
        assertEquals(earned, vm.ui.value.progress.selectedTitle)
    }

    @Test
    fun `진행도가 화면 상태에 노출된다`() = runTest(dispatcher) {
        val vm = vm(level = 0)
        vm.forgeTimes(3)
        assertTrue(vm.ui.value.progress.stats.attempts > 0)
    }

    /**
     * 강화만으로는 도감이 오르지 않는다. 바쳐야 열린다.
     *
     * 저절로 오르면 도감이 "지나간 자취" 일 뿐 아무 결정도 요구하지 않는다.
     */
    @Test
    fun `강화만으로는 도감이 오르지 않는다`() = runTest(dispatcher) {
        val vm = vm(level = 0)
        vm.forgeTimes(3)
        assertTrue(vm.ui.value.progress.codex.isEmpty())
    }
}
