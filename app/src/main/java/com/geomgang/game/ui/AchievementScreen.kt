package com.geomgang.game.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geomgang.core.Achievement
import com.geomgang.core.ProgressState

/**
 * 업적과 칭호.
 *
 * 달성한 업적의 칭호만 고를 수 있다. 칭호는 강화 화면 위에 붙는 자랑용이고
 * 게임 수치에는 영향을 주지 않는다.
 */
@Composable
fun AchievementScreen(
    progress: ProgressState,
    onSelectTitle: (Achievement?) -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        ScreenHeader(title = "업적 · 칭호", onBack = onBack)

        Text(
            text = "${progress.achievements.size} / ${Achievement.entries.size}",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "업적을 달성하면 칭호가 열린다. 눌러서 단다",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
        )

        Spacer(Modifier.height(14.dp))

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(4.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectTitle(null) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("칭호 없음")
                    if (progress.selectedTitle == null) SelectedMark()
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(4.dp)) {
                Achievement.entries.forEachIndexed { index, achievement ->
                    if (index > 0) HorizontalDivider()
                    AchievementRow(
                        achievement = achievement,
                        earned = achievement in progress.achievements,
                        selected = achievement == progress.selectedTitle,
                        onSelect = { onSelectTitle(achievement) },
                    )
                }
            }
        }
    }
}

@Composable
private fun AchievementRow(
    achievement: Achievement,
    earned: Boolean,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    val alpha = if (earned) 1f else 0.38f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (earned) Modifier.clickable(onClick = onSelect) else Modifier)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = achievement.displayName,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
            )
            Text(
                text = "칭호 · ${achievement.title}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = alpha * 0.9f),
            )
        }
        if (selected) SelectedMark()
    }
}

@Composable
private fun SelectedMark() {
    Text(
        text = "착용 중",
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
    )
}
