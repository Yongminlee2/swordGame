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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geomgang.core.Difficulty

/** 모드 하나의 요약. 모드 선택 화면에 보여 줄 것만 담는다. */
data class ModeSummary(
    val difficulty: Difficulty,
    val bestLevel: Int,
    val gold: Long,
    val started: Boolean,
)

/**
 * 모드 선택.
 *
 * 각 모드는 골드·아이템·기록이 완전히 분리된 독립 세이브다.
 * 카드를 누르면 들어가고, 아래 막대를 5초 누르면 그 모드만 초기화된다.
 * 도감·업적·통계는 초기화의 영향을 받지 않는다.
 */
@Composable
fun ModeSelectScreen(
    summaries: List<ModeSummary>,
    onEnter: (Difficulty) -> Unit,
    onReset: (Difficulty) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text("검 강화", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text(
            text = "모드마다 진행이 따로 저장된다",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
        )

        Spacer(Modifier.height(20.dp))

        summaries.forEach { summary ->
            ModeCard(
                summary = summary,
                onEnter = { onEnter(summary.difficulty) },
                onReset = { onReset(summary.difficulty) },
            )
            Spacer(Modifier.height(12.dp))
        }

        Text(
            text = "초기화해도 도감과 업적은 남는다",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
        )
    }
}

@Composable
private fun ModeCard(summary: ModeSummary, onEnter: () -> Unit, onReset: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onEnter),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        summary.difficulty.label(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = summary.difficulty.blurb(),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    if (summary.started) {
                        Text("최고 +${summary.bestLevel}", fontWeight = FontWeight.Medium)
                        Text(
                            text = "%,d골드".format(summary.gold),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                    } else {
                        Text(
                            "새로 시작",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            if (summary.started) {
                Spacer(Modifier.height(12.dp))
                HoldToReset(label = "5초 길게 눌러 초기화", onComplete = onReset)
            }
        }
    }
}

private fun Difficulty.label(): String = when (this) {
    Difficulty.EASY -> "쉬움"
    Difficulty.NORMAL -> "일반"
    Difficulty.HARD -> "지옥"
    Difficulty.ENDLESS -> "무한"
}

private fun Difficulty.blurb(): String = when (this) {
    Difficulty.EASY -> "성공률이 높다. +20까지 볼 만하다"
    Difficulty.NORMAL -> "기준 난이도. 대개 십몇 단계에서 막힌다"
    Difficulty.HARD -> "성공률 3/4. 여기서 +20을 본 사람은 드물다"
    Difficulty.ENDLESS -> "상한이 없다. 대신 실패는 무조건 파괴다"
}
