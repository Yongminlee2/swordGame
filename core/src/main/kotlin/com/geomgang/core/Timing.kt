package com.geomgang.core

/**
 * 반응 시간 규칙.
 *
 * 게임 규칙이지 UI 설정이 아니다. 이 값이 바뀌면 난이도가 바뀐다.
 * 그래서 화면이 아니라 여기에 둔다.
 */
object Timing {

    /** 파괴 직후 방지권을 쓸 수 있는 시간. 이 짧음이 이 게임의 핵심 긴장이다. */
    const val PREVENT_WINDOW_MILLIS: Long = 2_500

    /** 파괴가 확정된 뒤 파편을 주울 수 있는 시간. */
    const val SALVAGE_WINDOW_MILLIS: Long = 3_000

    /** 카운트다운 갱신 간격. */
    const val TICK_MILLIS: Long = 50
}
