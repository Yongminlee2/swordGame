package com.geomgang.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ForgeCostTest {

    private fun state(
        level: Int,
        gold: Long = 1_000_000_000,
        stones: Int = 100,
        storage: Int = 10,
    ) = GameState(
        difficulty = Difficulty.ENDLESS,
        gold = gold,
        sword = Sword(WeaponFamily.STRAIGHT, level),
        storage = List(storage) { Sword(WeaponFamily.STRAIGHT, 1) },
        forgeStones = stones,
    )

    @Test
    fun `15단계까지는 골드만 든다`() {
        for (level in 0..14) {
            val req = ForgeCost.requirementFor(level)
            assertEquals("+$level", Economy.upgradeCost(level), req.gold)
            assertEquals("+$level 강화석", 0, req.stones)
        }
    }

    @Test
    fun `16단계부터 강화석이 필수다`() {
        assertEquals(3, ForgeCost.requirementFor(15).stones)
        // v1.7에서 증가 속도를 절반으로 낮췄다 - 두 단계에 한 개씩 늘어난다.
        assertEquals(5, ForgeCost.requirementFor(19).stones)
    }

    @Test
    fun `무한 구간은 강화석이 두 단계마다 늘어난다`() {
        assertEquals(5, ForgeCost.requirementFor(20).stones)
        assertEquals(10, ForgeCost.requirementFor(29).stones)
    }

    @Test
    fun `보관함의 검은 강화에 쓰지 않는다`() {
        // v1.8: 검을 태우는 자리가 강화와 조합 둘이라 같은 보관함을 놓고 다퉜다.
        // 이제 강화는 골드와 강화석만 먹고, 검은 조합 전용이다.
        assertTrue(ForgeCost.canPay(state(20, storage = 0, stones = 99)))
    }

    @Test
    fun `강화석 요구는 상한을 넘지 않는다`() {
        assertEquals(ForgeCost.MAX_STONES, ForgeCost.requirementFor(200).stones)
    }

    @Test
    fun `요구를 다 갖추면 낼 수 있다`() {
        assertTrue(ForgeCost.canPay(state(16)))
    }

    @Test
    fun `강화석이 모자라면 못 낸다`() {
        assertFalse(ForgeCost.canPay(state(16, stones = 0)))
        assertNotNull(ForgeCost.missingText(state(16, stones = 0)))
    }

    @Test
    fun `골드가 모자라면 못 낸다`() {
        assertFalse(ForgeCost.canPay(state(5, gold = 0)))
    }

    @Test
    fun `낼 수 있으면 사유가 없다`() {
        assertNull(ForgeCost.missingText(state(16)))
    }

    @Test
    fun `검이 없으면 사유를 알려 준다`() {
        val empty = GameState(difficulty = Difficulty.ENDLESS, gold = 1000)
        assertNotNull(ForgeCost.missingText(empty))
    }
}
