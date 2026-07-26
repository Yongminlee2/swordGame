package com.geomgang.core

import kotlin.random.Random

/**
 * 정해 둔 값을 순서대로 돌려주는 테스트용 난수.
 *
 * 강화 판정처럼 분기가 확률로 갈리는 코드를 결정적으로 검증하기 위한 도구다.
 * [nextDouble] 외의 호출은 테스트가 의도치 않은 경로를 타고 있다는 뜻이므로 예외를 던진다.
 */
class ScriptedRandom(private vararg val values: Double) : Random() {

    private var index = 0

    /** 지금까지 소비한 값의 개수. */
    val consumed: Int get() = index

    override fun nextBits(bitCount: Int): Int =
        throw UnsupportedOperationException("ScriptedRandom only supports nextDouble()")

    override fun nextDouble(): Double {
        check(index < values.size) {
            "ScriptedRandom exhausted: ${values.size} values were scripted but more were requested"
        }
        return values[index++]
    }
}
