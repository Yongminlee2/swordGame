package com.geomgang.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** 무한 구간에서 담금질이 쌓이고 지워지는지. */
class ForgeEngineTemperTest {

    private fun endless(level: Int, temperLevel: Int = 0, temperFails: Int = 0) = GameState(
        difficulty = Difficulty.ENDLESS,
        gold = 1_000_000_000_000_000L,
        sword = Sword(WeaponFamily.STRAIGHT, level),
        forgeStones = 999,
        temperLevel = temperLevel,
        temperFails = temperFails,
    )

    /**
     * 항상 실패하는 난수.
     *
     * `nextDouble()` 은 `nextBits` 를 두 번 불러 만든다. 전부 1 로 채우면 1 에 가장
     * 가까운 값이 나와 어떤 성공률도 통과하지 못한다.
     */
    private fun alwaysFail(): Random = object : Random() {
        override fun nextBits(bitCount: Int): Int =
            if (bitCount >= 32) -1 else (1 shl bitCount) - 1
    }

    @Test
    fun `무한 구간에서 실패하면 담금질이 쌓인다`() {
        val result = ForgeEngine.attempt(endless(44), UsedItems.NONE, alwaysFail())
        assertEquals(45, result.state.temperLevel)
        assertEquals(1, result.state.temperFails)
    }

    @Test
    fun `쌓인 위에 또 실패하면 하나 더 쌓인다`() {
        val state = endless(44, temperLevel = 45, temperFails = 7)
        val result = ForgeEngine.attempt(state, UsedItems.NONE, alwaysFail())
        assertEquals(8, result.state.temperFails)
    }

    /**
     * 부적을 써도 담금질은 오른다.
     *
     * v2.3부터 부적은 하락만 막는다. 무한 구간 실패는 항상 파괴 판정(1.00)이라
     * 부적이 개입할 자리가 없고, 전설검은 단계를 잃는 것으로 대가를 치른다 —
     * 그 실패도 담금질에는 쌓여야 다음 도전이 가벼워진다.
     */
    @Test
    fun `부적을 쓴 실패도 담금질을 올린다`() {
        val state = endless(44).copy(inventory = Inventory(luckCharms = 1))
        val result = ForgeEngine.attempt(state, UsedItems(luckCharm = true), alwaysFail())
        assertTrue("결과=$result", result is ForgeResult.Drop)
        assertEquals(1, result.state.temperFails)
    }

    @Test
    fun `성공하면 담금질이 지워진다`() {
        // 담금질을 크게 쌓아 두면 상한 50%까지 오른다. 성공이 나올 때까지 같은 상태로 굴린다.
        val state = endless(44, temperLevel = 45, temperFails = 10_000)
        val rng = Random(7)
        var success: ForgeResult.Success? = null
        for (i in 0 until 200) {
            val r = ForgeEngine.attempt(state, UsedItems.NONE, rng)
            if (r is ForgeResult.Success) {
                success = r
                break
            }
        }
        val done = requireNotNull(success) { "상한 50%인데 200번 안에 성공이 없다" }
        assertEquals(0, done.state.temperLevel)
        assertEquals(0, done.state.temperFails)
    }

    @Test
    fun `유한 구간에서는 쌓지 않는다`() {
        val state = GameState(
            difficulty = Difficulty.NORMAL,
            gold = 1_000_000L,
            sword = Sword(WeaponFamily.STRAIGHT, 10),
        )
        val result = ForgeEngine.attempt(state, UsedItems.NONE, alwaysFail())
        assertEquals(0, result.state.temperLevel)
        assertEquals(0, result.state.temperFails)
    }
}
