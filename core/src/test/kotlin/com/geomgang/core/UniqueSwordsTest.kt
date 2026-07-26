package com.geomgang.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UniqueSwordsTest {

    private fun swords(vararg pairs: Pair<WeaponFamily, Int>) =
        pairs.map { Sword(it.first, it.second) }

    // --- 매칭 ---

    @Test
    fun `삼위일체 - 성검 셋 10단계 이상`() {
        val recipe = UniqueSwords.match(
            swords(
                WeaponFamily.HOLY to 10, WeaponFamily.HOLY to 12, WeaponFamily.HOLY to 11,
            ),
            emptyMap(),
        )
        assertEquals("trinity", recipe?.id)
    }

    @Test
    fun `단계가 모자라면 불발`() {
        assertNull(
            UniqueSwords.match(
                swords(
                    WeaponFamily.HOLY to 9, WeaponFamily.HOLY to 12, WeaponFamily.HOLY to 11,
                ),
                emptyMap(),
            ),
        )
    }

    @Test
    fun `정수가 모자라면 불발`() {
        val materials = swords(WeaponFamily.DRAGON to 15, WeaponFamily.DRAGON to 16)
        assertNull(UniqueSwords.match(materials, mapOf("dragon_nest" to 2)))
        assertEquals(
            "dragon_fang",
            UniqueSwords.match(materials, mapOf("dragon_nest" to 3))?.id,
        )
    }

    @Test
    fun `탐식자 - 마검 둘과 아무 검 둘`() {
        val recipe = UniqueSwords.match(
            swords(
                WeaponFamily.DEMON to 3, WeaponFamily.DEMON to 5,
                WeaponFamily.GREAT to 1, WeaponFamily.SCYTHE to 2,
            ),
            emptyMap(),
        )
        assertEquals("glutton", recipe?.id)
    }

    @Test
    fun `구체 레시피가 아무 검 레시피보다 우선한다`() {
        // 마검 셋 +16 + 심연 정수 = 심연을 삼킨 검. 탐식자(마검2+아무2)로 새면 안 된다.
        val materials = swords(
            WeaponFamily.DEMON to 16, WeaponFamily.DEMON to 17, WeaponFamily.DEMON to 18,
        )
        assertEquals(
            "abyss_eater",
            UniqueSwords.match(materials, mapOf("abyss" to 5))?.id,
        )
    }

    @Test
    fun `행운아 - 네 계열이 전부 달라야 한다`() {
        val ok = UniqueSwords.match(
            swords(
                WeaponFamily.CURVED to 0, WeaponFamily.RAPIER to 3,
                WeaponFamily.TWIN to 1, WeaponFamily.SPEAR to 2,
            ),
            emptyMap(),
        )
        assertEquals("lucky", ok?.id)
    }

    @Test
    fun `고유검은 재료가 될 수 없다`() {
        val withUnique = listOf(
            Sword(WeaponFamily.HOLY, 12, uniqueId = "trinity"),
            Sword(WeaponFamily.HOLY, 12),
            Sword(WeaponFamily.HOLY, 12),
        )
        assertNull(UniqueSwords.match(withUnique, emptyMap()))
    }

    @Test
    fun `재료 수가 넘치면 불발`() {
        assertNull(
            UniqueSwords.match(
                swords(
                    WeaponFamily.STRAIGHT to 1, WeaponFamily.STRAIGHT to 1,
                    WeaponFamily.STRAIGHT to 1, WeaponFamily.STRAIGHT to 1,
                    WeaponFamily.STRAIGHT to 1,
                ),
                emptyMap(),
            ),
        )
    }

    @Test
    fun `레시피는 10종이고 id가 겹치지 않는다`() {
        assertEquals(10, UniqueSwords.RECIPES.size)
        assertEquals(10, UniqueSwords.RECIPES.map { it.id }.toSet().size)
    }

    // --- 패시브 ---

    @Test
    fun `패시브 수치 표`() {
        fun u(id: String) = Sword(WeaponFamily.STRAIGHT, 10, uniqueId = id)
        assertEquals(1.4, UniqueSwords.bossBonusOf(u("trinity")), 0.0)
        assertEquals(2.0, UniqueSwords.shardMultOf(u("glutton")), 0.0)
        assertEquals(3.0, UniqueSwords.burnMultOf(u("dragon_fang")), 0.0)
        assertEquals(2.0, UniqueSwords.dropMultOf(u("lucky")), 0.0)
        assertEquals(0.10, UniqueSwords.critBonusOf(u("cleaver")), 0.0)
        assertEquals(0.7, UniqueSwords.tapIntervalMultOf(u("tempest")), 0.0)
        assertEquals(0.02, UniqueSwords.maxHpRatioOf(u("abyss_eater")), 0.0)
        assertEquals(1.5, UniqueSwords.goldMultOf(u("bloom")), 0.0)
        assertEquals(0.03, UniqueSwords.forgeBonusOf(u("origin")), 0.0)
        assertTrue(UniqueSwords.canRevive(u("phoenix")))
    }

    @Test
    fun `평범한 검과 null은 전부 중립값`() {
        val plain = Sword(WeaponFamily.HOLY, 15)
        for (sword in listOf(plain, null)) {
            assertEquals(1.0, UniqueSwords.bossBonusOf(sword), 0.0)
            assertEquals(1.0, UniqueSwords.shardMultOf(sword), 0.0)
            assertEquals(1.0, UniqueSwords.burnMultOf(sword), 0.0)
            assertEquals(1.0, UniqueSwords.dropMultOf(sword), 0.0)
            assertEquals(0.0, UniqueSwords.critBonusOf(sword), 0.0)
            assertEquals(1.0, UniqueSwords.tapIntervalMultOf(sword), 0.0)
            assertEquals(0.0, UniqueSwords.maxHpRatioOf(sword), 0.0)
            assertEquals(1.0, UniqueSwords.goldMultOf(sword), 0.0)
            assertEquals(0.0, UniqueSwords.forgeBonusOf(sword), 0.0)
            assertFalse(UniqueSwords.canRevive(sword))
        }
    }

    @Test
    fun `모든 레시피의 정수 구역 id는 실제 구역이다`() {
        for (recipe in UniqueSwords.RECIPES) {
            for (zoneId in recipe.essences.keys) {
                assertTrue("$zoneId 없음", Zone.entries.any { it.id == zoneId })
            }
        }
    }
}
