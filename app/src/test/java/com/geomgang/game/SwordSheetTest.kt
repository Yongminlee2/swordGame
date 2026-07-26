package com.geomgang.game

import com.geomgang.core.WeaponFamily
import com.geomgang.game.ui.SwordSheet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 스프라이트 좌표 검증.
 *
 * 그림이 예쁜지는 테스트할 수 없다. 하지만 **시트 밖을 가리키는 좌표**나
 * **같은 단계에서 계열끼리 같은 칸을 쓰는 것**은 잡을 수 있다.
 */
class SwordSheetTest {

    private val maxLevel = 20

    @Test
    fun `좌표가 시트 안에 있다`() {
        WeaponFamily.entries.forEach { family ->
            for (level in 0..60) {
                val o = SwordSheet.spriteOffset(family, level)
                assertTrue("${family.id} +$level x=${o.x}", o.x in 0 until SwordSheet.columns() * SwordSheet.CELL)
                assertTrue("${family.id} +$level y=${o.y}", o.y in 0 until SwordSheet.rows() * SwordSheet.CELL)
                assertEquals("x 가 격자에 안 맞는다", 0, o.x % SwordSheet.CELL)
                assertEquals("y 가 격자에 안 맞는다", 0, o.y % SwordSheet.CELL)
            }
        }
    }

    @Test
    fun `저단계는 낡은 검을 쓴다`() {
        // "녹슨 쇠칼"이 반짝이면 이름과 어긋난다
        WeaponFamily.entries.forEach { family ->
            val worn = SwordSheet.spriteOffset(family, 0).x
            val used = SwordSheet.spriteOffset(family, 1).x
            val fresh = SwordSheet.spriteOffset(family, 5).x
            assertTrue("+0 이 가장 낡은 열이어야 한다", worn > used)
            assertTrue("+1 이 +5 보다 낡아야 한다", used > fresh)
            assertEquals("+5 는 새것 열이어야 한다", 0, fresh)
        }
    }

    @Test
    fun `단계가 오르면 검 종류가 바뀐다`() {
        WeaponFamily.entries.forEach { family ->
            val rows = (0..maxLevel).map { SwordSheet.spriteOffset(family, it).y }.distinct()
            assertTrue("${family.id} 는 종류가 ${rows.size} 개뿐이다", rows.size >= 8)
        }
    }

    @Test
    fun `같은 단계에서 계열끼리 같은 칸을 쓰지 않는다`() {
        for (level in 0..maxLevel) {
            val used = WeaponFamily.entries.map { SwordSheet.spriteOffset(it, level) }
            assertEquals("+$level 에서 계열끼리 겹친다", used.size, used.toSet().size)
        }
    }

    @Test
    fun `계열 12종이 모두 시트 안의 행을 쓴다`() {
        assertEquals(12, WeaponFamily.entries.size)
        WeaponFamily.entries.forEach { family ->
            for (index in 0..SwordSheet.maxIndex()) {
                val row = SwordSheet.rowOf(family, index)
                assertTrue("${family.id} 인덱스 $index 행=$row", row in 0 until SwordSheet.rows())
            }
        }
    }

    @Test
    fun `같은 인덱스에서 열두 계열이 서로 다른 행을 쓴다`() {
        // 계열 수(12)가 시트 행 수(30)보다 작아야 성립하는 성질이다.
        for (index in 0..SwordSheet.maxIndex()) {
            val rows = WeaponFamily.entries.map { SwordSheet.rowOf(it, index) }
            assertEquals("인덱스 $index 에서 행이 겹친다", rows.size, rows.toSet().size)
        }
    }

    @Test
    fun `오라는 중간 단계부터 붙고 계속 세진다`() {
        assertEquals(0f, SwordSheet.auraFor(0).alpha, 1e-6f)
        assertEquals(0f, SwordSheet.auraFor(8).alpha, 1e-6f)
        assertTrue(SwordSheet.auraFor(12).alpha > 0f)
        for (level in 10..40) {
            assertTrue(
                "+$level 오라가 이전보다 약하다",
                SwordSheet.auraFor(level).alpha >= SwordSheet.auraFor(level - 1).alpha,
            )
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `음수 단계는 그릴 수 없다`() {
        SwordSheet.spriteOffset(WeaponFamily.STRAIGHT, -1)
    }
}
