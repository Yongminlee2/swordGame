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

    // ---------------- 조합 (v2.3: 고유검 레시피 전용) ----------------
    // 계열 조합(마검·성검)은 [Refinery]의 몫이 됐다. 여기 조합은 고유검만 만든다.

    @Test
    fun `고유검 레시피와 맞으면 조합된다`() {
        // 삼위일체 - 대검 +14 둘
        val result = Fusion.resultOrNull(
            listOf(sw(14, WeaponFamily.GREAT), sw(16, WeaponFamily.GREAT)),
        )
        assertEquals("trinity", result?.uniqueId)
        assertEquals(WeaponFamily.HOLY, result?.family)
    }

    @Test
    fun `레시피와 안 맞으면 아무것도 안 된다`() {
        // 옛 조합표(직검+곡도=마검, 대검+세검=성검)는 사라졌다
        assertNull(Fusion.resultOrNull(listOf(sw(5, WeaponFamily.STRAIGHT), sw(2, WeaponFamily.CURVED))))
        assertNull(Fusion.resultOrNull(listOf(sw(10, WeaponFamily.GREAT), sw(8, WeaponFamily.RAPIER))))
        // 마검+성검도 일반 조합으로는 안 된다 - 용검은 전설 칸 전용이다.
        // (정수를 갖추면 불사조 레시피가 잡지만, 그건 고유검이지 용검이 아니다)
        assertNull(
            Fusion.resultOrNull(listOf(sw(20, WeaponFamily.DEMON), sw(20, WeaponFamily.HOLY))),
        )
        val s = state(storage = listOf(sw(5, WeaponFamily.CURVED), sw(5, WeaponFamily.CURVED)))
        assertFalse(Fusion.canFuse(s, listOf(0, 1)))
    }

    /** 조합은 검을 만드는 장치다. 단계는 절대 재료 최고치를 넘지 않는다. */
    @Test
    fun `조합으로 단계가 오르지 않는다`() {
        val result = Fusion.resultOrNull(
            listOf(sw(14, WeaponFamily.GREAT), sw(16, WeaponFamily.GREAT)),
        )
        assertEquals(16, result?.level)
    }

    @Test
    fun `조합 결과에는 별이 이어지지 않는다`() {
        // 녹여서 새로 만드는 것이므로 0부터다
        val result = Fusion.resultOrNull(
            listOf(
                sw(14, WeaponFamily.GREAT, stars = 4),
                sw(16, WeaponFamily.GREAT, stars = 3),
            ),
        )
        assertEquals(0, result?.stars)
    }

    @Test
    fun `재료가 한 자루면 조합할 수 없다`() {
        val s = state(storage = listOf(sw(14, WeaponFamily.GREAT)))
        assertFalse(Fusion.canFuse(s, listOf(0)))
    }

    @Test
    fun `세 자루도 조합할 수 없다`() {
        val s = state(
            storage = listOf(
                sw(10, WeaponFamily.HOLY),
                sw(15, WeaponFamily.GREAT),
                sw(16, WeaponFamily.GREAT),
            ),
        )
        assertFalse(Fusion.canFuse(s, listOf(0, 1, 2)))
    }

    @Test
    fun `같은 자리를 두 번 넣을 수 없다`() {
        val s = state(storage = listOf(sw(14, WeaponFamily.GREAT), sw(16, WeaponFamily.GREAT)))
        assertFalse(Fusion.canFuse(s, listOf(0, 0)))
    }

    @Test
    fun `골드가 모자라면 조합할 수 없다`() {
        val s = state(
            storage = listOf(sw(14, WeaponFamily.GREAT), sw(16, WeaponFamily.GREAT)),
            gold = 1,
        )
        assertFalse(Fusion.canFuse(s, listOf(0, 1)))
    }

    @Test
    fun `조합하면 재료가 사라지고 결과가 보관함에 들어온다`() {
        val s = state(storage = listOf(sw(14, WeaponFamily.GREAT), sw(16, WeaponFamily.GREAT)))
        val after = Fusion.fuse(s, listOf(0, 1))
        assertEquals(1, after.storage.size)
        assertEquals("trinity", after.storage.first().uniqueId)
        assertEquals(16, after.storage.first().level)
    }

    @Test
    fun `조합은 골드를 쓴다`() {
        val s = state(
            storage = listOf(sw(14, WeaponFamily.GREAT), sw(16, WeaponFamily.GREAT)),
            gold = 1_000_000,
        )
        val cost = Fusion.cost(s, listOf(0, 1))
        assertTrue(cost > 0)
        assertEquals(1_000_000 - cost, Fusion.fuse(s, listOf(0, 1)).gold)
    }

    @Test
    fun `미리보기가 실제 결과와 같다`() {
        val s = state(storage = listOf(sw(14, WeaponFamily.GREAT), sw(16, WeaponFamily.GREAT)))
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
