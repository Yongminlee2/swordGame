package com.geomgang.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 조합표 — 계열의 유일한 출처.
 *
 * 조회는 재료의 **계열 집합**으로 한다. 직검 2자루든 3자루든 집합은 {직검} 이라
 * 똑같이 쌍검이 되고, 자루 수는 결과 단계에만 영향을 준다.
 */
class FusionTableTest {

    private fun f(vararg families: WeaponFamily) = families.toSet()

    @Test
    fun `직검 둘은 쌍검이 된다`() {
        assertEquals(WeaponFamily.TWIN, FusionTable.resultFor(f(WeaponFamily.STRAIGHT)))
    }

    @Test
    fun `직검과 대검은 도끼검이 된다`() {
        assertEquals(
            WeaponFamily.AXE,
            FusionTable.resultFor(f(WeaponFamily.STRAIGHT, WeaponFamily.GREAT)),
        )
    }

    @Test
    fun `곡도 둘은 낫검이 된다`() {
        assertEquals(WeaponFamily.SCYTHE, FusionTable.resultFor(f(WeaponFamily.CURVED)))
    }

    @Test
    fun `세검 둘은 창검이 된다`() {
        assertEquals(WeaponFamily.SPEAR, FusionTable.resultFor(f(WeaponFamily.RAPIER)))
    }

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

    @Test
    fun `마검과 성검은 용검이 된다`() {
        assertEquals(
            WeaponFamily.DRAGON,
            FusionTable.resultFor(f(WeaponFamily.DEMON, WeaponFamily.HOLY)),
        )
    }

    @Test
    fun `낫검과 창검은 정령검이 된다`() {
        assertEquals(
            WeaponFamily.SPIRIT,
            FusionTable.resultFor(f(WeaponFamily.SCYTHE, WeaponFamily.SPEAR)),
        )
    }

    @Test
    fun `서로 다른 네 계열은 합검이 된다`() {
        assertEquals(
            WeaponFamily.FUSED,
            FusionTable.resultFor(
                f(
                    WeaponFamily.STRAIGHT,
                    WeaponFamily.CURVED,
                    WeaponFamily.GREAT,
                    WeaponFamily.RAPIER,
                ),
            ),
        )
        // 어떤 네 계열이든 합검이다
        assertEquals(
            WeaponFamily.FUSED,
            FusionTable.resultFor(
                f(
                    WeaponFamily.DEMON,
                    WeaponFamily.HOLY,
                    WeaponFamily.SCYTHE,
                    WeaponFamily.AXE,
                ),
            ),
        )
    }

    @Test
    fun `표에 없는 조합은 null 이다`() {
        assertNull(FusionTable.resultFor(f(WeaponFamily.DEMON)))
        assertNull(FusionTable.resultFor(f(WeaponFamily.SPIRIT, WeaponFamily.AXE)))
    }

    /**
     * 허검을 회랑 전용에서 내렸다.
     *
     * 강화 게임인데 회랑 진도에 강제로 묶이는 것이 전설검 재료로는 너무 높은 문턱이었다.
     * 회랑 10층 보상은 그대로 남아 있어 얻는 길이 둘이 된다.
     */
    @Test
    fun `허검은 조합으로도 얻는다`() {
        assertTrue(FusionTable.ALL.any { it.result == WeaponFamily.VOID })
        assertEquals(
            WeaponFamily.VOID,
            FusionTable.resultFor(f(WeaponFamily.AXE, WeaponFamily.SPEAR)),
        )
    }

    @Test
    fun `조합 전용 계열 열 종이 전부 얻을 길이 있다`() {
        val fromTable = FusionTable.ALL.map { it.result }.toSet()
        val basics = WeaponFamily.BASICS.toSet()
        for (family in WeaponFamily.entries) {
            if (family in basics) continue
            val reachable = when (family) {
                // 합검은 표가 아니라 "서로 다른 4계열" 규칙으로, 허검은 회랑 보상으로 얻는다
                WeaponFamily.FUSED -> FusionTable.resultFor(
                    setOf(
                        WeaponFamily.STRAIGHT,
                        WeaponFamily.CURVED,
                        WeaponFamily.GREAT,
                        WeaponFamily.RAPIER,
                    ),
                ) == WeaponFamily.FUSED

                WeaponFamily.VOID -> true
                else -> family in fromTable
            }
            assertTrue("$family 를 얻을 길이 없다", reachable)
        }
    }

    @Test
    fun `표의 재료는 모두 서로 다른 집합이다`() {
        val keys = FusionTable.ALL.map { it.materials }
        assertEquals(keys.size, keys.toSet().size)
    }
}
