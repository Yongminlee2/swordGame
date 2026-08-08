package com.geomgang.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 전설검 등급.
 *
 * 계열은 +20 에서 끝나고 그 위는 **조합으로만** 간다. 벽이 높은 만큼 넘은 뒤에는
 * 되돌아가지 않는다 - 전설검은 파괴되지 않고 +21 로 돌아가며, 도감에 바친 기록은 영구다.
 */
class LegendForgeTest {

    private fun withMaterials() = GameState(
        difficulty = Difficulty.ENDLESS,
        gold = 1_000_000_000_000_000L,
        storage = LegendForge.MATERIALS.map { Sword(it, LegendForge.MATERIAL_LEVEL) },
    )

    // --- 계열 상한 ---

    @Test
    fun `계열 검은 20에서 더 못 올린다`() {
        assertFalse(LegendForge.canForge(Sword(WeaponFamily.STRAIGHT, 20)))
        assertTrue(LegendForge.canForge(Sword(WeaponFamily.STRAIGHT, 19)))
    }

    @Test
    fun `전설검은 계속 올릴 수 있다`() {
        assertTrue(LegendForge.canForge(Sword(WeaponFamily.STRAIGHT, 21)))
        assertTrue(LegendForge.canForge(Sword(WeaponFamily.STRAIGHT, 44)))
    }

    /** 상한은 판정 입구에서도 막혀야 한다. 화면만 가리면 자동강화가 뚫는다. */
    @Test
    fun `20강 계열 검은 굴릴 수 없다`() {
        val state = GameState(
            difficulty = Difficulty.ENDLESS,
            gold = 1_000_000_000_000_000L,
            sword = Sword(WeaponFamily.STRAIGHT, 20),
        )
        assertFalse(ForgeEngine.canRoll(state, UsedItems.NONE))
        assertTrue(
            ForgeEngine.canRoll(
                state.copy(sword = Sword(WeaponFamily.STRAIGHT, 19)),
                UsedItems.NONE,
            ),
        )
    }

    // --- 조합 ---

    @Test
    fun `재료가 다 있으면 벼릴 수 있다`() {
        assertTrue(LegendForge.canCraft(withMaterials(), ProgressState()))
        assertTrue(LegendForge.missingFor(withMaterials()).isEmpty())
    }

    @Test
    fun `모자란 재료를 알려 준다`() {
        val one = GameState(
            difficulty = Difficulty.ENDLESS,
            storage = LegendForge.MATERIALS.take(1).map { Sword(it, 20) },
        )
        assertEquals(LegendForge.MATERIALS.drop(1), LegendForge.missingFor(one))
        assertFalse(LegendForge.canCraft(one, ProgressState()))
    }

    @Test
    fun `단계가 모자라면 재료로 안 쳐준다`() {
        val low = GameState(
            difficulty = Difficulty.ENDLESS,
            storage = LegendForge.MATERIALS.map { Sword(it, 19) },
        )
        assertEquals(LegendForge.MATERIALS, LegendForge.missingFor(low))
        assertFalse(LegendForge.canCraft(low, ProgressState()))
    }

    /** 고유검은 하나뿐이다. 재료로 녹여 버리면 되돌릴 길이 없다. */
    @Test
    fun `고유검은 재료로 쓰지 않는다`() {
        val unique = GameState(
            difficulty = Difficulty.ENDLESS,
            storage = LegendForge.MATERIALS.map { Sword(it, 20, uniqueId = "phoenix") },
        )
        assertEquals(LegendForge.MATERIALS, LegendForge.missingFor(unique))
    }

    @Test
    fun `손에 검이 있으면 못 벼린다`() {
        val holding = withMaterials().copy(sword = Sword(WeaponFamily.STRAIGHT, 3))
        assertFalse(LegendForge.canCraft(holding, ProgressState()))
    }

    /**
     * 갓 벼린 용검은 +1이다(v2.5) — 마검·성검처럼 다시 오른다.
     * +21(전설)은 [recraft] 로 다시 벼릴 때만 곧장 온다.
     */
    @Test
    fun `벼리면 재료가 사라지고 용검 +1이 손에 온다`() {
        val (state, _) = LegendForge.craft(withMaterials(), ProgressState())
        assertEquals(LegendForge.CRAFT_LEVEL, state.sword?.level)
        assertEquals(WeaponFamily.DRAGON, state.sword?.family)
        assertFalse(state.sword!!.isLegend())
        assertTrue(state.storage.isEmpty())
    }

    /** 같은 계열이 여러 자루면 한 자루만 태운다. 나머지는 남는다. */
    @Test
    fun `재료는 계열마다 한 자루씩만 태운다`() {
        val spare = withMaterials().let {
            it.copy(storage = it.storage + Sword(LegendForge.MATERIALS.first(), 20))
        }
        val (state, _) = LegendForge.craft(spare, ProgressState())
        assertEquals(1, state.storage.size)
        assertEquals(LegendForge.MATERIALS.first(), state.storage.first().family)
    }

    // --- 전설 해금 ---

    @Test
    fun `처음 벼려도 아직 해금은 아니다`() {
        val (_, progress) = LegendForge.craft(withMaterials(), ProgressState())
        assertFalse(progress.legendUnlocked)
    }

    /** 도감에 바쳐야 해금이 남는다. 검은 사라지지만 벽을 넘은 기록은 영구다. */
    @Test
    fun `전설검을 도감에 바치면 해금이 남는다`() {
        val progress = LegendForge.onOffered(ProgressState(), Sword(WeaponFamily.STRAIGHT, 30))
        assertTrue(progress.legendUnlocked)
    }

    @Test
    fun `계열 검을 바쳐도 해금되지 않는다`() {
        val progress = LegendForge.onOffered(ProgressState(), Sword(WeaponFamily.STRAIGHT, 10))
        assertFalse(progress.legendUnlocked)
    }

    @Test
    fun `해금되면 조각으로 다시 벼린다`() {
        val unlocked = ProgressState(legendUnlocked = true)
        val rich = GameState(Difficulty.ENDLESS, shards = LegendForge.RECRAFT_SHARDS)
        assertTrue(LegendForge.canRecraft(rich, unlocked))

        val after = LegendForge.recraft(rich)
        assertEquals(LegendForge.LEVEL, after.sword?.level)
        assertEquals(0, after.shards)
    }

    @Test
    fun `해금 전에는 조각으로 못 벼린다`() {
        val rich = GameState(Difficulty.ENDLESS, shards = 99_999)
        assertFalse(LegendForge.canRecraft(rich, ProgressState()))
    }

    @Test
    fun `조각이 모자라면 다시 못 벼린다`() {
        val poor = GameState(Difficulty.ENDLESS, shards = LegendForge.RECRAFT_SHARDS - 1)
        assertFalse(LegendForge.canRecraft(poor, ProgressState(legendUnlocked = true)))
    }

    // --- 파괴 ---

    /**
     * 재료 넷을 다시 모으는 것은 몇 시간을 지우는 일이라 누를 엄두가 안 난다.
     *
     * 굴림 순서는 성공 → 파괴 → 파괴방지 셋이다. 전설검은 방어 특성이 있어 셋째 값을
     * 반드시 쓴다.
     */
    @Test
    fun `전설검은 파괴돼도 사라지지 않고 21로 돌아간다`() {
        val state = GameState(
            difficulty = Difficulty.ENDLESS,
            gold = 1_000_000_000_000_000L,
            sword = Sword(WeaponFamily.STRAIGHT, 44),
        )
        val result = ForgeEngine.attempt(
            state,
            UsedItems.NONE,
            ScriptedRandom(0.999, 0.0, 0.999),
        )
        assertTrue("결과=$result", result is ForgeResult.Drop)
        assertEquals(LegendForge.LEVEL, result.state.sword?.level)
        assertNull(result.state.pendingDestroy)
    }

    /** 계열 검은 그대로 파괴된다. 전설검만 예외라는 것을 못 박는다. */
    @Test
    fun `계열 검은 여전히 파괴된다`() {
        val state = GameState(
            difficulty = Difficulty.ENDLESS,
            gold = 1_000_000_000_000_000L,
            sword = Sword(WeaponFamily.STRAIGHT, 15),
        )
        val result = ForgeEngine.attempt(state, UsedItems.NONE, ScriptedRandom(0.999, 0.0))
        assertTrue("결과=$result", result is ForgeResult.Destroyed)
        assertNull(result.state.sword)
    }

    /**
     * 갓 벼린 +21 이 실패해도 +20 으로 내려가지 않는다.
     *
     * 무한 구간은 [RateTable.destroyChance] 가 1.00 이라 실패가 곧 파괴다. 전설검은
     * 그 자리에서 +21 로 되돌아가므로 **계열 구간으로 넘어가는 길이 없다.**
     */
    @Test
    fun `전설검은 21 아래로 떨어지지 않는다`() {
        val state = GameState(
            difficulty = Difficulty.ENDLESS,
            gold = 1_000_000_000_000_000L,
            sword = Sword(WeaponFamily.STRAIGHT, LegendForge.LEVEL),
        )
        val result = ForgeEngine.attempt(
            state,
            UsedItems.NONE,
            ScriptedRandom(0.999, 0.999, 0.999),
        )
        assertEquals(LegendForge.LEVEL, result.state.sword?.level)
        assertNull(result.state.pendingDestroy)
    }
}
