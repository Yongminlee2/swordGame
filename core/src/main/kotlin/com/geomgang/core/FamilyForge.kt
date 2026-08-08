package com.geomgang.core

/**
 * 이 검이 전설검인지. **계열이 아니라 단계가 정한다.**
 *
 * 전설검을 [WeaponFamily] 에 넣지 않는 이유: 계열이 15종이 되면 도감 계열칸이
 * 294 에서 315 로 늘어나는데, +0 짜리 전설검은 존재할 수 없으므로 영원히 못 채우는
 * 칸 21 개가 생긴다.
 */
fun Sword.isLegend(): Boolean = level > RateTable.MAX_FINITE_LEVEL

/**
 * 계열별 강화 특성.
 *
 * [FamilyStyle] 이 **싸우는 방식**을 담듯 여기는 **벼리는 방식**을 담는다.
 * 한 enum 에 섞지 않는 이유: 전투 밸런스와 강화 밸런스는 서로 다른 이유로 바뀐다.
 *
 * 합산되지 않는다 — **든 검에만** 붙는다. 그래서 "무엇을 들고 올릴까" 가 선택이 된다.
 *
 * @param successBonus   성공률 가산
 * @param dropGuard   파괴 판정을 무효로 돌릴 확률
 * @param temperCapBonus 담금질 상한 가산
 * @param costMult       강화 비용 배수
 * @param stoneRelief    강화석 요구를 줄이는 개수
 * @param salvageMult    파괴 후 줍는 조각 배수
 * @param blessingMult   축복서 효과 배수
 * @param codexPair      도감에 바칠 때 다음 단계 칸도 함께 여는지
 * @param refundStones   실패해도 강화석을 돌려받는지
 */
enum class FamilyForge(
    val successBonus: Double = 0.0,
    val dropGuard: Double = 0.0,
    val temperCapBonus: Double = 0.0,
    val costMult: Double = 1.0,
    val stoneRelief: Int = 0,
    val salvageMult: Double = 1.0,
    val blessingMult: Double = 1.0,
    val codexPair: Boolean = false,
    val refundStones: Boolean = false,
    val blurb: String = "",
) {
    /** 검이 없을 때. 아무것도 주지 않는다. */
    NONE,

    /**
     * 기준.
     *
     * **아무 특전도 없다.** 다른 계열이 이 값과 견줘 읽히려면 기준 하나는 비어 있어야
     * 하고, 무엇보다 [com.geomgang.core.sim.BalanceSimulation] 이 직검으로만 도는데
     * 여기에 값을 주면 그 모형이 재려던 "맨손" 이 맨손이 아니게 된다.
     */
    STRAIGHT(blurb = "기준. 특별할 것도 모자랄 것도 없다"),
    CURVED(successBonus = 0.004, blurb = "조금 더 잘 붙는다"),
    GREAT(dropGuard = 0.03, blurb = "잘 부서지지 않는다"),
    RAPIER(costMult = 0.7, blurb = "가벼워서 벼리는 값이 싸다"),
    TWIN(successBonus = 0.003, dropGuard = 0.015, blurb = "성공률과 내구가 조금씩"),
    DEMON(salvageMult = 2.0, blurb = "부서져도 조각을 두 배로 줍는다"),
    HOLY(blessingMult = 1.5, blurb = "축복서가 더 잘 듣는다"),
    DRAGON(successBonus = 0.002, dropGuard = 0.02, blurb = "단단하고 조금 잘 붙는다"),
    SCYTHE(codexPair = true, blurb = "도감에 바치면 다음 칸도 함께 열린다"),
    AXE(costMult = 0.8, blurb = "강화 비용이 싸다"),
    SPEAR(stoneRelief = 2, blurb = "강화석이 두 개 덜 든다"),
    SPIRIT(dropGuard = 0.025, blurb = "정령이 부서짐을 막아 준다"),
    FUSED(
        successBonus = 0.0025,
        dropGuard = 0.015,
        costMult = 0.9,
        salvageMult = 1.5,
        blessingMult = 1.25,
        blurb = "여러 계열의 벼림을 조금씩 전부",
    ),
    VOID(refundStones = true, blurb = "실패해도 강화석을 돌려받는다"),

    /**
     * 가장 어려운 길의 보상. 어느 계열보다 강하다.
     *
     * **담금질 관련 특성은 여기에만 있다.** 담금질은 +21 부터 붙는데 계열 검은 +20 에서
     * 끝나므로, 계열에 담금질 특성을 주면 영원히 발동하지 않는다.
     */
    LEGEND(
        successBonus = 0.03,
        dropGuard = 0.03,
        temperCapBonus = 0.20,
        blurb = "전설검. 벼림의 끝",
    ),

    /**
     * 고유검. 재료가 무엇이었든 상관없다 - 이미 완성된 검이라 계열 특성을 쓰지 않는다.
     *
     * 강화대에 오르지 않으므로(v2.3) 모든 값이 중립이다 - 실제 능력은 [UniqueSwords] 가
     * 별도 출처(소유 보너스·전투 패시브)로 준다.
     */
    UNIQUE(blurb = "고유검. 계열 특성 대신 저마다의 능력을 쓴다"),
    ;

    companion object {
        /** 고유검·전설 여부를 먼저 본다. 계열은 그다음이다. */
        fun of(sword: Sword?): FamilyForge {
            if (sword == null) return NONE
            if (sword.uniqueId != null) return UNIQUE
            if (sword.isLegend()) return LEGEND
            return when (sword.family) {
                WeaponFamily.STRAIGHT -> STRAIGHT
                WeaponFamily.CURVED -> CURVED
                WeaponFamily.GREAT -> GREAT
                WeaponFamily.RAPIER -> RAPIER
                WeaponFamily.TWIN -> TWIN
                WeaponFamily.DEMON -> DEMON
                WeaponFamily.HOLY -> HOLY
                WeaponFamily.DRAGON -> DRAGON
                WeaponFamily.SCYTHE -> SCYTHE
                WeaponFamily.AXE -> AXE
                WeaponFamily.SPEAR -> SPEAR
                WeaponFamily.SPIRIT -> SPIRIT
                WeaponFamily.FUSED -> FUSED
                WeaponFamily.VOID -> VOID
            }
        }
    }
}
