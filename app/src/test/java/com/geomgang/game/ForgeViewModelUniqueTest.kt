package com.geomgang.game

import com.geomgang.core.Difficulty
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class ForgeViewModelUniqueTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private class ZeroRandom : Random() {
        override fun nextBits(bitCount: Int): Int = 0
        override fun nextInt(until: Int): Int = 0
        override fun nextDouble(): Double = 1.0
    }

    @Test
    fun `레시피 재료를 조합하면 고유검이 나오고 도감에 등록된다`() = runTest(dispatcher) {
        val store = SaveStore(tmp.root)
        store.saveGame(
            GameState(
                difficulty = Difficulty.ENDLESS,
                gold = 1_000_000,
                sword = Sword(WeaponFamily.STRAIGHT, 3),
                storage = listOf(
                    Sword(WeaponFamily.HOLY, 10),
                    Sword(WeaponFamily.HOLY, 12),
                    Sword(WeaponFamily.HOLY, 11),
                ),
            ),
        )
        val vm = ForgeViewModel(store, Difficulty.ENDLESS, ZeroRandom())
        vm.fuse(listOf(0, 1, 2))
        val ui = vm.ui.value
        val result = ui.storage.single()
        assertEquals("trinity", result.uniqueId)
        assertEquals(12, result.level)
        assertTrue("도감 미등록", "trinity" in ui.progress.uniqueFound)
    }
}
