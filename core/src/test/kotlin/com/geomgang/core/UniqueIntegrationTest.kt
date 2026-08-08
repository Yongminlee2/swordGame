package com.geomgang.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

/** 고유검 패시브가 조합·강화·사냥 보상에 실제로 배선됐는지. */
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
            listOf(Sword(WeaponFamily.GREAT, 14), Sword(WeaponFamily.GREAT, 16)),
        )
        val fused = Fusion.fuse(state, listOf(0, 1))
        val result = fused.storage.single()
        assertEquals("trinity", result.uniqueId)
        assertEquals(16, result.level) // 고유검은 재료 중 최고 단계
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

    /**
     * 든 고유검은 "계열" 줄에 재료 계열(마검·성검…)로 새지 않는다 - 값이 없는
     * 줄이라 아예 뺀다(v2.4). 실제 값은 소유 보너스나 "고유검" 수집 줄이 보여준다.
     */
    @Test
    fun `든 고유검은 계열 출처 줄에 나오지 않는다`() {
        val holding = GameState(
            difficulty = Difficulty.ENDLESS,
            sword = Sword(WeaponFamily.DEMON, 12, uniqueId = "glutton"),
        )
        val labels = ForgeBonuses.sourcesOf(holding, ProgressState()).map { it.label }
        assertFalse("계열" in labels)
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

    /**
     * 탐식자·폭풍우·삼위일체도 시작의 검과 같은 방식으로 값을 한다(v2.4).
     *
     * 원래는 조각 두 배·공격 속도·보스 데미지로 전투에서만 값을 했는데, 셋 다
     * 시즌1에서 손에 넣을 수 있는데도 사냥터가 잠겨 있어(deepUnlocked) 얻자마자
     * 죽은 능력이었다. 소유만으로 강화 성공률을 주도록 바꿨다.
     */
    @Test
    fun `탐식자는 소유만으로 성공률 2%p 를 더한다`() {
        val without = GameState(difficulty = Difficulty.ENDLESS, sword = Sword(WeaponFamily.STRAIGHT, 5))
        val with = without.copy(
            storage = listOf(Sword(WeaponFamily.DEMON, 12, uniqueId = "glutton")),
        )
        val progress = ProgressState()
        val delta = ForgeBonuses.of(with, progress).successRate -
            ForgeBonuses.of(without, progress).successRate
        assertEquals(UniqueSwords.GLUTTON_FORGE_BONUS, delta, 1e-9)
    }

    @Test
    fun `폭풍우는 소유만으로 성공률 2%p 를 더한다`() {
        val without = GameState(difficulty = Difficulty.ENDLESS, sword = Sword(WeaponFamily.STRAIGHT, 5))
        val with = without.copy(
            storage = listOf(Sword(WeaponFamily.RAPIER, 12, uniqueId = "tempest")),
        )
        val progress = ProgressState()
        val delta = ForgeBonuses.of(with, progress).successRate -
            ForgeBonuses.of(without, progress).successRate
        assertEquals(UniqueSwords.TEMPEST_FORGE_BONUS, delta, 1e-9)
    }

    @Test
    fun `삼위일체는 소유만으로 성공률을 더한다`() {
        val without = GameState(difficulty = Difficulty.ENDLESS, sword = Sword(WeaponFamily.STRAIGHT, 5))
        val with = without.copy(
            storage = listOf(Sword(WeaponFamily.GREAT, 14, uniqueId = "trinity")),
        )
        val progress = ProgressState()
        val delta = ForgeBonuses.of(with, progress).successRate -
            ForgeBonuses.of(without, progress).successRate
        assertEquals(UniqueSwords.TRINITY_FORGE_BONUS, delta, 1e-9)
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
