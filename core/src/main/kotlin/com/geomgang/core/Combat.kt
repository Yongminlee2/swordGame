package com.geomgang.core

import kotlinx.serialization.Serializable
import kotlin.math.pow
import kotlin.math.roundToLong


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
    /** 적 최대체력에 비례해 추가되는 피해 비율. 허검만 0이 아니다. */
    val maxHpRatio: Double = 0.0,
) {
    BALANCED(1.0, 1, 150, 0.0, 1.0, 1.0, 0.0, "기준. 약점도 없다"),
    COMBO(0.9, 1, 140, 0.03, 1.0, 1.0, 0.0, "연속으로 칠수록 세진다"),
    HEAVY(2.1, 1, 380, 0.0, 1.0, 1.0, 0.0, "느리지만 한 방이 무겁다"),
    SWIFT(0.6, 1, 80, 0.0, 1.0, 1.0, 0.0, "가볍고 아주 빠르다"),
    DOUBLE(0.62, 2, 150, 0.0, 1.0, 1.0, 0.0, "한 번에 두 번 들어간다"),
    GREEDY(0.95, 1, 150, 0.0, 1.0, 1.6, 0.0, "조각을 더 많이 챙긴다"),
    SACRED(1.0, 1, 150, 0.0, 1.6, 1.0, 0.0, "보스에게 강하다"),
    BURNING(0.85, 1, 150, 0.0, 1.0, 1.0, 0.22, "화상을 입혀 계속 태운다"),
    REAPING(1.35, 1, 240, 0.02, 1.0, 1.2, 0.0, "크게 베고 조각도 챙긴다"),
    CLEAVING(2.6, 1, 520, 0.0, 1.0, 1.0, 0.0, "아주 느리지만 한 방이 압도적이다"),
    PIERCING(0.78, 3, 190, 0.0, 1.15, 1.0, 0.0, "한 번에 세 번 찌른다"),
    ELEMENTAL(0.7, 1, 130, 0.04, 1.0, 1.0, 0.12, "연속타격과 화상을 함께 쓴다"),
    OMNI(0.9, 1, 150, 0.02, 1.25, 1.25, 0.0, "여러 계열의 힘을 조금씩 전부 가졌다"),
    HOLLOW(
        0.5, 1, 150, 0.0, 1.0, 1.0, 0.0, "한 방은 가볍지만 거대한 것일수록 깊이 벤다",
        maxHpRatio = 0.015,
    ),
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
            // v2.0: 이 게임의 유일한 검([WeaponFamily.ONLY]). 기준값을 쓴다 —
            // 계열이 하나뿐이면 특성도 하나여야 하고, 화상 같은 개성은 단계 스킬로 옮긴다.
            WeaponFamily.DRAGON -> BALANCED
            WeaponFamily.SCYTHE -> REAPING
            WeaponFamily.AXE -> CLEAVING
            WeaponFamily.SPEAR -> PIERCING
            WeaponFamily.SPIRIT -> ELEMENTAL
            WeaponFamily.FUSED -> OMNI
            WeaponFamily.VOID -> HOLLOW
        }
    }
}

/** 한 번 탭한 결과. */
data class Hit(
    val damage: Long,
    /** 화면에 몇 번 튀어야 하는지. 쌍검은 2다. */
    val hits: Int,
    /** 치명타였는지. 화면이 크고 노랗게 띄운다. */
    val crit: Boolean = false,
    /** 이 탭에 터진 계열 스킬. 없으면 null. 화면이 이름을 크게 띄운다. */
    val skill: Skill? = null,
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

    /** 치명타 확률과 배수. 판정은 [hit] 에 난수 값을 넣어서 한다. */
    const val CRIT_CHANCE = 0.05
    const val CRIT_MULTIPLIER = 1.8

    /**
     * 검이 없으면 사냥할 수 없다.
     *
     * 강화 단계가 기본값을 정하고, 계열이 배수를, 별이 다시 배수를 곱한다.
     */
    fun attackPower(sword: Sword?): Long {
        if (sword == null) return 0
        val base = POWER_BASE * POWER_GROWTH.pow(sword.level.toDouble())
        val withFamily = base * FamilyStyle.of(sword.family).damage
        return (withFamily * StarForce.attackMultiplier(sword)).roundToLong().coerceAtLeast(1)
    }

    /**
     * 한 번 탭했을 때 들어가는 피해.
     *
     * @param combo       지금까지 연속으로 몇 번 쳤는지
     * @param isBoss      보스를 치는 중인지
     * @param critRoll    치명타 판정용 난수(0~1). 기본 1.0 = 치명타 없음.
     *                    난수 대신 값을 받는 것은 테스트를 결정적으로 만들기 위해서다.
     * @param targetMaxHp 대상의 최대체력. 허검(최대체력 비례 피해)만 쓴다.
     */
    fun hit(
        sword: Sword?,
        combo: Int,
        isBoss: Boolean,
        critRoll: Double = 1.0,
        targetMaxHp: Long = 0,
        skillRoll: Double = 1.0,
    ): Hit {
        if (sword == null) return Hit(0, 0)
        val style = FamilyStyle.of(sword.family)
        val comboBonus = (style.comboGain * combo).coerceAtMost(MAX_COMBO_BONUS)
        val bossBonus =
            if (isBoss) style.bossBonus * UniqueSwords.bossBonusOf(sword) else 1.0
        val crit = critRoll < CRIT_CHANCE + UniqueSwords.critBonusOf(sword)
        val maxHpBonus = targetMaxHp * (style.maxHpRatio + UniqueSwords.maxHpRatioOf(sword))
        val plain = attackPower(sword) * (1.0 + comboBonus) * bossBonus + maxHpBonus

        // 스킬은 계열 특성 위에 곱해진다. 치명타와도 겹친다 - 둘이 함께 터지면
        // 그 판이 뒤집히고, 5초 보스전에서 그 순간이 승패를 가른다.
        val skill = Skills.roll(sword, skillRoll)
        val withSkill = if (skill == null) {
            plain
        } else {
            val skillBoss = if (isBoss) skill.bossMult else 1.0
            plain * skill.damageMult * skillBoss + targetMaxHp * skill.maxHpRatio
        }

        val hits = skill?.hits ?: style.hits
        val perHit = withSkill / hits
        // 치명타 배수는 합산 피해에 곱한다 - 타격 수(쌍검 2연타 등)는 그대로다.
        val base = perHit.roundToLong().coerceAtLeast(1) * hits
        return Hit(
            damage = if (crit) (base * CRIT_MULTIPLIER).roundToLong() else base,
            hits = hits,
            crit = crit,
            skill = skill,
        )
    }

    /** 탭하지 않는 동안 1초마다 들어가는 피해. 용검 계열만 0이 아니다. */
    fun burnPerSecond(sword: Sword?): Long {
        if (sword == null) return 0
        val style = FamilyStyle.of(sword.family)
        if (style.burnRatio == 0.0) return 0
        return (attackPower(sword) * style.burnRatio * UniqueSwords.burnMultOf(sword))
            .roundToLong().coerceAtLeast(1)
    }

    fun minTapMillis(sword: Sword?): Long =
        sword?.let {
            (FamilyStyle.of(it.family).minTapMillis * UniqueSwords.tapIntervalMultOf(it))
                .roundToLong()
        } ?: 150

    /** 잡몹을 잡았을 때 얻는 조각. 계열·고유검 보정이 붙는다. */
    fun shardReward(sword: Sword?, base: Int): Int {
        if (base == 0) return 0
        val mult = sword?.let {
            FamilyStyle.of(it.family).shardBonus * UniqueSwords.shardMultOf(it)
        } ?: 1.0
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
