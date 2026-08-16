package com.geomgang.core

/** 도움말 한 꼭지. */
data class HelpTopic(val title: String, val body: String)

/**
 * 도움말.
 *
 * 규칙이 코드에만 있고 화면에는 없으면 플레이어는 스스로 알아내야 한다.
 * v1.4에서 강화 화폐가 바뀌고(재료) 계열이 조합 전용이 되면서 "왜 안 되지"가 늘었다.
 *
 * 숫자를 글로 다시 적지 않는다 — 전부 도메인 상수에서 읽어 온다.
 * 밸런스를 고쳤는데 도움말만 옛날 값을 말하는 일이 없어야 한다.
 */
object HelpTopics {

    val ALL: List<HelpTopic> = listOf(
        HelpTopic(
            "강화",
            "성공 시 +1. +${RateTable.SAFE_BAND_END}까지는 실패해도 유지된다. " +
                "이후에는 하락하며, +${RateTable.DROP_BAND_END + 1}부터 파괴될 수 있다.\n" +
                "파괴 직후 검 복구 또는 조각 회수를 선택한다. 제한 시간이 지나면 둘 다 놓친다.\n" +
                "조각은 상점의 +5·+10·+15 워프권에 쓴다.",
        ),
        HelpTopic(
            "강화 재료",
            "용검 조합 전에는 골드만 사용한다.\n" +
                "용검 조합 뒤 +${ForgeCost.STONE_BAND_START}부터 강화석도 필요하다. " +
                "사냥·보스·검 분해·조각 교환(${Recipes.STONE_SHARD_COST}개)·상점에서 얻는다.\n" +
                "보관함의 검은 조합 재료로만 사용한다.",
        ),
        HelpTopic(
            "계열",
            "상점은 기본 4계열만 판매한다. 시작 계열은 직검이다.\n" +
                "곡도: +${Progress.CURVED_UNLOCK_LEVEL} 달성 · " +
                "대검: 파괴 ${Progress.GREAT_UNLOCK_DESTROYS}회 · " +
                "세검: +${Progress.RAPIER_UNLOCK_LEVEL} 달성.\n" +
                "직검+20 + 곡도+20 → 마검+1\n" +
                "대검+20 + 세검+20 → 성검+1\n" +
                "마검+20 + 성검+20 → 용검+${LegendForge.CRAFT_LEVEL}",
        ),
        HelpTopic(
            "조합",
            "+${Refinery.MATERIAL_LEVEL} 검 두 자루로 새 계열 +1을 만든다. 조합표는 조합소에서 확인한다.\n" +
                "고유검 ${UniqueSwords.RECIPES.size}종은 힌트로 재료를 찾는다. 완성된 고유검은 강화할 수 없다.",
        ),
        HelpTopic(
            "사냥",
            "화면을 눌러 공격한다. 계열마다 속도와 타격 방식이 다르다.\n" +
                "잡몹 ${Zone.MONSTERS_BEFORE_BOSS}마리 처치 시 보스 등장. 제한 시간은 ${Zone.MEADOW.bossSeconds}초다.\n" +
                "보스 처치 시 다음 구역과 구역 정수를 얻는다.",
        ),
        HelpTopic(
            "검 스킬",
            "스킬은 용검 전용이며 +${Skills.DRAGON_MIN_LEVEL}부터 타격 시 발동한다.\n" +
                Skills.dragonProgressionText(),
        ),
        HelpTopic(
            "별 강화",
            "용검부터 별을 붙일 수 있다. 최대 ${StarForce.MAX_STARS}개.\n" +
                "별은 공격력만 높인다. 실패 시 검은 유지되고 별 하나만 잃는다.",
        ),
        HelpTopic(
            "무한 회랑",
            "화산의 군주 처치 시 열린다. 제한 시간 안에 층을 돌파하고 축복·보물·저주를 고른다.\n" +
                "${GauntletEngine.BOSS_EVERY}층마다 보상이 확정된다. 확정 전 실패 시 " +
                "${(GauntletEngine.LOSS_RATIO * 100).toInt()}%만 획득한다.",
        ),
        HelpTopic(
            "자리비움",
            "최대 ${IdleRewards.durationText(IdleRewards.MAX_SECONDS)}까지 보상이 쌓인다.\n" +
                "용검 전: 현재 검 판매가의 최대 ${(IdleRewards.FORGE_RATIO * 100).toInt()}%를 골드로 획득.\n" +
                "용검 조합 뒤: 최고 구역 기준 골드와 시간당 강화석 ${IdleRewards.STONES_PER_HOUR}개 획득.\n" +
                "직접 플레이가 더 빠르다.",
        ),
        HelpTopic(
            "펫",
            "보스가 낮은 확률로 펫 알을 떨어뜨린다. 중복 알은 펫 레벨을 올린다.\n" +
                "장착한 펫 한 마리만 효과를 준다.",
        ),
        HelpTopic(
            "정수와 제단",
            "보스 처치 시 구역 정수 획득. 깊은 구역 정수일수록 가치가 높다.\n" +
                "정수력 ${WardCharm.COST}로 수호 각인을 새긴다. 전설 파괴 시 한 단계만 하락하며 1회 소모된다.\n" +
                "일부 정수는 고유검 조합에도 필요하다.",
        ),
        HelpTopic(
            "전설 방지권",
            "용검 +${LegendForge.MATERIAL_LEVEL} 달성 시 전설의 시대가 열린다.\n" +
                "상점 가격: 골드 %,d 또는 조각 %,d.\n".format(
                    LegendProtection.GOLD_PRICE,
                    LegendProtection.SHARD_PRICE,
                ) +
                "전설 파괴 시 원래 단계로 복구하며 한 장을 소모한다.",
        ),
    )
}
