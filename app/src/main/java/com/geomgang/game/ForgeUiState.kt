package com.geomgang.game

import com.geomgang.core.Difficulty
import com.geomgang.core.ForgeResult
import com.geomgang.core.Sword

/** 강화 화면이 그리는 데 필요한 것 전부. 도메인 상태를 화면 언어로 옮긴 것이다. */
data class ForgeUiState(
    val difficulty: Difficulty,
    val sword: Sword?,
    val gold: Long,
    val shards: Int,
    val preventTickets: Int,
    val bestLevel: Int,
    val upgradeCost: Long,
    val sellPrice: Long,
    val successPercent: Int,
    val canForge: Boolean,
    val canBuySword: Boolean,
    /** 마지막 강화 결과. 연출이 끝나면 null 로 돌아간다. */
    val lastResult: ForgeResult? = null,
    /** 파괴 이후 무엇을 기다리는 중인지. 제한 시간 진행도가 여기 들어 있다. */
    val destroyPhase: DestroyPhase = DestroyPhase.None,
    val canPrevent: Boolean = false,
    /** 연출 재생 중이거나 제한 시간 창이 열려 있으면 입력을 받지 않는다. */
    val busy: Boolean = false,
) {
    /** 방지권이든 줍기든 응답을 기다리는 중인지. */
    val awaitingDestroyChoice: Boolean get() = destroyPhase != DestroyPhase.None
}
