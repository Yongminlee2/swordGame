package com.geomgang.core

/** 강화 한 번에 드는 것. */
data class ForgeRequirement(
    val gold: Long,
    /** 필수로 태워야 하는 보관함 검 자루 수. */
    val swords: Int,
    /** 필수 강화석. */
    val stones: Int,
)

/**
 * 강화 비용의 단일 출처.
 *
 * 초반은 골드만으로 굴러가고, 고단계로 갈수록 **재료가 화폐가 된다.**
 * 골드는 사냥으로 무한히 벌 수 있어서 그것만으로는 고단계가 "돈 쌓기"의 반복이 된다.
 * 재료를 필수로 만들면 사냥·보관함·분해가 강화의 전제 조건이 된다.
 *
 * 요구량은 **목표 단계**(현재 단계 + 1)를 기준으로 읽는다.
 */
object ForgeCost {

    /** 이 목표 단계부터 검이 필수다. */
    const val SWORD_BAND_START = 13

    /** 이 목표 단계부터 강화석도 필수다. */
    const val STONE_BAND_START = 16

    /** 무한 구간이 시작되는 목표 단계. */
    const val ENDLESS_BAND_START = 21

    /** 강화석 요구의 상한. 없으면 무한 구간에서 요구가 발산한다. */
    const val MAX_STONES = 40

    fun requirementFor(currentLevel: Int): ForgeRequirement {
        require(currentLevel >= 0) { "currentLevel must be >= 0, was $currentLevel" }
        val target = currentLevel + 1
        val swords = when {
            target < SWORD_BAND_START -> 0
            target < STONE_BAND_START -> 1
            target < ENDLESS_BAND_START -> 2
            else -> 3
        }
        val stones = when {
            target < STONE_BAND_START -> 0
            target < ENDLESS_BAND_START -> 3 + (target - STONE_BAND_START)
            else -> (8 + (target - 20)).coerceAtMost(MAX_STONES)
        }
        return ForgeRequirement(
            gold = Economy.upgradeCost(currentLevel),
            swords = swords,
            stones = stones,
        )
    }

    /**
     * 지금 상태로 요구를 낼 수 있는지.
     *
     * @param extraSwords 성공률 보너스로 더 태울 검. 필수분 위에 얹힌다.
     */
    fun canPay(state: GameState, extraSwords: Int = 0): Boolean {
        val sword = state.sword ?: return false
        val req = requirementFor(sword.level)
        if (state.gold < req.gold) return false
        if (state.storage.size < req.swords + extraSwords) return false
        if (state.forgeStones < req.stones) return false
        return true
    }

    /** 못 내는 이유 한 줄. 낼 수 있으면 null. 화면이 그대로 띄운다. */
    fun missingText(state: GameState): String? {
        val sword = state.sword ?: return "검이 없다"
        val req = requirementFor(sword.level)
        if (state.gold < req.gold) return "골드가 모자라다"
        if (state.storage.size < req.swords) {
            return "재료 검 ${req.swords}자루가 필요하다 (보관함 ${state.storage.size})"
        }
        if (state.forgeStones < req.stones) {
            return "강화석 ${req.stones}개가 필요하다 (보유 ${state.forgeStones})"
        }
        return null
    }
}
