package com.geomgang.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HuntEventsTest {

    // --- 발생 판정 ---

    @Test
    fun `발생 롤이 기준 미만이면 이벤트가 나온다`() {
        assertNotNull(HuntEvents.roll(chanceRoll = 0.079, pickRoll = 0.5))
    }

    @Test
    fun `발생 롤이 기준 이상이면 null`() {
        assertNull(HuntEvents.roll(chanceRoll = HuntEvents.CHANCE, pickRoll = 0.5))
        assertNull(HuntEvents.roll(chanceRoll = 0.5, pickRoll = 0.5))
    }

    // --- 가중치 추첨 ---

    @Test
    fun `추첨 롤 0은 첫 이벤트, 1 직전은 마지막 이벤트`() {
        assertEquals(HuntEvent.entries.first(), HuntEvents.pick(0.0))
        assertEquals(HuntEvent.entries.last(), HuntEvents.pick(0.999999))
    }

    @Test
    fun `가중치 구간 경계가 누적 합과 일치한다`() {
        val total = HuntEvent.entries.sumOf { it.weight }.toDouble()
        var acc = 0
        for (e in HuntEvent.entries) {
            // 구간 시작 직후와 끝 직전 모두 이 이벤트여야 한다
            assertEquals(e, HuntEvents.pick((acc + 0.001) / total))
            assertEquals(e, HuntEvents.pick((acc + e.weight - 0.001) / total))
            acc += e.weight
        }
    }

    @Test
    fun `유성우는 가중치가 가장 낮다`() {
        assertTrue(HuntEvent.METEOR_SHOWER.weight <= HuntEvent.entries.minOf { it.weight })
    }

    // --- 배수표 ---

    @Test
    fun `몬스터 이벤트는 네 가지다`() {
        val monsterEvents = HuntEvent.entries.filter { HuntEvents.isMonsterEvent(it) }
        assertEquals(
            setOf(HuntEvent.TREASURE, HuntEvent.MIMIC, HuntEvent.ELITE, HuntEvent.STRANGE_EGG),
            monsterEvents.toSet(),
        )
    }

    @Test
    fun `체력 배수 - 미믹 2배 정예 3배 그 외 1배`() {
        assertEquals(HuntEvents.MIMIC_HP, HuntEvents.hpMultOf(HuntEvent.MIMIC), 0.0)
        assertEquals(HuntEvents.ELITE_HP, HuntEvents.hpMultOf(HuntEvent.ELITE), 0.0)
        assertEquals(1.0, HuntEvents.hpMultOf(HuntEvent.TREASURE), 0.0)
        assertEquals(1.0, HuntEvents.hpMultOf(null), 0.0)
    }

    @Test
    fun `보상 배수 - 보물 10배 정예 5배 그 외 1배`() {
        assertEquals(HuntEvents.TREASURE_GOLD, HuntEvents.rewardMultOf(HuntEvent.TREASURE), 0.0)
        assertEquals(HuntEvents.ELITE_REWARD, HuntEvents.rewardMultOf(HuntEvent.ELITE), 0.0)
        assertEquals(1.0, HuntEvents.rewardMultOf(HuntEvent.MIMIC), 0.0)
        assertEquals(1.0, HuntEvents.rewardMultOf(null), 0.0)
    }

    @Test
    fun `금덩이 골드는 구역이 깊을수록 크고 최소 1이다`() {
        assertTrue(HuntEvents.nuggetGold(Zone.ENDLESS_HALL) > HuntEvents.nuggetGold(Zone.MEADOW))
        assertTrue(HuntEvents.nuggetGold(Zone.MEADOW) >= 1)
    }

    @Test
    fun `골든타임과 상인은 몬스터 이벤트가 아니다`() {
        assertFalse(HuntEvents.isMonsterEvent(HuntEvent.GOLDEN_TIME))
        assertFalse(HuntEvents.isMonsterEvent(HuntEvent.MERCHANT))
        assertFalse(HuntEvents.isMonsterEvent(HuntEvent.GOLD_NUGGET))
        assertFalse(HuntEvents.isMonsterEvent(HuntEvent.METEOR_SHOWER))
    }
}
