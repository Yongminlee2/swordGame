package com.geomgang.core

/**
 * 강화에 얹히는 보너스.
 *
 * @property successRate 성공률에 더하는 몫. 0.012 는 1.2%p 다
 * @property dropGuard 파괴 판정을 무효로 돌릴 확률
 */
data class ForgeBonus(
    val successRate: Double = 0.0,
    val dropGuard: Double = 0.0,
) {
    operator fun plus(other: ForgeBonus): ForgeBonus = ForgeBonus(
        successRate = successRate + other.successRate,
        dropGuard = dropGuard + other.dropGuard,
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
 * 출처가 넷(도감·스킬·고유검·든 검)인데 여러 군데서 더하면 왜 이 확률이 나왔는지
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
            // 화면에서 읽는 말이 기준이다. 도메인 이름([Smithy])과 달라도 된다.
            label = "스킬",
            detail = "Lv ${progress.smithyLevel} / ${Smithy.MAX_LEVEL}",
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
                bonus = ForgeBonus(forge.successBonus, forge.dropGuard),
            )
        },
    ) + listOfNotNull(
        // 시즌1 고유검 넷은 지니고만 있어도 값을 한다. 팔면 이 줄이 사라진다.
        ownedUniqueSource("시작의 검", UniqueSwords.originOwned(state), UniqueSwords.ORIGIN_FORGE_BONUS),
        ownedUniqueSource("탐식자", UniqueSwords.gluttonOwned(state), UniqueSwords.GLUTTON_FORGE_BONUS),
        ownedUniqueSource("폭풍우", UniqueSwords.tempestOwned(state), UniqueSwords.TEMPEST_FORGE_BONUS),
        ownedUniqueSource("삼위일체", UniqueSwords.trinityOwned(state), UniqueSwords.TRINITY_FORGE_BONUS),
    )

    private fun ownedUniqueSource(label: String, owned: Boolean, successRate: Double): BonusSource? =
        if (owned) BonusSource(label, detail = "소유 중", bonus = ForgeBonus(successRate = successRate)) else null

    fun of(state: GameState, progress: ProgressState): ForgeBonus =
        sourcesOf(state, progress).fold(ForgeBonus.NONE) { acc, s -> acc + s.bonus }
}
