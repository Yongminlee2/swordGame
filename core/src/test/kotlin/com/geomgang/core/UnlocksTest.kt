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

    /**
     * **한 번 열린 시즌2는 닫히지 않는다.**
     *
     * 「지금 용검을 들고 있는가」로만 재던 동안에는 그 검을 팔거나 도감에 바치는 순간
     * 사냥터가 잠기고 강화석이 지갑에서 사라졌다 — 한 번 넘은 산을 다시 넘게 하는 셈.
     */
    @Test
    fun `용검을 조합하면 그 검을 잃어도 시즌2가 닫히지 않는다`() {
        val ready = GameState(
            difficulty = Difficulty.ENDLESS,
            storage = LegendForge.MATERIALS.map { Sword(it, LegendForge.MATERIAL_LEVEL) },
        )
        val (forged, _) = LegendForge.craft(ready, ProgressState())
        assertTrue(forged.dragonForged)
        assertTrue(Unlocks.deepUnlocked(forged))

        // 그 용검을 팔았다 - 손에도 보관함에도 없고, +21 을 밟은 적도 없다
        val sold = forged.copy(sword = null)
        assertFalse(Unlocks.dragonOwned(sold))
        assertFalse(Unlocks.legendReached(sold))
        assertTrue("시즌2가 닫히면 안 된다", Unlocks.deepUnlocked(sold))
        assertTrue(Unlocks.huntOpen(sold))
    }

    /** 모드를 초기화하면 시즌1부터 다시다 — 시즌은 판에 속한 개념이다. */
    @Test
    fun `새 판은 시즌1에서 시작한다`() {
        assertFalse(Unlocks.deepUnlocked(GameState(Difficulty.ENDLESS)))
    }

    /**
     * **경계를 보는 곳이 하나여야 한다.** 상점 목록만 [Unlocks.legendReached] 를
     * 보던 탓에, 용검을 쥔 뒤 +21 전까지는 목록에 뜨는데 사면 거절당했다(v2.5).
     */
    @Test
    fun `용검을 쥐면 상점 교환도 함께 열린다`() {
        val s = state(bestLevel = RateTable.MAX_FINITE_LEVEL)
            .copy(sword = Sword(WeaponFamily.DRAGON, LegendForge.CRAFT_LEVEL))
        assertEquals(Recipes.ALL.size, Recipes.availableIn(s).size)
    }

    @Test
    fun `용검이 없으면 상점 교환은 워프권뿐이다`() {
        val s = state(bestLevel = RateTable.MAX_FINITE_LEVEL)
        assertTrue(Recipes.availableIn(s).size < Recipes.ALL.size)
        assertTrue(Recipes.availableIn(s).all { it.reward is RecipeReward.GrantSword })
    }

    /**
     * 용검은 **잃는 길을 전부 막는다.** 마검·성검을 +20 까지 다시 올려야 되찾는
     * 검이라, 조각 몇십 개로 바꾸거나 재료로 녹이는 것은 선택지가 아니라 함정이다.
     * `!isLegend()` 만 보던 시절에는 용검이 늘 +21 이라 저절로 막혔는데,
     * +1 부터 시작하면서 +20 이하 용검이 두 구멍으로 다 샜다(v2.5).
     */
    @Test
    fun `20 이하 용검도 부수거나 녹일 수 없다`() {
        for (level in intArrayOf(LegendForge.CRAFT_LEVEL, 10, RateTable.MAX_FINITE_LEVEL)) {
            val dragon = Sword(WeaponFamily.DRAGON, level)
            assertFalse("+$level 용검 분해", Storage.canScrap(dragon))
            assertFalse("+$level 용검 조합 재료", Fusion.meltable(dragon))
        }
        // 마검·성검은 고유검 레시피의 재료라 녹을 수 있어야 한다
        assertTrue(Fusion.meltable(Sword(WeaponFamily.DEMON, 12)))
    }

    /**
     * 별강화는 **용검부터**다 — 단계가 아니라 계열로 갈린다(v2.5).
     *
     * 「+20 에서 재료로 태워진다」와 「시즌1엔 사냥터가 없다」가 별을 잠근 이유였는데,
     * 용검은 태워지지 않고 사냥터도 이미 열려 있다.
     */
    @Test
    fun `용검은 1강부터 별을 붙일 수 있다`() {
        assertTrue(StarForce.canStar(Sword(WeaponFamily.DRAGON, LegendForge.CRAFT_LEVEL)))
        assertFalse(StarForce.canStar(Sword(WeaponFamily.DEMON, RateTable.MAX_FINITE_LEVEL)))
        // 옛 세이브의 비(非)용검 전설검도 그대로 열린다
        assertTrue(StarForce.canStar(Sword(WeaponFamily.STRAIGHT, LegendForge.LEVEL)))
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
