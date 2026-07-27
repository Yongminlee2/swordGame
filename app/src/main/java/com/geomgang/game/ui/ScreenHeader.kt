package com.geomgang.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geomgang.game.ForgeUiState

/** 지금 가진 재화. */
data class Wallet(
    val gold: Long,
    val shards: Int,
    val stones: Int,
    val tickets: Int,
)

fun ForgeUiState.wallet(): Wallet = Wallet(gold, shards, forgeStones, preventTickets)

/**
 * 강화 화면 위에 올라오는 화면들의 공통 머리.
 *
 * [wallet] 을 주면 제목 아래에 재화 줄이 붙는다. 화면마다 필요한 것만 보여 주던 탓에
 * (상점은 골드만, 조합소는 조각만) "지금 강화석이 몇 개더라" 를 확인하려면
 * 강화 화면까지 돌아가야 했다. 재화와 무관한 화면은 넘기지 않으면 된다.
 */
@Composable
fun ScreenHeader(title: String, onBack: () -> Unit, wallet: Wallet? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp)) {
            Text("← 뒤로", color = MaterialTheme.colorScheme.secondary)
        }
        Spacer(Modifier.width(12.dp))
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
    if (wallet != null) {
        Spacer(Modifier.height(8.dp))
        WalletBar(wallet)
    }
    Spacer(Modifier.height(12.dp))
}

/** 재화 한 줄. 아이콘만으로는 무슨 값인지 알 수 없으므로 이름을 함께 쓴다. */
@Composable
fun WalletBar(wallet: Wallet, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 8.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WalletItem("💰", "골드", compactGold(wallet.gold))
        WalletItem("💎", "조각", "${wallet.shards}")
        WalletItem("🪨", "강화석", "${wallet.stones}")
        WalletItem("🛡", "방지권", "${wallet.tickets}")
    }
}

@Composable
private fun WalletItem(icon: String, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(icon, fontSize = 13.sp)
        Spacer(Modifier.width(3.dp))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(3.dp))
        Text(
            text = label,
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
        )
    }
}

/**
 * 큰 수를 짧게. 26억을 자릿수로 다 쓰면 한 줄을 통째로 먹는다.
 *
 * 조(兆) 이상은 소수 첫째 자리까지만 보여 준다 — 정확한 값이 필요한 순간은
 * 비용을 낼 수 있는지뿐이고, 그건 버튼 활성화가 알려 준다.
 */
fun compactGold(value: Long): String = when {
    value >= 1_000_000_000_000L -> "%.1f조".format(value / 1_000_000_000_000.0)
    value >= 100_000_000L -> "%.1f억".format(value / 100_000_000.0)
    value >= 10_000L -> "%,d만".format(value / 10_000L)
    else -> "%,d".format(value)
}
