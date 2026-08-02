package com.geomgang.core.sim

import com.geomgang.core.Difficulty
import com.geomgang.core.Economy
import com.geomgang.core.ForgeCost
import com.geomgang.core.ForgeEngine
import com.geomgang.core.ForgeResult
import com.geomgang.core.GameState
import com.geomgang.core.Item
import com.geomgang.core.Recipes
import com.geomgang.core.Sword
import com.geomgang.core.UsedItems
import com.geomgang.core.WeaponFamily
import kotlin.random.Random

/** 한 판(가상 플레이어 1명)의 결과. */
data class RunOutcome(
    val bestLevel: Int,
    val attempts: Int,
    val bailouts: Int,
    val reachedCap: Boolean,
)

/** 여러 판을 돌린 요약. 확률표 튜닝의 판단 근거다. */
data class SimReport(
    val difficulty: Difficulty,
    val runs: Int,
    val averageBestLevel: Double,
    val medianBestLevel: Int,
    val p90BestLevel: Int,
    val p99BestLevel: Int,
    val maxBestLevel: Int,
    val bankruptcyRate: Double,
    val capRate: Double,
    val averageAttempts: Double,
) {
    override fun toString(): String = buildString {
        appendLine("[$difficulty] runs=$runs")
        appendLine("  평균 최고 단계  : %.2f".format(averageBestLevel))
        appendLine("  중앙값/p90/p99  : $medianBestLevel / $p90BestLevel / $p99BestLevel")
        appendLine("  최고 도달       : $maxBestLevel")
        appendLine("  파산 구제 발생율: %.1f%%".format(bankruptcyRate * 100))
        appendLine("  상한 도달률     : %.2f%%".format(capRate * 100))
        appendLine("  평균 시도 수    : %.1f".format(averageAttempts))
    }
}

/**
 * 자동 플레이 시뮬레이터.
 *
 * 밸런스를 감으로 조정하지 않기 위한 도구다. 확률표를 바꾸면 여기서 나오는 숫자가 바뀌고,
 * 그 숫자로 판단한다. 동시에 회귀 방지선 역할도 한다.
 *
 * 가상 플레이어는 두 국면을 오간다.
 *
 * **자금 모으기(FARM)** — 군자금이 [WAR_CHEST] 미만일 때.
 * 아이템을 사지 않고 [FARM_SELL_LEVEL]까지만 올린 뒤 팔아 현금으로 바꾼다.
 *
 * **밀어붙이기(PUSH)** — 군자금이 충분할 때.
 * 방지권을 [PREVENT_STOCK]장까지 채우고, 위험 구간에서는 축복서를,
 * 최상단에서는 행운부적까지 쓰며 갈 수 있는 데까지 올린다.
 *
 * 이 구분이 없으면 플레이어는 영구 파산 상태로 저단계만 맴돈다.
 * 파괴가 투자한 골드를 통째로 지우기 때문에, 중간에 현금화하는 국면이 없으면
 * 고단계에 도전할 자금 자체가 생기지 않는다.
 */
object BalanceSimulation {

    private const val START_GOLD = 500L

    /** 이 금액을 모으면 고단계 도전에 나선다. */
    private const val WAR_CHEST = 50_000L

    /** 자금 모으기 국면에서 검을 파는 단계. */
    // v2.3 - 파괴가 +10부터 시작하므로 안전하게 +9에서 판다. +10을 노리면
    // 자금 모으기 판의 10%가 통째로 부서진다.
    private const val FARM_SELL_LEVEL = 9

    /** 유지하려는 방지권 재고. */
    private const val PREVENT_STOCK = 5

    /** 이 단계부터 아이템을 챙기고 쓰기 시작한다. 실패가 파괴로 이어지는 구간이다. */
    private const val RISK_LEVEL = 12

    /** 이 단계부터는 행운부적까지 동원한다. */
    private const val CRITICAL_LEVEL = 18

    /** 강화 없이 준비 동작만 반복하는 상황을 끊는 안전장치. */
    private const val MAX_SPINS_WITHOUT_ATTEMPT = 100

    /**
     * 재료 검 한 자루를 구하는 데 드는 골드.
     *
     * 실제 게임에서는 사냥 드롭으로 얻지만, 시뮬레이터는 사냥을 모형화하지 않는다.
     * 골드와 재료가 **같은 출처(사냥)** 에서 나오므로 골드 환산이 타당하다.
     */
    private const val MATERIAL_SWORD_PRICE = Economy.BASE_SWORD_PRICE

    /** 강화석 하나를 구하는 데 드는 골드 환산값. 조각 20개 교환에 상응한다. */
    private const val STONE_PRICE = 240L

    fun simulateRun(difficulty: Difficulty, rng: Random, maxAttempts: Int): RunOutcome {
        var state = GameState(difficulty, gold = START_GOLD)
        var attempts = 0
        var bailouts = 0
        var spins = 0
        val cap = difficulty.maxLevel

        while (attempts < maxAttempts) {
            if (spins++ > MAX_SPINS_WITHOUT_ATTEMPT) break

            // 1. 파산 구제
            if (Economy.needsBailout(state)) {
                state = Economy.applyBailoutIfNeeded(state)
                bailouts++
                continue
            }

            // 2. 검 확보. 조각이 넉넉하면 가장 높은 워프권부터 쓴다(v2.3) -
            // 파괴가 남긴 조각으로 +15/+10/+5 에서 다시 시작하는 것이 이 경제의 축이다.
            if (state.sword == null) {
                val warp = listOf("sword15", "sword10", "sword5")
                    .map { Recipes.byId(it) }
                    .firstOrNull { Recipes.canCraft(state, it) }
                state = when {
                    warp != null ->
                        Recipes.craft(state, warp, WeaponFamily.STRAIGHT)

                    Economy.canBuySword(state) ->
                        Economy.buySword(state, WeaponFamily.STRAIGHT)

                    else -> break // 구제가 있으므로 여기 오면 안 되지만 방어한다
                }
                continue
            }

            // 3. 상한 도달
            val sword = state.sword ?: continue
            if (cap != null && sword.level >= cap) break

            val pushing = state.gold >= WAR_CHEST

            // 4. 자금 모으기 국면이면 목표 단계에서 현금화한다
            if (!pushing) {
                if (sword.level >= FARM_SELL_LEVEL && Economy.canSellSword(state)) {
                    state = Economy.sellSword(state)
                    continue
                }
            } else {
                // 5. 방지권 비축. 조각이 있으면 조각으로, 없으면 남는 골드로 산다.
                val prevent = Recipes.byId("prevent")
                if (state.inventory.countOf(Item.PREVENT_TICKET) < PREVENT_STOCK) {
                    if (Recipes.canCraft(state, prevent)) {
                        state = Recipes.craft(state, prevent, WeaponFamily.STRAIGHT)
                        continue
                    }
                    if (Economy.canBuyItem(state, Item.PREVENT_TICKET)) {
                        state = Economy.buyItem(state, Item.PREVENT_TICKET)
                        continue
                    }
                }

                // 6. 위험 구간이면 축복서를 한 장 확보한다
                if (sword.level >= RISK_LEVEL && state.inventory.blessingScrolls == 0 &&
                    Economy.canBuyItem(state, Item.BLESSING_SCROLL)
                ) {
                    state = Economy.buyItem(state, Item.BLESSING_SCROLL)
                    continue
                }
            }

            // 7. 재료 확보. +16부터 강화석이 필수다(ForgeCost).
            // 재료 검은 v1.8에서 강화 요구에서 빠졌다 - 보관함의 검은 조합 전용이다.
            val req = ForgeCost.requirementFor(sword.level)
            if (state.forgeStones < req.stones) {
                if (state.gold < STONE_PRICE) {
                    if (Economy.canSellSword(state)) {
                        state = Economy.sellSword(state)
                        continue
                    }
                    break
                }
                state = state.copy(
                    gold = state.gold - STONE_PRICE,
                    forgeStones = state.forgeStones + 1,
                )
                continue
            }

            // 8. 비용을 못 내면 검을 판다
            if (!ForgeEngine.canAttempt(state, UsedItems.NONE)) {
                if (Economy.canSellSword(state)) {
                    state = Economy.sellSword(state)
                    continue
                }
                break
            }

            // 9. 강화. 밀어붙이는 국면에서는 아이템을 아끼지 않는다.
            val targetLevel = sword.level + 1
            val items = UsedItems(
                blessing = pushing &&
                    targetLevel > RISK_LEVEL &&
                    state.inventory.blessingScrolls > 0,
                luckCharm = pushing &&
                    targetLevel > CRITICAL_LEVEL &&
                    state.inventory.luckCharms > 0,
            )
            val result = ForgeEngine.attempt(state, items, rng)
            attempts++
            spins = 0
            state = result.state

            if (result is ForgeResult.Destroyed) {
                state = if (ForgeEngine.canPrevent(state)) {
                    ForgeEngine.applyPrevent(state)
                } else {
                    ForgeEngine.applySalvage(state, rng)
                }
            }
        }

        return RunOutcome(
            bestLevel = state.bestLevel,
            attempts = attempts,
            bailouts = bailouts,
            reachedCap = cap != null && state.bestLevel >= cap,
        )
    }

    fun run(difficulty: Difficulty, runs: Int, maxAttempts: Int, seed: Long): SimReport {
        val rng = Random(seed)
        val outcomes = List(runs) { simulateRun(difficulty, rng, maxAttempts) }
        val levels = outcomes.map { it.bestLevel }.sorted()
        return SimReport(
            difficulty = difficulty,
            runs = runs,
            averageBestLevel = levels.average(),
            medianBestLevel = levels[levels.size / 2],
            p90BestLevel = levels[(levels.size * 90) / 100],
            p99BestLevel = levels[(levels.size * 99) / 100],
            maxBestLevel = levels.last(),
            bankruptcyRate = outcomes.count { it.bailouts > 0 }.toDouble() / runs,
            capRate = outcomes.count { it.reachedCap }.toDouble() / runs,
            averageAttempts = outcomes.map { it.attempts }.average(),
        )
    }
}
