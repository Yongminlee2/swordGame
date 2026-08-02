package com.geomgang.core

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 한 번에 여러 개 바꾸기.
 *
 * 고단계 강화석은 한 판에 열댓 개가 든다. 하나씩 누르게 두면 바꾸는 일이 노가다가 된다 —
 * 고민할 거리가 아니라 손가락 문제다.
 */
class CraftManyTest {

    private val stone = Recipes.byId("stone")
    private val sword5 = Recipes.byId("sword5")

    // 소모품·강화석 교환은 깊은 국면 전용이다(v2.3)
    private fun state(shards: Int, sword: Sword? = null) = GameState(
        bestLevel = LegendForge.LEVEL,
        difficulty = Difficulty.ENDLESS,
        shards = shards,
        sword = sword,
    )

    @Test
    fun `조각이 닿는 만큼 센다`() {
        assertEquals(5, Recipes.maxCraftable(state(Recipes.STONE_SHARD_COST * 5), stone))
        assertEquals(0, Recipes.maxCraftable(state(0), stone))
    }

    /** 손은 하나뿐이라 검은 언제나 한 자루다. */
    @Test
    fun `검은 여러 자루를 한 번에 받지 못한다`() {
        assertEquals(1, Recipes.maxCraftable(state(100_000), sword5))
    }

    @Test
    fun `여러 개를 한 번에 바꾸면 조각이 그만큼 빠진다`() {
        val before = state(Recipes.STONE_SHARD_COST * 7)
        val after = Recipes.craftMany(before, stone, 5, WeaponFamily.STRAIGHT)
        assertEquals(5, after.forgeStones)
        assertEquals(Recipes.STONE_SHARD_COST * 2, after.shards)
    }

    /** 살 수 있는 것보다 많이 부르면 살 수 있는 만큼만 바꾼다. */
    @Test
    fun `넘치게 불러도 조각이 모자라지 않는다`() {
        val before = state(Recipes.STONE_SHARD_COST * 3)
        val after = Recipes.craftMany(before, stone, 999, WeaponFamily.STRAIGHT)
        assertEquals(3, after.forgeStones)
        assertEquals(0, after.shards)
    }

    @Test
    fun `조각이 모자라면 아무 일도 없다`() {
        val before = state(Recipes.STONE_SHARD_COST - 1)
        assertEquals(before, Recipes.craftMany(before, stone, 10, WeaponFamily.STRAIGHT))
    }

    @Test
    fun `0개나 음수를 부르면 아무 일도 없다`() {
        val before = state(1_000)
        assertEquals(before, Recipes.craftMany(before, stone, 0, WeaponFamily.STRAIGHT))
        assertEquals(before, Recipes.craftMany(before, stone, -3, WeaponFamily.STRAIGHT))
    }

    /** 검을 들고 있으면 검 교환은 하나도 안 된다. */
    @Test
    fun `검을 들고 있으면 검 교환은 막힌다`() {
        val holding = state(100_000, Sword(WeaponFamily.STRAIGHT, 3))
        assertEquals(0, Recipes.maxCraftable(holding, sword5))
        assertEquals(holding, Recipes.craftMany(holding, sword5, 5, WeaponFamily.STRAIGHT))
    }
}
