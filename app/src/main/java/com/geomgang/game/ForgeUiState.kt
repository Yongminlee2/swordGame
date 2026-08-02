package com.geomgang.game

import com.geomgang.core.BonusSource
import com.geomgang.core.Difficulty
import com.geomgang.core.ForgeResult
import com.geomgang.core.IdleReward
import com.geomgang.core.Item
import com.geomgang.core.OddsPercent
import com.geomgang.core.PetKind
import com.geomgang.core.PetState
import com.geomgang.core.ProgressState
import com.geomgang.core.QuestState
import com.geomgang.core.Settings
import com.geomgang.core.Sword
import com.geomgang.core.WeaponFamily

/**
 * 담금질 표시. 붙지 않는 구간이면 null 이다.
 *
 * @property fails 이 단계에 쌓인 실패 수
 * @property basePercent 담금질 없는 성공률(%)
 * @property currentPercent 담금질을 반영한 지금 성공률(%)
 * @property ratio 게이지 채움 정도. 0..1
 */
data class TemperUi(
    val fails: Int,
    val basePercent: Double,
    val currentPercent: Double,
    val ratio: Float,
    /** 실패 한 번이 더해 주는 성공률(%p). 0회일 때도 이 장치가 뭔지 알리려면 필요하다. */
    val gainPerFail: Double,
    /** 담금질만으로 닿을 수 있는 최대 성공률(%). */
    val maxPercent: Double,
)

/** 강화 화면이 그리는 데 필요한 것 전부. 도메인 상태를 화면 언어로 옮긴 것이다. */
data class ForgeUiState(
    val difficulty: Difficulty,
    val sword: Sword?,
    val gold: Long,
    val shards: Int,
    val preventTickets: Int,
    val blessingScrolls: Int,
    val luckCharms: Int,
    val bestLevel: Int,
    val upgradeCost: Long,
    val sellPrice: Long,
    /**
     * 이번 강화가 어떻게 끝날 수 있는지. 성공·유지·하락·파괴 넷을 더하면 100이다.
     *
     * 켜 둔 아이템이 반영된 값이다 — 축복서를 켜면 성공률이 오르고,
     * 행운부적을 켜면 하락·파괴가 0이 된다.
     */
    val odds: OddsPercent = OddsPercent(0, 0, 0, 0),
    /** 담금질. 무한 구간에서만 채워진다. */
    val temper: TemperUi? = null,
    /** 이번 성공이 최고 기록을 갈아치웠는지. */
    val isRecord: Boolean = false,
    val canForge: Boolean,
    val canBuySword: Boolean,
    /** 손에 든 검과 무관하게 보관함으로 바로 살 수 있는지. */
    val canBuyToStorage: Boolean = false,
    /** 지금 강화석 한 개 값. 살수록 오르고, 한 단계 올리면 되돌아온다. */
    val stonePrice: Long = 0,
    /** 그다음 개의 값. 왜 오르는지 화면이 직접 보여 줘야 짜증이 되지 않는다. */
    val nextStonePrice: Long = 0,
    val canBuyStone: Boolean = false,
    /** 지금 아이템 값. 살수록 오른다 — 화면이 상수를 다시 쓰지 않게 여기서 준다. */
    val itemPrices: Map<Item, Long> = emptyMap(),
    /** 이 구간에서 산 소모품 수. 값이 왜 올랐는지 화면이 말해 줘야 한다. */
    val itemsBought: Int = 0,
    /** 지금 검을 살 때 고를 수 있는 계열. 업적으로 늘어난다. */
    val unlockedFamilies: List<WeaponFamily>,
    /** 다음 강화에 축복서를 쓸지. 한 번 쓰면 자동으로 내려간다. */
    val useBlessing: Boolean = false,
    /** 다음 강화에 행운부적을 쓸지. 한 번 쓰면 자동으로 내려간다. */
    val useLuckCharm: Boolean = false,
    /** 마지막 강화 결과. 연출이 끝나면 null 로 돌아간다. */
    val lastResult: ForgeResult? = null,
    /** 파괴 이후 무엇을 기다리는 중인지. 제한 시간 진행도가 여기 들어 있다. */
    val destroyPhase: DestroyPhase = DestroyPhase.None,
    val canPrevent: Boolean = false,
    /** 연출 재생 중이거나 제한 시간 창이 열려 있으면 입력을 받지 않는다. */
    val busy: Boolean = false,
    /** 도감·업적·칭호·통계. 모드와 무관한 전역 진행도다. */
    val progress: ProgressState = ProgressState(),
    /** 자리를 비운 사이 쌓인 보상. 켜자마자 한 번 알리고 비운다. */
    val idleReward: IdleReward? = null,
    val settings: Settings = Settings(),
    /** 보관함에 든 검. 조합과 재료 강화가 여기서 꺼내 쓴다. */
    val storage: List<Sword> = emptyList(),
    val storageCapacity: Int = 0,
    /** 별 강화 정보. 검이 조건을 못 갖추면 null. */
    val star: StarUiState? = null,
    /** 사냥 중일 때만 채워진다. */
    val hunt: HuntUiState? = null,
    /** 지금 검의 공격력. 사냥터 밖에서도 보여 준다. */
    val attackPower: Long = 0,
    /** 일일·주간 퀘스트. */
    val quests: QuestState = QuestState(),
    /** 일일 3개의 진행도 (quests.daily 와 같은 순서). */
    val questProgress: List<Int> = emptyList(),
    val weeklyProgress: Int = 0,
    /** 수령 가능한 퀘스트가 하나라도 있는지. 강화 화면 배지가 이걸 본다. */
    val questClaimable: Boolean = false,
    /** 구역 정수(Zone id → 개수). 고유검 레시피의 재료다. */
    val essences: Map<String, Int> = emptyMap(),
    /** 펫 보유·장착 상태. */
    val pets: PetState = PetState(),
    /** 방금 얻은 펫 알. 화면이 알린 뒤 비운다. */
    val lastEgg: PetKind? = null,
    /** 무한 회랑 런. 밖에서는 null. */
    val gauntlet: GauntletUiState? = null,
    /** 회랑이 열렸는지 (화산 보스 처치). */
    val gauntletUnlocked: Boolean = false,
    val gauntletBest: Int = 0,
    /** 강화석 보유량. 고단계 강화의 화폐다. */
    val forgeStones: Int = 0,
    /** 다음 강화에 필수인 강화석. */
    val requiredStones: Int = 0,
    /** 강화할 수 없는 이유 한 줄. 가능하면 null. */
    val forgeBlockedReason: String? = null,
    /** 강화 보너스의 출처별 내역. 화면이 "왜 이 확률인지" 말해 준다. */
    val bonusSources: List<BonusSource> = emptyList(),
    /**
     * 스킬 — 골드로 사는 영구 확률.
     *
     * 도메인 이름은 [com.geomgang.core.Smithy] 지만 화면에서는 "스킬"이다.
     * 저장 필드(`smithyLevel`)는 그대로 둔다 — 이름을 바꾸면 옛 세이브가 값을 잃는다.
     */
    val skillLevel: Int = 0,
    val skillPrice: Long = 0,
    val canUpgradeSkill: Boolean = false,
    /** 전설검 재료 중 아직 없는 것. 목표가 보여야 모으고 싶어진다. */
    val legendMissing: List<WeaponFamily> = emptyList(),
    val canCraftLegend: Boolean = false,
    val canRecraftLegend: Boolean = false,
    /** 전설검을 한 번이라도 도감에 바쳤는지. 켜지면 조각으로 다시 벼릴 수 있다. */
    val legendUnlocked: Boolean = false,
    /**
     * 용검을 손에 쥔 뒤의 「깊은 국면」인지([com.geomgang.core.Unlocks]).
     *
     * 꺼져 있으면 조각·강화석이 화면에 없고 사냥터도 잠긴다. 초반은 골드 하나로
     * **강화 → 판매 → 강화** 만 한다.
     */
    val deepUnlocked: Boolean = false,
    val huntOpen: Boolean = false,
    /** 가진 정수를 전부 무게로 환산한 값([com.geomgang.core.Essences]). */
    val essencePower: Int = 0,
    /** 수호 각인을 지녔는지. 한 장뿐이라 개수가 아니다. */
    val wardCharm: Boolean = false,
    val wardCharmCost: Int = 0,
    val canBuyWardCharm: Boolean = false,
) {
    /** 방지권이든 줍기든 응답을 기다리는 중인지. */
    val awaitingDestroyChoice: Boolean get() = destroyPhase != DestroyPhase.None
}
