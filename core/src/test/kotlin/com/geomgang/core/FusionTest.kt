package com.geomgang.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class FusionTest {

    private fun state(
        storage: List<Sword> = emptyList(),
        gold: Long = 10_000_000,
        shards: Int = 10_000,
        sword: Sword? = null,
    ) = GameState(
        difficulty = Difficulty.ENDLESS,
        gold = gold,
        shards = shards,
        sword = sword,
        storage = storage,
    )

    private fun sw(level: Int, family: WeaponFamily = WeaponFamily.STRAIGHT, stars: Int = 0) =
        Sword(family, level, stars)

    // ---------------- 조합 ----------------

    @Test
    fun `재료 두 자루면 최고 단계보다 한 단계 위가 나온다`() {
        val result = Fusion.resultOf(listOf(sw(5, WeaponFamily.CURVED), sw(3, WeaponFamily.GREAT)))
        assertEquals(6, result.level)
    }

    @Test
    fun `재료가 많을수록 결과가 좋아진다`() {
        val two = Fusion.resultOf(listOf(sw(8, WeaponFamily.CURVED), sw(2, WeaponFamily.GREAT)))
        val four = Fusion.resultOf(
            listOf(
                sw(8, WeaponFamily.CURVED),
                sw(2, WeaponFamily.GREAT),
                sw(1, WeaponFamily.HOLY),
                sw(1, WeaponFamily.AXE),
            ),
        )
        assertTrue("${two.level} -> ${four.level}", four.level > two.level)
    }

    @Test
    fun `계열을 맞추면 보너스가 붙는다`() {
        val mixed = Fusion.resultOf(listOf(sw(8, WeaponFamily.CURVED), sw(2, WeaponFamily.GREAT)))
        val same = Fusion.resultOf(listOf(sw(8, WeaponFamily.CURVED), sw(2, WeaponFamily.CURVED)))
        assertEquals(mixed.level + Fusion.SAME_FAMILY_BONUS, same.level)
    }

    @Test
    fun `결과 계열은 가장 많이 넣은 계열이다`() {
        val result = Fusion.resultOf(
            listOf(
                sw(9, WeaponFamily.STRAIGHT),
                sw(2, WeaponFamily.DRAGON),
                sw(1, WeaponFamily.DRAGON),
            ),
        )
        assertEquals(WeaponFamily.DRAGON, result.family)
    }

    @Test
    fun `동수면 최고 단계 검의 계열을 따른다`() {
        val result = Fusion.resultOf(
            listOf(sw(12, WeaponFamily.HOLY), sw(3, WeaponFamily.AXE)),
        )
        assertEquals(WeaponFamily.HOLY, result.family)
    }

    @Test
    fun `조합 결과는 유한 상한을 넘지 않는다`() {
        val result = Fusion.resultOf(
            List(4) { sw(RateTable.MAX_FINITE_LEVEL, WeaponFamily.CURVED) },
        )
        assertEquals(RateTable.MAX_FINITE_LEVEL, result.level)
    }

    @Test
    fun `조합 결과에는 별이 이어지지 않는다`() {
        // 녹여서 새로 만드는 것이므로 0부터다
        val result = Fusion.resultOf(listOf(sw(12, stars = 4), sw(11, stars = 3)))
        assertEquals(0, result.stars)
    }

    @Test
    fun `재료가 한 자루면 조합할 수 없다`() {
        val s = state(storage = listOf(sw(5)))
        assertFalse(Fusion.canFuse(s, listOf(0)))
    }

    @Test
    fun `같은 자리를 두 번 넣을 수 없다`() {
        val s = state(storage = listOf(sw(5), sw(6)))
        assertFalse(Fusion.canFuse(s, listOf(0, 0)))
    }

    @Test
    fun `골드가 모자라면 조합할 수 없다`() {
        val s = state(storage = listOf(sw(15), sw(15)), gold = 1)
        assertFalse(Fusion.canFuse(s, listOf(0, 1)))
    }

    @Test
    fun `조합하면 재료가 사라지고 결과가 보관함에 들어온다`() {
        val s = state(storage = listOf(sw(5, WeaponFamily.CURVED), sw(4, WeaponFamily.CURVED)))
        val after = Fusion.fuse(s, listOf(0, 1))
        assertEquals(1, after.storage.size)
        // 곡도 둘은 조합표에 따라 낫검이 된다 (v1.4)
        assertEquals(WeaponFamily.SCYTHE, after.storage.first().family)
        assertEquals(7, after.storage.first().level) // 5 + 1(자루수) + 1(계열)
    }

    @Test
    fun `조합은 골드를 쓴다`() {
        val s = state(storage = listOf(sw(10), sw(9)), gold = 1_000_000)
        val cost = Fusion.cost(s, listOf(0, 1))
        assertTrue(cost > 0)
        assertEquals(1_000_000 - cost, Fusion.fuse(s, listOf(0, 1)).gold)
    }

    @Test
    fun `조합이 최고 기록을 갱신한다`() {
        val s = state(storage = listOf(sw(12, WeaponFamily.CURVED), sw(12, WeaponFamily.CURVED)))
        assertEquals(14, Fusion.fuse(s, listOf(0, 1)).bestLevel)
    }

    @Test
    fun `미리보기가 실제 결과와 같다`() {
        val s = state(storage = listOf(sw(7, WeaponFamily.HOLY), sw(6, WeaponFamily.HOLY)))
        val preview = Fusion.preview(s, listOf(0, 1))
        val actual = Fusion.fuse(s, listOf(0, 1)).storage.last()
        assertEquals(preview, actual)
    }

    @Test
    fun `재료가 부족하면 미리보기가 없다`() {
        assertNull(Fusion.preview(state(storage = listOf(sw(5))), listOf(0)))
    }

    // ---------------- 재료 강화 ----------------

    @Test
    fun `재료를 넣으면 성공률이 오른다`() {
        assertTrue(MaterialBoost.bonusFor(listOf(sw(5))) > 0.0)
    }

    @Test
    fun `재료가 없으면 보정이 없다`() {
        assertEquals(0.0, MaterialBoost.bonusFor(emptyList()), 1e-9)
    }

    @Test
    fun `단계가 높은 재료가 더 큰 보정을 준다`() {
        assertTrue(MaterialBoost.bonusFor(listOf(sw(15))) > MaterialBoost.bonusFor(listOf(sw(2))))
    }

    @Test
    fun `재료 보정에 총 상한이 있다`() {
        val many = List(10) { sw(20) }
        assertEquals(MaterialBoost.TOTAL_CAP, MaterialBoost.bonusFor(many), 1e-9)
    }

    @Test
    fun `재료 한 자루가 줄 수 있는 보정에도 상한이 있다`() {
        assertTrue(MaterialBoost.bonusFor(listOf(sw(20))) <= MaterialBoost.PER_MATERIAL_CAP + 1e-9)
    }

    @Test
    fun `재료는 정해진 수까지만 넣을 수 있다`() {
        val s = state(storage = List(5) { sw(it) })
        assertTrue(MaterialBoost.canUse(s, listOf(0, 1, 2)))
        assertFalse(MaterialBoost.canUse(s, listOf(0, 1, 2, 3)))
    }

    @Test
    fun `재료는 강화 성패와 무관하게 태워진다`() {
        val s = state(storage = listOf(sw(3), sw(4), sw(5)))
        val after = MaterialBoost.consume(s, listOf(0, 2))
        assertEquals(listOf(sw(4)), after.storage)
    }

    // ---------------- 스타포스 ----------------

    @Test
    fun `일정 단계 아래에는 별을 붙일 수 없다`() {
        assertFalse(StarForce.canStar(sw(StarForce.MIN_LEVEL - 1)))
        assertTrue(StarForce.canStar(sw(StarForce.MIN_LEVEL)))
    }

    @Test
    fun `별에는 상한이 있다`() {
        assertFalse(StarForce.canStar(sw(15, stars = StarForce.MAX_STARS)))
    }

    @Test
    fun `별이 오르면 공격력이 오른다`() {
        val plain = Combat.attackPower(sw(15, stars = 0))
        val starred = Combat.attackPower(sw(15, stars = 3))
        assertTrue("$plain -> $starred", starred > plain)
    }

    @Test
    fun `별이 많을수록 비싸고 어려워진다`() {
        for (stars in 0 until StarForce.MAX_STARS - 1) {
            val a = sw(15, stars = stars)
            val b = sw(15, stars = stars + 1)
            assertTrue("조각", StarForce.shardCost(b) > StarForce.shardCost(a))
            assertTrue("확률", StarForce.successRate(b) <= StarForce.successRate(a))
        }
    }

    @Test
    fun `별 강화는 성공하면 별이 하나 오른다`() {
        val s = state(sword = sw(15, stars = 1))
        val result = StarForce.attempt(s, alwaysSucceed())
        assertTrue(result is StarForce.Result.Up)
        assertEquals(2, result.state.sword?.stars)
    }

    @Test
    fun `별 강화는 실패하면 별이 하나 줄지만 검은 부서지지 않는다`() {
        val s = state(sword = sw(15, stars = 2))
        val result = StarForce.attempt(s, alwaysFail())
        assertTrue(result is StarForce.Result.Down)
        assertEquals(1, result.state.sword?.stars)
        assertTrue("검이 남아 있어야 한다", result.state.sword != null)
    }

    @Test
    fun `별이 없을 때 실패해도 음수가 되지 않는다`() {
        val s = state(sword = sw(15, stars = 0))
        val result = StarForce.attempt(s, alwaysFail())
        assertEquals(0, result.state.sword?.stars)
    }

    @Test
    fun `별 강화 비용은 성패와 무관하게 빠진다`() {
        val sword = sw(15, stars = 1)
        val s = state(sword = sword, gold = 5_000_000, shards = 5_000)
        val expectedGold = 5_000_000 - StarForce.goldCost(sword)
        val expectedShards = 5_000 - StarForce.shardCost(sword)

        assertEquals(expectedGold, StarForce.attempt(s, alwaysSucceed()).state.gold)
        assertEquals(expectedShards, StarForce.attempt(s, alwaysFail()).state.shards)
    }

    @Test
    fun `조각이나 골드가 모자라면 별을 올릴 수 없다`() {
        assertFalse(StarForce.canAfford(state(sword = sw(15), shards = 0)))
        assertFalse(StarForce.canAfford(state(sword = sw(15), gold = 0)))
    }

    @Test
    fun `검이 없으면 별을 올릴 수 없다`() {
        assertFalse(StarForce.canAfford(state(sword = null)))
        assertFalse(StarForce.canStar(null))
    }

    @Test
    fun `별이 없으면 공격력 배수가 1이다`() {
        assertEquals(1.0, StarForce.attackMultiplier(sw(15, stars = 0)), 1e-9)
        assertEquals(1.0, StarForce.attackMultiplier(null), 1e-9)
    }

    private fun alwaysSucceed() = object : Random() {
        override fun nextBits(bitCount: Int): Int = 0
        override fun nextDouble(): Double = 0.0
    }

    private fun alwaysFail() = object : Random() {
        override fun nextBits(bitCount: Int): Int = 0
        override fun nextDouble(): Double = 0.999
    }
}
