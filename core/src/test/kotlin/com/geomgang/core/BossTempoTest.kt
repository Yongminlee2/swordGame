package com.geomgang.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 5초 보스전이 실제로 성립하는지.
 *
 * 제한 시간을 20초대에서 5초로 줄이면 쓸 수 있는 탭 수가 4~5배 줄어든다.
 * 체력을 그대로 두면 권장 단계에서 아무리 잘 눌러도 못 잡는다 — 그래서
 * 시간을 줄인 비율만큼 체력도 줄였다. 이 테스트가 그 균형을 못박는다.
 */
class BossTempoTest {

    @Test
    fun `모든 구역 보스가 5초다`() {
        for (zone in Zone.entries) {
            assertEquals("${zone.displayName}", 5, zone.bossSeconds)
        }
    }

    /**
     * 보스 체력은 **권장 단계 + 2**를 기준으로 잡았다.
     *
     * 권장 단계는 "여기부터 해 볼 만하다"는 힌트이고, 실제 플레이는 조금 넘겨서 온다.
     * 권장 단계에 딱 맞춰 반드시 잡히게 만들면 강화가 사냥의 문턱이 되지 못한다.
     */
    private val COMFORT_MARGIN = 2

    @Test
    fun `권장보다 두 단계 높은 검으로 모든 구역 보스를 잡을 수 있다`() {
        for (zone in Zone.entries) {
            val level = zone.recommendedLevel + COMFORT_MARGIN
            // 직검은 콤보 보너스가 없어 가장 불리하다. 최악 조건으로 검증한다.
            val sword = Sword(WeaponFamily.STRAIGHT, level)
            assertTrue(
                "${zone.displayName}: +$level 직검으로 못 잡는다 (체력 ${zone.bossHp})",
                Combat.canBeatBoss(sword, zone),
            )
        }
    }

    @Test
    fun `권장 단계 그대로는 대부분 문턱에 걸린다`() {
        // 강화가 사냥의 문턱이어야 한다. 아무 검으로나 다 잡히면 강화할 이유가 없다.
        val blocked = Zone.entries.count { zone ->
            !Combat.canBeatBoss(Sword(WeaponFamily.STRAIGHT, zone.recommendedLevel), zone)
        }
        assertTrue("권장 단계 검이 전 구역을 다 잡는다", blocked >= 5)
    }

    @Test
    fun `보스에게 강한 계열은 한 단계 덜 올려도 된다`() {
        // 성검(보스 1.6배)은 같은 단계에서 직검보다 유리해야 한다 - 계열 선택에 뜻이 생긴다.
        val zone = Zone.CAVE
        val level = zone.recommendedLevel + COMFORT_MARGIN - 1
        assertTrue(Combat.canBeatBoss(Sword(WeaponFamily.HOLY, level), zone))
    }

    @Test
    fun `회랑 층은 5초에 두 마리다`() {
        assertEquals(5, GauntletEngine.FLOOR_SECONDS)
        assertEquals(2, GauntletEngine.WAVE_SIZE)
    }

    @Test
    fun `회랑 첫 층은 권장 진입 단계 검으로 시간 안에 깰 수 있다`() {
        // 회랑은 화산(권장 +12) 클리어 후 열린다. 그 단계 검으로 1층은 여유 있어야 한다.
        val sword = Sword(WeaponFamily.STRAIGHT, 12)
        val run = GauntletEngine.start()
        val perTap = Combat.hit(sword, combo = Int.MAX_VALUE / 2, isBoss = false).damage
        val tapsIn5s = (GauntletEngine.FLOOR_SECONDS * 1000L /
            Combat.minTapMillis(sword)).toInt()
        val needed = run.monsterMaxHp * GauntletEngine.WAVE_SIZE
        assertTrue(
            "1층 체력 합 $needed / 5초 딜 ${perTap * tapsIn5s}",
            perTap * tapsIn5s >= needed,
        )
    }
}
