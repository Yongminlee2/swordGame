package com.geomgang.game

import com.geomgang.core.Difficulty
import com.geomgang.core.Economy
import com.geomgang.core.GameState
import com.geomgang.core.Inventory
import com.geomgang.core.Item
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class ForgeViewModelEconomyTest {

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
        gold: Long = 100_000,
        shards: Int = 0,
        sword: Sword? = Sword(WeaponFamily.STRAIGHT, 3),
        inventory: Inventory = Inventory(),
        rng: Random = alwaysSucceed(),
    ): ForgeViewModel {
        val store = SaveStore(tmp.root)
        store.saveGame(
            GameState(
                difficulty = Difficulty.NORMAL,
                gold = gold,
                shards = shards,
                sword = sword,
                inventory = inventory,
            ),
        )
        return ForgeViewModel(store, Difficulty.NORMAL, rng)
    }

    // --- 상점 ---

    @Test
    fun `아이템을 사면 골드가 줄고 보유량이 는다`() = runTest(dispatcher) {
        val vm = vm(gold = 10_000)
        vm.buyItem(Item.PREVENT_TICKET)
        assertEquals(10_000L - Economy.PREVENT_TICKET_PRICE, vm.ui.value.gold)
        assertEquals(1, vm.ui.value.preventTickets)
    }

    @Test
    fun `골드가 모자라면 사지지 않는다`() = runTest(dispatcher) {
        val vm = vm(gold = 10)
        vm.buyItem(Item.PREVENT_TICKET)
        assertEquals(10L, vm.ui.value.gold)
        assertEquals(0, vm.ui.value.preventTickets)
    }

    @Test
    fun `축복서와 행운부적도 살 수 있다`() = runTest(dispatcher) {
        val vm = vm(gold = 10_000)
        vm.buyItem(Item.BLESSING_SCROLL)
        vm.buyItem(Item.LUCK_CHARM)
        assertEquals(1, vm.ui.value.blessingScrolls)
        assertEquals(1, vm.ui.value.luckCharms)
    }

    // --- 조합소 ---

    @Test
    fun `조각을 바꾸면 조각이 줄고 아이템이 생긴다`() = runTest(dispatcher) {
        val vm = vm(shards = 50)
        vm.craft("prevent", WeaponFamily.STRAIGHT)
        assertEquals(40, vm.ui.value.shards)
        assertEquals(1, vm.ui.value.preventTickets)
    }

    @Test
    fun `조각이 모자라면 바뀌지 않는다`() = runTest(dispatcher) {
        val vm = vm(shards = 5)
        vm.craft("prevent", WeaponFamily.STRAIGHT)
        assertEquals(5, vm.ui.value.shards)
        assertEquals(0, vm.ui.value.preventTickets)
    }

    @Test
    fun `조각으로 5단계 검을 바꾸면 지정한 계열로 생긴다`() = runTest(dispatcher) {
        val vm = vm(shards = 200, sword = null)
        vm.craft("sword5", WeaponFamily.CURVED)
        assertEquals(Sword(WeaponFamily.CURVED, 5), vm.ui.value.sword)
        assertEquals(80, vm.ui.value.shards)
    }

    @Test
    fun `검을 들고 있으면 검 교환이 막힌다`() = runTest(dispatcher) {
        val vm = vm(shards = 200, sword = Sword(WeaponFamily.STRAIGHT, 3))
        vm.craft("sword5", WeaponFamily.CURVED)
        assertEquals(200, vm.ui.value.shards)
        assertEquals(3, vm.ui.value.sword?.level)
    }

    // --- 아이템 사용 배선 ---

    @Test
    fun `축복서 토글을 켜고 강화하면 축복서가 소모된다`() = runTest(dispatcher) {
        val vm = vm(inventory = Inventory(blessingScrolls = 1))
        vm.toggleBlessing()
        assertTrue(vm.ui.value.useBlessing)
        vm.forge()
        assertEquals(0, vm.ui.value.blessingScrolls)
    }

    @Test
    fun `축복서 토글은 한 번 쓰면 꺼진다`() = runTest(dispatcher) {
        val vm = vm(inventory = Inventory(blessingScrolls = 2))
        vm.toggleBlessing()
        vm.forge()
        // 켜 둔 채 잊고 연타하면 아이템이 순식간에 녹는다. 매번 다시 켜게 한다.
        assertFalse(vm.ui.value.useBlessing)
        assertEquals(1, vm.ui.value.blessingScrolls)
    }

    @Test
    fun `축복서가 없으면 토글이 켜지지 않는다`() = runTest(dispatcher) {
        val vm = vm(inventory = Inventory())
        vm.toggleBlessing()
        assertFalse(vm.ui.value.useBlessing)
    }

    @Test
    fun `행운부적 토글도 같은 규칙을 따른다`() = runTest(dispatcher) {
        val vm = vm(inventory = Inventory(luckCharms = 1))
        vm.toggleLuckCharm()
        assertTrue(vm.ui.value.useLuckCharm)
        vm.forge()
        assertEquals(0, vm.ui.value.luckCharms)
        assertFalse(vm.ui.value.useLuckCharm)
    }

    @Test
    fun `토글을 다시 누르면 꺼진다`() = runTest(dispatcher) {
        val vm = vm(inventory = Inventory(blessingScrolls = 1))
        vm.toggleBlessing()
        vm.toggleBlessing()
        assertFalse(vm.ui.value.useBlessing)
    }

    @Test
    fun `아이템을 다 쓰면 토글이 자동으로 내려간다`() = runTest(dispatcher) {
        val vm = vm(inventory = Inventory(blessingScrolls = 1))
        vm.toggleBlessing()
        vm.forge()
        vm.toggleBlessing()
        assertFalse("남은 축복서가 없으면 켜질 수 없다", vm.ui.value.useBlessing)
    }

    // --- 계열 해금 ---

    @Test
    fun `처음에는 기본 계열 4종만 고를 수 있다`() = runTest(dispatcher) {
        assertEquals(WeaponFamily.STARTERS, vm().ui.value.unlockedFamilies)
    }

    @Test
    fun `검 구매는 지정한 계열로 생긴다`() = runTest(dispatcher) {
        val vm = vm(gold = 1_000, sword = null)
        vm.buySword(WeaponFamily.GREAT)
        assertNotNull(vm.ui.value.sword)
        assertEquals(WeaponFamily.GREAT, vm.ui.value.sword?.family)
    }

    @Test
    fun `검을 팔면 검이 사라지고 골드가 는다`() = runTest(dispatcher) {
        val vm = vm(gold = 0, sword = Sword(WeaponFamily.STRAIGHT, 5))
        vm.sellSword()
        assertNull(vm.ui.value.sword)
        assertEquals(Economy.sellPrice(5), vm.ui.value.gold)
    }
}
