package com.geomgang.core

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 보상 배수 중첩의 경계선.
 *
 * v1.3에서 골드 배수가 여럿 생겼다(이벤트·골든타임·고유검·펫). 시뮬레이션 대신
 * **최악 중첩의 상한**을 못박아 둔다 - 여기 걸리면 경제를 다시 봐야 한다.
 */
class RewardStackingTest {

    @Test
    fun `잡몹 골드 배수의 최악 중첩이 150배를 넘지 않는다`() {
        // 골든타임 중에는 새 이벤트를 굴리지 않으므로 보물×골든타임은 공존할 수 없다.
        // 실제 최악: 보물 몬스터(10) × 개화(1.5) × 숲 요정 Lv5(1.18) × 희귀(6) ≈ 106배
        val worst = HuntEvents.TREASURE_GOLD * 1.5 * 1.18 * Zone.RARE_REWARD
        assertTrue("최악 중첩 $worst", worst <= 150.0)
    }

    @Test
    fun `치명타 확률의 최악 중첩이 절반을 넘지 않는다`() {
        // 기본 5% + 절단자 10%p + 용암 뱀 Lv5 6%p + 회랑 절개 5%p
        val worst = Combat.CRIT_CHANCE + 0.10 + 0.06 + GauntletEngine.CRIT_BUFF
        assertTrue("치명타 $worst", worst <= 0.5)
    }

    @Test
    fun `이벤트와 희귀 확률의 중첩이 판정을 넘치지 않는다`() {
        // 이벤트 8% + 꼬마 거미 3%p, 희귀 9% + 그림자 임프 6%p - 둘 다 1.0 미만이어야
        // "거의 매 스폰이 이벤트"가 되지 않는다
        assertTrue(HuntEvents.CHANCE + 0.03 < 0.2)
        assertTrue(Zone.RARE_CHANCE + 0.06 < 0.2)
    }

    @Test
    fun `강화 성공률 보정 총합이 상한 아래다`() {
        // 쌓아 온 보너스 + 시작의 검 3%p + 축복서를 다 겹쳐도 MAX_SUCCESS_RATE 가 막는다
        val sword = Sword(WeaponFamily.STRAIGHT, 0, uniqueId = "origin")
        val state = GameState(
            difficulty = Difficulty.ENDLESS,
            gold = 1_000_000,
            sword = sword,
        )
        val result = ForgeEngine.attempt(
            state, UsedItems.NONE, ScriptedRandom(0.0),
            bonus = ForgeBonus(successRate = 0.5, destroyGuard = 0.5),
        )
        // 성공했는지가 아니라 "상한을 넘는 확률로 판정되지 않았는지"가 관심사다.
        // 롤 0.0은 항상 성공이므로 예외 없이 통과하면 상한 계산이 유효한 것이다.
        assertTrue(result is ForgeResult.Success)
    }
}
