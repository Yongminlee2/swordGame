package com.geomgang.core

/**
 * 화면에 쓰는 백분율. 0.01%p 단위로 반올림되어 있고 넷을 더하면 정확히 100이다.
 *
 * 정수 %였을 때는 도감 한 장(+0.1%p 수준)의 변화가 반올림에 통째로 삼켜졌다 -
 * 쌓는 재미는 숫자가 움직이는 것이 보여야 생긴다.
 */
data class OddsPercent(
    val success: Double,
    val stay: Double,
    val drop: Double,
    val destroy: Double,
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
        // 만분율(0.01%p)로 반올림한 뒤 오차를 흡수한다. 단위만 촘촘해졌지 방법은 같다.
        val s = Math.round(success * 10_000).toInt()
        val st = Math.round(stay * 10_000).toInt()
        val dr = Math.round(drop * 10_000).toInt()
        val de = Math.round(destroy * 10_000).toInt()
        val gap = 10_000 - (s + st + dr + de)

        // 오차는 가장 큰 항목에 얹는다 - 큰 수에서 0.01%p 는 눈에 띄지 않는다.
        val biggest = listOf(s, st, dr, de).max()
        val (fs, fst, fdr, fde) = when {
            gap == 0 -> listOf(s, st, dr, de)
            biggest == s -> listOf(s + gap, st, dr, de)
            biggest == st -> listOf(s, st + gap, dr, de)
            biggest == dr -> listOf(s, st, dr + gap, de)
            else -> listOf(s, st, dr, de + gap)
        }
        return OddsPercent(fs / 100.0, fst / 100.0, fdr / 100.0, fde / 100.0)
    }

    companion object {
        /**
         * @param targetLevel 이번 시도로 **도달하려는** 단계 (현재 단계 + 1)
         * @param items 지금 켜 둔 아이템. 축복서는 성공률을 올리고,
         *   행운부적은 **하락만** 막는다 - 파괴 확률은 그대로다.
         * @param temperFails 이 단계에 쌓인 담금질. 성공률을 올린다.
         * @param bonus 쌓아 온 몫과 계열 특성을 더한 성공률 가산([ForgeBonuses]).
         * @param destroyGuard 파괴가 정해진 뒤 한 번 더 막을 확률. 막히면 단계가 그대로다.
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
            destroyGuard: Double = 0.0,
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

                FailureBand.DROP ->
                    // 부적은 하락을 막는다. 이 구간의 실패는 전부 유지가 된다.
                    if (items.luckCharm) {
                        ForgeOdds(success, stay = fail, drop = 0.0, destroy = 0.0)
                    } else {
                        ForgeOdds(success, stay = 0.0, drop = fail, destroy = 0.0)
                    }

                FailureBand.DESTROY_OR_DROP -> {
                    val destroyShare = RateTable.destroyChance(targetLevel)
                    val doomed = fail * destroyShare
                    // 파괴가 정해져도 방지 특성이 한 번 더 막는다. 막히면 단계가 그대로다.
                    val guarded = doomed * destroyGuard.coerceIn(0.0, 1.0)
                    val lost = doomed - guarded
                    // 파괴를 면한 실패. 부적은 **여기만** 붙든다 —
                    // 파괴 확률은 부적을 켜도 한 치도 줄지 않는다(v2.3).
                    val survived = fail * (1.0 - destroyShare)
                    ForgeOdds(
                        success = success,
                        stay = guarded + if (items.luckCharm) survived else 0.0,
                        // 전설검은 부서지지 않고 +21 로 돌아간다 - 화면도 그렇게 말해야 한다.
                        // 그 하락은 파괴 판정에서 나온 것이라 부적으로도 못 막는다.
                        drop = (if (items.luckCharm) 0.0 else survived) +
                            if (legend) lost else 0.0,
                        destroy = if (legend) 0.0 else lost,
                    )
                }
            }
        }
    }
}
