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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geomgang.core.AdventureState
import com.geomgang.core.Combat
import com.geomgang.core.FamilyStyle
import com.geomgang.core.Sword
import com.geomgang.core.Zone
import com.geomgang.game.ForgeUiState

/**
 * 사냥터.
 *
 * 강화가 중심이고 이 화면은 그 돈줄이다. 검을 들고 몬스터를 탭해서 골드와 조각을 번다.
 * **더 센 검 = 탭 한 번에 더 크게 깎임** 이 손끝으로 느껴져야 강화할 이유가 생긴다.
 */
@Composable
fun HuntScreen(
    state: ForgeUiState,
    adventure: AdventureState,
    onEnterZone: (Zone) -> Unit,
    onTap: () -> Unit,
    onChallengeBoss: () -> Unit,
    onLeave: () -> Unit,
    onBack: () -> Unit,
) {
    val hunt = state.hunt
    if (hunt == null) {
        ZonePicker(state, adventure, onEnterZone, onBack)
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(onClick = onLeave) { Text("← 사냥터") }
            Text(
                text = hunt.zone.displayName,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "공격력 %,d".format(hunt.attackPower),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Spacer(Modifier.height(10.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("골드 %,d".format(state.gold), fontSize = 12.sp)
            Text("조각 ${state.shards}", fontSize = 12.sp)
            Text("잡은 수 ${hunt.killsInZone}/${hunt.killsNeeded}", fontSize = 12.sp)
        }

        Spacer(Modifier.height(16.dp))

        // --- 대상 ---
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clickable(enabled = hunt.targetHp > 0, onClick = onTap),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = hunt.targetName,
                    fontSize = if (hunt.isBoss) 26.sp else 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (hunt.isBoss) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onBackground
                    },
                )
                Spacer(Modifier.height(12.dp))

                MonsterBlob(hunt.hpRatio, hunt.isBoss)

                Spacer(Modifier.height(14.dp))
                HpBar(hunt.hpRatio, hunt.isBoss)
                Text(
                    text = "%,d / %,d".format(hunt.targetHp, hunt.targetMaxHp),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                )

                if (hunt.isBoss) {
                    Spacer(Modifier.height(10.dp))
                    TimerBar(hunt.bossTimeRatio)
                    Text(
                        text = "남은 시간 %.1f초".format(hunt.bossRemainingMillis / 1000.0),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                if (hunt.lastDamage > 0 && hunt.targetHp > 0) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = if (hunt.lastHits > 1) {
                            "-%,d  (%d연타)".format(hunt.lastDamage, hunt.lastHits)
                        } else {
                            "-%,d".format(hunt.lastDamage)
                        },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD48A),
                    )
                }

                if (hunt.combo >= 3) {
                    Text(
                        text = "${hunt.combo}연속",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                if (hunt.targetHp > 0) {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "화면을 눌러 공격",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                    )
                }
            }
        }

        // --- 알림과 보스 도전 ---
        if (hunt.zoneCleared) {
            Notice("${hunt.zone.displayName}을 깼다. 다음 구역이 열렸다", MaterialTheme.colorScheme.primary)
        } else if (hunt.bossFailed) {
            Notice("시간 안에 못 잡았다. 잡몹부터 다시 모아야 한다", MaterialTheme.colorScheme.error)
        }

        if (hunt.killsInZone >= hunt.killsNeeded && !hunt.isBoss && !hunt.zoneCleared) {
            Spacer(Modifier.height(10.dp))
            val beatable = Combat.canBeatBoss(state.sword, hunt.zone)
            if (!beatable) {
                Text(
                    text = "지금 공격력으로는 시간 안에 잡기 어렵다 (권장 +${hunt.zone.recommendedLevel})",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.height(6.dp))
            }
            Button(
                onClick = onChallengeBoss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                Text("${hunt.zone.bossName} 도전", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ZonePicker(
    state: ForgeUiState,
    adventure: AdventureState,
    onEnterZone: (Zone) -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
    ) {
        ScreenHeader(title = "사냥터", onBack = onBack)

        if (state.sword == null) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("검이 없다", fontWeight = FontWeight.Bold)
                    Text(
                        text = "검을 들지 않으면 사냥할 수 없다. 상점에서 먼저 검을 구해라.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
            }
            return@Column
        }

        val sword = state.sword
        Text(
            text = "공격력 %,d".format(state.attackPower),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "${sword.family.displayName} · ${FamilyStyle.of(sword.family).blurb}",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
        )

        Spacer(Modifier.height(16.dp))

        Zone.entries.forEach { zone ->
            ZoneCard(
                zone = zone,
                sword = sword,
                unlocked = adventure.isUnlocked(zone),
                cleared = adventure.isCleared(zone),
                onClick = { onEnterZone(zone) },
            )
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun ZoneCard(
    zone: Zone,
    sword: Sword,
    unlocked: Boolean,
    cleared: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (unlocked) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val alpha = if (unlocked) 1f else 0.35f
            Column(Modifier.weight(1f)) {
                Text(
                    text = zone.displayName,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                )
                Text(
                    text = "${zone.monsterName} · 권장 +${zone.recommendedLevel}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha * 0.6f),
                )
                Text(
                    text = "마리당 %,d골드".format(zone.monsterGold),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha * 0.6f),
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                when {
                    !unlocked -> Text(
                        "잠김",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    )

                    cleared -> Text(
                        "클리어",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                    )

                    else -> Text("›", fontSize = 20.sp, color = MaterialTheme.colorScheme.secondary)
                }
                if (unlocked && !Combat.canBeatBoss(sword, zone)) {
                    Text(
                        "보스 무리",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                    )
                }
            }
        }
    }
}

/** 몬스터 자리. 체력이 줄면 쪼그라든다. */
@Composable
private fun MonsterBlob(hpRatio: Float, isBoss: Boolean) {
    val base = if (isBoss) 150 else 110
    val side = (base * (0.55f + hpRatio * 0.45f)).dp
    Box(
        modifier = Modifier
            .size(side)
            .clip(RoundedCornerShape(percent = 45))
            .background(
                if (isBoss) Color(0xFF7A2436) else Color(0xFF3A3350),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (hpRatio <= 0f) "쓰러짐" else if (isBoss) "보스" else "",
            fontSize = 13.sp,
            color = Color.White.copy(alpha = 0.75f),
        )
    }
}

@Composable
private fun HpBar(ratio: Float, isBoss: Boolean) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(14.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(Color(0xFF2A2340)),
    ) {
        Box(
            Modifier
                .fillMaxWidth(ratio)
                .fillMaxHeight()
                .background(if (isBoss) Color(0xFFE05A5A) else Color(0xFF7FD48A)),
        )
    }
}

@Composable
private fun TimerBar(ratio: Float) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF2A2340)),
    ) {
        Box(
            Modifier
                .fillMaxWidth(ratio)
                .fillMaxHeight()
                .background(Color(0xFFE0A458)),
        )
    }
}

@Composable
private fun Notice(text: String, color: Color) {
    Spacer(Modifier.height(8.dp))
    Text(text = text, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
}
