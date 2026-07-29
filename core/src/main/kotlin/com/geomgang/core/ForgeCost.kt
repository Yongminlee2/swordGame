package com.geomgang.core

/** 강화 한 번에 드는 것. */
data class ForgeRequirement(
    val gold: Long,
    /** 필수 강화석. */
    val stones: Int,
)

/**
 * 강화 비용의 단일 출처.
 *
 * 초반은 골드만으로 굴러가고, 고단계로 갈수록 **강화석이 화폐가 된다.**
 * 골드는 사냥으로 무한히 벌 수 있어서 그것만으로는 고단계가 "돈 쌓기"의 반복이 된다.
 *
 * **재료 검은 v1.8에서 뺐다.** 검을 태우는 자리가 강화와 조합 두 군데였는데,
 * 그 둘이 같은 보관함을 놓고 다퉈서 "조합하려고 모으면 강화를 못 하고, 강화하려고
 * 태우면 조합할 게 없는" 상태가 됐다. 이제 **보관함의 검은 조합에만** 쓴다.
 *
 * 요구량은 **목표 단계**(현재 단계 + 1)를 기준으로 읽는다.
 */
object ForgeCost {

    /** 이 목표 단계부터 강화석이 필수다. */
    const val STONE_BAND_START = 16

    /** 무한 구간이 시작되는 목표 단계. */
    const val ENDLESS_BAND_START = 21

    /**
     * 강화석 요구의 상한.
     *
     * v1.6까지 40이었는데, 그 탓에 +38 한 번에 26개가 들어 **보스 하나에 강화 한 번**이 됐다.
     * 준비가 강화보다 오래 걸리면 이 게임은 사냥 게임이 된다. [ForgeTempoTest] 가 지킨다.
     */
    const val MAX_STONES = 15

    /** 구역 완주 한 번으로 굴릴 수 있어야 하는 최소 강화 횟수. */
    const val RUNS_PER_ZONE_CLEAR = 5.0

    /** 보스 하나가 감당하는 강화 횟수. 나머지는 잡몹 드롭이 채운다. */
    const val BOSS_STONE_RUNS = 5

    /** 강화석을 아직 안 먹는 저단계 구역의 보스가 주는 양. 미리 모아 두라는 뜻이다. */
    const val EARLY_BOSS_STONES = 3

    /**
     * 그 구역 보스가 주는 강화석.
     *
     * 손으로 적지 않는다 — 요구 곡선을 한 번이라도 고치면 24개 숫자가 전부 어긋나고,
     * 어긋난 채로 몇 판이 지나가면 다시 "보스 하나에 강화 한 번" 이 된다.
     */
    fun bossStonesFor(recommendedLevel: Int): Int {
        val need = requirementFor(recommendedLevel).stones
        return if (need == 0) EARLY_BOSS_STONES else need * BOSS_STONE_RUNS
    }

    /** 잡몹이 강화석을 떨어뜨릴 확률. 재료 공급의 절반이 여기서 나온다. */
    const val MOB_STONE_CHANCE = 0.15

    /** @param relief 계열 특성이 깎아 주는 강화석 수. 창검이 쓴다 */
    fun requirementFor(currentLevel: Int, relief: Int = 0): ForgeRequirement {
        require(currentLevel >= 0) { "currentLevel must be >= 0, was $currentLevel" }
        val target = currentLevel + 1
        // 증가 속도를 v1.6의 절반으로 낮췄다. 요구가 단계마다 1씩 오르면
        // 후반에는 강화 한 번을 위해 구역을 두세 번 돌아야 한다.
        val stones = when {
            target < STONE_BAND_START -> 0
            target < ENDLESS_BAND_START -> 3 + (target - STONE_BAND_START) / 2
            else -> (5 + (target - 20) / 2).coerceAtMost(MAX_STONES)
        }
        return ForgeRequirement(
            gold = Economy.upgradeCost(currentLevel),
            stones = (stones - relief).coerceAtLeast(0),
        )
    }

    /** 지금 상태로 요구를 낼 수 있는지. */
    fun canPay(state: GameState): Boolean {
        val sword = state.sword ?: return false
        val req = requirementFor(sword.level)
        if (state.gold < req.gold) return false
        if (state.forgeStones < req.stones) return false
        return true
    }

    /** 못 내는 이유 한 줄. 낼 수 있으면 null. 화면이 그대로 띄운다. */
    fun missingText(state: GameState): String? {
        val sword = state.sword ?: return "검이 없다"
        val req = requirementFor(sword.level)
        if (state.gold < req.gold) return "골드가 모자라다"
        if (state.forgeStones < req.stones) {
            return "강화석 ${req.stones}개가 필요하다 (보유 ${state.forgeStones})"
        }
        return null
    }
}
