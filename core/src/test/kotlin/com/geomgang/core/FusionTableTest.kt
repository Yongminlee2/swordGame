package com.geomgang.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 조합표 - 계열의 유일한 출처 (v2.1: 두 줄).
 *
 * 조회는 재료의 **계열 집합**으로 한다. 표에 없으면 조합 자체가 안 된다.
 */
class FusionTableTest {

    private fun f(vararg families: WeaponFamily) = families.toSet()

    @Test
    fun `직검과 곡도는 마검이 된다`() {
        assertEquals(
            WeaponFamily.DEMON,
            FusionTable.resultFor(f(WeaponFamily.STRAIGHT, WeaponFamily.CURVED)),
        )
    }

    @Test
    fun `대검과 세검은 성검이 된다`() {
        assertEquals(
            WeaponFamily.HOLY,
            FusionTable.resultFor(f(WeaponFamily.GREAT, WeaponFamily.RAPIER)),
        )
    }

    /** 용검은 표에 없다 - 전설 칸([LegendForge]) 전용이다. */
    @Test
    fun `마검과 성검은 일반 조합으로 안 된다`() {
        assertNull(FusionTable.resultFor(f(WeaponFamily.DEMON, WeaponFamily.HOLY)))
    }

    @Test
    fun `표에 없는 조합은 null 이다`() {
        assertNull(FusionTable.resultFor(f(WeaponFamily.STRAIGHT)))
        assertNull(FusionTable.resultFor(f(WeaponFamily.STRAIGHT, WeaponFamily.GREAT)))
        assertNull(FusionTable.resultFor(f(WeaponFamily.CURVED, WeaponFamily.RAPIER)))
    }

    /** 표의 재료·결과가 전부 노출 계열이어야 한다. 숨긴 계열이 나오면 빈 그림이 뜬다. */
    @Test
    fun `조합표는 노출 계열만 쓴다`() {
        for (entry in FusionTable.ALL) {
            assertTrue("${entry.result}", entry.result in WeaponFamily.VISIBLE)
            for (family in entry.materials) {
                assertTrue("$family", family in WeaponFamily.VISIBLE)
            }
        }
    }

    @Test
    fun `표의 재료와 결과는 서로 겹치지 않는다`() {
        val keys = FusionTable.ALL.map { it.materials }
        assertEquals(keys.size, keys.toSet().size)
        val results = FusionTable.ALL.map { it.result }
        assertEquals(results.size, results.toSet().size)
    }
}
