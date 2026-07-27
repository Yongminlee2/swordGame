package com.geomgang.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geomgang.core.DailyQuests
import com.geomgang.core.QuestInstance
import com.geomgang.game.ForgeUiState

/**
 * 일일·주간 퀘스트.
 *
 * 매일 켤 이유를 주는 화면이다. 진행도는 통계 카운터의 차분이라
 * 다른 화면에서 뭘 하든 저절로 오른다.
 */
@Composable
fun QuestScreen(
    state: ForgeUiState,
    onClaim: (Int) -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
    ) {
        ScreenHeader(title = "퀘스트", onBack = onBack, wallet = state.wallet())

        Text(
            text = "자정에 새 일일 퀘스트가 온다",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
        )
        Spacer(Modifier.height(12.dp))

        Text(
            text = "일일",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(6.dp))
        state.quests.daily.forEachIndexed { index, quest ->
            QuestRow(
                quest = quest,
                progress = state.questProgress.getOrElse(index) { 0 },
                goldReward = DailyQuests.dailyGold(quest.kind),
                shardReward = DailyQuests.DAILY_SHARDS,
                onClaim = { onClaim(index) },
            )
            Spacer(Modifier.height(8.dp))
        }

        val weekly = state.quests.weekly
        if (weekly != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "주간",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
            )
            Spacer(Modifier.height(6.dp))
            QuestRow(
                quest = weekly,
                progress = state.weeklyProgress,
                goldReward = DailyQuests.dailyGold(weekly.kind) * 2,
                shardReward = DailyQuests.WEEKLY_SHARDS,
                onClaim = { onClaim(-1) },
            )
        }
    }
}

@Composable
private fun QuestRow(
    quest: QuestInstance,
    progress: Int,
    goldReward: Long,
    shardReward: Int,
    onClaim: () -> Unit,
) {
    val done = progress >= quest.target
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "${quest.kind.label} ${quest.target}회",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                    )
                    Text(
                        text = "보상 %,d골드 · 조각 %d".format(goldReward, shardReward),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
                when {
                    quest.claimed -> Text(
                        "수령 완료",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    )
                    done -> Button(onClick = onClaim) { Text("수령", fontSize = 13.sp) }
                    else -> Text(
                        "$progress / ${quest.target}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF2A2340)),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth((progress.toFloat() / quest.target).coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(
                            if (done) Color(0xFF7FD48A) else MaterialTheme.colorScheme.primary,
                        ),
                )
            }
        }
    }
}
