package com.geomgang.game.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geomgang.core.CodexEntry
import com.geomgang.core.Progress
import com.geomgang.core.ProgressState
import com.geomgang.core.UniqueSwords
import com.geomgang.core.WeaponCatalog
import com.geomgang.core.WeaponFamily

/**
 * 도감.
 *
 * **그림 한 장에 칸 하나다.** 계열 14 × 단계 21 = 294 칸에 전설 20 칸을 더해 314 칸이며,
 * 시트3 의 그림 수와 정확히 같다. 얻은 칸은 실제 검을, 못 얻은 칸은 어두운 실루엣을 보여 준다.
 *
 * 모드를 초기화해도 여기는 지워지지 않는다. 그게 이 게임의 재도전 동력이다.
 */
@Composable
fun CodexScreen(progress: ProgressState, onBack: () -> Unit) {
    val owned: Set<CodexEntry> = Progress.entriesOf(progress)

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
            columns = GridCells.Fixed(COLUMNS),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // 계열마다 구획을 나눈다. 노출 계열만이다 - 숨긴 계열의 칸은
            // 도감 어디에도 없다([WeaponFamily.CODEX_FAMILIES]).
            WeaponFamily.CODEX_FAMILIES.forEach { family ->
                val levels = WeaponCatalog.LEVELS_PER_FAMILY.toList()
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SectionHeader(
                        title = family.displayName,
                        owned = levels.count { CodexEntry(family, it) in owned },
                        total = levels.size,
                    )
                }
                items(levels) { level ->
                    val entry = CodexEntry(family, level)
                    CodexCell(entry = entry, discovered = entry in owned)
                }
            }

            // 용검은 위에서 이미 제 계열 구획(+0~+20)을 받았다. 여기는 +21 위 —
            // 계열과 무관하게 같은 그림을 쓰는 전설 전용 구획이다.
            val legendLevels = WeaponCatalog.LEGEND_LEVELS.toList()
            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionHeader(
                    title = "용검(전설)  +${WeaponCatalog.FAMILY_MAX_LEVEL + 1}~" +
                        "+${WeaponCatalog.LEGEND_MAX_LEVEL}",
                    owned = legendLevels.count { CodexEntry(null, it) in owned },
                    total = legendLevels.size,
                    color = Color(0xFFFFD54A),
                )
            }
            items(legendLevels) { level ->
                val entry = CodexEntry(null, level)
                CodexCell(entry = entry, discovered = entry in owned)
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
                            "마검·성검은 +20 두 자루의 조합으로, " +
                            "용검은 그 둘 +20을 합쳐 얻는다.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                    )
                }
            }
            items(
                items = com.geomgang.core.Refinery.RECIPES,
                span = { GridItemSpan(maxLineSpan) },
            ) { recipe ->
                Card(Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        LevelThumb(
                            family = recipe.result,
                            level = 6,
                            size = 36.dp,
                        )
                        Column(Modifier.padding(start = 10.dp)) {
                            Text(
                                text = recipe.result.displayName,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = recipe.materials.joinToString(" + ") {
                                    "${it.displayName}+${com.geomgang.core.Refinery.MATERIAL_LEVEL}"
                                },
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            )
                        }
                    }
                }
            }
            // 용검(전설)로 가는 길도 여기서 말한다. 표에는 없지만 길은 있다.
            item(span = { GridItemSpan(maxLineSpan) }) {
                Card(Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        LevelThumb(family = null, level = 21, size = 36.dp)
                        Column(Modifier.padding(start = 10.dp)) {
                            Text(
                                "용검(전설)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFD54A),
                            )
                            Text(
                                text = "마검 +20 + 성검 +20 — 조합소 전설 칸에서",
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

/** 계열 한 구획의 머리. 몇 칸을 채웠는지가 여기서 바로 읽혀야 한다. */
@Composable
private fun SectionHeader(
    title: String,
    owned: Int,
    total: Int,
    color: Color = MaterialTheme.colorScheme.secondary,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = color)
        Text(
            text = "$owned / $total",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(
                alpha = if (owned == total) 1f else 0.55f,
            ),
            fontWeight = if (owned == total) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

/**
 * 도감 한 칸.
 *
 * 계열은 구획 머리가 말해 주므로 칸에는 쓰지 않는다. 남는 것은 그림과 단계뿐이라
 * 칸이 작아져도 314칸이 읽힌다.
 */
@Composable
private fun CodexCell(entry: CodexEntry, discovered: Boolean) {
    // Card 가 아니라 Box 다. 그림자를 가진 칸이 324개면 그리는 값이 그만큼 붙는데,
    // 칸을 가르는 데 필요한 것은 배경색 하나뿐이다.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center,
            ) {
                // 못 얻은 칸도 형태는 보여 준다. 무엇을 노려야 하는지 알아야 모으고 싶어진다.
                LevelThumb(
                    family = entry.family,
                    level = entry.level,
                    dimmed = !discovered,
                    size = 40.dp,
                )
            }
            Text(
                text = "+${entry.level}",
                fontSize = 10.sp,
                maxLines = 1,
                fontWeight = if (discovered) FontWeight.Bold else FontWeight.Normal,
                color = if (discovered) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                },
            )
        }
    }
}

/**
 * 격자 열 수.
 *
 * 칸이 314개라 4열이면 너무 길고 6열이면 "+40" 이 잘린다. 21칸짜리 계열 구획이
 * 다섯 줄로 딱 떨어지는 것도 5열이다.
 */
private const val COLUMNS = 5

