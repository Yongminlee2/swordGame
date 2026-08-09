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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geomgang.core.HelpTopic
import com.geomgang.core.HelpTopics

/**
 * 도움말.
 *
 * 규칙 문구는 [HelpTopics] 가 도메인 상수에서 만들어 온다. 화면은 그리기만 한다 —
 * 밸런스를 고쳤는데 도움말만 옛 숫자를 말하는 일이 없어야 한다.
 */
@Composable
fun HelpScreen(onBack: () -> Unit) {
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
            items(HelpTopics.ALL) { topic -> TopicForgePanel(topic) }
        }
    }
}

@Composable
private fun TopicForgePanel(topic: HelpTopic) {
    ForgePanel(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ForgeIcon(
                    imageVector = helpIcon(topic.title),
                    contentDescription = null,
                    modifier = Modifier.size(17.dp),
                    tint = MaterialTheme.colorScheme.primary,
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

private fun helpIcon(title: String): ImageVector = when (title) {
    "강화" -> Icons.Outlined.Construction
    "강화 재료" -> Icons.Outlined.ChangeHistory
    "계열" -> Icons.Outlined.Category
    "조합" -> Icons.Outlined.Science
    "사냥" -> Icons.Outlined.Forest
    "스킬" -> Icons.Outlined.Bolt
    "특수강화" -> Icons.Outlined.Star
    "무한 회랑" -> Icons.Outlined.AllInclusive
    "자리비움" -> Icons.Outlined.Schedule
    "펫" -> Icons.Outlined.Pets
    else -> Icons.Outlined.AutoAwesome
}
