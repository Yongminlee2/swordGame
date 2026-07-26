package com.geomgang.core

import kotlin.math.roundToLong

/**
 * 사냥 랜덤 이벤트.
 *
 * 사냥이 "탭 → 골드"의 반복으로 끝나지 않게 하는 변수들이다.
 * 여기에는 판정과 보상 수식만 있고, 시간·탭 상태는 화면 계층(ViewModel)이 센다.
 *
 * @param weight 등장 비중. 유성우만 극단적으로 낮다 - 잭팟은 드물어야 잭팟이다.
 */
enum class HuntEvent(val id: String, val displayName: String, val weight: Int) {
    TREASURE("treasure", "보물 몬스터", 12),
    GOLDEN_TIME("golden_time", "골든타임", 10),
    MIMIC("mimic", "미믹", 12),
    MERCHANT("merchant", "떠돌이 상인", 8),
    ELITE("elite", "분노한 정예", 12),
    GOLD_NUGGET("gold_nugget", "금덩이", 20),
    METEOR_SHOWER("meteor_shower", "유성우", 1),
    STRANGE_EGG("strange_egg", "수상한 알", 8),
}

object HuntEvents {

    /** 잡몹 스폰마다 이벤트가 붙을 확률. 희귀 판정과는 독립이다. */
    const val CHANCE = 0.08

    const val TREASURE_SECONDS = 5
    const val TREASURE_GOLD = 10.0

    const val GOLDEN_SECONDS = 30
    const val GOLDEN_MULT = 2.0

    const val MIMIC_HP = 2.0

    /** 미믹 드롭의 단계 보정. 드롭 확정에 더해 조금 더 좋은 검이 나온다. */
    const val MIMIC_DROP_BONUS = 1

    const val MERCHANT_SECONDS = 20
    const val MERCHANT_DISCOUNT = 0.3

    const val ELITE_HP = 3.0
    const val ELITE_REWARD = 5.0

    const val NUGGET_SECONDS = 3
    const val NUGGET_GOLD_FACTOR = 6.0

    const val METEOR_COUNT = 5

    /** 수상한 알 처치 시 조각. (M14에서 낮은 확률의 펫 알로 확장된다) */
    const val EGG_SHARDS = 30

    /**
     * 이벤트 발생 판정 + 종류 추첨.
     *
     * 난수 대신 값 두 개를 받는 것은 테스트를 결정적으로 만들기 위해서다.
     */
    fun roll(chanceRoll: Double, pickRoll: Double): HuntEvent? =
        if (chanceRoll < CHANCE) pick(pickRoll) else null

    /** 0~1 롤을 가중치 누적 구간으로 바꾼다. */
    fun pick(pickRoll: Double): HuntEvent {
        val total = HuntEvent.entries.sumOf { it.weight }
        var r = (pickRoll * total)
        for (e in HuntEvent.entries) {
            if (r < e.weight) return e
            r -= e.weight
        }
        return HuntEvent.entries.last()
    }

    /** 몬스터 자체를 바꾸는 이벤트인지. 나머지는 배너·버프·탭 대상이다. */
    fun isMonsterEvent(e: HuntEvent): Boolean = when (e) {
        HuntEvent.TREASURE, HuntEvent.MIMIC, HuntEvent.ELITE, HuntEvent.STRANGE_EGG -> true
        else -> false
    }

    fun hpMultOf(e: HuntEvent?): Double = when (e) {
        HuntEvent.MIMIC -> MIMIC_HP
        HuntEvent.ELITE -> ELITE_HP
        else -> 1.0
    }

    fun rewardMultOf(e: HuntEvent?): Double = when (e) {
        HuntEvent.TREASURE -> TREASURE_GOLD
        HuntEvent.ELITE -> ELITE_REWARD
        else -> 1.0
    }

    /** 금덩이 하나가 주는 골드. 구역 기준 보상에 비례한다. */
    fun nuggetGold(zone: Zone): Long =
        (zone.baseGold * NUGGET_GOLD_FACTOR).roundToLong().coerceAtLeast(1)
}
