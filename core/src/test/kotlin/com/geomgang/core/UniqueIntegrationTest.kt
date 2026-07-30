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
            gold = 1_000_000,
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

    @Test
    fun `시작의 검은 성공률이 3%p 높다`() {
        // 성공 경계 - 기본 확률로는 실패하고 +3%p 면 성공하는 롤을 찾는다
        val level = 14
        val base = RateTable.successRate(Difficulty.ENDLESS, level + 1, blessing = false)
        val roll = base + 0.01 // 기본으로는 실패, 보정으로는 성공
        val origin = GameState(
            difficulty = Difficulty.ENDLESS,
            gold = 1_000_000,
            sword = Sword(WeaponFamily.STRAIGHT, level, uniqueId = "origin"),
            forgeStones = 50,
        )
        val result = ForgeEngine.attempt(origin, UsedItems.NONE, ScriptedRandom(roll))
        assertTrue(result is ForgeResult.Success)
    }

    @Test
    fun `불사조는 파괴 대신 3단계를 잃고 살아나며 고유의 힘을 잃는다`() {
        val level = 17 // 파괴 가능 구간
        val state = GameState(
            difficulty = Difficulty.ENDLESS,
            gold = 10_000_000,
            sword = Sword(WeaponFamily.HOLY, level, uniqueId = "phoenix"),
            forgeStones = 50,
        )
        // 난수: 성공 판정 실패(1.0) -> 파괴 판정 성공(0.0)
        val result = ForgeEngine.attempt(state, UsedItems.NONE, ScriptedRandom(1.0, 0.0))
        assertTrue(result is ForgeResult.Drop)
        val sword = result.state.sword!!
        assertEquals(level - UniqueSwords.REVIVE_LEVEL_LOSS, sword.level)
        assertNull(sword.uniqueId)
        assertNull(result.state.pendingDestroy)
    }
}
