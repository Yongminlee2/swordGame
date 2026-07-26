package com.geomgang.core

import kotlin.math.floor
import kotlin.random.Random

/**
 * 강화 1회의 결과.
 *
 * [state]는 결과가 이미 반영된 새 상태다. 비용 차감과 아이템 소모도 포함되어 있다.
 */
sealed interface ForgeResult {

    val state: GameState

    /** 단계가 올랐다. */
    data class Success(override val state: GameState, val newLevel: Int) : ForgeResult

    /** 실패했지만 단계는 그대로다. 안전구간이거나 행운부적을 썼을 때. */
    data class Stay(override val state: GameState, val level: Int) : ForgeResult

    /** 실패해서 한 단계 떨어졌다. */
    data class Drop(override val state: GameState, val newLevel: Int) : ForgeResult

    /**
     * 파괴됐다. [state]의 검은 이미 null 이고 `pendingDestroy`가 채워져 있다.
     *
     * @property preventable 방지권을 갖고 있어 되살릴 수 있는지
     */
    data class Destroyed(
        override val state: GameState,
        val lostLevel: Int,
        val preventable: Boolean,
    ) : ForgeResult
}

/**
 * 강화 판정.
 *
 * 확률 숫자는 [RateTable], 비용은 [Economy]에만 있다. 여기에는 규칙의 조합만 둔다.
 * 밸런스를 고칠 때 고칠 곳을 한 군데로 유지하기 위해서다.
 *
 * 난수 소비 순서는 테스트가 의존하는 계약이다.
 * 1. 성공 판정
 * 2. 파괴/하락 판정 (파괴 가능 구간에서 부적 없이 실패했을 때만)
 */
object ForgeEngine {

    /**
     * 자동 강화가 허용되는 최대 현재 단계.
     *
     * 이 단계에서 시도하면 목표가 안전구간의 끝([RateTable.SAFE_BAND_END])이라
     * 실패해도 단계가 유지된다. 하락·파괴가 걸린 구간을 자동화하면
     * 그 구간의 긴장이 사라지고 게임이 남지 않는다.
     */
    const val AUTO_FORGE_MAX_LEVEL: Int = RateTable.SAFE_BAND_END - 1

    fun canAttempt(state: GameState, items: UsedItems): Boolean {
        val sword = state.sword ?: return false
        if (state.pendingDestroy != null) return false

        val max = state.difficulty.maxLevel
        if (max != null && sword.level >= max) return false

        if (items.blessing && state.inventory.blessingScrolls <= 0) return false
        if (items.luckCharm && state.inventory.luckCharms <= 0) return false

        return state.gold >= Economy.upgradeCost(sword.level)
    }

    /** 자동 강화 루프가 한 번 더 돌아도 되는지. 안전구간을 벗어나면 멈춘다. */
    fun canAutoForge(state: GameState): Boolean {
        val sword = state.sword ?: return false
        if (sword.level > AUTO_FORGE_MAX_LEVEL) return false
        return canAttempt(state, UsedItems.NONE)
    }

    fun attempt(state: GameState, items: UsedItems, rng: Random): ForgeResult {
        check(canAttempt(state, items)) {
            "attempt() called on a state that fails canAttempt()"
        }
        val sword = requireNotNull(state.sword)
        val targetLevel = sword.level + 1

        var inventory = state.inventory
        if (items.blessing) inventory = inventory.minus(Item.BLESSING_SCROLL, 1)
        if (items.luckCharm) inventory = inventory.minus(Item.LUCK_CHARM, 1)

        val paid = state.copy(
            gold = state.gold - Economy.upgradeCost(sword.level),
            inventory = inventory,
        )

        val successRate = RateTable.successRate(state.difficulty, targetLevel, items.blessing)
        if (rng.nextDouble() < successRate) {
            return ForgeResult.Success(
                state = paid.copy(
                    sword = sword.copy(level = targetLevel),
                    bestLevel = maxOf(paid.bestLevel, targetLevel),
                ),
                newLevel = targetLevel,
            )
        }

        // 행운부적은 실패의 결과 자체를 무효화한다. 파괴 판정 난수도 소비하지 않는다.
        if (items.luckCharm) {
            return ForgeResult.Stay(paid, sword.level)
        }

        return when (RateTable.failureBand(targetLevel)) {
            FailureBand.STAY -> ForgeResult.Stay(paid, sword.level)

            FailureBand.DROP -> drop(paid, sword)

            FailureBand.DESTROY_OR_DROP ->
                if (rng.nextDouble() < RateTable.destroyChance(targetLevel)) {
                    ForgeResult.Destroyed(
                        state = paid.copy(
                            sword = null,
                            pendingDestroy = PendingDestroy(sword.family, sword.level),
                        ),
                        lostLevel = sword.level,
                        preventable = paid.inventory.preventTickets > 0,
                    )
                } else {
                    drop(paid, sword)
                }
        }
    }

    /** 조각 회수량 = 단계 × 이 값 × 흔들림. */
    const val SALVAGE_MULTIPLIER: Int = 2

    private const val SALVAGE_JITTER_MIN = 0.7
    private const val SALVAGE_JITTER_MAX = 1.3

    fun canPrevent(state: GameState): Boolean =
        state.pendingDestroy != null && state.inventory.preventTickets > 0

    /**
     * 방지권을 태워 파괴 직전 상태로 되돌린다.
     *
     * 제한 시간 안에 눌렀을 때만 호출된다. 시간을 넘겼으면 [confirmDestroy]를 부른다.
     */
    fun applyPrevent(state: GameState): GameState {
        // 파괴 대기 여부는 인자 검증이 아니라 상태 전제조건이므로 checkNotNull 을 쓴다.
        val pending = checkNotNull(state.pendingDestroy) { "no pending destroy to prevent" }
        check(state.inventory.preventTickets > 0) { "no prevent ticket" }
        return state.copy(
            sword = Sword(pending.family, pending.level),
            inventory = state.inventory.minus(Item.PREVENT_TICKET, 1),
            pendingDestroy = null,
        )
    }

    /** 파괴된 검에서 나오는 조각 수. 최소 1개는 나온다. */
    fun salvageAmount(level: Int, rng: Random): Int {
        require(level >= 0) { "level must be >= 0, was $level" }
        val jitter =
            SALVAGE_JITTER_MIN + rng.nextDouble() * (SALVAGE_JITTER_MAX - SALVAGE_JITTER_MIN)
        val raw = floor(level * SALVAGE_MULTIPLIER * jitter).toInt()
        return maxOf(1, raw)
    }

    /** 파편을 주워 조각을 얻고 파괴를 마무리한다. */
    fun applySalvage(state: GameState, rng: Random): GameState {
        val pending = checkNotNull(state.pendingDestroy) { "no pending destroy to salvage" }
        return state.copy(
            shards = state.shards + salvageAmount(pending.level, rng),
            pendingDestroy = null,
        )
    }

    /**
     * 파괴를 확정한다. 아무것도 주지 않는다.
     *
     * 방지권·줍기 제한 시간을 넘겼을 때, 그리고 **파괴 대기 상태가 저장된 채로
     * 앱이 다시 켜졌을 때** 호출한다. 후자를 처리하지 않으면 방지권 대기 중
     * 강제 종료로 파괴를 무효화할 수 있다.
     */
    fun confirmDestroy(state: GameState): GameState =
        if (state.pendingDestroy == null) state else state.copy(pendingDestroy = null)

    private fun drop(paid: GameState, sword: Sword): ForgeResult.Drop {
        val dropped = maxOf(0, sword.level - 1)
        return ForgeResult.Drop(
            state = paid.copy(sword = sword.copy(level = dropped)),
            newLevel = dropped,
        )
    }
}
