package com.geomgang.core

import kotlinx.serialization.Serializable

/** 강화 한 판의 결과를 한 글자로 줄인 것. */
@Serializable
enum class ForgeMark { UP, STAY, DOWN, BREAK }

/**
 * 최근 강화 결과의 자취.
 *
 * 결과는 한 번 뜨고 사라져서 **"세 판째 말아먹는 중"이라는 이야기가 남지 않았다.**
 * 연속 실패 횟수는 이미 통계로 세고 있는데 화면에 없었다.
 */
object ForgeMarks {

    /** 화면에 남기는 판 수. 한 줄에 들어가고 흐름이 읽히는 길이다. */
    const val KEEP: Int = 12

    fun of(result: ForgeResult): ForgeMark = when (result) {
        is ForgeResult.Success -> ForgeMark.UP
        is ForgeResult.Stay -> ForgeMark.STAY
        is ForgeResult.Drop -> ForgeMark.DOWN
        is ForgeResult.Destroyed -> ForgeMark.BREAK
    }

    /** 새 결과를 뒤에 붙이고 [KEEP] 을 넘으면 앞에서 버린다. */
    fun push(marks: List<ForgeMark>, mark: ForgeMark): List<ForgeMark> =
        (marks + mark).takeLast(KEEP)
}
