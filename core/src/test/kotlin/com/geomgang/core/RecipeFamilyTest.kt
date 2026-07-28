package com.geomgang.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 조각 교환으로 받는 검의 계열.
 *
 * 화면이 고르게 하지 않는 대신 도감이 덜 찬 계열을 먼저 준다. 무작위지만 굴림값을
 * 밖에서 넣으므로 여기서 결과를 못 박을 수 있다.
 */
class RecipeFamilyTest {

    private val basics = listOf(
        WeaponFamily.STRAIGHT,
        WeaponFamily.CURVED,
        WeaponFamily.GREAT,
        WeaponFamily.RAPIER,
    )

    @Test
    fun `도감이 덜 찬 계열만 나온다`() {
        val incomplete = setOf(WeaponFamily.GREAT, WeaponFamily.RAPIER)
        // 굴림값을 전부 훑어도 다 찬 계열은 절대 나오지 않는다
        val picked = (0 until 40).map { Recipes.familyFor(basics, incomplete, it) }.toSet()
        assertEquals(incomplete, picked)
    }

    @Test
    fun `덜 찬 계열이 없으면 열린 계열 전부에서 고른다`() {
        val picked = (0 until 40).map { Recipes.familyFor(basics, emptySet(), it) }.toSet()
        assertEquals(basics.toSet(), picked)
    }

    /** 잠긴 계열은 덜 찼더라도 주지 않는다. 못 여는 검이 손에 들어오면 안 된다. */
    @Test
    fun `열리지 않은 계열은 나오지 않는다`() {
        val unlocked = listOf(WeaponFamily.STRAIGHT)
        val incomplete = setOf(WeaponFamily.GREAT, WeaponFamily.RAPIER)
        repeat(20) {
            assertEquals(WeaponFamily.STRAIGHT, Recipes.familyFor(unlocked, incomplete, it))
        }
    }

    /** 굴림값이 음수로 와도 자리를 벗어나지 않는다. */
    @Test
    fun `굴림값이 음수여도 목록 안에서 고른다`() {
        val picked = Recipes.familyFor(basics, emptySet(), -7)
        assertTrue(picked in basics)
    }

    @Test
    fun `도감이 하나도 안 찼으면 모든 계열이 후보다`() {
        val empty = ProgressState()
        val incomplete = Progress.incompleteFamilies(empty)
        assertTrue(basics.all { it in incomplete })
    }

    /** 한 계열의 칸을 전부 채우면 그 계열은 후보에서 빠진다. */
    @Test
    fun `다 채운 계열은 후보에서 빠진다`() {
        val filled = WeaponCatalog.ENTRIES
            .filter { it.family == WeaponFamily.STRAIGHT }
            .map { CodexKey(it.family, it.tier, Difficulty.ENDLESS) }
            .toSet()

        val incomplete = Progress.incompleteFamilies(ProgressState(codex = filled))
        assertTrue(WeaponFamily.STRAIGHT !in incomplete)
        assertTrue(WeaponFamily.CURVED in incomplete)
    }
}
