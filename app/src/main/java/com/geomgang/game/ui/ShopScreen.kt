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
import com.geomgang.core.Recipe
import com.geomgang.core.RecipeReward
import com.geomgang.core.Recipes
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
    onBuySwordToStorage: (WeaponFamily) -> Unit,
    onBuyStone: () -> Unit,
    onSellSword: () -> Unit,
    onBuyItem: (Item) -> Unit,
    onCraft: (recipeId: String, count: Int) -> Unit,
    onBack: () -> Unit,
) {
    var family by remember { mutableStateOf(state.unlockedFamilies.first()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        // 재화는 머리의 지갑 줄이 전부 보여 준다 - 화면마다 따로 쓰지 않는다.
        ScreenHeader(title = "상점", onBack = onBack, wallet = state.wallet())

        // --- 검 ---
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("검", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))

                Text(
                    "살 계열을 고른다",
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
                // 아직 잠긴 기본 계열의 조건을 알려 준다. 나머지 10계열은 조합 전용이라
                // 상점에 나올 일이 없으므로 여기 쓰지 않는다.
                val locked = com.geomgang.core.WeaponFamily.BASICS
                    .filterNot { it in state.unlockedFamilies }
                if (locked.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    locked.forEach { f ->
                        com.geomgang.core.Progress
                            .basicFamilyHint(state.progress, f)
                            ?.let { hint ->
                                Text(
                                    text = "🔒 ${f.displayName} — $hint",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                        .copy(alpha = 0.5f),
                                )
                            }
                    }
                }

                Spacer(Modifier.height(10.dp))
                if (state.sword == null) {
                    Button(
                        onClick = { onBuySword(family) },
                        enabled = state.canBuySword,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("손에 들기  ·  %,d".format(Economy.BASE_SWORD_PRICE))
                    }
                    if (!state.canBuySword) {
                        Reason("골드가 모자란다")
                    }
                    Spacer(Modifier.height(8.dp))
                }

                // 재료 검을 모으는 길. 손에 든 검을 팔거나 넣었다 뺐다 하지 않아도 된다.
                OutlinedButton(
                    onClick = { onBuySwordToStorage(family) },
                    enabled = state.canBuyToStorage,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        "가방에 넣기  ·  %,d  (%d/%d)".format(
                            Economy.BASE_SWORD_PRICE,
                            state.storage.size,
                            state.storageCapacity,
                        ),
                    )
                }
                if (!state.canBuyToStorage) {
                    Reason(
                        if (state.storage.size >= state.storageCapacity) {
                            "가방이 가득 찼다"
                        } else {
                            "골드가 모자란다"
                        },
                    )
                }

                if (state.sword != null) {
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(10.dp))
                    Text("들고 있는 검  ·  +${state.sword.level} ${state.sword.family.displayName}")
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onSellSword,
                        enabled = !state.busy,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("판매  ·  %,d".format(state.sellPrice))
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // --- 재료 ---
        // 골드는 남고 강화석은 모자란 상태가 후반의 기본값이다. 둘을 잇는다.
        // 시즌1은 강화석을 아예 안 먹으므로([Unlocks.stonesUsed]) 칸째로 감춘다.
        if (state.deepUnlocked) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("재료", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("🪨 강화석", fontWeight = FontWeight.Medium)
                        Text(
                            text = "보유 ${state.forgeStones}개",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        )
                        Text(
                            text = "다음 개는 %,d".format(state.nextStonePrice),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        )
                    }
                    Button(onClick = onBuyStone, enabled = state.canBuyStone) {
                        Text("%,d".format(state.stonePrice))
                    }
                }
                Spacer(Modifier.height(6.dp))
                // 값이 왜 오르고 언제 풀리는지 말해 주지 않으면 그냥 짜증으로만 남는다.
                Text(
                    text = "살수록 값이 오른다. 한 단계 올리면 처음 값으로 돌아온다.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        }

        // --- 아이템 ---
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("아이템", fontWeight = FontWeight.Bold)
                // 값이 왜 오르고 언제 풀리는지 말해 주지 않으면 그냥 짜증으로만 남는다.
                Text(
                    text = if (state.itemsBought > 0) {
                        "이 구간에서 ${state.itemsBought}개 샀다. 살수록 값이 오르고, " +
                            "한 단계 올리면 처음 값으로 돌아온다."
                    } else {
                        "살수록 값이 오른다. 한 단계 올리면 처음 값으로 돌아온다."
                    },
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                )
                Spacer(Modifier.height(4.dp))
                Item.entries.forEachIndexed { index, item ->
                    if (index > 0) HorizontalDivider(Modifier.padding(vertical = 4.dp))
                    val price = state.itemPrices[item] ?: Economy.priceOf(item)
                    ItemRow(
                        item = item,
                        owned = state.ownedCountOf(item),
                        price = price,
                        affordable = state.gold >= price && !state.busy,
                        onBuy = { onBuyItem(item) },
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // --- 조각 교환 ---
        // 예전에는 "조합소"라는 별도 화면이었다. 그런데 하는 일이 상점과 똑같다 —
        // 화폐를 내고 물건을 받는다. 화폐가 골드가 아니라 조각일 뿐이라 여기로 옮겼다.
        // 덕분에 조합소는 이름 그대로 검을 조합하는 곳만 남았다.
        //
        // 용검 이전에는 통째로 감춘다. 그때는 조각이 아예 나오지 않으므로
        // ([com.geomgang.core.Unlocks]) 살 수 없는 목록만 늘어놓게 된다.
        if (!state.deepUnlocked) return@Column
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("조각 교환", fontWeight = FontWeight.Bold)
                Text(
                    text = "파괴된 검에서 주운 조각으로 바꾼다. " +
                        "골드가 바닥나도 여기서 다시 일어설 수 있다.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                )
                Spacer(Modifier.height(4.dp))
                Recipes.ALL.forEachIndexed { index, recipe ->
                    if (index > 0) HorizontalDivider(Modifier.padding(vertical = 4.dp))
                    RecipeRow(
                        recipe = recipe,
                        state = state,
                        onCraft = { count -> onCraft(recipe.id, count) },
                    )
                }
            }
        }
    }
}

/**
 * 조각 교환 한 줄.
 *
 * 검을 주는 교환도 계열을 묻지 않는다 — 계열끼리 성능이 같아 고를 이유가 없었고,
 * 지금은 [Recipes.familyFor] 가 도감이 덜 찬 계열을 먼저 준다.
 */
@Composable
private fun RecipeRow(
    recipe: Recipe,
    state: ForgeUiState,
    onCraft: (count: Int) -> Unit,
) {
    val grantsSword = recipe.reward is RecipeReward.GrantSword
    val blockedBySword = grantsSword && state.sword != null
    val enough = state.shards >= recipe.shardCost
    val canTap = enough && !blockedBySword && !state.busy
    // 검은 손이 하나뿐이라 한 자루씩이다. 나머지는 조각이 닿는 만큼 한 번에 바꾼다.
    val most = if (grantsSword) 1 else state.shards / recipe.shardCost

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(recipe.displayName, fontWeight = FontWeight.Medium)
            val reason = when {
                blockedBySword -> "검을 들고 있으면 바꿀 수 없다"
                !enough -> "조각 ${recipe.shardCost - state.shards}개 부족"
                grantsSword -> "도감이 덜 찬 계열로 나온다"
                else -> null
            }
            if (reason != null) {
                Text(
                    text = reason,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(onClick = { onCraft(1) }, enabled = canTap) {
                Text("💎 ${recipe.shardCost}", fontSize = 13.sp)
            }
            // 고단계 강화석은 한 판에 열댓 개가 든다. 하나씩 누르면 그건 노가다다.
            if (most > 1) {
                OutlinedButton(onClick = { onCraft(most) }, enabled = canTap) {
                    Text("최대 ×$most", fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun ItemRow(
    item: Item,
    owned: Int,
    price: Long,
    affordable: Boolean,
    onBuy: () -> Unit,
) {
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
            Text("%,d".format(price))
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
    Item.LUCK_CHARM -> "다음 1회 실패해도 하락 없음 (파괴는 방지권으로)"
}

private fun ForgeUiState.ownedCountOf(item: Item): Int = when (item) {
    Item.PREVENT_TICKET -> preventTickets
    Item.BLESSING_SCROLL -> blessingScrolls
    Item.LUCK_CHARM -> luckCharms
}
