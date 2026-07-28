package com.geomgang.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 골드가 쓸 데를 갖는가.
 *
 * 수입은 초원 잡몹 14골드에서 끝의 문 1.4억골드까지 천만 배가 되는데 상점 값은
 * 처음 그대로였다. 그래서 후반에 골드는 쌓이기만 했다.
 *
 * 두 가지를 동시에 지켜야 한다.
 * - **몰아 사기 금지**: 살수록 비싸져야 사냥이 계속 쓸모 있다
 * - **영원히 비싸지지 않기**: 한 단계 올리면 값이 되돌아와야 골드가 다시 쓸 데를 얻는다
 */
class GoldShopTest {

    private fun state(best: Int, gold: Long = 1_000_000_000_000) = GameState(
        difficulty = Difficulty.ENDLESS,
        gold = gold,
        sword = Sword(WeaponFamily.STRAIGHT, best),
        bestLevel = best,
        priceBandLevel = best,
    )

    @Test
    fun `살수록 비싸진다`() {
        val s = state(best = 20)
        val first = GoldShop.stonePrice(s)
        val second = GoldShop.stonePrice(s.copy(stonesBought = 1))
        val fifth = GoldShop.stonePrice(s.copy(stonesBought = 4))

        assertTrue("$first -> $second", second > first)
        assertTrue("$second -> $fifth", fifth > second)
    }

    @Test
    fun `한 단계 올리면 값이 되돌아온다`() {
        // 이 게임의 골드 싱크가 막다른 길이 되지 않게 하는 장치다.
        val spent = state(best = 20).copy(stonesBought = 10)
        val expensive = GoldShop.stonePrice(spent)

        val leveled = GoldShop.rebase(spent.copy(bestLevel = 21))
        assertEquals("누진이 풀려야 한다", 0, leveled.stonesBought)
        assertTrue("$expensive -> ${GoldShop.stonePrice(leveled)}", GoldShop.stonePrice(leveled) < expensive)
    }

    @Test
    fun `단계가 그대로면 리셋하지 않는다`() {
        val s = state(best = 20).copy(stonesBought = 7)
        assertEquals(7, GoldShop.rebase(s).stonesBought)
    }

    @Test
    fun `깊이 갈수록 기준가가 오른다`() {
        assertTrue(GoldShop.stonePrice(state(best = 26)) > GoldShop.stonePrice(state(best = 20)))
    }

    @Test
    fun `첫 강화석은 그 구역 잡몹 몇 마리 값이다`() {
        // 사는 편이 사냥보다 크게 손해면 결국 사냥을 강요하는 설계가 된다.
        val zone = Zone.ENDLESS_HALL // 권장 +20
        val price = GoldShop.stonePrice(state(best = zone.recommendedLevel))
        val mob = zone.goldOf(zone.monsters.first())
        assertTrue("첫 개가 잡몹 ${price / mob}마리 값", price / mob <= 5)
    }

    @Test
    fun `강화석을 사면 개수가 늘고 골드가 준다`() {
        val before = state(best = 20)
        val price = GoldShop.stonePrice(before)
        val after = GoldShop.buyStone(before)

        assertEquals(before.forgeStones + 1, after.forgeStones)
        assertEquals(before.gold - price, after.gold)
        assertEquals(1, after.stonesBought)
    }

    @Test
    fun `골드가 모자라면 못 산다`() {
        val broke = state(best = 20, gold = 0)
        assertFalse(GoldShop.canBuyStone(broke))
    }

    @Test
    fun `소모품 값은 단계에 연동하지 않는다`() {
        // 연동을 시도했다가 BalanceSimulationTest 가 거부했다 - 아이템을 못 사면
        // 파괴가 자금을 통째로 지우고 고단계 도전 밑천이 안 생긴다.
        // 골드 싱크는 진행을 막지 않는 곳(재료 구매)에만 둔다.
        for (best in listOf(0, 10, 20, 30)) {
            assertEquals(
                "best=$best",
                Economy.PREVENT_TICKET_PRICE,
                Economy.priceOf(Item.PREVENT_TICKET),
            )
        }
    }

    @Test
    fun `재료 검도 같은 곡선을 탄다`() {
        val s = state(best = 20)
        assertTrue(GoldShop.materialSwordPrice(s.copy(swordsBought = 3)) > GoldShop.materialSwordPrice(s))
        assertTrue(
            "재료 검이 강화석보다 싸야 한다",
            GoldShop.materialSwordPrice(s) < GoldShop.stonePrice(s),
        )
    }
}
