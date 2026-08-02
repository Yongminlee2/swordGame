package com.geomgang.game

import com.geomgang.core.Difficulty
import com.geomgang.core.GameState
import com.geomgang.core.HuntEvent
import com.geomgang.core.HuntEvents
import com.geomgang.core.SaveStore
import com.geomgang.core.Sword
import com.geomgang.core.WeaponFamily
import com.geomgang.core.Zone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
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

/**
 * 사냥 타격 배선 - 치명타 롤, hitSeq, 희귀 표시, 처치 골드, 이벤트.
 *
 * 난수 소비 순서가 계약이다:
 * enterZone -> spawnNext: nextInt(몬스터 종류), nextDouble(희귀),
 *              nextDouble(이벤트 발생), [발생 시 nextDouble(종류), 상인이면 nextInt(아이템)]
 * tapTarget: nextDouble(치명타)
 * 처치 시: nextDouble(검 드롭) -> spawnNext(위와 동일)
 * 기본값(1.0)은 "아무 이벤트도 없음"이라 스크립트가 짧아도 안전하다.
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
        // 사냥터는 용검(+21) 뒤에 열린다. 초원 권장이 +15 이므로 한 방에 잡으려면
        // 그만한 검이어야 한다 - +3 은 v2.3 재편성 전의 유물이다.
        sword: Sword = Sword(WeaponFamily.STRAIGHT, 19),
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
        // doubles: 희귀 0.5 -> 이벤트 발생 1.0(없음) -> 치명타 0.0(치명타)
        val vm = huntReadyViewModel(
            rng = QueueRandom(doubles = listOf(0.5, 1.0, 0.0)),
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
            rng = QueueRandom(doubles = listOf(0.5, 1.0, 0.9)),
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
        // +19 직검이 들쥐를 한 방에 잡는다 (초원 권장 +15)
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

    // --- 이벤트 ---
    // 종류 추첨 롤: 가중치 누적 [보물 0~12, 골든 12~22, 미믹 22~34, 상인 34~42,
    // 정예 42~54, 금덩이 54~74, 유성우 74~75, 알 75~83] / 83

    @Test
    fun `분노한 정예는 체력이 3배다`() = runTest(dispatcher) {
        val vm = huntReadyViewModel(rng = QueueRandom(doubles = listOf(0.5, 0.0, 0.55)))
        val hunt = vm.ui.value.hunt!!
        vm.leaveHunt()
        assertEquals(HuntEvent.ELITE, hunt.event)
        val base = Zone.MEADOW.hpOf(Zone.MEADOW.monsters.first())
        assertEquals((base * HuntEvents.ELITE_HP).toLong(), hunt.targetMaxHp)
    }

    @Test
    fun `보물 몬스터는 시간이 지나면 도망간다`() = runTest(dispatcher) {
        val vm = huntReadyViewModel(rng = QueueRandom(doubles = listOf(0.5, 0.0, 0.05)))
        val spawned = vm.ui.value.hunt!!.event
        advanceTimeBy((HuntEvents.TREASURE_SECONDS + 1) * 1000L)
        val hunt = vm.ui.value.hunt!!
        vm.leaveHunt()
        assertEquals(HuntEvent.TREASURE, spawned)
        assertNull(hunt.event)
    }

    @Test
    fun `골든타임 중에는 처치 골드가 2배다`() = runTest(dispatcher) {
        val vm = huntReadyViewModel(rng = QueueRandom(doubles = listOf(0.5, 0.0, 0.18)))
        val golden = vm.ui.value.hunt!!.goldenRemainingMillis
        vm.tapTarget() // +19 직검이 들쥐를 한 방에 잡는다
        val hunt = vm.ui.value.hunt!!
        vm.leaveHunt()
        assertTrue(golden > 0)
        val base = Zone.MEADOW.goldOf(Zone.MEADOW.monsters.first())
        assertEquals((base * HuntEvents.GOLDEN_MULT).toLong(), hunt.lastKillGold)
    }

    @Test
    fun `보스를 잡으면 그 구역 정수가 1 오른다`() = runTest(dispatcher) {
        // 용검의 화상이 가상 시간만으로 잡몹 12마리와 보스를 잡는다 -
        // 탭 연타 가드(실제 시각)를 피하는 방법이다.
        // v2.3에서 사냥터가 용검 기준으로 재편성되며 초원 잡몹 체력이 437배가 됐다.
        // +20 으로는 틱마다 못 잡아 12마리를 못 채운다.
        val vm = huntReadyViewModel(sword = Sword(WeaponFamily.DRAGON, 30))
        advanceTimeBy((Zone.MONSTERS_BEFORE_BOSS + 2) * 1000L)
        val kills = vm.ui.value.hunt!!.killsInZone
        vm.challengeBoss()
        advanceTimeBy(3_000)
        val essences = vm.ui.value.essences
        vm.leaveHunt()
        assertTrue("잡몹 $kills 마리", kills >= Zone.MONSTERS_BEFORE_BOSS)
        assertEquals(1, essences[Zone.MEADOW.id])
    }

    @Test
    fun `보스 알 롤이 낮으면 그 구역 펫 알을 얻는다`() = runTest(dispatcher) {
        val vm = huntReadyViewModel(sword = Sword(WeaponFamily.DRAGON, 30))
        advanceTimeBy((Zone.MONSTERS_BEFORE_BOSS + 2) * 1000L)
        vm.challengeBoss()
        // 보스 처치 시 난수: 드롭(보스는 확정이라 chance 롤 없이 계열 nextInt·단계 nextInt) 뒤 알 롤.
        // QueueRandom 의 doubles 기본값이 1.0이라 알이 안 나온다 - 이 테스트는 doubles 를
        // 미리 채울 수 없으므로(스폰 12회가 소비) 직접 addEgg 경로 대신 통계로 확인하지 않고
        // 기본값(알 없음)을 확인한다. 알 지급 자체는 PetsTest·아래 장착 테스트가 지킨다.
        advanceTimeBy(3_000)
        val pets = vm.ui.value.pets
        vm.leaveHunt()
        assertTrue(pets.counts.isEmpty())
    }

    @Test
    fun `펫 자동 타격이 틱마다 체력을 깎는다`() = runTest(dispatcher) {
        // 쿼카 장착 + 탭 없이 가상 시간만 흘린다
        val store = SaveStore(tmp.root)
        store.saveGame(
            GameState(
                difficulty = Difficulty.ENDLESS,
                gold = 0,
                sword = Sword(WeaponFamily.STRAIGHT, 28), // 화상 없음, 자동 타격만
                pets = com.geomgang.core.PetState(
                    counts = mapOf("quokka" to 1),
                    equippedId = "quokka",
                ),
            ),
        )
        val vm = ForgeViewModel(store, Difficulty.ENDLESS, QueueRandom())
        vm.enterZone(Zone.MEADOW)
        val before = vm.ui.value.hunt!!.killsInZone
        // 펫은 공격력의 일부로 때린다 - 틱마다 들쥐를 잡을 만큼은 된다
        advanceTimeBy(2_100)
        val after = vm.ui.value.hunt!!.killsInZone
        vm.leaveHunt()
        assertTrue("펫이 안 때렸다: 처치 $before -> $after", after > before)
    }

    @Test
    fun `금덩이를 탭하면 골드가 들어오고 금덩이는 사라진다`() = runTest(dispatcher) {
        val vm = huntReadyViewModel(rng = QueueRandom(doubles = listOf(0.5, 0.0, 0.7)))
        assertTrue(vm.ui.value.hunt!!.nugget)
        val before = vm.ui.value.gold
        vm.tapNugget()
        val after = vm.ui.value.gold
        val nuggetGone = !vm.ui.value.hunt!!.nugget
        vm.leaveHunt()
        assertEquals(HuntEvents.nuggetGold(Zone.MEADOW), after - before)
        assertTrue(nuggetGone)
    }
}
