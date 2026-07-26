package com.geomgang.core

import kotlinx.serialization.Serializable

/**
 * 펫 한 종류. 구역마다 하나씩 있고, 그 구역 보스가 알을 떨어뜨린다.
 */
enum class PetKind(
    val id: String,
    val displayName: String,
    val zoneId: String,
    val blurb: String,
) {
    QUOKKA("quokka", "초원 쿼카", "meadow", "주인 대신 부지런히 두드린다"),
    SPRIGGAN("spriggan", "숲 요정", "forest", "골드를 조금씩 더 물어온다"),
    SPIDERLING("spiderling", "꼬마 거미", "cave", "신기한 일을 더 자주 몰고 온다"),
    GOLEMLING("golemling", "꼬마 골렘", "mine", "조각을 더 캐 온다"),
    BLINK_FROG("blink_frog", "도약 개구리", "swamp", "검이 더 잘 떨어진다"),
    LAVA_SNAKE("lava_snake", "용암 뱀", "volcano", "급소를 무는 법을 안다"),
    POLAR_CUB("polar_cub", "꼬마 백곰", "snowfield", "보스 앞에서 시간을 벌어 준다"),
    DRAKELING("drakeling", "아기 용", "dragon_nest", "작지만 화력이 진짜다"),
    SHADOW_IMP("shadow_imp", "그림자 임프", "abyss", "희귀한 것을 끌어들인다"),
    HALL_WISP("hall_wisp", "회랑의 정령", "endless_hall", "회랑의 보상을 늘린다"),
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

    /** 틱마다 공격력의 이 비율만큼 자동 타격. 쿼카·아기 용. */
    fun autoTapRatio(state: PetState): Double {
        val (kind, level) = equipped(state) ?: return 0.0
        return when (kind) {
            PetKind.QUOKKA -> scaled(level, 0.10, 0.30)
            PetKind.DRAKELING -> scaled(level, 0.16, 0.40)
            else -> 0.0
        }
    }

    fun goldMultOf(state: PetState): Double {
        val (kind, level) = equipped(state) ?: return 1.0
        return if (kind == PetKind.SPRIGGAN) 1.0 + scaled(level, 0.06, 0.18) else 1.0
    }

    fun eventBonusOf(state: PetState): Double {
        val (kind, level) = equipped(state) ?: return 0.0
        return if (kind == PetKind.SPIDERLING) scaled(level, 0.01, 0.03) else 0.0
    }

    fun shardMultOf(state: PetState): Double {
        val (kind, level) = equipped(state) ?: return 1.0
        return if (kind == PetKind.GOLEMLING) 1.0 + scaled(level, 0.08, 0.24) else 1.0
    }

    fun dropMultOf(state: PetState): Double {
        val (kind, level) = equipped(state) ?: return 1.0
        return if (kind == PetKind.BLINK_FROG) 1.0 + scaled(level, 0.2, 0.6) else 1.0
    }

    fun critBonusOf(state: PetState): Double {
        val (kind, level) = equipped(state) ?: return 0.0
        return if (kind == PetKind.LAVA_SNAKE) scaled(level, 0.02, 0.06) else 0.0
    }

    fun bossTimeBonusMillis(state: PetState): Long {
        val (kind, level) = equipped(state) ?: return 0
        return if (kind == PetKind.POLAR_CUB) (scaled(level, 1.0, 3.0) * 1000).toLong() else 0
    }

    fun rareBonusOf(state: PetState): Double {
        val (kind, level) = equipped(state) ?: return 0.0
        return if (kind == PetKind.SHADOW_IMP) scaled(level, 0.02, 0.06) else 0.0
    }

    /** 회랑 보상 배수. M15 회랑이 쓴다. */
    fun gauntletMultOf(state: PetState): Double {
        val (kind, level) = equipped(state) ?: return 1.0
        return if (kind == PetKind.HALL_WISP) 1.0 + scaled(level, 0.10, 0.30) else 1.0
    }

    /** 화면용 효과 설명 한 줄. */
    fun effectLine(kind: PetKind, level: Int): String {
        val lv = level.coerceIn(1, MAX_LEVEL)
        return when (kind) {
            PetKind.QUOKKA -> "자동 타격 공격력의 ${(scaled(lv, 0.10, 0.30) * 100).toInt()}%"
            PetKind.SPRIGGAN -> "골드 +${(scaled(lv, 0.06, 0.18) * 100).toInt()}%"
            PetKind.SPIDERLING -> "이벤트 확률 +${(scaled(lv, 0.01, 0.03) * 100).toInt()}%p"
            PetKind.GOLEMLING -> "조각 +${(scaled(lv, 0.08, 0.24) * 100).toInt()}%"
            PetKind.BLINK_FROG -> "검 드롭률 +${(scaled(lv, 0.2, 0.6) * 100).toInt()}%"
            PetKind.LAVA_SNAKE -> "치명타 +${(scaled(lv, 0.02, 0.06) * 100).toInt()}%p"
            PetKind.POLAR_CUB -> "보스 시간 +${scaled(lv, 1.0, 3.0).toInt()}초"
            PetKind.DRAKELING -> "자동 타격 공격력의 ${(scaled(lv, 0.16, 0.40) * 100).toInt()}%"
            PetKind.SHADOW_IMP -> "희귀 몬스터 +${(scaled(lv, 0.02, 0.06) * 100).toInt()}%p"
            PetKind.HALL_WISP -> "회랑 보상 +${(scaled(lv, 0.10, 0.30) * 100).toInt()}%"
        }
    }
}
