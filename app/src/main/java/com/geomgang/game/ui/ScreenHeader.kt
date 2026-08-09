package com.geomgang.game.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.ChangeHistory
import androidx.compose.material.icons.outlined.Diamond
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geomgang.game.ForgeUiState

data class Wallet(
    val gold: Long,
    val shards: Int,
    val stones: Int,
    val tickets: Int,
    val deep: Boolean,
)

fun ForgeUiState.wallet(): Wallet =
    Wallet(gold, shards, forgeStones, preventTickets, deepUnlocked)

/** 모든 보조 화면이 공유하는 현대적인 대장간 머리. */
@Composable
fun ScreenHeader(title: String, onBack: () -> Unit, wallet: Wallet? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(
            onClick = onBack,
            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp),
        ) {
            ForgeIcon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "뒤로",
                modifier = Modifier.size(19.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "뒤로",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            text = title,
            modifier = Modifier
                .padding(start = 10.dp)
                .weight(1f),
            fontSize = 21.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-0.3).sp,
        )
        SeasonStamp(compact = true)
    }
    ThinRule(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 3.dp),
    )
    if (wallet != null) {
        Spacer(Modifier.height(8.dp))
        WalletBar(wallet)
    }
    Spacer(Modifier.height(12.dp))
}

/** 시안처럼 한 줄로 정리한 재화판. 시즌1에서는 강화석만 빠진다. */
@Composable
fun WalletBar(wallet: Wallet, modifier: Modifier = Modifier) {
    ForgePanel(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WalletItem(Icons.Outlined.AttachMoney, "골드", compactGold(wallet.gold), Modifier.weight(1.25f))
            WalletItem(Icons.Outlined.Diamond, "조각", "${wallet.shards}", Modifier.weight(0.9f))
            if (wallet.deep) {
                WalletItem(Icons.Outlined.ChangeHistory, "강화석", "${wallet.stones}", Modifier.weight(0.95f))
            }
            WalletItem(Icons.Outlined.Shield, "방지권", "${wallet.tickets}", Modifier.weight(0.95f))
        }
    }
}

@Composable
private fun WalletItem(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        ForgeIcon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Column(Modifier.padding(start = 4.dp)) {
            Text(
                text = value,
                fontSize = 13.sp,
                lineHeight = 13.sp,
                maxLines = 1,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = label,
                fontSize = 8.sp,
                lineHeight = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

fun compactGold(value: Long): String = when {
    value >= 1_000_000_000_000L -> "%.1f조".format(value / 1_000_000_000_000.0)
    value >= 100_000_000L -> "%.1f억".format(value / 100_000_000.0)
    value >= 10_000L -> "%,d만".format(value / 10_000L)
    else -> "%,d".format(value)
}
