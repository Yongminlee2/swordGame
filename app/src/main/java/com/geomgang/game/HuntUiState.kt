package com.geomgang.game

import com.geomgang.core.Zone

/** 사냥 중일 때 화면이 그리는 것. 사냥터 밖에서는 null 이다. */
data class HuntUiState(
    val zone: Zone,
    val targetName: String,
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
    /** 보스 제한 시간을 넘겨 실패했는지. */
    val bossFailed: Boolean,
    /** 방금 구역을 깼는지. */
    val zoneCleared: Boolean,
) {
    val hpRatio: Float get() = (targetHp.toFloat() / targetMaxHp).coerceIn(0f, 1f)

    val bossTimeRatio: Float
        get() = if (!isBoss) 0f
        else (bossRemainingMillis.toFloat() / (zone.bossSeconds * 1000f)).coerceIn(0f, 1f)
}
