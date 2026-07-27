package com.geomgang.game.ui

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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geomgang.core.Economy
import com.geomgang.core.Item
import com.geomgang.core.WeaponFamily
import com.geomgang.game.ForgeUiState

/**
 * 상점.
 *
 * 가격은 전부 [Economy] 에서 읽는다. 화면에 숫자를 다시 쓰지 않는다 —
 * 밸런스를 고칠 때 고칠 곳이 두 군데가 되면 반드시 어긋난다.
 */
@Composable
fun ShopScreen(
    state: ForgeUiState,
    onBuySword: (WeaponFamily) -> Unit,
    onSellSword: () -> Unit,
    onBuyItem: (Item) -> Unit,
    onBack: () -> Unit,
) {
    var family by remember { mutableStateOf(state.unlockedFamilies.first()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        ScreenHeader(title = "상점", onBack = onBack)

        Text(
            text = "보유 골드  %,d".format(state.gold),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )

        Spacer(Modifier.height(16.dp))

        // --- 검 ---
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("검", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))

                if (state.sword == null) {
                    Text(
                        "기본 검을 살 계열을 고른다",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                    Spacer(Modifier.height(6.dp))
                    // 미리보기도 게임 전체와 같은 그림이다 - 사는 검이 곧 보이는 검
                    SwordThumb(
                        sword = com.geomgang.core.Sword(family, 0),
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(vertical = 4.dp),
                        size = 96.dp,
                    )
                    Spacer(Modifier.height(6.dp))
                    FamilyPicker(
                        families = state.unlockedFamilies,
                        selected = family,
                        onSelect = { family = it },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = { onBuySword(family) },
                        enabled = state.canBuySword,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("${family.displayName} 구입  ·  %,d".format(Economy.BASE_SWORD_PRICE))
                    }
                    if (!state.canBuySword) {
                        Reason("골드가 모자란다")
                    }
                } else {
                    Text("+${state.sword.level} ${state.sword.family.displayName}")
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onSellSword,
                        enabled = !state.busy,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("판매  ·  %,d".format(state.sellPrice))
                    }
                    Reason("검을 들고 있는 동안에는 새 검을 살 수 없다")
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // --- 아이템 ---
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("아이템", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Item.entries.forEachIndexed { index, item ->
                    if (index > 0) HorizontalDivider(Modifier.padding(vertical = 4.dp))
                    ItemRow(
                        item = item,
                        owned = state.ownedCountOf(item),
                        affordable = state.gold >= Economy.priceOf(item) && !state.busy,
                        onBuy = { onBuyItem(item) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ItemRow(item: Item, owned: Int, affordable: Boolean, onBuy: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(item.displayName, fontWeight = FontWeight.Medium)
            Text(
                text = item.hint(),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
            Text(
                text = "보유 ${owned}개",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            )
        }
        Button(onClick = onBuy, enabled = affordable) {
            Text("%,d".format(Economy.priceOf(item)))
        }
    }
}

@Composable
private fun Reason(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        modifier = Modifier.padding(top = 6.dp),
    )
}

private fun Item.hint(): String = when (this) {
    Item.PREVENT_TICKET -> "파괴 직후 눌러 검을 되살린다"
    Item.BLESSING_SCROLL -> "다음 1회 성공률 +10%p"
    Item.LUCK_CHARM -> "다음 1회 실패해도 하락·파괴 없음"
}

private fun ForgeUiState.ownedCountOf(item: Item): Int = when (item) {
    Item.PREVENT_TICKET -> preventTickets
    Item.BLESSING_SCROLL -> blessingScrolls
    Item.LUCK_CHARM -> luckCharms
}
