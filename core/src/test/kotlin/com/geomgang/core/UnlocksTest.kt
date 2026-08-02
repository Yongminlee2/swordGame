package com.geomgang.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * 두 국면의 경계.
 *
 * 용검을 손에 쥐기 전에는 **골드 하나로만** 논다 — 강화 → 판매 → 강화.
 * 강화석·조각·사냥터가 초반에 끼어들면 그 고리가 끊긴다.
 */
class UnlocksTest {

    private fun state(bestLevel: Int, level: Int = 15) = GameState(
        difficulty = Difficulty.ENDLESS,
        gold = 1_000_000_000,
        sword = Sword(WeaponFamily.STRAIGHT, level),
        bestLevel = bestLevel,
    )

    @Test
    fun `용검을 밟아야 깊은 국면이 열린다`() {
        assertFalse(Unlocks.legendReached(RateTable.MAX_FINITE_LEVEL))
        assertTrue(Unlocks.legendReached(LegendForge.LEVEL))
        assertTrue(Unlocks.legendReached(LegendForge.LEVEL + 9))
    }

    /** 계열이 +20 에서 끝나므로 +21 을 밟았다는 것은 용검을 벼렸다는 뜻이다. */
    @Test
    fun `계열 상한까지 올려도 초반 국면이다`() {
        val s = state(bestLevel = RateTable.MAX_FINITE_LEVEL)
        assertFalse(Unlocks.huntOpen(s))
        assertFalse(Unlocks.stonesUsed(s))
        assertFalse(Unlocks.shardsUsed(s))
    }

    // --- 강화석 ---

    @Test
    fun `초반 강화는 강화석을 묻지 않는다`() {
        // +16 은 원래 강화석이 필수인 구간이다
        val early = state(bestLevel = 15, level = 15)
        assertTrue(ForgeCost.requirementFor(15).stones > 0)
        assertEquals(0, ForgeCost.requirementOf(early)?.stones)
        assertTrue("골드만 있으면 굴려야 한다", ForgeCost.canPay(early))
    }

    @Test
    fun `용검 뒤에는 강화석을 다시 묻는다`() {
        val deep = state(bestLevel = LegendForge.LEVEL, level = 15)
        assertEquals(ForgeCost.requirementFor(15).stones, ForgeCost.requirementOf(deep)?.stones)
    }

    /** 강화석이 없다고 강화가 막히면 강화대를 떠나야 한다 - 그게 이 변경의 이유다. */
    @Test
    fun `강화석 없이도 초반 고단계 강화가 실행된다`() {
        val early = state(bestLevel = 18, level = 18).copy(forgeStones = 0)
        assertTrue(ForgeEngine.canAttempt(early, UsedItems.NONE))
    }

    // --- 조각 ---

    @Test
    fun `초반 줍기는 조각 대신 골드를 준다`() {
        val destroyed = state(bestLevel = 15).copy(
            sword = null,
            gold = 0,
            pendingDestroy = PendingDestroy(WeaponFamily.STRAIGHT, 12),
        )
        val after = ForgeEngine.applySalvage(destroyed, Random(1))
        assertEquals(0, after.shards)
        assertEquals(Unlocks.salvageGold(12), after.gold)
    }

    @Test
    fun `용검 뒤 줍기는 조각을 준다`() {
        val destroyed = state(bestLevel = LegendForge.LEVEL).copy(
            sword = null,
            gold = 0,
            pendingDestroy = PendingDestroy(WeaponFamily.STRAIGHT, 12),
        )
        val after = ForgeEngine.applySalvage(destroyed, Random(1))
        assertTrue(after.shards > 0)
        assertEquals(0L, after.gold)
    }

    /** 파괴는 여전히 손해여야 한다. 주워 든 골드가 판매가를 넘으면 파괴가 이득이 된다. */
    @Test
    fun `줍기 골드는 판매가보다 적다`() {
        for (level in 1..RateTable.MAX_FINITE_LEVEL) {
            assertTrue("level=$level", Unlocks.salvageGold(level) < Economy.sellPrice(level))
        }
    }

    // --- 자리비움 ---

    /**
     * 시즌1에도 자리비움은 있다. 다만 **대장간이 벌어 준 골드**다 —
     * 사냥터가 없으니 가리킬 구역이 없고, 화폐가 하나뿐이니 강화석도 없다.
     */
    @Test
    fun `시즌1 자리비움은 골드만 준다`() {
        val early = state(bestLevel = 15)
        val reward = requireNotNull(IdleRewards.rewardFor(early, 3600))
        assertEquals(null, reward.zone)
        assertEquals(0, reward.stones)
        assertTrue("gold=${reward.gold}", reward.gold > 0)
    }

    /** 진행도에 매단다 — 고정값은 초반에 과하고 후반에 먼지가 된다. */
    @Test
    fun `시즌1 자리비움은 진행도를 따라 커진다`() {
        val low = requireNotNull(IdleRewards.rewardFor(state(bestLevel = 5), 3600)).gold
        val high = requireNotNull(IdleRewards.rewardFor(state(bestLevel = 15), 3600)).gold
        assertTrue("low=$low high=$high", high > low)
    }

    /** 자리비움은 덤이다. 한 자루 파는 것보다 한참 아래여야 한다. */
    @Test
    fun `시즌1 자리비움은 검 한 자루 판 값을 넘지 않는다`() {
        for (level in 5..RateTable.MAX_FINITE_LEVEL) {
            val full = requireNotNull(
                IdleRewards.rewardFor(state(bestLevel = level), IdleRewards.MAX_SECONDS),
            )
            assertTrue("level=$level 자리비움=${full.gold}", full.gold < Economy.sellPrice(level))
        }
    }

    @Test
    fun `용검 뒤에는 구역이 벌어 준다`() {
        val deep = state(bestLevel = LegendForge.LEVEL)
        val reward = requireNotNull(IdleRewards.rewardFor(deep, 3600))
        assertTrue(reward.zone != null)
    }
}
