package com.geomgang.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipesTest {

    private fun state(shards: Int, sword: Sword? = null) =
        // 소모품·강화석 교환은 깊은 국면에서만 열린다(v2.3) - 기본 상태를 그쪽에 둔다
        GameState(Difficulty.NORMAL, shards = shards, sword = sword, bestLevel = LegendForge.LEVEL)

    @Test
    fun `교환식은 7종이고 스펙 조각 가격과 일치한다`() {
        assertEquals(7, Recipes.ALL.size)
        assertEquals(Recipes.STONE_SHARD_COST, Recipes.byId("stone").shardCost)
        assertEquals(10, Recipes.byId("prevent").shardCost)
        assertEquals(30, Recipes.byId("blessing").shardCost)
        assertEquals(60, Recipes.byId("luck").shardCost)
        // v2.3 - 워프권 값 인하(120/400/850 -> 50/150/250). 파괴 두 번·다섯 번·여섯 번쯤이다
        assertEquals(50, Recipes.byId("sword5").shardCost)
        assertEquals(150, Recipes.byId("sword10").shardCost)
        assertEquals(250, Recipes.byId("sword15").shardCost)
    }

    @Test
    fun `조각 5검 교환가는 파산 판정에서 쓰는 상수와 같다`() {
        assertEquals(Recipes.SWORD5_SHARD_COST, Recipes.byId("sword5").shardCost)
    }

    @Test
    fun `조각이 모자라면 교환할 수 없다`() {
        val recipe = Recipes.byId("prevent")
        assertFalse(Recipes.canCraft(state(shards = 9), recipe))
        assertTrue(Recipes.canCraft(state(shards = 10), recipe))
    }

    @Test
    fun `아이템 교환은 조각을 차감하고 아이템을 준다`() {
        val before = state(shards = 35)
        val after = Recipes.craft(before, Recipes.byId("blessing"), WeaponFamily.STRAIGHT)
        assertEquals(5, after.shards)
        assertEquals(1, after.inventory.blessingScrolls)
    }

    @Test
    fun `검 교환은 지정한 계열의 검을 준다`() {
        val before = state(shards = 60)
        val after = Recipes.craft(before, Recipes.byId("sword5"), WeaponFamily.DRAGON)
        assertEquals(10, after.shards)
        assertEquals(Sword(WeaponFamily.DRAGON, 5), after.sword)
    }

    @Test
    fun `검 교환으로 최고 기록이 갱신된다`() {
        // 워프권은 시즌1에도 열려 있으므로 초반 상태로 확인한다
        val before = GameState(Difficulty.NORMAL, shards = 400)
        val after = Recipes.craft(before, Recipes.byId("sword10"), WeaponFamily.STRAIGHT)
        assertEquals(10, after.bestLevel)
    }

    @Test
    fun `이미 검이 있으면 검 교환을 할 수 없다`() {
        val holding = state(shards = 400, sword = Sword(WeaponFamily.STRAIGHT, 3))
        assertFalse(Recipes.canCraft(holding, Recipes.byId("sword5")))
        // 아이템 교환은 검을 들고 있어도 가능하다
        assertTrue(Recipes.canCraft(holding, Recipes.byId("prevent")))
    }

    @Test(expected = IllegalStateException::class)
    fun `조건을 만족하지 않으면 교환은 예외를 던진다`() {
        Recipes.craft(state(shards = 1), Recipes.byId("prevent"), WeaponFamily.STRAIGHT)
    }

    @Test
    fun `교환식 아이디는 서로 겹치지 않는다`() {
        val ids = Recipes.ALL.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }
}
