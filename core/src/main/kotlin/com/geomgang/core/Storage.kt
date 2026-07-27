package com.geomgang.core

import kotlin.random.Random

/**
 * 무기 보관함.
 *
 * 검을 한 자루만 들 수 있던 것을 여러 자루 보관할 수 있게 한다.
 * **조합**과 **재료 강화**가 둘 다 "검이 여러 자루 있다"를 전제로 하므로 이것이 토대다.
 *
 * 보관함이 채워지는 경로는 셋이다 — 사냥 드롭, 상점 구매, 조합 결과.
 * 드롭이 없으면 보관함이 늘 비어 있어 조합이 죽은 기능이 된다.
 */
object Storage {

    /** 보관 가능한 자루 수. */
    const val CAPACITY = 24

    fun isFull(state: GameState): Boolean = state.storage.size >= CAPACITY

    fun canStore(state: GameState): Boolean = state.sword != null && !isFull(state)

    /** 들고 있는 검을 보관함에 넣는다. 손은 비게 된다. */
    fun store(state: GameState): GameState {
        val sword = state.sword
        check(sword != null) { "no sword to store" }
        check(!isFull(state)) { "storage is full" }
        return state.copy(sword = null, storage = state.storage + sword)
    }

    /**
     * 보관함의 검을 든다.
     *
     * 이미 들고 있던 검은 보관함으로 들어간다 — 자리를 맞바꾸는 것이라
     * 보관함이 꽉 차 있어도 교체는 된다.
     */
    fun equip(state: GameState, index: Int): GameState {
        require(index in state.storage.indices) { "no sword at $index" }
        // 파괴 대기 중에는 손을 대지 않는다. 결과가 아직 확정되지 않았다.
        check(state.pendingDestroy == null) { "cannot equip while a destroy is pending" }

        val picked = state.storage[index]
        val rest = state.storage.toMutableList().apply { removeAt(index) }
        val held = state.sword
        if (held != null) rest.add(held)
        return state.copy(
            sword = picked,
            storage = rest,
            bestLevel = maxOf(state.bestLevel, picked.level),
        )
    }

    /** 보관함의 검을 판다. */
    fun sell(state: GameState, index: Int): GameState {
        require(index in state.storage.indices) { "no sword at $index" }
        val sword = state.storage[index]
        return state.copy(
            gold = state.gold + Economy.sellPrice(sword.level),
            storage = state.storage.toMutableList().apply { removeAt(index) },
        )
    }

    /**
     * 보관함의 검을 부숴 조각과 강화석으로 바꾼다.
     *
     * 팔면 골드, 부수면 조각·강화석이다. 고단계 강화가 강화석을 먹으므로
     * "파는 것보다 부수는 것이 나은 순간"이 생긴다.
     */
    fun scrap(state: GameState, index: Int): GameState {
        require(index in state.storage.indices) { "no sword at $index" }
        val sword = state.storage[index]
        return state.copy(
            shards = state.shards + scrapShards(sword),
            forgeStones = state.forgeStones + scrapStones(sword),
            storage = state.storage.toMutableList().apply { removeAt(index) },
        )
    }

    /** 부숴서 나오는 조각. 단계가 높을수록 많다. */
    fun scrapShards(sword: Sword): Int = (2 + sword.level * 2).coerceAtLeast(1)

    /** 부숴서 나오는 강화석. 단계가 높을수록 많지만 상한이 있다. */
    fun scrapStones(sword: Sword): Int = (1 + sword.level / 8).coerceAtMost(3)
}

/**
 * 사냥에서 검이 떨어지는 규칙.
 *
 * 드롭이 있어야 보관함이 채워지고, 그래야 조합과 재료 강화가 살아난다.
 * 동시에 사냥 보상이 골드·조각뿐이던 것보다 두터워진다.
 */
object SwordDrop {

    /** 잡몹이 검을 떨어뜨릴 확률. */
    const val NORMAL_CHANCE = 0.07

    /** 희귀 몬스터가 검을 떨어뜨릴 확률. */
    const val RARE_CHANCE = 0.30

    /**
     * 떨어진 검. 보관함이 꽉 차 있으면 null 을 준다 —
     * 화면이 "보관함이 꽉 찼다"고 알려 줄 수 있도록 조용히 버리지 않는다.
     *
     * @param families 지금 나올 수 있는 계열. 해금된 것만 넘긴다
     */
    fun roll(
        zone: Zone,
        isRare: Boolean,
        isBoss: Boolean,
        families: List<WeaponFamily>,
        rng: Random,
        /** 드롭률 배수. 행운아(고유검) 같은 보정이 들어온다. */
        chanceMult: Double = 1.0,
    ): Sword? {
        if (families.isEmpty()) return null
        val chance = when {
            isBoss -> 1.0
            isRare -> RARE_CHANCE
            else -> NORMAL_CHANCE
        } * chanceMult
        if (rng.nextDouble() >= chance) return null

        val family = families[rng.nextInt(families.size)]
        val level = dropLevel(zone, isBoss, rng)
        return Sword(family, level)
    }

    /**
     * 떨어진 검의 강화 단계.
     *
     * 구역 권장 단계 근처로 나온다. 보스는 조금 더 좋은 것을 준다.
     * 권장보다 크게 높은 검이 나오면 강화할 이유가 사라지므로 위쪽을 좁게 잡는다.
     */
    fun dropLevel(zone: Zone, isBoss: Boolean, rng: Random): Int {
        val base = zone.recommendedLevel
        val jitter = if (isBoss) rng.nextInt(0, 3) else rng.nextInt(-3, 2)
        return (base + jitter).coerceIn(0, RateTable.MAX_FINITE_LEVEL)
    }
}
