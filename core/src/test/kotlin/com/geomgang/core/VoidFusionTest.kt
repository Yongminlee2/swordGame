package com.geomgang.core

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 허검을 회랑 10층에서 내렸다.
 *
 * 강화 게임인데 회랑 진도에 강제로 묶이는 것이 전설검 재료로는 너무 높은 문턱이었다.
 * 회랑 보상은 남아 있어 길이 둘이 된다.
 */
class VoidFusionTest {

    @Test
    fun `도끼검과 창검을 합치면 허검이 된다`() {
        assertEquals(
            WeaponFamily.VOID,
            FusionTable.resultFor(setOf(WeaponFamily.AXE, WeaponFamily.SPEAR)),
        )
    }

    /** 조합표의 결과가 겹치면 어느 쪽이 나올지 우연에 맡겨진다. */
    @Test
    fun `조합 결과가 겹치지 않는다`() {
        val results = FusionTable.ALL.map { it.result }
        assertEquals(results.size, results.toSet().size)
    }

    /** 재료 집합도 겹치면 안 된다. 같은 재료가 두 결과를 가리킬 수 없다. */
    @Test
    fun `재료 조합이 겹치지 않는다`() {
        val materials = FusionTable.ALL.map { it.materials }
        assertEquals(materials.size, materials.toSet().size)
    }
}
