package com.geomgang.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 고유검 6종 - 전부 재료 두 자루다(v2.1). */
class UniqueSwordsTest {

    private fun swords(vararg pairs: Pair<WeaponFamily, Int>) =
        pairs.map { Sword(it.first, it.second) }

    // --- 매칭 ---

    @Test
    fun `삼위일체 - 성검 둘 10단계 이상`() {
        val recipe = UniqueSwords.match(
            swords(WeaponFamily.HOLY to 10, WeaponFamily.HOLY to 12),
            emptyMap(),
        )
        assertEquals("trinity", recipe?.id)
    }

    @Test
    fun `단계가 모자라면 불발`() {
        assertNull(
            UniqueSwords.match(
                swords(WeaponFamily.HOLY to 9, WeaponFamily.HOLY to 12),
                emptyMap(),
            ),
        )
    }

    @Test
    fun `정수가 모자라면 불발`() {
        val materials = swords(WeaponFamily.DEMON to 16, WeaponFamily.DEMON to 17)
        // 심연은 abyss 정수 5가 필요하다. 4로는 탐식자(정수 없음)로 떨어진다.
        assertEquals("glutton", UniqueSwords.match(materials, mapOf("abyss" to 4))?.id)
        assertEquals("abyss_eater", UniqueSwords.match(materials, mapOf("abyss" to 5))?.id)
    }

    @Test
    fun `탐식자 - 마검 아무 둘`() {
        val recipe = UniqueSwords.match(
            swords(WeaponFamily.DEMON to 3, WeaponFamily.DEMON to 5),
            emptyMap(),
        )
        assertEquals("glutton", recipe?.id)
    }

    @Test
    fun `불사조 - 성검과 마검 12단계 이상과 화산 정수`() {
        val materials = swords(WeaponFamily.HOLY to 12, WeaponFamily.DEMON to 13)
        assertNull(UniqueSwords.match(materials, emptyMap()))
        assertEquals("phoenix", UniqueSwords.match(materials, mapOf("volcano" to 3))?.id)
    }

    @Test
    fun `시작의 검 - 직검 둘`() {
        val recipe = UniqueSwords.match(
            swords(WeaponFamily.STRAIGHT to 0, WeaponFamily.STRAIGHT to 1),
            emptyMap(),
        )
        assertEquals("origin", recipe?.id)
    }

    // --- 목록 규칙 ---

    @Test
    fun `레시피는 6종이고 id가 겹치지 않는다`() {
        assertEquals(6, UniqueSwords.RECIPES.size)
        assertEquals(
            UniqueSwords.RECIPES.size,
            UniqueSwords.RECIPES.map { it.id }.toSet().size,
        )
    }

    /** 조합이 두 자루라 레시피도 전부 두 자루여야 한다. 아니면 영원히 못 만든다. */
    @Test
    fun `모든 레시피는 재료 두 자루다`() {
        for (recipe in UniqueSwords.RECIPES) {
            assertEquals(recipe.id, 2, recipe.needs.sumOf { it.third })
        }
    }

    /** 숨긴 계열을 쓰는 레시피가 남아 있으면 조합소에 만들 수 없는 힌트가 뜬다. */
    @Test
    fun `레시피는 노출 계열만 쓴다`() {
        for (recipe in UniqueSwords.RECIPES) {
            assertTrue(recipe.id, recipe.resultFamily in WeaponFamily.VISIBLE)
            for ((family, _, _) in recipe.needs) {
                if (family != null) {
                    assertTrue("${recipe.id}: $family", family in WeaponFamily.VISIBLE)
                }
            }
        }
    }

    /** 구체 레시피(단계 하한·정수)가 넓은 레시피보다 앞이어야 재료가 새지 않는다. */
    @Test
    fun `심연이 탐식자보다 앞이다`() {
        val ids = UniqueSwords.RECIPES.map { it.id }
        assertTrue(ids.indexOf("abyss_eater") < ids.indexOf("glutton"))
    }

    // --- 보유 보너스 ---

    @Test
    fun `발견한 고유검 수만큼 보너스가 쌓인다`() {
        val p = ProgressState(uniqueFound = setOf("origin", "glutton"))
        assertEquals(
            UniqueSwords.PER_UNIQUE * 2,
            UniqueSwords.holdingBonus(p).successRate,
            1e-9,
        )
    }

    @Test
    fun `모르는 id는 세지 않는다`() {
        // 내려간 옛 레시피(dragon_fang 등)를 발견한 세이브도 그대로 열린다
        val p = ProgressState(uniqueFound = setOf("origin", "dragon_fang", "hacked"))
        assertEquals(
            UniqueSwords.PER_UNIQUE * 1,
            UniqueSwords.holdingBonus(p).successRate,
            1e-9,
        )
    }
}
