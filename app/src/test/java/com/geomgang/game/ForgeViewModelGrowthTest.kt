package com.geomgang.game

import com.geomgang.core.Difficulty
import com.geomgang.core.GameState
import com.geomgang.core.LegendForge
import com.geomgang.core.Progress
import com.geomgang.core.SaveStore
import com.geomgang.core.Smithy
import com.geomgang.core.Sword
import com.geomgang.core.WeaponCatalog
import com.geomgang.core.WeaponFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
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

/** 성장 축(도감 수집·대장간·보너스 내역)과 전설검이 화면 상태까지 배선됐는지. */
@OptIn(ExperimentalCoroutinesApi::class)
class ForgeViewModelGrowthTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun vm(state: GameState): ForgeViewModel {
        val store = SaveStore(tmp.root)
        store.saveGame(state)
        return ForgeViewModel(store, Difficulty.ENDLESS)
    }

    private fun rich(sword: Sword? = Sword(WeaponFamily.STRAIGHT, 5)) = GameState(
        difficulty = Difficulty.ENDLESS,
        gold = 1_000_000_000_000_000L,
        sword = sword,
        forgeStones = 9_999,
    )

    /** 보관함에 검을 넣어 둔 판. 바치는 자리는 가방 하나뿐이다. */
    private fun withStored(vararg swords: Sword) =
        rich(sword = null).copy(storage = swords.toList())

    // --- 보너스 내역 ---

    @Test
    fun `보너스 출처가 화면 상태로 온다`() {
        val sources = vm(rich()).ui.value.bonusSources
        assertTrue(sources.any { it.label == "도감" })
        assertTrue(sources.any { it.label == "스킬" })
        assertTrue(sources.any { it.label == "고유검" })
    }

    // --- 도감 수집 ---

    /** 바치는 자리는 가방이다. 강화 화면에 버튼을 두면 그 화면이 강화에 집중하지 못한다. */
    @Test
    fun `가방에서 바치면 검이 사라지고 칸이 열린다`() {
        val vm = vm(withStored(Sword(WeaponFamily.STRAIGHT, 5)))
        val before = vm.ui.value.progress.codex.size

        vm.offerFromStorage(0)

        assertTrue(vm.ui.value.storage.isEmpty())
        assertTrue(vm.ui.value.progress.codex.size > before)
    }

    @Test
    fun `이미 찬 칸은 다시 바쳐도 늘지 않는다`() {
        val vm = vm(
            withStored(
                Sword(WeaponFamily.STRAIGHT, 5),
                Sword(WeaponFamily.STRAIGHT, 5),
            ),
        )
        vm.offerFromStorage(0)
        val filled = vm.ui.value.progress.codex.size

        vm.offerFromStorage(0)

        assertEquals(filled, vm.ui.value.progress.codex.size)
        assertEquals(1, vm.ui.value.storage.size)
    }

    // --- 스킬 ---

    @Test
    fun `스킬을 올리면 레벨이 오르고 골드가 빠진다`() {
        val vm = vm(rich())
        val before = vm.ui.value.gold

        vm.upgradeSkill()

        assertEquals(1, vm.ui.value.skillLevel)
        assertTrue(vm.ui.value.gold < before)
    }

    @Test
    fun `스킬은 상한에서 멈춘다`() {
        val vm = vm(rich())
        repeat(Smithy.MAX_LEVEL + 3) { vm.upgradeSkill() }
        assertEquals(Smithy.MAX_LEVEL, vm.ui.value.skillLevel)
        assertFalse(vm.ui.value.canUpgradeSkill)
    }

    // --- 계열 상한 ---

    @Test
    fun `20강에서는 강화가 막히고 이유가 나온다`() {
        val ui = vm(rich(Sword(WeaponFamily.STRAIGHT, 20))).ui.value
        assertFalse(ui.canForge)
        assertNotNull(ui.forgeBlockedReason)
        assertTrue(ui.forgeBlockedReason!!.contains("조합소"))
    }

    // --- 전설검 ---

    @Test
    fun `재료가 다 있으면 전설검을 벼린다`() {
        val vm = vm(
            rich(sword = null).copy(
                storage = LegendForge.MATERIALS.map { Sword(it, LegendForge.MATERIAL_LEVEL) },
            ),
        )
        assertTrue(vm.ui.value.canCraftLegend)

        vm.craftLegend()

        assertEquals(LegendForge.LEVEL, vm.ui.value.sword?.level)
        assertTrue(vm.ui.value.storage.isEmpty())
    }

    @Test
    fun `모자란 재료가 화면에 나온다`() {
        assertEquals(LegendForge.MATERIALS, vm(rich(sword = null)).ui.value.legendMissing)
    }

    /** 전설검을 바치면 다음부터는 조각으로 다시 벼릴 수 있다. */
    @Test
    fun `전설검을 바치면 해금되고 조각으로 되찾는다`() {
        val vm = vm(
            withStored(Sword(WeaponFamily.DRAGON, LegendForge.LEVEL))
                .copy(shards = LegendForge.RECRAFT_SHARDS),
        )
        vm.offerFromStorage(0)

        // 검은 도감으로 갔지만 해금은 남는다
        assertTrue(vm.ui.value.storage.isEmpty())
        assertTrue(vm.ui.value.legendUnlocked)
        assertTrue(vm.ui.value.canRecraftLegend)

        vm.recraftLegend()

        assertEquals(LegendForge.LEVEL, vm.ui.value.sword?.level)
        assertEquals(0, vm.ui.value.shards)
    }

    /**
     * 이 기능이 없던 세이브 이관.
     *
     * 이미 +21 위에 있으면 벽을 넘은 사람이다. 해금 없이 시작했다가 검을 잃으면
     * 조합 재료 넷을 처음부터 다시 모아야 한다.
     */
    @Test
    fun `옛 세이브가 이미 전설검을 들고 있으면 해금된 채로 연다`() {
        assertTrue(vm(rich(Sword(WeaponFamily.STRAIGHT, 30))).ui.value.legendUnlocked)
    }

    /**
     * 숨긴 계열의 검은 도감에 바칠 수 없다 (v2.1).
     *
     * 칸이 도감에 없으니 검만 사라지고 아무것도 열리지 않는 함정이 된다.
     * 옛 세이브의 낫검이 여기 걸린다.
     */
    @Test
    fun `숨긴 계열 검은 바칠 수 없다`() {
        val vm = vm(withStored(Sword(WeaponFamily.SCYTHE, 5)))
        val before = Progress.entriesOf(vm.ui.value.progress).size

        vm.offerFromStorage(0)

        // 검도 그대로, 도감도 그대로
        assertEquals(1, vm.ui.value.storage.size)
        assertEquals(before, Progress.entriesOf(vm.ui.value.progress).size)
    }

    /** 전설검은 부서지지 않으므로 확률 표시에도 파괴가 남으면 안 된다. */
    @Test
    fun `전설검 확률 표시에 파괴가 없다`() {
        val odds = vm(rich(Sword(WeaponFamily.STRAIGHT, 30))).ui.value.odds
        assertEquals(0, odds.destroy)
        assertTrue(odds.drop > 0)
    }
}
