package com.geomgang.game

import com.geomgang.core.Difficulty
import com.geomgang.core.Economy
import com.geomgang.core.ForgeCost
import com.geomgang.core.GameState
import com.geomgang.core.SaveStore
import com.geomgang.core.Sword
import com.geomgang.core.WeaponFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.random.Random

/**
 * **딱 필요한 만큼만 가진 상태로 강화하기.**
 *
 * 실기기에서 +13 이상 강화를 누르면 앱이 죽던 버그의 재발 방지선이다.
 * 재료는 판정 **전에** 태워지는데 판정이 그 재료를 다시 요구해서,
 * 여유분 없이 정확히 맞춰 온 플레이어가 낼 것을 다 내고도 튕겼다.
 *
 * 여유분을 넉넉히 채운 기존 테스트들은 이 버그를 통과시켰다 — 그래서 여기서는
 * **한 자루도 남기지 않는다.**
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ForgeViewModelExactMaterialTest {

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

    /** 항상 실패하는 난수. 성공 경로만 고치고 실패 경로를 놓치는 일을 막는다. */
    private fun alwaysFail() = object : Random() {
        override fun nextBits(bitCount: Int): Int = 0
        override fun nextDouble(): Double = 0.99
    }

    /** [level] 에서 요구하는 재료를 **정확히 그만큼만** 가진 판. */
    private fun exactVm(level: Int, rng: Random = alwaysSucceed()): ForgeViewModel {
        val req = ForgeCost.requirementFor(level)
        val store = SaveStore(tmp.newFolder("lv$level-${rng.hashCode()}"))
        store.saveGame(
            GameState(
                difficulty = Difficulty.ENDLESS,
                gold = req.gold,
                sword = Sword(WeaponFamily.STRAIGHT, level),
                forgeStones = req.stones,
                bestLevel = level,
            ),
        )
        return ForgeViewModel(store, Difficulty.ENDLESS, rng)
    }

    @Test
    fun `강화석 요구 구간에서 재료를 딱 맞춰도 강화가 실행된다`() = runTest(dispatcher) {
        // 목표 +16 - 강화석이 처음 필수가 되는 단계
        val level = ForgeCost.STONE_BAND_START - 1
        val vm = exactVm(level)
        assertTrue("딱 맞춘 상태는 강화할 수 있어야 한다", vm.ui.value.canForge)

        vm.forge()

        assertEquals(level + 1, vm.ui.value.sword?.level)
        assertEquals(0, vm.ui.value.forgeStones)
    }

    @Test
    fun `실패해도 튕기지 않는다`() = runTest(dispatcher) {
        val level = ForgeCost.STONE_BAND_START + 2
        val vm = exactVm(level, rng = alwaysFail())
        vm.forge()

        // 결과가 무엇이든 예외 없이 한 판이 끝나 있어야 한다
        assertNotNull(vm.ui.value.lastResult ?: vm.ui.value.destroyPhase)
        assertEquals(0, vm.ui.value.forgeStones)
    }

    @Test
    fun `재료 요구 전 구간을 딱 맞춰 굴려도 튕기지 않는다`() = runTest(dispatcher) {
        // 재료가 붙는 모든 단계를 훑는다. 한 구간이라도 어긋나면 여기서 잡힌다.
        for (level in ForgeCost.STONE_BAND_START - 1..ForgeCost.ENDLESS_BAND_START + 4) {
            val vm = exactVm(level)
            vm.forge()
            assertEquals("level=$level", level + 1, vm.ui.value.sword?.level)
        }
    }

    // --- 초기화 ---

    @Test
    fun `초기화하면 검을 살 수 있는 자금이 생긴다`() = runTest(dispatcher) {
        val store = SaveStore(tmp.newFolder("reset"))
        store.saveGame(
            GameState(
                difficulty = Difficulty.ENDLESS,
                gold = 999_999,
                sword = Sword(WeaponFamily.STRAIGHT, 9),
            ),
        )
        val vm = ForgeViewModel(store, Difficulty.ENDLESS, alwaysSucceed())

        vm.resetProgress()

        val ui = vm.ui.value
        assertEquals("검이 사라져야 한다", null, ui.sword)
        assertTrue(
            "골드 ${ui.gold} 로는 검을 살 수 없다 - 아무것도 못 하는 판이 된다",
            ui.gold >= Economy.BASE_SWORD_PRICE,
        )
        assertTrue(ui.canBuySword)
    }

    @Test
    fun `초기화한 상태가 저장까지 된다`() = runTest(dispatcher) {
        val dir = tmp.newFolder("reset-persist")
        val store = SaveStore(dir)
        store.saveGame(
            GameState(
                difficulty = Difficulty.ENDLESS,
                gold = 999_999,
                sword = Sword(WeaponFamily.STRAIGHT, 9),
            ),
        )
        ForgeViewModel(store, Difficulty.ENDLESS, alwaysSucceed()).resetProgress()

        // 다시 켰을 때도 자금이 남아 있어야 한다
        val reopened = ForgeViewModel(SaveStore(dir), Difficulty.ENDLESS, alwaysSucceed())
        assertTrue(reopened.ui.value.gold >= Economy.BASE_SWORD_PRICE)
    }
}
