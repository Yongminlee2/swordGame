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
import androidx.compose.foundation.lazy.grid.GridItemSpan
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
import com.geomgang.core.SwordNames
import com.geomgang.core.UniqueSwords
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

            // --- 조합표 ---
            // 숨기지 않는다. 계열을 모으는 길이 보여야 조합소에 갈 이유가 생긴다.
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "조합표",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    Text(
                        text = "기본 4계열(직검·곡도·대검·세검)만 상점에 나온다. " +
                            "나머지는 조합으로 얻는다.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                    )
                }
            }
            items(
                items = com.geomgang.core.FusionTable.ALL,
                span = { GridItemSpan(maxLineSpan) },
            ) { entry ->
                Card(Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TierThumb(
                            family = entry.result,
                            tier = com.geomgang.core.WeaponTier.SILVER,
                            size = 36.dp,
                        )
                        Column(Modifier.padding(start = 10.dp)) {
                            Text(
                                text = entry.result.displayName,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = entry.hint,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            )
                        }
                    }
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Card(Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TierThumb(
                            family = WeaponFamily.FUSED,
                            tier = com.geomgang.core.WeaponTier.SILVER,
                            size = 36.dp,
                        )
                        Column(Modifier.padding(start = 10.dp)) {
                            Text("합검", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = "서로 다른 4계열",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            )
                        }
                    }
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Card(Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TierThumb(
                            family = WeaponFamily.VOID,
                            tier = com.geomgang.core.WeaponTier.SILVER,
                            size = 36.dp,
                        )
                        Column(Modifier.padding(start = 10.dp)) {
                            Text("허검", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = "무한 회랑 10층 돌파",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            )
                        }
                    }
                }
            }

            // --- 고유검 ---
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "고유검  ${progress.uniqueFound.size} / ${UniqueSwords.RECIPES.size}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD54A),
                    )
                    Text(
                        text = "특별한 조합이 특별한 검을 만든다. 힌트를 읽고 재료를 찾아라.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                    )
                }
            }
            items(
                items = UniqueSwords.RECIPES,
                span = { GridItemSpan(maxLineSpan) },
            ) { recipe ->
                UniqueRow(recipe = recipe, found = recipe.id in progress.uniqueFound)
            }
        }
    }
}

@Composable
private fun UniqueRow(recipe: com.geomgang.core.UniqueRecipe, found: Boolean) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 미발견도 실루엣은 보여 준다 - 목표가 보여야 모으고 싶어진다
            UniqueThumb(uniqueId = recipe.id, size = 44.dp, dimmed = !found)
            Column(Modifier.padding(start = 10.dp)) {
                Text(
                    text = if (found) "✦ ${recipe.name}" else "???",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (found) {
                        Color(0xFFFFD54A)
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    },
                )
                Text(
                    // 발견하면 패시브가, 못 했으면 힌트가 보인다. 힌트가 곧 콘텐츠다.
                    text = if (found) recipe.blurb else recipe.hint,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
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
                contentAlignment = Alignment.Center,
            ) {
                // 못 얻은 칸도 형태는 보여 준다. 무엇을 노려야 하는지 알아야 모으고 싶어진다.
                TierThumb(
                    family = entry.family,
                    tier = entry.tier,
                    dimmed = !discovered,
                    size = 52.dp,
                )
            }

            Text(
                // 도감도 단계 이름을 쓴다. 이름 체계를 두 벌 두면 헷갈린다.
                text = if (discovered) SwordNames.nameFor(entry.tier.minLevel) else "???",
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

