package com.geomgang.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EconomyTest {

    private fun state(
        gold: Long = 0,
        shards: Int = 0,
        sword: Sword? = null,
        pending: PendingDestroy? = null,
    ) = GameState(
        Difficulty.NORMAL,
        gold = gold,
        shards = shards,
        sword = sword,
        pendingDestroy = pending,
    )

    @Test
    fun `강화 비용이 스펙 표와 일치한다`() {
        // round(30 * 1.45^currentLevel)
        assertEquals(30L, Economy.upgradeCost(0))
        assertEquals(133L, Economy.upgradeCost(4))
        assertEquals(850L, Economy.upgradeCost(9))
        assertEquals(5448L, Economy.upgradeCost(14))
        assertEquals(34923L, Economy.upgradeCost(19))
    }

    @Test
    fun `판매가가 스펙 표와 일치한다`() {
        // round(60 * 1.6^level)
        assertEquals(60L, Economy.sellPrice(0))
        assertEquals(96L, Economy.sellPrice(1))
        assertEquals(629L, Economy.sellPrice(5))
        assertEquals(6597L, Economy.sellPrice(10))
        assertEquals(69175L, Economy.sellPrice(15))
        assertEquals(725355L, Economy.sellPrice(20))
    }

    @Test
    fun `판매가는 비용보다 빠르게 증가한다`() {
        // 스펙의 핵심 불변식. 고단계 도전이 계산이 서는 도박이 되려면 이게 성립해야 한다.
        for (level in 1..20) {
            val priceRatio = Economy.sellPrice(level).toDouble() / Economy.sellPrice(level - 1)
            val costRatio = Economy.upgradeCost(level).toDouble() / Economy.upgradeCost(level - 1)
            assertTrue("level=$level price=$priceRatio cost=$costRatio", priceRatio > costRatio)
        }
    }

    @Test
    fun `무한 모드 고단계 판매가가 Long 범위에서 계산된다`() {
        assertTrue(Economy.sellPrice(40) > Economy.sellPrice(30))
        assertTrue(Economy.sellPrice(40) > 0)
    }

    @Test
    fun `상점 가격이 스펙과 일치한다`() {
        assertEquals(800L, Economy.priceOf(Item.PREVENT_TICKET))
        assertEquals(1200L, Economy.priceOf(Item.BLESSING_SCROLL))
        assertEquals(2000L, Economy.priceOf(Item.LUCK_CHARM))
        assertEquals(100L, Economy.BASE_SWORD_PRICE)
    }

    @Test
    fun `아이템 구매는 골드를 차감하고 아이템을 준다`() {
        val after = Economy.buyItem(state(gold = 1000), Item.PREVENT_TICKET)
        assertEquals(200L, after.gold)
        assertEquals(1, after.inventory.preventTickets)
    }

    @Test
    fun `골드가 모자라면 아이템을 살 수 없다`() {
        assertFalse(Economy.canBuyItem(state(gold = 799), Item.PREVENT_TICKET))
        assertTrue(Economy.canBuyItem(state(gold = 800), Item.PREVENT_TICKET))
    }

    @Test
    fun `기본 검 구매는 지정한 계열의 0단계 검을 준다`() {
        val after = Economy.buySword(state(gold = 300), WeaponFamily.CURVED)
        assertEquals(200L, after.gold)
        assertEquals(Sword(WeaponFamily.CURVED, 0), after.sword)
    }

    @Test
    fun `검을 들고 있으면 또 살 수 없다`() {
        val holding = state(gold = 999, sword = Sword(WeaponFamily.STRAIGHT, 1))
        assertFalse(Economy.canBuySword(holding))
    }

    @Test
    fun `검 판매는 골드를 주고 검을 없앤다`() {
        val before = state(gold = 0, sword = Sword(WeaponFamily.STRAIGHT, 10))
        val after = Economy.sellSword(before)
        assertEquals(6597L, after.gold)
        assertNull(after.sword)
    }

    @Test
    fun `검이 없으면 팔 수 없다`() {
        assertFalse(Economy.canSellSword(state()))
    }

    @Test
    fun `파산 판정은 세 조건이 모두 성립할 때만 참이다`() {
        // 검 없음 + 골드 100 미만 + 조각 120 미만
        assertTrue(Economy.needsBailout(state(gold = 99, shards = 119)))
        // 검이 있으면 아니다
        assertFalse(
            Economy.needsBailout(
                state(gold = 0, shards = 0, sword = Sword(WeaponFamily.STRAIGHT, 0)),
            ),
        )
        // 검을 살 골드가 있으면 아니다
        assertFalse(Economy.needsBailout(state(gold = 100, shards = 0)))
        // +5 검을 바꿀 조각이 있으면 아니다
        assertFalse(Economy.needsBailout(state(gold = 0, shards = 120)))
    }

    @Test
    fun `파괴 대기 중에는 파산 구제가 발동하지 않는다`() {
        val pending = PendingDestroy(WeaponFamily.STRAIGHT, 14)
        assertFalse(Economy.needsBailout(state(gold = 0, shards = 0, pending = pending)))
    }

    @Test
    fun `파산 구제는 골드를 300으로 채운다`() {
        val after = Economy.applyBailoutIfNeeded(state(gold = 12, shards = 3))
        assertEquals(300L, after.gold)
        // 검은 주지 않는다. 상점에서 원하는 계열을 직접 고르게 한다.
        assertNull(after.sword)
    }

    @Test
    fun `파산이 아니면 구제는 상태를 바꾸지 않는다`() {
        val rich = state(gold = 5000)
        assertEquals(rich, Economy.applyBailoutIfNeeded(rich))
    }

    @Test
    fun `구제 반복으로 골드를 불릴 수 없다`() {
        // 사고(100) 되파는(60) 순환은 한 바퀴에 40 손해이고 구제 상한이 300이므로
        // 몇 바퀴를 돌려도 골드가 300을 넘지 못한다.
        var s = state(gold = 0)
        repeat(50) {
            s = Economy.applyBailoutIfNeeded(s)
            if (Economy.canBuySword(s)) s = Economy.buySword(s, WeaponFamily.STRAIGHT)
            if (Economy.canSellSword(s)) s = Economy.sellSword(s)
        }
        assertTrue("gold=${s.gold}", s.gold <= Economy.BAILOUT_GOLD)
    }
}
