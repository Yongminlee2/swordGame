package com.geomgang.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 강화석이 어디서 나오는지. 고단계 강화의 화폐이므로 경로가 여러 갈래여야 한다. */
class ForgeStoneSourceTest {

    @Test
    fun `분해하면 강화석도 나온다`() {
        val state = GameState(
            difficulty = Difficulty.ENDLESS,
            storage = listOf(Sword(WeaponFamily.STRAIGHT, 16)),
        )
        val after = Storage.scrap(state, 0)
        assertEquals(Storage.scrapStones(Sword(WeaponFamily.STRAIGHT, 16)), after.forgeStones)
        assertTrue(after.shards > 0)
    }

    @Test
    fun `단계가 높은 검이 강화석을 더 준다 - 상한 3`() {
        assertEquals(1, Storage.scrapStones(Sword(WeaponFamily.STRAIGHT, 0)))
        assertEquals(2, Storage.scrapStones(Sword(WeaponFamily.STRAIGHT, 8)))
        assertEquals(3, Storage.scrapStones(Sword(WeaponFamily.STRAIGHT, 16)))
        assertEquals(3, Storage.scrapStones(Sword(WeaponFamily.STRAIGHT, 60)))
    }

    @Test
    fun `모든 구역 보스가 강화석을 준다`() {
        for (zone in Zone.entries) {
            assertTrue("${zone.displayName} 보스 강화석", zone.bossStones >= 3)
        }
    }

    @Test
    fun `깊은 구역 보스가 더 많이 준다`() {
        assertTrue(Zone.ABYSS.bossStones > Zone.MEADOW.bossStones)
    }

    @Test
    fun `조각을 강화석으로 바꿀 수 있다`() {
        val recipe = Recipes.byId("stone")
        assertEquals(Recipes.STONE_SHARD_COST, recipe.shardCost)
        val state = GameState(difficulty = Difficulty.ENDLESS, shards = 100)
        val after = Recipes.craft(state, recipe, WeaponFamily.STRAIGHT)
        assertEquals(1, after.forgeStones)
        assertEquals(100 - Recipes.STONE_SHARD_COST, after.shards)
    }

    @Test
    fun `강화석 교환은 검을 들고 있어도 된다`() {
        val recipe = Recipes.byId("stone")
        val state = GameState(
            difficulty = Difficulty.ENDLESS,
            shards = 100,
            sword = Sword(WeaponFamily.STRAIGHT, 5),
        )
        assertTrue(Recipes.canCraft(state, recipe))
    }
}
