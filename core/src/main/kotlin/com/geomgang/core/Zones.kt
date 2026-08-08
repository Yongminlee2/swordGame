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
) {
    MEADOW(
        "meadow", "초원", 0, 24, 10,
        listOf(
            MonsterKind("들쥐", 0.7, 0.7, 0, 4),
            MonsterKind("들개", 1.0, 1.0, 0, 4),
            MonsterKind("초원 토끼", 0.5, 0.6, 0, 3),
            MonsterKind("성난 들개", 1.6, 1.7, 1, 2),
            MonsterKind("떠돌이 늑대", 1.3, 1.4, 1, 3),
        ),
        "들개 우두머리", 220, 5, 610, 6,
    ),
    FOREST(
        "forest", "숲", 3, 90, 34,
        listOf(
            MonsterKind("숲거미", 0.8, 0.8, 0, 4),
            MonsterKind("멧돼지", 1.0, 1.0, 1, 4),
            MonsterKind("숲 다람쥐", 0.6, 0.7, 0, 3),
            MonsterKind("숲의 파수병", 1.7, 1.8, 2, 2),
            MonsterKind("덩굴 요괴", 1.4, 1.5, 1, 3),
        ),
        "거대 멧돼지", 730, 5, 1_800, 9,
    ),
    CAVE(
        "cave", "동굴", 5, 210, 63,
        listOf(
            MonsterKind("동굴 박쥐", 0.8, 0.8, 1, 4),
            MonsterKind("굴 도마뱀", 1.0, 1.0, 1, 4),
            MonsterKind("굴 지네", 0.7, 0.7, 1, 3),
            MonsterKind("석골 병사", 1.8, 1.9, 3, 2),
            MonsterKind("석순 골렘", 1.5, 1.6, 2, 3),
        ),
        "굴의 파수꾼", 1_650, 5, 3_900, 14,
    ),
    MINE(
        "mine", "폐광", 7, 520, 130,
        listOf(
            MonsterKind("광차 유령", 0.8, 0.9, 1, 4),
            MonsterKind("녹슨 자동인형", 1.0, 1.0, 2, 4),
            MonsterKind("탄광 쥐", 0.7, 0.8, 1, 3),
            MonsterKind("광부의 원귀", 1.9, 2.0, 4, 2),
            MonsterKind("녹슨 감시기", 1.5, 1.6, 3, 3),
        ),
        "무너진 갱도의 주인", 3_700, 5, 8_200, 18,
    ),
    SWAMP(
        "swamp", "늪지", 9, 1_100, 290,
        listOf(
            MonsterKind("늪 거머리", 0.8, 0.8, 2, 4),
            MonsterKind("독개구리", 1.0, 1.0, 2, 4),
            MonsterKind("늪 모기", 0.7, 0.7, 1, 3),
            MonsterKind("늪의 마녀", 2.0, 2.2, 5, 2),
            MonsterKind("이끼 거인", 1.6, 1.8, 4, 3),
        ),
        "늪을 삼킨 것", 8_300, 5, 17_000, 24,
    ),
    VOLCANO(
        "volcano", "화산", 12, 3_200, 940,
        listOf(
            MonsterKind("불티 정령", 0.8, 0.8, 2, 4),
            MonsterKind("용암 도마뱀", 1.0, 1.0, 3, 4),
            MonsterKind("화산 박쥐", 0.7, 0.7, 2, 3),
            MonsterKind("화염 거인", 2.1, 2.3, 6, 2),
            MonsterKind("용암 골렘", 1.7, 1.9, 5, 3),
        ),
        "화산의 군주", 28_000, 5, 51_000, 32,
    ),
    SNOWFIELD(
        "snowfield", "설원", 14, 7_400, 2_000,
        listOf(
            MonsterKind("눈늑대", 0.8, 0.9, 3, 4),
            MonsterKind("얼음 골렘", 1.0, 1.0, 4, 4),
            MonsterKind("설원 여우", 0.7, 0.8, 2, 3),
            MonsterKind("서리 기사", 2.1, 2.3, 7, 2),
            MonsterKind("얼음 마녀", 1.8, 2.0, 6, 3),
        ),
        "설원의 백룡", 63_000, 5, 110_000, 42,
    ),
    DRAGON_NEST(
        "dragon_nest", "용의 둥지", 16, 17_000, 4_200,
        listOf(
            MonsterKind("알 도둑", 0.8, 0.9, 4, 4),
            MonsterKind("새끼 용", 1.0, 1.0, 5, 4),
            MonsterKind("둥지 도마뱀", 0.7, 0.8, 3, 3),
            MonsterKind("둥지 수호룡", 2.2, 2.4, 9, 2),
            MonsterKind("비늘 파수병", 1.8, 2.0, 7, 3),
        ),
        "늙은 흑룡", 140_000, 5, 230_000, 60,
    ),
    ABYSS(
        "abyss", "심연", 18, 42_000, 8_500,
        listOf(
            MonsterKind("심연의 눈", 0.8, 0.9, 5, 4),
            MonsterKind("그림자 검사", 1.0, 1.0, 6, 4),
            MonsterKind("심연 촉수", 0.7, 0.8, 4, 3),
            MonsterKind("이름 없는 것", 2.3, 2.5, 11, 2),
            MonsterKind("공허 사제", 1.9, 2.1, 9, 3),
        ),
        "심연의 왕", 320_000, 5, 480_000, 84,
    ),
    ENDLESS_HALL(
        "endless_hall", "무한 회랑", 20, 110_000, 17_000,
        listOf(
            MonsterKind("회랑의 잔상", 0.9, 1.0, 7, 5),
            MonsterKind("망각한 검사", 1.2, 1.3, 8, 3),
            MonsterKind("회랑 파편", 0.8, 0.9, 5, 4),
            MonsterKind("회랑의 주인", 2.5, 2.8, 14, 2),
            MonsterKind("잊힌 파수꾼", 2.0, 2.2, 11, 3),
        ),
        "회랑 끝의 그림자", 720_000, 5, 1_000_000, 120,
    ),
    SKY_GALLERY(
        "sky_gallery", "천공 회랑", 22, 280_000, 37_000,
        listOf(
            MonsterKind("바람 정령", 0.7, 0.8, 6, 4),
            MonsterKind("구름 위의 검사", 0.9, 1.0, 8, 4),
            MonsterKind("천공 파수병", 1.2, 1.3, 10, 4),
            MonsterKind("번개 조련사", 1.8, 2.0, 13, 3),
            MonsterKind("천공의 재판관", 2.6, 2.9, 16, 2),
        ),
        "천공을 걷는 자", 1_600_000, 5, 2_100_000, 160,
    ),
    RUINED_CAPITAL(
        "ruined_capital", "폐허 왕도", 26, 720_000, 160_000,
        listOf(
            MonsterKind("무너진 병사", 0.7, 0.8, 8, 4),
            MonsterKind("왕도의 원귀", 0.9, 1.0, 10, 4),
            MonsterKind("석화된 기사", 1.3, 1.4, 13, 4),
            MonsterKind("왕실 처형인", 1.9, 2.1, 17, 3),
            MonsterKind("옥좌의 잔영", 2.8, 3.1, 22, 2),
        ),
        "잊힌 왕", 4_200_000, 5, 9_500_000, 210,
    ),

    // --- v1.6 후반 12구역 ---
    // 여기서부터는 권장 단계가 한 칸씩 올라간다. 앞 구간처럼 두세 단계씩 건너뛰면
    // 남은 강화 단계보다 구역이 먼저 떨어져 "다 돌아 버린" 상태가 된다.
    // 체력은 전부 같은 식으로 잡았다 - 권장 단계 공격력의 1.8배(= 216 × 1.5^권장).
    // 권장 단계로는 막히고 두 단계 위에서 잡히는 지점이다.

    SILENT_TEMPLE(
        "silent_temple", "침묵의 사원", 27, 1_400_000, 230_000,
        listOf(
            MonsterKind("사원 종지기", 0.7, 0.8, 9, 4),
            MonsterKind("침묵의 사제", 1.0, 1.0, 12, 4),
            MonsterKind("봉인된 석상", 1.3, 1.4, 15, 3),
            MonsterKind("성전 파수병", 1.9, 2.1, 20, 3),
            MonsterKind("무언의 심판자", 2.8, 3.1, 26, 2),
        ),
        "이름을 잃은 신관", 8_200_000, 5, 14_000_000, 270,
    ),
    GLASS_DESERT(
        "glass_desert", "유리 사막", 28, 3_000_000, 330_000,
        listOf(
            MonsterKind("유리 전갈", 0.7, 0.8, 11, 4),
            MonsterKind("모래 벌레", 1.0, 1.0, 14, 4),
            MonsterKind("유리 나방", 1.3, 1.4, 18, 3),
            MonsterKind("사막 도마뱀", 1.9, 2.1, 24, 3),
            MonsterKind("유리 거인", 2.8, 3.1, 31, 2),
        ),
        "사막을 걷는 유리왕", 18_000_000, 5, 20_000_000, 340,
    ),
    FLOATING_ISLE(
        "floating_isle", "부유 섬", 29, 4_700_000, 480_000,
        listOf(
            MonsterKind("바람 껍질", 0.7, 0.8, 13, 4),
            MonsterKind("떠도는 씨앗", 1.0, 1.0, 17, 4),
            MonsterKind("부유 해파리", 1.3, 1.4, 21, 3),
            MonsterKind("섬의 파수", 1.9, 2.1, 28, 3),
            MonsterKind("하늘 뱀", 2.8, 3.1, 37, 2),
        ),
        "섬을 든 자", 28_000_000, 5, 29_000_000, 430,
    ),
    WARPED_WOOD(
        "warped_wood", "뒤틀린 숲", 30, 6_800_000, 690_000,
        listOf(
            MonsterKind("뒤틀린 덩굴", 0.7, 0.8, 16, 4),
            MonsterKind("숲의 도둑", 1.0, 1.0, 20, 4),
            MonsterKind("나무 골렘", 1.3, 1.4, 26, 3),
            MonsterKind("숲의 기수", 1.9, 2.1, 34, 3),
            MonsterKind("숲의 여왕", 2.8, 3.1, 44, 2),
        ),
        "뒤틀림의 근원", 41_000_000, 5, 42_000_000, 540,
    ),
    SUNKEN_CITY(
        "sunken_city", "수몰 도시", 31, 10_000_000, 980_000,
        listOf(
            MonsterKind("잠긴 병사", 0.7, 0.8, 19, 4),
            MonsterKind("심해 뱀", 1.0, 1.0, 24, 4),
            MonsterKind("수몰 촉수", 1.3, 1.4, 31, 3),
            MonsterKind("익사한 마법사", 1.9, 2.1, 41, 3),
            MonsterKind("물의 정령", 2.8, 3.1, 53, 2),
        ),
        "도시를 삼킨 것", 62_000_000, 5, 61_000_000, 680,
    ),
    ASH_PLAIN(
        "ash_plain", "재의 평원", 32, 15_000_000, 1_400_000,
        listOf(
            MonsterKind("재의 사냥개", 0.7, 0.8, 23, 4),
            MonsterKind("잿불 도롱뇽", 1.0, 1.0, 29, 4),
            MonsterKind("타버린 유령", 1.3, 1.4, 37, 3),
            MonsterKind("재의 정령", 1.9, 2.1, 49, 3),
            MonsterKind("잿더미 거인", 2.8, 3.1, 64, 2),
        ),
        "재를 뿌리는 자", 93_000_000, 5, 89_000_000, 850,
    ),
    STAR_TOMB(
        "star_tomb", "별의 무덤", 33, 23_000_000, 2_100_000,
        listOf(
            MonsterKind("별 부스러기", 0.7, 0.8, 28, 4),
            MonsterKind("무덤 지기", 1.0, 1.0, 35, 4),
            MonsterKind("별빛 유령", 1.3, 1.4, 45, 3),
            MonsterKind("떨어진 별", 1.9, 2.1, 59, 3),
            MonsterKind("무덤의 리치", 2.8, 3.1, 77, 2),
        ),
        "별을 삼킨 무덤", 140_000_000, 5, 130_000_000, 1_060,
    ),
    TIME_RIFT(
        "time_rift", "시간의 균열", 34, 35_000_000, 3_100_000,
        listOf(
            MonsterKind("시간의 잔재", 0.7, 0.8, 33, 4),
            MonsterKind("되감긴 병사", 1.0, 1.0, 42, 4),
            MonsterKind("균열 나방", 1.3, 1.4, 54, 3),
            MonsterKind("뒤엉킨 그림자", 1.9, 2.1, 71, 3),
            MonsterKind("시간의 포식자", 2.8, 3.1, 93, 2),
        ),
        "균열 너머의 것", 210_000_000, 5, 190_000_000, 1_330,
    ),
    BLOOD_KEEP(
        "blood_keep", "피의 성채", 35, 52_000_000, 4_500_000,
        listOf(
            MonsterKind("성채의 사냥개", 0.7, 0.8, 40, 4),
            MonsterKind("피의 기사", 1.0, 1.0, 50, 4),
            MonsterKind("성채 궁수", 1.3, 1.4, 65, 3),
            MonsterKind("피의 마법사", 1.9, 2.1, 85, 3),
            MonsterKind("성채의 처형인", 2.8, 3.1, 111, 2),
        ),
        "피의 군주", 314_000_000, 5, 270_000_000, 1_660,
    ),
    FROST_HEART(
        "frost_heart", "서리 심장", 36, 78_000_000, 6_400_000,
        listOf(
            MonsterKind("서리 곰", 0.7, 0.8, 48, 4),
            MonsterKind("얼음 악마", 1.0, 1.0, 60, 4),
            MonsterKind("서리 망령", 1.3, 1.4, 78, 3),
            MonsterKind("얼음 인형", 1.9, 2.1, 102, 3),
            MonsterKind("서리 용", 2.8, 3.1, 133, 2),
        ),
        "심장을 얼린 것", 470_000_000, 5, 390_000_000, 2_080,
    ),
    FIRST_FORGE(
        "first_forge", "태초의 대장간", 37, 118_000_000, 9_300_000,
        listOf(
            MonsterKind("화로의 불티", 0.7, 0.8, 58, 4),
            MonsterKind("쇳물 골렘", 1.0, 1.0, 72, 4),
            MonsterKind("대장간 수호상", 1.3, 1.4, 94, 3),
            MonsterKind("무쇠 악마", 1.9, 2.1, 123, 3),
            MonsterKind("첫 검의 잔영", 2.8, 3.1, 160, 2),
        ),
        "태초의 대장장이", 707_000_000, 5, 570_000_000, 2_600,
    ),
    FINAL_GATE(
        "final_gate", "끝의 문", 38, 176_000_000, 14_000_000,
        listOf(
            MonsterKind("문지기", 0.7, 0.8, 69, 4),
            MonsterKind("끝의 사냥개", 1.0, 1.0, 87, 4),
            MonsterKind("문 너머의 눈", 1.3, 1.4, 113, 3),
            MonsterKind("심판의 낫", 1.9, 2.1, 148, 3),
            MonsterKind("끝의 전령", 2.8, 3.1, 192, 2),
        ),
        "문을 여는 자", 1_060_000_000, 5, 820_000_000, 3_250,
    ),
    ;

    /**
     * 보스가 확정으로 주는 강화석.
     *
     * 구역마다 손으로 적던 값이었는데, 요구 곡선을 한 번 고칠 때마다 24개가 같이 어긋났고
     * 어긋난 채로 몇 판이 지나가면 "보스 하나에 강화 한 번" 으로 되돌아갔다.
     * 요구량에서 끌어내면 **잡을 때마다 강화 [ForgeCost.BOSS_STONE_RUNS] 번**이
     * 구조적으로 보장된다.
     */
    val bossStones: Int get() = ForgeCost.bossStonesFor(recommendedLevel)

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
