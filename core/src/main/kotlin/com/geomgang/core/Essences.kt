package com.geomgang.core

/**
 * 정수 — **깊이의 무게.**
 *
 * 보스를 잡을 때마다 그 구역의 정수가 하나 남는다. 구역이 24곳이니 정수도 24종인데,
 * 예전에는 고유검 레시피가 요구하는 세 종(심연·화산·설원)만 쓸모가 있었다.
 * **나머지 스물한 종은 모아도 쓸 데가 없었다.**
 *
 * 이제 정수마다 **무게**가 있다. 깊은 구역일수록 무겁고, 그 무게를 모아
 * [WardCharm] 을 산다. 24구역이 전부 값어치를 갖되 깊은 구역이 확실히 낫다 —
 * 사냥터 후반이 존재할 이유가 여기서 생긴다.
 *
 * 저장 구조([GameState.essences])는 그대로다. 옛 세이브가 손실 없이 열리고
 * 구역별 정체성도 남는다.
 */
object Essences {

    /** 가장 얕은 구역의 권장 단계. 무게의 원점이다. */
    private val SHALLOWEST: Int = Zone.entries.minOf { it.recommendedLevel }

    /**
     * 정수 한 개의 무게 = 가장 얕은 구역에서 얼마나 더 깊은가 + 1.
     *
     * 초원이 1, 끝의 문이 39다. 권장 레벨을 쓰는 이유는 그것이 이미
     * "얼마나 깊은가" 의 단일 출처이기 때문이다 — 따로 표를 두면 구역을 손볼 때마다
     * 두 곳이 어긋난다.
     *
     * **원점을 빼는 이유**: v2.3에서 사냥터 사다리를 통째로 +15 올렸다(용검을 쥐어야
     * 들어올 수 있는 곳이라 권장 0 짜리 구역은 뜻이 없었다). 권장 레벨을 그대로 쓰면
     * 그때 정수 무게가 전부 15씩 뛰어 [WardCharm.COST] 가 조용히 헐값이 된다.
     * 무게는 **구역 사이의 거리**여야지 사다리의 절대 높이여서는 안 된다.
     */
    fun weightOf(zoneId: String): Int =
        Zone.fromId(zoneId).recommendedLevel - SHALLOWEST + 1

    /** 지금 가진 정수를 전부 무게로 환산한 값. */
    fun powerOf(essences: Map<String, Int>): Int =
        essences.entries.sumOf { (zoneId, count) -> weightOf(zoneId) * count }

    /**
     * 무게 [amount] 만큼을 태운 뒤 남는 정수.
     *
     * **얕은 구역부터 쓴다.** 고유검이 요구하는 깊은 구역 정수를 각인이 먼저
     * 먹어 치우면 두 쓰임이 경쟁이 아니라 방해가 된다.
     *
     * 마지막 한 개는 무게가 남아도 통째로 태워진다 — 정수는 쪼갤 수 없다.
     */
    fun spend(essences: Map<String, Int>, amount: Int): Map<String, Int> {
        require(amount >= 0) { "amount must be >= 0, was $amount" }
        var left = amount
        val remaining = essences.toMutableMap()
        // 얕은 것부터. 같은 무게면 id 순으로 — 결과가 우연에 맡겨지지 않게 한다.
        val order = remaining.keys.sortedWith(compareBy({ weightOf(it) }, { it }))
        for (zoneId in order) {
            if (left <= 0) break
            val weight = weightOf(zoneId)
            val have = remaining[zoneId] ?: continue
            // 남은 무게를 덮는 데 필요한 개수. 올림이라 마지막 한 개는 통째로 나간다.
            val use = minOf(have, (left + weight - 1) / weight)
            left -= use * weight
            if (use >= have) remaining.remove(zoneId) else remaining[zoneId] = have - use
        }
        return remaining
    }
}

/**
 * 수호 각인 — 전설검이 미끄러질 때 붙드는 것.
 *
 * 전설검은 실패하면 무조건 [LegendForge.LEVEL] 로 돌아간다. +45 에서 한 번
 * 미끄러지면 24단계가 사라지는데, 그걸 막을 수단이 아무것도 없었다.
 *
 * 각인을 지니고 있으면 그 한 번을 **한 단계 하락**으로 바꾼다. 쓰면 사라진다.
 *
 * **한 장만 지닌다.** 쌓아 두면 전설 등반이 거저가 된다. 한 장뿐이면
 * "사냥해서 각인 사고 → 강화하다 터지면 다시 사냥" 이라는 왕복이 생기고,
 * 정수가 곧 진도가 된다.
 */
object WardCharm {

    /** 한 장 값. 끝의 문 보스 3번, 화산 보스 10번쯤이다. */
    const val COST: Int = 120

    fun canBuy(state: GameState): Boolean =
        !state.wardCharm &&
            state.pendingDestroy == null &&
            Essences.powerOf(state.essences) >= COST

    fun buy(state: GameState): GameState {
        check(canBuy(state)) { "cannot buy a ward charm in this state" }
        return state.copy(
            wardCharm = true,
            essences = Essences.spend(state.essences, COST),
        )
    }

    /** 이 검을 각인이 지켜 줄 수 있는지. **전설검 전용**이다. */
    fun protects(state: GameState, sword: Sword): Boolean =
        state.wardCharm && sword.isLegend()
}
