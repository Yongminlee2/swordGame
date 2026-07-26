package com.geomgang.game.ui

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.geomgang.core.WeaponFamily
import com.geomgang.game.R

/**
 * 검 그림의 단일 출처.
 *
 * **계열이 어떤 실루엣 묶음을 쓰는지**, **강화 단계가 그 묶음의 몇 번째를 쓰는지**를 정한다.
 * 단계가 오르면 실루엣이 실제로 바뀐다 — +9, +10, +11 이 서로 다른 검이다.
 * 예전에는 티어 하나가 2~3단계를 덮어서 같은 그림이 반복됐다.
 *
 * 실루엣은 game-icons.net 의 CC BY 3.0 아이콘이다. SVG 의 path 만 뽑아
 * VectorDrawable 로 넣었으므로 비트맵이 하나도 없다. 표기는 설정 → 라이선스 화면에 있다.
 *
 * 나중에 다른 에셋으로 갈아 끼우려면 이 파일의 표만 바꾸면 된다.
 */
object SwordArt {

    /** 유한 모드의 최대 단계. 계열마다 이 개수 + 1 만큼 실루엣을 갖는다. */
    private const val MAX_LEVEL = 20

    /**
     * 계열별 실루엣 묶음. 인덱스가 강화 단계다.
     *
     * 아이콘 64개로 8계열 × 21단계를 채우므로 계열 간에는 일부 실루엣이 겹친다.
     * 다만 **같은 단계에서 두 계열이 같은 실루엣을 쓰지 않도록** 배치했고,
     * `SwordArtTest` 가 그것을 검사한다.
     */
    private val SILHOUETTES: Map<WeaponFamily, IntArray> = mapOf(
        // 직검 — 곧은 양날, 정통적인 검
        WeaponFamily.STRAIGHT to intArrayOf(
            R.drawable.sw_delapouite_rusty_sword,
            R.drawable.sw_lorc_broadsword,
            R.drawable.sw_lorc_pointy_sword,
            R.drawable.sw_lorc_shining_sword,
            R.drawable.sw_lorc_striped_sword,
            R.drawable.sw_lorc_relic_blade,
            R.drawable.sw_lorc_rune_sword,
            R.drawable.sw_delapouite_ancient_sword,
            R.drawable.sw_lorc_piercing_sword,
            R.drawable.sw_lorc_winged_sword,
            R.drawable.sw_lorc_zeus_sword,
            R.drawable.sw_lorc_thunder_blade,
            R.drawable.sw_lorc_energy_sword,
            R.drawable.sw_lorc_lightning_saber,
            R.drawable.sw_delapouite_diamond_hilt,
            R.drawable.sw_delapouite_spiral_hilt,
            R.drawable.sw_lorc_sword_hilt,
            R.drawable.sw_lorc_bloody_sword,
            R.drawable.sw_lorc_shard_sword,
            R.drawable.sw_lorc_fragmented_sword,
            R.drawable.sw_lorc_dripping_sword,
        ),

        // 곡도 — 휜 한쪽날
        WeaponFamily.CURVED to intArrayOf(
            R.drawable.sw_delapouite_katana,
            R.drawable.sw_lorc_cracked_saber,
            R.drawable.sw_lorc_crescent_blade,
            R.drawable.sw_skoll_crescent_blade,
            R.drawable.sw_lorc_machete,
            R.drawable.sw_delapouite_sickle,
            R.drawable.sw_lorc_scythe,
            R.drawable.sw_lorc_reaper_scythe,
            R.drawable.sw_lorc_curvy_knife,
            R.drawable.sw_delapouite_bat_leth,
            R.drawable.sw_delapouite_chakram,
            R.drawable.sw_lorc_sparkling_sabre,
            R.drawable.sw_delapouite_razor,
            R.drawable.sw_delapouite_sai,
            R.drawable.sw_delapouite_cleaver,
            R.drawable.sw_lorc_meat_cleaver,
            R.drawable.sw_delapouite_axe_sword,
            R.drawable.sw_delapouite_glaive,
            R.drawable.sw_lorc_croc_sword,
            R.drawable.sw_lorc_gooey_sword,
            R.drawable.sw_lorc_dripping_blade,
        ),

        // 대검 — 크고 두껍다
        WeaponFamily.GREAT to intArrayOf(
            R.drawable.sw_delapouite_two_handed_sword,
            R.drawable.sw_delapouite_sverd_i_fjell,
            R.drawable.sw_lorc_broadsword,
            R.drawable.sw_skoll_gladius,
            R.drawable.sw_delapouite_cleaver,
            R.drawable.sw_lorc_meat_cleaver,
            R.drawable.sw_delapouite_axe_sword,
            R.drawable.sw_delapouite_glaive,
            R.drawable.sw_lorc_croc_sword,
            R.drawable.sw_lorc_shattered_sword,
            R.drawable.sw_lorc_gooey_sword,
            R.drawable.sw_lorc_relic_blade,
            R.drawable.sw_lorc_striped_sword,
            R.drawable.sw_lorc_zeus_sword,
            R.drawable.sw_lorc_thunder_blade,
            R.drawable.sw_lorc_winged_sword,
            R.drawable.sw_lorc_energy_sword,
            R.drawable.sw_lorc_shard_sword,
            R.drawable.sw_lorc_fragmented_sword,
            // +20 은 직검이 dripping_sword 를 쓰므로 순서를 바꿨다
            R.drawable.sw_lorc_dripping_sword,
            R.drawable.sw_lorc_bloody_sword,
        ),

        // 세검 — 가늘고 길다
        WeaponFamily.RAPIER to intArrayOf(
            R.drawable.sw_lorc_stiletto,
            R.drawable.sw_skoll_stiletto,
            R.drawable.sw_skoll_bayonet,
            R.drawable.sw_lorc_scalpel,
            R.drawable.sw_lorc_plain_dagger,
            R.drawable.sw_lorc_broad_dagger,
            R.drawable.sw_lorc_piercing_sword,
            R.drawable.sw_lorc_pointy_sword,
            R.drawable.sw_delapouite_sai,
            R.drawable.sw_delapouite_razor,
            R.drawable.sw_skoll_switchblade,
            R.drawable.sw_skoll_butterfly_knife,
            R.drawable.sw_delapouite_butterfly_knife,
            R.drawable.sw_lorc_bowie_knife,
            R.drawable.sw_skoll_bowie_knife,
            R.drawable.sw_skoll_trench_knife,
            R.drawable.sw_lorc_bone_knife,
            R.drawable.sw_delapouite_bone_knife,
            R.drawable.sw_lorc_thrown_knife,
            R.drawable.sw_lorc_dripping_knife,
            R.drawable.sw_lorc_sacrificial_dagger,
        ),

        // 쌍검 — 반드시 두 자루로 보여야 한다.
        // 다른 계열이 쓰는 단검 아이콘은 한 자루라서 쌍검에 쓰면 이름과 그림이 어긋난다.
        // 그래서 두 자루가 함께 그려진 아이콘 10개만으로 21단계를 채운다.
        WeaponFamily.TWIN to intArrayOf(
            R.drawable.sw_lorc_daggers,
            R.drawable.sw_lorc_crossed_swords,
            R.drawable.sw_lorc_dervish_swords,
            R.drawable.sw_lorc_thrown_daggers,
            R.drawable.sw_lorc_crossed_sabres,
            R.drawable.sw_delapouite_light_sabers,
            R.drawable.sw_lorc_sabers_choc,
            R.drawable.sw_lorc_all_for_one,
            R.drawable.sw_lorc_sword_array,
            R.drawable.sw_lorc_battle_gear,
            R.drawable.sw_lorc_daggers,
            R.drawable.sw_lorc_dervish_swords,
            R.drawable.sw_lorc_crossed_sabres,
            R.drawable.sw_lorc_sabers_choc,
            R.drawable.sw_lorc_sword_array,
            R.drawable.sw_lorc_crossed_swords,
            R.drawable.sw_lorc_thrown_daggers,
            R.drawable.sw_delapouite_light_sabers,
            R.drawable.sw_lorc_all_for_one,
            R.drawable.sw_lorc_battle_gear,
            R.drawable.sw_lorc_dervish_swords,
        ),

        // 마검 — 흉하고 피가 흐른다
        WeaponFamily.DEMON to intArrayOf(
            R.drawable.sw_lorc_bloody_sword,
            R.drawable.sw_lorc_sacrificial_dagger,
            R.drawable.sw_lorc_bone_knife,
            R.drawable.sw_delapouite_bone_knife,
            R.drawable.sw_lorc_dripping_sword,
            R.drawable.sw_lorc_dripping_knife,
            R.drawable.sw_lorc_dripping_blade,
            R.drawable.sw_lorc_gooey_sword,
            // +8 은 대검이 croc_sword 를 쓰므로 여기서는 machete 를 쓴다.
            // 같은 단계에서 계열끼리 실루엣이 겹치면 SwordArtTest 가 잡는다.
            R.drawable.sw_lorc_machete,
            R.drawable.sw_lorc_shard_sword,
            R.drawable.sw_lorc_fragmented_sword,
            R.drawable.sw_lorc_shattered_sword,
            R.drawable.sw_lorc_scythe,
            R.drawable.sw_lorc_reaper_scythe,
            R.drawable.sw_lorc_meat_cleaver,
            R.drawable.sw_delapouite_cleaver,
            R.drawable.sw_lorc_curvy_knife,
            R.drawable.sw_skoll_trench_knife,
            R.drawable.sw_delapouite_sickle,
            R.drawable.sw_lorc_croc_sword,
            R.drawable.sw_lorc_cracked_saber,
        ),

        // 성검 — 빛나고 정교하다
        WeaponFamily.HOLY to intArrayOf(
            R.drawable.sw_lorc_shining_sword,
            R.drawable.sw_lorc_relic_blade,
            R.drawable.sw_lorc_winged_sword,
            R.drawable.sw_lorc_zeus_sword,
            R.drawable.sw_delapouite_ancient_sword,
            R.drawable.sw_delapouite_sverd_i_fjell,
            R.drawable.sw_delapouite_diamond_hilt,
            R.drawable.sw_delapouite_spiral_hilt,
            R.drawable.sw_lorc_sword_hilt,
            R.drawable.sw_lorc_rune_sword,
            R.drawable.sw_lorc_striped_sword,
            R.drawable.sw_lorc_broadsword,
            R.drawable.sw_skoll_gladius,
            R.drawable.sw_delapouite_two_handed_sword,
            R.drawable.sw_lorc_energy_sword,
            R.drawable.sw_lorc_lightning_saber,
            R.drawable.sw_lorc_thunder_blade,
            R.drawable.sw_lorc_sparkling_sabre,
            R.drawable.sw_delapouite_glaive,
            R.drawable.sw_delapouite_bat_leth,
            R.drawable.sw_delapouite_chakram,
        ),

        // 용검 — 화려하고 기괴하다
        WeaponFamily.DRAGON to intArrayOf(
            R.drawable.sw_lorc_croc_sword,
            R.drawable.sw_lorc_gooey_sword,
            R.drawable.sw_lorc_energy_sword,
            R.drawable.sw_lorc_thunder_blade,
            R.drawable.sw_lorc_lightning_saber,
            R.drawable.sw_lorc_zeus_sword,
            R.drawable.sw_lorc_winged_sword,
            R.drawable.sw_lorc_shard_sword,
            R.drawable.sw_lorc_fragmented_sword,
            // +9 는 대검, +10 은 곡도가 먼저 쓰고 있어 순서를 옮겼다
            R.drawable.sw_delapouite_glaive,
            R.drawable.sw_delapouite_axe_sword,
            R.drawable.sw_delapouite_bat_leth,
            R.drawable.sw_lorc_shattered_sword,
            R.drawable.sw_delapouite_chakram,
            R.drawable.sw_lorc_sparkling_sabre,
            R.drawable.sw_lorc_crescent_blade,
            R.drawable.sw_skoll_crescent_blade,
            R.drawable.sw_lorc_scythe,
            R.drawable.sw_lorc_reaper_scythe,
            R.drawable.sw_delapouite_katana,
            R.drawable.sw_delapouite_two_handed_sword,
        ),
    )

    /**
     * 단계별 색.
     *
     * 티어 단위가 아니라 **단계 단위**로 둔다. 인접한 단계가 색까지 같으면
     * 실루엣이 바뀌어도 변화가 눈에 덜 들어온다.
     */
    private val PALETTES: Array<SwordPalette> = arrayOf(
        SwordPalette(Color(0xFF7C7468), Color(0xFF9A9184), 0f), // +0 녹슨
        SwordPalette(Color(0xFF8A8276), Color(0xFFA8A093), 0f), // +1
        SwordPalette(Color(0xFF9A9184), Color(0xFFB8B0A2), 0f), // +2
        SwordPalette(Color(0xFFA6ADB6), Color(0xFFD2D9E2), 0f), // +3 강철
        SwordPalette(Color(0xFFB2BAC4), Color(0xFFDDE4EC), 0f), // +4
        SwordPalette(Color(0xFFC0C8D2), Color(0xFFE8EFF6), 0f), // +5
        SwordPalette(Color(0xFFD5DCE6), Color(0xFFF4F8FF), 0f), // +6 은장
        SwordPalette(Color(0xFFDDE4EE), Color(0xFFFFFFFF), 0f), // +7
        SwordPalette(Color(0xFFC8D8E8), Color(0xFFF0F8FF), 0.06f), // +8
        SwordPalette(Color(0xFF8FC9D6), Color(0xFFCDF2FA), 0.10f), // +9 룬
        SwordPalette(Color(0xFF6FBFD2), Color(0xFFB8ECF8), 0.14f), // +10
        SwordPalette(Color(0xFF4FB4CE), Color(0xFFA0E6F6), 0.18f), // +11
        SwordPalette(Color(0xFFE08240), Color(0xFFFFC98A), 0.22f), // +12 화염
        SwordPalette(Color(0xFFEE7028), Color(0xFFFFB268), 0.26f), // +13
        SwordPalette(Color(0xFFFF5E14), Color(0xFFFF9E4A), 0.30f), // +14
        SwordPalette(Color(0xFFE8D556), Color(0xFFFFF6B8), 0.36f), // +15 뇌전
        SwordPalette(Color(0xFFF5E63C), Color(0xFFFFFCD0), 0.42f), // +16
        SwordPalette(Color(0xFFF3EEDA), Color(0xFFFFFFFF), 0.48f), // +17 여명
        SwordPalette(Color(0xFFFFF8C8), Color(0xFFFFFFFF), 0.54f), // +18
        SwordPalette(Color(0xFF8B5BC9), Color(0xFFD6B4FF), 0.62f), // +19 흑룡참
        SwordPalette(Color(0xFFA05CFF), Color(0xFFEAD4FF), 0.70f), // +20
    )

    /** 무한 모드 +21 이상. 오라만 계속 세진다. */
    private val ENDLESS_PALETTES: Array<SwordPalette> = arrayOf(
        SwordPalette(Color(0xFFD8497E), Color(0xFFFFA8C6), 0.76f), // 용린참
        SwordPalette(Color(0xFF3A46B0), Color(0xFF8A96FF), 0.84f), // 심연검
        SwordPalette(Color(0xFFF2F2F2), Color(0xFFFFFFFF), 0.92f), // 이름 없는 검
    )

    /** 이 계열·단계의 검 실루엣. */
    @DrawableRes
    fun drawableFor(family: WeaponFamily, level: Int): Int {
        require(level >= 0) { "level must be >= 0, was $level" }
        val set = SILHOUETTES.getValue(family)
        // 무한 구간은 최상위 실루엣들을 순환한다
        return if (level <= MAX_LEVEL) set[level] else set[MAX_LEVEL - (level - MAX_LEVEL - 1) % 4]
    }

    /** 이 단계의 색과 오라. */
    fun paletteFor(level: Int): SwordPalette {
        require(level >= 0) { "level must be >= 0, was $level" }
        if (level <= MAX_LEVEL) return PALETTES[level]
        val step = (level - MAX_LEVEL - 1) / 5
        return ENDLESS_PALETTES[step.coerceAtMost(ENDLESS_PALETTES.lastIndex)]
    }

    /** 테스트가 쓰는 값. */
    internal fun silhouetteSets(): Map<WeaponFamily, IntArray> = SILHOUETTES

    internal fun maxLevel(): Int = MAX_LEVEL
}

/** 검을 그릴 때 쓰는 색 한 벌. */
data class SwordPalette(
    val blade: Color,
    val glow: Color,
    val auraAlpha: Float,
) {
    /**
     * 아래쪽 그늘 색. 날 색을 어둡게 깎아 만든다.
     *
     * 단계마다 따로 적지 않는 이유: 그늘은 날 색에서 파생되는 값이라
     * 손으로 21개를 적으면 어긋날 수 있다.
     */
    val shade: Color
        get() = Color(
            red = blade.red * 0.42f,
            green = blade.green * 0.42f,
            blue = blade.blue * 0.42f,
            alpha = 1f,
        )
}

