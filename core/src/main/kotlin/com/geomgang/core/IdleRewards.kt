package com.geomgang.core

/**
 * 자리를 비운 사이 쌓인 보상.
 *
 * @param seconds 실제로 인정된 시간(상한이 이미 적용된 값)
 * @param zone 이 보상을 계산한 기준 구역
 */
data class IdleReward(
    val seconds: Long,
    val zone: Zone,
    val gold: Long,
    val stones: Int,
)

/**
 * 자리비움 보상.
 *
 * 켜 두지 않아도 조금씩 쌓이게 해서 "다시 켤 이유"를 만든다. 다만 **손으로 잡는 편이
 * 훨씬 빨라야 한다** — 자리비움이 사냥보다 이득이면 사냥터가 죽는다. 그래서
 * 1분에 잡몹 한 마리 값만 주고, 상한을 [MAX_SECONDS] 로 끊는다.
 *
 * 기준은 **깬 구역 중 가장 깊은 곳**이다. 지금 서 있는 구역으로 하면 이길 수 없는
 * 구역에 들어가 앱을 끄는 것만으로 수익이 오른다.
 */
object IdleRewards {

    /** 이 시간에 못 미치면 보상이 없다. 잠깐 화면을 나갔다 오는 것까지 알릴 필요는 없다. */
    const val MIN_SECONDS = 120L

    /** 아무리 오래 비워도 여기까지만 쌓인다. */
    const val MAX_SECONDS = 8L * 60 * 60

    /** 1분에 잡몹 몇 마리 값을 줄지. 1마리 = 손으로 잡는 속도의 한참 아래다. */
    const val KILLS_PER_MINUTE = 1

    /** 한 시간에 쌓이는 강화석. 상한과 맞물려 한 번 복귀에 최대 8개다. */
    const val STONES_PER_HOUR = 1

    /**
     * 보상 계산의 기준이 되는 구역.
     *
     * 깬 구역이 없으면 첫 구역이다 — 시작하자마자 앱을 껐다 켜도 무언가는 쌓인다.
     */
    fun baseZone(state: GameState): Zone =
        Zone.entries.lastOrNull { state.adventure.isCleared(it) } ?: Zone.entries.first()

    /**
     * [elapsedSeconds] 동안 쌓인 보상. 보상이 없을 만큼 짧으면 null.
     *
     * 음수(기기 시계를 되돌린 경우)는 0으로 본다. 시계를 앞당겨 얻는 이득은
     * [MAX_SECONDS] 가 막는다.
     */
    fun rewardFor(state: GameState, elapsedSeconds: Long): IdleReward? {
        val seconds = elapsedSeconds.coerceIn(0, MAX_SECONDS)
        if (seconds < MIN_SECONDS) return null

        val zone = baseZone(state)
        val minutes = seconds / 60
        val gold = zone.baseGold * KILLS_PER_MINUTE * minutes
        val stones = (seconds / 3600).toInt() * STONES_PER_HOUR

        return IdleReward(seconds = seconds, zone = zone, gold = gold, stones = stones)
    }

    /** 보상을 상태에 반영한다. */
    fun apply(state: GameState, reward: IdleReward): GameState = state.copy(
        gold = state.gold + reward.gold,
        forgeStones = state.forgeStones + reward.stones,
    )

    /** "3시간 20분" 처럼 읽히는 문구. 화면과 도움말이 함께 쓴다. */
    fun durationText(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        return when {
            hours > 0 && minutes > 0 -> "${hours}시간 ${minutes}분"
            hours > 0 -> "${hours}시간"
            else -> "${minutes}분"
        }
    }
}
