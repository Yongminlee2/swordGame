package com.geomgang.core

import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.roundToLong
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

    /**
     * 실패해서 단계가 떨어졌다.
     *
     * @property shattered **파괴 판정에서 살아남아** 바닥까지 떨어진 것인지.
     *   부서지지 않는 검(전설검·조합검)만 참이 된다. 한 단계 하락과 겉모습이
     *   같아서, 이것을 구분하지 않으면 +14 검이 갑자기 +1 이 된 것이 버그로 보인다.
     */
    data class Drop(
        override val state: GameState,
        val newLevel: Int,
        val shattered: Boolean = false,
    ) : ForgeResult

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

        // 계열은 +20 에서 끝난다. 그 위는 조합으로만 간다([LegendForge]).
        if (!LegendForge.canForge(sword)) return false

        // 고유검은 벼려진 그대로다. 그 자체로 완성이라 강화대에 올리지 않는다.
        if (sword.uniqueId != null) return false

        val max = state.difficulty.maxLevel
        if (max != null && sword.level >= max) return false

        if (items.blessing && state.inventory.blessingScrolls <= 0) return false
        if (items.luckCharm && state.inventory.luckCharms <= 0) return false

        // 골드는 판정이 직접 깎으므로 여기서도 봐야 한다.
        return state.gold >= Economy.upgradeCost(sword.level)
    }

    /**
     * @param bonus 플레이어가 쌓아 온 몫([ForgeBonuses]). 성공률을 올리고 파괴를 막는다.
     *   상한은 [RateTable.MAX_SUCCESS_RATE] 가 지킨다.
     */
    fun attempt(
        state: GameState,
        items: UsedItems,
        rng: Random,
        bonus: ForgeBonus = ForgeBonus.NONE,
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

        // 계열마다 벼리는 방식이 다르다. 비용·담금질·확률·내구가 전부 여기서 갈린다.
        val forge = FamilyForge.of(sword)

        val paid = state.copy(
            gold = state.gold -
                (Economy.upgradeCost(sword.level) * forge.costMult).roundToLong(),
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
            RateTable.successRate(
                state.difficulty,
                targetLevel,
                items.blessing,
                fails,
                bonus.successRate + forge.successBonus,
                forge.temperCapBonus,
                forge.blessingMult,
            )
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

        // 하락을 막는 것 둘: 행운부적(소모품)과 하락방지 보너스(도감·스킬·고유검·계열).
        // **파괴를 막는 것은 방지권뿐이다**(v2.3) - 보너스가 파괴까지 막던 시절에는
        // 방지권이 설 자리가 없었고, 파괴가 조각(워프의 연료)을 남기는 지금은
        // 파괴를 확률로 뭉개면 조각 경제까지 마른다.
        return when (RateTable.failureBand(targetLevel)) {
            FailureBand.STAY -> ForgeResult.Stay(failed, sword.level)

            FailureBand.DROP ->
                dropOrGuard(failed, sword, items, bonus.dropGuard + forge.dropGuard, rng)

            FailureBand.DESTROY_OR_DROP ->
                if (rng.nextDouble() < RateTable.destroyChance(targetLevel)) {
                    if (WardCharm.protects(failed, sword)) {
                        // 수호 각인 - 전설검이 미끄러지는 것을 한 번 붙든다.
                        val guarded = sword.copy(level = maxOf(LegendForge.LEVEL, sword.level - 1))
                        ForgeResult.Drop(
                            state = failed.copy(sword = guarded, wardCharm = false),
                            newLevel = guarded.level,
                            shattered = true,
                        )
                    } else if (sword.isLegend()) {
                        // 전설검은 사라지지 않는다. 재료 둘을 다시 +20 까지 올리는 것은
                        // 몇 시간을 지우는 일이라 누를 엄두가 안 난다. 단계를 잃는 것으로 충분하다.
                        ForgeResult.Drop(
                            state = failed.copy(sword = sword.copy(level = LegendForge.LEVEL)),
                            newLevel = LegendForge.LEVEL,
                            shattered = true,
                        )
                    } else if (sword.family in LegendForge.REFINED_FAMILIES) {
                        // 조합검(마검·성검·+20 이하 용검)은 **부서지지 않는다.** 언제나 +1 로 남는다.
                        //
                        // 셋 다 기본 검 +20 두 자루(용검은 마검·성검 +20)를 태워야 나온다.
                        // 통째로 잃으면 그 시간이 한 번에 지워져서 "한 번 파괴되면 답이 없다"가
                        // 된다. 절반만 살리는 확률로도 해 봤는데 그 절반이 너무 아팠다 —
                        // 전설검과 같은 규칙(부서지는 대신 바닥으로)으로 통일한다.
                        ForgeResult.Drop(
                            state = failed.copy(sword = sword.copy(level = MATERIAL_FLOOR)),
                            newLevel = MATERIAL_FLOOR,
                            shattered = true,
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
                    // 파괴는 면했다. 남은 것은 하락 - 부적과 하락방지 보너스가 붙들 수 있다.
                    dropOrGuard(failed, sword, items, bonus.dropGuard + forge.dropGuard, rng)
                }
        }
    }

    /**
     * 하락이 정해진 자리에서 마지막으로 붙드는 판정.
     *
     * 부적이 먼저다 - 켰다면 이미 값을 치렀으니 난수 없이 확정으로 막는다.
     * 그다음이 하락방지 보너스(도감·스킬·고유검·계열)의 확률 굴림이다.
     */
    private fun dropOrGuard(
        failed: GameState,
        sword: Sword,
        items: UsedItems,
        dropGuard: Double,
        rng: Random,
    ): ForgeResult = when {
        items.luckCharm -> ForgeResult.Stay(failed, sword.level)
        dropGuard > 0 && rng.nextDouble() < dropGuard -> ForgeResult.Stay(failed, sword.level)
        else -> drop(failed, sword)
    }

    /**
     * 조합검(마검·성검)이 파괴를 면했을 때 남는 단계.
     *
     * [Refinery] 가 내놓는 단계와 같다 — 조합 직후로 되돌아가는 셈이라
     * "다시 여기서부터"가 규칙 하나로 읽힌다.
     */
    const val MATERIAL_FLOOR: Int = 1

    /** 이 계열은 파괴되지 않고 [MATERIAL_FLOOR] 로 떨어진다. 전설검과 같은 규칙이다. */
    fun survivesDestroy(sword: Sword): Boolean =
        sword.isLegend() || sword.family in LegendForge.REFINED_FAMILIES

    /**
     * 조각 회수량 = 단계 × 이 값 × 흔들림.
     *
     * v2.3에서 2 → 3. 파괴가 워프권([Recipes])의 연료가 되면서 조각이 시즌1의
     * 재기 화폐가 됐다 - 회수량이 짜면 "모아서 워프한다"는 고리가 체감되지 않는다.
     */
    const val SALVAGE_MULTIPLIER: Int = 3

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

    /**
     * 파편을 주워 파괴를 마무리한다. 얻는 것은 **조각**이다 - 시즌1에서도.
     *
     * 한때 용검 이전에는 골드로 바꿔 줬다. 지금은 조각이 곧 워프권([Recipes])이라
     * 시즌1에도 쓸 데가 확실하다 - 파괴가 재기의 밑천이 되는 것이 이 경제의 핵심이다.
     */
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
        // 전설검은 +21 아래로 내려가지 않는다. 그 아래는 계열의 영역이다.
        val floor = if (sword.isLegend()) LegendForge.LEVEL else 0
        val dropped = maxOf(floor, sword.level - 1)
        return ForgeResult.Drop(
            state = paid.copy(sword = sword.copy(level = dropped)),
            newLevel = dropped,
        )
    }
}
