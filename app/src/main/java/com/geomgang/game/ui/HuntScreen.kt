package com.geomgang.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.sp
import com.geomgang.game.R
import com.geomgang.core.AdventureState
import com.geomgang.core.Combat
import com.geomgang.core.FamilyStyle
import com.geomgang.core.HuntEvent
import com.geomgang.core.Sword
import com.geomgang.core.familyLabel
import com.geomgang.core.Zone
import com.geomgang.game.ForgeUiState
import com.geomgang.game.HuntUiState
import kotlin.random.Random
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween

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
    val bossReady = hunt.killsInZone >= hunt.killsNeeded && !hunt.isBoss && !hunt.zoneCleared

    // 승패는 알림 한 줄이 아니라 창으로 알린다. 5초를 걸고 싸운 결과가
    // 화면 구석 한 줄로 지나가면 그 순간이 없던 일이 된다.
    if (hunt.bossFailed) {
        BossFailedDialog(hunt, onGiveUpBoss)
    } else if (hunt.zoneCleared) {
        BossWonDialog(hunt, onStayInZone, onNextZone, onBack)
    }

    // 전투 중 화면은 고정한다. 대상 패널이 남은 높이를 차지하고 이벤트·보스 버튼이
    // 나타날 때만 스스로 줄어들어, 연타 중 화면이 위아래로 미끄러지지 않는다.
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
                        LText("사냥터")
                    }
                    // 가운데 칸에 폭을 주지 않으면 번역된 긴 이름이 계절 도장 위로 넘친다.
                    Column(
                        modifier = Modifier.weight(1f).padding(horizontal = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        LText(
                            text = hunt.zone.displayName,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        LText(
                            text = "공격력 %,d".format(hunt.attackPower),
                            fontSize = 11.sp,
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    SeasonStamp(compact = true)
                }
                ThinRule(Modifier.fillMaxWidth().padding(vertical = 8.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    HuntStat(R.drawable.ui_pixel_gold, "%,d".format(state.gold))
                    HuntStat(R.drawable.ui_pixel_gem, "${state.shards}")
                    HuntStat(
                        R.drawable.ui_pixel_target,
                        "${hunt.killsInZone}/${hunt.killsNeeded}",
                    )
                }
            }
        }

        // --- 이벤트 띠 ---
        if (hunt.goldenRemainingMillis > 0) {
            Spacer(Modifier.height(8.dp))
            LText(
                text = "골든타임 · 골드·조각 2배 (${hunt.goldenRemainingMillis / 1000}초)",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = ForgeAmber,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CutCornerShape(4.dp))
                    .background(Color(0x33FFD54A))
                    .border(1.dp, ForgeAmber.copy(alpha = 0.55f), CutCornerShape(4.dp))
                    .padding(vertical = 6.dp, horizontal = 10.dp),
            )
        }
        if (hunt.merchantOffer != null) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CutCornerShape(4.dp))
                    .background(Color(0x3364B5F6))
                    .border(1.dp, Color(0xFF64B5F6).copy(alpha = 0.55f), CutCornerShape(4.dp))
                    .padding(vertical = 6.dp, horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                LText(
                    text = "떠돌이 상인 — ${hunt.merchantOffer.displayName} 30% 할인",
                    fontSize = 12.sp,
                    color = Color(0xFF9BD1FF),
                )
                TextButton(
                    onClick = onBuyMerchant,
                    enabled = state.gold >= hunt.merchantPrice,
                ) {
                    LText("%,d골드".format(hunt.merchantPrice), fontSize = 12.sp)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // --- 대상 ---
        Box(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 300.dp)
                .fillMaxWidth()
                .clip(CutCornerShape(5.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f))
                .border(1.dp, MaterialTheme.colorScheme.outline, CutCornerShape(5.dp))
                .clickable(enabled = hunt.targetHp > 0 && !bossReady, onClick = onTap),
            contentAlignment = Alignment.Center,
        ) {
            BattleArenaBackdrop(Modifier.fillMaxSize(), danger = hunt.isBoss || bossReady)

            if (bossReady) {
                Column(
                    modifier = Modifier.padding(horizontal = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    LText(
                        text = "BOSS GATE OPEN",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = ForgeRed,
                        modifier = Modifier
                            .clip(CutCornerShape(3.dp))
                            .background(ForgeRed.copy(alpha = 0.16f))
                            .border(1.dp, ForgeRed.copy(alpha = 0.65f), CutCornerShape(3.dp))
                            .padding(horizontal = 9.dp, vertical = 3.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    LText(
                        text = "${hunt.zone.bossName} 출현",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.error,
                    )
                    LText(
                        text = "잡몹 ${hunt.killsNeeded}마리 처치 완료",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    )
                    if (hunt.lastHitKilled && hunt.lastDamage > 0L) {
                        LText(
                            text = "마지막 피해 %,d".format(hunt.lastDamage),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = ForgeAmber,
                        )
                    }
                    MonsterSprite(
                        name = hunt.zone.bossName,
                        hpRatio = 1f,
                        isBoss = true,
                        isRare = false,
                        enraged = false,
                        hitSeq = 0,
                    )
                    LText(
                        text = "아래 버튼을 눌러 보스전에 돌입",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ForgeAmber,
                    )
                }
            } else Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (hunt.event != null) {
                        LText(
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
                                .clip(CutCornerShape(3.dp))
                                .background(ForgeAmber)
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                    LText(
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
                    LText(
                        text = "%.1f초 안에 잡아라!".format(hunt.eventRemainingMillis / 1000.0),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = ForgeAmber,
                    )
                }
                Spacer(Modifier.height(12.dp))

                MonsterSprite(
                    name = hunt.rawTargetName,
                    hpRatio = hunt.hpRatio,
                    isBoss = hunt.isBoss,
                    isRare = hunt.isRare,
                    enraged = hunt.enraged,
                    // 한 방 처치 후 바로 나온 다음 몬스터를 이전 타격으로 흔들지 않는다.
                    hitSeq = if (hunt.lastHitKilled && hunt.targetHp > 0) 0 else hunt.hitSeq,
                )
                if (hunt.targetHp <= 0) {
                    LText(
                        text = "쓰러짐",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.75f),
                    )
                }

                Spacer(Modifier.height(14.dp))
                HpBar(hunt.hpRatio, hunt.isBoss)
                LText(
                    text = "%,d / %,d".format(hunt.targetHp, hunt.targetMaxHp),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                )

                if (hunt.isBoss) {
                    Spacer(Modifier.height(10.dp))
                    TimerBar(hunt.bossTimeRatio)
                    LText(
                        text = "남은 시간 %.1f초".format(hunt.bossRemainingMillis / 1000.0),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                if (hunt.combo >= 3) {
                    LText(
                        text = "${hunt.combo}연속",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                if (hunt.targetHp > 0) {
                    Spacer(Modifier.height(14.dp))
                    LText(
                        "화면을 눌러 공격",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                    )
                }
            }

            // 데미지 숫자는 몬스터 위 레이어에서 튀어오른다
            if (!bossReady) CombatFeedbackOverlay(hunt)
        }

        // --- 금덩이 ---
        if (hunt.nugget) {
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onTapNugget,
                shape = CutCornerShape(5.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ForgeAmber,
                    contentColor = Color(0xFF10222E),
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                LText("금덩이가 떨어졌다! 탭!", fontWeight = FontWeight.Bold)
            }
        }

        // --- 보스 도전 ---
        if (bossReady) {
            Spacer(Modifier.height(10.dp))
            val beatable = Combat.canBeatBoss(state.sword, hunt.zone)
            if (!beatable) {
                LText(
                    text = "공격력 부족 · 권장 +${hunt.zone.recommendedLevel}",
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
                LText("${hunt.zone.bossName} 도전", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun HuntStat(resource: Int, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        PixelIcon(
            resource = resource,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        LText(
            text = value,
            modifier = Modifier.padding(start = 5.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
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
    ScrollableForgeScreen(title = "사냥터", onBack = onBack, wallet = state.wallet()) {
        if (state.sword == null) {
            ForgePanel(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    LText("검이 없다", fontWeight = FontWeight.Bold)
                    LText(
                        text = "검을 장착해야 사냥할 수 있다.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
            }
            return@ScrollableForgeScreen
        }

        val sword = state.sword
        LText(
            text = "공격력 %,d".format(state.attackPower),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        LText(
            text = "${sword.familyLabel} · ${FamilyStyle.of(sword.family).blurb}",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
        )
        if (sword.family == com.geomgang.core.WeaponFamily.DRAGON) {
            val skill = com.geomgang.core.Skills.of(sword)
            LText(
                text = "스킬 ${skill.name} · ${skill.blurb}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.82f),
            )
        }

        Spacer(Modifier.height(12.dp))

        // 무한 회랑은 사냥의 변형이라 여기서 들어간다. 강화 화면에 두면
        // 강화와 무관한 문이 그 화면의 자리를 먹는다.
        OutlinedButton(
            onClick = onEnterGauntlet,
            enabled = !state.busy && state.gauntletUnlocked,
            modifier = Modifier.fillMaxWidth(),
        ) {
            LText(
                text = when {
                    !state.gauntletUnlocked -> "잠김 · 무한 회랑 — 화산 보스를 잡으면 열린다"
                    state.gauntletBest > 0 -> "무한 회랑 · 최고 ${state.gauntletBest}층"
                    else -> "무한 회랑"
                },
                color = if (state.gauntletUnlocked) MaterialTheme.colorScheme.primary else Color.Unspecified,
            )
        }

        Spacer(Modifier.height(12.dp))

        Zone.entries.forEach { zone ->
            ZoneForgePanel(
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
private fun ZoneForgePanel(
    zone: Zone,
    sword: Sword,
    unlocked: Boolean,
    cleared: Boolean,
    onClick: () -> Unit,
) {
    ForgePanel(
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
            val titleAlpha = if (unlocked) 1f else 0.62f
            val secondaryAlpha = if (unlocked) 0.60f else 0.44f
            val detailAlpha = if (unlocked) 0.50f else 0.38f
            Column(Modifier.weight(1f)) {
                LText(
                    text = zone.displayName,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = titleAlpha),
                )
                LText(
                    text = "권장 +${zone.recommendedLevel} · 몬스터 ${zone.monsters.size}종",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = secondaryAlpha),
                )
                LText(
                    text = zone.monsters.joinToString(" · ") { it.name },
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = detailAlpha),
                )
                LText(
                    text = "마리당 %,d~%,d골드".format(
                        zone.monsters.minOf { zone.goldOf(it) },
                        zone.monsters.maxOf { zone.goldOf(it) },
                    ),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = secondaryAlpha),
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                when {
                    !unlocked -> LText(
                        "잠김",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                    )

                    cleared -> LText(
                        "클리어",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                    )

                    else -> PixelIcon(
                        resource = R.drawable.ui_pixel_back,
                        contentDescription = null,
                        modifier = Modifier
                            .size(18.dp)
                            .rotate(180f),
                    )
                }
                if (unlocked && !Combat.canBeatBoss(sword, zone)) {
                    LText(
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
/* Legacy text-only damage popup retained below for source history; the selected illustrated
 * overlay above is the only implementation invoked by HuntScreen. */
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
    LText(
        text = label,
        fontSize = if (pop.strong) 24.sp else 16.sp,
        fontWeight = FontWeight.Bold,
        // 스킬은 청록빛으로 구분한다 - 치명타(금색)와 겹쳐 터져도 무엇이 터졌는지 읽힌다
        color = when {
            pop.skill != null -> Color(0xFF7FE8FF)
            pop.strong -> ForgeAmber
            else -> MaterialTheme.colorScheme.onSurface
        },
        modifier = Modifier
            .offset { IntOffset(pop.xJitter, (-90 * progress.value).toInt()) }
            .graphicsLayer { alpha = 1f - progress.value * progress.value },
    )
}

@Composable
private fun HpBar(ratio: Float, isBoss: Boolean) {
    PixelProgressBar(
        progress = ratio,
        modifier = Modifier.fillMaxWidth(),
        height = 14.dp,
        color = if (isBoss) ForgeRed else ForgeGreen,
    )
}

@Composable
private fun TimerBar(ratio: Float) {
    PixelProgressBar(
        progress = ratio,
        modifier = Modifier.fillMaxWidth(),
        height = 8.dp,
        color = ForgeAmber,
    )
}

/**
 * 보스를 놓쳤다.
 *
 * 실패를 확인하고 사냥터 목록으로 돌아간다. 유료 즉시 재도전은 제공하지 않는다.
 */
@Composable
internal fun BossFailedDialog(
    hunt: HuntUiState,
    onGiveUp: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = {},
        properties = STICKY_DIALOG,
        title = { LText("보스를 놓쳤다", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                LText("${hunt.zone.bossName} 도주", fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                LText(
                    text = "잡몹 처치 수는 초기화된다.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onGiveUp) { LText("확인") }
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
        title = { LText("${hunt.zone.bossName} 격파", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                if (reward != null) {
                    LText("골드 %,d".format(reward.gold), fontWeight = FontWeight.Bold)
                    LText("조각 ${reward.shards}", fontWeight = FontWeight.Bold)
                    LText("강화석 ${reward.stones}", fontWeight = FontWeight.Bold)
                    reward.petName?.let {
                        LText("$it 알 획득", color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.height(8.dp))
                }
                if (hunt.lastHitKilled && hunt.lastDamage > 0L) {
                    LText(
                        text = "마지막 피해 %,d".format(hunt.lastDamage),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = ForgeAmber,
                    )
                    Spacer(Modifier.height(4.dp))
                }
                LText(
                    text = "다음 구역 해금",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
        },
        // 셋을 가로로 늘어놓으면 글자가 접힌다. 세로로 쌓아 각 줄을 온전히 읽게 한다.
        // 보스를 잡은 뒤가 강화하러 돌아가기 가장 좋은 때다 — 그 문을 여기 둔다.
        confirmButton = {
            Column(horizontalAlignment = Alignment.End) {
                TextButton(onClick = onNextZone) { LText("다음 구역으로") }
                TextButton(onClick = onStay) { LText("이 구역 더 돌기") }
                TextButton(onClick = onGoHome) { LText("홈으로 · 강화하러") }
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
    LText(text = text, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
}
