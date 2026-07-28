package com.geomgang.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IdleRewardsTest {

    private fun state(cleared: Set<String> = emptySet(), gold: Long = 0, stones: Int = 0) =
        GameState(
            difficulty = Difficulty.ENDLESS,
            gold = gold,
            forgeStones = stones,
            adventure = AdventureState(clearedZoneIds = cleared),
        )

    @Test
    fun `짧게 나갔다 오면 보상이 없다`() {
        assertNull(IdleRewards.rewardFor(state(), 0))
        assertNull(IdleRewards.rewardFor(state(), IdleRewards.MIN_SECONDS - 1))
        assertNotNull(IdleRewards.rewardFor(state(), IdleRewards.MIN_SECONDS))
    }

    @Test
    fun `기준 구역은 깬 구역 중 가장 깊은 곳이다`() {
        assertEquals(Zone.entries.first(), IdleRewards.baseZone(state()))
        assertEquals(
            Zone.CAVE,
            IdleRewards.baseZone(state(cleared = setOf(Zone.MEADOW.id, Zone.FOREST.id, Zone.CAVE.id))),
        )
    }

    @Test
    fun `깊은 구역을 깰수록 수익이 커진다`() {
        val shallow = IdleRewards.rewardFor(state(cleared = setOf(Zone.MEADOW.id)), 3600)!!
        val deep = IdleRewards.rewardFor(state(cleared = setOf(Zone.MEADOW.id, Zone.FOREST.id)), 3600)!!
        assertTrue("${shallow.gold} < ${deep.gold}", shallow.gold < deep.gold)
    }

    @Test
    fun `1분에 잡몹 한 마리 값이다`() {
        val reward = IdleRewards.rewardFor(state(), 600)!!
        assertEquals(Zone.MEADOW.baseGold * 10, reward.gold)
    }

    @Test
    fun `아무리 오래 비워도 상한에서 멈춘다`() {
        val capped = IdleRewards.rewardFor(state(), IdleRewards.MAX_SECONDS)!!
        val longer = IdleRewards.rewardFor(state(), IdleRewards.MAX_SECONDS * 100)!!
        assertEquals(IdleRewards.MAX_SECONDS, longer.seconds)
        assertEquals(capped.gold, longer.gold)
        assertEquals(capped.stones, longer.stones)
    }

    @Test
    fun `강화석은 시간 단위로만 쌓인다`() {
        val perHour = IdleRewards.STONES_PER_HOUR
        assertEquals(0, IdleRewards.rewardFor(state(), 3599)!!.stones)
        assertEquals(perHour, IdleRewards.rewardFor(state(), 3600)!!.stones)
        assertEquals(perHour * 3, IdleRewards.rewardFor(state(), 3600 * 3)!!.stones)
        // 상한이 8시간이므로 한 번 복귀로 얻는 강화석도 8시간치가 끝이다
        assertEquals(
            perHour * 8,
            IdleRewards.rewardFor(state(), IdleRewards.MAX_SECONDS * 5)!!.stones,
        )
    }

    @Test
    fun `시계를 되돌려도 보상이 생기지 않는다`() {
        assertNull(IdleRewards.rewardFor(state(), -99999))
    }

    @Test
    fun `보상을 반영하면 골드와 강화석이 늘어난다`() {
        val before = state(gold = 100, stones = 2)
        val reward = IdleRewards.rewardFor(before, 3600)!!
        val after = IdleRewards.apply(before, reward)

        assertEquals(100 + reward.gold, after.gold)
        assertEquals(2 + reward.stones, after.forgeStones)
        // 다른 것은 건드리지 않는다
        assertEquals(before.copy(gold = after.gold, forgeStones = after.forgeStones), after)
    }

    @Test
    fun `손으로 잡는 편이 훨씬 빠르다`() {
        // 자리비움 1분 = 잡몹 1마리. 실제 사냥은 1분에 그보다 훨씬 많이 잡는다.
        // 이 관계가 뒤집히면 사냥터를 갈 이유가 사라진다.
        assertTrue(IdleRewards.KILLS_PER_MINUTE <= 1)
    }

    @Test
    fun `걸린 시간을 사람이 읽는 문구로 바꾼다`() {
        assertEquals("5분", IdleRewards.durationText(300))
        assertEquals("1시간", IdleRewards.durationText(3600))
        assertEquals("3시간 20분", IdleRewards.durationText(3600 * 3 + 1200))
    }
}
