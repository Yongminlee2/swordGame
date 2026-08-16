package com.geomgang.game.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.annotation.DrawableRes
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geomgang.core.HelpTopic
import com.geomgang.core.HelpTopics
import com.geomgang.core.GameSeason
import com.geomgang.game.R

/**
 * 도움말.
 *
 * 규칙 문구는 [HelpTopics] 가 도메인 상수에서 만들어 온다. 화면은 그리기만 한다 —
 * 밸런스를 고쳤는데 도움말만 옛 숫자를 말하는 일이 없어야 한다.
 */
@Composable
fun HelpScreen(
    season: GameSeason,
    onBack: () -> Unit,
) {
    val deepUnlocked = season != GameSeason.EMBER
    val topics = HelpTopics.ALL.filterNot { topic ->
        when (season) {
            GameSeason.EMBER -> topic.title in DEEP_HELP_TOPICS
            GameSeason.ABYSS -> topic.title == "전설 방지권"
            GameSeason.LEGEND -> false
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        ScreenHeader(title = "도움말", onBack = onBack)

        LText(
            text = "강화 규칙과 해금 조건",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
        )
        Spacer(Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(topics) { topic ->
                TopicForgePanel(
                    if (!deepUnlocked) {
                        topic.copy(body = seasonOneBody(topic.title, topic.body))
                    } else {
                        topic
                    },
                )
            }
        }
    }
}

/** 용검을 얻기 전에는 아직 조작할 수도, 얻을 수도 없는 시즌2 규칙을 숨긴다. */
private val DEEP_HELP_TOPICS = setOf(
    "사냥",
    "검 스킬",
    "별 강화",
    "무한 회랑",
    "펫",
    "정수와 제단",
    "전설 방지권",
)

private fun seasonOneBody(title: String, body: String): String = when (title) {
    "강화 재료" -> body.substringBefore("\n용검 조합 뒤")
    "자리비움" -> body.substringBefore("\n용검 조합 뒤:")
    else -> body
}

@Composable
private fun TopicForgePanel(topic: HelpTopic) {
    ForgePanel(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PixelIcon(
                    resource = helpIcon(topic.title),
                    contentDescription = null,
                    modifier = Modifier.size(23.dp),
                )
                LText(
                    text = "  ${topic.title}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.height(4.dp))
            LText(
                text = topic.body,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
            )
        }
    }
}

@DrawableRes
private fun helpIcon(title: String): Int = when (title) {
    "강화" -> R.drawable.ui_pixel_training
    "강화 재료" -> R.drawable.ui_pixel_stone
    "계열" -> R.drawable.ui_pixel_book
    "조합" -> R.drawable.ui_pixel_craft
    "사냥" -> R.drawable.ui_pixel_hunt
    "검 스킬" -> R.drawable.ui_pixel_bolt
    "별 강화" -> R.drawable.ui_pixel_star
    "전설 방지권" -> R.drawable.ui_pixel_shield
    "무한 회랑" -> R.drawable.ui_pixel_hunt
    "자리비움" -> R.drawable.ui_pixel_gold
    "펫" -> R.drawable.ui_pixel_pets
    else -> R.drawable.ui_pixel_help
}
