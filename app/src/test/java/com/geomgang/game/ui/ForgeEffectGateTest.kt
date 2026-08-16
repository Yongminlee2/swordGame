package com.geomgang.game.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ForgeEffectGateTest {

    @Test
    fun `상점에서 돌아오면 이미 끝난 강화 연출을 다시 틀지 않는다`() {
        val beforeShop = ForgeEffectGate(initialSequence = 7)
        assertFalse(beforeShop.consume(7))
        assertTrue(beforeShop.consume(8))

        // 상점으로 나갔다가 새로 만들어진 강화 화면은 현재 번호를 기준으로 시작한다.
        val returnedFromShop = ForgeEffectGate(initialSequence = 8)
        assertFalse(returnedFromShop.consume(8))
        assertTrue(returnedFromShop.consume(9))
        assertFalse(returnedFromShop.consume(9))
    }
}
