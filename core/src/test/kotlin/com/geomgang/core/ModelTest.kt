package com.geomgang.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelTest {

    @Test
    fun `계열은 12종이고 기본 해금은 4종이다`() {
        assertEquals(12, WeaponFamily.entries.size)
        assertEquals(4, WeaponFamily.STARTERS.size)
        assertTrue(WeaponFamily.STRAIGHT in WeaponFamily.STARTERS)
        assertTrue(WeaponFamily.DRAGON !in WeaponFamily.STARTERS)
    }

    @Test
    fun `계열 아이디는 서로 겹치지 않는다`() {
        val ids = WeaponFamily.entries.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `아이디로 계열을 찾을 수 있다`() {
        assertEquals(WeaponFamily.DEMON, WeaponFamily.fromId("demon"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `음수 단계 검은 만들 수 없다`() {
        Sword(WeaponFamily.STRAIGHT, -1)
    }

    @Test
    fun `인벤토리는 아이템별 수량을 반환한다`() {
        val inv = Inventory(preventTickets = 3, blessingScrolls = 1, luckCharms = 0)
        assertEquals(3, inv.countOf(Item.PREVENT_TICKET))
        assertEquals(1, inv.countOf(Item.BLESSING_SCROLL))
        assertEquals(0, inv.countOf(Item.LUCK_CHARM))
    }

    @Test
    fun `인벤토리 가감은 새 인스턴스를 만든다`() {
        val before = Inventory(preventTickets = 1)
        val after = before.plus(Item.PREVENT_TICKET, 2)
        assertEquals(1, before.preventTickets)
        assertEquals(3, after.preventTickets)
        assertEquals(1, after.minus(Item.PREVENT_TICKET, 2).preventTickets)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `보유량보다 많이 뺄 수 없다`() {
        Inventory(luckCharms = 1).minus(Item.LUCK_CHARM, 2)
    }

    @Test
    fun `새 게임 상태의 기본값`() {
        val state = GameState(Difficulty.NORMAL)
        assertEquals(0L, state.gold)
        assertEquals(0, state.shards)
        assertNull(state.sword)
        assertNull(state.pendingDestroy)
        assertEquals(0, state.bestLevel)
        assertEquals(Inventory(), state.inventory)
    }

    @Test
    fun `사용 아이템 기본값은 아무것도 쓰지 않음이다`() {
        assertEquals(false, UsedItems.NONE.blessing)
        assertEquals(false, UsedItems.NONE.luckCharm)
    }
}
