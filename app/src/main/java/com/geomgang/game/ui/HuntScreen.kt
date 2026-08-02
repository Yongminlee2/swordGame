package com.geomgang.game.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.sp
import com.geomgang.core.AdventureState
import com.geomgang.core.Combat
import com.geomgang.core.FamilyStyle
import com.geomgang.core.HuntEvent
import com.geomgang.core.Sword
import com.geomgang.core.Zone
import com.geomgang.game.ForgeUiState
import com.geomgang.game.HuntUiState
import kotlin.random.Random

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
    onEnterGauntlet: () -> Unit,
    onEnterZone: (Zone) -> Unit,
    onTap: () -> Unit,
    onChallengeBoss: () -> Unit,
    onTapNugget: () -> Unit,
    onBuyMerchant: () -> Unit,
    onRetryBoss: () -> Unit,
    onGiveUpBoss: () -> Unit,
    onStayInZone: () -> Unit,
    onNextZone: () -> Unit,
    onLeave: () -> Unit,
    onBack: () -> Unit,
) {
    val hunt = state.hunt
    if (hunt == null) {
        ZonePicker(state, adventure, onEnterGauntlet, onEnterZone, onBack)
        return
    }

    // 승패는 알림 한 줄이 아니라 창으로 알린다. 5초를 걸고 싸운 결과가
    // 화면 구석 한 줄로 지나가면 그 순간이 없던 일이 된다.
    if (hunt.bossFailed) {
        BossFailedDialog(hunt, onRetryBoss, onGiveUpBoss)
    } else if (hunt.zoneCleared) {
        BossWonDialog(hunt, onStayInZone, onNextZone, onBack)
    }

    // 강화 화면과 같은 이유로 스크롤된다 - 이벤트 배너·금덩이 버튼·보스 도전이
    // 한꺼번에 뜨면 짧은 화면에서 아래가 잘린다. 탭 공격은 스크롤과 충돌하지 않는다.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(zoneBrush(hunt.zone))
            .verticalScroll(rememberScrollState())
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

        // --- 이벤트 띠 ---
        if (hunt.goldenRemainingMillis > 0) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "✨ 골든타임! 골드·조각 2배 (${hunt.goldenRemainingMillis / 1000}초)",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFD54A),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x33FFD54A))
                    .padding(vertical = 6.dp, horizontal = 10.dp),
            )
        }
        if (hunt.merchantOffer != null) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x3364B5F6))
                    .padding(vertical = 6.dp, horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "떠돌이 상인 — ${hunt.merchantOffer.displayName} 30% 할인",
                    fontSize = 12.sp,
                    color = Color(0xFF9BD1FF),
                )
                TextButton(
                    onClick = onBuyMerchant,
                    enabled = state.gold >= hunt.merchantPrice,
                ) {
                    Text("%,d골드".format(hunt.merchantPrice), fontSize = 12.sp)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // --- 대상 ---
        // 스크롤되는 열 안에서는 weight 를 쓸 수 없다(높이가 무한대다). 최소 높이로 잡는다.
        Box(
            modifier = Modifier
                .heightIn(min = 260.dp)
                .fillMaxWidth()
                .clickable(enabled = hunt.targetHp > 0, onClick = onTap),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (hunt.event != null) {
                        Text(
                            text = when (hunt.event) {
                                HuntEvent.TREASURE -> "보물"
                                HuntEvent.MIMIC -> "미믹"
                                HuntEvent.ELITE -> "정예"
                                HuntEvent.STRANGE_EGG -> "알"
                                else -> hunt.event.displayName
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10222E),
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFFFD54A))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
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
                }
                if (hunt.event == HuntEvent.TREASURE && hunt.targetHp > 0) {
                    Text(
                        text = "%.1f초 안에 잡아라!".format(hunt.eventRemainingMillis / 1000.0),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD54A),
                    )
                }
                Spacer(Modifier.height(12.dp))

                MonsterSprite(
                    name = hunt.rawTargetName,
                    hpRatio = hunt.hpRatio,
                    isBoss = hunt.isBoss,
                    isRare = hunt.isRare,
                    enraged = hunt.enraged,
                    hitSeq = hunt.hitSeq,
                )
                if (hunt.targetHp <= 0) {
                    Text(
                        text = "쓰러짐",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.75f),
                    )
                }

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

            // 데미지 숫자는 몬스터 위 레이어에서 튀어오른다
            DamagePopups(hunt)
        }

        // --- 금덩이 ---
        if (hunt.nugget) {
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onTapNugget,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFD54A),
                    contentColor = Color(0xFF10222E),
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text("금덩이가 떨어졌다! 탭!", fontWeight = FontWeight.Bold)
            }
        }

        // --- 보스 도전 ---
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
    onEnterGauntlet: () -> Unit,
    onEnterZone: (Zone) -> Unit,
    onBack: () -> Unit,
) {
    // 구역이 24곳이라 한 화면에 다 들어가지 않는다. 스크롤이 없으면 화면 높이만큼만
    // 보이고 나머지는 없는 것이 된다 - 실제로 "늪지밖에 없다"는 말을 들었다.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        ScreenHeader(title = "사냥터", onBack = onBack, wallet = state.wallet())

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

        Spacer(Modifier.height(12.dp))

        // 무한 회랑은 사냥의 변형이라 여기서 들어간다. 강화 화면에 두면
        // 강화와 무관한 문이 그 화면의 자리를 먹는다.
        OutlinedButton(
            onClick = onEnterGauntlet,
            enabled = !state.busy && state.gauntletUnlocked,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = when {
                    !state.gauntletUnlocked -> "🔒 무한 회랑 — 화산 보스를 잡으면 열린다"
                    state.gauntletBest > 0 -> "무한 회랑 · 최고 ${state.gauntletBest}층"
                    else -> "무한 회랑"
                },
                color = if (state.gauntletUnlocked) Color(0xFFC79BFF) else Color.Unspecified,
            )
        }

        Spacer(Modifier.height(12.dp))

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
                    text = "권장 +${zone.recommendedLevel} · 몬스터 ${zone.monsters.size}종",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha * 0.6f),
                )
                Text(
                    text = zone.monsters.joinToString(" · ") { it.name },
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha * 0.5f),
                )
                Text(
                    text = "마리당 %,d~%,d골드".format(
                        zone.monsters.minOf { zone.goldOf(it) },
                        zone.monsters.maxOf { zone.goldOf(it) },
                    ),
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

/**
 * 화면에 떠 있는 숫자 하나.
 *
 * 음수 id 는 처치 골드 팝업이고, [skill] 이 채워지면 스킬 이름을 크게 띄운다.
 */
private data class DamagePop(
    val id: Long,
    val text: String,
    val strong: Boolean,
    val xJitter: Int,
    val skill: String? = null,
)

/**
 * 데미지 숫자 팝업.
 *
 * hitSeq 가 바뀔 때마다 숫자가 튀어올라 사라진다. 치명타와 처치 골드는 크고 노랗게.
 * 연타 시 화면이 숫자로 뒤덮이지 않게 동시에 6개까지만 띄운다.
 */
@Composable
private fun DamagePopups(hunt: HuntUiState, modifier: Modifier = Modifier) {
    val pops = remember { mutableStateListOf<DamagePop>() }
    LaunchedEffect(hunt.hitSeq) {
        if (hunt.hitSeq == 0L || hunt.lastDamage <= 0) return@LaunchedEffect
        val text = if (hunt.lastHits > 1) {
            "-%,d ×${hunt.lastHits}".format(hunt.lastDamage / hunt.lastHits)
        } else {
            "-%,d".format(hunt.lastDamage)
        }
        pops += DamagePop(
            id = hunt.hitSeq,
            text = text,
            // 스킬이 터지면 치명타가 아니어도 크게 띄운다 - 그 순간이 판을 가른다
            strong = hunt.lastCrit || hunt.lastSkill != null,
            xJitter = Random.nextInt(-40, 41),
            skill = hunt.lastSkill?.name,
        )
        if (hunt.targetHp <= 0 && hunt.lastKillGold > 0) {
            pops += DamagePop(-hunt.hitSeq, "+%,d".format(hunt.lastKillGold), true, 0)
        }
        while (pops.size > 6) pops.removeAt(0)
    }
    Box(modifier) {
        pops.forEach { pop ->
            key(pop.id) {
                PopText(pop) { pops.remove(pop) }
            }
        }
    }
}

@Composable
private fun PopText(pop: DamagePop, onDone: () -> Unit) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(pop.id) {
        progress.animateTo(1f, tween(durationMillis = if (pop.strong) 900 else 650))
        onDone()
    }
    val label = when {
        pop.skill != null -> "${pop.skill}!  ${pop.text}"
        pop.strong && pop.id > 0 -> "치명타! ${pop.text}"
        else -> pop.text
    }
    Text(
        text = label,
        fontSize = if (pop.strong) 24.sp else 16.sp,
        fontWeight = FontWeight.Bold,
        // 스킬은 청록빛으로 구분한다 - 치명타(금색)와 겹쳐 터져도 무엇이 터졌는지 읽힌다
        color = when {
            pop.skill != null -> Color(0xFF7FE8FF)
            pop.strong -> Color(0xFFFFD54A)
            else -> Color(0xFFEEEEEE)
        },
        modifier = Modifier
            .offset { IntOffset(pop.xJitter, (-90 * progress.value).toInt()) }
            .graphicsLayer { alpha = 1f - progress.value * progress.value },
    )
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

/**
 * 보스를 놓쳤다.
 *
 * 골드를 내면 잡몹을 다시 모으지 않고 **즉시** 다시 붙는다. 낮추려는 것은
 * 재도전 문턱이지 5초의 긴장이 아니다. 값은 다시 도전할 때마다 두 배가 된다.
 */
@Composable
private fun BossFailedDialog(
    hunt: HuntUiState,
    onRetry: () -> Unit,
    onGiveUp: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = {},
        properties = STICKY_DIALOG,
        title = { Text("보스를 놓쳤다", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("${hunt.zone.bossName}이(가) 달아났다.", fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "나가면 잡몹부터 다시 모아야 한다.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onRetry, enabled = hunt.canRetry) {
                Text("다시 도전 · %,d".format(hunt.retryPrice))
            }
        },
        dismissButton = {
            TextButton(onClick = onGiveUp) { Text("나가기") }
        },
    )
}

/** 보스를 잡았다. 무엇을 얼마나 벌었는지가 승리감의 대부분이다. */
@Composable
private fun BossWonDialog(
    hunt: HuntUiState,
    onStay: () -> Unit,
    onNextZone: () -> Unit,
    onGoHome: () -> Unit,
) {
    val reward = hunt.bossReward
    AlertDialog(
        onDismissRequest = {},
        properties = STICKY_DIALOG,
        title = { Text("${hunt.zone.bossName} 격파", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                if (reward != null) {
                    Text("💰 %,d".format(reward.gold), fontWeight = FontWeight.Bold)
                    Text("💎 조각 ${reward.shards}", fontWeight = FontWeight.Bold)
                    Text("🪨 강화석 ${reward.stones}", fontWeight = FontWeight.Bold)
                    reward.petName?.let {
                        Text("🥚 $it 알!", color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.height(8.dp))
                }
                Text(
                    text = "다음 구역이 열렸다.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
        },
        // 셋을 가로로 늘어놓으면 글자가 접힌다. 세로로 쌓아 각 줄을 온전히 읽게 한다.
        // 보스를 잡은 뒤가 강화하러 돌아가기 가장 좋은 때다 — 그 문을 여기 둔다.
        confirmButton = {
            Column(horizontalAlignment = Alignment.End) {
                TextButton(onClick = onNextZone) { Text("다음 구역으로") }
                TextButton(onClick = onStay) { Text("이 구역 더 돌기") }
                TextButton(onClick = onGoHome) { Text("🔨 홈으로 (강화하러)") }
            }
        },
    )
}

/**
 * 승패 창은 바깥을 눌러도, 뒤로 가도 닫히지 않는다.
 *
 * 기본값대로 두면 잘못 스친 손가락이 대신 고른다 — 승리 창에서는 그것이
 * "이 구역 더 돌기"가 되고, 패배 창에서는 골드가 걸린 갈림길이 그냥 지나간다.
 * 둘 중 하나를 반드시 직접 눌러야 한다.
 */
private val STICKY_DIALOG = DialogProperties(
    dismissOnBackPress = false,
    dismissOnClickOutside = false,
)

@Composable
private fun Notice(text: String, color: Color) {
    Spacer(Modifier.height(8.dp))
    Text(text = text, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
}
