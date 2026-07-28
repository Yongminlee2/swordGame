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
     * 강화를 시작할 수 있는지. **재료를 태우기 전에** 묻는 관문이다.
     *
     */
    fun canAttempt(state: GameState, items: UsedItems): Boolean =
        // 골드·강화석 요구는 ForgeCost 가 단일 출처다.
        canRoll(state, items) && ForgeCost.canPay(state)

    /**
     * 주사위를 굴릴 수 있는 상태인지. **재료는 묻지 않는다.**
     *
     * 재료는 판정 전에 태워지므로 판정 시점에는 이미 보관함에서 사라져 있다.
     * 여기서 재료를 다시 물으면 **딱 필요한 만큼만 가진 플레이어**가 낼 것을 다 내고도
     * 조건을 못 맞춘 상태가 되어 [attempt] 의 `check` 가 터진다.
     * 재료는 입장료고, 입장료는 한 번만 받는다.
     */
    fun canRoll(state: GameState, items: UsedItems): Boolean {
        val sword = state.sword ?: return false
        if (state.pendingDestroy != null) return false

        val max = state.difficulty.maxLevel
        if (max != null && sword.level >= max) return false

        if (items.blessing && state.inventory.blessingScrolls <= 0) return false
        if (items.luckCharm && state.inventory.luckCharms <= 0) return false

        // 골드는 판정이 직접 깎으므로 여기서도 봐야 한다.
        return state.gold >= Economy.upgradeCost(sword.level)
    }

    /**
     * @param extraSuccessRate 재료 강화 같은 외부 보정(%p). 상한은 [RateTable.MAX_SUCCESS_RATE] 가 지킨다.
     */
    fun attempt(
        state: GameState,
        items: UsedItems,
        rng: Random,
        extraSuccessRate: Double = 0.0,
    ): ForgeResult {
        // 재료를 다시 묻지 않는다 - 여기 오기 전에 이미 태워졌기 때문이다.
        check(canRoll(state, items)) {
            "attempt() called on a state that fails canRoll()"
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

        // 담금질은 이 목표 단계에 쌓인 것만 센다. 단계가 바뀌었으면 0 부터다.
        val fails = Tempering.failsFor(state, targetLevel)

        /**
         * 실패했을 때의 바탕 상태. 담금질이 한 칸 쌓여 있다.
         *
         * 부적을 써서 검이 무사한 실패도 여기를 지난다 - 그러지 않으면 부적이
         * "손해 없는 굴림" 이 되어 고를 이유가 사라진다.
         */
        val failed = if (Tempering.applies(targetLevel)) {
            paid.copy(temperLevel = targetLevel, temperFails = fails + 1)
        } else {
            paid
        }

        val successRate = (
            RateTable.successRate(state.difficulty, targetLevel, items.blessing, fails) +
                extraSuccessRate + UniqueSwords.forgeBonusOf(sword)
            ).coerceAtMost(RateTable.MAX_SUCCESS_RATE)
        if (rng.nextDouble() < successRate) {
            return ForgeResult.Success(
                state = paid.copy(
                    sword = sword.copy(level = targetLevel),
                    bestLevel = maxOf(paid.bestLevel, targetLevel),
                    // 성공하면 담금질은 처음으로 돌아간다.
                    temperLevel = 0,
                    temperFails = 0,
                ),
                newLevel = targetLevel,
            )
        }

        // 행운부적은 실패의 결과 자체를 무효화한다. 파괴 판정 난수도 소비하지 않는다.
        if (items.luckCharm) {
            return ForgeResult.Stay(failed, sword.level)
        }

        return when (RateTable.failureBand(targetLevel)) {
            FailureBand.STAY -> ForgeResult.Stay(failed, sword.level)

            FailureBand.DROP -> drop(failed, sword)

            FailureBand.DESTROY_OR_DROP ->
                if (rng.nextDouble() < RateTable.destroyChance(targetLevel)) {
                    if (UniqueSwords.canRevive(sword)) {
                        // 불사조 - 파괴 대신 한 번 되살아난다. 대가로 단계를 잃고
                        // 고유의 힘도 재가 된다(uniqueId 소멸). 난수 소비는 파괴와 동일.
                        val revived = sword.copy(
                            level = (sword.level - UniqueSwords.REVIVE_LEVEL_LOSS)
                                .coerceAtLeast(0),
                            uniqueId = null,
                        )
                        ForgeResult.Drop(
                            state = failed.copy(sword = revived),
                            newLevel = revived.level,
                        )
                    } else {
                        ForgeResult.Destroyed(
                            state = failed.copy(
                                sword = null,
                                pendingDestroy = PendingDestroy(sword.family, sword.level),
                            ),
                            lostLevel = sword.level,
                            preventable = failed.inventory.preventTickets > 0,
                        )
                    }
                } else {
                    drop(failed, sword)
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
