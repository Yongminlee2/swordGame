package com.geomgang.game

/** 파괴 판정 이후 지금 무엇을 기다리는 중인지. */
sealed interface DestroyPhase {

    /** 남은 시간 비율. 1.0 에서 시작해 0.0 으로 줄어든다. */
    val progress: Float

    /** 기다리는 것이 없다. 평소 상태. */
    data object None : DestroyPhase {
        override val progress: Float get() = 0f
    }

    /** 방지권을 쓸 수 있는 창이 열려 있다. */
    data class Prevent(val remainingMillis: Long, val totalMillis: Long) : DestroyPhase {
        override val progress: Float
            get() = (remainingMillis.toFloat() / totalMillis).coerceIn(0f, 1f)
    }

    /** 파편을 주울 수 있는 창이 열려 있다. */
    data class Salvage(val remainingMillis: Long, val totalMillis: Long) : DestroyPhase {
        override val progress: Float
            get() = (remainingMillis.toFloat() / totalMillis).coerceIn(0f, 1f)
    }
}
