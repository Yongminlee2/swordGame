package com.geomgang.core

/**
 * 검 계열과 강화 단계에 맞는 표시 이름.
 *
 * 화면의 21칸 진화 시트와 같은 순서(+0~+20)로 이름을 둔다. 계열마다 별도 목록을
 * 쓰므로 같은 강화 단계라도 직검·곡도·대검·세검·마검·성검·용검의 이름이 겹치지 않는다.
 */
object SwordNames {

    private val STRAIGHT = arrayOf(
        "녹슨 쇠칼", "손질한 직검", "벼린 직검", "병사의 검", "용병의 검", "수호자의 검",
        "왕실 장검", "청옥 장검", "기사단 장검", "룬 장검", "청명검", "파쇄 장검",
        "심연 장검", "푸른 룬검", "흑요 장검", "마력 장검", "새벽 장검", "성운검",
        "천공 장검", "황금 장검", "창천검",
    )

    private val CURVED = arrayOf(
        "녹슨 만도", "손질한 곡도", "벼린 곡도", "사막 곡도", "송곳니 곡도", "흑월도",
        "청월도", "은월도", "유수도", "청류도", "금월도", "귀월도", "벽력도", "천광도",
        "황월도", "창월도", "비천월도", "풍신도", "금조월도", "태양월도", "천익월도",
    )

    private val GREAT = arrayOf(
        "녹슨 대검", "무쇠 대검", "중철 대검", "가시 대검", "청광 대검", "흑철 대검",
        "왕가 대검", "균열 대검", "톱날 대검", "화염톱날", "용암 대검", "쌍날 대검",
        "홍염 대검", "화심 대검", "파멸 대검", "격노 대검", "용광 대검", "흑염 대검",
        "천벌 대검", "태양 대검", "종말 대검",
    )

    private val RAPIER = arrayOf(
        "낡은 세검", "은침", "청풍 세검", "흑침", "청옥 세검", "백금 세검", "황금 세검",
        "명예의 세검", "룬 세검", "은월 세검", "가시 세검", "마력 세검", "청린 세검",
        "파동 세검", "벽력 세검", "성휘 세검", "수정 세검", "청명 세검", "천공 세검",
        "왕관 세검", "창천 레이피어",
    )

    private val DEMON = arrayOf(
        "핏빛 단검", "흑마검", "혈석 마검", "혈아 마검", "귀화 마검", "골룡 마검",
        "자염 마검", "적안 마검", "혈월 마검", "귀왕 마검", "마수검", "사슬 마검",
        "나락 마검", "혈혼 마검", "독안 마검", "자월 마검", "악몽 마검", "주술 마검",
        "마왕검", "멸혼 마검", "진마왕검",
    )

    private val HOLY = arrayOf(
        "순례자의 검", "백은 성검", "청옥 성검", "서광 성검", "수호 성검", "천사 성검",
        "심판 성검", "정화 성검", "성기사검", "축복 성검", "광휘 성검", "천문 성검",
        "날개 성검", "태양 성검", "천륜 성검", "쌍익 성검", "성운 성검", "찬란 성검",
        "대천사검", "천상 성검", "천계의 검",
    )

    private val DRAGON = arrayOf(
        "용골 단검", "어린 용의 이빨", "홍룡의 비늘", "홍룡아", "용린 톱날", "골룡아",
        "염룡검", "용두검", "붉은 용아", "용익검", "용염검", "화룡검", "역린검",
        "용황검", "고룡검", "홍련용검", "천룡검", "용왕검", "폭염용검", "용신검",
        "진룡황검",
    )

    private val VISIBLE_BY_FAMILY = mapOf(
        WeaponFamily.STRAIGHT to STRAIGHT,
        WeaponFamily.CURVED to CURVED,
        WeaponFamily.GREAT to GREAT,
        WeaponFamily.RAPIER to RAPIER,
        WeaponFamily.DEMON to DEMON,
        WeaponFamily.HOLY to HOLY,
        WeaponFamily.DRAGON to DRAGON,
    )

    private val HIDDEN_STAGES = arrayOf(
        "낡은", "손질한", "벼린", "단단한", "날카로운", "정교한", "청옥", "은빛",
        "왕가의", "룬", "마력", "균열의", "심연의", "벽력", "흑요", "성운",
        "새벽의", "천공", "황금", "태양", "초월",
    )

    private val ENDLESS_STAGES = arrayOf("초월", "심연", "성좌", "무명")

    /** 고정 이름이 있는 마지막 단계. */
    val maxNamedLevel: Int get() = STRAIGHT.lastIndex

    /** 옛 호출부용 기본 이름. 계열 정보가 없으면 직검 이름을 쓴다. */
    fun nameFor(level: Int): String = nameFor(WeaponFamily.STRAIGHT, level)

    fun nameFor(family: WeaponFamily, level: Int): String {
        require(level >= 0) { "level must be >= 0, was $level" }
        if (level <= maxNamedLevel) {
            return VISIBLE_BY_FAMILY[family]?.get(level)
                ?: "${HIDDEN_STAGES[level]} ${family.displayName}"
        }
        val step = ((level - maxNamedLevel - 1) / 5).coerceAtMost(ENDLESS_STAGES.lastIndex)
        return "${ENDLESS_STAGES[step]} ${family.displayName}"
    }

    /** 고유검은 고유 이름을, 일반 검은 계열·단계별 이름을 쓴다. */
    fun nameFor(sword: Sword): String =
        sword.uniqueId?.let { UniqueSwords.byId(it)?.name }
            ?: nameFor(sword.family, sword.level)

    internal fun namedLevels(family: WeaponFamily = WeaponFamily.STRAIGHT): Array<String> =
        VISIBLE_BY_FAMILY[family]?.copyOf()
            ?: Array(maxNamedLevel + 1) { level -> "${HIDDEN_STAGES[level]} ${family.displayName}" }

    internal fun endlessNames(family: WeaponFamily = WeaponFamily.STRAIGHT): Array<String> =
        ENDLESS_STAGES.map { "$it ${family.displayName}" }.toTypedArray()
}
