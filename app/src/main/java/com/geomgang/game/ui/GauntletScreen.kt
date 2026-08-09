package com.geomgang.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geomgang.core.GauntletChoice
import com.geomgang.core.Zone
import com.geomgang.game.ForgeUiState
import com.geomgang.game.GauntletUiState

/**
 * 무한 회랑.
 *
 * 층 웨이브 + 갈림길 3택 + 5층 체크포인트. 몬스터 그림은 무한 회랑 구역의
 * 스프라이트를 순환해 쓴다 - 회랑은 그 구역의 끝없는 복도라는 설정이다.
 */
@Composable
fun GauntletScreen(
    state: ForgeUiState,
    onTap: () -> Unit,
    onChoose: (Int) -> Unit,
    onLeave: () -> Unit,
) {
    val g = state.gauntlet ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(onClick = onLeave) {
                ForgeIcon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text("나가기")
            }
            Text(
                text = "${g.floor}층" + if (g.cursed) " · 저주" else "",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = if (g.cursed) ForgeRed else MaterialTheme.colorScheme.primary,
            )
            Text("최고 ${g.best}층", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                "확정 %,d골드 · %d조각".format(g.bankedGold, g.bankedShards),
                fontSize = 12.sp,
                color = ForgeGreen,
            )
            Text(
                "미확정 %,d골드 · %d조각".format(g.pendingGold, g.pendingShards),
                fontSize = 12.sp,
                color = ForgeAmber,
            )
        }
        if (g.buffs.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = g.buffs.joinToString(" · ") { "${it.label}(${it.blurb})" },
                fontSize = 11.sp,
                color = ForgeAmber,
            )
        }

        when {
            g.over -> RunOver(g, onLeave)
            g.choosing -> Crossroad(g, onChoose)
            else -> Wave(g, onTap)
        }
    }
}

/** 웨이브 전투. */
@Composable
private fun androidx.compose.foundation.layout.ColumnScope.Wave(
    g: GauntletUiState,
    onTap: () -> Unit,
) {
    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("처치 ${g.kills}/${g.waveSize}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
        Text(
            "남은 시간 %.1f초".format(g.timeLeftMillis / 1000.0),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (g.timeLeftMillis < 6_000) ForgeRed else MaterialTheme.colorScheme.onSurface,
        )
    }
    Spacer(Modifier.height(4.dp))
    TimeBar(g.timeLeftMillis / (25_000f + 3_000f))

    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .clickable(enabled = g.monsterHp > 0, onClick = onTap),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // 회랑 몬스터는 무한 회랑 구역의 그림을 순환한다. 보스 층은 보스 그림.
            val hallZone = Zone.ENDLESS_HALL
            val name = if (g.isBossFloor) {
                hallZone.bossName
            } else {
                hallZone.monsters[g.kills % hallZone.monsters.size].name
            }
            Text(
                text = if (g.isBossFloor) "회랑의 수문장" else "${g.floor}층의 그림자",
                fontSize = if (g.isBossFloor) 24.sp else 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (g.isBossFloor) ForgeRed else MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(10.dp))
            MonsterSprite(
                name = name,
                hpRatio = g.hpRatio,
                isBoss = g.isBossFloor,
                isRare = false,
                enraged = g.cursed,
                hitSeq = 0,
            )
            Spacer(Modifier.height(12.dp))
            HpLine(g)
            Spacer(Modifier.height(12.dp))
            Text(
                "화면을 눌러 공격",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            )
        }
    }
}

@Composable
private fun HpLine(g: GauntletUiState) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(12.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Box(
            Modifier
                .fillMaxWidth(g.hpRatio)
                .fillMaxHeight()
                .background(if (g.isBossFloor) ForgeRed else MaterialTheme.colorScheme.primary),
        )
    }
    Text(
        "%,d / %,d".format(g.monsterHp, g.monsterMaxHp),
        fontSize = 11.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun TimeBar(ratio: Float) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Box(
            Modifier
                .fillMaxWidth(ratio.coerceIn(0f, 1f))
                .fillMaxHeight()
                .background(ForgeAmber),
        )
    }
}

/** 갈림길. 시간이 멈춘다 - 고민은 공짜다. */
@Composable
private fun androidx.compose.foundation.layout.ColumnScope.Crossroad(
    g: GauntletUiState,
    onChoose: (Int) -> Unit,
) {
    Spacer(Modifier.height(20.dp))
    Text(
        "${g.floor}층을 깼다. 갈림길이다.",
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
    )
    Spacer(Modifier.height(14.dp))
    g.choices.forEachIndexed { index, choice ->
        ForgePanel(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp)
                .clickable { onChoose(index) },
        ) {
            Column(Modifier.padding(14.dp)) {
                when (choice) {
                    is GauntletChoice.Blessing -> {
                        Text(
                            "축복 — ${choice.buff.label}",
                            fontWeight = FontWeight.Bold,
                            color = ForgeAmber,
                        )
                        Text(
                            "${choice.buff.blurb} (이번 런 동안)",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                    }
                    is GauntletChoice.Treasure -> {
                        Text("보물", fontWeight = FontWeight.Bold, color = ForgeGreen)
                        Text(
                            "%,d골드 · %d조각 (위험 없음)".format(choice.gold, choice.shards),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                    }
                    is GauntletChoice.Cursed -> {
                        Text(
                            "저주받은 방",
                            fontWeight = FontWeight.Bold,
                            color = ForgeRed,
                        )
                        Text(
                            "다음 층 체력 2배, 보상 4배",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                    }
                }
            }
        }
    }
}

/** 런 종료 정산. */
@Composable
private fun androidx.compose.foundation.layout.ColumnScope.RunOver(
    g: GauntletUiState,
    onLeave: () -> Unit,
) {
    Spacer(Modifier.height(40.dp))
    Text("시간이 다했다", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = ForgeRed)
    Spacer(Modifier.height(10.dp))
    Text("${g.floor}층에서 런 종료 · 최고 기록 ${g.best}층", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
    Spacer(Modifier.height(16.dp))
    Text(
        "확정 %,d골드는 전부, 미확정 %,d골드는 70%%만 들고 나간다"
            .format(g.bankedGold, g.pendingGold),
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(20.dp))
    Button(onClick = onLeave, modifier = Modifier.fillMaxWidth().height(52.dp)) {
        Text("정산하고 나가기", fontWeight = FontWeight.Bold)
    }
}
