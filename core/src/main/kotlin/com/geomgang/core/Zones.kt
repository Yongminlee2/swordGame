package com.geomgang.core

/**
 * 구역에 나오는 몬스터 한 종류.
 *
 * @param weight 등장 비중. 같은 구역 안에서 상대적으로 얼마나 자주 나오는지
 */
data class MonsterKind(
    val name: String,
    val hpFactor: Double,
    val goldFactor: Double,
    val shards: Int,
    val weight: Int,
)

/**
 * 사냥터.
 *
 * 구역마다 몬스터가 세 종류씩 나온다. 한 종류만 계속 잡으면 금방 지루해진다.
 * [hpFactor]·[goldFactor] 는 구역 기준값에 곱해지는 배수라, 구역 난이도를 한 줄로 조절할 수 있다.
 */
enum class Zone(
    val id: String,
    val displayName: String,
    val recommendedLevel: Int,
    /** 이 구역 잡몹의 기준 체력. 몬스터별 배수가 여기 곱해진다. */
    val baseHp: Long,
    /** 이 구역 잡몹의 기준 보상. */
    val baseGold: Long,
    val monsters: List<MonsterKind>,
    val bossName: String,
    val bossHp: Long,
    val bossSeconds: Int,
    val bossGold: Long,
    val bossShards: Int,
    /** 보스가 확정으로 주는 강화석. 구역이 깊을수록 많다. */
    val bossStones: Int,
) {
    MEADOW(
        "meadow", "초원", 0, 24, 14,
        listOf(
            MonsterKind("들쥐", 0.7, 0.7, 0, 4),
            MonsterKind("들개", 1.0, 1.0, 0, 4),
            MonsterKind("초원 토끼", 0.5, 0.6, 0, 3),
            MonsterKind("성난 들개", 1.6, 1.7, 1, 2),
            MonsterKind("떠돌이 늑대", 1.3, 1.4, 1, 3),
        ),
        "들개 우두머리", 220, 5, 900, 6, 3,
    ),
    FOREST(
        "forest", "숲", 3, 90, 52,
        listOf(
            MonsterKind("숲거미", 0.8, 0.8, 0, 4),
            MonsterKind("멧돼지", 1.0, 1.0, 1, 4),
            MonsterKind("숲 다람쥐", 0.6, 0.7, 0, 3),
            MonsterKind("숲의 파수병", 1.7, 1.8, 2, 2),
            MonsterKind("덩굴 요괴", 1.4, 1.5, 1, 3),
        ),
        "거대 멧돼지", 730, 5, 2_800, 9, 3,
    ),
    CAVE(
        "cave", "동굴", 5, 210, 120,
        listOf(
            MonsterKind("동굴 박쥐", 0.8, 0.8, 1, 4),
            MonsterKind("굴 도마뱀", 1.0, 1.0, 1, 4),
            MonsterKind("굴 지네", 0.7, 0.7, 1, 3),
            MonsterKind("석골 병사", 1.8, 1.9, 3, 2),
            MonsterKind("석순 골렘", 1.5, 1.6, 2, 3),
        ),
        "굴의 파수꾼", 1_650, 5, 7_500, 14, 4,
    ),
    MINE(
        "mine", "폐광", 7, 520, 320,
        listOf(
            MonsterKind("광차 유령", 0.8, 0.9, 1, 4),
            MonsterKind("녹슨 자동인형", 1.0, 1.0, 2, 4),
            MonsterKind("탄광 쥐", 0.7, 0.8, 1, 3),
            MonsterKind("광부의 원귀", 1.9, 2.0, 4, 2),
            MonsterKind("녹슨 감시기", 1.5, 1.6, 3, 3),
        ),
        "무너진 갱도의 주인", 3_700, 5, 20_000, 18, 4,
    ),
    SWAMP(
        "swamp", "늪지", 9, 1_100, 760,
        listOf(
            MonsterKind("늪 거머리", 0.8, 0.8, 2, 4),
            MonsterKind("독개구리", 1.0, 1.0, 2, 4),
            MonsterKind("늪 모기", 0.7, 0.7, 1, 3),
            MonsterKind("늪의 마녀", 2.0, 2.2, 5, 2),
            MonsterKind("이끼 거인", 1.6, 1.8, 4, 3),
        ),
        "늪을 삼킨 것", 8_300, 5, 44_000, 24, 5,
    ),
    VOLCANO(
        "volcano", "화산", 12, 3_200, 2_400,
        listOf(
            MonsterKind("불티 정령", 0.8, 0.8, 2, 4),
            MonsterKind("용암 도마뱀", 1.0, 1.0, 3, 4),
            MonsterKind("화산 박쥐", 0.7, 0.7, 2, 3),
            MonsterKind("화염 거인", 2.1, 2.3, 6, 2),
            MonsterKind("용암 골렘", 1.7, 1.9, 5, 3),
        ),
        "화산의 군주", 28_000, 5, 130_000, 32, 5,
    ),
    SNOWFIELD(
        "snowfield", "설원", 14, 7_400, 5_600,
        listOf(
            MonsterKind("눈늑대", 0.8, 0.9, 3, 4),
            MonsterKind("얼음 골렘", 1.0, 1.0, 4, 4),
            MonsterKind("설원 여우", 0.7, 0.8, 2, 3),
            MonsterKind("서리 기사", 2.1, 2.3, 7, 2),
            MonsterKind("얼음 마녀", 1.8, 2.0, 6, 3),
        ),
        "설원의 백룡", 63_000, 5, 300_000, 42, 6,
    ),
    DRAGON_NEST(
        "dragon_nest", "용의 둥지", 16, 17_000, 13_000,
        listOf(
            MonsterKind("알 도둑", 0.8, 0.9, 4, 4),
            MonsterKind("새끼 용", 1.0, 1.0, 5, 4),
            MonsterKind("둥지 도마뱀", 0.7, 0.8, 3, 3),
            MonsterKind("둥지 수호룡", 2.2, 2.4, 9, 2),
            MonsterKind("비늘 파수병", 1.8, 2.0, 7, 3),
        ),
        "늙은 흑룡", 140_000, 5, 700_000, 60, 6,
    ),
    ABYSS(
        "abyss", "심연", 18, 42_000, 32_000,
        listOf(
            MonsterKind("심연의 눈", 0.8, 0.9, 5, 4),
            MonsterKind("그림자 검사", 1.0, 1.0, 6, 4),
            MonsterKind("심연 촉수", 0.7, 0.8, 4, 3),
            MonsterKind("이름 없는 것", 2.3, 2.5, 11, 2),
            MonsterKind("공허 사제", 1.9, 2.1, 9, 3),
        ),
        "심연의 왕", 320_000, 5, 1_800_000, 84, 7,
    ),
    ENDLESS_HALL(
        "endless_hall", "무한 회랑", 20, 110_000, 86_000,
        listOf(
            MonsterKind("회랑의 잔상", 0.9, 1.0, 7, 5),
            MonsterKind("망각한 검사", 1.2, 1.3, 8, 3),
            MonsterKind("회랑 파편", 0.8, 0.9, 5, 4),
            MonsterKind("회랑의 주인", 2.5, 2.8, 14, 2),
            MonsterKind("잊힌 파수꾼", 2.0, 2.2, 11, 3),
        ),
        "회랑 끝의 그림자", 720_000, 5, 5_000_000, 120, 8,
    ),
    SKY_GALLERY(
        "sky_gallery", "천공 회랑", 22, 280_000, 210_000,
        listOf(
            MonsterKind("바람 정령", 0.7, 0.8, 6, 4),
            MonsterKind("구름 위의 검사", 0.9, 1.0, 8, 4),
            MonsterKind("천공 파수병", 1.2, 1.3, 10, 4),
            MonsterKind("번개 조련사", 1.8, 2.0, 13, 3),
            MonsterKind("천공의 재판관", 2.6, 2.9, 16, 2),
        ),
        "천공을 걷는 자", 1_600_000, 5, 12_000_000, 160, 9,
    ),
    RUINED_CAPITAL(
        "ruined_capital", "폐허 왕도", 26, 720_000, 560_000,
        listOf(
            MonsterKind("무너진 병사", 0.7, 0.8, 8, 4),
            MonsterKind("왕도의 원귀", 0.9, 1.0, 10, 4),
            MonsterKind("석화된 기사", 1.3, 1.4, 13, 4),
            MonsterKind("왕실 처형인", 1.9, 2.1, 17, 3),
            MonsterKind("옥좌의 잔영", 2.8, 3.1, 22, 2),
        ),
        "잊힌 왕", 4_200_000, 5, 34_000_000, 210, 10,
    ),
    ;

    /** 이 구역에서 [roll] (0 이상) 이 뽑았을 몬스터. */
    fun monsterFor(roll: Int): MonsterKind {
        val total = monsters.sumOf { it.weight }
        var r = ((roll % total) + total) % total
        for (m in monsters) {
            if (r < m.weight) return m
            r -= m.weight
        }
        return monsters.last()
    }

    fun hpOf(kind: MonsterKind): Long = (baseHp * kind.hpFactor).toLong().coerceAtLeast(1)

    fun goldOf(kind: MonsterKind): Long = (baseGold * kind.goldFactor).toLong().coerceAtLeast(1)

    companion object {
        fun fromId(id: String): Zone = entries.firstOrNull { it.id == id } ?: MEADOW

        /** 구역 하나에서 보스가 나오기까지 잡아야 하는 잡몹 수. */
        const val MONSTERS_BEFORE_BOSS = 12

        /** 희귀 몬스터가 나올 확률. */
        const val RARE_CHANCE = 0.09

        /** 희귀 몬스터의 체력·보상 배수. 체력은 조금, 보상은 크게 올린다. */
        const val RARE_HP = 1.8
        const val RARE_REWARD = 6.0
    }
}
