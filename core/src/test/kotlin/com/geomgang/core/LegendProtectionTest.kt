package com.geomgang.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LegendProtectionTest {

    private fun season3(
        gold: Long = LegendProtection.GOLD_PRICE,
        shards: Int = LegendProtection.SHARD_PRICE,
    ) = GameState(
        difficulty = Difficulty.ENDLESS,
        gold = gold,
        shards = shards,
        sword = Sword(WeaponFamily.DRAGON, LegendForge.MATERIAL_LEVEL),
        dragonForged = true,
        legendSeasonUnlocked = true,
    )

    @Test
    fun `시즌3에서 천만 골드로 한 장 산다`() {
        val after = LegendProtection.buyWithGold(season3())
        assertEquals(0, after.gold)
        assertEquals(1, after.inventory.legendPreventTickets)
    }

    @Test
    fun `시즌3에서 조각 만 개로 한 장 산다`() {
        val after = LegendProtection.buyWithShards(season3())
        assertEquals(0, after.shards)
        assertEquals(1, after.inventory.legendPreventTickets)
    }

    @Test
    fun `시즌2에는 돈이 있어도 전설 방지권을 살 수 없다`() {
        val season2 = season3().copy(
            sword = Sword(WeaponFamily.DRAGON, LegendForge.CRAFT_LEVEL),
            legendSeasonUnlocked = false,
        )
        assertFalse(LegendProtection.canBuyWithGold(season2))
        assertFalse(LegendProtection.canBuyWithShards(season2))
    }

    @Test
    fun `두 결제 수단은 각각 잔액을 검사한다`() {
        assertFalse(LegendProtection.canBuyWithGold(season3(gold = 9_999_999)))
        assertFalse(LegendProtection.canBuyWithShards(season3(shards = 9_999)))
        assertTrue(LegendProtection.canBuyWithGold(season3()))
        assertTrue(LegendProtection.canBuyWithShards(season3()))
    }
}
