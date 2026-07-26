package com.geomgang.core

import kotlinx.serialization.Serializable
import kotlin.random.Random

/**
 * 퀘스트 종류.
 *
 * 진행도는 전용 카운터가 아니라 [Stats]의 누적 카운터를 **차분**으로 읽는다.
 * 배정 시점 값을 [QuestInstance.baseline] 에 남겨 두면, 그 뒤로 얼마나 했는지가 진행도다.
 * 카운터 재활용이라 구현이 싸고, 통계와 퀘스트가 어긋날 수도 없다.
 */
@Serializable
enum class QuestKind(val id: String, val label: String) {
    KILL("kill", "잡몹 처치"),
    BOSS("boss", "보스 처치"),
    FORGE("forge", "강화 시도"),
    FORGE_SUCCESS("forge_success", "강화 성공"),
    FUSE("fuse", "조합"),
    STAR("star", "별 강화"),
    EVENT("event", "이벤트 조우"),
}

@Serializable
data class QuestInstance(
    val kind: QuestKind,
    val target: Int,
    /** 배정 시점의 카운터 값. 진행도 = 현재 - 이 값. */
    val baseline: Long,
    val claimed: Boolean = false,
)

@Serializable
data class QuestState(
    /** yyyyMMdd. 다르면 일일을 새로 뽑는다. 시계는 앱 계층이 준다. */
    val dateKey: String = "",
    val weekKey: String = "",
    val daily: List<QuestInstance> = emptyList(),
    val weekly: QuestInstance? = null,
)

object DailyQuests {

    const val DAILY_COUNT = 3
    const val DAILY_SHARDS = 15
    const val WEEKLY_SHARDS = 120
    const val WEEKLY_KILL_TARGET = 300

    fun dailyTarget(kind: QuestKind): Int = when (kind) {
        QuestKind.KILL -> 60
        QuestKind.BOSS -> 2
        QuestKind.FORGE -> 15
        QuestKind.FORGE_SUCCESS -> 8
        QuestKind.FUSE -> 2
        QuestKind.STAR -> 3
        QuestKind.EVENT -> 3
    }

    fun dailyGold(kind: QuestKind): Long = when (kind) {
        QuestKind.KILL -> 1_200L
        QuestKind.BOSS -> 1_500L
        QuestKind.FORGE -> 1_800L
        QuestKind.FORGE_SUCCESS -> 2_000L
        QuestKind.FUSE -> 1_600L
        QuestKind.STAR -> 1_500L
        QuestKind.EVENT -> 1_400L
    }

    fun counterOf(kind: QuestKind, stats: Stats): Long = when (kind) {
        QuestKind.KILL -> stats.monsterKills
        QuestKind.BOSS -> stats.bossKills
        QuestKind.FORGE -> stats.attempts
        QuestKind.FORGE_SUCCESS -> stats.successes
        QuestKind.FUSE -> stats.fusions
        QuestKind.STAR -> stats.starAttempts
        QuestKind.EVENT -> stats.eventsSeen
    }

    fun progressOf(q: QuestInstance, stats: Stats): Int =
        (counterOf(q.kind, stats) - q.baseline).coerceIn(0, q.target.toLong()).toInt()

    fun isDone(q: QuestInstance, stats: Stats): Boolean = progressOf(q, stats) >= q.target

    fun weeklyQuest(stats: Stats): QuestInstance = QuestInstance(
        kind = QuestKind.KILL,
        target = WEEKLY_KILL_TARGET,
        baseline = stats.monsterKills,
    )

    /**
     * 날짜·주가 바뀌었으면 새로 뽑는다. 같으면 그대로.
     *
     * @param pool 뽑을 수 있는 종류. 미해금 컨텐츠(예: 별 강화 전)는 호출자가 빼고 넘긴다 -
     *             못 깨는 퀘스트를 받으면 하루를 버리는 기분이 든다.
     */
    fun refresh(
        state: QuestState,
        stats: Stats,
        dateKey: String,
        weekKey: String,
        rng: Random,
        pool: List<QuestKind> = QuestKind.entries,
    ): QuestState {
        var next = state

        if (state.dateKey != dateKey) {
            val remaining = pool.toMutableList()
            val picked = buildList {
                repeat(DAILY_COUNT.coerceAtMost(remaining.size)) {
                    add(remaining.removeAt(rng.nextInt(remaining.size)))
                }
            }
            next = next.copy(
                dateKey = dateKey,
                daily = picked.map {
                    QuestInstance(it, dailyTarget(it), counterOf(it, stats))
                },
            )
        }

        if (state.weekKey != weekKey) {
            next = next.copy(weekKey = weekKey, weekly = weeklyQuest(stats))
        }

        return next
    }

    /**
     * 보상을 수령한다. [index] 0..2 = 일일, -1 = 주간.
     *
     * 완료 전·이중 수령은 상태 전제조건 위반이라 [IllegalStateException] 이다.
     */
    fun claim(game: GameState, stats: Stats, index: Int): GameState {
        val quests = game.quests
        return if (index < 0) {
            val weekly = checkNotNull(quests.weekly) { "no weekly quest" }
            check(!weekly.claimed) { "weekly already claimed" }
            check(isDone(weekly, stats)) { "weekly not done" }
            game.copy(
                gold = game.gold + dailyGold(weekly.kind) * 2,
                shards = game.shards + WEEKLY_SHARDS,
                quests = quests.copy(weekly = weekly.copy(claimed = true)),
            )
        } else {
            val q = quests.daily.getOrNull(index)
                ?: throw IllegalStateException("no daily quest at $index")
            check(!q.claimed) { "daily $index already claimed" }
            check(isDone(q, stats)) { "daily $index not done" }
            game.copy(
                gold = game.gold + dailyGold(q.kind),
                shards = game.shards + DAILY_SHARDS,
                quests = quests.copy(
                    daily = quests.daily.mapIndexed { i, item ->
                        if (i == index) item.copy(claimed = true) else item
                    },
                ),
            )
        }
    }
}
