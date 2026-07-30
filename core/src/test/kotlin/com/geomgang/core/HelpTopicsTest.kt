package com.geomgang.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 도움말은 도메인 상수에서 문구를 만든다.
 *
 * 밸런스를 고쳤는데 도움말만 옛 숫자를 말하는 일이 없어야 한다 —
 * 이 테스트가 "숫자를 손으로 적지 않았는지"를 지킨다.
 */
class HelpTopicsTest {

    @Test
    fun `모든 꼭지에 아이콘과 제목과 본문이 있다`() {
        assertTrue(HelpTopics.ALL.isNotEmpty())
        for (topic in HelpTopics.ALL) {
            assertTrue("아이콘 없음: ${topic.title}", topic.icon.isNotBlank())
            assertTrue("제목 없음", topic.title.isNotBlank())
            assertTrue("본문 없음: ${topic.title}", topic.body.length >= 20)
        }
    }

    @Test
    fun `제목이 겹치지 않는다`() {
        val titles = HelpTopics.ALL.map { it.title }
        assertEquals(titles.size, titles.toSet().size)
    }

    @Test
    fun `재료 설명이 실제 요구 단계를 말한다`() {
        val body = HelpTopics.ALL.first { it.title == "강화 재료" }.body
        assertTrue(body.contains("+${ForgeCost.STONE_BAND_START}"))
        assertTrue(body.contains("${Recipes.STONE_SHARD_COST}"))
    }

    @Test
    fun `계열 설명이 실제 해금 조건을 말한다`() {
        val body = HelpTopics.ALL.first { it.title == "계열" }.body
        assertTrue(body.contains("+${Progress.CURVED_UNLOCK_LEVEL}"))
        assertTrue(body.contains("${Progress.GREAT_UNLOCK_ZONES}"))
    }

    @Test
    fun `스킬 설명이 실제 해금 단계를 말한다`() {
        val body = HelpTopics.ALL.first { it.title == "스킬" }.body
        assertTrue(body.contains("+${Skills.MIN_LEVEL}"))
    }

    @Test
    fun `사냥 설명이 실제 보스 제한 시간을 말한다`() {
        val body = HelpTopics.ALL.first { it.title == "사냥" }.body
        assertTrue(body.contains("${Zone.MEADOW.bossSeconds}초"))
        assertTrue(body.contains("${Zone.MONSTERS_BEFORE_BOSS}"))
    }

    @Test
    fun `특수강화 설명이 실제 별 상한을 말한다`() {
        val body = HelpTopics.ALL.first { it.title == "특수강화" }.body
        assertTrue(body.contains("+${StarForce.MIN_LEVEL}"))
        assertTrue(body.contains("${StarForce.MAX_STARS}"))
    }

    @Test
    fun `조합 설명이 실제 고유검 수를 말한다`() {
        val body = HelpTopics.ALL.first { it.title == "조합" }.body
        assertTrue(body.contains("${UniqueSwords.RECIPES.size}"))
        assertTrue(body.contains("두 자루"))
    }

    @Test
    fun `회랑 설명이 실제 체크포인트 주기와 손실률을 말한다`() {
        val body = HelpTopics.ALL.first { it.title == "무한 회랑" }.body
        assertTrue(body.contains("${GauntletEngine.BOSS_EVERY}층"))
        assertTrue(body.contains("${(GauntletEngine.LOSS_RATIO * 100).toInt()}%"))
    }

    @Test
    fun `자리비움 설명이 실제 상한을 말한다`() {
        val body = HelpTopics.ALL.first { it.title == "자리비움" }.body
        assertTrue(body.contains(IdleRewards.durationText(IdleRewards.MAX_SECONDS)))
        assertTrue(body.contains("${IdleRewards.STONES_PER_HOUR}"))
    }

    @Test
    fun `옛 규칙을 말하지 않는다`() {
        // v1.3까지 쓰던 20초 보스, 4계열 시작 같은 표현이 남아 있으면 안 된다.
        val all = HelpTopics.ALL.joinToString("\n") { it.body }
        assertFalse("옛 보스 시간이 남아 있다", all.contains("20초"))
        assertFalse("옛 회랑 시간이 남아 있다", all.contains("25초"))
    }
}
