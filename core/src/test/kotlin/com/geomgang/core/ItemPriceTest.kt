package com.geomgang.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 방지권·축복서·행운부적의 고정가 규칙. */
class ItemPriceTest {

    private fun state(bestLevel: Int, itemsBought: Int = 0) = GameState(
        difficulty = Difficulty.ENDLESS,
        gold = 1_000_000_000_000_000L,
        bestLevel = bestLevel,
        itemsBought = itemsBought,
    )

    @Test
    fun `모든 시즌에서 처음 정한 소모품 가격을 쓴다`() {
        for (level in listOf(0, 20, 21, 30, 50)) {
            for (item in Item.entries) {
                assertEquals(
                    "level=$level item=$item",
                    Economy.priceOf(item),
                    GoldShop.itemPrice(state(level), item),
                )
            }
        }
    }

    @Test
    fun `예전 누진 카운터가 세이브에 남아 있어도 가격은 안 오른다`() {
        for (item in Item.entries) {
            assertEquals(
                Economy.priceOf(item),
                GoldShop.itemPrice(state(50, itemsBought = 999), item),
            )
        }
    }

    @Test
    fun `소모품을 사도 누진 카운터를 늘리지 않는다`() {
        val before = state(30, itemsBought = 7)
        val after = GoldShop.buyItem(before, Item.PREVENT_TICKET)
        assertEquals(7, after.itemsBought)
        assertEquals(
            Economy.priceOf(Item.BLESSING_SCROLL),
            GoldShop.itemPrice(after, Item.BLESSING_SCROLL),
        )
    }

    @Test
    fun `소모품 상대 가격 순서는 유지된다`() {
        val s = state(30)
        assertTrue(
            GoldShop.itemPrice(s, Item.PREVENT_TICKET) <
                GoldShop.itemPrice(s, Item.BLESSING_SCROLL),
        )
        assertTrue(
            GoldShop.itemPrice(s, Item.BLESSING_SCROLL) <
                GoldShop.itemPrice(s, Item.LUCK_CHARM),
        )
    }

    @Test
    fun `조각 교환값도 소모품 구매 횟수의 영향을 받지 않는다`() {
        val rich = state(44, itemsBought = 20)
        val prevent = Recipes.byId("prevent")
        assertTrue(Recipes.canCraft(rich.copy(shards = prevent.shardCost), prevent))
    }
}
