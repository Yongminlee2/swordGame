package com.geomgang.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geomgang.core.GauntletChoice
import com.geomgang.core.GauntletEngine
import com.geomgang.core.Zone
import com.geomgang.game.ForgeUiState
import com.geomgang.game.GauntletUiState
import com.geomgang.game.R

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
        ForgePanel(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(onClick = onLeave) {
                        PixelIcon(
                            resource = R.drawable.ui_pixel_back,
                            contentDescription = null,
                            modifier = Modifier.size(21.dp),
                        )
                        LText("나가기")
                    }
                    LText(
                        text = "${g.floor}층" + if (g.cursed) " · 저주" else "",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = if (g.cursed) ForgeRed else MaterialTheme.colorScheme.primary,
                    )
                    LText(
                        "최고 ${g.best}층",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                ThinRule(Modifier.fillMaxWidth().padding(vertical = 8.dp))
                // 두 문장에 폭을 나눠 준다. 폭이 없으면 번역된 긴 문장이 서로
                // 맞붙어 두 줄로 넘쳤다 - 인도네시아어에서 그랬다.
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    LText(
                        "확정 %,d골드 · %d조각".format(g.bankedGold, g.bankedShards),
                        modifier = Modifier.weight(1f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        color = ForgeGreen,
                    )
                    LText(
                        "미확정 %,d골드 · %d조각".format(g.pendingGold, g.pendingShards),
                        modifier = Modifier.weight(1f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        textAlign = TextAlign.End,
                        color = ForgeAmber,
                    )
                }
                if (g.buffs.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    LText(
                        text = g.buffs.joinToString(" · ") { "${it.label}(${it.blurb})" },
                        fontSize = 11.sp,
                        color = ForgeAmber,
                    )
                }
            }
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
        LText("처치 ${g.kills}/${g.waveSize}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
        LText(
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
            .clip(CutCornerShape(5.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.70f))
            .border(1.dp, MaterialTheme.colorScheme.outline, CutCornerShape(5.dp))
            .clickable(enabled = g.monsterHp > 0, onClick = onTap),
        contentAlignment = Alignment.Center,
    ) {
        BattleArenaBackdrop(Modifier.fillMaxSize(), danger = g.isBossFloor || g.cursed)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // 회랑 몬스터는 무한 회랑 구역의 그림을 순환한다. 보스 층은 보스 그림.
            val hallZone = Zone.ENDLESS_HALL
            val name = if (g.isBossFloor) {
                hallZone.bossName
            } else {
                hallZone.monsters[g.kills % hallZone.monsters.size].name
            }
            LText(
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
            LText(
                "화면을 눌러 공격",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            )
        }
    }
}

@Composable
private fun HpLine(g: GauntletUiState) {
    PixelProgressBar(
        progress = g.hpRatio,
        modifier = Modifier.fillMaxWidth(),
        height = 12.dp,
        color = if (g.isBossFloor) ForgeRed else MaterialTheme.colorScheme.primary,
    )
    LText(
        "%,d / %,d".format(g.monsterHp, g.monsterMaxHp),
        fontSize = 11.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun TimeBar(ratio: Float) {
    PixelProgressBar(
        progress = ratio,
        modifier = Modifier.fillMaxWidth(),
        height = 6.dp,
        color = ForgeAmber,
    )
}

/** 갈림길. 시간이 멈춘다 - 고민은 공짜다. */
@Composable
private fun androidx.compose.foundation.layout.ColumnScope.Crossroad(
    g: GauntletUiState,
    onChoose: (Int) -> Unit,
) {
    Spacer(Modifier.height(20.dp))
    LText(
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
                        LText(
                            "축복 — ${choice.buff.label}",
                            fontWeight = FontWeight.Bold,
                            color = ForgeAmber,
                        )
                        LText(
                            "${choice.buff.blurb} (이번 도전 동안)",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                    }
                    is GauntletChoice.Treasure -> {
                        LText("보물", fontWeight = FontWeight.Bold, color = ForgeGreen)
                        LText(
                            "%,d골드 · %d조각 (위험 없음)".format(choice.gold, choice.shards),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                    }
                    is GauntletChoice.Cursed -> {
                        LText(
                            "저주받은 방",
                            fontWeight = FontWeight.Bold,
                            color = ForgeRed,
                        )
                        LText(
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
    LText("시간이 다했다", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = ForgeRed)
    Spacer(Modifier.height(10.dp))
    LText(
        "${g.floor}층에서 도전 종료 · 최고 ${g.best}층",
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Spacer(Modifier.height(16.dp))
    LText(
        "확정 %,d · 미확정 %,d 중 %d%% 획득".format(
            g.bankedGold,
            g.pendingGold,
            (GauntletEngine.LOSS_RATIO * 100).toInt(),
        ),
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(20.dp))
    Button(onClick = onLeave, modifier = Modifier.fillMaxWidth().height(52.dp)) {
        LText("정산하고 나가기", fontWeight = FontWeight.Bold)
    }
}
