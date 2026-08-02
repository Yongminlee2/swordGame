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
    fun `삼위일체 - 대검 둘 14단계 이상`() {
        val recipe = UniqueSwords.match(
            swords(WeaponFamily.GREAT to 14, WeaponFamily.GREAT to 16),
            emptyMap(),
        )
        assertEquals("trinity", recipe?.id)
    }

    @Test
    fun `단계가 모자라면 불발`() {
        assertNull(
            UniqueSwords.match(
                swords(WeaponFamily.GREAT to 13, WeaponFamily.GREAT to 16),
                emptyMap(),
            ),
        )
    }

    @Test
    fun `정수가 모자라면 불발`() {
        val materials = swords(WeaponFamily.DEMON to 12, WeaponFamily.DEMON to 13)
        // 심연은 abyss 정수 5가 필요하다. 모자라면 받아 줄 레시피가 없다 -
        // v2.3에서 탐식자가 곡도로 옮겨져 마검 둘을 받는 레시피는 심연뿐이다.
        assertNull(UniqueSwords.match(materials, mapOf("abyss" to 4)))
        assertEquals("abyss_eater", UniqueSwords.match(materials, mapOf("abyss" to 5))?.id)
    }

    @Test
    fun `탐식자 - 곡도 둘 12단계 이상`() {
        val recipe = UniqueSwords.match(
            swords(WeaponFamily.CURVED to 12, WeaponFamily.CURVED to 14),
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
    fun `시작의 검 - 깊이 벼린 직검 둘`() {
        // +10 하한(v2.3) - 직검 두 자루 값에 영구 보너스는 너무 쌌다
        val recipe = UniqueSwords.match(
            swords(WeaponFamily.STRAIGHT to 10, WeaponFamily.STRAIGHT to 12),
            emptyMap(),
        )
        assertEquals("origin", recipe?.id)

        val shallow = UniqueSwords.match(
            swords(WeaponFamily.STRAIGHT to 0, WeaponFamily.STRAIGHT to 1),
            emptyMap(),
        )
        assertNull(shallow)
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

    /**
     * 시즌1에서 손에 넣을 수 있는 길이 반드시 있어야 한다.
     *
     * v2.3 이전에는 여섯 중 넷이 마검·성검을 요구했는데, 그 둘이 「+20 두 자루의
     * 의식」이 되면서 고유검에 닿는 길이 사실상 사라졌다. 기본 4계열로 만들 수 있는
     * 레시피가 없어지면 같은 일이 다시 일어난다.
     */
    @Test
    fun `기본 4계열마다 정수 없는 레시피가 하나씩 있다`() {
        val basicOnly = UniqueSwords.RECIPES.filter { recipe ->
            recipe.essences.isEmpty() &&
                recipe.needs.all { (family, _, _) -> family in WeaponFamily.BASICS }
        }
        val families = basicOnly.flatMap { it.needs.mapNotNull { need -> need.first } }.toSet()
        assertEquals(WeaponFamily.BASICS.toSet(), families)
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
