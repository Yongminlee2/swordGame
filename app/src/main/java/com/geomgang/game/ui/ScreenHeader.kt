package com.geomgang.game.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Paid
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
    val deep: Boolean,
)

fun ForgeUiState.wallet(): Wallet =
    Wallet(gold, shards, forgeStones, preventTickets, deepUnlocked)

/** 강화 화면 위에 올라오는 화면들의 공통 머리. */
@Composable
fun ScreenHeader(title: String, onBack: () -> Unit, wallet: Wallet? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.82f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "뒤로",
                    tint = MaterialTheme.colorScheme.secondary,
                )
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(start = 14.dp),
        )
    }
    if (wallet != null) {
        Spacer(Modifier.height(8.dp))
        WalletBar(wallet)
    }
    Spacer(Modifier.height(14.dp))
}

/** 이름과 값을 같이 보여 주는 재화 보드. 아이콘 모양은 기기마다 달라지지 않는다. */
@Composable
fun WalletBar(wallet: Wallet, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WalletItem(
                icon = Icons.Rounded.Paid,
                label = "골드",
                value = compactGold(wallet.gold),
                tint = ForgeGold,
                modifier = Modifier.weight(1f),
            )
            WalletItem(
                icon = Icons.Rounded.Diamond,
                label = "조각",
                value = "${wallet.shards}",
                tint = Color(0xFF78D7EA),
                modifier = Modifier.weight(1f),
            )
            if (wallet.deep) {
                WalletItem(
                    icon = Icons.Rounded.LocalFireDepartment,
                    label = "강화석",
                    value = "${wallet.stones}",
                    tint = ForgeWarning,
                    modifier = Modifier.weight(1f),
                )
            }
            WalletItem(
                icon = Icons.Rounded.Shield,
                label = "방지권",
                value = "${wallet.tickets}",
                tint = ForgeSteel,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun WalletItem(
    icon: ImageVector,
    label: String,
    value: String,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = tint,
            )
            Text(
                text = value,
                modifier = Modifier.padding(start = 4.dp),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
        Text(
            text = label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.52f),
        )
    }
}

/** 큰 수를 짧게. 정확한 값보다 현재 지불 가능 여부를 빠르게 읽는 것이 우선이다. */
fun compactGold(value: Long): String = when {
    value >= 1_000_000_000_000L -> "%.1f조".format(value / 1_000_000_000_000.0)
    value >= 100_000_000L -> "%.1f억".format(value / 100_000_000.0)
    value >= 10_000L -> "%,d만".format(value / 10_000L)
    else -> "%,d".format(value)
}
