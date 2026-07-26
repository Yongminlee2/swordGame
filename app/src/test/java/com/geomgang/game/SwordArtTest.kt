package com.geomgang.game

import com.geomgang.core.WeaponFamily
import com.geomgang.game.ui.SwordArt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 검 그림 배치 검증.
 *
 * 그려진 그림이 예쁜지는 테스트할 수 없다. 하지만 **같은 단계에서 두 계열이 같은 그림이 되는 것**,
 * **인접한 단계가 통째로 같아지는 것**은 잡을 수 있다.
 * 아이콘 표를 손보다 값을 복사해 놓고 고치는 걸 잊으면 그런 일이 생긴다.
 */
class SwordArtTest {

    private val maxLevel = SwordArt.maxLevel()

    @Test
    fun `계열 8종 모두 단계별 실루엣을 갖는다`() {
        val sets = SwordArt.silhouetteSets()
        assertEquals(8, sets.size)
        WeaponFamily.entries.forEach { family ->
            val set = sets.getValue(family)
            assertEquals("${family.id} 실루엣 개수", maxLevel + 1, set.size)
            assertTrue("${family.id} 에 0 인 리소스가 있다", set.all { it != 0 })
        }
    }

    @Test
    fun `한 계열 안에서 인접한 단계는 다른 실루엣이다`() {
        // 예전 구조는 티어 하나가 2~3단계를 덮어 같은 그림이 반복됐다. 그것을 막는다.
        SwordArt.silhouetteSets().forEach { (family, set) ->
            for (level in 1..maxLevel) {
                assertTrue(
                    "${family.id} +$level 이 +${level - 1} 과 같은 실루엣이다",
                    set[level] != set[level - 1],
                )
            }
        }
    }

    @Test
    fun `같은 단계에서 두 계열이 같은 실루엣을 쓰지 않는다`() {
        for (level in 0..maxLevel) {
            val used = WeaponFamily.entries.map { SwordArt.drawableFor(it, level) }
            assertEquals("+$level 에서 계열끼리 실루엣이 겹친다", used.size, used.toSet().size)
        }
    }

    @Test
    fun `단계마다 색이 다르다`() {
        val palettes = (0..maxLevel).map { SwordArt.paletteFor(it) }
        assertEquals("같은 색을 쓰는 단계가 있다", palettes.size, palettes.toSet().size)
    }

    @Test
    fun `오라는 중간 단계부터 붙고 계속 세진다`() {
        assertEquals(0f, SwordArt.paletteFor(0).auraAlpha, 1e-6f)
        assertEquals(0f, SwordArt.paletteFor(5).auraAlpha, 1e-6f)
        assertTrue(SwordArt.paletteFor(15).auraAlpha > 0f)
        for (level in 9..maxLevel) {
            assertTrue(
                "+$level 오라가 이전 단계보다 약하다",
                SwordArt.paletteFor(level).auraAlpha >= SwordArt.paletteFor(level - 1).auraAlpha,
            )
        }
        assertTrue(SwordArt.paletteFor(maxLevel).auraAlpha > SwordArt.paletteFor(15).auraAlpha)
    }

    @Test
    fun `무한 구간도 실루엣과 색이 있다`() {
        WeaponFamily.entries.forEach { family ->
            for (level in maxLevel + 1..60) {
                assertTrue(SwordArt.drawableFor(family, level) != 0)
                assertTrue(SwordArt.paletteFor(level).auraAlpha > 0f)
            }
        }
    }

    @Test
    fun `무한 구간 오라가 유한 최고 단계보다 강하다`() {
        assertTrue(
            SwordArt.paletteFor(maxLevel + 1).auraAlpha > SwordArt.paletteFor(maxLevel).auraAlpha,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `음수 단계는 그릴 수 없다`() {
        SwordArt.drawableFor(WeaponFamily.STRAIGHT, -1)
    }
}
