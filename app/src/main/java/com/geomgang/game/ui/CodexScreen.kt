package com.geomgang.game.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geomgang.core.CodexEntry
import com.geomgang.core.CodexKey
import com.geomgang.core.Difficulty
import com.geomgang.core.ProgressState
import com.geomgang.core.WeaponCatalog
import com.geomgang.core.WeaponFamily

/**
 * 도감.
 *
 * 계열 8종 × 티어 11종 = 88칸. 얻은 칸은 실제 검을, 못 얻은 칸은 어두운 실루엣을 보여 준다.
 * 칸마다 어느 난이도에서 얻었는지 점으로 표시한다.
 *
 * 모드를 초기화해도 여기는 지워지지 않는다. 그게 이 게임의 재도전 동력이다.
 */
@Composable
fun CodexScreen(progress: ProgressState, onBack: () -> Unit) {
    val owned: Set<CodexEntry> = progress.codex
        .map { CodexEntry(it.family, it.tier) }
        .toSet()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
    ) {
        ScreenHeader(title = "도감", onBack = onBack)

        Text(
            text = "${owned.size} / ${WeaponCatalog.ENTRIES.size}",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { owned.size.toFloat() / WeaponCatalog.ENTRIES.size },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(14.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(WeaponCatalog.ENTRIES) { entry ->
                CodexCell(
                    entry = entry,
                    discovered = entry in owned,
                    difficulties = progress.codex
                        .filter { it.family == entry.family && it.tier == entry.tier }
                        .map(CodexKey::difficulty)
                        .toSet(),
                )
            }
        }
    }
}

@Composable
private fun CodexCell(
    entry: CodexEntry,
    discovered: Boolean,
    difficulties: Set<Difficulty>,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.8f),
            ) {
                // 못 얻은 칸도 형태는 보여 준다. 무엇을 노려야 하는지 알아야 모으고 싶어진다.
                Canvas(Modifier.fillMaxSize()) {
                    if (discovered) {
                        drawSword(entry.family, entry.tier.minLevel)
                    } else {
                        drawSilhouette(entry.family, entry.tier.minLevel)
                    }
                }
            }

            Text(
                text = if (discovered) entry.tier.displayName else "???",
                fontSize = 10.sp,
                maxLines = 1,
                color = if (discovered) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                },
            )
            Text(
                text = entry.family.displayName,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
            )

            Spacer(Modifier.height(3.dp))
            DifficultyDots(
                available = WeaponCatalog.difficultiesFor(entry.tier),
                earned = difficulties,
            )
        }
    }
}

/** 이 티어를 얻을 수 있는 난이도마다 점 하나. 얻은 난이도는 채워진다. */
@Composable
private fun DifficultyDots(available: List<Difficulty>, earned: Set<Difficulty>) {
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        available.forEach { d ->
            Box(
                Modifier
                    .height(6.dp)
                    .aspectRatio(1f),
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    drawCircle(
                        color = if (d in earned) dotColor(d) else Color(0xFF3A3350),
                    )
                }
            }
        }
    }
}

private fun dotColor(d: Difficulty): Color = when (d) {
    Difficulty.EASY -> Color(0xFF7FD48A)
    Difficulty.NORMAL -> Color(0xFF7FA5C4)
    Difficulty.HARD -> Color(0xFFE05A5A)
    Difficulty.ENDLESS -> Color(0xFFC79BFF)
}

/**
 * 아직 못 얻은 칸. 형태만 어둡게 남긴다.
 *
 * 색을 흉내 내 따로 그리지 않고, 실제 검을 그린 뒤 배경색을 덮는다.
 * 그래야 실루엣이 실제 검과 정확히 같은 모양이 되고, 아트를 고쳐도 따라온다.
 */
private fun DrawScope.drawSilhouette(family: WeaponFamily, level: Int) {
    drawSword(family, level)
    drawRect(color = Color(0xFF1A1426).copy(alpha = 0.82f))
}
