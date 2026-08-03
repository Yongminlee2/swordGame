package com.geomgang.core

/** 도움말 한 꼭지. */
data class HelpTopic(val icon: String, val title: String, val body: String)

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
            "⚒", "강화",
            "성공하면 한 단계 오른다. +${RateTable.SAFE_BAND_END}까지는 실패해도 그대로고, " +
                "그 위는 떨어지며, +${RateTable.DROP_BAND_END + 1}부터는 부서질 수 있다.\n" +
                "부서지는 순간 제한 시간이 열린다 — 방지권을 쓰거나 파편(조각)을 주울 수 있다.\n" +
                "모은 조각으로 상점에서 워프권(+5·+10·+15 검)을 산다 — " +
                "파괴의 재가 재기의 밑천이다.",
        ),
        HelpTopic(
            "🪨", "강화 재료",
            "용검을 조합하기 전에는 강화가 골드만 먹는다.\n" +
                "용검을 조합한 뒤에는 +${ForgeCost.STONE_BAND_START}부터 강화석이 함께 든다. " +
                "강화석은 사냥·보스·검 분해·조각 교환(${Recipes.STONE_SHARD_COST}개)·" +
                "상점 구매로 모은다.\n" +
                "보관함의 검은 강화에 쓰지 않는다 — 조합 재료로만 쓴다.",
        ),
        HelpTopic(
            "🗡", "계열",
            "상점에 나오는 것은 기본 4계열뿐이고, 시작은 직검 하나다.\n" +
                "곡도는 +${Progress.CURVED_UNLOCK_LEVEL} 달성, " +
                "대검은 검 파괴 ${Progress.GREAT_UNLOCK_DESTROYS}회 겪기, " +
                "세검은 +${Progress.RAPIER_UNLOCK_LEVEL} 달성으로 열린다.\n" +
                "직검+20과 곡도+20을 조합하면 마검 +1, 대검+20과 세검+20은 성검 +1. " +
                "마검·성검을 다시 +${LegendForge.MATERIAL_LEVEL}까지 올려 합치면 " +
                "용검(전설, +${LegendForge.LEVEL})이다.",
        ),
        HelpTopic(
            "⚗", "조합",
            "+${Refinery.MATERIAL_LEVEL} 두 자루를 태워 새 계열의 +1 을 얻는다. " +
                "끝까지 올린 두 자루를 바치는 의식이다.\n" +
                "무엇을 바치면 무엇이 나오는지는 조합소 화면에 적혀 있다.\n" +
                "숨은 레시피도 있다 — 고유검 ${UniqueSwords.RECIPES.size}종은 힌트만 보고 찾아야 한다. " +
                "고유검은 완성된 검이라 강화할 수 없다.",
        ),
        HelpTopic(
            "⚔", "사냥",
            "화면을 눌러 공격한다. 계열마다 치는 속도와 방식이 다르다.\n" +
                "잡몹 ${Zone.MONSTERS_BEFORE_BOSS}마리를 잡으면 보스가 열리고, " +
                "보스는 ${Zone.MEADOW.bossSeconds}초 안에 잡아야 한다.\n" +
                "보스를 잡으면 다음 구역과 구역 정수가 열린다.",
        ),
        HelpTopic(
            "⚡", "스킬",
            "+${Skills.MIN_LEVEL}부터 계열 고유 스킬이 열린다. " +
                "탭할 때 낮은 확률로 터져 피해가 배로 들어간다.\n" +
                "치명타와 겹쳐 터지기도 한다 — ${Zone.MEADOW.bossSeconds}초 보스전에서는 그 한 번이 승패를 가른다.",
        ),
        HelpTopic(
            "★", "특수강화",
            "용검(+${StarForce.MIN_LEVEL})부터 별을 붙일 수 있다. " +
                "최대 ${StarForce.MAX_STARS}개.\n" +
                "계열 검은 +${RateTable.MAX_FINITE_LEVEL}에서 조합 재료가 되므로 " +
                "별을 붙여도 곧 사라진다 — 그래서 용검부터다.\n" +
                "실패해도 검은 부서지지 않는다 — 별 하나를 잃을 뿐이다.\n" +
                "강화 단계와 별개의 계층이라 파괴의 공포가 겹치지 않는다.",
        ),
        HelpTopic(
            "🌀", "무한 회랑",
            "화산의 군주를 잡으면 열린다. 층마다 제한 시간 안에 잡몹을 정리하고 " +
                "갈림길에서 축복·보물·저주 중 하나를 고른다.\n" +
                "${GauntletEngine.BOSS_EVERY}층마다 보스를 잡으면 그때까지 모은 보상이 확정된다. " +
                "확정 전에 런이 끝나면 " +
                "${(GauntletEngine.LOSS_RATIO * 100).toInt()}%만 들고 나온다.",
        ),
        HelpTopic(
            "🌙", "자리비움",
            "앱을 꺼 둔 사이에도 조금씩 쌓인다. " +
                "${IdleRewards.durationText(IdleRewards.MAX_SECONDS)}까지만 쌓인다.\n" +
                "용검 전에는 대장간이 벌어 둔 골드가 온다 — " +
                "꽉 채우면 지금 검 판매가의 " +
                "${(IdleRewards.FORGE_RATIO * 100).toInt()}%다.\n" +
                "용검 뒤에는 깬 구역 중 가장 깊은 곳이 기준이 되어 " +
                "1분마다 잡몹 ${IdleRewards.KILLS_PER_MINUTE}마리 값과 " +
                "시간당 강화석 ${IdleRewards.STONES_PER_HOUR}개가 온다.\n" +
                "손으로 하는 편이 훨씬 빠르다 — 자리비움은 덤이다.",
        ),
        HelpTopic(
            "🐾", "펫",
            "보스가 낮은 확률로 펫 알을 떨어뜨린다. 같은 알을 또 얻으면 레벨이 오르고, " +
                "장착한 한 마리가 사냥을 돕는다.",
        ),
        HelpTopic(
            "💠", "정수와 제단",
            "보스를 잡으면 그 구역의 정수가 하나 남는다. " +
                "정수는 깊은 구역일수록 무겁다 — 초원 1, 끝의 문 39.\n" +
                "조합소 제단에서 그 무게로 수호 각인을 새긴다(정수력 ${WardCharm.COST}). " +
                "각인을 지니면 전설검이 미끄러질 때 +${LegendForge.LEVEL} 복귀 대신 " +
                "한 단계만 잃는다. 쓰면 사라지고 한 장만 지닌다.\n" +
                "특정 구역 정수는 고유검 조합의 촉매이기도 하다 — " +
                "각인으로 태울지 아껴 둘지가 선택이다.",
        ),
    )
}
