package com.geomgang.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 계열 조합 — +20 두 자루를 태워 새 계열 +1 을 얻는 의식.
 *
 * 시즌1을 길게 만드는 장치다. 마검·성검이 여기서만 나오고, 그 둘을 다시 +20까지
 * 올려야 용검이다. +20 네 자루 값이 곧 시즌1의 길이다.
 */
class RefineryTest {

    private val demonRecipe = Refinery.RECIPES.first { it.result == WeaponFamily.DEMON }

    private fun state(storage: List<Sword>, sword: Sword? = null) = GameState(
        difficulty = Difficulty.NORMAL,
        gold = 1_000,
        sword = sword,
        storage = storage,
    )

    @Test
    fun `레시피는 둘이고 마검과 성검을 만든다`() {
        assertEquals(
            setOf(WeaponFamily.DEMON, WeaponFamily.HOLY),
            Refinery.RECIPES.map { it.result }.toSet(),
        )
        // 결과는 +1 - 시즌1을 길게 만드는 핵심이다. +20에서 시작하면 조합이 곧 완성이 된다.
        assertTrue(Refinery.RECIPES.all { it.resultLevel == 1 })
    }

    @Test
    fun `재료가 다 있으면 조합할 수 있다`() {
        val s = state(
            listOf(
                Sword(WeaponFamily.STRAIGHT, 20),
                Sword(WeaponFamily.CURVED, 20),
            ),
        )
        assertTrue(Refinery.missingFor(s, demonRecipe).isEmpty())
        assertTrue(Refinery.canCraft(s, demonRecipe))
    }

    @Test
    fun `단계가 모자란 재료는 세지 않는다`() {
        val s = state(
            listOf(
                Sword(WeaponFamily.STRAIGHT, 20),
                Sword(WeaponFamily.CURVED, 19),
            ),
        )
        assertEquals(listOf(WeaponFamily.CURVED), Refinery.missingFor(s, demonRecipe))
        assertFalse(Refinery.canCraft(s, demonRecipe))
    }

    @Test
    fun `고유검과 전설검은 재료가 되지 않는다`() {
        val s = state(
            listOf(
                Sword(WeaponFamily.STRAIGHT, 20, uniqueId = "origin"),
                Sword(WeaponFamily.CURVED, 20),
            ),
        )
        assertEquals(listOf(WeaponFamily.STRAIGHT), Refinery.missingFor(s, demonRecipe))
    }

    @Test
    fun `손에 검이 있어도 조합할 수 있다`() {
        // 결과가 보관함으로 들어가므로 손을 비울 이유가 없다
        val s = state(
            listOf(
                Sword(WeaponFamily.STRAIGHT, 20),
                Sword(WeaponFamily.CURVED, 20),
            ),
            sword = Sword(WeaponFamily.GREAT, 5),
        )
        assertTrue(Refinery.canCraft(s, demonRecipe))
    }

    @Test
    fun `조합하면 재료 두 자루가 사라지고 결과가 보관함에 들어온다`() {
        val bystander = Sword(WeaponFamily.GREAT, 7)
        val s = state(
            listOf(
                Sword(WeaponFamily.STRAIGHT, 20),
                bystander,
                Sword(WeaponFamily.CURVED, 20),
            ),
        )
        val after = Refinery.craft(s, demonRecipe)
        assertEquals(2, after.storage.size)
        assertTrue(bystander in after.storage)
        assertTrue(Sword(WeaponFamily.DEMON, 1) in after.storage)
    }

    @Test
    fun `한 계열이 여러 자루면 하나만 태운다`() {
        val s = state(
            listOf(
                Sword(WeaponFamily.STRAIGHT, 20),
                Sword(WeaponFamily.STRAIGHT, 20),
                Sword(WeaponFamily.CURVED, 20),
            ),
        )
        val after = Refinery.craft(s, demonRecipe)
        assertEquals(2, after.storage.size)
        assertTrue(after.storage.any { it.family == WeaponFamily.STRAIGHT })
    }

    /** 용검 재료(마검·성검)는 전부 여기서 나온다 — 사슬이 끊기면 안 된다. */
    @Test
    fun `전설 재료가 전부 조합으로 나온다`() {
        for (material in LegendForge.MATERIALS) {
            assertTrue("$material", Refinery.RECIPES.any { it.result == material })
        }
    }
}
