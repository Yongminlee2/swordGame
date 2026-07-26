package com.geomgang.game.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geomgang.core.Difficulty
import com.geomgang.core.ForgeResult
import com.geomgang.game.ForgeUiState
import kotlinx.coroutines.delay

/** 결과 배너를 띄워 두는 시간. M3 에서 진짜 연출로 대체한다. */
private const val RESULT_BANNER_MILLIS = 450L

@Composable
fun ForgeScreen(
    state: ForgeUiState,
    onForge: () -> Unit,
    onPrevent: () -> Unit,
    onSalvage: () -> Unit,
    onSell: () -> Unit,
    onBuy: () -> Unit,
    onAnimationEnd: () -> Unit,
) {
    // 파괴는 사용자의 응답을 기다려야 하므로 잠금을 자동으로 풀지 않는다.
    LaunchedEffect(state.lastResult, state.awaitingDestroyChoice) {
        if (state.lastResult != null && !state.awaitingDestroyChoice) {
            delay(RESULT_BANNER_MILLIS)
            onAnimationEnd()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "${state.difficulty.displayLabel()} · 최고 +${state.bestLevel}",
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                SwordView(state.sword, Modifier.size(150.dp, 210.dp))
                Spacer(Modifier.height(8.dp))
                Text(
                    text = state.sword?.let { "+${it.level} ${it.family.displayName}" }
                        ?: "검이 없다",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(6.dp))
                ResultBanner(state.lastResult)
            }
        }

        InfoRow("골드", "%,d".format(state.gold))
        InfoRow("조각", "${state.shards}")
        InfoRow("방지권", "${state.preventTickets}장")

        if (state.sword != null) {
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            InfoRow("다음 성공률", "${state.successPercent}%")
            InfoRow("강화 비용", "%,d".format(state.upgradeCost))
            InfoRow("판매가", "%,d".format(state.sellPrice))
        }

        Spacer(Modifier.height(16.dp))

        if (state.awaitingDestroyChoice) {
            Text(
                text = if (state.canPrevent) "방지권으로 되살리거나 조각을 주울 수 있다" else "파편이라도 줍자",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = onPrevent,
                    enabled = state.canPrevent,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                ) { Text("방지권 사용") }
                OutlinedButton(
                    onClick = onSalvage,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                ) { Text("줍기") }
            }
        } else {
            Button(
                onClick = onForge,
                enabled = state.canForge,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
            ) {
                Text("강 화", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onSell,
                    enabled = !state.busy && state.sword != null,
                    modifier = Modifier.weight(1f),
                ) { Text("판매") }
                OutlinedButton(
                    onClick = onBuy,
                    enabled = state.canBuySword,
                    modifier = Modifier.weight(1f),
                ) { Text("새 검 구입") }
            }
        }
    }
}

@Composable
private fun ResultBanner(result: ForgeResult?) {
    val (text, color) = when (result) {
        is ForgeResult.Success -> "성공!  +${result.newLevel}" to Color(0xFF7FD48A)
        is ForgeResult.Stay -> "실패 — 단계 유지" to Color(0xFFD4C87F)
        is ForgeResult.Drop -> "하락…  +${result.newLevel}" to Color(0xFFD49A5A)
        is ForgeResult.Destroyed -> "파괴!!" to Color(0xFFE05A5A)
        null -> "" to Color.Transparent
    }
    Text(text = text, color = color, fontSize = 18.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
        Text(value, fontWeight = FontWeight.Medium)
    }
}

private fun Difficulty.displayLabel(): String = when (this) {
    Difficulty.EASY -> "쉬움"
    Difficulty.NORMAL -> "일반"
    Difficulty.HARD -> "지옥"
    Difficulty.ENDLESS -> "무한"
}
