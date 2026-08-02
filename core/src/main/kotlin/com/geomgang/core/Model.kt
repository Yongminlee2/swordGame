package com.geomgang.core

import kotlinx.serialization.Serializable

/** 소비 아이템. */
enum class Item(val id: String, val displayName: String) {
    /** 파괴를 1회 무효화한다. 파괴 판정 직후 제한 시간 안에 눌러야 한다. */
    PREVENT_TICKET("prevent", "방지권"),

    /** 다음 1회 성공률을 [RateTable.BLESSING_BONUS] 만큼 올린다. */
    BLESSING_SCROLL("blessing", "축복서"),

    /**
     * 다음 1회 실패해도 **하락만** 막는다. 파괴는 그대로다(v2.3) —
     * 부적이 파괴까지 막으면 방지권이 죽은 물건이 된다.
     */
    LUCK_CHARM("luck", "행운부적"),
}

/**
 * 무기 계열. 외형만 다르고 확률·경제에는 전혀 영향을 주지 않는다.
 * 해금 조건은 Progress 가 관리한다.
 */
enum class WeaponFamily(val id: String, val displayName: String) {
    STRAIGHT("straight", "직검"),
    CURVED("curved", "곡도"),
    GREAT("great", "대검"),
    RAPIER("rapier", "세검"),
    TWIN("twin", "쌍검"),
    DEMON("demon", "마검"),
    HOLY("holy", "성검"),
    DRAGON("dragon", "용검"),
    SCYTHE("scythe", "낫검"),
    AXE("axe", "도끼검"),
    SPEAR("spear", "창검"),
    SPIRIT("spirit", "정령검"),
    FUSED("fused", "합검"),
    VOID("void", "허검"),
    ;

    companion object {
        /** 처음부터 쓸 수 있는 계열. 직검 하나뿐이다 - 나머지는 조건으로 열린다. */
        val STARTERS: List<WeaponFamily> = listOf(STRAIGHT)

        /**
         * 상점·드롭에 나올 수 있는 기본 계열 4종.
         *
         * 직검만 처음부터고 나머지 셋은 진행도 조건으로 열린다([Progress.unlockedFamilies]).
         * 이 넷을 뺀 10계열은 **조합이나 회랑 보상으로만** 얻는다 -
         * 계열이 열리는 것 자체가 진행의 이정표가 되게 한 설계다.
         */
        val BASICS: List<WeaponFamily> = listOf(STRAIGHT, CURVED, GREAT, RAPIER)

        /**
         * 상점·드롭에 절대 나오지 않는 특수 계열.
         * 합검은 서로 다른 4계열 조합, 허검은 무한 회랑 보상으로만 얻는다.
         */
        val SPECIAL: Set<WeaponFamily> = setOf(FUSED, VOID)

        /**
         * v2.1에서 게임에 노출되는 계열 — **노출 여부의 단일 출처.**
         *
         * 계열 14종·고유검 10종이 첫 화면 언저리에 전부 있으니 처음 하는 사람이
         * 읽을 것이 너무 많았다. 일곱 자루만 남기고 나머지는 **숨긴다. 지우지 않는다** —
         * enum·저장 필드·특성 코드는 그대로라, 되살릴 때 이 목록에 넣기만 하면 된다.
         * 옛 세이브의 숨긴 계열 검도 그대로 보이고 쓰인다. 새로 얻을 길만 없다.
         *
         * 용검은 여기 있지만 +21(전설) 전용이다. 20강 이하 용검은 만들 수 없다.
         */
        val VISIBLE: List<WeaponFamily> =
            listOf(STRAIGHT, CURVED, GREAT, RAPIER, DEMON, HOLY, DRAGON)

        /** 도감에 계열 구획(+0~+20)을 가지는 계열. 용검은 전설 구획에만 산다. */
        val CODEX_FAMILIES: List<WeaponFamily> = VISIBLE - DRAGON

        fun fromId(id: String): WeaponFamily =
            entries.firstOrNull { it.id == id }
                ?: throw IllegalArgumentException("unknown family id: $id")
    }
}

/**
 * 보유 중인 검 한 자루.
 *
 * [stars] 는 강화 단계와 별개의 계층이다. 단계는 파괴 위험을 안고 올리는 것이고,
 * 별은 파괴 없이 조각을 태워 올리는 것이다. 자세한 규칙은 [StarForce] 에 있다.
 * 기본값이 0이라 별이 없던 옛 세이브도 그대로 읽힌다.
 */
@Serializable
data class Sword(
    val family: WeaponFamily,
    val level: Int,
    val stars: Int = 0,
    /**
     * 고유검 id. 숨은 레시피 조합으로만 채워진다. null = 평범한 검.
     * 이름·패시브 정의는 [UniqueSwords] 에 있다. 기본값이라 옛 세이브 호환.
     */
    val uniqueId: String? = null,
) {
    init {
        require(level >= 0) { "level must be >= 0, was $level" }
        require(stars >= 0) { "stars must be >= 0, was $stars" }
    }
}

/** 소비 아이템 보유량. */
@Serializable
data class Inventory(
    val preventTickets: Int = 0,
    val blessingScrolls: Int = 0,
    val luckCharms: Int = 0,
) {
    init {
        require(preventTickets >= 0) { "preventTickets must be >= 0, was $preventTickets" }
        require(blessingScrolls >= 0) { "blessingScrolls must be >= 0, was $blessingScrolls" }
        require(luckCharms >= 0) { "luckCharms must be >= 0, was $luckCharms" }
    }

    fun countOf(item: Item): Int = when (item) {
        Item.PREVENT_TICKET -> preventTickets
        Item.BLESSING_SCROLL -> blessingScrolls
        Item.LUCK_CHARM -> luckCharms
    }

    fun plus(item: Item, n: Int): Inventory {
        require(n >= 0) { "n must be >= 0, was $n" }
        return when (item) {
            Item.PREVENT_TICKET -> copy(preventTickets = preventTickets + n)
            Item.BLESSING_SCROLL -> copy(blessingScrolls = blessingScrolls + n)
            Item.LUCK_CHARM -> copy(luckCharms = luckCharms + n)
        }
    }

    /** 보유량보다 많이 빼려 하면 init 블록의 require 가 걸려 예외가 난다. */
    fun minus(item: Item, n: Int): Inventory {
        require(n >= 0) { "n must be >= 0, was $n" }
        return when (item) {
            Item.PREVENT_TICKET -> copy(preventTickets = preventTickets - n)
            Item.BLESSING_SCROLL -> copy(blessingScrolls = blessingScrolls - n)
            Item.LUCK_CHARM -> copy(luckCharms = luckCharms - n)
        }
    }
}

/**
 * 파괴 판정이 났지만 방지권/줍기 응답을 아직 받지 못한 상태.
 *
 * 이 값이 세이브에 남아 있는 채로 앱이 다시 켜지면 파괴를 확정 처리한다.
 * 그렇게 하지 않으면 방지권 대기 중 강제 종료로 파괴를 무효화할 수 있다.
 */
@Serializable
data class PendingDestroy(val family: WeaponFamily, val level: Int)

/** 한 모드의 전체 진행 상태. */
@Serializable
data class GameState(
    val difficulty: Difficulty,
    val gold: Long = 0,
    val shards: Int = 0,
    val sword: Sword? = null,
    val inventory: Inventory = Inventory(),
    val bestLevel: Int = 0,
    val pendingDestroy: PendingDestroy? = null,
    /** 사냥 진행. 기본값이 있어 이 필드가 없던 옛 세이브도 그대로 읽힌다. */
    val adventure: AdventureState = AdventureState(),
    /** 보관함. 조합과 재료 강화가 여기서 검을 꺼내 쓴다. */
    val storage: List<Sword> = emptyList(),
    /** 일일·주간 퀘스트. 날짜 키가 바뀌면 새로 뽑힌다. */
    val quests: QuestState = QuestState(),
    /** 구역 정수(Zone id → 개수). 보스가 주고 고유검 레시피가 먹는다. */
    val essences: Map<String, Int> = emptyMap(),
    /** 펫 보유·장착 상태. */
    val pets: PetState = PetState(),
    /** 무한 회랑 최고 기록(깬 층). */
    val gauntletBest: Int = 0,
    /** 강화석. 고단계 강화만 먹는 자원이다 - 조각과 수요가 겹치지 않게 분리했다. */
    val forgeStones: Int = 0,
    /**
     * 이 구간에서 골드로 산 강화석 수. 살수록 값이 오른다([GoldShop]).
     *
     * 최고 단계가 오르면 0으로 풀린다 — 그래야 골드가 다음 한 단계에 다시 묶인다.
     */
    val stonesBought: Int = 0,
    /** 이 구간에서 골드로 산 재료 검 수. */
    val swordsBought: Int = 0,
    /**
     * 수호 각인을 지니고 있는지([WardCharm]).
     *
     * 전설검이 미끄러질 때 한 번 붙들어 준다. **한 장뿐이다** — 개수가 아니라
     * 참/거짓인 이유가 그것이다. 기본값이라 옛 세이브가 그대로 열린다.
     */
    val wardCharm: Boolean = false,
    /**
     * 이 구간에서 골드로 산 소모품 수(방지권·축복서·행운부적 합산).
     *
     * 셋이 카운터를 함께 쓴다 — 따로 세면 셋 다 쟁이는 것이 늘 최선이 되어
     * 고를 것이 없어진다.
     */
    val itemsBought: Int = 0,
    /** 누진 카운터를 마지막으로 푼 최고 단계. */
    val priceBandLevel: Int = 0,
    /**
     * 마지막으로 저장된 시각(epoch ms). 자리비움 보상이 이 값과 지금을 견준다.
     *
     * 0 은 "모른다"는 뜻이다 — 이 필드가 없던 옛 세이브가 0 으로 읽히고,
     * 그때는 보상을 주지 않는다. 처음 저장되는 순간부터 시계가 돈다.
     */
    val lastSeenMillis: Long = 0,
    /**
     * 담금질이 쌓인 목표 단계. 단계가 바뀌면 누적을 버린다.
     *
     * 0 은 "쌓인 것 없음"이다 — +1 을 노리는 시도의 targetLevel 이 1 이므로
     * 0 과 겹치지 않는다.
     */
    val temperLevel: Int = 0,
    /** [temperLevel] 에서 실패한 횟수. 성공하면 0 으로 돌아간다. */
    val temperFails: Int = 0,
    /**
     * 최근 강화 결과. 왼쪽이 오래된 것, 오른쪽이 방금 것이다.
     *
     * 세이브에 남기는 이유: 앱을 껐다 켰다고 자기 연패가 없던 일이 되면 안 된다.
     */
    val recentMarks: List<ForgeMark> = emptyList(),
) {
    init {
        require(gold >= 0) { "gold must be >= 0, was $gold" }
        require(shards >= 0) { "shards must be >= 0, was $shards" }
        require(bestLevel >= 0) { "bestLevel must be >= 0, was $bestLevel" }
        require(forgeStones >= 0) { "forgeStones must be >= 0, was $forgeStones" }
        require(temperFails >= 0) { "temperFails must be >= 0, was $temperFails" }
    }
}

/**
 * 이번 강화 시도에 함께 사용할 아이템.
 *
 * 축복서와 부적은 **함께 켤 수 있다** (v2.1).
 *
 * 배타로 두었던 이유는 "있으면 전부 켠다"가 유일한 최선이 되는 것을 막기 위해서였는데,
 * 실제로 해 보니 갈림길이 아니라 **함정**으로 읽혔다 — 왜 하나가 꺼지는지 화면이
 * 설명하지 못했다. 이제 값(골드)이 선택을 가른다. 둘 다 켜면 두 장이 나간다.
 *
 * - 축복서 — 이번 판 확률만 올린다
 * - 부적 — 실패해도 **하락만** 막는다. 파괴는 못 막는다(v2.3) — 그건 방지권의 몫이다.
 *   담금질은 오른다.
 */
data class UsedItems(
    val blessing: Boolean = false,
    val luckCharm: Boolean = false,
) {
    fun toggleBlessing(): UsedItems = copy(blessing = !blessing)

    fun toggleLuckCharm(): UsedItems = copy(luckCharm = !luckCharm)

    companion object {
        val NONE: UsedItems = UsedItems()
    }
}
