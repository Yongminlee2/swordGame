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
 * 강화석과 소모품은 상점에 표시된 고정가를 지키고, 재료 검만
 * 후반 골드 소모처 역할을 하도록 누진 곡선을 쓴다.
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
    fun `강화석은 연속으로 사도 고정가다`() {
        val s = state(best = 20)
        val first = GoldShop.stonePrice(s)
        val second = GoldShop.stonePrice(s.copy(stonesBought = 1))
        val fifth = GoldShop.stonePrice(s.copy(stonesBought = 4))

        assertEquals(GoldShop.STONE_PRICE, first)
        assertEquals(first, second)
        assertEquals(first, fifth)
    }

    @Test
    fun `최고 단계가 올라도 강화석 값은 같다`() {
        val spent = state(best = 20).copy(stonesBought = 10)
        val before = GoldShop.stonePrice(spent)

        val leveled = GoldShop.rebase(spent.copy(bestLevel = 21))
        assertEquals(before, GoldShop.stonePrice(leveled))
    }

    @Test
    fun `단계가 그대로면 리셋하지 않는다`() {
        val s = state(best = 20).copy(stonesBought = 7)
        assertEquals(7, GoldShop.rebase(s).stonesBought)
    }

    @Test
    fun `깊이 가도 강화석은 고정가다`() {
        assertEquals(GoldShop.stonePrice(state(best = 20)), GoldShop.stonePrice(state(best = 50)))
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
        assertEquals(before.stonesBought, after.stonesBought)
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
    fun `소모품은 연속으로 사도 가격이 그대로다`() {
        for (item in Item.entries) {
            val first = state(best = 50)
            val boughtMany = first.copy(itemsBought = 999)
            assertEquals(Economy.priceOf(item), GoldShop.itemPrice(first, item))
            assertEquals(Economy.priceOf(item), GoldShop.itemPrice(boughtMany, item))
            assertEquals(999, GoldShop.buyItem(boughtMany, item).itemsBought)
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
