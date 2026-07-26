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
    /** 파괴 판정이 나서 방지권/줍기 응답을 기다리는 중인지. */
    val awaitingDestroyChoice: Boolean = false,
    val canPrevent: Boolean = false,
    /** 연출 재생 중에는 입력을 받지 않는다. */
    val busy: Boolean = false,
)
