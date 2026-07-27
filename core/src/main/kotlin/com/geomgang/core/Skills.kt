package com.geomgang.core

/**
 * 계열 고유 스킬.
 *
 * @param damageMult   총 피해 배수 (연타 스킬은 합계 배수다)
 * @param hits         화면에 몇 번 튀는지
 * @param bossMult     보스에게 추가로 곱해지는 배수. 심판만 1.0이 아니다
 * @param maxHpRatio   적 최대체력에 비례해 더해지는 피해
 * @param shardBonus   발동할 때 얻는 조각. 흡혈만 0이 아니다
 * @param burnBurst    화상을 한 번 크게 터뜨리는지
 */
data class Skill(
    val id: String,
    val name: String,
    val damageMult: Double,
    val hits: Int,
    val bossMult: Double = 1.0,
    val maxHpRatio: Double = 0.0,
    val shardBonus: Int = 0,
    val burnBurst: Boolean = false,
    val blurb: String,
)

/**
 * 스킬 발동.
 *
 * 강화 단계가 [MIN_LEVEL] 이상이면 탭할 때 [CHANCE] 확률로 계열 고유 스킬이 터진다.
 * +15는 파괴 구간 한복판이라 "위험을 무릅쓸 값어치"가 생기고,
 * 5초 보스전에서는 스킬이 터지느냐가 승패를 가른다.
 *
 * 판정은 난수를 값으로 받는다 — 치명타와 같은 방식이라 테스트가 결정적이다.
 */
object Skills {

    /** 스킬이 열리는 최소 강화 단계. */
    const val MIN_LEVEL = 15

    /** 탭 한 번에 스킬이 터질 확률. */
    const val CHANCE = 0.12

    /** 화상 폭발이 한 번에 넣는 피해 = 초당 화상 × 이 값. */
    const val BURN_BURST_MULT = 10

    private val BY_FAMILY: Map<WeaponFamily, Skill> = mapOf(
        WeaponFamily.STRAIGHT to Skill(
            "flash", "일섬", damageMult = 3.0, hits = 1,
            blurb = "한 번에 3배로 베어 넘긴다",
        ),
        WeaponFamily.CURVED to Skill(
            "moonfall", "월광참", damageMult = 2.5, hits = 1,
            blurb = "달빛을 그리며 2.5배",
        ),
        WeaponFamily.GREAT to Skill(
            "collapse", "붕괴", damageMult = 5.0, hits = 1,
            blurb = "무게로 짓눌러 5배",
        ),
        WeaponFamily.RAPIER to Skill(
            "flurry", "연속 찌르기", damageMult = 4.0, hits = 4,
            blurb = "네 번 찔러 합 4배",
        ),
        WeaponFamily.TWIN to Skill(
            "twinmoon", "쌍월", damageMult = 4.0, hits = 2,
            blurb = "두 자루가 각각 2배",
        ),
        WeaponFamily.DEMON to Skill(
            "drain", "흡혈", damageMult = 3.0, hits = 1, shardBonus = 1,
            blurb = "3배로 베고 조각을 빤다",
        ),
        WeaponFamily.HOLY to Skill(
            "judgment", "심판", damageMult = 3.0, hits = 1, bossMult = 2.0,
            blurb = "보스에게 6배, 잡몹에게 3배",
        ),
        WeaponFamily.DRAGON to Skill(
            "dragonbreath", "용의 숨결", damageMult = 3.0, hits = 1, burnBurst = true,
            blurb = "3배 + 화상을 한꺼번에 터뜨린다",
        ),
        WeaponFamily.SCYTHE to Skill(
            "reap", "사신의 낫", damageMult = 3.0, hits = 1, maxHpRatio = 0.05,
            blurb = "3배 + 최대체력 5%",
        ),
        WeaponFamily.AXE to Skill(
            "crush", "분쇄", damageMult = 6.0, hits = 1,
            blurb = "내려찍어 6배",
        ),
        WeaponFamily.SPEAR to Skill(
            "pierce", "관통", damageMult = 3.6, hits = 3,
            blurb = "세 번 관통해 합 3.6배",
        ),
        WeaponFamily.SPIRIT to Skill(
            "spiritburst", "정령 폭발", damageMult = 3.0, hits = 1, burnBurst = true,
            blurb = "3배 + 화상 폭발",
        ),
        WeaponFamily.FUSED to Skill(
            "allthings", "만상", damageMult = 4.0, hits = 1,
            blurb = "모든 계열의 힘으로 4배",
        ),
        WeaponFamily.VOID to Skill(
            "voidcall", "공허", damageMult = 3.0, hits = 1, maxHpRatio = 0.08,
            blurb = "3배 + 최대체력 8%",
        ),
    )

    fun of(family: WeaponFamily): Skill =
        BY_FAMILY[family] ?: error("no skill for $family")

    /** 이 검이 스킬을 쓸 수 있는지. */
    fun unlocked(sword: Sword?): Boolean = sword != null && sword.level >= MIN_LEVEL

    /**
     * 스킬 발동 판정.
     *
     * @param skillRoll 0~1 난수. 기본 1.0 = 발동 없음.
     */
    fun roll(sword: Sword?, skillRoll: Double = 1.0): Skill? {
        if (!unlocked(sword)) return null
        if (skillRoll >= CHANCE) return null
        return of(sword!!.family)
    }
}
