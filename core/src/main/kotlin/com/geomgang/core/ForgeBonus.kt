package com.geomgang.core

/**
 * 강화에 얹히는 보너스.
 *
 * @property successRate 성공률에 더하는 몫. 0.012 는 1.2%p 다
 * @property destroyGuard 파괴 판정을 무효로 돌릴 확률
 */
data class ForgeBonus(
    val successRate: Double = 0.0,
    val destroyGuard: Double = 0.0,
) {
    operator fun plus(other: ForgeBonus): ForgeBonus = ForgeBonus(
        successRate = successRate + other.successRate,
        destroyGuard = destroyGuard + other.destroyGuard,
    )

    companion object {
        val NONE: ForgeBonus = ForgeBonus()
    }
}

/** 보너스 한 줄. 화면이 "왜 이 확률인지" 말해 주기 위한 것이다. */
data class BonusSource(
    val label: String,
    val detail: String,
    val bonus: ForgeBonus,
)

/**
 * 강화 보너스를 모으는 **유일한 곳**.
 *
 * 출처가 넷(도감·대장간·고유검·든 검)인데 여러 군데서 더하면 왜 이 확률이 나왔는지
 * 아무도 설명 못 하게 된다. 합산도 여기서만 하고, 화면이 출처별로 쪼개 보여 줄 수 있게
 * [sourcesOf] 로 낱개도 낸다.
 *
 * **시뮬레이터는 이 값을 쓰지 않는다.** 새 세이브는 네 출처가 전부 0이고, 그게
 * [com.geomgang.core.sim.BalanceSimulation] 이 검증하려는 "맨손 신규 플레이어" 다.
 * 시뮬레이터에 보너스를 넣으면 그 모형이 재려던 것 자체가 흐려진다.
 */
object ForgeBonuses {

    fun sourcesOf(state: GameState, progress: ProgressState): List<BonusSource> = listOf(
        BonusSource(
            label = "도감",
            detail = "${Progress.entriesOf(progress).size} / ${WeaponCatalog.ENTRIES.size}",
            bonus = CodexOffer.bonusOf(progress),
        ),
        BonusSource(
            label = "대장간",
            detail = "Lv ${progress.smithyLevel}",
            bonus = Smithy.bonusOf(progress),
        ),
        BonusSource(
            label = "고유검",
            detail = "${progress.uniqueFound.count { UniqueSwords.byId(it) != null }} / " +
                "${UniqueSwords.RECIPES.size}",
            bonus = UniqueSwords.holdingBonus(progress),
        ),
        FamilyForge.of(state.sword).let { forge ->
            BonusSource(
                label = if (state.sword?.isLegend() == true) "전설검" else "계열",
                detail = forge.blurb,
                bonus = ForgeBonus(forge.successBonus, forge.destroyGuard),
            )
        },
    )

    fun of(state: GameState, progress: ProgressState): ForgeBonus =
        sourcesOf(state, progress).fold(ForgeBonus.NONE) { acc, s -> acc + s.bonus }
}
