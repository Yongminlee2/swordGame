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
                "부서지는 순간 제한 시간이 열린다 — 방지권을 쓰거나 파편을 주울 수 있다.",
        ),
        HelpTopic(
            "🪨", "강화 재료",
            "+${ForgeCost.SWORD_BAND_START}부터는 골드만으로 강화할 수 없다. " +
                "보관함의 검을 재료로 태우고, +${ForgeCost.STONE_BAND_START}부터는 강화석도 든다.\n" +
                "강화석은 사냥·보스·검 분해·조합소 교환(조각 ${Recipes.STONE_SHARD_COST}개)으로 모은다.\n" +
                "필수 재료 위에 검을 더 태우면 성공률이 오른다.",
        ),
        HelpTopic(
            "🗡", "계열",
            "상점에 나오는 것은 기본 4계열뿐이고, 시작은 직검 하나다.\n" +
                "곡도는 +${Progress.CURVED_UNLOCK_LEVEL} 달성, " +
                "대검은 구역 ${Progress.GREAT_UNLOCK_ZONES}곳 클리어, " +
                "세검은 고유검 발견으로 열린다.\n" +
                "나머지 계열은 전부 조합으로만 얻는다.",
        ),
        HelpTopic(
            "⚗", "조합",
            "보관함의 검 ${Fusion.MIN_MATERIALS}~${Fusion.MAX_MATERIALS}자루를 녹여 한 자루로 만든다. " +
                "재료가 많을수록, 계열을 맞출수록 결과 단계가 높다.\n" +
                "무엇을 넣으면 무엇이 나오는지는 도감의 조합표에 적혀 있다.\n" +
                "숨은 레시피도 있다 — 고유검 ${UniqueSwords.RECIPES.size}종은 힌트만 보고 찾아야 한다.",
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
            "+${StarForce.MIN_LEVEL}부터 별을 붙일 수 있다. 최대 ${StarForce.MAX_STARS}개.\n" +
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
            "🐾", "펫과 퀘스트",
            "보스가 낮은 확률로 펫 알을 떨어뜨린다. 같은 알을 또 얻으면 레벨이 오르고, " +
                "장착한 한 마리가 사냥을 돕는다.\n" +
                "일일 퀘스트는 자정에 새로 온다. 주간 퀘스트를 깨면 펫 알을 준다.",
        ),
    )
}
