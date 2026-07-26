package com.geomgang.core

import kotlinx.serialization.Serializable
import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * 사냥터. 구역을 깨 나가며 앞으로 간다.
 *
 * [recommendedLevel] 은 이 구역을 편히 돌 수 있는 강화 단계다.
 * 보스는 제한 시간이 있어 공격력이 부족하면 시간 안에 죽지 않는다.
 * 그것이 "이 보스 잡으려면 +N 은 돼야 한다"는 목표를 만든다.
 */
enum class Zone(
    val id: String,
    val displayName: String,
    val recommendedLevel: Int,
    val monsterName: String,
    val monsterHp: Long,
    val monsterGold: Long,
    val monsterShards: Int,
    val bossName: String,
    val bossHp: Long,
    val bossSeconds: Int,
    val bossGold: Long,
    val bossShards: Int,
) {
    MEADOW(
        id = "meadow", displayName = "초원", recommendedLevel = 0,
        monsterName = "들개", monsterHp = 24, monsterGold = 14, monsterShards = 0,
        bossName = "들개 우두머리", bossHp = 420, bossSeconds = 20,
        bossGold = 900, bossShards = 6,
    ),
    CAVE(
        id = "cave", displayName = "동굴", recommendedLevel = 5,
        monsterName = "동굴 박쥐", monsterHp = 210, monsterGold = 120, monsterShards = 1,
        bossName = "굴의 파수꾼", bossHp = 4_200, bossSeconds = 20,
        bossGold = 7_500, bossShards = 14,
    ),
    VOLCANO(
        id = "volcano", displayName = "화산", recommendedLevel = 10,
        monsterName = "용암 도마뱀", monsterHp = 1_900, monsterGold = 1_400, monsterShards = 2,
        bossName = "화산의 군주", bossHp = 38_000, bossSeconds = 22,
        bossGold = 70_000, bossShards = 30,
    ),
    DRAGON_NEST(
        id = "dragon_nest", displayName = "용의 둥지", recommendedLevel = 15,
        monsterName = "새끼 용", monsterHp = 16_000, monsterGold = 12_000, monsterShards = 4,
        bossName = "늙은 흑룡", bossHp = 320_000, bossSeconds = 25,
        bossGold = 600_000, bossShards = 60,
    ),
    ;

    companion object {
        fun fromId(id: String): Zone =
            entries.firstOrNull { it.id == id } ?: MEADOW

        /** 구역 하나에서 보스가 나오기까지 잡아야 하는 잡몹 수. */
        const val MONSTERS_BEFORE_BOSS = 10
    }
}

/**
 * 계열이 싸우는 방식.
 *
 * 공격력은 강화 단계에서 나오고, 계열은 그 공격력을 **어떻게 쓰는지**를 정한다.
 * 그래서 계열을 모으는 이유가 외형 말고도 생긴다.
 *
 * @param damage        한 번 칠 때의 배수
 * @param hits          한 번 탭에 들어가는 타격 횟수
 * @param minTapMillis  다음 탭까지 기다려야 하는 시간. 짧으면 연타가 된다
 * @param comboGain     연속 탭마다 붙는 추가 배수
 * @param bossBonus     보스에게 주는 추가 배수
 * @param shardBonus    조각 획득 배수
 * @param burnRatio     탭하지 않는 동안 1초마다 들어가는 공격력 비율
 */
enum class FamilyStyle(
    val damage: Double,
    val hits: Int,
    val minTapMillis: Long,
    val comboGain: Double,
    val bossBonus: Double,
    val shardBonus: Double,
    val burnRatio: Double,
    val blurb: String,
) {
    BALANCED(1.0, 1, 150, 0.0, 1.0, 1.0, 0.0, "기준. 약점도 없다"),
    COMBO(0.9, 1, 140, 0.03, 1.0, 1.0, 0.0, "연속으로 칠수록 세진다"),
    HEAVY(2.1, 1, 380, 0.0, 1.0, 1.0, 0.0, "느리지만 한 방이 무겁다"),
    SWIFT(0.6, 1, 80, 0.0, 1.0, 1.0, 0.0, "가볍고 아주 빠르다"),
    DOUBLE(0.62, 2, 150, 0.0, 1.0, 1.0, 0.0, "한 번에 두 번 들어간다"),
    GREEDY(0.95, 1, 150, 0.0, 1.0, 1.6, 0.0, "조각을 더 많이 챙긴다"),
    SACRED(1.0, 1, 150, 0.0, 1.6, 1.0, 0.0, "보스에게 강하다"),
    BURNING(0.85, 1, 150, 0.0, 1.0, 1.0, 0.22, "화상을 입혀 계속 태운다"),
    ;

    companion object {
        fun of(family: WeaponFamily): FamilyStyle = when (family) {
            WeaponFamily.STRAIGHT -> BALANCED
            WeaponFamily.CURVED -> COMBO
            WeaponFamily.GREAT -> HEAVY
            WeaponFamily.RAPIER -> SWIFT
            WeaponFamily.TWIN -> DOUBLE
            WeaponFamily.DEMON -> GREEDY
            WeaponFamily.HOLY -> SACRED
            WeaponFamily.DRAGON -> BURNING
        }
    }
}

/** 한 번 탭한 결과. */
data class Hit(
    val damage: Long,
    /** 화면에 몇 번 튀어야 하는지. 쌍검은 2다. */
    val hits: Int,
)

/** 사냥 진행 상황. 모드별 세이브에 함께 저장된다. */
@Serializable
data class AdventureState(
    val zoneId: String = Zone.MEADOW.id,
    /** 지금 구역에서 잡은 잡몹 수. [Zone.MONSTERS_BEFORE_BOSS] 에 닿으면 보스가 나온다. */
    val killsInZone: Int = 0,
    val clearedZoneIds: Set<String> = emptySet(),
) {
    val zone: Zone get() = Zone.fromId(zoneId)

    val bossReady: Boolean get() = killsInZone >= Zone.MONSTERS_BEFORE_BOSS

    fun isCleared(zone: Zone): Boolean = zone.id in clearedZoneIds

    /** 들어갈 수 있는 구역. 앞 구역을 깨야 다음이 열린다. */
    fun isUnlocked(zone: Zone): Boolean {
        val index = Zone.entries.indexOf(zone)
        if (index == 0) return true
        return Zone.entries[index - 1].id in clearedZoneIds
    }
}

/**
 * 전투 계산.
 *
 * 전부 순수 함수다. 시간과 탭은 화면 쪽이 세고, 여기서는 "이만큼 치면 얼마가 들어가는가"만 답한다.
 */
object Combat {

    private const val POWER_BASE = 6.0
    private const val POWER_GROWTH = 1.5

    /** 연속 타격으로 얻을 수 있는 최대 추가 배수. */
    const val MAX_COMBO_BONUS = 0.6

    /** 검이 없으면 사냥할 수 없다. */
    fun attackPower(sword: Sword?): Long {
        if (sword == null) return 0
        val base = POWER_BASE * POWER_GROWTH.pow(sword.level.toDouble())
        return (base * FamilyStyle.of(sword.family).damage).roundToLong().coerceAtLeast(1)
    }

    /**
     * 한 번 탭했을 때 들어가는 피해.
     *
     * @param combo  지금까지 연속으로 몇 번 쳤는지
     * @param isBoss 보스를 치는 중인지
     */
    fun hit(sword: Sword?, combo: Int, isBoss: Boolean): Hit {
        if (sword == null) return Hit(0, 0)
        val style = FamilyStyle.of(sword.family)
        val comboBonus = (style.comboGain * combo).coerceAtMost(MAX_COMBO_BONUS)
        val bossBonus = if (isBoss) style.bossBonus else 1.0
        val perHit = attackPower(sword) * (1.0 + comboBonus) * bossBonus / style.hits
        return Hit(
            damage = perHit.roundToLong().coerceAtLeast(1) * style.hits,
            hits = style.hits,
        )
    }

    /** 탭하지 않는 동안 1초마다 들어가는 피해. 용검만 0이 아니다. */
    fun burnPerSecond(sword: Sword?): Long {
        if (sword == null) return 0
        val style = FamilyStyle.of(sword.family)
        if (style.burnRatio == 0.0) return 0
        return (attackPower(sword) * style.burnRatio).roundToLong().coerceAtLeast(1)
    }

    fun minTapMillis(sword: Sword?): Long =
        sword?.let { FamilyStyle.of(it.family).minTapMillis } ?: 150

    /** 잡몹을 잡았을 때 얻는 조각. 계열 보정이 붙는다. */
    fun shardReward(sword: Sword?, base: Int): Int {
        if (base == 0) return 0
        val mult = sword?.let { FamilyStyle.of(it.family).shardBonus } ?: 1.0
        return (base * mult).roundToLong().toInt().coerceAtLeast(1)
    }

    /**
     * 이 검으로 보스를 제한 시간 안에 잡을 수 있는지 어림한다.
     *
     * 초당 [tapsPerSecond] 번 친다고 가정한다. 화면에 "지금 실력으로는 무리"를
     * 미리 알려 주기 위한 값이며, 실제 판정은 탭한 결과로 한다.
     */
    fun canBeatBoss(sword: Sword?, zone: Zone, tapsPerSecond: Double = 4.0): Boolean {
        if (sword == null) return false
        val style = FamilyStyle.of(sword.family)
        val tapsAvailable = (zone.bossSeconds * tapsPerSecond)
            .coerceAtMost(zone.bossSeconds * 1000.0 / style.minTapMillis)
        val perTap = hit(sword, combo = Int.MAX_VALUE / 2, isBoss = true).damage
        val burn = burnPerSecond(sword) * zone.bossSeconds
        return tapsAvailable * perTap + burn >= zone.bossHp
    }
}
