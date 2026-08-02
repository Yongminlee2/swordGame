package com.geomgang.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 계열별 판매가.
 *
 * 같은 단계면 계열 무관 같은 값이었다 — 조합검을 만들어도 판매가가 그대로라
 * 조합의 값어치가 가격에 없었다. 이제 계열 배수가 붙는다.
 */
class FamilyValueTest {

    @Test
    fun `같은 단계면 조합검이 기본검보다 비싸다`() {
        val level = 10
        val straight = Economy.sellPrice(Sword(WeaponFamily.STRAIGHT, level))
        val demon = Economy.sellPrice(Sword(WeaponFamily.DEMON, level))
        val holy = Economy.sellPrice(Sword(WeaponFamily.HOLY, level))
        assertTrue("마검=$demon 직검=$straight", demon > straight)
        assertTrue("성검=$holy 마검=$demon", holy > demon)
    }

    @Test
    fun `기본 4계열도 해금 순으로 값이 다르다`() {
        val order = listOf(
            WeaponFamily.STRAIGHT,
            WeaponFamily.CURVED,
            WeaponFamily.GREAT,
            WeaponFamily.RAPIER,
        )
        for (i in 1 until order.size) {
            assertTrue(
                "${order[i]}",
                Economy.familyMult(order[i]) > Economy.familyMult(order[i - 1]),
            )
        }
    }

    /** 직검은 기준(1.0)이다. 시뮬레이터가 직검으로만 돌기 때문이다. */
    @Test
    fun `직검 판매가는 기준 곡선 그대로다`() {
        for (level in 0..20) {
            assertEquals(
                Economy.sellPrice(level),
                Economy.sellPrice(Sword(WeaponFamily.STRAIGHT, level)),
            )
        }
    }

    /**
     * **사서 되파는 것으로 골드가 늘어서는 안 된다** — 계열 배수가 붙어도.
     *
     * 상점에서 살 수 있는 기본 4계열의 +0 판매가가 구매가를 넘으면
     * 무한 골드 순환이 생긴다(파산 구제가 밑을 받친다).
     */
    @Test
    fun `기본 계열은 배수가 붙어도 사서 되팔면 손해다`() {
        for (family in WeaponFamily.BASICS) {
            assertTrue(
                "$family: ${Economy.sellPrice(Sword(family, 0))}",
                Economy.sellPrice(Sword(family, 0)) < Economy.BASE_SWORD_PRICE,
            )
        }
    }

    /**
     * **조합해 팔기 < 재료 둘을 그냥 팔기.**
     *
     * 1.8 곡선의 볼록성과 평균 단계가 이 부등식을 지킨다. 조합이 돈벌이가 되면
     * 강화 대신 조합만 돌리는 게 최선이 된다. 프리미엄은 손해를 덜어 줄 뿐이다.
     */
    @Test
    fun `조합해 팔기는 재료 둘을 파는 것보다 늘 손해다`() {
        for (a in 0..20) {
            for (b in 0..20) {
                val materials = Economy.sellPrice(Sword(WeaponFamily.STRAIGHT, a)) +
                    Economy.sellPrice(Sword(WeaponFamily.CURVED, b))
                val fused = Economy.sellPrice(Sword(WeaponFamily.DEMON, (a + b) / 2))
                assertTrue("a=$a b=$b 재료=$materials 마검=$fused", fused < materials)
            }
        }
    }

    /** 모든 계열이 배수를 갖는다. when 이 else 없이 전 계열을 덮는 것의 확인이다. */
    @Test
    fun `모든 계열의 배수가 1 이상이다`() {
        for (family in WeaponFamily.entries) {
            assertTrue("$family", Economy.familyMult(family) >= 1.0)
        }
    }
}
