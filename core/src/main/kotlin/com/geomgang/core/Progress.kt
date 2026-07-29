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

    // 사냥형 — 강화 밖의 갈래들. v1.3~v1.4 에서 늘어난 컨텐츠를 담는다.
    HUNT_100("hunt_100", "잡몹 100마리 처치", "첫 사냥꾼"),
    HUNT_1000("hunt_1000", "잡몹 1,000마리 처치", "들판의 청소부"),
    HUNT_10000("hunt_10000", "잡몹 10,000마리 처치", "몰이꾼"),
    BOSS_FIRST("boss_first", "첫 보스 처치", "우두머리 사냥"),
    ZONES_ALL("zones_all", "모든 구역 클리어", "열두 땅의 정복자"),
    EVENT_50("event_50", "사냥 이벤트 50회 조우", "운명을 부르는 자"),

    // 조합형
    FUSE_10("fuse_10", "조합 10회", "도가니의 손"),
    FUSE_100("fuse_100", "조합 100회", "합성의 대가"),
    UNIQUE_FIRST("unique_first", "고유검 발견", "전설의 시작"),
    UNIQUE_5("unique_5", "고유검 5종 발견", "전설 수집가"),
    UNIQUE_ALL("unique_all", "고유검 10종 전부 발견", "이름을 아는 자"),

    // 특수강화·재료형
    STAR_3("star_3", "별 3개 달성", "별을 붙인 자"),
    STAR_5("star_5", "별 5개 달성", "별의 정점"),
    STONE_500("stone_500", "강화석 500개 획득", "광맥의 주인"),
    SKILL_100("skill_100", "스킬 100회 발동", "검술의 경지"),

    // 회랑·수집형
    GAUNTLET_10("gauntlet_10", "무한 회랑 10층", "회랑의 방문자"),
    GAUNTLET_25("gauntlet_25", "무한 회랑 25층", "회랑의 주인"),
    GAUNTLET_50("gauntlet_50", "무한 회랑 50층", "끝없는 복도"),
    PET_FIRST("pet_first", "펫 1마리 얻기", "동행"),
    PET_ALL("pet_all", "펫 12종 전부 모으기", "동물의 친구"),
}

/**
 * 도감 한 칸의 획득 기록. 같은 칸이라도 난이도가 다르면 별개 기록이다.
 *
 * [family] 가 null 이면 전설 칸이다 — [WeaponCatalog.FAMILY_MAX_LEVEL] 위는 계열과
 * 무관하게 같은 그림이라 칸도 하나다.
 */
@Serializable
data class CodexKey(
    val family: WeaponFamily? = null,
    val level: Int = LEGACY_LEVEL,
    val difficulty: Difficulty,
    /**
     * 옛 세이브 호환용.
     *
     * 티어가 곧 칸이던 시절의 기록이다. 불러올 때 [Progress.migrateCodex] 가
     * 티어의 첫 단계로 옮기고 이 값을 버린다. 새로 쓰는 기록에는 들어가지 않는다.
     */
    val tier: WeaponTier? = null,
) {
    companion object {
        /** 단계가 적히지 않은 기록 — 티어 시절 세이브라는 뜻이다. */
        const val LEGACY_LEVEL: Int = -1
    }
}

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
    // v1.4 확장. 업적이 새 시스템을 볼 수 있게 하는 카운터들이다.
    val skillsTriggered: Long = 0,
    val stonesEarned: Long = 0,
    val gauntletBestEver: Int = 0,
    val maxStars: Int = 0,
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
    /**
     * 보스를 깬 구역 id. 모드 초기화로도 지워지지 않는 전역 기록이다.
     * 계열 해금 조건(대검)이 이걸 본다.
     */
    val clearedZones: Set<String> = emptySet(),
    /**
     * 대장간 스킬 레벨. 골드로 올리는 영구 성장이다.
     *
     * 진행도에 두는 이유: 모드 초기화로 지워지면 아무도 초기화를 누르지 않는다.
     */
    val smithyLevel: Int = 0,
    /**
     * 전설검을 한 번이라도 도감에 바쳤는지.
     *
     * 켜지면 조각으로 전설검을 다시 벼릴 수 있다. 진행도에 두어 모드 초기화로도
     * 지워지지 않게 한다 — **가장 높은 벽을 두 번 넘으라고 하면 안 된다.**
     */
    val legendUnlocked: Boolean = false,
    /**
     * 한 번이라도 얻어 본 펫 id. 수집 기록이므로 [uniqueFound] 와 같이 전역에 남는다.
     * 지금 보유·레벨은 모드 세이브([GameState.pets])가 들고 있다.
     */
    val petsFound: Set<String> = emptySet(),
)

/** 진행도 누적 규칙. */
object Progress {

    /** 곡도가 열리는 최고 단계. */
    const val CURVED_UNLOCK_LEVEL = 10

    /** 대검이 열리는 데 필요한 서로 다른 구역 클리어 수. */
    const val GREAT_UNLOCK_ZONES = 3

    /**
     * 기본 계열의 해금 조건.
     *
     * 시작은 직검 하나뿐이다. 계열이 열리는 것 자체가 진행의 이정표가 되고,
     * 조건이 서로 다른 활동(강화·사냥·조합)을 가리켜 한 갈래만 파도 다 열리지 않는다.
     * 조건 판정은 전역 진행도만 본다 — 그래서 **이미 달성한 세이브는 소급 적용**된다.
     */
    fun basicFamilyUnlocked(p: ProgressState, family: WeaponFamily): Boolean = when (family) {
        WeaponFamily.STRAIGHT -> true
        WeaponFamily.CURVED -> p.stats.bestLevelEver >= CURVED_UNLOCK_LEVEL
        WeaponFamily.GREAT -> p.clearedZones.size >= GREAT_UNLOCK_ZONES
        WeaponFamily.RAPIER -> p.uniqueFound.isNotEmpty()
        else -> false
    }

    /** 아직 잠긴 기본 계열의 해금 조건 설명. 열려 있으면 null. */
    fun basicFamilyHint(p: ProgressState, family: WeaponFamily): String? {
        if (basicFamilyUnlocked(p, family)) return null
        return when (family) {
            WeaponFamily.CURVED -> "아무 검 +$CURVED_UNLOCK_LEVEL 달성"
            WeaponFamily.GREAT ->
                "구역 ${GREAT_UNLOCK_ZONES}곳 클리어 " +
                    "(${p.clearedZones.size}/$GREAT_UNLOCK_ZONES)"

            WeaponFamily.RAPIER -> "고유검 1개 발견"
            else -> null
        }
    }

    /**
     * 도감이 아직 다 차지 않은 계열.
     *
     * 조각 교환으로 검을 줄 때 이쪽을 먼저 준다. 계열은 성능이 전부 같아서
     * 무엇을 받든 결과가 같은데, 도감 한 칸이 걸리면 받는 쪽에 의미가 생긴다.
     */
    fun incompleteFamilies(p: ProgressState): Set<WeaponFamily> {
        val found = entriesOf(p)
        return WeaponCatalog.ENTRIES
            .filterNot { it in found }
            .mapNotNull { it.family }
            .toSet()
    }

    /** 도감에서 채워진 칸. 난이도를 지우고 칸만 센다. */
    fun entriesOf(p: ProgressState): Set<CodexEntry> =
        p.codex.map { CodexEntry(it.family, it.level) }.toSet()

    /**
     * 티어 시절 도감 기록을 칸 단위로 옮긴다.
     *
     * 옛 기록에는 정확한 단계가 남아 있지 않으므로 티어의 **첫 단계**로 옮긴다.
     * 채운 칸 수는 그대로 살고 분모만 커진다.
     *
     * 다만 무한 구간 티어(+21 위)는 계열마다 있던 것이 전설 칸 하나로 모인다 —
     * 그 구간은 원래 모든 계열이 같은 그림을 쓰므로 칸을 계열마다 둘 이유가 없다.
     */
    fun migrateCodex(p: ProgressState): ProgressState {
        if (p.codex.none { it.level == CodexKey.LEGACY_LEVEL }) return p
        val moved = p.codex.mapNotNull { key ->
            if (key.level != CodexKey.LEGACY_LEVEL) return@mapNotNull key
            val family = key.family ?: return@mapNotNull null
            val tier = key.tier ?: return@mapNotNull null
            val slot = WeaponCatalog.slotFor(family, tier.minLevel)
            CodexKey(slot.family, slot.level, key.difficulty)
        }
        return p.copy(codex = moved.toSet())
    }

    /** 검을 손에 넣었을 때 도감에 등록한다. 구매·조합·강화 성공 모두 여기를 지난다. */
    fun registerSword(p: ProgressState, difficulty: Difficulty, sword: Sword): ProgressState {
        val slot = WeaponCatalog.slotFor(sword.family, sword.level)
        val key = CodexKey(slot.family, slot.level, difficulty)
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

        // 도감은 여기서 채우지 않는다. 강화에 성공했다고 저절로 오르면 도감이
        // "지나간 자취"일 뿐 아무 결정도 요구하지 않는다. [CodexOffer] 로 바쳐야 열린다.
        return p.copy(stats = stats)
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
     *
     * 도감 기록의 이관도 여기서 한다 — 불러오기가 반드시 지나는 길이라 옛 세이브가
     * 티어 시절 기록을 그대로 들고 화면까지 가는 일이 없다.
     */
    fun refresh(state: ProgressState): ProgressState {
        val p = migrateCodex(state)
        val s = p.stats
        val distinctEntries = entriesOf(p).size
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

            // --- 사냥 ---
            if (s.monsterKills >= 100) add(Achievement.HUNT_100)
            if (s.monsterKills >= 1_000) add(Achievement.HUNT_1000)
            if (s.monsterKills >= 10_000) add(Achievement.HUNT_10000)
            if (s.bossKills >= 1) add(Achievement.BOSS_FIRST)
            if (p.clearedZones.size >= Zone.entries.size) add(Achievement.ZONES_ALL)
            if (s.eventsSeen >= 50) add(Achievement.EVENT_50)

            // --- 조합 ---
            if (s.fusions >= 10) add(Achievement.FUSE_10)
            if (s.fusions >= 100) add(Achievement.FUSE_100)
            if (p.uniqueFound.isNotEmpty()) add(Achievement.UNIQUE_FIRST)
            if (p.uniqueFound.size >= 5) add(Achievement.UNIQUE_5)
            if (p.uniqueFound.size >= UniqueSwords.RECIPES.size) add(Achievement.UNIQUE_ALL)

            // --- 특수강화·재료 ---
            if (s.maxStars >= 3) add(Achievement.STAR_3)
            if (s.maxStars >= StarForce.MAX_STARS) add(Achievement.STAR_5)
            if (s.stonesEarned >= 500) add(Achievement.STONE_500)
            if (s.skillsTriggered >= 100) add(Achievement.SKILL_100)

            // --- 회랑·수집 ---
            if (s.gauntletBestEver >= 10) add(Achievement.GAUNTLET_10)
            if (s.gauntletBestEver >= 25) add(Achievement.GAUNTLET_25)
            if (s.gauntletBestEver >= 50) add(Achievement.GAUNTLET_50)
            if (p.petsFound.isNotEmpty()) add(Achievement.PET_FIRST)
            if (p.petsFound.size >= PetKind.entries.size) add(Achievement.PET_ALL)
        }
        return if (earned == p.achievements) p else p.copy(achievements = earned)
    }

    /**
     * 상점·드롭에 나올 수 있는 계열들. 항상 enum 선언 순서를 유지한다.
     *
     * 기본 4계열 중 조건을 채운 것만이다. 나머지 10계열은 조합·회랑 전용이라
     * 여기 절대 들어오지 않는다.
     */
    fun unlockedFamilies(p: ProgressState): List<WeaponFamily> =
        WeaponFamily.BASICS.filter { basicFamilyUnlocked(p, it) }

    /** 보스를 깬 구역을 기록한다. 대검 해금 조건이 이걸 센다. */
    fun onZoneCleared(p: ProgressState, zoneId: String): ProgressState =
        if (zoneId in p.clearedZones) p else p.copy(clearedZones = p.clearedZones + zoneId)

    /** 펫 알을 얻었다. 수집 기록은 전역에 남는다. */
    fun onPetFound(p: ProgressState, petId: String): ProgressState =
        if (petId in p.petsFound) p else p.copy(petsFound = p.petsFound + petId)

    /** 계열 스킬이 터졌다. */
    fun onSkill(p: ProgressState): ProgressState =
        p.copy(stats = p.stats.copy(skillsTriggered = p.stats.skillsTriggered + 1))

    /** 강화석을 얻었다. */
    fun onStones(p: ProgressState, count: Int): ProgressState =
        if (count <= 0) p else p.copy(stats = p.stats.copy(stonesEarned = p.stats.stonesEarned + count))

    /** 회랑 층을 깼다. 최고 기록만 남긴다. */
    fun onGauntletFloor(p: ProgressState, floor: Int): ProgressState =
        if (floor <= p.stats.gauntletBestEver) {
            p
        } else {
            p.copy(stats = p.stats.copy(gauntletBestEver = floor))
        }

    /** 별을 올렸다. 최고 별 수만 남긴다. */
    fun onStars(p: ProgressState, stars: Int): ProgressState =
        if (stars <= p.stats.maxStars) p else p.copy(stats = p.stats.copy(maxStars = stars))

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
