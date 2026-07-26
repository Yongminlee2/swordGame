package com.geomgang.game

import com.geomgang.core.Difficulty
import com.geomgang.core.GameState
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

/**
 * 사냥 타격 배선 - 치명타 롤, hitSeq, 희귀 표시, 처치 골드.
 *
 * 난수 소비 순서가 계약이다:
 * enterZone -> spawnNext: nextInt(몬스터 종류), nextDouble(희귀)
 * tapTarget: nextDouble(치명타)
 * 처치 시: nextDouble(검 드롭) -> spawnNext(nextInt, nextDouble)
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ForgeViewModelHuntTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    /**
     * 정해 둔 값을 순서대로 돌려주는 난수. 바닥나면 "아무 일도 안 일어나는" 기본값
     * (희귀 없음·치명타 없음·드롭 없음)으로 떨어진다.
     */
    private class QueueRandom(
        ints: List<Int> = emptyList(),
        doubles: List<Double> = emptyList(),
    ) : Random() {
        private val intQueue = ArrayDeque(ints)
        private val doubleQueue = ArrayDeque(doubles)
        override fun nextBits(bitCount: Int): Int = 0
        override fun nextInt(until: Int): Int =
            if (intQueue.isEmpty()) 0 else intQueue.removeFirst() % until
        override fun nextDouble(): Double =
            if (doubleQueue.isEmpty()) 1.0 else doubleQueue.removeFirst()
    }

    private fun huntReadyViewModel(
        rng: Random = QueueRandom(),
        sword: Sword = Sword(WeaponFamily.STRAIGHT, 3),
    ): ForgeViewModel {
        val store = SaveStore(tmp.root)
        store.saveGame(GameState(difficulty = Difficulty.ENDLESS, gold = 0, sword = sword))
        val vm = ForgeViewModel(store, Difficulty.ENDLESS, rng)
        vm.enterZone(Zone.MEADOW)
        return vm
    }

    // 규칙: 상태를 캡처하고 leaveHunt() 로 루프를 멈춘 **뒤에** 단언한다.
    // 단언이 leaveHunt 앞에 있으면, 실패 시 예외가 leaveHunt 를 건너뛰어
    // 사냥 루프(1초 delay 반복)가 살아남고 runTest 의 가상 시간 정리가 영원히 돈다 -
    // 테스트 실패가 행(hang)으로 둔갑한다.

    @Test
    fun `탭마다 hitSeq가 1씩 오른다`() = runTest(dispatcher) {
        val vm = huntReadyViewModel()
        val before = vm.ui.value.hunt!!.hitSeq
        vm.tapTarget()
        val after = vm.ui.value.hunt!!.hitSeq
        vm.leaveHunt()
        assertEquals(before + 1, after)
    }

    @Test
    fun `치명타 롤이 낮으면 lastCrit이 참이다`() = runTest(dispatcher) {
        // +0 검(공격력 6)은 들쥐(체력 16)를 못 잡는다 - 잡으면 spawnNext 가 lastCrit 을 지운다.
        // doubles: 희귀 판정 0.5(희귀 아님) -> 치명타 판정 0.0(치명타)
        val vm = huntReadyViewModel(
            rng = QueueRandom(doubles = listOf(0.5, 0.0)),
            sword = Sword(WeaponFamily.STRAIGHT, 0),
        )
        vm.tapTarget()
        val hunt = vm.ui.value.hunt!!
        vm.leaveHunt()
        assertTrue(hunt.lastCrit)
    }

    @Test
    fun `치명타 롤이 높으면 lastCrit이 거짓이다`() = runTest(dispatcher) {
        val vm = huntReadyViewModel(
            rng = QueueRandom(doubles = listOf(0.5, 0.9)),
            sword = Sword(WeaponFamily.STRAIGHT, 0),
        )
        vm.tapTarget()
        val hunt = vm.ui.value.hunt!!
        vm.leaveHunt()
        assertFalse(hunt.lastCrit)
    }

    @Test
    fun `희귀 몬스터면 isRare가 참이다`() = runTest(dispatcher) {
        val vm = huntReadyViewModel(rng = QueueRandom(doubles = listOf(0.01)))
        val hunt = vm.ui.value.hunt!!
        vm.leaveHunt()
        assertTrue(hunt.isRare)
    }

    @Test
    fun `잡몹을 잡으면 lastKillGold에 보상이 남는다`() = runTest(dispatcher) {
        // +3 직검(공격력 20)이 들쥐(체력 16)를 한 방에 잡는다
        val vm = huntReadyViewModel()
        vm.tapTarget()
        val hunt = vm.ui.value.hunt!!
        vm.leaveHunt()
        assertEquals(Zone.MEADOW.goldOf(Zone.MEADOW.monsters.first()), hunt.lastKillGold)
    }

    @Test
    fun `표시 이름에는 희귀 접두어가 붙고 rawTargetName에는 안 붙는다`() = runTest(dispatcher) {
        val vm = huntReadyViewModel(rng = QueueRandom(doubles = listOf(0.01)))
        val hunt = vm.ui.value.hunt!!
        vm.leaveHunt()
        assertTrue(hunt.targetName.startsWith("희귀 "))
        assertEquals(Zone.MEADOW.monsters.first().name, hunt.rawTargetName)
    }
}
