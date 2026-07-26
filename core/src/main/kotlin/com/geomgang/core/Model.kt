package com.geomgang.core

import kotlinx.serialization.Serializable

/** 소비 아이템. */
enum class Item(val id: String, val displayName: String) {
    /** 파괴를 1회 무효화한다. 파괴 판정 직후 제한 시간 안에 눌러야 한다. */
    PREVENT_TICKET("prevent", "방지권"),

    /** 다음 1회 성공률을 [RateTable.BLESSING_BONUS] 만큼 올린다. */
    BLESSING_SCROLL("blessing", "축복서"),

    /** 다음 1회 실패해도 하락·파괴가 일어나지 않는다. */
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
    ;

    companion object {
        /** 업적 없이 처음부터 쓸 수 있는 계열. */
        val STARTERS: List<WeaponFamily> = listOf(STRAIGHT, CURVED, GREAT, RAPIER)

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
) {
    init {
        require(gold >= 0) { "gold must be >= 0, was $gold" }
        require(shards >= 0) { "shards must be >= 0, was $shards" }
        require(bestLevel >= 0) { "bestLevel must be >= 0, was $bestLevel" }
    }
}

/** 이번 강화 시도에 함께 사용할 아이템. */
data class UsedItems(
    val blessing: Boolean = false,
    val luckCharm: Boolean = false,
) {
    companion object {
        val NONE: UsedItems = UsedItems()
    }
}
