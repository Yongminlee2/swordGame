package com.geomgang.game.ui

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geomgang.core.Combat
import com.geomgang.core.Economy
import com.geomgang.core.FamilyStyle
import com.geomgang.core.Storage
import com.geomgang.core.SwordNames
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
    onBack: () -> Unit,
) {
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

        // 들고 있는 검
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
                        SwordThumb(held.family, held.level, size = 40.dp)
                        Spacer(Modifier.height(0.dp))
                        Column(Modifier.padding(start = 10.dp)) {
                            Text("장착 중", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                            Text(SwordNames.nameFor(held.level), fontWeight = FontWeight.Bold)
                            Text(
                                text = "+${held.level} · ${held.family.displayName} · " +
                                    "공격력 %,d".format(Combat.attackPower(held)),
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

        Spacer(Modifier.height(12.dp))

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
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SwordThumb(sword.family, sword.level, size = 38.dp)
                            Column(Modifier.padding(start = 10.dp)) {
                                Text(SwordNames.nameFor(sword.level), fontWeight = FontWeight.Bold)
                                Text(
                                    text = "+${sword.level} · ${sword.family.displayName}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                )
                                Text(
                                    text = "공격력 %,d · %s".format(
                                        Combat.attackPower(sword),
                                        FamilyStyle.of(sword.family).blurb,
                                    ),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onEquip(index) },
                                enabled = !state.busy && !state.awaitingDestroyChoice,
                                modifier = Modifier.weight(1f),
                            ) { Text("장착", fontSize = 13.sp) }
                            OutlinedButton(
                                onClick = { onSell(index) },
                                enabled = !state.busy,
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("%,d".format(Economy.sellPrice(sword.level)), fontSize = 12.sp)
                            }
                            TextButton(
                                onClick = { onScrap(index) },
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
    }
}
