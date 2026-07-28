package com.geomgang.core

/** 화면에 쓰는 정수 백분율. 넷을 더하면 100이다. */
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
         *   행운부적은 실패의 결과 자체를 없앤다.
         * @param temperFails 이 단계에 쌓인 담금질. 성공률을 올린다.
         */
        fun of(
            difficulty: Difficulty,
            targetLevel: Int,
            items: UsedItems = UsedItems.NONE,
            temperFails: Int = 0,
        ): ForgeOdds {
            val success =
                RateTable.successRate(difficulty, targetLevel, items.blessing, temperFails)
            val fail = 1.0 - success

            // 행운부적은 실패해도 단계를 지키므로 실패분이 전부 유지가 된다.
            if (items.luckCharm) {
                return ForgeOdds(success = success, stay = fail, drop = 0.0, destroy = 0.0)
            }

            return when (RateTable.failureBand(targetLevel)) {
                FailureBand.STAY ->
                    ForgeOdds(success, stay = fail, drop = 0.0, destroy = 0.0)

                FailureBand.DROP ->
                    ForgeOdds(success, stay = 0.0, drop = fail, destroy = 0.0)

                FailureBand.DESTROY_OR_DROP -> {
                    val destroyShare = RateTable.destroyChance(targetLevel)
                    ForgeOdds(
                        success = success,
                        stay = 0.0,
                        drop = fail * (1.0 - destroyShare),
                        destroy = fail * destroyShare,
                    )
                }
            }
        }
    }
}
