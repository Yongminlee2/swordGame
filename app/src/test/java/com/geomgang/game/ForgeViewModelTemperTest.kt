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
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.random.Random

/** 담금질·자취·신기록이 화면 상태까지 오는지. */
@OptIn(ExperimentalCoroutinesApi::class)
class ForgeViewModelTemperTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    /** 항상 성공하는 난수. */
    private fun alwaysSucceed() = object : Random() {
        override fun nextBits(bitCount: Int): Int = 0
        override fun nextDouble(): Double = 0.0
    }

    private fun vm(
        level: Int,
        inventory: Inventory = Inventory(),
        rng: Random = alwaysSucceed(),
    ): ForgeViewModel {
        val store = SaveStore(tmp.root)
        store.saveGame(
            GameState(
                difficulty = Difficulty.ENDLESS,
                gold = 1_000_000_000_000_000L,
                sword = Sword(WeaponFamily.STRAIGHT, level),
                inventory = inventory,
                forgeStones = 9_999,
                bestLevel = level,
            ),
        )
        return ForgeViewModel(store, Difficulty.ENDLESS, rng)
    }

    @Test
    fun `담금질이 붙지 않는 구간에서는 표시가 없다`() = runTest(dispatcher) {
        // 손에 든 검이 +10 이면 목표가 +11 이라 담금질이 붙지 않는다
        assertNull(vm(10).ui.value.temper)
    }

    @Test
    fun `무한 구간에서는 담금질 표시가 나온다`() = runTest(dispatcher) {
        val temper = requireNotNull(vm(30).ui.value.temper)
        assertEquals(0, temper.fails)
        assertTrue("ratio=${temper.ratio}", temper.ratio in 0f..1f)
    }

    /** v2.1: 축복서와 부적은 함께 켤 수 있다. 값(골드)이 선택을 가른다. */
    @Test
    fun `축복서와 부적을 함께 켤 수 있다`() = runTest(dispatcher) {
        val vm = vm(3, Inventory(blessingScrolls = 1, luckCharms = 1))
        vm.toggleLuckCharm()
        vm.toggleBlessing()
        assertTrue(vm.ui.value.useBlessing)
        assertTrue(vm.ui.value.useLuckCharm)
    }

    @Test
    fun `낮은 단계 성공은 신기록으로 치지 않는다`() = runTest(dispatcher) {
        val vm = vm(0)
        vm.forge()
        assertFalse(vm.ui.value.isRecord)
    }

    @Test
    fun `문턱 위에서 최고를 넘기면 신기록이다`() = runTest(dispatcher) {
        // +12 에서 성공하면 +13 이고, 이는 이 세이브의 최고(+12)를 넘는다
        val vm = vm(12)
        vm.forge()
        assertTrue(vm.ui.value.isRecord)
    }
}
