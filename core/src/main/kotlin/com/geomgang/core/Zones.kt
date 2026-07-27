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
            MonsterKind("성난 들개", 1.6, 1.7, 1, 2),
        ),
        "들개 우두머리", 420, 20, 900, 6, 3,
    ),
    FOREST(
        "forest", "숲", 3, 90, 52,
        listOf(
            MonsterKind("숲거미", 0.8, 0.8, 0, 4),
            MonsterKind("멧돼지", 1.0, 1.0, 1, 4),
            MonsterKind("숲의 파수병", 1.7, 1.8, 2, 2),
        ),
        "거대 멧돼지", 1_600, 20, 2_800, 9, 3,
    ),
    CAVE(
        "cave", "동굴", 5, 210, 120,
        listOf(
            MonsterKind("동굴 박쥐", 0.8, 0.8, 1, 4),
            MonsterKind("굴 도마뱀", 1.0, 1.0, 1, 4),
            MonsterKind("석골 병사", 1.8, 1.9, 3, 2),
        ),
        "굴의 파수꾼", 4_200, 20, 7_500, 14, 4,
    ),
    MINE(
        "mine", "폐광", 7, 520, 320,
        listOf(
            MonsterKind("광차 유령", 0.8, 0.9, 1, 4),
            MonsterKind("녹슨 자동인형", 1.0, 1.0, 2, 4),
            MonsterKind("광부의 원귀", 1.9, 2.0, 4, 2),
        ),
        "무너진 갱도의 주인", 11_000, 20, 20_000, 18, 4,
    ),
    SWAMP(
        "swamp", "늪지", 9, 1_100, 760,
        listOf(
            MonsterKind("늪 거머리", 0.8, 0.8, 2, 4),
            MonsterKind("독개구리", 1.0, 1.0, 2, 4),
            MonsterKind("늪의 마녀", 2.0, 2.2, 5, 2),
        ),
        "늪을 삼킨 것", 24_000, 22, 44_000, 24, 5,
    ),
    VOLCANO(
        "volcano", "화산", 12, 3_200, 2_400,
        listOf(
            MonsterKind("불티 정령", 0.8, 0.8, 2, 4),
            MonsterKind("용암 도마뱀", 1.0, 1.0, 3, 4),
            MonsterKind("화염 거인", 2.1, 2.3, 6, 2),
        ),
        "화산의 군주", 72_000, 22, 130_000, 32, 5,
    ),
    SNOWFIELD(
        "snowfield", "설원", 14, 7_400, 5_600,
        listOf(
            MonsterKind("눈늑대", 0.8, 0.9, 3, 4),
            MonsterKind("얼음 골렘", 1.0, 1.0, 4, 4),
            MonsterKind("서리 기사", 2.1, 2.3, 7, 2),
        ),
        "설원의 백룡", 170_000, 24, 300_000, 42, 6,
    ),
    DRAGON_NEST(
        "dragon_nest", "용의 둥지", 16, 17_000, 13_000,
        listOf(
            MonsterKind("알 도둑", 0.8, 0.9, 4, 4),
            MonsterKind("새끼 용", 1.0, 1.0, 5, 4),
            MonsterKind("둥지 수호룡", 2.2, 2.4, 9, 2),
        ),
        "늙은 흑룡", 400_000, 25, 700_000, 60, 6,
    ),
    ABYSS(
        "abyss", "심연", 18, 42_000, 32_000,
        listOf(
            MonsterKind("심연의 눈", 0.8, 0.9, 5, 4),
            MonsterKind("그림자 검사", 1.0, 1.0, 6, 4),
            MonsterKind("이름 없는 것", 2.3, 2.5, 11, 2),
        ),
        "심연의 왕", 1_000_000, 26, 1_800_000, 84, 7,
    ),
    ENDLESS_HALL(
        "endless_hall", "무한 회랑", 20, 110_000, 86_000,
        listOf(
            MonsterKind("회랑의 잔상", 0.9, 1.0, 7, 5),
            MonsterKind("망각한 검사", 1.2, 1.3, 8, 3),
            MonsterKind("회랑의 주인", 2.5, 2.8, 14, 2),
        ),
        "회랑 끝의 그림자", 2_800_000, 28, 5_000_000, 120, 8,
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
