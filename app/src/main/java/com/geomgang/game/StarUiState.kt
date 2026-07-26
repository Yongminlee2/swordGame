package com.geomgang.game

/** 별 강화(특수강화) 화면 정보. */
data class StarUiState(
    val stars: Int,
    val maxStars: Int,
    val successPercent: Int,
    val shardCost: Int,
    val goldCost: Long,
    val affordable: Boolean,
    /** 별이 지금 올려 주는 공격력 비율(%). */
    val attackBonusPercent: Int,
    /** 마지막 시도가 성공했는지. 알린 뒤 비운다. */
    val lastUp: Boolean? = null,
)
