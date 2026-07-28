package com.geomgang.core

import org.junit.Assert.assertEquals
import org.junit.Test

class ForgeMarksTest {

    private val state = GameState(Difficulty.ENDLESS)

    @Test
    fun `네 결과가 각각 다른 글자로 간다`() {
        assertEquals(ForgeMark.UP, ForgeMarks.of(ForgeResult.Success(state, 12)))
        assertEquals(ForgeMark.STAY, ForgeMarks.of(ForgeResult.Stay(state, 11)))
        assertEquals(ForgeMark.DOWN, ForgeMarks.of(ForgeResult.Drop(state, 10)))
        assertEquals(
            ForgeMark.BREAK,
            ForgeMarks.of(ForgeResult.Destroyed(state, lostLevel = 11, preventable = false)),
        )
    }

    @Test
    fun `방금 것이 마지막에 온다`() {
        val marks = ForgeMarks.push(listOf(ForgeMark.UP), ForgeMark.BREAK)
        assertEquals(listOf(ForgeMark.UP, ForgeMark.BREAK), marks)
    }

    @Test
    fun `빈 목록에도 넣을 수 있다`() {
        assertEquals(listOf(ForgeMark.UP), ForgeMarks.push(emptyList(), ForgeMark.UP))
    }

    @Test
    fun `보관 수를 넘으면 앞에서 버린다`() {
        var marks = emptyList<ForgeMark>()
        repeat(ForgeMarks.KEEP) { marks = ForgeMarks.push(marks, ForgeMark.STAY) }
        marks = ForgeMarks.push(marks, ForgeMark.UP)

        assertEquals(ForgeMarks.KEEP, marks.size)
        assertEquals(ForgeMark.UP, marks.last())
        assertEquals(ForgeMark.STAY, marks.first())
    }

    /** 어떤 이유로 목록이 길어져 있어도 한 번 밀면 제자리로 돌아온다. */
    @Test
    fun `이미 넘쳐 있어도 보관 수로 잘린다`() {
        val tooMany = List(ForgeMarks.KEEP + 5) { ForgeMark.DOWN }
        val marks = ForgeMarks.push(tooMany, ForgeMark.UP)
        assertEquals(ForgeMarks.KEEP, marks.size)
        assertEquals(ForgeMark.UP, marks.last())
    }
}
