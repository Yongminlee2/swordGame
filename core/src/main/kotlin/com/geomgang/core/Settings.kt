package com.geomgang.core

import kotlinx.serialization.Serializable

/**
 * 모드와 무관한 전역 설정.
 *
 * 세이브가 아니라 취향이므로 모드 초기화의 영향을 받지 않는다.
 */
@Serializable
data class Settings(
    /**
     * 파괴 판정이 나면 제한 시간 창을 열지 않고 방지권을 즉시 쓴다.
     *
     * 기본값이 꺼짐인 이유: 파괴 순간 2.5초 안에 눌러야 하는 긴장이 이 게임의 핵심이다.
     * 편의를 원하는 사람만 켜게 한다.
     */
    val autoPrevent: Boolean = false,
)
