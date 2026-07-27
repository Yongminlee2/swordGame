package com.geomgang.core

import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.random.Random

/** 런 한정 버프. 나가면 사라진다 - 영구 성장은 본편에서만 온다. */
enum class GauntletBuff(val label: String, val blurb: String) {
    ATTACK("맹공", "피해 +20%"),
    TIME("여유", "층 제한 시간 +3초"),
    GOLD("탐욕", "회랑 보상 +50%"),
    CRIT("절개", "치명타 +5%p"),
}

/** 갈림길 선택지. */
sealed interface GauntletChoice {
    /** 런 한정 버프 하나. */
    data class Blessing(val buff: GauntletBuff) : GauntletChoice

    /** 위험 없는 즉시 보상. */
    data class Treasure(val gold: Long, val shards: Int) : GauntletChoice

    /** 다음 층 체력 2배, 보상 4배. */
    data object Cursed : GauntletChoice
}

/**
 * 회랑 런 하나의 상태. 전부 불변이고 전이는 [GauntletEngine] 함수로만 한다.
 *
 * [pendingGold] 는 아직 확정되지 않은 보상이다. 5층마다 보스를 잡으면 [bankedGold] 로
 * 옮겨져 확정된다. 런이 끝나면(시간초과·퇴장) 미확정분은 70%만 들고 나간다.
 */
data class GauntletRun(
    val floor: Int = 1,
    val killsInFloor: Int = 0,
    val timeLeftMillis: Long = GauntletEngine.FLOOR_SECONDS * 1000L,
    val monsterHp: Long = GauntletEngine.monsterHp(1, isBossFloor = false, cursed = false),
    val monsterMaxHp: Long = monsterHp,
    val cursed: Boolean = false,
    val pendingGold: Long = 0,
    val pendingShards: Int = 0,
    val bankedGold: Long = 0,
    val bankedShards: Int = 0,
    val buffs: Set<GauntletBuff> = emptySet(),
    val choosing: Boolean = false,
    val choices: List<GauntletChoice> = emptyList(),
    val over: Boolean = false,
) {
    val isBossFloor: Boolean get() = floor % GauntletEngine.BOSS_EVERY == 0

    val waveSize: Int get() = if (isBossFloor) 1 else GauntletEngine.WAVE_SIZE
}

/**
 * 무한 회랑.
 *
 * 상한 없는 층 + 갈림길 3택 + 5층 체크포인트. 검은 여기서 절대 부서지지 않는다 -
 * 파괴의 공포는 강화에만 있어야 무겁다.
 */
object GauntletEngine {

    /**
     * 한 층의 잡몹 수와 제한 시간.
     *
     * 5초에 5마리는 물리적으로 불가능하다. 마리당 체력은 그대로 두고 마리 수를 줄여
     * "짧고 굵은 한 판"이 되게 했다.
     */
    const val WAVE_SIZE = 2
    const val FLOOR_SECONDS = 5
    const val BOSS_EVERY = 5
    const val HP_GROWTH = 1.35
    const val BASE_HP = 60.0
    const val BASE_GOLD = 40.0
    const val BOSS_HP_MULT = 6.0
    const val CURSED_HP = 2.0
    const val CURSED_REWARD = 4.0

    /** 런 종료 시 미확정 보상에서 들고 나가는 비율. */
    const val LOSS_RATIO = 0.7

    /** 최초 돌파 보상 층. */
    const val VOID_FLOOR = 10
    const val WISP_FLOOR = 25

    const val ATTACK_BUFF = 1.2
    const val TIME_BUFF_MILLIS = 3_000L
    const val GOLD_BUFF = 1.5
    const val CRIT_BUFF = 0.05

    fun start(): GauntletRun = GauntletRun()

    fun monsterHp(floor: Int, isBossFloor: Boolean, cursed: Boolean): Long {
        val base = BASE_HP * HP_GROWTH.pow((floor - 1).toDouble())
        val boss = if (isBossFloor) BOSS_HP_MULT else 1.0
        val curse = if (cursed) CURSED_HP else 1.0
        return (base * boss * curse).roundToLong().coerceAtLeast(1)
    }

    /** 처치 하나의 보상. */
    fun killReward(
        floor: Int,
        isBossFloor: Boolean,
        cursed: Boolean,
        buffs: Set<GauntletBuff>,
    ): Pair<Long, Int> {
        val base = BASE_GOLD * HP_GROWTH.pow((floor - 1).toDouble())
        val boss = if (isBossFloor) BOSS_HP_MULT else 1.0
        val curse = if (cursed) CURSED_REWARD else 1.0
        val buff = if (GauntletBuff.GOLD in buffs) GOLD_BUFF else 1.0
        val gold = (base * boss * curse * buff).roundToLong().coerceAtLeast(1)
        val shards = (1 + floor / 3) * (if (isBossFloor) 4 else 1)
        return gold to shards
    }

    /**
     * 피해를 넣는다. 처치·웨이브 완료·보스 뱅킹까지 한 번에 처리한다.
     *
     * @param hit 이미 계산된 피해(공격력·치명타 포함). ATTACK 버프는 여기서 곱한다.
     */
    fun damage(run: GauntletRun, hit: Long): GauntletRun {
        if (run.over || run.choosing || run.monsterHp <= 0) return run
        val dealt = if (GauntletBuff.ATTACK in run.buffs) {
            (hit * ATTACK_BUFF).roundToLong()
        } else {
            hit
        }
        val hp = run.monsterHp - dealt
        if (hp > 0) return run.copy(monsterHp = hp)

        // 처치
        val (gold, shards) = killReward(run.floor, run.isBossFloor, run.cursed, run.buffs)
        var next = run.copy(
            monsterHp = 0,
            killsInFloor = run.killsInFloor + 1,
            pendingGold = run.pendingGold + gold,
            pendingShards = run.pendingShards + shards,
        )

        if (next.killsInFloor >= next.waveSize) {
            // 층 클리어. 보스 층이면 체크포인트 - 미확정 보상이 확정된다.
            if (next.isBossFloor) {
                next = next.copy(
                    bankedGold = next.bankedGold + next.pendingGold,
                    bankedShards = next.bankedShards + next.pendingShards,
                    pendingGold = 0,
                    pendingShards = 0,
                )
            }
            next = next.copy(choosing = true)
        } else {
            next = next.copy(
                monsterHp = monsterHp(next.floor, next.isBossFloor, next.cursed),
                monsterMaxHp = monsterHp(next.floor, next.isBossFloor, next.cursed),
            )
        }
        return next
    }

    /** 갈림길 선택지 셋. 축복 버프는 난수로 고른다. */
    fun rollChoices(floor: Int, rng: Random): List<GauntletChoice> {
        val buff = GauntletBuff.entries[rng.nextInt(GauntletBuff.entries.size)]
        val treasureGold = (BASE_GOLD * HP_GROWTH.pow(floor.toDouble()) * 2).roundToLong()
        return listOf(
            GauntletChoice.Blessing(buff),
            GauntletChoice.Treasure(treasureGold, 4 + floor / 2),
            GauntletChoice.Cursed,
        )
    }

    /** 갈림길에서 하나를 고르고 다음 층을 연다. */
    fun choose(run: GauntletRun, index: Int): GauntletRun {
        check(run.choosing) { "not choosing" }
        val choice = run.choices.getOrNull(index) ?: run.choices.firstOrNull()
            ?: return openNextFloor(run, cursed = false)

        var next = run
        var cursed = false
        when (choice) {
            is GauntletChoice.Blessing -> next = next.copy(buffs = next.buffs + choice.buff)
            is GauntletChoice.Treasure -> next = next.copy(
                pendingGold = next.pendingGold + choice.gold,
                pendingShards = next.pendingShards + choice.shards,
            )
            is GauntletChoice.Cursed -> cursed = true
        }
        return openNextFloor(next, cursed)
    }

    private fun openNextFloor(run: GauntletRun, cursed: Boolean): GauntletRun {
        val floor = run.floor + 1
        val isBoss = floor % BOSS_EVERY == 0
        val hp = monsterHp(floor, isBoss, cursed)
        val timeBuff = if (GauntletBuff.TIME in run.buffs) TIME_BUFF_MILLIS else 0L
        return run.copy(
            floor = floor,
            killsInFloor = 0,
            cursed = cursed,
            timeLeftMillis = FLOOR_SECONDS * 1000L + timeBuff,
            monsterHp = hp,
            monsterMaxHp = hp,
            choosing = false,
            choices = emptyList(),
        )
    }

    /** 시간이 흐른다. 갈림길에서는 시간이 멈춘다 - 고민은 공짜여야 한다. */
    fun tick(run: GauntletRun, millis: Long): GauntletRun {
        if (run.over || run.choosing) return run
        val left = run.timeLeftMillis - millis
        return if (left <= 0) run.copy(timeLeftMillis = 0, over = true) else run.copy(timeLeftMillis = left)
    }

    /** 정산. 확정분 전액 + 미확정분 70%. */
    fun payout(run: GauntletRun): Pair<Long, Int> =
        (run.bankedGold + (run.pendingGold * LOSS_RATIO).roundToLong()) to
            (run.bankedShards + (run.pendingShards * LOSS_RATIO).toInt())

    /**
     * 층 돌파 마일스톤을 적용한다.
     *
     * 10층 최초 돌파 = 허검 +10 (보관함이 꽉 찼어도 지급한다 - 1회뿐인 보상을 잃게 하지 않는다),
     * 25층 최초 돌파 = 회랑의 정령 알. 최고 기록도 여기서 갱신한다.
     */
    fun applyMilestones(state: GameState, clearedFloor: Int): GameState {
        var s = state
        if (state.gauntletBest < VOID_FLOOR && clearedFloor >= VOID_FLOOR) {
            s = s.copy(storage = s.storage + Sword(WeaponFamily.VOID, 10))
        }
        if (state.gauntletBest < WISP_FLOOR && clearedFloor >= WISP_FLOOR) {
            s = s.copy(pets = Pets.addEgg(s.pets, PetKind.HALL_WISP.id))
        }
        if (clearedFloor > s.gauntletBest) {
            s = s.copy(gauntletBest = clearedFloor)
        }
        return s
    }
}
