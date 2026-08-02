package com.geomgang.game.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.graphics.Color
import com.geomgang.core.CodexOffer
import com.geomgang.core.Combat
import com.geomgang.core.Economy
import com.geomgang.core.FamilyStyle
import com.geomgang.core.Storage
import com.geomgang.core.Sword
import com.geomgang.core.SwordNames
import com.geomgang.core.UniqueSwords
import com.geomgang.game.ForgeUiState

/**
 * 보관함.
 *
 * 검을 여러 자루 들고 있을 수 있게 하는 화면이다. 하는 일은 **보관·장착·판매·분해**
 * 넷뿐이다.
 *
 * 조합은 [CraftScreen] 으로 옮겼다. 여기에 두면 조합소에 정작 조합이 없고,
 * 이 화면은 성격이 다른 일을 둘 하게 된다.
 */
@Composable
fun StorageScreen(
    state: ForgeUiState,
    onStore: () -> Unit,
    onEquip: (Int) -> Unit,
    onSell: (Int) -> Unit,
    onScrap: (Int) -> Unit,
    onOffer: (Int) -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
    ) {
        ScreenHeader(title = "보관함", onBack = onBack, wallet = state.wallet())

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = "${state.storage.size} / ${state.storageCapacity}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            // 조각은 시즌2 화폐다. 시즌1에 보여 주면 쓸 수 없는 숫자만 하나 는다.
            Text(
                text = if (state.deepUnlocked) {
                    "골드 %,d · 조각 %,d".format(state.gold, state.shards)
                } else {
                    "골드 %,d".format(state.gold)
                },
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            )
        }

        Spacer(Modifier.height(12.dp))
        HeldSwordCard(state, onStore)
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
                StorageRow(
                    state = state,
                    sword = sword,
                    onEquip = { onEquip(index) },
                    onSell = { onSell(index) },
                    onScrap = { onScrap(index) },
                    onOffer = { onOffer(index) },
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
private fun StorageRow(
    state: ForgeUiState,
    sword: Sword,
    onEquip: () -> Unit,
    onSell: () -> Unit,
    onScrap: () -> Unit,
    onOffer: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            // 검 그림과 장착이 **한 줄에 나란히** 있다. 예전에는 그림 아래로 설명이 세 줄,
            // 그 아래 버튼이 또 한 줄이라 한 자루가 화면의 3분의 1을 먹었다.
            Row(verticalAlignment = Alignment.CenterVertically) {
                SwordThumb(sword, size = 32.dp)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                ) {
                    Text(
                        text = SwordNames.nameFor(sword),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        color = if (sword.uniqueId != null) {
                            Color(0xFFFFD54A)
                        } else {
                            Color.Unspecified
                        },
                    )
                    Text(
                        text = swordLine(sword),
                        fontSize = 10.sp,
                        maxLines = 1,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
                Button(
                    onClick = onEquip,
                    enabled = !state.busy && !state.awaitingDestroyChoice,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier.height(34.dp),
                ) { Text("🗡 장착", fontSize = 12.sp, maxLines = 1) }
            }

            // 고유검 설명만 남긴다. 계열 설명은 어느 자루에나 같은 말이 붙어서
            // 스무 자루가 같은 문장을 스무 번 반복하는 줄이 됐다.
            sword.uniqueId?.let { id ->
                UniqueSwords.byId(id)?.let { recipe ->
                    Text(
                        text = recipe.blurb,
                        fontSize = 10.sp,
                        maxLines = 1,
                        color = Color(0xFFFFD54A).copy(alpha = 0.8f),
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                RowAction(
                    // 자릿수를 다 적으면 버튼 안에서 접힌다. 지갑 줄과 같은 축약을 쓴다.
                    label = "💰 ${compactGold(Economy.sellPrice(sword))}",
                    enabled = !state.busy,
                    modifier = Modifier.weight(1f),
                    onClick = onSell,
                )
                // 분해는 조각·강화석을 주는 시즌2 창구다. 시즌1에는 자리도 없다 -
                // 쓸 수 없는 재화를 주는 버튼은 함정이다.
                if (state.deepUnlocked) {
                    RowAction(
                        label = "🔨 ${Storage.scrapShards(sword)}",
                        // 전설검을 부수면 다시 벼릴 값의 5분의 1도 안 나온다([Storage.canScrap]).
                        enabled = !state.busy && Storage.canScrap(sword),
                        modifier = Modifier.weight(1f),
                        onClick = onScrap,
                    )
                }
                RowAction(
                    label = "📖 도감",
                    // 이미 찬 칸이면 잠긴다. 검만 사라지고 얻는 게 없으면 함정이다.
                    enabled = !state.busy && CodexOffer.canOffer(state.progress, sword),
                    modifier = Modifier.weight(1f),
                    onClick = onOffer,
                )
            }
        }
    }
}

/** 보관함 한 자루의 부수 조작. 높이를 낮춰 한 자루가 먹는 자리를 줄인다. */
@Composable
private fun RowAction(
    label: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
        modifier = modifier.height(30.dp),
    ) {
        Text(label, fontSize = 11.sp, maxLines = 1)
    }
}

private fun swordLine(sword: Sword): String {
    val stars = if (sword.stars > 0) " " + "★".repeat(sword.stars) else ""
    return "+${sword.level}$stars · ${sword.family.displayName} · " +
        "공격력 %,d".format(Combat.attackPower(sword))
}
