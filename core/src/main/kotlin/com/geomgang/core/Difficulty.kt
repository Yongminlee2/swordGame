package com.geomgang.core

/**
 * 게임 모드. 각 모드는 세이브가 완전히 분리된 독립 진행이다.
 *
 * 확률표는 [RateTable]에 한 벌만 두고, 여기 [multiplier]를 곱해 난이도를 만든다.
 * 표를 여러 벌 두지 않는 이유는 밸런스를 고칠 곳을 한 군데로 유지하기 위해서다.
 *
 * @property multiplier 기준 성공률에 곱하는 배수
 * @property maxLevel   강화 상한. null 이면 상한 없음(무한 모드)
 */
enum class Difficulty(
    val id: String,
    val multiplier: Double,
    val maxLevel: Int?,
) {
    EASY("easy", 1.25, 20),
    NORMAL("normal", 1.00, 20),
    HARD("hard", 0.75, 20),
    ENDLESS("endless", 1.00, null),
    ;

    val isEndless: Boolean get() = maxLevel == null

    companion object {
        fun fromId(id: String): Difficulty =
            entries.firstOrNull { it.id == id }
                ?: throw IllegalArgumentException("unknown difficulty id: $id")
    }
}
