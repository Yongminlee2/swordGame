package com.geomgang.game

import com.geomgang.core.DecodedSave
import com.geomgang.core.Difficulty
import com.geomgang.core.GameState
import com.geomgang.core.SaveCodec
import com.geomgang.core.SaveStore
import com.geomgang.core.Sword
import com.geomgang.core.WeaponFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class ForgeViewModelAsyncSaveTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `강화 결과는 암호화 저장을 기다리지 않고 먼저 화면에 반영된다`() {
        val codec = CountingCodec()
        val store = SaveStore(tmp.root, codec)
        store.saveGame(
            GameState(
                difficulty = Difficulty.ENDLESS,
                gold = 1_000_000,
                sword = Sword(WeaponFamily.STRAIGHT, 0),
            ),
        )
        val vm = ForgeViewModel(
            store = store,
            difficulty = Difficulty.ENDLESS,
            rng = AlwaysSucceed,
            now = { 1_000L },
            saveDispatcher = dispatcher,
        )
        dispatcher.scheduler.advanceUntilIdle()
        val writesAfterOpen = codec.encodeCalls

        vm.forge()

        assertEquals(1, vm.ui.value.sword?.level)
        assertEquals(writesAfterOpen, codec.encodeCalls)

        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, store.loadGame(Difficulty.ENDLESS).sword?.level)
        assertEquals(writesAfterOpen + 2, codec.encodeCalls)
    }

    private class CountingCodec : SaveCodec {
        var encodeCalls: Int = 0
            private set

        override fun encode(fileName: String, plaintext: ByteArray): ByteArray {
            encodeCalls += 1
            return plaintext
        }

        override fun decode(fileName: String, stored: ByteArray): DecodedSave = DecodedSave(stored)
    }

    private object AlwaysSucceed : Random() {
        override fun nextBits(bitCount: Int): Int = 0
    }
}
