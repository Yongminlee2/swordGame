package com.geomgang.core

/**
 * 용검이 강화 구간별로 계승하는 스킬.
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
 * 사냥터와 함께 열리는 용검만 탭할 때 [CHANCE] 확률로 스킬을 쓴다.
 * 기존 계열의 스킬은 용검의 강화 구간으로 옮겨 왔으며, 원래 검에서는 발동하지 않는다.
 *
 * 판정은 난수를 값으로 받는다 — 치명타와 같은 방식이라 테스트가 결정적이다.
 */
object Skills {

    /** 용검은 사냥터와 함께 열리므로 제작 직후부터 스킬을 쓴다. */
    const val DRAGON_MIN_LEVEL = LegendForge.CRAFT_LEVEL

    /** 탭 한 번에 스킬이 터질 확률. */
    const val CHANCE = 0.12

    /** 화상 폭발이 한 번에 넣는 피해 = 초당 화상 × 이 값. */
    const val BURN_BURST_MULT = 10

    private val BY_FAMILY: Map<WeaponFamily, Skill> = mapOf(
        WeaponFamily.STRAIGHT to Skill(
            "flash", "일섬", damageMult = 3.0, hits = 1,
            blurb = "3배 강타",
        ),
        WeaponFamily.CURVED to Skill(
            "moonfall", "월광참", damageMult = 2.5, hits = 1,
            blurb = "2.5배 참격",
        ),
        WeaponFamily.GREAT to Skill(
            "collapse", "붕괴", damageMult = 5.0, hits = 1,
            blurb = "5배 강타",
        ),
        WeaponFamily.RAPIER to Skill(
            "flurry", "연속 찌르기", damageMult = 4.0, hits = 4,
            blurb = "4연타 · 합 4배",
        ),
        WeaponFamily.TWIN to Skill(
            "twinmoon", "쌍월", damageMult = 4.0, hits = 2,
            blurb = "2연타 · 합 4배",
        ),
        WeaponFamily.DEMON to Skill(
            "drain", "흡혈", damageMult = 3.0, hits = 1, shardBonus = 1,
            blurb = "3배 · 조각 +1",
        ),
        WeaponFamily.HOLY to Skill(
            "judgment", "심판", damageMult = 3.0, hits = 1, bossMult = 2.0,
            blurb = "보스 6배 · 일반 3배",
        ),
        WeaponFamily.DRAGON to Skill(
            "dragonbreath", "용의 숨결", damageMult = 5.0, hits = 1, burnBurst = true,
            blurb = "5배 · 화상 폭발",
        ),
        WeaponFamily.SCYTHE to Skill(
            "reap", "사신의 낫", damageMult = 3.0, hits = 1, maxHpRatio = 0.05,
            blurb = "3배 · 체력 5%",
        ),
        WeaponFamily.AXE to Skill(
            "crush", "분쇄", damageMult = 6.0, hits = 1,
            blurb = "6배 강타",
        ),
        WeaponFamily.SPEAR to Skill(
            "pierce", "관통", damageMult = 3.6, hits = 3,
            blurb = "3연타 · 합 3.6배",
        ),
        WeaponFamily.SPIRIT to Skill(
            "spiritburst", "정령 폭발", damageMult = 3.0, hits = 1, burnBurst = true,
            blurb = "3배 · 화상 폭발",
        ),
        WeaponFamily.FUSED to Skill(
            "allthings", "만상", damageMult = 4.0, hits = 1,
            blurb = "4배 만상격",
        ),
        WeaponFamily.VOID to Skill(
            "voidcall", "공허", damageMult = 3.0, hits = 1, maxHpRatio = 0.08,
            blurb = "3배 · 체력 8%",
        ),
    )

    fun of(family: WeaponFamily): Skill =
        BY_FAMILY[family] ?: error("no skill for $family")

    /** 용검이 강화되며 계승하는 스킬 구간. 후반일수록 화력과 특수 효과가 커진다. */
    data class DragonStage(
        val levels: IntRange,
        val sourceFamily: WeaponFamily,
    ) {
        val skill: Skill get() = of(sourceFamily)

        val rangeLabel: String get() = if (levels.last == Int.MAX_VALUE) {
            "+${levels.first} 이상"
        } else {
            "+${levels.first}~+${levels.last}"
        }
    }

    private val DRAGON_STAGES: List<DragonStage> = listOf(
        DragonStage(1..7, WeaponFamily.CURVED),
        DragonStage(8..14, WeaponFamily.STRAIGHT),
        DragonStage(15..20, WeaponFamily.DEMON),
        DragonStage(21..27, WeaponFamily.RAPIER),
        DragonStage(28..34, WeaponFamily.GREAT),
        DragonStage(35..41, WeaponFamily.HOLY),
        DragonStage(42..Int.MAX_VALUE, WeaponFamily.DRAGON),
    )

    fun dragonStage(level: Int): DragonStage =
        DRAGON_STAGES.first { level.coerceAtLeast(DRAGON_MIN_LEVEL) in it.levels }

    /** 실제 검이 쓸 스킬. 용검 외의 검에 요청하는 것은 규칙 위반이다. */
    fun of(sword: Sword): Skill {
        require(sword.family == WeaponFamily.DRAGON) { "skills are exclusive to dragon swords" }
        return dragonStage(sword.level).skill
    }

    fun stageLabel(sword: Sword): String? =
        if (sword.family == WeaponFamily.DRAGON) dragonStage(sword.level).rangeLabel else null

    fun dragonProgressionText(): String = DRAGON_STAGES.joinToString(" · ") {
        "${it.rangeLabel} ${it.skill.name}"
    }

    /** 이 검이 스킬을 쓸 수 있는지. */
    fun unlocked(sword: Sword?): Boolean =
        sword?.family == WeaponFamily.DRAGON && sword.level >= DRAGON_MIN_LEVEL

    /**
     * 스킬 발동 판정.
     *
     * @param skillRoll 0~1 난수. 기본 1.0 = 발동 없음.
     */
    fun roll(sword: Sword?, skillRoll: Double = 1.0): Skill? {
        if (!unlocked(sword)) return null
        if (skillRoll >= CHANCE) return null
        return of(sword!!)
    }
}
