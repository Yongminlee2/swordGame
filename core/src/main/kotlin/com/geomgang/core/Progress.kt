package com.geomgang.core

import kotlinx.serialization.Serializable

/**
 * 업적. 각 업적은 해금되는 칭호를 하나씩 갖는다.
 *
 * 세 갈래로 나뉜다.
 * - 도달형: 특정 단계 달성
 * - 누적형: 횟수·수량 누적
 * - 불운형: 실패와 관련된 것들. 망했을 때도 얻는 게 있어야 재도전으로 이어진다.
 */
enum class Achievement(val id: String, val displayName: String, val title: String) {
    // 도달형
    REACH_5("reach_5", "+5 달성", "견습 대장장이"),
    REACH_10("reach_10", "+10 달성", "숙련 대장장이"),
    REACH_12("reach_12", "+12 달성", "쌍검의 계승자"),
    REACH_15("reach_15", "+15 달성", "빛을 담은 자"),
    REACH_18("reach_18", "+18 달성", "용을 벼린 자"),
    REACH_20("reach_20", "+20 달성", "흑룡참의 주인"),
    ENDLESS_25("endless_25", "무한 모드 +25 달성", "끝을 보는 자"),

    // 누적형
    ATTEMPTS_1000("attempts_1000", "강화 1,000회", "망치의 세월"),
    DESTROY_50("destroy_50", "파괴 50회", "마검의 부름"),
    DESTROY_100("destroy_100", "파괴 100회", "잿더미의 증인"),
    SHARDS_5000("shards_5000", "조각 5,000개 획득", "부지런한 넝마주이"),
    SALVAGE_10("salvage_10", "줍기 10회 성공", "알뜰한 손"),
    PREVENT_10("prevent_10", "방지권 10회 사용", "위기의 순간"),
    CODEX_HALF("codex_half", "도감 절반 수집", "수집가"),
    CODEX_FULL("codex_full", "도감 완성", "만검의 주인"),
    BAILOUT("bailout", "파산 구제 받기", "빈털터리"),

    // 불운형
    FAIL_STREAK_10("fail_streak_10", "10연속 실패", "불운의 화신"),
    PREVENT_MISS_3("prevent_miss_3", "방지권 3회 놓침", "굼뜬 손"),
    DESTROY_AT_19("destroy_at_19", "+19 검을 파괴", "한 끗 차이"),
    FIRST_FAIL("first_fail", "첫 강화에서 실패", "불길한 시작"),
}

/** 도감 한 칸의 획득 기록. 같은 계열·티어라도 난이도가 다르면 별개 기록이다. */
@Serializable
data class CodexKey(
    val family: WeaponFamily,
    val tier: WeaponTier,
    val difficulty: Difficulty,
)

/**
 * 누적 통계. 전 모드 합산이다.
 *
 * [attemptsByLevel]과 [successesByLevel]이 통계 화면의 "표기 확률 대 실제 확률" 비교표를 만든다.
 */
@Serializable
data class Stats(
    val attempts: Long = 0,
    val successes: Long = 0,
    val stays: Long = 0,
    val drops: Long = 0,
    val destroys: Long = 0,
    val attemptsByLevel: Map<Int, Long> = emptyMap(),
    val successesByLevel: Map<Int, Long> = emptyMap(),
    val currentFailStreak: Int = 0,
    val maxFailStreak: Int = 0,
    val highestDestroyedLevel: Int = -1,
    val goldSpent: Long = 0,
    val goldEarned: Long = 0,
    val shardsEarned: Long = 0,
    val preventUsed: Long = 0,
    val preventMissed: Long = 0,
    val salvageTaken: Long = 0,
    val salvageMissed: Long = 0,
    val bailouts: Long = 0,
    val bestLevelEver: Int = 0,
    val bestEndlessLevel: Int = 0,
    // v1.3 확장. 전부 기본값이라 옛 세이브가 그대로 열린다.
    val monsterKills: Long = 0,
    val bossKills: Long = 0,
    val fusions: Long = 0,
    val starAttempts: Long = 0,
    val eventsSeen: Long = 0,
) {
    /** 이 단계에서 실제로 관측된 성공률. 시도한 적이 없으면 null. */
    fun observedRate(targetLevel: Int): Double? {
        val tried = attemptsByLevel[targetLevel] ?: return null
        if (tried == 0L) return null
        val won = successesByLevel[targetLevel] ?: 0L
        return won.toDouble() / tried.toDouble()
    }
}

/**
 * 모드에 종속되지 않는 전역 진행도.
 *
 * 모드를 초기화해도 이 상태는 지우지 않는다. 수집물이 초기화로 날아가면
 * 아무도 초기화를 누르지 않게 되고, 그러면 재도전이라는 이 장르의 핵심 재미가 막힌다.
 */
@Serializable
data class ProgressState(
    val codex: Set<CodexKey> = emptySet(),
    val achievements: Set<Achievement> = emptySet(),
    val selectedTitle: Achievement? = null,
    val stats: Stats = Stats(),
    /** 발견한 고유검 id. 도감 고유검 페이지가 이걸 본다. */
    val uniqueFound: Set<String> = emptySet(),
)

/** 진행도 누적 규칙. */
object Progress {

    /** 계열별 해금 조건. null 이면 처음부터 열려 있다. */
    private val FAMILY_UNLOCK: Map<WeaponFamily, Achievement?> = mapOf(
        WeaponFamily.STRAIGHT to null,
        WeaponFamily.CURVED to null,
        WeaponFamily.GREAT to null,
        WeaponFamily.RAPIER to null,
        WeaponFamily.TWIN to Achievement.REACH_12,
        WeaponFamily.DEMON to Achievement.DESTROY_50,
        WeaponFamily.HOLY to Achievement.REACH_15,
        WeaponFamily.DRAGON to Achievement.REACH_18,
        WeaponFamily.SCYTHE to Achievement.REACH_10,
        WeaponFamily.AXE to Achievement.DESTROY_100,
        WeaponFamily.SPEAR to Achievement.SALVAGE_10,
        WeaponFamily.SPIRIT to Achievement.REACH_20,
    )

    /** 검을 손에 넣었을 때 도감에 등록한다. 구매·조합·강화 성공 모두 여기를 지난다. */
    fun registerSword(p: ProgressState, difficulty: Difficulty, sword: Sword): ProgressState {
        val key = CodexKey(sword.family, WeaponCatalog.tierFor(sword.level), difficulty)
        return if (key in p.codex) p else p.copy(codex = p.codex + key)
    }

    /**
     * 강화 시도 한 번을 통계와 도감에 반영한다.
     *
     * @param targetLevel 이번 시도로 도달하려던 단계
     * @param cost        이번 시도에 쓴 골드
     */
    fun onAttempt(
        p: ProgressState,
        difficulty: Difficulty,
        family: WeaponFamily,
        targetLevel: Int,
        cost: Long,
        result: ForgeResult,
    ): ProgressState {
        val s = p.stats
        val succeeded = result is ForgeResult.Success
        val streak = if (succeeded) 0 else s.currentFailStreak + 1

        var stats = s.copy(
            attempts = s.attempts + 1,
            successes = s.successes + if (succeeded) 1 else 0,
            stays = s.stays + if (result is ForgeResult.Stay) 1 else 0,
            drops = s.drops + if (result is ForgeResult.Drop) 1 else 0,
            destroys = s.destroys + if (result is ForgeResult.Destroyed) 1 else 0,
            attemptsByLevel = s.attemptsByLevel.increment(targetLevel),
            successesByLevel =
                if (succeeded) s.successesByLevel.increment(targetLevel) else s.successesByLevel,
            currentFailStreak = streak,
            maxFailStreak = maxOf(s.maxFailStreak, streak),
            goldSpent = s.goldSpent + cost,
        )

        if (result is ForgeResult.Destroyed) {
            stats = stats.copy(
                highestDestroyedLevel = maxOf(stats.highestDestroyedLevel, result.lostLevel),
            )
        }

        if (succeeded) {
            stats = stats.copy(
                bestLevelEver = maxOf(stats.bestLevelEver, targetLevel),
                bestEndlessLevel =
                    if (difficulty == Difficulty.ENDLESS) {
                        maxOf(stats.bestEndlessLevel, targetLevel)
                    } else {
                        stats.bestEndlessLevel
                    },
            )
        }

        val withStats = p.copy(stats = stats)
        return if (succeeded) {
            registerSword(withStats, difficulty, Sword(family, targetLevel))
        } else {
            withStats
        }
    }

    fun onPreventUsed(p: ProgressState): ProgressState =
        p.copy(stats = p.stats.copy(preventUsed = p.stats.preventUsed + 1))

    fun onPreventMissed(p: ProgressState): ProgressState =
        p.copy(stats = p.stats.copy(preventMissed = p.stats.preventMissed + 1))

    fun onSalvage(p: ProgressState, shards: Int): ProgressState =
        p.copy(
            stats = p.stats.copy(
                salvageTaken = p.stats.salvageTaken + 1,
                shardsEarned = p.stats.shardsEarned + shards,
            ),
        )

    fun onSalvageMissed(p: ProgressState): ProgressState =
        p.copy(stats = p.stats.copy(salvageMissed = p.stats.salvageMissed + 1))

    fun onSell(p: ProgressState, gold: Long): ProgressState =
        p.copy(stats = p.stats.copy(goldEarned = p.stats.goldEarned + gold))

    fun onMonsterKill(p: ProgressState, isBoss: Boolean): ProgressState =
        p.copy(
            stats = if (isBoss) {
                p.stats.copy(bossKills = p.stats.bossKills + 1)
            } else {
                p.stats.copy(monsterKills = p.stats.monsterKills + 1)
            },
        )

    fun onFusion(p: ProgressState): ProgressState =
        p.copy(stats = p.stats.copy(fusions = p.stats.fusions + 1))

    fun onStarAttempt(p: ProgressState): ProgressState =
        p.copy(stats = p.stats.copy(starAttempts = p.stats.starAttempts + 1))

    fun onEventSeen(p: ProgressState): ProgressState =
        p.copy(stats = p.stats.copy(eventsSeen = p.stats.eventsSeen + 1))

    /** 고유검을 발견했다. 도감의 "???"가 이름으로 바뀐다. */
    fun onUniqueFound(p: ProgressState, uniqueId: String): ProgressState =
        if (uniqueId in p.uniqueFound) p else p.copy(uniqueFound = p.uniqueFound + uniqueId)

    fun onBailout(p: ProgressState): ProgressState =
        p.copy(stats = p.stats.copy(bailouts = p.stats.bailouts + 1))

    /**
     * 현재 통계·도감을 보고 달성 업적을 통째로 다시 계산한다.
     *
     * 즉석에서 하나씩 판정하지 않는 이유: 세이브를 불러왔을 때나 업적을 새로 추가했을 때
     * 소급 적용이 자동으로 되기 때문이다. 이미 달성한 업적은 절대 취소되지 않는다.
     */
    fun refresh(p: ProgressState): ProgressState {
        val s = p.stats
        val distinctEntries = p.codex.map { CodexEntry(it.family, it.tier) }.toSet().size
        val total = WeaponCatalog.ENTRIES.size

        val earned = buildSet {
            addAll(p.achievements)

            if (s.bestLevelEver >= 5) add(Achievement.REACH_5)
            if (s.bestLevelEver >= 10) add(Achievement.REACH_10)
            if (s.bestLevelEver >= 12) add(Achievement.REACH_12)
            if (s.bestLevelEver >= 15) add(Achievement.REACH_15)
            if (s.bestLevelEver >= 18) add(Achievement.REACH_18)
            if (s.bestLevelEver >= 20) add(Achievement.REACH_20)
            if (s.bestEndlessLevel >= 25) add(Achievement.ENDLESS_25)

            if (s.attempts >= 1_000) add(Achievement.ATTEMPTS_1000)
            if (s.destroys >= 50) add(Achievement.DESTROY_50)
            if (s.destroys >= 100) add(Achievement.DESTROY_100)
            if (s.shardsEarned >= 5_000) add(Achievement.SHARDS_5000)
            if (s.salvageTaken >= 10) add(Achievement.SALVAGE_10)
            if (s.preventUsed >= 10) add(Achievement.PREVENT_10)
            if (s.bailouts >= 1) add(Achievement.BAILOUT)
            if (distinctEntries >= total / 2) add(Achievement.CODEX_HALF)
            if (distinctEntries >= total) add(Achievement.CODEX_FULL)

            if (s.maxFailStreak >= 10) add(Achievement.FAIL_STREAK_10)
            if (s.preventMissed >= 3) add(Achievement.PREVENT_MISS_3)
            if (s.highestDestroyedLevel >= 19) add(Achievement.DESTROY_AT_19)
            // 아직 한 번도 성공하지 못했는데 +1 시도를 한 적이 있으면 첫 강화에서 실패한 것이다.
            if (s.successes == 0L && s.attemptsByLevel[1] != null) add(Achievement.FIRST_FAIL)
        }
        return if (earned == p.achievements) p else p.copy(achievements = earned)
    }

    /** 지금 고를 수 있는 계열들. 항상 enum 선언 순서를 유지한다. */
    fun unlockedFamilies(p: ProgressState): List<WeaponFamily> =
        WeaponFamily.entries.filter { family ->
            // 특수 계열(합검·허검)은 상점·드롭에 절대 나오지 않는다.
            if (family in WeaponFamily.SPECIAL) return@filter false
            val required = FAMILY_UNLOCK[family]
            required == null || required in p.achievements
        }

    /** 칭호를 고르거나(달성한 업적만) 해제한다(null). */
    fun selectTitle(p: ProgressState, achievement: Achievement?): ProgressState {
        if (achievement != null) {
            check(achievement in p.achievements) { "achievement not earned: ${achievement.id}" }
        }
        return p.copy(selectedTitle = achievement)
    }

    private fun Map<Int, Long>.increment(key: Int): Map<Int, Long> =
        this + (key to ((this[key] ?: 0L) + 1L))
}
