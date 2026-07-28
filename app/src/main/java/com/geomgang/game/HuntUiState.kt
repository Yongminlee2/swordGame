package com.geomgang.game

import com.geomgang.core.HuntEvent
import com.geomgang.core.Item
import com.geomgang.core.Skill
import com.geomgang.core.Zone

/**
 * 보스를 잡고 얻은 것.
 *
 * 지금까지 이 넷은 조용히 들어갔다. 승리감의 대부분이 "얼마나 벌었나" 인데
 * 화면이 알려 주지 않으면 그 순간이 그냥 지나간다.
 */
data class BossReward(
    val gold: Long,
    val shards: Int,
    val stones: Int,
    /** 떨어진 펫 이름. 안 나왔으면 null. */
    val petName: String? = null,
)

/** 사냥 중일 때 화면이 그리는 것. 사냥터 밖에서는 null 이다. */
data class HuntUiState(
    val zone: Zone,
    val targetName: String,
    /** 접두어("희귀 ") 없는 원본 이름. 스프라이트 시트 매핑에 쓴다. */
    val rawTargetName: String,
    val targetHp: Long,
    val targetMaxHp: Long,
    val isBoss: Boolean,
    /** 보스 제한 시간의 남은 밀리초. 잡몹이면 0. */
    val bossRemainingMillis: Long,
    val killsInZone: Int,
    val killsNeeded: Int,
    val attackPower: Long,
    val combo: Int,
    /** 마지막 타격 피해. 숫자를 튀우는 연출에 쓴다. */
    val lastDamage: Long,
    /** 마지막 타격이 몇 번 들어갔는지. 쌍검은 2다. */
    val lastHits: Int,
    /** 마지막 타격이 치명타였는지. */
    val lastCrit: Boolean,
    /** 마지막 타격에 터진 계열 스킬. 없으면 null. */
    val lastSkill: Skill? = null,
    /** 타격마다 1씩 오르는 일련번호. 화면이 팝업 애니메이션 트리거로 쓴다. */
    val hitSeq: Long,
    /** 지금 대상이 희귀 몬스터인지. 금색 틴트를 입힌다. */
    val isRare: Boolean,
    /** 방금 처치로 번 골드. 처치 직후가 아니면 0. */
    val lastKillGold: Long,
    /** 보스 제한 시간을 넘겨 실패했는지. */
    val bossFailed: Boolean,
    /** 방금 구역을 깼는지. */
    val zoneCleared: Boolean,
    /** 지금 몬스터에 붙은 이벤트(보물·미믹·정예·알). 없으면 null. */
    val event: HuntEvent? = null,
    /** 보물 몬스터의 남은 밀리초. 보물이 아니면 0. */
    val eventRemainingMillis: Long = 0,
    /** 골든타임 남은 밀리초. 0이면 꺼진 상태. */
    val goldenRemainingMillis: Long = 0,
    /** 떠돌이 상인이 파는 물건. null 이면 상인 없음. */
    val merchantOffer: Item? = null,
    /** 상인의 할인가. */
    val merchantPrice: Long = 0,
    /** 화면에 금덩이가 떠 있는지. */
    val nugget: Boolean = false,
    /** 지금 즉시 재도전하는 값. 놓친 직후에만 뜻이 있다. */
    val retryPrice: Long = 0,
    /** 그 값을 낼 수 있는지. */
    val canRetry: Boolean = false,
    /** 방금 보스에게서 얻은 것. 승리 팝업이 그대로 띄운다. */
    val bossReward: BossReward? = null,
) {
    val hpRatio: Float get() = (targetHp.toFloat() / targetMaxHp).coerceIn(0f, 1f)

    val bossTimeRatio: Float
        get() = if (!isBoss) 0f
        else (bossRemainingMillis.toFloat() / (zone.bossSeconds * 1000f)).coerceIn(0f, 1f)

    /** 보스가 급해졌는지 - 남은 시간 25% 이하. 붉은 틴트 + 흔들림. */
    val enraged: Boolean
        get() = isBoss && targetHp > 0 && bossTimeRatio <= 0.25f
}
