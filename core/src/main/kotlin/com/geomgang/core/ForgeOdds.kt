package com.geomgang.core

/**
 * 화면에 쓰는 정수 백분율. 넷을 더하면 100이다.
 *
 * 소수 둘째 자리까지 보여 준 적이 있는데(v2.3) **읽기 나빴다** — 46.00% 같은 표기는
 * 정보가 아니라 소음이다. 도감·고유검이 얼마나 올려 줬는지는 여기가 아니라
 * 보너스 내역이 말한다. 큰 숫자는 굵게, 잔돈은 내역에 — 그게 이 화면의 규칙이다.
 */
data class OddsPercent(
    val success: Int,
    val stay: Int,
    val drop: Int,
    val destroy: Int,
)

/**
 * 이번 강화 한 번이 어떻게 끝날 수 있는지.
 *
 * 화면에는 성공률만 있었다. 떨어질 확률과 부서질 확률은 [RateTable] 안에 있었는데
 * 보이지 않아서, 특히 **무한 구간에서는 실패가 곧 파괴**라는 사실을 모르고 눌렀다.
 *
 * 계산을 여기 한 곳에 둔다. 화면이 확률을 다시 계산하면 규칙이 두 군데가 되고
 * 반드시 어긋난다.
 */
data class ForgeOdds(
    val success: Double,
    /** 실패했지만 단계가 그대로일 확률. */
    val stay: Double,
    val drop: Double,
    val destroy: Double,
) {
    /**
     * 정수 백분율.
     *
     * 넷을 각각 반올림하면 합이 99나 101이 될 수 있다. 성공률을 먼저 반올림하고
     * 나머지를 큰 쪽부터 채운 뒤, 남는 오차는 **가장 큰 항목**이 흡수한다.
     */
    fun percents(): OddsPercent {
        val s = Math.round(success * 100).toInt()
        val st = Math.round(stay * 100).toInt()
        val dr = Math.round(drop * 100).toInt()
        val de = Math.round(destroy * 100).toInt()
        val gap = 100 - (s + st + dr + de)
        if (gap == 0) return OddsPercent(s, st, dr, de)

        // 오차는 가장 큰 항목에 얹는다 - 큰 수에서 1%p 는 눈에 띄지 않는다.
        val biggest = listOf(s, st, dr, de).max()
        return when (biggest) {
            s -> OddsPercent(s + gap, st, dr, de)
            st -> OddsPercent(s, st + gap, dr, de)
            dr -> OddsPercent(s, st, dr + gap, de)
            else -> OddsPercent(s, st, dr, de + gap)
        }
    }

    companion object {
        /**
         * @param targetLevel 이번 시도로 **도달하려는** 단계 (현재 단계 + 1)
         * @param items 지금 켜 둔 아이템. 축복서는 성공률을 올리고,
         *   행운부적은 **하락만** 막는다 - 파괴 확률은 그대로다.
         * @param temperFails 이 단계에 쌓인 담금질. 성공률을 올린다.
         * @param bonus 쌓아 온 몫과 계열 특성을 더한 성공률 가산([ForgeBonuses]).
         * @param dropGuard 하락이 정해진 뒤 한 번 더 막을 확률. 막히면 단계가 그대로다.
         *   파괴에는 손대지 못한다 - 파괴를 막는 것은 방지권뿐이다(v2.3).
         * @param legend 전설검인지. 전설검은 부서지는 대신 [LegendForge.LEVEL] 로 돌아간다.
         * @param temperCapBonus 담금질 상한 가산. 전설검만 갖는다.
         * @param blessingMult 축복서 효과 배수. 성검이 1.5배로 쓴다.
         */
        fun of(
            difficulty: Difficulty,
            targetLevel: Int,
            items: UsedItems = UsedItems.NONE,
            temperFails: Int = 0,
            bonus: Double = 0.0,
            dropGuard: Double = 0.0,
            legend: Boolean = false,
            temperCapBonus: Double = 0.0,
            blessingMult: Double = 1.0,
        ): ForgeOdds {
            val success = RateTable.successRate(
                difficulty,
                targetLevel,
                items.blessing,
                temperFails,
                bonus,
                temperCapBonus,
                blessingMult,
            )
            val fail = 1.0 - success

            return when (RateTable.failureBand(targetLevel)) {
                FailureBand.STAY ->
                    ForgeOdds(success, stay = fail, drop = 0.0, destroy = 0.0)

                FailureBand.DROP -> {
                    // 하락을 붙드는 것 둘: 부적(확정)과 하락방지 보너스(확률).
                    val saved = if (items.luckCharm) fail else fail * dropGuard.coerceIn(0.0, 1.0)
                    ForgeOdds(success, stay = saved, drop = fail - saved, destroy = 0.0)
                }

                FailureBand.DESTROY_OR_DROP -> {
                    val destroyShare = RateTable.destroyChance(targetLevel)
                    // 파괴는 아무 보너스도 못 막는다 - 방지권(사후)만이 되살린다.
                    val lost = fail * destroyShare
                    // 파괴를 면한 실패가 하락이고, 부적·하락방지는 여기만 붙든다.
                    val survived = fail * (1.0 - destroyShare)
                    val saved =
                        if (items.luckCharm) survived else survived * dropGuard.coerceIn(0.0, 1.0)
                    ForgeOdds(
                        success = success,
                        stay = saved,
                        // 전설검은 부서지지 않고 +21 로 돌아간다 - 화면도 그렇게 말해야 한다.
                        // 그 하락은 파괴 판정에서 나온 것이라 부적으로도 못 막는다.
                        drop = (survived - saved) + if (legend) lost else 0.0,
                        destroy = if (legend) 0.0 else lost,
                    )
                }
            }
        }
    }
}
