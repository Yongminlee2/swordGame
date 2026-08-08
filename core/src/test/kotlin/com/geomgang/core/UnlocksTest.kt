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

    /** 기본 계열을 상한까지 올린 것만으로는 아직 초반 국면이다. */
    @Test
    fun `계열 상한까지 올려도 초반 국면이다`() {
        val s = state(bestLevel = RateTable.MAX_FINITE_LEVEL)
        assertFalse(Unlocks.huntOpen(s))
        assertFalse(Unlocks.stonesUsed(s))
    }

    /**
     * **용검을 쥐면 +21 을 밟기 전에도 시즌2다**(v2.5).
     *
     * 용검이 +1 부터 시작하게 되면서 「+21 을 밟았는지」만 보면 조합을 해내고도
     * 스무 단계를 더 올라야 사냥터가 열린다 — 큰 산을 넘은 값을 못 느낀다.
     */
    @Test
    fun `용검을 손에 쥐면 바로 깊은 국면이다`() {
        val s = state(bestLevel = RateTable.MAX_FINITE_LEVEL)
            .copy(sword = Sword(WeaponFamily.DRAGON, LegendForge.CRAFT_LEVEL))
        assertTrue(Unlocks.deepUnlocked(s))
        assertTrue(Unlocks.huntOpen(s))
        assertTrue(Unlocks.stonesUsed(s))
    }

    /** 사냥하려고 검을 바꿔 드는 일이 흔하다 — 보관함에 넣어 둬도 같다. */
    @Test
    fun `보관함의 용검도 깊은 국면을 연다`() {
        val s = state(bestLevel = RateTable.MAX_FINITE_LEVEL)
            .copy(storage = listOf(Sword(WeaponFamily.DRAGON, LegendForge.CRAFT_LEVEL)))
        assertTrue(Unlocks.deepUnlocked(s))
    }

    /** 용검을 팔았거나 도감에 바친 옛 세이브도 열린 채 남는다. */
    @Test
    fun `용검이 없어도 최고 기록이 21이면 열린 채 남는다`() {
        val s = state(bestLevel = LegendForge.LEVEL)
        assertTrue(Unlocks.deepUnlocked(s))
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

    /** 조각은 시즌을 가리지 않는다(v2.3) — 파괴의 재가 워프권의 값이다. */
    @Test
    fun `시즌1 줍기도 조각을 준다`() {
        val destroyed = state(bestLevel = 15).copy(
            sword = null,
            gold = 0,
            pendingDestroy = PendingDestroy(WeaponFamily.STRAIGHT, 12),
        )
        val after = ForgeEngine.applySalvage(destroyed, Random(1))
        assertTrue(after.shards > 0)
        assertEquals(0L, after.gold)
    }

    /** 시즌1 조각의 쓸 곳은 워프권뿐이다 — 소모품·강화석 교환은 시즌2 몫이다. */
    @Test
    fun `시즌1 조각 교환은 워프권만 열린다`() {
        val early = state(bestLevel = 15).copy(sword = null, shards = 10_000)
        val open = Recipes.availableIn(early)
        assertTrue(open.isNotEmpty())
        assertTrue(open.all { it.reward is RecipeReward.GrantSword })
        // canCraft 도 같은 게이트를 지킨다 - UI만 가리면 다른 경로로 샌다
        assertFalse(Recipes.canCraft(early, Recipes.byId("prevent")))
        assertTrue(Recipes.canCraft(early, Recipes.byId("sword15")))

        val deep = early.copy(bestLevel = LegendForge.LEVEL)
        assertTrue(Recipes.canCraft(deep, Recipes.byId("prevent")))
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
