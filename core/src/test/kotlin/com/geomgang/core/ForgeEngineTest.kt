package com.geomgang.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ForgeEngineTest {

    /**
     * 강화 판정 테스트용 상태.
     *
     * 재료 검·강화석을 넉넉히 채워 둔다 — 이 클래스의 관심사는 파괴·하락 판정이고,
     * 재료 요구는 [ForgeCostTest] 와 위의 "재료 요구" 절이 따로 지킨다.
     */
    private fun state(
        level: Int,
        gold: Long = 1_000_000,
        difficulty: Difficulty = Difficulty.NORMAL,
        inventory: Inventory = Inventory(),
        family: WeaponFamily = WeaponFamily.STRAIGHT,
    ) = GameState(
        difficulty = difficulty,
        gold = gold,
        sword = Sword(family, level),
        inventory = inventory,
        bestLevel = level,
        storage = List(4) { Sword(WeaponFamily.STRAIGHT, 1) },
        forgeStones = 100,
    )

    // --- 재료 요구 (v1.4) ---

    /** 강화석을 실제로 먹는 판. `bestLevel` 이 용검 단계여야 요구가 산다([Unlocks]). */
    private fun materialState(level: Int, stones: Int, storage: Int) = GameState(
        difficulty = Difficulty.ENDLESS,
        gold = 1_000_000_000,
        sword = Sword(WeaponFamily.STRAIGHT, level),
        storage = List(storage) { Sword(WeaponFamily.STRAIGHT, 1) },
        forgeStones = stones,
        bestLevel = LegendForge.LEVEL,
    )

    @Test
    fun `재료를 다 태운 뒤에도 굴릴 수 있다`() {
        // 재료는 판정 전에 태워진다. 판정이 그 재료를 다시 요구하면, 딱 맞춰 온
        // 플레이어가 낼 것을 다 내고도 조건 미달이 되어 attempt() 가 터진다.
        // 실기기에서 +13 이상이 전부 죽던 버그가 이것이었다.
        val paid = materialState(level = 15, stones = 0, storage = 0)
        assertFalse("재료가 없으니 시작은 못 한다", ForgeEngine.canAttempt(paid, UsedItems.NONE))
        assertTrue("그러나 이미 치른 판은 굴러가야 한다", ForgeEngine.canRoll(paid, UsedItems.NONE))
        // 터지지 않는다
        ForgeEngine.attempt(paid, UsedItems.NONE, ScriptedRandom(0.1))
    }

    @Test
    fun `골드가 없으면 굴릴 수 없다`() {
        val broke = materialState(level = 15, stones = 50, storage = 3).copy(gold = 0)
        assertFalse(ForgeEngine.canRoll(broke, UsedItems.NONE))
    }

    @Test
    fun `16단계 목표는 강화석이 없으면 시도할 수 없다`() {
        assertFalse(
            ForgeEngine.canAttempt(
                materialState(level = 15, stones = 0, storage = 3),
                UsedItems.NONE,
            ),
        )
    }

    @Test
    fun `16단계 목표는 재료가 갖춰지면 시도할 수 있다`() {
        assertTrue(
            ForgeEngine.canAttempt(
                materialState(level = 15, stones = 50, storage = 3),
                UsedItems.NONE,
            ),
        )
    }

    @Test
    fun `보관함이 비어 있어도 강화할 수 있다`() {
        // v1.8: 재료 검은 강화 요구에서 빠졌다. 보관함의 검은 조합 전용이다.
        assertTrue(
            ForgeEngine.canAttempt(
                materialState(level = 15, stones = 50, storage = 0),
                UsedItems.NONE,
            ),
        )
    }

    @Test
    fun `저단계는 재료 없이도 시도할 수 있다`() {
        assertTrue(
            ForgeEngine.canAttempt(
                materialState(level = 3, stones = 0, storage = 0),
                UsedItems.NONE,
            ),
        )
    }

    // --- canAttempt ---

    @Test
    fun `검이 없으면 강화할 수 없다`() {
        val empty = GameState(Difficulty.NORMAL, gold = 1_000_000)
        assertFalse(ForgeEngine.canAttempt(empty, UsedItems.NONE))
    }

    @Test
    fun `골드가 비용보다 적으면 강화할 수 없다`() {
        assertFalse(ForgeEngine.canAttempt(state(level = 0, gold = 29), UsedItems.NONE))
        assertTrue(ForgeEngine.canAttempt(state(level = 0, gold = 30), UsedItems.NONE))
    }

    @Test
    fun `상한에 도달하면 더 강화할 수 없다`() {
        assertFalse(ForgeEngine.canAttempt(state(level = 20), UsedItems.NONE))
        // 무한 모드에도 계열 상한은 있다. +20 위는 조합으로만 간다([LegendForge]).
        assertFalse(
            ForgeEngine.canAttempt(
                state(level = 20, difficulty = Difficulty.ENDLESS),
                UsedItems.NONE,
            ),
        )
        // 전설검이 되면 상한이 풀린다
        assertTrue(
            ForgeEngine.canAttempt(
                state(level = 21, difficulty = Difficulty.ENDLESS),
                UsedItems.NONE,
            ),
        )
    }

    @Test
    fun `없는 아이템은 사용 지정할 수 없다`() {
        val s = state(level = 0)
        assertFalse(ForgeEngine.canAttempt(s, UsedItems(blessing = true)))
        assertFalse(ForgeEngine.canAttempt(s, UsedItems(luckCharm = true)))
        val stocked = state(level = 0, inventory = Inventory(blessingScrolls = 1, luckCharms = 1))
        assertTrue(ForgeEngine.canAttempt(stocked, UsedItems(blessing = true, luckCharm = true)))
    }

    @Test
    fun `파괴 대기 중에는 강화할 수 없다`() {
        val pending = state(level = 5).copy(
            sword = null,
            pendingDestroy = PendingDestroy(WeaponFamily.STRAIGHT, 14),
        )
        assertFalse(ForgeEngine.canAttempt(pending, UsedItems.NONE))
    }

    // --- 성공 ---

    @Test
    fun `성공하면 단계가 오르고 비용이 빠진다`() {
        val before = state(level = 3, gold = 1000)
        // 일반 +4 성공률 0.80, 난수 0.5 는 그 아래이므로 성공
        val result = ForgeEngine.attempt(before, UsedItems.NONE, ScriptedRandom(0.5))
        assertTrue(result is ForgeResult.Success)
        result as ForgeResult.Success
        assertEquals(4, result.newLevel)
        assertEquals(4, result.state.sword?.level)
        assertEquals(1000L - Economy.upgradeCost(3), result.state.gold)
    }

    @Test
    fun `성공하면 최고 기록이 갱신된다`() {
        val before = state(level = 3).copy(bestLevel = 3)
        val result = ForgeEngine.attempt(before, UsedItems.NONE, ScriptedRandom(0.1))
        assertEquals(4, result.state.bestLevel)
    }

    @Test
    fun `하락 후 재상승은 최고 기록을 낮추지 않는다`() {
        val before = state(level = 6).copy(bestLevel = 12)
        val result = ForgeEngine.attempt(before, UsedItems.NONE, ScriptedRandom(0.1))
        assertEquals(12, result.state.bestLevel)
    }

    // --- 실패: 구간별 ---

    @Test
    fun `안전구간 실패는 단계를 유지한다`() {
        val before = state(level = 4)
        // 일반 +5 성공률 0.75, 난수 0.9 는 실패
        val result = ForgeEngine.attempt(before, UsedItems.NONE, ScriptedRandom(0.9))
        assertTrue(result is ForgeResult.Stay)
        assertEquals(4, result.state.sword?.level)
    }

    @Test
    fun `하락구간 실패는 한 단계 떨어뜨린다`() {
        val before = state(level = 8)
        // 일반 +9 성공률 0.47, 난수 0.9 는 실패
        val result = ForgeEngine.attempt(before, UsedItems.NONE, ScriptedRandom(0.9))
        assertTrue(result is ForgeResult.Drop)
        result as ForgeResult.Drop
        assertEquals(7, result.newLevel)
        assertEquals(7, result.state.sword?.level)
    }

    @Test
    fun `파괴구간 실패는 두 번째 난수로 파괴와 하락이 갈린다`() {
        val before = state(level = 13)
        // 일반 +14 성공률 0.19 → 0.9 로 실패. 파괴 확률 0.40.
        val destroyed = ForgeEngine.attempt(before, UsedItems.NONE, ScriptedRandom(0.9, 0.1))
        assertTrue(destroyed is ForgeResult.Destroyed)

        val dropped = ForgeEngine.attempt(before, UsedItems.NONE, ScriptedRandom(0.9, 0.9))
        assertTrue(dropped is ForgeResult.Drop)
    }

    @Test
    fun `파괴되면 검이 사라지고 파괴 대기 상태가 남는다`() {
        val before = state(level = 13, family = WeaponFamily.DRAGON)
        // 용검은 파괴방지 특성이 있어 난수를 하나 더 쓴다. 0.9 는 방지에 실패하는 값이다.
        val result = ForgeEngine.attempt(before, UsedItems.NONE, ScriptedRandom(0.9, 0.1, 0.9))
        result as ForgeResult.Destroyed
        assertEquals(13, result.lostLevel)
        assertNull(result.state.sword)
        assertEquals(PendingDestroy(WeaponFamily.DRAGON, 13), result.state.pendingDestroy)
    }

    @Test
    fun `방지권이 있어야 되살릴 수 있는 파괴로 표시된다`() {
        val without = ForgeEngine.attempt(
            state(level = 13),
            UsedItems.NONE,
            ScriptedRandom(0.9, 0.1),
        ) as ForgeResult.Destroyed
        assertFalse(without.preventable)

        val withTicket = ForgeEngine.attempt(
            state(level = 13, inventory = Inventory(preventTickets = 1)),
            UsedItems.NONE,
            ScriptedRandom(0.9, 0.1),
        ) as ForgeResult.Destroyed
        assertTrue(withTicket.preventable)
    }

    /**
     * 무한 구간은 [RateTable.destroyChance] 가 1.00 이라 실패가 곧 파괴 판정이다.
     *
     * 다만 그 구간의 검은 전부 전설검이고, 전설검은 사라지는 대신 +21 로 되돌아간다.
     * 파괴방지 특성이 있어 난수도 하나 더 쓴다.
     */
    @Test
    fun `무한 구간 실패는 두 번째 난수와 무관하게 파괴 판정이다`() {
        val before = state(level = 21, difficulty = Difficulty.ENDLESS)
        val result = ForgeEngine.attempt(before, UsedItems.NONE, ScriptedRandom(0.9, 0.99, 0.9))
        assertTrue("결과=$result", result is ForgeResult.Drop)
        assertEquals(LegendForge.LEVEL, result.state.sword?.level)
    }

    // --- 아이템 ---

    @Test
    fun `축복서는 성공률을 올리고 소모된다`() {
        val before = state(level = 9, inventory = Inventory(blessingScrolls = 1))
        // 일반 +10 성공률 0.46, 축복서로 0.56. 난수 0.50 은 축복서가 있어야 성공한다.
        val result = ForgeEngine.attempt(before, UsedItems(blessing = true), ScriptedRandom(0.50))
        assertTrue(result is ForgeResult.Success)
        assertEquals(0, result.state.inventory.blessingScrolls)
    }

    @Test
    fun `축복서 없이 같은 난수면 실패한다`() {
        // +10 목표는 이제 파괴 구간이다(v2.3) - 파괴 판정 난수까지 하나 더 준다
        val before = state(level = 9)
        val result = ForgeEngine.attempt(before, UsedItems.NONE, ScriptedRandom(0.50, 0.99))
        assertFalse(result is ForgeResult.Success)
    }

    @Test
    fun `행운부적은 파괴를 면한 실패를 유지로 바꾼다`() {
        val before = state(level = 19, inventory = Inventory(luckCharms = 1))
        // 난수: 성공 실패(0.99) → 파괴 판정 통과(0.99 > 0.55). 부적은 하락만 막는다.
        val result = ForgeEngine.attempt(
            before,
            UsedItems(luckCharm = true),
            ScriptedRandom(0.99, 0.99),
        )
        assertTrue(result is ForgeResult.Stay)
        assertEquals(19, result.state.sword?.level)
        assertEquals(0, result.state.inventory.luckCharms)
    }

    /** 부적이 파괴까지 막으면 방지권이 죽은 물건이 된다(v2.3) — 파괴는 그대로다. */
    @Test
    fun `행운부적이 있어도 파괴는 일어난다`() {
        val before = state(level = 19, inventory = Inventory(luckCharms = 1))
        // 난수: 성공 실패(0.99) → 파괴 판정 적중(0.10 < 0.55)
        val result = ForgeEngine.attempt(
            before,
            UsedItems(luckCharm = true),
            ScriptedRandom(0.99, 0.10),
        )
        assertTrue(result is ForgeResult.Destroyed)
    }

    @Test
    fun `행운부적은 성공했을 때도 소모된다`() {
        val before = state(level = 3, inventory = Inventory(luckCharms = 1))
        val result = ForgeEngine.attempt(before, UsedItems(luckCharm = true), ScriptedRandom(0.1))
        assertTrue(result is ForgeResult.Success)
        assertEquals(0, result.state.inventory.luckCharms)
    }

    @Test
    fun `행운부적을 써도 파괴 판정 난수는 굴린다`() {
        // v2.3 - 부적은 하락만 막으므로 파괴 판정은 언제나 돈다.
        val before = state(level = 19, inventory = Inventory(luckCharms = 1))
        val rng = ScriptedRandom(0.99, 0.99)
        ForgeEngine.attempt(before, UsedItems(luckCharm = true), rng)
        assertEquals(2, rng.consumed)
    }

    // --- 방어 ---

    @Test(expected = IllegalStateException::class)
    fun `조건을 만족하지 않는 상태로 시도하면 예외가 난다`() {
        ForgeEngine.attempt(state(level = 0, gold = 0), UsedItems.NONE, ScriptedRandom(0.1))
    }

    @Test
    fun `단계가 음수로 내려가지 않는다`() {
        // +6 시도(현재 5)에서 실패하면 4로 떨어진다. 0 아래로는 어떤 경우에도 내려가지 않는다.
        var s = state(level = 5)
        repeat(10) {
            if (ForgeEngine.canAttempt(s, UsedItems.NONE)) {
                s = ForgeEngine.attempt(s, UsedItems.NONE, ScriptedRandom(0.99, 0.99)).state
            }
            assertTrue((s.sword?.level ?: 0) >= 0)
        }
    }
}
