package com.geomgang.game

import com.geomgang.core.GauntletBuff
import com.geomgang.core.GauntletChoice

/** 무한 회랑 화면이 그리는 것. 회랑 밖에서는 null 이다. */
data class GauntletUiState(
    val floor: Int,
    val kills: Int,
    val waveSize: Int,
    val timeLeftMillis: Long,
    val monsterHp: Long,
    val monsterMaxHp: Long,
    val isBossFloor: Boolean,
    val cursed: Boolean,
    val buffs: Set<GauntletBuff>,
    val choosing: Boolean,
    val choices: List<GauntletChoice>,
    val pendingGold: Long,
    val pendingShards: Int,
    val bankedGold: Long,
    val bankedShards: Int,
    val over: Boolean,
    val best: Int,
) {
    val hpRatio: Float get() = (monsterHp.toFloat() / monsterMaxHp).coerceIn(0f, 1f)
}
