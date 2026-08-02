package com.geomgang.core

/**
 * 게임이 두 국면으로 나뉘는 자리 — **깊은 국면의 단일 출처.**
 *
 * v2.2 전에는 처음부터 골드·조각·강화석 세 화폐와 사냥터가 한꺼번에 열려 있었다.
 * 그래서 이 게임의 심장인 **"강화해서 팔고, 그 돈으로 또 강화한다"** 가 묻혔다 —
 * 사냥이 골드를 벌어다 주니 검을 팔 이유가 없고, 강화석이 모자라면 강화 자체가
 * 막혀서 강화대 앞을 떠나야 했다.
 *
 * 그래서 용검(전설)을 손에 쥐기 전까지는 **골드 하나로만** 논다.
 * 강화 → 판매 → 강화. 그 고리가 손에 붙은 뒤에 나머지가 열린다.
 *
 * 판별은 최고 기록 하나로 한다. 계열은 +20 이 끝이므로 **+21 을 밟았다는 것은
 * 용검을 벼렸다는 뜻**이고, 새 저장 필드가 필요 없어 옛 세이브도 그대로 읽힌다.
 */
object Unlocks {

    /** 용검을 한 번이라도 손에 쥐었는지. */
    fun legendReached(bestLevel: Int): Boolean = bestLevel >= LegendForge.LEVEL

    fun legendReached(state: GameState): Boolean = legendReached(state.bestLevel)

    /** 사냥터·회랑이 열렸는지. 초반에는 강화대 앞을 떠날 이유가 없어야 한다. */
    fun huntOpen(state: GameState): Boolean = legendReached(state)

    /** 강화석이 강화 요구에 등장하는지. 초반 강화는 골드만 먹는다. */
    fun stonesUsed(state: GameState): Boolean = legendReached(state)

    /** 조각이 화폐로 도는지. 초반에는 파편을 주워도 골드가 나온다. */
    fun shardsUsed(state: GameState): Boolean = legendReached(state)

    /**
     * 초반 줍기가 주는 골드.
     *
     * 조각 한 개를 같은 단계 검 판매가의 이 비율로 환산한다. 조각으로 받던 값어치를
     * 골드로 옮기되, 그냥 파는 것보다는 못하게 둔다 — 파괴는 여전히 손해여야 한다.
     */
    const val SALVAGE_GOLD_RATIO: Double = 0.35

    /** 초반 줍기 보상. [ForgeEngine.applySalvage] 가 조각 대신 이것을 준다. */
    fun salvageGold(level: Int): Long =
        (Economy.sellPrice(level) * SALVAGE_GOLD_RATIO).toLong().coerceAtLeast(1)
}
