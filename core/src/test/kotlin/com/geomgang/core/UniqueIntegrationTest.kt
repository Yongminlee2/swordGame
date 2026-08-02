package com.geomgang.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 고유검 패시브가 조합·전투·강화에 실제로 배선됐는지. */
class UniqueIntegrationTest {

    // --- 조합 ---

    private fun stateWith(storage: List<Sword>, essences: Map<String, Int> = emptyMap()) =
        GameState(
            difficulty = Difficulty.ENDLESS,
            gold = 1_000_000_000,
            storage = storage,
            essences = essences,
        )

    @Test
    fun `레시피와 맞으면 조합이 고유검을 만든다`() {
        val state = stateWith(
            listOf(Sword(WeaponFamily.HOLY, 10), Sword(WeaponFamily.HOLY, 12)),
        )
        val fused = Fusion.fuse(state, listOf(0, 1))
        val result = fused.storage.single()
        assertEquals("trinity", result.uniqueId)
        assertEquals(12, result.level) // 고유검은 재료 중 최고 단계
        assertEquals(WeaponFamily.HOLY, result.family)
    }

    @Test
    fun `고유검 조합은 정수를 차감한다`() {
        val state = stateWith(
            listOf(Sword(WeaponFamily.DEMON, 16), Sword(WeaponFamily.DEMON, 17)),
            essences = mapOf("abyss" to 7),
        )
        val fused = Fusion.fuse(state, listOf(0, 1))
        assertEquals("abyss_eater", fused.storage.single().uniqueId)
        assertEquals(2, fused.essences["abyss"])
    }

    @Test
    fun `정수를 다 쓰면 항목이 사라진다`() {
        val state = stateWith(
            listOf(Sword(WeaponFamily.DEMON, 16), Sword(WeaponFamily.DEMON, 17)),
            essences = mapOf("abyss" to 5),
        )
        val fused = Fusion.fuse(state, listOf(0, 1))
        assertNull(fused.essences["abyss"])
    }

    @Test
    fun `고유검은 조합 재료로 쓸 수 없다`() {
        val state = stateWith(
            listOf(
                Sword(WeaponFamily.HOLY, 12, uniqueId = "trinity"),
                Sword(WeaponFamily.HOLY, 12),
            ),
        )
        assertFalse(Fusion.canFuse(state, listOf(0, 1)))
    }

    @Test
    fun `레시피에도 표에도 없으면 조합되지 않는다`() {
        // 성검 둘 +10 미만 - 삼위일체 하한에 못 미치고 {성검} 은 조합표에 없다
        val state = stateWith(
            listOf(Sword(WeaponFamily.HOLY, 5), Sword(WeaponFamily.HOLY, 3)),
        )
        assertNull(Fusion.resultOrNull(state.storage, state.essences))
        assertFalse(Fusion.canFuse(state, listOf(0, 1)))
    }

    // --- 전투 ---

    @Test
    fun `삼위일체는 보스에게만 더 아프다`() {
        val plain = Sword(WeaponFamily.HOLY, 15)
        val trinity = plain.copy(uniqueId = "trinity")
        assertEquals(
            Combat.hit(plain, 0, isBoss = false).damage,
            Combat.hit(trinity, 0, isBoss = false).damage,
        )
        assertTrue(
            Combat.hit(trinity, 0, isBoss = true).damage >
                Combat.hit(plain, 0, isBoss = true).damage,
        )
    }

    @Test
    fun `폭풍우는 손이 빨라진다`() {
        val plain = Sword(WeaponFamily.RAPIER, 10)
        val tempest = plain.copy(uniqueId = "tempest")
        assertTrue(Combat.minTapMillis(tempest) < Combat.minTapMillis(plain))
    }

    // --- 강화 ---

    /** 고유검은 벼려진 그대로다(v2.3) — 강화대에 오르지 않는다. */
    @Test
    fun `고유검은 강화할 수 없다`() {
        val state = GameState(
            difficulty = Difficulty.ENDLESS,
            gold = 1_000_000_000,
            sword = Sword(WeaponFamily.STRAIGHT, 14, uniqueId = "origin"),
            forgeStones = 50,
        )
        assertFalse(ForgeEngine.canAttempt(state, UsedItems.NONE))
    }

    /** 시작의 검은 **지니고만 있어도** 강화 보너스를 준다 — 보관함에 있어도 된다. */
    @Test
    fun `시작의 검은 소유만으로 성공률 3%p 를 더한다`() {
        val without = GameState(
            difficulty = Difficulty.ENDLESS,
            sword = Sword(WeaponFamily.STRAIGHT, 5),
        )
        val with = without.copy(
            storage = listOf(Sword(WeaponFamily.STRAIGHT, 10, uniqueId = "origin")),
        )
        val progress = ProgressState()
        val delta = ForgeBonuses.of(with, progress).successRate -
            ForgeBonuses.of(without, progress).successRate
        assertEquals(UniqueSwords.ORIGIN_FORGE_BONUS, delta, 1e-9)
    }

    /** 불사조는 사냥 골드를 부른다 — 되살아나는 검이 아니다(v2.3). */
    @Test
    fun `불사조는 사냥 골드를 늘린다`() {
        val plain = Sword(WeaponFamily.HOLY, 17)
        val phoenix = plain.copy(uniqueId = "phoenix")
        assertEquals(1.25, UniqueSwords.goldMultOf(phoenix), 1e-9)
        assertEquals(1.0, UniqueSwords.goldMultOf(plain), 1e-9)
    }
}
