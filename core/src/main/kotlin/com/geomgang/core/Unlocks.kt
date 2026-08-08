package com.geomgang.core

/**
 * 게임이 두 국면으로 나뉘는 자리 — **깊은 국면의 단일 출처.**
 *
 * v2.2 전에는 처음부터 골드·조각·강화석 세 화폐와 사냥터가 한꺼번에 열려 있었다.
 * 그래서 이 게임의 심장인 **"강화해서 팔고, 그 돈으로 또 강화한다"** 가 묻혔다 —
 * 사냥이 골드를 벌어다 주니 검을 팔 이유가 없고, 강화석이 모자라면 강화 자체가
 * 막혀서 강화대 앞을 떠나야 했다.
 *
 * 그래서 용검을 손에 쥐기 전까지는 **골드 하나로만** 논다.
 * 강화 → 판매 → 강화. 그 고리가 손에 붙은 뒤에 나머지가 열린다.
 *
 * 판별에 새 저장 필드는 없다 — 가진 검의 계열과 최고 기록만 보므로 옛 세이브도
 * 그대로 읽힌다([deepUnlocked]).
 */
object Unlocks {

    /** 전설(+21)을 밟았는지. */
    fun legendReached(bestLevel: Int): Boolean = bestLevel >= LegendForge.LEVEL

    fun legendReached(state: GameState): Boolean = legendReached(state.bestLevel)

    /**
     * 용검을 손에 넣었는지 — **조합을 해냈다는 증거.**
     *
     * 용검은 마검 +20 과 성검 +20 을 태워야만 나오므로([LegendForge]), 계열이
     * 용검이라는 것만으로 「기본 4계열을 끝까지 올려 두 번 조합했다」가 증명된다.
     * 손에 들었든 보관함에 넣었든 같다 - 사냥하려고 검을 바꿔 드는 일이 흔하다.
     */
    fun dragonOwned(state: GameState): Boolean =
        state.sword?.family == WeaponFamily.DRAGON ||
            state.storage.any { it.family == WeaponFamily.DRAGON }

    /**
     * 깊은 국면(시즌2)에 들어섰는지 — **경계는 여기 하나뿐이다.**
     *
     * **용검을 쥐는 순간 열린다**(v2.5). 전에는 +21(전설)이 조건이었는데, 용검이
     * +1 부터 시작하게 되면서 그 사이 스무 단계가 통째로 잠긴 구간이 됐다 —
     * 조합이라는 큰 산을 넘고도 보상이 스무 판 뒤에 오면 넘은 값을 못 느낀다.
     *
     * 사냥터만 따로 열지 않는 이유: 사냥은 강화석을 벌어다 주는데 강화석이 화면에
     * 없으면(지갑이 [deepUnlocked] 를 본다) 번 것이 어디로 갔는지 알 수 없다.
     * 경계가 둘이 되는 순간 반드시 이런 틈이 생긴다.
     *
     * 옛 세이브(용검을 팔았거나 도감에 바친 경우)도 [legendReached] 로 열린 채 남는다.
     */
    fun deepUnlocked(state: GameState): Boolean = legendReached(state) || dragonOwned(state)

    /** 사냥터·회랑이 열렸는지. 초반에는 강화대 앞을 떠날 이유가 없어야 한다. */
    fun huntOpen(state: GameState): Boolean = deepUnlocked(state)

    /** 강화석이 강화 요구에 등장하는지. 초반 강화는 골드만 먹는다. */
    fun stonesUsed(state: GameState): Boolean = deepUnlocked(state)

    // 조각은 시즌을 가리지 않는다(v2.3). 파괴가 남기는 조각이 워프권([Recipes])의
    // 값이라 시즌1에도 쓸 데가 처음부터 있다 - 「초반 줍기는 골드」 규칙과
    // salvageGold 환산은 그때 함께 사라졌다. 시즌1 조각의 **쓸 곳**만 워프권으로
    // 제한한다([Recipes.availableIn]).
}
