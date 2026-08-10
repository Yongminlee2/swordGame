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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.annotation.DrawableRes
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geomgang.core.HelpTopic
import com.geomgang.core.HelpTopics
import com.geomgang.game.R

/**
 * 도움말.
 *
 * 규칙 문구는 [HelpTopics] 가 도메인 상수에서 만들어 온다. 화면은 그리기만 한다 —
 * 밸런스를 고쳤는데 도움말만 옛 숫자를 말하는 일이 없어야 한다.
 */
@Composable
fun HelpScreen(
    deepUnlocked: Boolean,
    onBack: () -> Unit,
) {
    val topics = if (deepUnlocked) {
        HelpTopics.ALL
    } else {
        HelpTopics.ALL.filterNot { it.title in DEEP_HELP_TOPICS }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        ScreenHeader(title = "도움말", onBack = onBack)

        Text(
            text = "강화가 중심이고, 나머지는 전부 강화를 위한 것이다.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
        )
        Spacer(Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(topics) { topic ->
                TopicForgePanel(
                    if (!deepUnlocked && topic.title == "자리비움") {
                        topic.copy(body = seasonOneIdleBody(topic.body))
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
    "강화 재료",
    "사냥",
    "스킬",
    "특수강화",
    "무한 회랑",
    "펫",
    "정수와 제단",
)

private fun seasonOneIdleBody(body: String): String {
    val seasonOne = body.substringBefore("\n용검 뒤에는")
    return "$seasonOne\n손으로 하는 편이 훨씬 빠르다 — 자리비움은 덤이다."
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
                Text(
                    text = "  ${topic.title}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
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
    "스킬" -> R.drawable.ui_pixel_bolt
    "특수강화" -> R.drawable.ui_pixel_star
    "무한 회랑" -> R.drawable.ui_pixel_hunt
    "자리비움" -> R.drawable.ui_pixel_gold
    "펫" -> R.drawable.ui_pixel_pets
    else -> R.drawable.ui_pixel_help
}
