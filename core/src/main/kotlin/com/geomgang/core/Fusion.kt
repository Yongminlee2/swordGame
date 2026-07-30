package com.geomgang.core

import kotlin.math.roundToLong
import kotlin.random.Random

/**
 * 조합.
 *
 * 보관함의 검 **두 자루**를 녹여 한 자루로 만든다.
 *
 * v2.1에서 2~4자루 · 최고단계+보너스 · 다수결 규칙을 전부 걷어냈다. 규칙이 셋이면
 * 결과를 예측하려고 표를 외워야 한다. 이제 규칙은 하나다 —
 * **결과 단계 = 두 단계의 평균(내림), 계열은 조합표가 정한다.**
 * 조합은 단계를 올리는 장치가 아니라 계열을 만드는 장치다.
 */
object Fusion {

    /** 재료는 정확히 두 자루다. */
    const val MIN_MATERIALS = 2
    const val MAX_MATERIALS = 2

    fun canFuse(state: GameState, indices: List<Int>): Boolean {
        if (indices.size != MIN_MATERIALS) return false
        if (indices.distinct().size != indices.size) return false
        if (indices.any { it !in state.storage.indices }) return false
        // 고유검은 녹일 수 없다. 실수 한 번으로 전설이 사라지면 안 된다.
        // 용검(전설)도 같다 — 결과가 +20 으로 깎이므로 녹이면 **반드시 손해**고,
        // 되돌리려면 마검·성검을 +20 까지 다시 올려야 한다.
        if (indices.any { !meltable(state.storage[it]) }) return false
        // 만들어지는 것이 없으면 조합이 아니다. 표(또는 고유검 레시피)에 있어야 한다.
        if (resultOrNull(indices.map { state.storage[it] }, state.essences) == null) return false
        return state.gold >= cost(state, indices)
    }

    /**
     * 이 검을 녹일 수 있는지.
     *
     * 화면도 같은 판단을 써야 한다 — 목록에서 고를 수 있는데 버튼이 안 눌리면
     * 왜 안 되는지 알 길이 없다.
     */
    fun meltable(sword: Sword): Boolean = sword.uniqueId == null && !sword.isLegend()

    /** 조합 비용. 재료 중 가장 좋은 검 판매가의 절반이다. */
    fun cost(state: GameState, indices: List<Int>): Long =
        costOf(indices.mapNotNull { state.storage.getOrNull(it) })

    /**
     * 재료 목록만으로 비용을 낸다.
     *
     * 화면은 [GameState] 를 들고 있지 않으므로 이 형태가 필요하다.
     */
    fun costOf(materials: List<Sword>): Long {
        if (materials.isEmpty()) return 0
        val best = materials.maxOf { it.level }
        return (Economy.sellPrice(best) * 0.5).roundToLong()
    }

    /** 조합 결과를 미리 계산한다. 화면이 "무엇이 나오는지" 보여 주기 위해 필요하다. */
    fun preview(state: GameState, indices: List<Int>): Sword? {
        val materials = indices.mapNotNull { state.storage.getOrNull(it) }
        if (materials.size != MIN_MATERIALS) return null
        return resultOrNull(materials, state.essences)
    }

    /**
     * 재료 둘로 만들어지는 검. 만들어지는 것이 없으면 null.
     *
     * 우선순위는 둘이다.
     * 1. **숨은 고유검 레시피** — 재료·정수가 [UniqueSwords] 와 맞으면 고유검
     * 2. **조합표**([FusionTable]) — 계열 집합이 표에 있으면 그 계열
     *
     * 결과 단계 = **(a + b) ÷ 2 내림.** 고유검만 최고 단계를 따른다 —
     * 레시피가 이미 단계 하한을 요구하므로 평균으로 또 깎으면 이중 벌이다.
     * 별은 이어지지 않는다 — 녹여서 새로 만드는 것이므로 0부터다.
     */
    fun resultOrNull(materials: List<Sword>, essences: Map<String, Int> = emptyMap()): Sword? {
        if (materials.size != MIN_MATERIALS) return null

        UniqueSwords.match(materials, essences)?.let { recipe ->
            return Sword(
                family = recipe.resultFamily,
                level = materials.maxOf { it.level },
                stars = 0,
                uniqueId = recipe.id,
            )
        }

        val family = FusionTable.resultFor(materials.map { it.family }.toSet()) ?: return null
        return Sword(
            family = family,
            level = materials.sumOf { it.level } / 2,
            stars = 0,
        )
    }

    /** 재료를 소모하고 결과 검을 보관함에 넣는다. 고유검이면 정수도 차감한다. */
    fun fuse(state: GameState, indices: List<Int>): GameState {
        check(canFuse(state, indices)) { "cannot fuse with $indices" }
        val materials = indices.map { state.storage[it] }
        val result = checkNotNull(resultOrNull(materials, state.essences))
        val remaining = state.storage.filterIndexed { i, _ -> i !in indices }

        val essencesLeft = if (result.uniqueId != null) {
            val recipe = checkNotNull(UniqueSwords.byId(result.uniqueId))
            state.essences.toMutableMap().apply {
                for ((zoneId, count) in recipe.essences) {
                    val next = (this[zoneId] ?: 0) - count
                    if (next > 0) this[zoneId] = next else remove(zoneId)
                }
            }
        } else {
            state.essences
        }

        return state.copy(
            gold = state.gold - cost(state, indices),
            storage = remaining + result,
            bestLevel = maxOf(state.bestLevel, result.level),
            essences = essencesLeft,
        )
    }
}

/**
 * 재료 강화.
 *
 * 강화할 때 보관함의 검을 함께 태워 성공률을 올린다.
 * 사냥에서 얻은 검이 "쓸 데 없는 하급품"이 아니라 **연료**가 된다.
 *
 * 축복서와 달리 상한이 있고 재료를 여럿 넣을 수 있어, 고단계에서 확률을
 * 눈에 보이게 끌어올리는 유일한 수단이다.
 */
object MaterialBoost {

    /** 한 번에 넣을 수 있는 재료 수. */
    const val MAX_MATERIALS = 3

    /** 재료 한 자루가 줄 수 있는 최대 성공률(%p). */
    const val PER_MATERIAL_CAP = 0.06

    /** 재료를 다 넣어도 넘을 수 없는 총 상한(%p). */
    const val TOTAL_CAP = 0.18

    /**
     * 재료들이 더해 주는 성공률.
     *
     * 단계가 높은 검이 더 큰 보정을 준다 — 아무 검이나 넣어도 되면
     * 좋은 검을 태우는 결단이 의미를 잃는다.
     */
    fun bonusFor(materials: List<Sword>): Double {
        if (materials.isEmpty()) return 0.0
        val sum = materials.take(MAX_MATERIALS).sumOf {
            (0.01 * (it.level + 1)).coerceAtMost(PER_MATERIAL_CAP)
        }
        return sum.coerceAtMost(TOTAL_CAP)
    }

    fun canUse(state: GameState, indices: List<Int>): Boolean {
        if (indices.size > MAX_MATERIALS) return false
        if (indices.distinct().size != indices.size) return false
        return indices.all { it in state.storage.indices }
    }

    /** 재료를 소모한다. 강화 성공 여부와 무관하게 태워진다. */
    fun consume(state: GameState, indices: List<Int>): GameState =
        state.copy(storage = state.storage.filterIndexed { i, _ -> i !in indices })
}

/**
 * 스타포스 — 특수강화.
 *
 * 강화 단계와 **별개의 계층**이다. 단계는 파괴 위험을 안고 올리고,
 * 별은 파괴 없이 조각과 골드를 태워 올린다.
 * 실패하면 별 하나를 잃을 뿐 검은 부서지지 않는다 — 그래야 두 계층의 긴장이 겹치지 않는다.
 */
object StarForce {

    /** 별을 붙일 수 있는 최소 강화 단계. */
    const val MIN_LEVEL = 10

    /**
     * 최대 별.
     *
     * 10개로 늘려 봤다가 5개로 되돌렸다. 계단을 잘게 나누면 오르는 맛이 자주 오지만,
     * 별 다섯 개가 한눈에 읽히는 것만 못했다. ★★★☆☆ 는 세어 보지 않아도 보인다.
     */
    const val MAX_STARS = 5

    /** 별 하나가 올려 주는 공격력 비율. 다섯 개를 다 채우면 1.7배다. */
    const val ATTACK_PER_STAR = 0.14

    fun canStar(sword: Sword?): Boolean =
        sword != null && sword.level >= MIN_LEVEL && sword.stars < MAX_STARS

    /** 다음 별을 붙이는 데 드는 조각. 별이 늘수록 급격히 비싸진다. */
    fun shardCost(sword: Sword): Int = (12 * (sword.stars + 1) * (sword.stars + 1))

    /** 다음 별을 붙이는 데 드는 골드. */
    fun goldCost(sword: Sword): Long =
        (Economy.upgradeCost(sword.level) * (sword.stars + 1)).coerceAtLeast(1)

    /** 다음 별의 성공률. 뒤로 갈수록 가팔라진다. */
    fun successRate(sword: Sword): Double = when (sword.stars) {
        0 -> 0.80
        1 -> 0.62
        2 -> 0.45
        3 -> 0.30
        else -> 0.18
    }

    fun canAfford(state: GameState): Boolean {
        val sword = state.sword ?: return false
        if (!canStar(sword)) return false
        return state.shards >= shardCost(sword) && state.gold >= goldCost(sword)
    }

    /** 별 강화 결과. */
    sealed interface Result {
        val state: GameState

        data class Up(override val state: GameState, val stars: Int) : Result

        /** 실패. 별이 하나 줄어든다. 0이었으면 그대로다. */
        data class Down(override val state: GameState, val stars: Int) : Result
    }

    /**
     * 별을 하나 올려 본다.
     *
     * 검은 절대 부서지지 않는다. 비용은 성공·실패와 무관하게 빠진다.
     */
    fun attempt(state: GameState, rng: Random): Result {
        val sword = state.sword
        check(sword != null && canAfford(state)) { "cannot star in this state" }

        val paid = state.copy(
            gold = state.gold - goldCost(sword),
            shards = state.shards - shardCost(sword),
        )

        return if (rng.nextDouble() < successRate(sword)) {
            val next = sword.copy(stars = sword.stars + 1)
            Result.Up(paid.copy(sword = next), next.stars)
        } else {
            val next = sword.copy(stars = (sword.stars - 1).coerceAtLeast(0))
            Result.Down(paid.copy(sword = next), next.stars)
        }
    }

    /** 별이 곱해 주는 공격력 배수. */
    fun attackMultiplier(sword: Sword?): Double =
        1.0 + (sword?.stars ?: 0) * ATTACK_PER_STAR
}
