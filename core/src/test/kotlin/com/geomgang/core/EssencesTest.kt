package com.geomgang.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 정수 = 깊이의 무게.
 *
 * 예전에는 24종 중 세 종만 쓸모가 있었다. 이제 전부 무게를 갖되 깊은 구역이
 * 확실히 낫다 — 사냥터 후반이 존재할 이유가 여기서 생긴다.
 */
class EssencesTest {

    @Test
    fun `정수 무게는 구역 권장 레벨 더하기 하나다`() {
        assertEquals(1, Essences.weightOf("meadow")) // 권장 0
        assertEquals(13, Essences.weightOf("volcano")) // 권장 12
        assertEquals(19, Essences.weightOf("abyss")) // 권장 18
    }

    /** 24구역이 전부 값어치를 가져야 한다. 무게 0 짜리가 있으면 죽은 정수다. */
    @Test
    fun `모든 구역 정수가 무게를 갖는다`() {
        for (zone in Zone.entries) {
            assertTrue("${zone.id}", Essences.weightOf(zone.id) > 0)
        }
    }

    @Test
    fun `깊은 구역일수록 무겁다`() {
        val sorted = Zone.entries.sortedBy { it.recommendedLevel }
        for (i in 1 until sorted.size) {
            assertTrue(
                "${sorted[i].id}",
                Essences.weightOf(sorted[i].id) >= Essences.weightOf(sorted[i - 1].id),
            )
        }
    }

    @Test
    fun `정수력은 무게의 합이다`() {
        val held = mapOf("meadow" to 3, "volcano" to 2)
        assertEquals(3 * 1 + 2 * 13, Essences.powerOf(held))
        assertEquals(0, Essences.powerOf(emptyMap()))
    }

    // --- 소모 ---

    /** 고유검이 찾는 깊은 정수를 각인이 먼저 먹어 치우면 두 쓰임이 서로를 방해한다. */
    @Test
    fun `얕은 구역 정수부터 태운다`() {
        val held = mapOf("meadow" to 5, "abyss" to 2)
        val left = Essences.spend(held, 3)
        assertEquals(2, left["meadow"]) // 초원 3개(무게 3)로 덮인다
        assertEquals(2, left["abyss"]) // 심연은 그대로
    }

    @Test
    fun `얕은 것이 모자라면 깊은 것으로 넘어간다`() {
        val held = mapOf("meadow" to 2, "abyss" to 3)
        val left = Essences.spend(held, 21)
        // 초원 2(무게 2) + 심연 1(무게 19) = 21
        assertNull(left["meadow"])
        assertEquals(2, left["abyss"])
    }

    /** 정수는 쪼갤 수 없다. 무게가 남아도 마지막 한 개는 통째로 나간다. */
    @Test
    fun `모자란 무게를 덮으려면 한 개가 통째로 나간다`() {
        val left = Essences.spend(mapOf("abyss" to 2), 1)
        assertEquals(1, left["abyss"])
    }

    @Test
    fun `다 태우면 항목이 사라진다`() {
        assertEquals(emptyMap<String, Int>(), Essences.spend(mapOf("meadow" to 2), 2))
    }

    @Test
    fun `0을 태우면 그대로다`() {
        val held = mapOf("meadow" to 2)
        assertEquals(held, Essences.spend(held, 0))
    }
}

/**
 * 수호 각인.
 *
 * 전설검이 실패하면 무조건 +21 로 돌아간다. 각인은 그 한 번을 한 단계 하락으로
 * 바꾼다. **한 장뿐이다** — 쌓아 두면 전설 등반이 거저가 된다.
 */
class WardCharmTest {

    private fun state(
        power: Int,
        ward: Boolean = false,
        level: Int = LegendForge.LEVEL + 10,
    ) = GameState(
        difficulty = Difficulty.ENDLESS,
        gold = 1_000_000_000_000L,
        sword = Sword(WeaponFamily.DRAGON, level),
        bestLevel = LegendForge.LEVEL + 10,
        // 심연 정수(무게 19) 로 원하는 정수력을 만든다
        essences = mapOf("abyss" to (power + 18) / 19),
        wardCharm = ward,
    )

    @Test
    fun `정수력이 모자라면 살 수 없다`() {
        assertFalse(WardCharm.canBuy(state(power = WardCharm.COST - 19)))
    }

    @Test
    fun `사면 정수가 빠지고 각인이 생긴다`() {
        val before = state(power = WardCharm.COST + 19)
        assertTrue(WardCharm.canBuy(before))

        val after = WardCharm.buy(before)

        assertTrue(after.wardCharm)
        assertTrue(
            "정수력이 줄어야 한다",
            Essences.powerOf(after.essences) < Essences.powerOf(before.essences),
        )
    }

    /** 한 장뿐이라는 것이 이 장치의 전부다. */
    @Test
    fun `이미 지녔으면 또 살 수 없다`() {
        assertFalse(WardCharm.canBuy(state(power = WardCharm.COST * 3, ward = true)))
    }

    @Test
    fun `각인은 전설검만 지킨다`() {
        val s = state(power = 0, ward = true)
        assertTrue(WardCharm.protects(s, Sword(WeaponFamily.DRAGON, LegendForge.LEVEL)))
        assertFalse(WardCharm.protects(s, Sword(WeaponFamily.STRAIGHT, 20)))
    }

    // --- 판정 ---

    /**
     * 각인이 있으면 +21 로 떨어지지 않고 한 단계만 잃는다.
     *
     * 굴림 순서는 성공 → 파괴 → 파괴방지 셋이다(전설검은 방어 특성이 있다).
     */
    @Test
    fun `전설검이 미끄러질 때 각인이 한 단계만 잃게 한다`() {
        val before = state(power = 0, ward = true, level = 40)
        val result = ForgeEngine.attempt(
            before,
            UsedItems.NONE,
            ScriptedRandom(0.999, 0.0, 0.999),
        )
        assertTrue("결과=$result", result is ForgeResult.Drop)
        assertEquals(39, result.state.sword?.level)
        assertFalse("각인은 쓰면 사라진다", result.state.wardCharm)
    }

    @Test
    fun `각인이 없으면 21로 돌아간다`() {
        val before = state(power = 0, ward = false, level = 40)
        val result = ForgeEngine.attempt(
            before,
            UsedItems.NONE,
            ScriptedRandom(0.999, 0.0, 0.999),
        )
        assertEquals(LegendForge.LEVEL, result.state.sword?.level)
    }

    /** 방지 굴림이 먼저다. 그게 성공하면 각인은 아껴진다. */
    @Test
    fun `파괴방지가 막으면 각인은 남는다`() {
        val before = state(power = 0, ward = true, level = 40)
        val result = ForgeEngine.attempt(
            before,
            UsedItems.NONE,
            // 셋째 값 0.0 = 방지 굴림 성공
            ScriptedRandom(0.999, 0.0, 0.0),
        )
        assertTrue("결과=$result", result is ForgeResult.Stay)
        assertTrue("각인은 그대로여야 한다", result.state.wardCharm)
    }

    /** 계열 검은 각인과 무관하게 부서진다. */
    @Test
    fun `각인이 있어도 계열 검은 부서진다`() {
        val before = GameState(
            difficulty = Difficulty.ENDLESS,
            gold = 1_000_000_000L,
            sword = Sword(WeaponFamily.STRAIGHT, 15),
            wardCharm = true,
        )
        val result = ForgeEngine.attempt(before, UsedItems.NONE, ScriptedRandom(0.999, 0.0))
        assertTrue("결과=$result", result is ForgeResult.Destroyed)
        assertTrue("각인은 그대로여야 한다", result.state.wardCharm)
    }
}
