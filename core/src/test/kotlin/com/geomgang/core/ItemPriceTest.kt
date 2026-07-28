package com.geomgang.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 소모품 값.
 *
 * 유한 구간은 고정가 그대로여야 한다 — 거기가 밸런스 시뮬레이션이 도는 구간이고,
 * 그 모형에는 사냥이 없어서 진행 연동 가격을 무엇이든 거부한다.
 */
class ItemPriceTest {

    private fun state(bestLevel: Int, itemsBought: Int = 0) = GameState(
        difficulty = Difficulty.ENDLESS,
        gold = 1_000_000_000_000_000L,
        bestLevel = bestLevel,
        itemsBought = itemsBought,
    )

    @Test
    fun `유한 구간에서는 고정가 그대로다`() {
        for (level in 0 until GoldShop.ITEM_BAND_LEVEL) {
            for (item in Item.entries) {
                assertEquals(
                    "level=$level item=$item",
                    Economy.priceOf(item),
                    GoldShop.itemPrice(state(level), item),
                )
            }
        }
    }

    /** 유한 구간에서는 몇 개를 사도 값이 그대로다. 초반 진행을 막지 않는다. */
    @Test
    fun `유한 구간은 누진도 붙지 않는다`() {
        assertEquals(
            Economy.priceOf(Item.PREVENT_TICKET),
            GoldShop.itemPrice(state(20, itemsBought = 30), Item.PREVENT_TICKET),
        )
    }

    @Test
    fun `무한 구간부터 강화 비용을 따라 오른다`() {
        val at21 = GoldShop.itemPrice(state(21), Item.PREVENT_TICKET)
        val at30 = GoldShop.itemPrice(state(30), Item.PREVENT_TICKET)
        assertTrue("21=$at21 30=$at30", at30 > at21)
        assertTrue("21=$at21 고정가=${Economy.priceOf(Item.PREVENT_TICKET)}", at21 > Economy.priceOf(Item.PREVENT_TICKET))
    }

    @Test
    fun `살수록 값이 오른다`() {
        val first = GoldShop.itemPrice(state(30, itemsBought = 0), Item.PREVENT_TICKET)
        val sixth = GoldShop.itemPrice(state(30, itemsBought = 5), Item.PREVENT_TICKET)
        assertTrue("first=$first sixth=$sixth", sixth > first)
    }

    /** 세 소모품이 카운터를 함께 쓴다 — 무엇을 쟁일지가 선택이 되어야 한다. */
    @Test
    fun `방지권을 사면 축복서도 비싸진다`() {
        val before = GoldShop.itemPrice(state(30), Item.BLESSING_SCROLL)
        val after = GoldShop.buyItem(state(30), Item.PREVENT_TICKET)
        assertTrue(GoldShop.itemPrice(after, Item.BLESSING_SCROLL) > before)
    }

    @Test
    fun `상대 순서는 고정가와 같다`() {
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

    /** 한 단계 올리면 누진이 풀린다. 골드가 늘 "다음 한 단계"에 묶여 있어야 한다. */
    @Test
    fun `단계가 오르면 누진이 풀린다`() {
        val stocked = state(30, itemsBought = 8).copy(priceBandLevel = 30)
        val levelled = GoldShop.rebase(stocked.copy(bestLevel = 31))
        assertEquals(0, levelled.itemsBought)
    }

    /** 조각 교환은 값이 그대로다 — 골드로 못 사도 진행이 막히지 않는 안전 밸브다. */
    @Test
    fun `조각 교환값은 소모품 누진을 타지 않는다`() {
        val rich = state(44, itemsBought = 20)
        val prevent = Recipes.byId("prevent")
        assertTrue(Recipes.canCraft(rich.copy(shards = prevent.shardCost), prevent))
    }
}
