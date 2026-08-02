package com.geomgang.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ForgeRecoveryTest {

    private fun destroyed(
        level: Int = 14,
        tickets: Int = 1,
        family: WeaponFamily = WeaponFamily.STRAIGHT,
    ) = GameState(
        difficulty = Difficulty.NORMAL,
        gold = 1000,
        sword = null,
        inventory = Inventory(preventTickets = tickets),
        bestLevel = level,
        pendingDestroy = PendingDestroy(family, level),
    )

    // --- 방지권 ---

    @Test
    fun `방지권이 있고 파괴 대기 중이면 되살릴 수 있다`() {
        assertTrue(ForgeEngine.canPrevent(destroyed()))
    }

    @Test
    fun `방지권이 없으면 되살릴 수 없다`() {
        assertFalse(ForgeEngine.canPrevent(destroyed(tickets = 0)))
    }

    @Test
    fun `파괴 대기 상태가 아니면 되살릴 수 없다`() {
        val normal = GameState(Difficulty.NORMAL, sword = Sword(WeaponFamily.STRAIGHT, 3))
        assertFalse(ForgeEngine.canPrevent(normal))
    }

    @Test
    fun `되살리면 파괴 직전 단계와 계열이 그대로 복구된다`() {
        val after = ForgeEngine.applyPrevent(destroyed(level = 17, family = WeaponFamily.HOLY))
        assertEquals(Sword(WeaponFamily.HOLY, 17), after.sword)
        assertNull(after.pendingDestroy)
    }

    @Test
    fun `되살리면 방지권이 한 장 소모된다`() {
        val after = ForgeEngine.applyPrevent(destroyed(tickets = 3))
        assertEquals(2, after.inventory.preventTickets)
    }

    @Test(expected = IllegalStateException::class)
    fun `방지권 없이 되살리려 하면 예외가 난다`() {
        ForgeEngine.applyPrevent(destroyed(tickets = 0))
    }

    // --- 줍기 ---

    @Test
    fun `조각 회수량은 단계의 두 배에 0점7에서 1점3 배 흔들림이 붙는다`() {
        // level 10, jitter 최소(난수 0.0) → floor(10 * 2 * 0.7) = 14
        assertEquals(14, ForgeEngine.salvageAmount(10, ScriptedRandom(0.0)))
        // jitter 중앙(난수 0.5) → floor(10 * 2 * 1.0) = 20
        assertEquals(20, ForgeEngine.salvageAmount(10, ScriptedRandom(0.5)))
        // jitter 최대(난수 1.0) → floor(10 * 2 * 1.3) = 26
        assertEquals(26, ForgeEngine.salvageAmount(10, ScriptedRandom(1.0)))
    }

    @Test
    fun `0단계 검을 잃어도 최소 한 조각은 나온다`() {
        assertEquals(1, ForgeEngine.salvageAmount(0, ScriptedRandom(0.0)))
    }

    /** 조각은 용검 뒤의 화폐다([Unlocks.shardsUsed]) — 그 전에는 줍기가 골드를 준다. */
    @Test
    fun `줍기는 조각을 더하고 파괴 대기를 해제한다`() {
        val before = destroyed(level = 10).copy(bestLevel = LegendForge.LEVEL)
        val after = ForgeEngine.applySalvage(before, ScriptedRandom(0.5))
        assertEquals(20, after.shards)
        assertNull(after.pendingDestroy)
        assertNull(after.sword)
    }

    @Test
    fun `초반 줍기는 골드를 준다`() {
        val before = destroyed(level = 10)
        val after = ForgeEngine.applySalvage(before, ScriptedRandom(0.5))
        assertEquals(0, after.shards)
        assertEquals(before.gold + Unlocks.salvageGold(10), after.gold)
        assertNull(after.pendingDestroy)
    }

    @Test(expected = IllegalStateException::class)
    fun `파괴 대기가 아닐 때 줍기는 예외가 난다`() {
        val normal = GameState(Difficulty.NORMAL, sword = Sword(WeaponFamily.STRAIGHT, 3))
        ForgeEngine.applySalvage(normal, ScriptedRandom(0.5))
    }

    // --- 파괴 확정 ---

    @Test
    fun `파괴 확정은 대기 상태만 지우고 아무것도 주지 않는다`() {
        val before = destroyed(level = 14)
        val after = ForgeEngine.confirmDestroy(before)
        assertNull(after.pendingDestroy)
        assertNull(after.sword)
        assertEquals(0, after.shards)
        assertEquals(before.inventory, after.inventory)
    }

    @Test
    fun `대기 상태가 없으면 파괴 확정은 아무 변화도 없다`() {
        val normal = GameState(Difficulty.NORMAL, sword = Sword(WeaponFamily.STRAIGHT, 3))
        assertEquals(normal, ForgeEngine.confirmDestroy(normal))
    }

    @Test
    fun `앱 재시작 시나리오 - 대기 상태로 저장된 검은 확정 파괴된다`() {
        // 방지권 원이 떠 있는 동안 앱을 강제 종료한 상황을 재현한다.
        // 다시 켜면 confirmDestroy 가 호출되어 파괴가 없던 일이 되지 않는다.
        val savedMidDestroy = destroyed(level = 19, tickets = 5)
        val resumed = ForgeEngine.confirmDestroy(savedMidDestroy)
        assertNull(resumed.sword)
        assertNull(resumed.pendingDestroy)
        // 방지권도 소모되지 않는다. 기회를 그냥 잃는 것이다.
        assertEquals(5, resumed.inventory.preventTickets)
        assertFalse(ForgeEngine.canPrevent(resumed))
    }
}
