package com.geomgang.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 전설검을 잃는 길을 막는다.
 *
 * 전설검은 재료 넷을 각각 +20 까지 올려야 나온다 — 몇 시간짜리다. **한 번의 오조작으로
 * 사라지는 자리가 하나라도 있으면 안 된다.** 고유검에 이미 같은 이유의 방어가 있다.
 */
class LegendSafetyTest {

    private fun stateWith(vararg swords: Sword) = GameState(
        difficulty = Difficulty.ENDLESS,
        gold = 1_000_000_000_000_000L,
        storage = swords.toList(),
    )

    private val legend = Sword(WeaponFamily.DRAGON, LegendForge.LEVEL + 10)

    /**
     * 조합 결과는 [RateTable.MAX_FINITE_LEVEL] 로 깎인다. 전설검을 녹이면
     * **반드시 손해**고 되돌릴 방법이 없다.
     */
    @Test
    fun `전설검은 조합 재료로 녹일 수 없다`() {
        val state = stateWith(legend, Sword(WeaponFamily.STRAIGHT, 5))
        assertFalse(Fusion.canFuse(state, listOf(0, 1)))
        assertFalse(Fusion.meltable(legend))
    }

    @Test
    fun `고유검도 여전히 녹일 수 없다`() {
        assertFalse(Fusion.meltable(Sword(WeaponFamily.DRAGON, 15, uniqueId = "phoenix")))
    }

    @Test
    fun `평범한 검은 녹인다`() {
        // v2.3부터 조합은 고유검 레시피만 남았다 - 시작의 검(직검 +10 둘)으로 확인한다.
        val state = stateWith(Sword(WeaponFamily.STRAIGHT, 10), Sword(WeaponFamily.STRAIGHT, 12))
        assertTrue(Fusion.meltable(state.storage[0]))
        assertTrue(Fusion.canFuse(state, listOf(0, 1)))
    }

    /** 부수면 조각 92개, 다시 벼리는 데 500개. 그건 선택지가 아니라 함정이다. */
    @Test
    fun `전설검은 부술 수 없다`() {
        assertFalse(Storage.canScrap(legend))
        assertTrue(Storage.scrapShards(legend) < LegendForge.RECRAFT_SHARDS)
    }

    @Test
    fun `평범한 검은 부술 수 있다`() {
        assertTrue(Storage.canScrap(Sword(WeaponFamily.STRAIGHT, 20)))
    }

    /**
     * 값 누진의 상한.
     *
     * [GoldShop.rebase] 는 최고 단계가 올라야 누진을 푸는데, 계열이 +20 에서 끝나면서
     * 최고 단계가 그 자리에 오래 머무는 구간이 생겼다. 그동안 값이 끝없이 오르면
     * 골드가 다시 쓸 데를 잃는다.
     */
    @Test
    fun `강화석 값은 끝없이 오르지 않는다`() {
        val base = GameState(Difficulty.ENDLESS, bestLevel = 20)
        val capped = GoldShop.stonePrice(base.copy(stonesBought = GoldShop.GROWTH_CAP))
        val beyond = GoldShop.stonePrice(base.copy(stonesBought = GoldShop.GROWTH_CAP + 200))
        assertEquals(capped, beyond)
    }

    @Test
    fun `상한 전까지는 값이 오른다`() {
        val base = GameState(Difficulty.ENDLESS, bestLevel = 20)
        val one = GoldShop.stonePrice(base.copy(stonesBought = 1))
        val ten = GoldShop.stonePrice(base.copy(stonesBought = 10))
        assertTrue("1개=$one 10개=$ten", ten > one)
    }

    /** 재료 검도 같은 상한을 쓴다. 둘 중 하나만 막으면 상대 순서가 뒤집힌다. */
    @Test
    fun `재료 검 값도 상한을 쓴다`() {
        val base = GameState(Difficulty.ENDLESS, bestLevel = 20)
        assertEquals(
            GoldShop.materialSwordPrice(base.copy(swordsBought = GoldShop.GROWTH_CAP)),
            GoldShop.materialSwordPrice(base.copy(swordsBought = GoldShop.GROWTH_CAP + 200)),
        )
    }

    /**
     * 낫검의 짝 열기는 계열 칸 안에서만이다.
     *
     * +20 낫검의 "다음" 은 전설 칸인데, 전설 칸은 전설검으로만 열려야 한다.
     */
    @Test
    fun `20강의 다음 칸은 계열 칸이 아니다`() {
        assertTrue(WeaponCatalog.isFamilyArt(RateTable.MAX_FINITE_LEVEL))
        assertFalse(WeaponCatalog.isFamilyArt(RateTable.MAX_FINITE_LEVEL + 1))
        assertEquals(
            CodexEntry(null, LegendForge.LEVEL),
            WeaponCatalog.slotFor(WeaponFamily.SCYTHE, LegendForge.LEVEL),
        )
    }
}
