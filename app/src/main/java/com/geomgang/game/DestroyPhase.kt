package com.geomgang.game

/** 파괴 판정 이후 지금 무엇을 기다리는 중인지. */
sealed interface DestroyPhase {

    /** 남은 시간 비율. 1.0 에서 시작해 0.0 으로 줄어든다. */
    val progress: Float

    /** 기다리는 것이 없다. 평소 상태. */
    data object None : DestroyPhase {
        override val progress: Float get() = 0f
    }

    /**
     * 방지권과 줍기를 **함께** 고를 수 있는 창이 열려 있다.
     *
     * 한때 Prevent 와 Salvage 두 단계였다. 방지권 창을 흘려보내야 줍기 창이
     * 열려서, 방지권을 안 쓸 작정이어도 기다려야 했다. 하나로 합쳤다.
     */
    data class Choice(val remainingMillis: Long, val totalMillis: Long) : DestroyPhase {
        override val progress: Float
            get() = (remainingMillis.toFloat() / totalMillis).coerceIn(0f, 1f)
    }
}
