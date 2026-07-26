package com.geomgang.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GauntletTest {

    /** 몬스터 하나를 한 방에 잡는 피해. */
    private fun killOne(run: GauntletRun): GauntletRun =
        GauntletEngine.damage(run, run.monsterHp * 2)

    private fun clearFloor(run: GauntletRun): GauntletRun {
        var r = run
        repeat(r.waveSize) { r = killOne(r) }
        return r
    }

    @Test
    fun `층이 오르면 체력이 오르고 보스는 더 세다`() {
        val f1 = GauntletEngine.monsterHp(1, false, false)
        val f2 = GauntletEngine.monsterHp(2, false, false)
        val boss = GauntletEngine.monsterHp(5, true, false)
        assertTrue(f2 > f1)
        assertTrue(boss > GauntletEngine.monsterHp(5, false, false))
    }

    @Test
    fun `저주는 체력 2배 보상 4배`() {
        // 반올림이 각자 붙으므로 비율로 확인한다
        val hpRatio = GauntletEngine.monsterHp(3, false, true).toDouble() /
            GauntletEngine.monsterHp(3, false, false)
        assertEquals(GauntletEngine.CURSED_HP, hpRatio, 0.02)
        val plain = GauntletEngine.killReward(3, false, false, emptySet()).first
        val cursed = GauntletEngine.killReward(3, false, true, emptySet()).first
        assertEquals(GauntletEngine.CURSED_REWARD, cursed.toDouble() / plain, 0.02)
    }

    @Test
    fun `잡몹 5마리를 잡으면 갈림길이 열린다`() {
        var run = GauntletEngine.start()
        repeat(4) {
            run = killOne(run)
            assertFalse(run.choosing)
        }
        run = killOne(run)
        assertTrue(run.choosing)
    }

    @Test
    fun `보스 층은 1마리다`() {
        var run = GauntletEngine.start()
        repeat(4) { // 1~4층
            run = clearFloor(run)
            run = run.copy(choices = GauntletEngine.rollChoices(run.floor, ScriptedIntRandom(1)))
            run = GauntletEngine.choose(run, 1) // 보물 - 버프·저주 없이
        }
        assertEquals(5, run.floor)
        assertTrue(run.isBossFloor)
        assertEquals(1, run.waveSize)
    }

    @Test
    fun `보스를 잡으면 미확정 보상이 확정된다`() {
        var run = GauntletEngine.start()
        repeat(4) {
            run = clearFloor(run)
            run = run.copy(choices = GauntletEngine.rollChoices(run.floor, ScriptedIntRandom(1)))
            run = GauntletEngine.choose(run, 1)
        }
        assertTrue(run.pendingGold > 0)
        run = killOne(run) // 5층 보스
        assertEquals(0, run.pendingGold)
        assertTrue(run.bankedGold > 0)
        assertTrue(run.choosing)
    }

    @Test
    fun `공격 버프는 피해를 늘리고 시간 버프는 다음 층 시간을 늘린다`() {
        val base = GauntletEngine.start()
        val hp = base.monsterHp
        val buffed = base.copy(buffs = setOf(GauntletBuff.ATTACK))
        // 버프 없으면 못 잡는 피해가 버프로는 잡는다
        val notQuite = (hp / GauntletEngine.ATTACK_BUFF).toLong() + 1
        assertTrue(GauntletEngine.damage(base, notQuite).monsterHp > 0)
        assertEquals(1, GauntletEngine.damage(buffed, notQuite).killsInFloor)

        var timed = clearFloor(base.copy(buffs = setOf(GauntletBuff.TIME)))
        timed = timed.copy(choices = GauntletEngine.rollChoices(1, ScriptedIntRandom(1)))
        timed = GauntletEngine.choose(timed, 1)
        assertEquals(
            GauntletEngine.FLOOR_SECONDS * 1000L + GauntletEngine.TIME_BUFF_MILLIS,
            timed.timeLeftMillis,
        )
    }

    @Test
    fun `시간이 다 되면 런이 끝난다`() {
        var run = GauntletEngine.start()
        run = GauntletEngine.tick(run, GauntletEngine.FLOOR_SECONDS * 1000L + 1)
        assertTrue(run.over)
    }

    @Test
    fun `갈림길에서는 시간이 멈춘다`() {
        var run = clearFloor(GauntletEngine.start())
        val before = run.timeLeftMillis
        run = GauntletEngine.tick(run, 10_000)
        assertEquals(before, run.timeLeftMillis)
        assertFalse(run.over)
    }

    @Test
    fun `정산은 확정 전액에 미확정 70%다`() {
        val run = GauntletRun(
            pendingGold = 1000, pendingShards = 10,
            bankedGold = 500, bankedShards = 5,
        )
        val (gold, shards) = GauntletEngine.payout(run)
        assertEquals(500 + 700L, gold)
        assertEquals(5 + 7, shards)
    }

    @Test
    fun `저주를 고르면 다음 층이 저주받는다`() {
        var run = clearFloor(GauntletEngine.start())
        run = run.copy(choices = GauntletEngine.rollChoices(1, ScriptedIntRandom(0)))
        run = GauntletEngine.choose(run, 2) // Cursed
        assertTrue(run.cursed)
        assertEquals(GauntletEngine.monsterHp(2, false, true), run.monsterHp)
        // 그 다음 층은 저주가 풀린다
        var after = clearFloor(run)
        after = after.copy(choices = GauntletEngine.rollChoices(2, ScriptedIntRandom(0)))
        after = GauntletEngine.choose(after, 1)
        assertFalse(after.cursed)
    }

    @Test
    fun `10층 최초 돌파는 허검을 주고 두 번째는 안 준다`() {
        val state = GameState(difficulty = Difficulty.ENDLESS)
        val first = GauntletEngine.applyMilestones(state, 10)
        assertEquals(WeaponFamily.VOID, first.storage.single().family)
        assertEquals(10, first.gauntletBest)
        val second = GauntletEngine.applyMilestones(first, 11)
        assertEquals(1, second.storage.size)
        assertEquals(11, second.gauntletBest)
    }

    @Test
    fun `25층 최초 돌파는 회랑의 정령 알을 준다`() {
        val state = GameState(difficulty = Difficulty.ENDLESS, gauntletBest = 20)
        val after = GauntletEngine.applyMilestones(state, 25)
        assertEquals(1, after.pets.counts[PetKind.HALL_WISP.id])
        // 10층 보상은 이미 지났으므로(gauntletBest 20) 다시 주지 않는다
        assertTrue(after.storage.isEmpty())
    }

    /** nextInt 만 쓰는 간단 난수. */
    private class ScriptedIntRandom(private val value: Int) : kotlin.random.Random() {
        override fun nextBits(bitCount: Int): Int = 0
        override fun nextInt(until: Int): Int = value % until
    }
}
