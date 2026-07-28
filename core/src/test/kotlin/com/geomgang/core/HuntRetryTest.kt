package com.geomgang.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 보스를 놓쳤을 때 즉시 재도전하는 값.
 *
 * 지금까지는 놓치면 잡몹 진행이 조용히 지워져 12마리를 다시 모아야 했다.
 * 실패 한 번이 너무 무겁고, 무엇을 잃었는지 화면에 뜨지도 않았다.
 *
 * 대가를 골드로 받는 이유는 후반에 남아도는 자원이라 쓸 데가 하나 더 생기고,
 * **2배 누진**이라 무한 재도전이 스스로 막히기 때문이다.
 */
class HuntRetryTest {

    @Test
    fun `첫 재도전은 보스 보상의 절반이다`() {
        val zone = Zone.MEADOW
        assertEquals(zone.bossGold / 2, HuntRetry.priceOf(zone, retries = 0))
    }

    @Test
    fun `재도전할수록 두 배가 된다`() {
        val zone = Zone.MEADOW
        val first = HuntRetry.priceOf(zone, retries = 0)
        assertEquals(first * 2, HuntRetry.priceOf(zone, retries = 1))
        assertEquals(first * 4, HuntRetry.priceOf(zone, retries = 2))
        assertEquals(first * 8, HuntRetry.priceOf(zone, retries = 3))
    }

    @Test
    fun `깊은 구역일수록 재도전이 비싸다`() {
        assertTrue(
            HuntRetry.priceOf(Zone.ABYSS, 0) > HuntRetry.priceOf(Zone.MEADOW, 0),
        )
    }

    @Test
    fun `골드가 모자라면 재도전할 수 없다`() {
        val zone = Zone.CAVE
        val price = HuntRetry.priceOf(zone, retries = 0)
        assertTrue(HuntRetry.canRetry(gold = price, zone = zone, retries = 0))
        assertFalse(HuntRetry.canRetry(gold = price - 1, zone = zone, retries = 0))
    }

    @Test
    fun `누진이 발산해도 넘치지 않는다`() {
        // 2배씩 오르므로 예순 번쯤이면 Long 을 넘긴다. 상한에서 멈춰야 한다.
        val price = HuntRetry.priceOf(Zone.entries.last(), retries = 200)
        assertTrue("가격이 음수가 됐다: $price", price > 0)
        assertEquals(HuntRetry.MAX_PRICE, price)
    }
}
