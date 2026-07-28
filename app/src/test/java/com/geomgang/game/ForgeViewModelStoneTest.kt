package com.geomgang.game

import com.geomgang.core.Difficulty
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.random.Random

/** 강화석과 필수 재료가 강화 화면·강화 실행에 배선됐는지. */
@OptIn(ExperimentalCoroutinesApi::class)
class ForgeViewModelStoneTest {

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
        stones: Int,
        storage: Int,
        rng: Random = alwaysSucceed(),
    ): ForgeViewModel {
        val store = SaveStore(tmp.root)
        store.saveGame(
            GameState(
                difficulty = Difficulty.ENDLESS,
                gold = 1_000_000_000,
                sword = Sword(WeaponFamily.STRAIGHT, level),
                storage = List(storage) { Sword(WeaponFamily.STRAIGHT, 1) },
                forgeStones = stones,
            ),
        )
        return ForgeViewModel(store, Difficulty.ENDLESS, rng)
    }

    @Test
    fun `강화석이 없으면 고단계 강화 버튼이 잠긴다`() {
        val ui = vm(level = 15, stones = 0, storage = 5).ui.value
        assertFalse(ui.canForge)
        assertNotNull(ui.forgeBlockedReason)
    }

    @Test
    fun `요구량이 화면에 실린다`() {
        val ui = vm(level = 15, stones = 50, storage = 5).ui.value
        assertEquals(ForgeCost.requirementFor(15).stones, ui.requiredStones)
        assertEquals(50, ui.forgeStones)
    }

    @Test
    fun `저단계는 요구가 없다`() {
        val ui = vm(level = 3, stones = 0, storage = 0).ui.value
        assertTrue(ui.canForge)
        assertEquals(0, ui.requiredStones)
    }

    @Test
    fun `강화하면 강화석만 빠진다`() = runTest(dispatcher) {
        // v1.8: 보관함의 검은 강화가 건드리지 않는다. 조합 재료로 남는다.
        val v = vm(level = 15, stones = 50, storage = 5)
        val req = ForgeCost.requirementFor(15)
        v.forge()
        val ui = v.ui.value
        assertEquals(50 - req.stones, ui.forgeStones)
        assertEquals("보관함은 그대로여야 한다", 5, ui.storage.size)
    }

    @Test
    fun `저단계 강화는 아무것도 태우지 않는다`() = runTest(dispatcher) {
        val v = vm(level = 3, stones = 10, storage = 5)
        v.forge()
        val ui = v.ui.value
        assertEquals(10, ui.forgeStones)
        assertEquals(5, ui.storage.size)
    }

    @Test
    fun `보관함이 비어도 고단계 강화가 된다`() = runTest(dispatcher) {
        val v = vm(level = 20, stones = 50, storage = 0)
        assertTrue(v.ui.value.canForge)
        v.forge()
        assertEquals(21, v.ui.value.sword?.level)
    }

    @Test
    fun `분해하면 강화석이 들어온다`() = runTest(dispatcher) {
        val v = vm(level = 3, stones = 0, storage = 2)
        v.scrapFromStorage(0)
        assertTrue(v.ui.value.forgeStones > 0)
    }
}
