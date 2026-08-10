package com.geomgang.game.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geomgang.game.ForgeUiState
import com.geomgang.game.R

data class Wallet(
    val gold: Long,
    val shards: Int,
    val stones: Int,
    val tickets: Int,
    val deep: Boolean,
)

fun ForgeUiState.wallet(): Wallet =
    Wallet(gold, shards, forgeStones, preventTickets, deepUnlocked)

/** 모든 보조 화면이 기준 시안의 작은 제목·시즌 구분·밑줄 구조를 공유한다. */
@Composable
fun ScreenHeader(title: String, onBack: () -> Unit, wallet: Wallet? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = onBack)
                .padding(end = 10.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PixelIcon(
                resource = R.drawable.ui_pixel_back,
                contentDescription = "뒤로",
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = "뒤로",
                modifier = Modifier.padding(start = 2.dp),
                color = MaterialTheme.colorScheme.primary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
            )
        }
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            fontSize = 21.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-0.5).sp,
        )
        Spacer(
            Modifier
                .width(1.dp)
                .height(24.dp)
                .background(MaterialTheme.colorScheme.outline),
        )
        SeasonStamp(Modifier.padding(start = 12.dp))
    }
    Box(Modifier.fillMaxWidth()) {
        ThinRule(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.9f))
        ThinRule(
            modifier = Modifier.width(74.dp),
            thickness = 2.dp,
            color = MaterialTheme.colorScheme.primary,
        )
    }
    if (wallet != null) {
        WalletBar(wallet)
    } else {
        Spacer(Modifier.height(10.dp))
    }
}

/** 기준 시안처럼 외곽 카드 없이 한 줄로 정리한 재화 표시. */
@Composable
fun WalletBar(wallet: Wallet, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(43.dp)
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(1.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WalletItem(
                R.drawable.ui_pixel_gold,
                "골드",
                compactGold(wallet.gold),
                Modifier.weight(1.3f),
            )
            WalletItem(
                R.drawable.ui_pixel_gem,
                "조각",
                "${wallet.shards}",
                Modifier.weight(0.95f),
            )
            if (wallet.deep) {
                WalletItem(
                    R.drawable.ui_pixel_stone,
                    "강화석",
                    "${wallet.stones}",
                    Modifier.weight(1.05f),
                )
            }
            WalletItem(
                R.drawable.ui_pixel_shield,
                "방지권",
                "${wallet.tickets}",
                Modifier.weight(1.05f),
            )
        }
        ThinRule(Modifier.fillMaxWidth())
        Spacer(Modifier.height(7.dp))
    }
}

@Composable
private fun WalletItem(
    @DrawableRes icon: Int,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        PixelIcon(
            resource = icon,
            contentDescription = label,
            modifier = Modifier.size(21.dp),
        )
        Text(
            text = value,
            modifier = Modifier.padding(start = 3.dp),
            fontSize = 13.sp,
            maxLines = 1,
            fontWeight = FontWeight.Black,
        )
        Text(
            text = label,
            modifier = Modifier.padding(start = 4.dp),
            fontSize = 9.sp,
            maxLines = 1,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

fun compactGold(value: Long): String = when {
    value >= 1_000_000_000_000L -> "%.1f조".format(value / 1_000_000_000_000.0)
    value >= 100_000_000L -> "%.1f억".format(value / 100_000_000.0)
    value >= 10_000L -> "%,d만".format(value / 10_000L)
    else -> "%,d".format(value)
}
