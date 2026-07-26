package com.geomgang.core

/**
 * 강화 단계마다 붙는 검 이름.
 *
 * "화염검 +13" 처럼 같은 이름에 숫자만 붙이면 올라가는 맛이 없다.
 * 단계마다 이름이 아예 달라야 한 단계가 사건처럼 느껴진다.
 *
 * 계열은 형태를, 단계는 이름을 정한다. 화면에는 이름을 크게 띄우고
 * 계열을 부제로 작게 붙인다.
 *
 * 이름은 모두 일반적인 판타지 명칭이라 특정 저작물과 무관하다.
 */
object SwordNames {

    private val BY_LEVEL = arrayOf(
        "녹슨 쇠칼", // +0
        "이 빠진 검", // +1
        "벼린 쇠검", // +2
        "강철검", // +3
        "단단한 강철검", // +4
        "예리한 강철검", // +5
        "은장검", // +6
        "은빛 장검", // +7
        "세공된 은검", // +8
        "룬이 새겨진 검", // +9
        "룬각검", // +10
        "고대 룬검", // +11
        "불꽃이 감긴 검", // +12
        "화염검", // +13
        "용암검", // +14
        "뇌전검", // +15
        "벼락을 품은 검", // +16
        "여명검", // +17
        "새벽빛 성검", // +18
        "흑룡의 이빨", // +19
        "흑룡참", // +20
    )

    /** 무한 구간. 다섯 단계마다 다음 이름으로 넘어간다. */
    private val ENDLESS = arrayOf(
        "용린참",
        "심연검",
        "별을 가른 검",
        "이름 없는 검",
    )

    /** 유한 구간의 마지막 단계. */
    val maxNamedLevel: Int get() = BY_LEVEL.lastIndex

    fun nameFor(level: Int): String {
        require(level >= 0) { "level must be >= 0, was $level" }
        if (level <= maxNamedLevel) return BY_LEVEL[level]
        val step = (level - maxNamedLevel - 1) / 5
        return ENDLESS[step.coerceAtMost(ENDLESS.lastIndex)]
    }

    /** 검 한 자루의 표시 이름. 고유검은 단계 이름 대신 고유 이름을 쓴다. */
    fun nameFor(sword: Sword): String =
        sword.uniqueId?.let { UniqueSwords.byId(it)?.name } ?: nameFor(sword.level)

    /** 테스트가 쓰는 값. */
    internal fun namedLevels(): Array<String> = BY_LEVEL

    internal fun endlessNames(): Array<String> = ENDLESS
}
