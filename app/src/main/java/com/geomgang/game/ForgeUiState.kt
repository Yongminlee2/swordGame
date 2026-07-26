package com.geomgang.game

import com.geomgang.core.Difficulty
import com.geomgang.core.ForgeResult
import com.geomgang.core.PetKind
import com.geomgang.core.PetState
import com.geomgang.core.ProgressState
import com.geomgang.core.QuestState
import com.geomgang.core.Settings
import com.geomgang.core.Sword
import com.geomgang.core.WeaponFamily

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
    val successPercent: Int,
    val canForge: Boolean,
    val canBuySword: Boolean,
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
    val settings: Settings = Settings(),
    /** 자동강화가 도는 중인지. */
    val autoForging: Boolean = false,
    /** 자동강화를 켤 수 있는 상태인지 (안전구간 + 비용 충족). */
    val canAutoForge: Boolean = false,
    /** 보관함에 든 검. 조합과 재료 강화가 여기서 꺼내 쓴다. */
    val storage: List<Sword> = emptyList(),
    val storageCapacity: Int = 0,
    /** 방금 사냥에서 떨어진 검. 화면에 알린 뒤 비운다. */
    val lastDrop: Sword? = null,
    /** 보관함이 꽉 차서 드롭을 놓쳤는지. */
    val dropMissed: Boolean = false,
    /** 다음 강화에 태울 재료 수. 보관함의 낮은 검부터 자동으로 고른다. */
    val materialCount: Int = 0,
    val materialBonusPercent: Int = 0,
    val maxMaterials: Int = 0,
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
) {
    /** 방지권이든 줍기든 응답을 기다리는 중인지. */
    val awaitingDestroyChoice: Boolean get() = destroyPhase != DestroyPhase.None
}
