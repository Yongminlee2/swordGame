package com.geomgang.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TemperingTest {

    @Test
    fun `유한 구간에는 붙지 않고 무한 구간부터 붙는다`() {
        assertFalse(Tempering.applies(20))
        assertTrue(Tempering.applies(21))
        assertTrue(Tempering.applies(45))
    }

    @Test
    fun `실패가 없으면 기준값 그대로다`() {
        assertEquals(0.005, Tempering.rateFor(0.005, 45, 0), 1e-9)
    }

    /** 붙지 않는 구간에서는 실패가 쌓여 있어도 무시한다. */
    @Test
    fun `유한 구간은 실패가 쌓여도 오르지 않는다`() {
        assertEquals(0.40, Tempering.rateFor(0.40, 10, 50), 1e-9)
    }

    @Test
    fun `실패 두 번이면 기준의 두 배가 된다`() {
        // base + base * 0.5 * 2 = base * 2
        assertEquals(0.010, Tempering.rateFor(0.005, 45, 2), 1e-9)
    }

    @Test
    fun `실패 수에 대해 단조 증가한다`() {
        var previous = Tempering.rateFor(0.005, 45, 0)
        for (fails in 1..200) {
            val now = Tempering.rateFor(0.005, 45, fails)
            assertTrue("fails=$fails prev=$previous now=$now", now >= previous)
            previous = now
        }
    }

    @Test
    fun `상한을 넘지 않는다`() {
        assertEquals(Tempering.MAX_RATE, Tempering.rateFor(0.005, 45, 100_000), 1e-9)
    }

    /** 담금질은 올려 주기만 한다. 이미 상한보다 높은 기준값을 끌어내리면 안 된다. */
    @Test
    fun `상한보다 높은 기준값은 낮추지 않는다`() {
        assertEquals(0.80, Tempering.rateFor(0.80, 45, 0), 1e-9)
        assertEquals(0.80, Tempering.rateFor(0.80, 45, 10), 1e-9)
    }

    @Test
    fun `음수 실패 수는 0으로 다룬다`() {
        assertEquals(0.005, Tempering.rateFor(0.005, 45, -3), 1e-9)
    }

    @Test
    fun `같은 단계에 쌓인 실패만 세어 준다`() {
        val state = GameState(Difficulty.ENDLESS, temperLevel = 45, temperFails = 7)
        assertEquals(7, Tempering.failsFor(state, 45))
        assertEquals(0, Tempering.failsFor(state, 44))
        assertEquals(0, Tempering.failsFor(GameState(Difficulty.ENDLESS), 45))
    }
}
