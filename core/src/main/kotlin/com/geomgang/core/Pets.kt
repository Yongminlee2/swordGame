package com.geomgang.core

import kotlinx.serialization.Serializable

/**
 * 펫이 무엇을 도와주는지.
 *
 * 종류마다 `when` 가지를 늘리면 펫 하나를 더할 때마다 열한 군데를 함께 고쳐야 한다.
 * 효과를 **데이터로** 들고 있으면 펫 표에 한 줄만 늘리면 된다.
 */
enum class PetEffect {
    /** 틱마다 공격력의 이 비율만큼 자동 타격. */
    AUTO_TAP,
    GOLD,
    EVENT,
    SHARD,
    DROP,
    CRIT,

    /** 보스 제한 시간을 늘린다(초). 다른 효과와 달리 비율이 아니다. */
    BOSS_TIME,
    RARE,
    GAUNTLET,
    STONE,
    SKILL,
}

/**
 * 펫 한 종류. 구역마다 하나씩 있고, 그 구역 보스가 알을 떨어뜨린다.
 *
 * 후반 구역 펫은 앞 구역 펫과 **같은 종류의 도움을 더 세게** 준다.
 * 효과를 스물네 가지로 늘리면 수집이 아니라 암기가 된다.
 *
 * @param min 레벨 1에서의 효과 크기
 * @param max 레벨 [Pets.MAX_LEVEL] 에서의 효과 크기
 */
enum class PetKind(
    val id: String,
    val displayName: String,
    val zoneId: String,
    val blurb: String,
    val effect: PetEffect,
    val min: Double,
    val max: Double,
) {
    QUOKKA("quokka", "초원 쿼카", "meadow", "주인 대신 부지런히 두드린다", PetEffect.AUTO_TAP, 0.10, 0.30),
    SPRIGGAN("spriggan", "숲 요정", "forest", "골드를 조금씩 더 물어온다", PetEffect.GOLD, 0.06, 0.18),
    SPIDERLING("spiderling", "꼬마 거미", "cave", "신기한 일을 더 자주 몰고 온다", PetEffect.EVENT, 0.01, 0.03),
    GOLEMLING("golemling", "꼬마 골렘", "mine", "조각을 더 캐 온다", PetEffect.SHARD, 0.08, 0.24),
    BLINK_FROG("blink_frog", "도약 개구리", "swamp", "검이 더 잘 떨어진다", PetEffect.DROP, 0.2, 0.6),
    LAVA_SNAKE("lava_snake", "용암 뱀", "volcano", "급소를 무는 법을 안다", PetEffect.CRIT, 0.02, 0.06),
    POLAR_CUB("polar_cub", "꼬마 백곰", "snowfield", "보스 앞에서 시간을 벌어 준다", PetEffect.BOSS_TIME, 1.0, 3.0),
    DRAKELING("drakeling", "아기 용", "dragon_nest", "작지만 화력이 진짜다", PetEffect.AUTO_TAP, 0.16, 0.40),
    SHADOW_IMP("shadow_imp", "그림자 임프", "abyss", "희귀한 것을 끌어들인다", PetEffect.RARE, 0.02, 0.06),
    HALL_WISP("hall_wisp", "회랑의 정령", "endless_hall", "회랑의 보상을 늘린다", PetEffect.GAUNTLET, 0.10, 0.30),
    SKY_HAWK("sky_hawk", "천공 매", "sky_gallery", "강화석을 더 잘 찾아낸다", PetEffect.STONE, 0.02, 0.06),
    CAPITAL_WRAITH(
        "capital_wraith", "왕도의 유령", "ruined_capital", "검의 스킬을 자주 끌어낸다",
        PetEffect.SKILL, 0.02, 0.06,
    ),

    // --- v1.6 후반 12구역의 펫 ---
    TEMPLE_FIREFLY(
        "temple_firefly", "사원 반딧불", "silent_temple", "어둠 속에서 금붙이를 찾아낸다",
        PetEffect.GOLD, 0.14, 0.38,
    ),
    GLASS_MOTH(
        "glass_moth", "유리 나비", "glass_desert", "떨어진 검을 물고 온다",
        PetEffect.DROP, 0.4, 1.1,
    ),
    SKY_SEED(
        "sky_seed", "하늘 씨앗", "floating_isle", "바람을 타고 소식을 물어온다",
        PetEffect.EVENT, 0.02, 0.05,
    ),
    WOOD_IMP(
        "wood_imp", "숲 장난꾼", "warped_wood", "부서진 것에서 쓸 것을 골라낸다",
        PetEffect.SHARD, 0.16, 0.44,
    ),
    TIDE_FISH(
        "tide_fish", "물결 물고기", "sunken_city", "가라앉은 돌을 주워 온다",
        PetEffect.STONE, 0.04, 0.11,
    ),
    ASH_NEWT(
        "ash_newt", "잿불 도롱뇽", "ash_plain", "약한 곳을 정확히 문다",
        PetEffect.CRIT, 0.04, 0.11,
    ),
    STARLING(
        "starling", "별똥별", "star_tomb", "귀한 것 곁에 내려앉는다",
        PetEffect.RARE, 0.04, 0.11,
    ),
    TIME_SHARD(
        "time_shard", "시간 조각", "time_rift", "보스 앞에서 시계를 늦춘다",
        PetEffect.BOSS_TIME, 2.0, 5.0,
    ),
    BLOOD_BAT(
        "blood_bat", "피의 박쥐", "blood_keep", "주인보다 빠르게 달려든다",
        PetEffect.AUTO_TAP, 0.24, 0.55,
    ),
    FROST_SHEEP(
        "frost_sheep", "서리 양", "frost_heart", "회랑에서 더 많이 챙겨 나온다",
        PetEffect.GAUNTLET, 0.18, 0.48,
    ),
    FORGE_EMBER(
        "forge_ember", "대장간 불씨", "first_forge", "검이 제 힘을 자주 떠올리게 한다",
        PetEffect.SKILL, 0.04, 0.11,
    ),
    GATE_SHADE(
        "gate_shade", "문의 그림자", "final_gate", "문 너머의 것을 대신 두드린다",
        PetEffect.AUTO_TAP, 0.32, 0.70,
    ),
    ;

    companion object {
        fun byId(id: String): PetKind? = entries.firstOrNull { it.id == id }

        fun byZone(zoneId: String): PetKind? = entries.firstOrNull { it.zoneId == zoneId }
    }
}

/**
 * 펫 보유 상태.
 *
 * [counts] 는 펫별로 모은 알의 수다. 알 수가 곧 레벨(상한 [Pets.MAX_LEVEL])이라
 * 중복 알이 아깝지 않다. 장착은 한 마리뿐 - 여러 마리 효과가 겹치면 수치가 풀린다.
 */
@Serializable
data class PetState(
    val counts: Map<String, Int> = emptyMap(),
    val equippedId: String? = null,
)

/**
 * 펫 효과 수식.
 *
 * 전부 **장착한 펫** 기준이고, 없으면 중립값이다. 레벨(1~5)에 비례해 세진다.
 */
object Pets {

    const val MAX_LEVEL = 5

    /** 보스가 알을 떨어뜨릴 확률. */
    const val EGG_DROP_CHANCE = 0.05

    /** 수상한 알(이벤트)에서 진짜 알이 나올 확률. */
    const val EGG_EVENT_CHANCE = 0.20

    fun levelOf(state: PetState, id: String): Int =
        (state.counts[id] ?: 0).coerceAtMost(MAX_LEVEL)

    fun owns(state: PetState, id: String): Boolean = (state.counts[id] ?: 0) > 0

    fun addEgg(state: PetState, id: String): PetState =
        state.copy(counts = state.counts + (id to (state.counts[id] ?: 0) + 1))

    /** 장착. 소유한 펫만, null = 해제. */
    fun equip(state: PetState, id: String?): PetState {
        if (id != null && !owns(state, id)) return state
        return state.copy(equippedId = id)
    }

    private fun equipped(state: PetState): Pair<PetKind, Int>? {
        val id = state.equippedId ?: return null
        val kind = PetKind.byId(id) ?: return null
        val level = levelOf(state, id)
        if (level <= 0) return null
        return kind to level
    }

    /** 레벨 1~5를 [min]~[max] 사이 값으로 편다. */
    private fun scaled(level: Int, min: Double, max: Double): Double =
        min + (max - min) * (level - 1) / (MAX_LEVEL - 1).toDouble()

    /**
     * 장착한 펫이 [effect] 를 가졌다면 그 크기, 아니면 0.
     *
     * 모든 효과 함수가 이 한 줄을 통과한다 — 펫이 늘어도 고칠 곳은 표뿐이다.
     */
    private fun amountOf(state: PetState, effect: PetEffect): Double {
        val (kind, level) = equipped(state) ?: return 0.0
        if (kind.effect != effect) return 0.0
        return scaled(level, kind.min, kind.max)
    }

    /** 틱마다 공격력의 이 비율만큼 자동 타격. */
    fun autoTapRatio(state: PetState): Double = amountOf(state, PetEffect.AUTO_TAP)

    fun goldMultOf(state: PetState): Double = 1.0 + amountOf(state, PetEffect.GOLD)

    fun eventBonusOf(state: PetState): Double = amountOf(state, PetEffect.EVENT)

    fun shardMultOf(state: PetState): Double = 1.0 + amountOf(state, PetEffect.SHARD)

    fun dropMultOf(state: PetState): Double = 1.0 + amountOf(state, PetEffect.DROP)

    fun critBonusOf(state: PetState): Double = amountOf(state, PetEffect.CRIT)

    fun bossTimeBonusMillis(state: PetState): Long =
        (amountOf(state, PetEffect.BOSS_TIME) * 1000).toLong()

    fun rareBonusOf(state: PetState): Double = amountOf(state, PetEffect.RARE)

    /** 회랑 보상 배수. M15 회랑이 쓴다. */
    fun gauntletMultOf(state: PetState): Double = 1.0 + amountOf(state, PetEffect.GAUNTLET)

    /** 잡몹이 강화석을 떨어뜨릴 확률에 더해지는 값(%p). */
    fun stoneBonusOf(state: PetState): Double = amountOf(state, PetEffect.STONE)

    /** 계열 스킬 발동 확률에 더해지는 값(%p). */
    fun skillBonusOf(state: PetState): Double = amountOf(state, PetEffect.SKILL)

    /** 화면용 효과 설명 한 줄. */
    fun effectLine(kind: PetKind, level: Int): String {
        val lv = level.coerceIn(1, MAX_LEVEL)
        val value = scaled(lv, kind.min, kind.max)
        val percent = (value * 100).toInt()
        return when (kind.effect) {
            PetEffect.AUTO_TAP -> "자동 타격 공격력의 $percent%"
            PetEffect.GOLD -> "골드 +$percent%"
            PetEffect.EVENT -> "이벤트 확률 +$percent%p"
            PetEffect.SHARD -> "조각 +$percent%"
            PetEffect.DROP -> "검 드롭률 +$percent%"
            PetEffect.CRIT -> "치명타 +$percent%p"
            PetEffect.BOSS_TIME -> "보스 시간 +${value.toInt()}초"
            PetEffect.RARE -> "희귀 몬스터 +$percent%p"
            PetEffect.GAUNTLET -> "회랑 보상 +$percent%"
            PetEffect.STONE -> "강화석 확률 +$percent%p"
            PetEffect.SKILL -> "스킬 확률 +$percent%p"
        }
    }
}
