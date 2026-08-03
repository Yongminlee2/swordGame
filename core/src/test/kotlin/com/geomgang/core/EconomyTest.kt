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
        // +10 까지 round(110 * 1.8^level), +11~14 는 1.50, +15~20 은 1.20 (v2.3)
        assertEquals(110L, Economy.sellPrice(0))
        assertEquals(198L, Economy.sellPrice(1))
        assertEquals(2079L, Economy.sellPrice(5))
        assertEquals(39275L, Economy.sellPrice(10))
        assertEquals(198830L, Economy.sellPrice(14))
        assertEquals(238596L, Economy.sellPrice(15))
        assertEquals(593704L, Economy.sellPrice(20))
    }

    /**
     * 벌이가 나는 구간은 +14 까지다.
     *
     * 여기서 한 단계를 올리면 판매가가 비용보다 빠르게 늘어 자본이 쌓인다.
     * **+15 위는 일부러 뒤집혀 있다** — 조합 재료(+20)를 만드는 구간이지 돈을 버는
     * 구간이 아니다. 그 구간까지 이 부등식을 요구하면 +20 한 자루로 시즌1이 끝난다.
     * 경제가 실제로 도는지는 [com.geomgang.core.sim.BalanceSimulation] 의 파산율이 지킨다.
     */
    @Test
    fun `벌이 구간에서는 판매가가 비용보다 빠르게 증가한다`() {
        for (level in 1..14) {
            val priceRatio = Economy.sellPrice(level).toDouble() / Economy.sellPrice(level - 1)
            val costRatio = Economy.upgradeCost(level).toDouble() / Economy.upgradeCost(level - 1)
            assertTrue("level=$level price=$priceRatio cost=$costRatio", priceRatio > costRatio)
        }
    }

    @Test
    fun `판매가가 하락 구간의 기대 비용을 앞선다`() {
        // 원시 비용끼리 비교하는 것만으로는 경제가 돌아가는지 알 수 없다.
        // 하락 구간에서는 실패가 단계를 깎으므로, 한 단계를 올리는 기대 비용이
        // 재시도(1/p)와 되돌아오는 비용까지 곱해져 훨씬 빠르게 커진다.
        // +0 에서 +10 까지 올리는 기대 비용보다 +10 판매가가 커야 자본이 쌓인다.
        var expectedCostToNext = 0.0
        var cumulative = 0.0
        for (current in 0 until 10) {
            val target = current + 1
            val p = RateTable.successRate(Difficulty.NORMAL, target)
            val cost = Economy.upgradeCost(current).toDouble()
            expectedCostToNext = when (RateTable.failureBand(target)) {
                // 실패해도 제자리이므로 단순 재시도
                FailureBand.STAY -> cost / p
                // 실패하면 한 단계 떨어져 되돌아오는 비용이 더 붙는다
                else -> (cost + (1 - p) * expectedCostToNext) / p
            }
            cumulative += expectedCostToNext
        }
        assertTrue(
            "기대 비용 $cumulative vs 판매가 ${Economy.sellPrice(10)}",
            Economy.sellPrice(10) > cumulative,
        )
    }

    @Test
    fun `무한 모드 고단계 판매가가 Long 범위에서 계산된다`() {
        assertTrue(Economy.sellPrice(40) > Economy.sellPrice(30))
        assertTrue(Economy.sellPrice(40) > 0)
    }

    @Test
    fun `상점 가격이 스펙과 일치한다`() {
        // v2.3에서 10배. 방지권이 늘 사 두는 것이 아니라 고민거리가 되어야 한다.
        assertEquals(8_000L, Economy.priceOf(Item.PREVENT_TICKET))
        assertEquals(12_000L, Economy.priceOf(Item.BLESSING_SCROLL))
        assertEquals(20_000L, Economy.priceOf(Item.LUCK_CHARM))
        assertEquals(160L, Economy.BASE_SWORD_PRICE)
    }

    @Test
    fun `아이템 구매는 골드를 차감하고 아이템을 준다`() {
        val after = Economy.buyItem(state(gold = 10_000), Item.PREVENT_TICKET)
        assertEquals(2_000L, after.gold)
        assertEquals(1, after.inventory.preventTickets)
    }

    @Test
    fun `골드가 모자라면 아이템을 살 수 없다`() {
        assertFalse(Economy.canBuyItem(state(gold = 7_999), Item.PREVENT_TICKET))
        assertTrue(Economy.canBuyItem(state(gold = 8_000), Item.PREVENT_TICKET))
    }

    @Test
    fun `기본 검 구매는 지정한 계열의 0단계 검을 준다`() {
        val after = Economy.buySword(state(gold = 300), WeaponFamily.CURVED)
        assertEquals(300L - Economy.BASE_SWORD_PRICE, after.gold)
        assertEquals(Sword(WeaponFamily.CURVED, 0), after.sword)
    }

    @Test
    fun `검을 들고 있으면 또 손에 들 수 없다`() {
        val holding = state(gold = 999, sword = Sword(WeaponFamily.STRAIGHT, 1))
        assertFalse(Economy.canBuySword(holding))
    }

    // --- 가방으로 바로 구매 ---

    @Test
    fun `검을 들고 있어도 가방으로는 살 수 있다`() {
        // 고단계 강화는 재료 검을 먹는다. 그때마다 들고 있던 검을 넣었다 뺐다 하지 않아도 된다.
        val holding = state(gold = 999, sword = Sword(WeaponFamily.STRAIGHT, 1))
        assertTrue(Economy.canBuyToStorage(holding))

        val after = Economy.buyToStorage(holding, WeaponFamily.CURVED)
        assertEquals(999L - Economy.BASE_SWORD_PRICE, after.gold)
        assertEquals(listOf(Sword(WeaponFamily.CURVED, 0)), after.storage)
        assertEquals("손에 든 검은 그대로다", holding.sword, after.sword)
    }

    @Test
    fun `가방이 가득 차면 가방으로 살 수 없다`() {
        val full = state(gold = 999).copy(
            storage = List(Storage.CAPACITY) { Sword(WeaponFamily.STRAIGHT, 0) },
        )
        assertFalse(Economy.canBuyToStorage(full))
    }

    @Test
    fun `골드가 모자라면 가방으로도 살 수 없다`() {
        assertFalse(Economy.canBuyToStorage(state(gold = Economy.BASE_SWORD_PRICE - 1)))
        assertTrue(Economy.canBuyToStorage(state(gold = Economy.BASE_SWORD_PRICE)))
    }

    @Test
    fun `검 판매는 골드를 주고 검을 없앤다`() {
        val before = state(gold = 0, sword = Sword(WeaponFamily.STRAIGHT, 10))
        val after = Economy.sellSword(before)
        assertEquals(39275L, after.gold)
        assertNull(after.sword)
    }

    @Test
    fun `검이 없으면 팔 수 없다`() {
        assertFalse(Economy.canSellSword(state()))
    }

    @Test
    fun `파산 판정은 세 조건이 모두 성립할 때만 참이다`() {
        // 검 없음 + 검 살 골드 없음 + 워프권(+5) 살 조각 미만
        assertTrue(
            Economy.needsBailout(
                state(
                    gold = Economy.BASE_SWORD_PRICE - 1,
                    shards = Recipes.SWORD5_SHARD_COST - 1,
                ),
            ),
        )
        // 검이 있으면 아니다
        assertFalse(
            Economy.needsBailout(
                state(gold = 0, shards = 0, sword = Sword(WeaponFamily.STRAIGHT, 0)),
            ),
        )
        // 검을 살 골드가 있으면 아니다
        assertFalse(Economy.needsBailout(state(gold = Economy.BASE_SWORD_PRICE, shards = 0)))
        // +5 검을 바꿀 조각이 있으면 아니다
        assertFalse(Economy.needsBailout(state(gold = 0, shards = Recipes.SWORD5_SHARD_COST)))
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

    /**
     * **사서 되파는 것으로 골드가 늘어서는 안 된다.**
     *
     * +0 검을 [Economy.BASE_SWORD_PRICE] 에 사서 [Economy.sellPrice] 로 되파는 순환이
     * 이득이면 강화할 이유가 통째로 사라진다. 파산 구제가 밑을 받쳐 주므로 무한 순환이 된다.
     * v2.2에서 판매가 바닥을 올렸다가 실제로 이 선을 넘었다.
     */
    @Test
    fun `사서 되파는 것으로 골드가 늘지 않는다`() {
        assertTrue(
            "구매가=${Economy.BASE_SWORD_PRICE} 판매가=${Economy.sellPrice(0)}",
            Economy.BASE_SWORD_PRICE > Economy.sellPrice(0),
        )
        var s = state(gold = 0)
        repeat(50) {
            s = Economy.applyBailoutIfNeeded(s)
            if (Economy.canBuySword(s)) s = Economy.buySword(s, WeaponFamily.STRAIGHT)
            if (Economy.canSellSword(s)) s = Economy.sellSword(s)
        }
        assertTrue("gold=${s.gold}", s.gold <= Economy.BAILOUT_GOLD)
    }
}
