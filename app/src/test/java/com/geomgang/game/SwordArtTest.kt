package com.geomgang.game

import com.geomgang.core.WeaponCatalog
import com.geomgang.core.WeaponFamily
import com.geomgang.core.WeaponTier
import com.geomgang.game.ui.bladeSpecOf
import com.geomgang.game.ui.tierStyleOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 88조합이 서로 구분되는지를 파라미터 수준에서 확인한다.
 *
 * 그려진 그림이 예쁜지는 테스트할 수 없지만, 두 조합이 **똑같은 값으로 그려지는** 일은
 * 여기서 잡을 수 있다. 계열이나 티어를 추가하다 값을 복사해 놓고 고치는 걸 잊으면
 * 도감에 똑같이 생긴 칸이 두 개 생긴다.
 */
class SwordArtTest {

    @Test
    fun `계열 8종은 서로 다른 형태를 갖는다`() {
        val specs = WeaponFamily.entries.map { bladeSpecOf(it) }
        assertEquals(8, specs.size)
        assertEquals("같은 형태를 쓰는 계열이 있다", specs.size, specs.toSet().size)
    }

    @Test
    fun `티어 11종은 서로 다른 색을 갖는다`() {
        val styles = WeaponTier.entries.map { tierStyleOf(it) }
        assertEquals(11, styles.size)
        assertEquals("같은 색을 쓰는 티어가 있다", styles.size, styles.toSet().size)
    }

    @Test
    fun `88조합이 모두 서로 다르게 그려진다`() {
        val combos = WeaponFamily.entries.flatMap { family ->
            WeaponTier.entries.map { tier -> bladeSpecOf(family) to tierStyleOf(tier) }
        }
        assertEquals(88, combos.size)
        assertEquals("똑같이 그려지는 조합이 있다", 88, combos.toSet().size)
    }

    @Test
    fun `오라는 15단계부터 붙는다`() {
        // 뇌전검(+15~16)이 오라가 붙는 첫 티어다
        assertTrue(tierStyleOf(WeaponTier.RUNE).auraAlpha == 0f)
        assertTrue(tierStyleOf(WeaponTier.FLAME).auraAlpha == 0f)
        assertTrue(tierStyleOf(WeaponTier.THUNDER).auraAlpha > 0f)
        assertEquals(
            WeaponCatalog.AURA_MIN_LEVEL,
            WeaponTier.THUNDER.minLevel,
        )
    }

    @Test
    fun `단계가 오를수록 오라가 세진다`() {
        val withAura = WeaponTier.entries.filter { tierStyleOf(it).auraAlpha > 0f }
        for (i in 1 until withAura.size) {
            val prev = tierStyleOf(withAura[i - 1]).auraAlpha
            val cur = tierStyleOf(withAura[i]).auraAlpha
            assertTrue("${withAura[i].id}: $prev -> $cur", cur > prev)
        }
    }

    @Test
    fun `0부터 40단계까지 모든 계열이 그릴 파라미터를 갖는다`() {
        for (family in WeaponFamily.entries) {
            for (level in 0..40) {
                val tier = WeaponCatalog.tierFor(level)
                bladeSpecOf(family)
                tierStyleOf(tier)
            }
        }
    }

    @Test
    fun `날 폭과 길이가 그릴 수 있는 범위 안에 있다`() {
        WeaponFamily.entries.forEach { family ->
            val spec = bladeSpecOf(family)
            assertTrue("${family.id} 폭=${spec.widthRatio}", spec.widthRatio in 0.02f..0.35f)
            assertTrue("${family.id} 길이=${spec.lengthRatio}", spec.lengthRatio in 0.3f..0.75f)
        }
    }
}
