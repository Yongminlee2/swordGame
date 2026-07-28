package com.geomgang.core

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * **강화가 주인공인가.**
 *
 * 강화 한 번은 1초인데 그 한 번에 드는 재료가 전부 사냥에서만 나오면,
 * 실제로 하는 일은 사냥이 되고 강화는 그 사이에 잠깐 누르는 것이 된다.
 * v1.6까지가 그랬다 — 후반에는 보스 하나를 잡아야 강화 한 번이었다.
 *
 * 이 테스트가 그 비율을 못박는다. [BossTempoTest] 가 5초 보스전을 지키는 것과 같은 자리다.
 */
class ForgeTempoTest {

    /**
     * 구역 완주 한 번(잡몹 [Zone.MONSTERS_BEFORE_BOSS] 마리 + 보스 1)으로 얻는 강화석 기대값.
     *
     * 펫·업그레이드 보정은 넣지 않는다 — **아무 보정 없는 맨몸**이 기준선이어야 한다.
     */
    private fun stonesPerRun(zone: Zone): Double =
        Zone.MONSTERS_BEFORE_BOSS * ForgeCost.MOB_STONE_CHANCE + zone.bossStones

    @Test
    fun `구역 하나를 돌면 그 단계 강화를 다섯 번은 굴릴 수 있다`() {
        for (zone in Zone.entries) {
            val need = ForgeCost.requirementFor(zone.recommendedLevel).stones
            if (need == 0) continue // 강화석을 안 먹는 저단계 구역
            val runs = stonesPerRun(zone) / need
            assertTrue(
                "${zone.displayName}: 완주 1회로 %.1f번뿐 (필요 %d개, 수확 %.1f개)"
                    .format(runs, need, stonesPerRun(zone)),
                runs >= ForgeCost.RUNS_PER_ZONE_CLEAR,
            )
        }
    }

    @Test
    fun `요구량은 상한에서 멈춘다`() {
        // 무한 구간에서 요구가 발산하면 후반이 통째로 사냥 게임이 된다.
        for (level in 0..200) {
            assertTrue(
                "level=$level",
                ForgeCost.requirementFor(level).stones <= ForgeCost.MAX_STONES,
            )
        }
        assertTrue(ForgeCost.requirementFor(200).stones == ForgeCost.MAX_STONES)
    }

    @Test
    fun `요구량은 줄지 않는다`() {
        // 단계가 오르는데 요구가 낮아지면 곡선이 뒤집힌 것이다.
        var prev = 0
        for (level in 0..60) {
            val now = ForgeCost.requirementFor(level).stones
            assertTrue("level=$level: $prev -> $now", now >= prev)
            prev = now
        }
    }

    @Test
    fun `가장 깊은 구역도 보스 하나로 강화 한 번이 되지 않는다`() {
        // v1.6의 증상을 그대로 겨눈다: 끝 구역에서 보스 1 = 강화 1 이면 실패다.
        val last = Zone.entries.last()
        val need = ForgeCost.requirementFor(last.recommendedLevel).stones
        assertTrue(
            "끝 구역 보스가 ${last.bossStones}개를 주는데 한 번에 ${need}개가 든다",
            last.bossStones >= need * 2,
        )
    }
}
