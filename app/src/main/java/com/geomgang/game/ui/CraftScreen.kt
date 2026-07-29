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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geomgang.core.Fusion
import com.geomgang.core.LegendForge
import com.geomgang.core.Sword
import com.geomgang.core.SwordNames
import com.geomgang.core.UniqueSwords
import com.geomgang.core.WeaponFamily
import com.geomgang.core.Zone
import com.geomgang.game.ForgeUiState

/**
 * 조합소 — 검을 녹여 검을 만드는 곳.
 *
 * 예전에는 여기가 조각으로 물건을 사는 곳이었고, 정작 조합은 보관함에 붙어 있었다.
 * 조합소에 들어가면 조합이 없는 셈이라 이름과 하는 일이 어긋나 있었다.
 * 조각 교환은 성격이 같은 [ShopScreen] 으로 보내고, 이 화면은 조합만 한다.
 *
 * 재료는 보관함에서 고른다. 목록을 여기서 직접 보여 주므로 화면을 오갈 필요가 없다.
 */
@Composable
fun CraftScreen(
    state: ForgeUiState,
    onFuse: (List<Int>) -> Unit,
    onCraftLegend: () -> Unit,
    onRecraftLegend: () -> Unit,
    onBack: () -> Unit,
) {
    var picked by remember { mutableStateOf(setOf<Int>()) }

    // 보관함이 바뀌면 골라 둔 자리가 어긋난다. 비워서 엉뚱한 검을 녹이는 것을 막는다.
    LaunchedEffect(state.storage.size) { picked = emptySet() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
    ) {
        ScreenHeader(title = "조합소", onBack = onBack, wallet = state.wallet())

        Text(
            text = "보관함의 검 여러 자루를 녹여 한 자루로 만든다",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
        )

        // 재료가 모자라 아래가 잘려 나가도 이 칸은 남는다 - 무엇을 모아야 하는지가
        // 바로 그때 가장 필요한 정보다.
        Spacer(Modifier.height(12.dp))
        LegendPanel(
            missing = state.legendMissing,
            canCraft = state.canCraftLegend,
            canRecraft = state.canRecraftLegend,
            unlocked = state.legendUnlocked,
            shards = state.shards,
            onCraft = onCraftLegend,
            onRecraft = onRecraftLegend,
        )

        if (state.storage.size < Fusion.MIN_MATERIALS) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = "재료가 모자라다. 보관함에 검이 " +
                    "${Fusion.MIN_MATERIALS}자루는 있어야 한다.\n" +
                    "사냥에서 몬스터가 검을 떨어뜨린다 — 보스는 반드시 준다.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
            )
            return@Column
        }

        Spacer(Modifier.height(12.dp))
        FusionPanel(
            state = state,
            picked = picked,
            onFuse = {
                onFuse(picked.toList())
                picked = emptySet()
            },
            onClear = { picked = emptySet() },
        )

        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(state.storage) { index, sword ->
                MaterialRow(
                    sword = sword,
                    selected = index in picked,
                    // 고유검은 녹일 수 없다. 실수 한 번으로 전설이 사라지면 안 된다.
                    selectable = sword.uniqueId == null,
                    onToggle = {
                        picked = when {
                            index in picked -> picked - index
                            picked.size >= Fusion.MAX_MATERIALS -> picked
                            else -> picked + index
                        }
                    },
                )
            }
        }
    }
}

/**
 * 전설검 벼리기.
 *
 * 재료가 모자라도 **무엇이 필요한지 늘 보여 준다.** 목표가 보여야 모으고 싶어진다.
 */
@Composable
private fun LegendPanel(
    missing: List<WeaponFamily>,
    canCraft: Boolean,
    canRecraft: Boolean,
    unlocked: Boolean,
    shards: Int,
    onCraft: () -> Unit,
    onRecraft: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text("전설검 벼리기", fontWeight = FontWeight.Bold, color = Color(0xFFFFD54A))
            Text(
                text = "계열은 +${LegendForge.MATERIAL_LEVEL}에서 끝난다. 그 위는 여기서만 간다.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
            )
            Spacer(Modifier.height(8.dp))
            LegendForge.MATERIALS.forEach { family ->
                val have = family !in missing
                Text(
                    text = "${if (have) "✓" else "✗"} ${family.displayName} " +
                        "+${LegendForge.MATERIAL_LEVEL}",
                    fontSize = 12.sp,
                    color = if (have) {
                        Color(0xFF7FD48A)
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    },
                )
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = onCraft, enabled = canCraft, modifier = Modifier.fillMaxWidth()) {
                Text("재료로 벼리기")
            }
            // 한 번 넘은 벽을 두 번 넘으라고 하면 아무도 두 번째 도전을 하지 않는다.
            if (unlocked) {
                Spacer(Modifier.height(6.dp))
                OutlinedButton(
                    onClick = onRecraft,
                    enabled = canRecraft,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "💎 ${LegendForge.RECRAFT_SHARDS}로 다시 벼리기  (보유 $shards)",
                        fontSize = 13.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun FusionPanel(
    state: ForgeUiState,
    picked: Set<Int>,
    onFuse: () -> Unit,
    onClear: () -> Unit,
) {
    val materials = picked.sorted().mapNotNull { state.storage.getOrNull(it) }
    val preview = if (materials.size in Fusion.MIN_MATERIALS..Fusion.MAX_MATERIALS &&
        materials.none { it.uniqueId != null }
    ) {
        Fusion.resultOf(materials, state.essences)
    } else {
        null
    }
    val cost = Fusion.costOf(materials)
    val affordable = preview != null && state.gold >= cost

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(
                text = "재료 ${Fusion.MIN_MATERIALS}~${Fusion.MAX_MATERIALS}자루를 고른다 " +
                    "(${picked.size}자루 선택)",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
            if (state.essences.isNotEmpty()) {
                Text(
                    text = "보유 정수: " + state.essences.entries.joinToString(" · ") {
                        "${Zone.fromId(it.key).displayName} ${it.value}"
                    },
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                )
            }

            if (preview != null) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SwordThumb(preview, size = 40.dp)
                    Column(Modifier.padding(start = 10.dp)) {
                        if (preview.uniqueId != null) {
                            Text(
                                text = "✦ ${SwordNames.nameFor(preview)} ✦",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFD54A),
                            )
                            Text(
                                text = UniqueSwords.byId(preview.uniqueId!!)?.blurb ?: "",
                                fontSize = 11.sp,
                                color = Color(0xFFFFD54A).copy(alpha = 0.8f),
                            )
                        } else {
                            Text(
                                text = "${SwordNames.nameFor(preview)} " +
                                    "(+${preview.level} ${preview.family.displayName})",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Text(
                            text = "비용 %,d골드 · 계열을 맞추면 한 단계 더".format(cost),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                        if (!affordable) {
                            Text(
                                "골드가 모자라다",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onFuse,
                    enabled = affordable && !state.busy,
                    modifier = Modifier.weight(1f),
                ) { Text("조합") }
                OutlinedButton(
                    onClick = onClear,
                    enabled = picked.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                ) { Text("선택 해제") }
            }
        }
    }
}

/** 재료 후보 한 줄. 조작 버튼 없이 고르기만 한다 — 여기서 팔거나 장착할 일은 없다. */
@Composable
private fun MaterialRow(
    sword: Sword,
    selected: Boolean,
    selectable: Boolean,
    onToggle: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (selectable) Modifier.clickable(onClick = onToggle) else Modifier),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = when {
                    !selectable -> "✦"
                    selected -> "●"
                    else -> "○"
                },
                fontSize = 20.sp,
                modifier = Modifier.padding(end = 10.dp),
                color = when {
                    !selectable -> Color(0xFFFFD54A)
                    selected -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                },
            )
            SwordThumb(sword, size = 38.dp, dimmed = !selectable)
            Column(Modifier.padding(start = 10.dp)) {
                Text(
                    text = SwordNames.nameFor(sword),
                    fontWeight = FontWeight.Bold,
                    color = if (sword.uniqueId != null) Color(0xFFFFD54A) else Color.Unspecified,
                )
                Text(
                    text = if (selectable) {
                        "+${sword.level} ${sword.family.displayName}"
                    } else {
                        "고유검은 녹일 수 없다"
                    },
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
        }
    }
}
