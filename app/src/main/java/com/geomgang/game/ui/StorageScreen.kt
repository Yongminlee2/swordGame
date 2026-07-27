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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import com.geomgang.core.Combat
import com.geomgang.core.Economy
import com.geomgang.core.FamilyStyle
import com.geomgang.core.Fusion
import com.geomgang.core.Storage
import com.geomgang.core.Sword
import com.geomgang.core.SwordNames
import com.geomgang.core.UniqueSwords
import com.geomgang.core.Zone
import com.geomgang.game.ForgeUiState

/**
 * 보관함.
 *
 * 검을 여러 자루 들고 있을 수 있게 하는 화면이다.
 * 사냥에서 떨어진 검이 여기 쌓이고, 조합과 재료 강화가 여기서 검을 꺼내 쓴다.
 */
@Composable
fun StorageScreen(
    state: ForgeUiState,
    onStore: () -> Unit,
    onEquip: (Int) -> Unit,
    onSell: (Int) -> Unit,
    onScrap: (Int) -> Unit,
    onFuse: (List<Int>) -> Unit,
    onBack: () -> Unit,
) {
    // 조합할 재료를 고르는 중인지. 선택 모드에서는 개별 조작 버튼을 감춘다.
    var picking by remember { mutableStateOf(false) }
    var picked by remember { mutableStateOf(setOf<Int>()) }

    // 보관함 내용이 바뀌면 골라 둔 자리가 어긋난다. 비워서 잘못 녹이는 것을 막는다.
    LaunchedEffect(state.storage.size) { picked = emptySet() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
    ) {
        ScreenHeader(title = "보관함", onBack = onBack)

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = "${state.storage.size} / ${state.storageCapacity}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "골드 %,d · 조각 %,d".format(state.gold, state.shards),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            )
        }

        Spacer(Modifier.height(12.dp))
        HeldSwordCard(state, onStore)
        Spacer(Modifier.height(12.dp))

        if (state.storage.size >= Fusion.MIN_MATERIALS) {
            FusionPanel(
                state = state,
                picking = picking,
                picked = picked,
                onStartPicking = { picking = true },
                onCancel = {
                    picking = false
                    picked = emptySet()
                },
                onFuse = {
                    onFuse(picked.toList())
                    picking = false
                    picked = emptySet()
                },
            )
            Spacer(Modifier.height(12.dp))
        }

        if (state.storage.isEmpty()) {
            Text(
                text = "보관함이 비어 있다.\n사냥에서 몬스터가 검을 떨어뜨린다 — 보스는 반드시 준다.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
            )
            return@Column
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(state.storage) { index, sword ->
                StorageRow(
                    state = state,
                    sword = sword,
                    picking = picking,
                    selected = index in picked,
                    onToggle = {
                        picked = if (index in picked) picked - index else picked + index
                    },
                    onEquip = { onEquip(index) },
                    onSell = { onSell(index) },
                    onScrap = { onScrap(index) },
                )
            }
        }
    }
}

@Composable
private fun HeldSwordCard(state: ForgeUiState, onStore: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val held = state.sword
            if (held == null) {
                Text(
                    "손에 든 검이 없다",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SwordThumb(held, size = 40.dp)
                    Column(Modifier.padding(start = 10.dp)) {
                        Text(
                            "장착 중",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = SwordNames.nameFor(held),
                            fontWeight = FontWeight.Bold,
                            color = if (held.uniqueId != null) {
                                Color(0xFFFFD54A)
                            } else {
                                Color.Unspecified
                            },
                        )
                        Text(
                            text = swordLine(held),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                    }
                }
                OutlinedButton(
                    onClick = onStore,
                    enabled = !state.busy && state.storage.size < state.storageCapacity,
                ) { Text("넣기") }
            }
        }
    }
}

@Composable
private fun FusionPanel(
    state: ForgeUiState,
    picking: Boolean,
    picked: Set<Int>,
    onStartPicking: () -> Unit,
    onCancel: () -> Unit,
    onFuse: () -> Unit,
) {
    if (!picking) {
        OutlinedButton(
            onClick = onStartPicking,
            enabled = !state.busy,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("조합하기 — 여러 자루를 녹여 한 자루로") }
        return
    }

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
                text = "아래에서 재료를 ${Fusion.MIN_MATERIALS}~${Fusion.MAX_MATERIALS}자루 고른다 " +
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
                Spacer(Modifier.height(6.dp))
                if (preview.uniqueId != null) {
                    Text(
                        text = "결과 → ✦ ${SwordNames.nameFor(preview)} ✦",
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
                        text = "결과 → ${SwordNames.nameFor(preview)} " +
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
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onFuse,
                    enabled = affordable && !state.busy,
                    modifier = Modifier.weight(1f),
                ) { Text("조합") }
                OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                    Text("취소")
                }
            }
        }
    }
}

@Composable
private fun StorageRow(
    state: ForgeUiState,
    sword: Sword,
    picking: Boolean,
    selected: Boolean,
    onToggle: () -> Unit,
    onEquip: () -> Unit,
    onSell: () -> Unit,
    onScrap: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (picking) Modifier.clickable(onClick = onToggle) else Modifier),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (picking) {
                    Text(
                        text = if (selected) "●" else "○",
                        fontSize = 20.sp,
                        modifier = Modifier.padding(end = 10.dp),
                        color = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        },
                    )
                }
                SwordThumb(sword, size = 38.dp)
                Column(Modifier.padding(start = 10.dp)) {
                    Text(
                        text = SwordNames.nameFor(sword),
                        fontWeight = FontWeight.Bold,
                        color = if (sword.uniqueId != null) {
                            Color(0xFFFFD54A)
                        } else {
                            Color.Unspecified
                        },
                    )
                    Text(
                        text = swordLine(sword),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                    Text(
                        text = sword.uniqueId?.let { UniqueSwords.byId(it)?.blurb }
                            ?: FamilyStyle.of(sword.family).blurb,
                        fontSize = 11.sp,
                        color = if (sword.uniqueId != null) {
                            Color(0xFFFFD54A).copy(alpha = 0.8f)
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        },
                    )
                }
            }

            // 선택 모드에서는 개별 조작 버튼을 감춘다. 잘못 눌러 재료가 사라지면 안 된다.
            if (!picking) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onEquip,
                        enabled = !state.busy && !state.awaitingDestroyChoice,
                        modifier = Modifier.weight(1f),
                    ) { Text("장착", fontSize = 13.sp) }
                    OutlinedButton(
                        onClick = onSell,
                        enabled = !state.busy,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("%,d".format(Economy.sellPrice(sword.level)), fontSize = 12.sp)
                    }
                    TextButton(
                        onClick = onScrap,
                        enabled = !state.busy,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("조각 ${Storage.scrapShards(sword)}", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

private fun swordLine(sword: Sword): String {
    val stars = if (sword.stars > 0) " " + "★".repeat(sword.stars) else ""
    return "+${sword.level}$stars · ${sword.family.displayName} · " +
        "공격력 %,d".format(Combat.attackPower(sword))
}
