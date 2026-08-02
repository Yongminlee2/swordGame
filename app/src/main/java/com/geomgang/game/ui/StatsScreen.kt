package com.geomgang.game.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geomgang.core.Difficulty
import com.geomgang.core.ProgressState
import com.geomgang.core.RateTable
import kotlin.math.abs

/**
 * 통계.
 *
 * 이 화면의 존재 이유는 맨 위의 **표기 확률 대 실제 확률 비교표**다.
 * 강화 게임 하는 사람은 늘 "확률 조작 아니냐"고 의심하는데, 그걸 데이터로 직접 확인시켜 준다.
 *
 * 표기 확률은 지금 보고 있는 모드 기준이고, 실제 확률은 전 모드 합산이다.
 * 그래서 여러 모드를 오갔다면 둘이 벌어져 보일 수 있다 — 화면에도 그렇게 적어 둔다.
 */
@Composable
fun StatsScreen(
    difficulty: Difficulty,
    progress: ProgressState,
    /** 용검 이후의 깊은 국면인지. 시즌1에는 조각·사냥·강화석 항목이 통째로 없다. */
    deepUnlocked: Boolean,
    onBack: () -> Unit,
) {
    val stats = progress.stats

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        ScreenHeader(title = "통계", onBack = onBack)

        Text("표기 확률 대 실제 확률", fontWeight = FontWeight.Bold)
        Text(
            text = "표기는 ${difficulty.statsLabel()} 기준, 실제는 전 모드 합산이다",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
        )
        Spacer(Modifier.height(8.dp))

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                RateHeaderRow()
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                (1..RateTable.MAX_FINITE_LEVEL).forEach { level ->
                    RateRow(
                        level = level,
                        expected = RateTable.successRate(difficulty, level),
                        observed = stats.observedRate(level),
                        tries = stats.attemptsByLevel[level] ?: 0L,
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("누적", fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                StatRow("총 강화 시도", "%,d".format(stats.attempts))
                StatRow("성공", "%,d".format(stats.successes))
                StatRow("유지", "%,d".format(stats.stays))
                StatRow("하락", "%,d".format(stats.drops))
                StatRow("파괴", "%,d".format(stats.destroys))
                HorizontalDivider(Modifier.padding(vertical = 6.dp))
                StatRow("최다 연속 실패", "${stats.maxFailStreak}회")
                StatRow("최고 도달 단계", "+${stats.bestLevelEver}")
                if (stats.bestEndlessLevel > 0) {
                    StatRow("무한 모드 최고", "+${stats.bestEndlessLevel}")
                }
                HorizontalDivider(Modifier.padding(vertical = 6.dp))
                StatRow("번 골드", "%,d".format(stats.goldEarned))
                StatRow("쓴 골드", "%,d".format(stats.goldSpent))
                // 조각은 시즌2 화폐다. 시즌1 화면에 보이면 "이건 어디서 얻지"만 남긴다.
                if (deepUnlocked) {
                    StatRow("모은 조각", "%,d".format(stats.shardsEarned))
                }
                HorizontalDivider(Modifier.padding(vertical = 6.dp))
                StatRow("방지권 사용", "${stats.preventUsed}회")
                StatRow("방지권 놓침", "${stats.preventMissed}회")
                StatRow("줍기 성공", "${stats.salvageTaken}회")
                StatRow("줍기 놓침", "${stats.salvageMissed}회")
                StatRow("파산 구제", "${stats.bailouts}회")
            }
        }

        // 사냥·강화석·회랑은 전부 용검 뒤의 세계다. 시즌1 통계에 0으로 늘어놓으면
        // "내가 뭘 놓치고 있나"라는 잘못된 신호만 준다.
        if (deepUnlocked) {
            Spacer(Modifier.height(16.dp))
            Text("사냥과 수집", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    StatRow("잡몹 처치", "%,d".format(stats.monsterKills))
                    StatRow("보스 처치", "%,d".format(stats.bossKills))
                    StatRow("이벤트 조우", "%,d".format(stats.eventsSeen))
                    HorizontalDivider(Modifier.padding(vertical = 6.dp))
                    StatRow("조합", "%,d".format(stats.fusions))
                    StatRow("별 강화 시도", "%,d".format(stats.starAttempts))
                    StatRow("최고 별", "★".repeat(stats.maxStars).ifEmpty { "없음" })
                    HorizontalDivider(Modifier.padding(vertical = 6.dp))
                    StatRow("스킬 발동", "%,d".format(stats.skillsTriggered))
                    StatRow("모은 강화석", "%,d".format(stats.stonesEarned))
                    StatRow("회랑 최고 층", "${stats.gauntletBestEver}층")
                }
            }
        } else {
            Spacer(Modifier.height(12.dp))
            Text(
                text = "용검을 조합하면 사냥과 수집 통계가 열린다",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
            )
        }
    }
}

@Composable
private fun RateHeaderRow() {
    Row(Modifier.fillMaxWidth()) {
        Cell("단계", 0.18f, bold = true)
        Cell("표기", 0.22f, bold = true)
        Cell("실제", 0.24f, bold = true)
        Cell("시도", 0.20f, bold = true)
        Cell("차이", 0.16f, bold = true)
    }
}

@Composable
private fun RateRow(level: Int, expected: Double, observed: Double?, tries: Long) {
    val diff = observed?.let { it - expected }
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Cell("+$level", 0.18f)
        Cell("%.2f%%".format(expected * 100), 0.22f)
        Cell(observed?.let { "%.2f%%".format(it * 100) } ?: "—", 0.24f)
        Cell(if (tries > 0) "$tries" else "—", 0.20f)
        Cell(
            text = diff?.let { "%+.2f".format(it * 100) } ?: "",
            weight = 0.16f,
            color = when {
                diff == null -> Color.Unspecified
                // 시도가 적으면 차이가 커도 의미가 없다. 색으로 강조하지 않는다.
                tries < 20 -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                abs(diff) < 0.05 -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                diff > 0 -> Color(0xFF7FD48A)
                else -> Color(0xFFE0906A)
            },
        )
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.Cell(
    text: String,
    weight: Float,
    bold: Boolean = false,
    color: Color = Color.Unspecified,
) {
    Text(
        text = text,
        modifier = Modifier.weight(weight),
        fontSize = 12.sp,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        color = color,
    )
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
        )
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
    Spacer(Modifier.width(0.dp))
}

private fun Difficulty.statsLabel(): String = when (this) {
    Difficulty.EASY -> "쉬움"
    Difficulty.NORMAL -> "일반"
    Difficulty.HARD -> "지옥"
    Difficulty.ENDLESS -> "무한"
}
