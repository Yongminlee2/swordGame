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
    fun `12단계까지는 골드만 든다`() {
        for (level in 0..11) {
            val req = ForgeCost.requirementFor(level)
            assertEquals("+$level", Economy.upgradeCost(level), req.gold)
            assertEquals("+$level 검", 0, req.swords)
            assertEquals("+$level 강화석", 0, req.stones)
        }
    }

    @Test
    fun `13단계부터 검 한 자루가 필수다`() {
        val req = ForgeCost.requirementFor(12)
        assertEquals(1, req.swords)
        assertEquals(0, req.stones)
    }

    @Test
    fun `16단계부터 검 두 자루와 강화석이 필수다`() {
        val req16 = ForgeCost.requirementFor(15)
        assertEquals(2, req16.swords)
        assertEquals(3, req16.stones)
        // v1.7에서 증가 속도를 절반으로 낮췄다 - 두 단계에 한 개씩 늘어난다.
        val req20 = ForgeCost.requirementFor(19)
        assertEquals(2, req20.swords)
        assertEquals(5, req20.stones)
    }

    @Test
    fun `무한 구간은 검 세 자루와 강화석이 두 단계마다 늘어난다`() {
        val req21 = ForgeCost.requirementFor(20)
        assertEquals(3, req21.swords)
        assertEquals(5, req21.stones)
        val req30 = ForgeCost.requirementFor(29)
        assertEquals(3, req30.swords)
        assertEquals(10, req30.stones)
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
    fun `재료 검이 모자라면 못 낸다`() {
        assertFalse(ForgeCost.canPay(state(16, storage = 1)))
    }

    @Test
    fun `골드가 모자라면 못 낸다`() {
        assertFalse(ForgeCost.canPay(state(5, gold = 0)))
    }

    @Test
    fun `추가 재료를 요구하면 그만큼 더 필요하다`() {
        assertTrue(ForgeCost.canPay(state(16, storage = 5), extraSwords = 3))
        assertFalse(ForgeCost.canPay(state(16, storage = 4), extraSwords = 3))
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
