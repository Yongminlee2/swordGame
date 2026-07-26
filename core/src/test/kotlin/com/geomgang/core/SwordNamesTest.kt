package com.geomgang.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SwordNamesTest {

    @Test
    fun `0부터 20까지 이름이 다 있다`() {
        assertEquals(20, SwordNames.maxNamedLevel)
        for (level in 0..20) {
            assertTrue("+$level 이름이 비었다", SwordNames.nameFor(level).isNotBlank())
        }
    }

    @Test
    fun `단계마다 이름이 다르다`() {
        // 같은 이름에 숫자만 붙는 것을 없애려고 만든 표다. 겹치면 의미가 없다.
        val names = (0..20).map { SwordNames.nameFor(it) }
        assertEquals("겹치는 이름이 있다", names.size, names.toSet().size)
    }

    @Test
    fun `인접한 단계는 반드시 다른 이름이다`() {
        for (level in 1..20) {
            assertNotEquals(
                "+$level 이 +${level - 1} 과 같은 이름이다",
                SwordNames.nameFor(level - 1),
                SwordNames.nameFor(level),
            )
        }
    }

    @Test
    fun `이름에 강화 숫자를 넣지 않는다`() {
        // 숫자는 화면이 부제로 따로 붙인다. 이름 자체에 들어가면 중복이다.
        (0..20).forEach { level ->
            val name = SwordNames.nameFor(level)
            assertTrue("$name 에 숫자가 들어 있다", name.none { it.isDigit() })
            assertTrue("$name 에 + 가 들어 있다", !name.contains('+'))
        }
    }

    @Test
    fun `무한 구간에도 이름이 있고 다섯 단계마다 바뀐다`() {
        val first = SwordNames.nameFor(21)
        assertTrue(first.isNotBlank())
        assertNotEquals(SwordNames.nameFor(20), first)
        assertEquals(first, SwordNames.nameFor(25))
        assertNotEquals(first, SwordNames.nameFor(26))
    }

    @Test
    fun `아주 높은 단계도 마지막 이름으로 답한다`() {
        val last = SwordNames.endlessNames().last()
        assertEquals(last, SwordNames.nameFor(999))
    }

    @Test
    fun `무한 구간 이름도 서로 다르다`() {
        val names = SwordNames.endlessNames().toList()
        assertEquals(names.size, names.toSet().size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `음수 단계에는 이름이 없다`() {
        SwordNames.nameFor(-1)
    }
}
