package com.geomgang.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressTest {

    private val empty = ProgressState()

    private fun forge(
        p: ProgressState,
        level: Int,
        result: ForgeResult,
        difficulty: Difficulty = Difficulty.NORMAL,
        family: WeaponFamily = WeaponFamily.STRAIGHT,
    ): ProgressState = Progress.onAttempt(
        p = p,
        difficulty = difficulty,
        family = family,
        targetLevel = level,
        cost = Economy.upgradeCost(level - 1),
        result = result,
    )

    private fun successAt(level: Int): ForgeResult.Success =
        ForgeResult.Success(
            state = GameState(Difficulty.NORMAL, sword = Sword(WeaponFamily.STRAIGHT, level)),
            newLevel = level,
        )

    private fun failAt(level: Int): ForgeResult.Stay =
        ForgeResult.Stay(
            state = GameState(Difficulty.NORMAL, sword = Sword(WeaponFamily.STRAIGHT, level - 1)),
            level = level - 1,
        )

    private fun destroyedAt(level: Int): ForgeResult.Destroyed =
        ForgeResult.Destroyed(
            state = GameState(
                Difficulty.NORMAL,
                pendingDestroy = PendingDestroy(WeaponFamily.STRAIGHT, level - 1),
            ),
            lostLevel = level - 1,
            preventable = false,
        )

    // --- 통계 ---

    @Test
    fun `강화 시도가 통계에 누적된다`() {
        var p = empty
        p = forge(p, 1, successAt(1))
        p = forge(p, 2, failAt(2))
        assertEquals(2L, p.stats.attempts)
        assertEquals(1L, p.stats.successes)
        assertEquals(1L, p.stats.stays)
    }

    @Test
    fun `단계별 시도와 성공이 따로 기록된다`() {
        var p = empty
        repeat(3) { p = forge(p, 10, failAt(10)) }
        p = forge(p, 10, successAt(10))
        assertEquals(4L, p.stats.attemptsByLevel[10])
        assertEquals(1L, p.stats.successesByLevel[10])
    }

    @Test
    fun `실제 성공률을 계산할 수 있다`() {
        var p = empty
        repeat(3) { p = forge(p, 10, failAt(10)) }
        p = forge(p, 10, successAt(10))
        assertEquals(0.25, p.stats.observedRate(10)!!, 1e-9)
    }

    @Test
    fun `시도한 적 없는 단계의 실제 성공률은 없다`() {
        assertNull(empty.stats.observedRate(7))
    }

    @Test
    fun `연속 실패 최대치가 기록되고 성공하면 초기화된다`() {
        var p = empty
        repeat(4) { p = forge(p, 8, failAt(8)) }
        assertEquals(4, p.stats.maxFailStreak)
        assertEquals(4, p.stats.currentFailStreak)
        p = forge(p, 8, successAt(8))
        assertEquals(0, p.stats.currentFailStreak)
        assertEquals(4, p.stats.maxFailStreak)
        repeat(2) { p = forge(p, 8, failAt(8)) }
        assertEquals(4, p.stats.maxFailStreak)
    }

    @Test
    fun `소비 골드가 누적된다`() {
        var p = empty
        p = forge(p, 1, successAt(1))
        assertEquals(Economy.upgradeCost(0), p.stats.goldSpent)
    }

    @Test
    fun `판매 수입이 누적된다`() {
        val p = Progress.onSell(empty, 6597)
        assertEquals(6597L, p.stats.goldEarned)
    }

    @Test
    fun `잡몹 처치는 monsterKills만 올린다`() {
        val p = Progress.onMonsterKill(empty, isBoss = false)
        assertEquals(1L, p.stats.monsterKills)
        assertEquals(0L, p.stats.bossKills)
    }

    @Test
    fun `보스 처치는 bossKills만 올린다`() {
        val p = Progress.onMonsterKill(empty, isBoss = true)
        assertEquals(0L, p.stats.monsterKills)
        assertEquals(1L, p.stats.bossKills)
    }

    @Test
    fun `조합과 별 강화가 따로 기록된다`() {
        var p = Progress.onFusion(empty)
        p = Progress.onStarAttempt(p)
        p = Progress.onStarAttempt(p)
        assertEquals(1L, p.stats.fusions)
        assertEquals(2L, p.stats.starAttempts)
    }

    @Test
    fun `이벤트 조우가 기록된다`() {
        val p = Progress.onEventSeen(empty)
        assertEquals(1L, p.stats.eventsSeen)
    }

    @Test
    fun `특수 계열은 어떤 업적으로도 드롭·상점에 풀리지 않는다`() {
        // 모든 업적을 다 딴 상태를 흉내 낸다
        val all = empty.copy(achievements = Achievement.entries.toSet())
        val unlocked = Progress.unlockedFamilies(all)
        assertTrue(WeaponFamily.FUSED !in unlocked)
        assertTrue(WeaponFamily.VOID !in unlocked)
    }

    @Test
    fun `방지권 사용과 놓침이 따로 기록된다`() {
        var p = Progress.onPreventUsed(empty)
        p = Progress.onPreventMissed(p)
        p = Progress.onPreventMissed(p)
        assertEquals(1L, p.stats.preventUsed)
        assertEquals(2L, p.stats.preventMissed)
    }

    @Test
    fun `줍기 성공과 놓침이 따로 기록된다`() {
        var p = Progress.onSalvage(empty, 24)
        p = Progress.onSalvageMissed(p)
        assertEquals(1L, p.stats.salvageTaken)
        assertEquals(1L, p.stats.salvageMissed)
        assertEquals(24L, p.stats.shardsEarned)
    }

    // --- 도감 ---

    @Test
    fun `검을 얻으면 해당 계열과 단계가 도감에 등록된다`() {
        val p = Progress.registerSword(
            empty,
            Difficulty.HARD,
            Sword(WeaponFamily.DRAGON, 19),
        )
        assertTrue(CodexKey(WeaponFamily.DRAGON, 19, Difficulty.HARD) in p.codex)
    }

    @Test
    fun `강화에 성공하면 도달한 단계가 도감에 등록된다`() {
        val p = forge(empty, 12, successAt(12), family = WeaponFamily.HOLY)
        assertTrue(CodexKey(WeaponFamily.HOLY, 12, Difficulty.NORMAL) in p.codex)
    }

    /** +21 위는 계열이 없는 전설 칸이다. 계열이 달라도 같은 칸을 채운다. */
    @Test
    fun `무한 구간 검은 계열 없는 전설 칸에 등록된다`() {
        var p = Progress.registerSword(empty, Difficulty.ENDLESS, Sword(WeaponFamily.DRAGON, 25))
        p = Progress.registerSword(p, Difficulty.ENDLESS, Sword(WeaponFamily.HOLY, 25))
        assertTrue(CodexKey(null, 25, Difficulty.ENDLESS) in p.codex)
        assertEquals(1, p.codex.size)
    }

    /**
     * 티어가 칸이던 시절 세이브를 불러온다.
     *
     * 정확한 단계가 남아 있지 않으므로 티어의 첫 단계로 옮긴다. 채운 칸 수는 그대로다.
     */
    @Test
    fun `옛 티어 기록은 티어의 첫 단계로 옮겨진다`() {
        val old = ProgressState(
            codex = setOf(
                CodexKey(WeaponFamily.DRAGON, difficulty = Difficulty.ENDLESS, tier = WeaponTier.FLAME),
                CodexKey(WeaponFamily.HOLY, difficulty = Difficulty.ENDLESS, tier = WeaponTier.RUSTY),
            ),
        )
        val moved = Progress.migrateCodex(old).codex
        assertEquals(2, moved.size)
        assertTrue(CodexKey(WeaponFamily.DRAGON, 12, Difficulty.ENDLESS) in moved)
        assertTrue(CodexKey(WeaponFamily.HOLY, 0, Difficulty.ENDLESS) in moved)
    }

    /** 무한 구간 티어는 계열마다 있던 것이 전설 칸 하나로 모인다. */
    @Test
    fun `옛 무한 구간 티어는 전설 칸 하나로 모인다`() {
        val old = ProgressState(
            codex = setOf(
                CodexKey(WeaponFamily.DRAGON, difficulty = Difficulty.ENDLESS, tier = WeaponTier.ABYSS),
                CodexKey(WeaponFamily.HOLY, difficulty = Difficulty.ENDLESS, tier = WeaponTier.ABYSS),
            ),
        )
        val moved = Progress.migrateCodex(old).codex
        assertEquals(setOf(CodexKey(null, 26, Difficulty.ENDLESS)), moved)
    }

    /** 이미 옮겨진 기록은 다시 건드리지 않는다. */
    @Test
    fun `단계가 적힌 기록은 이관이 지나간다`() {
        val current = ProgressState(codex = setOf(CodexKey(WeaponFamily.DRAGON, 7, Difficulty.ENDLESS)))
        assertEquals(current.codex, Progress.migrateCodex(current).codex)
    }

    @Test
    fun `같은 칸을 다시 얻어도 도감 크기는 늘지 않는다`() {
        var p = forge(empty, 12, successAt(12))
        val size = p.codex.size
        p = forge(p, 12, successAt(12))
        assertEquals(size, p.codex.size)
    }

    @Test
    fun `실패했을 때는 도감에 등록되지 않는다`() {
        val p = forge(empty, 12, failAt(12))
        assertTrue(p.codex.isEmpty())
    }

    // --- 업적 ---

    @Test
    fun `도달형 업적이 최고 단계로 달성된다`() {
        var p = forge(empty, 10, successAt(10))
        p = Progress.refresh(p)
        assertTrue(Achievement.REACH_5 in p.achievements)
        assertTrue(Achievement.REACH_10 in p.achievements)
        assertFalse(Achievement.REACH_12 in p.achievements)
    }

    @Test
    fun `불운형 업적 - 10연속 실패`() {
        var p = empty
        repeat(10) { p = forge(p, 8, failAt(8)) }
        p = Progress.refresh(p)
        assertTrue(Achievement.FAIL_STREAK_10 in p.achievements)
    }

    @Test
    fun `불운형 업적 - 첫 강화에서 실패`() {
        var p = forge(empty, 1, failAt(1))
        p = Progress.refresh(p)
        assertTrue(Achievement.FIRST_FAIL in p.achievements)
    }

    @Test
    fun `첫 강화에 성공하면 그 업적은 안 달린다`() {
        var p = forge(empty, 1, successAt(1))
        p = Progress.refresh(p)
        assertFalse(Achievement.FIRST_FAIL in p.achievements)
    }

    @Test
    fun `불운형 업적 - 19에서 파괴`() {
        var p = forge(empty, 20, destroyedAt(20))
        p = Progress.refresh(p)
        assertTrue(Achievement.DESTROY_AT_19 in p.achievements)
    }

    @Test
    fun `누적형 업적 - 파괴 50회와 100회`() {
        var p = empty
        repeat(50) { p = forge(p, 14, destroyedAt(14)) }
        p = Progress.refresh(p)
        assertTrue(Achievement.DESTROY_50 in p.achievements)
        assertFalse(Achievement.DESTROY_100 in p.achievements)
        repeat(50) { p = forge(p, 14, destroyedAt(14)) }
        p = Progress.refresh(p)
        assertTrue(Achievement.DESTROY_100 in p.achievements)
    }

    @Test
    fun `업적은 40종이고 아이디가 겹치지 않는다`() {
        assertEquals(40, Achievement.entries.size)
        val ids = Achievement.entries.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `모든 업적에 칭호가 붙어 있다`() {
        assertTrue(Achievement.entries.all { it.title.isNotBlank() })
    }

    @Test
    fun `한번 달성한 업적은 취소되지 않는다`() {
        var p = empty
        repeat(10) { p = forge(p, 8, failAt(8)) }
        p = Progress.refresh(p)
        p = forge(p, 8, successAt(8))
        p = Progress.refresh(p)
        // 연속 실패는 끊겼지만 업적은 남는다
        assertEquals(0, p.stats.currentFailStreak)
        assertTrue(Achievement.FAIL_STREAK_10 in p.achievements)
    }

    // --- 계열 해금 ---

    @Test
    fun `처음에는 기본 계열 4종만 열려 있다`() {
        assertEquals(WeaponFamily.STARTERS, Progress.unlockedFamilies(empty))
    }

    // v1.4: 계열 해금이 업적에서 진행도 조건으로 바뀌었다.
    // 쌍검·마검·용검 등 10계열은 이제 조합 전용이라 상점에 나오지 않는다.
    // 해금 규칙 자체는 FamilyUnlockTest 가 지킨다.

    @Test
    fun `10단계를 달성하면 곡도가 열린다`() {
        var p = forge(empty, 10, successAt(10))
        p = Progress.refresh(p)
        assertTrue(WeaponFamily.CURVED in Progress.unlockedFamilies(p))
    }

    @Test
    fun `강화만 해서는 조합 전용 계열이 열리지 않는다`() {
        var p = forge(empty, 18, successAt(18))
        p = Progress.refresh(p)
        assertFalse(WeaponFamily.TWIN in Progress.unlockedFamilies(p))
        assertFalse(WeaponFamily.DRAGON in Progress.unlockedFamilies(p))
        assertFalse(WeaponFamily.DEMON in Progress.unlockedFamilies(p))
    }

    // --- 칭호 ---

    @Test
    fun `달성한 업적의 칭호만 선택할 수 있다`() {
        var p = forge(empty, 5, successAt(5))
        p = Progress.refresh(p)
        p = Progress.selectTitle(p, Achievement.REACH_5)
        assertEquals(Achievement.REACH_5, p.selectedTitle)
    }

    @Test(expected = IllegalStateException::class)
    fun `달성하지 않은 칭호는 선택할 수 없다`() {
        Progress.selectTitle(empty, Achievement.REACH_20)
    }

    @Test
    fun `칭호를 해제할 수 있다`() {
        var p = forge(empty, 5, successAt(5))
        p = Progress.refresh(p)
        p = Progress.selectTitle(p, Achievement.REACH_5)
        p = Progress.selectTitle(p, null)
        assertNull(p.selectedTitle)
    }
}
