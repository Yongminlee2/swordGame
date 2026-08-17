package com.geomgang.game.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.annotation.DrawableRes
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.geomgang.core.BonusSource
import com.geomgang.core.ForgeResult
import com.geomgang.core.IdleReward
import com.geomgang.core.IdleRewards
import com.geomgang.core.LegendForge
import com.geomgang.core.Smithy
import com.geomgang.core.SwordNames
import com.geomgang.core.familyLabel
import com.geomgang.core.isLegend
import com.geomgang.game.DestroyPhase
import com.geomgang.game.ForgeUiState
import com.geomgang.game.R
import com.geomgang.game.TemperUi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/** S20 Ultra에서 쓰던 결과별 연출 길이(ms). 입력을 잠그지는 않는다. */
private const val SUCCESS_MILLIS = 350
private const val STAY_MILLIS = 250
private const val DROP_MILLIS = 400
private const val DESTROY_MILLIS = 300

/** 흔들림 진폭(px). 하락은 유지보다 크게 흔들려 손해가 바로 느껴진다. */
private const val STAY_SHAKE = 12f
private const val DROP_SHAKE = 26f

@Composable
fun ForgeScreen(
    state: ForgeUiState,
    onForge: () -> Unit,
    onPrevent: () -> Unit,
    onSalvage: () -> Unit,
    onToggleBlessing: () -> Unit,
    onToggleLuckCharm: () -> Unit,
    onOpenHunt: () -> Unit,
    onOpenStorage: () -> Unit,
    onOpenShop: () -> Unit,
    onOpenCraft: () -> Unit,
    onOpenCodex: () -> Unit,
    onOpenMenu: () -> Unit,
    onDismissIdle: () -> Unit,
    onOpenTraining: () -> Unit,
) {
    val shake = remember { Animatable(0f) }
    val buttonFeedback = remember { Animatable(0f) }
    var buttonFeedbackColor by remember { mutableStateOf(Color.White) }
    // 화면을 나갔다 돌아왔을 때 저장된 마지막 결과를 새 강화로 오인하지 않는다.
    val effectGate = remember { ForgeEffectGate(state.forgeResultSeq) }

    // 강화 판정과 연출을 분리했다. 효과는 예전처럼 나오지만 다음 입력을 막지 않는다.
    // 결과 객체 대신 시도 번호를 키로 써서 같은 실패가 연속되어도 매번 다시 재생한다.
    LaunchedEffect(state.forgeResultSeq) {
        if (!effectGate.consume(state.forgeResultSeq)) {
            shake.snapTo(0f)
            buttonFeedback.snapTo(0f)
            return@LaunchedEffect
        }
        val result = state.lastResult ?: run {
            shake.snapTo(0f)
            buttonFeedback.snapTo(0f)
            return@LaunchedEffect
        }
        val (color, durationMillis) = when (result) {
            is ForgeResult.Success -> Color(0xFFFFF3D0) to SUCCESS_MILLIS
            is ForgeResult.Stay -> Color(0xFF6C5A3A) to STAY_MILLIS
            is ForgeResult.Drop ->
                (if (result.shattered) ForgeRed else ForgeOrange) to DROP_MILLIS
            is ForgeResult.Destroyed -> ForgeRed to DESTROY_MILLIS
        }
        buttonFeedbackColor = color
        // 흔들림과 버튼 피드백은 같은 판정에서 동시에 시작한다.
        coroutineScope {
            launch {
                when (result) {
                    is ForgeResult.Stay -> shake.shakeOnceRealtime(STAY_SHAKE, STAY_MILLIS)
                    is ForgeResult.Drop -> shake.shakeOnceRealtime(DROP_SHAKE, DROP_MILLIS)
                    is ForgeResult.Success,
                    is ForgeResult.Destroyed -> shake.snapTo(0f)
                }
            }
            launch { buttonFeedback.buttonFeedbackOnceRealtime(durationMillis) }
        }
    }

    // 자리비움 보상은 창으로 알린다. 화면에 자리를 만들어 두면 평소에는 빈 칸이다.
    state.idleReward?.let { IdleRewardDialog(it, onDismissIdle) }

    // 파괴는 이 게임에서 가장 아픈 순간이다. 작은 원 하나로 지나가면 무엇을 놓쳤는지도
    // 모른 채 검이 사라진다. 창으로 묻되 **제한 시간은 그대로 둔다** - 2.5초의 긴장이
    // 이 게임의 핵심이고, 창은 그 긴장을 없애는 것이 아니라 보이게 하는 것이다.
    when (val phase = state.destroyPhase) {
        is DestroyPhase.Choice -> DestroyDialog(
            progress = phase.progress,
            canPrevent = state.canPrevent,
            // 검이 손에 남아 있으면 부서지고도 살아남은 것이다(전설검·조합검).
            // 되살릴 것이 없어 방지권만 잠기고, 파편은 똑같이 줍는다.
            survived = state.sword != null,
            preventTickets = if (state.usesLegendPrevent) {
                state.legendPreventTickets
            } else {
                state.preventTickets
            },
            legendPrevent = state.usesLegendPrevent,
            onPrevent = onPrevent,
            onSalvage = onSalvage,
        )

        DestroyPhase.None -> Unit
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // 강화는 한 화면에서 끝난다. 검 영역만 남는 높이를 나눠 쓰고,
        // 핵심 조작과 하단 메뉴는 어떤 진행 단계에서도 화면 밖으로 밀지 않는다.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LText(
                        text = state.season.smithyName,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp,
                    )
                    Spacer(
                        Modifier
                            .padding(horizontal = 12.dp)
                            .width(1.dp)
                            .height(24.dp)
                            .background(MaterialTheme.colorScheme.outline),
                    )
                    LText(
                        text = "${state.season.roman} · ${state.season.displayName}",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
                state.progress.selectedTitle?.let {
                    LText(
                        text = it.title,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            ForgePanel(
                modifier = Modifier.clickable(enabled = !state.busy, onClick = onOpenMenu),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PixelIcon(
                        resource = R.drawable.ui_pixel_record,
                        contentDescription = "기록 메뉴",
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(Modifier.width(5.dp))
                    Column(horizontalAlignment = Alignment.End) {
                        LText(
                            text = "최고 +${state.bestLevel}",
                            fontSize = 12.sp,
                            lineHeight = 12.sp,
                            fontWeight = FontWeight.Black,
                        )
                        LText(
                            text = "기록",
                            fontSize = 8.sp,
                            lineHeight = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        Box(Modifier.fillMaxWidth().padding(top = 5.dp)) {
            ThinRule(Modifier.fillMaxWidth())
            ThinRule(
                modifier = Modifier.width(82.dp),
                thickness = 2.dp,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        // 재화는 **맨 위**에 둔다. 검 그림 아래에 두었더니 화면이 짧은 기기에서는
        // 스크롤해야 보여서 "몇 개 있는지 모르겠다" 는 말이 나왔다.
        Spacer(Modifier.height(6.dp))
        WalletBar(state.wallet())
        Spacer(Modifier.height(6.dp))

        // 검 그림이 남는 높이를 **전부** 가져간다. 상한을 두었더니 용검처럼 아래
        // 정보가 많은 판에서 그림만 손톱만 해졌다 - 이 화면의 주인공은 검이다.
        Box(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 132.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            when (val phase = state.destroyPhase) {
                // 한 창에서 둘을 고른다 — **왼쪽이 되살리기, 오른쪽이 줍기다.**
                // 예전에는 원이 먼저, 파편이 나중이라 방지권을 안 쓸 작정이어도
                // 첫 창이 끝나기를 기다려야 했다.
                is DestroyPhase.Choice -> Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PreventRing(
                        progress = phase.progress,
                        enabled = state.canPrevent,
                        onTap = onPrevent,
                    )
                    SalvageShards(
                        progress = phase.progress,
                        onTap = onSalvage,
                    )
                }

                // 남는 높이에 맞춰 커진다. 화면마다 조금씩 달라지더라도 검이 크게
                // 보이는 쪽을 택했다.
                DestroyPhase.None -> SwordView(
                    sword = state.sword,
                    modifier = Modifier.fillMaxHeight().aspectRatio(1f),
                    shake = shake.value,
                )
            }
        }

        // 이름·강화 단계는 가변 영역 밖이라 어떤 화면에서도 잘리지 않는다.
        LText(
            text = when (state.destroyPhase) {
                is DestroyPhase.Choice -> "지금 골라야 한다"
                // 이름은 단계마다 다르다. 계열은 형태만 정하고 부제로 내려간다.
                // 고유검은 고유 이름을 금색으로.
                DestroyPhase.None -> state.sword?.let {
                    SwordNames.nameFor(it)
                } ?: "검이 없다"
            },
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            color = if (state.destroyPhase == DestroyPhase.None &&
                state.sword?.uniqueId != null
            ) {
                ForgeAmber
            } else {
                Color.Unspecified
            },
        )
        if (state.destroyPhase == DestroyPhase.None && state.sword != null) {
            LText(
                text = "+${state.sword.level} · ${state.sword.familyLabel}" +
                    if (state.sword.stars > 0) "  · 별 ${state.sword.stars}" else "",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
            )
            // 스킬은 사냥터와 함께 열리는 용검 전용이다.
            if (state.sword.family == com.geomgang.core.WeaponFamily.DRAGON) {
                val skill = com.geomgang.core.Skills.of(state.sword)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PixelIcon(
                        resource = R.drawable.ui_pixel_bolt,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    LText(
                        text = "${com.geomgang.core.Skills.stageLabel(state.sword)?.let { "$it · " } ?: ""}" +
                            "${skill.name} · ${skill.blurb}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        ResultBanner(state.lastResult)
        Spacer(Modifier.height(6.dp))

        // 재화(골드·조각·강화석·방지권)는 맨 위 지갑 줄이 이미 보여 준다.
        // 여기 남는 것은 **이번 강화의 정보** 뿐이다 - 성공률·비용·판매가.
        if (state.sword != null) {
            // 이번 한 번이 어떻게 끝날 수 있는지. 성공률만 보여 주던 탓에
            // 무한 구간에서 **실패가 곧 파괴**라는 걸 모르고 누르게 됐다.
            // 라벨에 숫자를 다시 적지 않는다 - 큰 값 아래 "성공 20%" 가 또 붙으면
            // 같은 수가 두 번 보여 오타처럼 읽힌다. 숫자는 위, 이름은 아래 한 번씩이다.
            val outcomeStats = buildList {
                add(
                    OutcomeDisplay(
                        R.drawable.ui_pixel_target,
                        "성공",
                        "${state.odds.success}%",
                        MaterialTheme.colorScheme.primary,
                    ),
                )
                if (state.odds.stay > 0) {
                    add(OutcomeDisplay(R.drawable.ui_pixel_equal, "유지", "${state.odds.stay}%"))
                }
                if (state.odds.drop > 0) {
                    add(
                        OutcomeDisplay(
                            R.drawable.ui_pixel_down,
                            "하락",
                            "${state.odds.drop}%",
                            ForgeOrange,
                        ),
                    )
                }
                if (state.odds.destroy > 0) {
                    add(
                        OutcomeDisplay(
                            R.drawable.ui_pixel_burst,
                            "파괴",
                            "${state.odds.destroy}%",
                            MaterialTheme.colorScheme.error,
                        ),
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                outcomeStats.forEachIndexed { index, stat ->
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Stat(stat.icon, stat.label, stat.value, stat.color)
                    }
                    if (index < outcomeStats.lastIndex) {
                        Spacer(
                            Modifier
                                .width(1.dp)
                                .height(42.dp)
                                .background(MaterialTheme.colorScheme.outline),
                        )
                    }
                }
            }
            // 쌓은 보너스 합계와 담금질은 재설계에서 떨어져 나갔던 것을 되살린 것이다.
            // 보너스 줄은 도감·스킬이 자라는 것이 보이는 유일한 자리고, 담금질은
            // 무한 구간에서 실패가 눈에 보이는 무언가를 남기는 유일한 자리다.
            Spacer(Modifier.height(6.dp))
            BonusBreakdown(state.bonusSources)
            state.temper?.let {
                Spacer(Modifier.height(6.dp))
                TemperBar(it)
            }
            Spacer(Modifier.height(8.dp))
            ThinRule(Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CostStat(R.drawable.ui_pixel_fire, "강화 비용", compactGold(state.upgradeCost))
                Spacer(
                    Modifier
                        .width(1.dp)
                        .height(34.dp)
                        .background(MaterialTheme.colorScheme.outline),
                )
                CostStat(R.drawable.ui_pixel_tag, "판매가", compactGold(state.sellPrice))
            }

            if (!state.awaitingDestroyChoice) {
                Spacer(Modifier.height(8.dp))
                // 아이템은 한 번 쓰면 토글이 내려간다. 매번 다시 켜야 한다.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PixelToggle(
                        icon = R.drawable.ui_pixel_scroll,
                        label = "축복서 ${state.blessingScrolls}",
                        selected = state.useBlessing,
                        onClick = onToggleBlessing,
                        enabled = state.blessingScrolls > 0 && !state.busy,
                        modifier = Modifier.weight(1f),
                    )
                    PixelToggle(
                        icon = R.drawable.ui_pixel_clover,
                        label = "행운부적 ${state.luckCharms}",
                        selected = state.useLuckCharm,
                        onClick = onToggleLuckCharm,
                        enabled = state.luckCharms > 0 && !state.busy,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // 창이 열려 있으면 하단 버튼을 감춘다. 원과 파편을 눌러야 하기 때문이다.
        if (state.awaitingDestroyChoice) {
            LText(
                text = if (state.canPrevent) {
                    "왼쪽: 복구 · 오른쪽: 조각"
                } else {
                    "복구권 없음 · 조각 회수"
                },
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            )
            Spacer(Modifier.height(64.dp))
        } else {
            // 계열의 끝에서는 강화 버튼이 영원히 잠긴다. 잠긴 버튼만 두면 무엇을 더
            // 사야 풀리는지 찾아 헤매게 되므로, 버튼 자리를 안내로 바꾼다.
            //
            // **판정과 같은 규칙을 봐야 한다**([LegendForge.canForge]). 여기서 조건을
            // 따로 적었다가 용검이 +20 에서 갇혔다 - 도메인은 굴릴 수 있다는데 화면만
            // "여기가 계열의 끝"을 띄워 버튼이 아예 없었다.
            val atFamilyCap = state.sword?.let { !LegendForge.canForge(it) } == true
            if (atFamilyCap) {
                FamilyCapNotice()
            } else {
                PixelActionButton(
                    onClick = onForge,
                    enabled = state.canForge,
                    feedback = buttonFeedback.value,
                    feedbackColor = buttonFeedbackColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        LText(
                            "강화하기",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF171005),
                        )
                        val helper = state.forgeBlockedReason
                        helper?.let {
                            LText(
                                text = it,
                                fontSize = 10.sp,
                                maxLines = 1,
                                color = if (state.canForge) {
                                    Color(0xFF1C1202).copy(alpha = 0.72f)
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
                                },
                            )
                        }
                    }
                }
            }
            if (state.isRecord) {
                Spacer(Modifier.height(6.dp))
                LText(
                    text = "최고 기록!",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = ForgeAmber,
                )
            }
            Spacer(Modifier.height(4.dp))
            if (state.deepUnlocked) {
                DeepActionStrip(
                    requiredStones = state.requiredStones,
                    attackPower = state.attackPower,
                    huntEnabled = state.huntOpen && !state.busy && state.sword != null,
                    onOpenHunt = onOpenHunt,
                )
                Spacer(Modifier.height(7.dp))
            }
            // 회랑은 사냥터 안으로 갔다. 퀘스트는 v2.1에서 숨겼다 — 다섯 개면 한 줄에 선다.
            val enabled = !state.busy
            ThinRule(Modifier.fillMaxWidth())
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
            ) {
                IconEntry(R.drawable.ui_pixel_shop, "상점", enabled, Modifier.weight(1f), onOpenShop)
                IconEntry(R.drawable.ui_pixel_craft, "조합", enabled, Modifier.weight(1f), onOpenCraft)
                IconEntry(
                    icon = R.drawable.ui_pixel_bag,
                    label = "가방",
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenStorage,
                    badge = "${state.storage.size}/${state.storageCapacity}",
                )
                IconEntry(
                    icon = R.drawable.ui_pixel_training,
                    label = "단련",
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenTraining,
                    // 올릴 돈이 있으면 알려 준다 - 안 그러면 들어가 볼 이유를 잊는다
                    highlight = state.canUpgradeSkill,
                )
                IconEntry(R.drawable.ui_pixel_book, "도감", enabled, Modifier.weight(1f), onOpenCodex)
            }
        }
    }
    }
}

/**
 * 자리비움 보상 알림.
 *
 * 보상은 이미 들어가 있다. 이 창은 "얼마가 들어왔는지" 만 알린다 —
 * 받기를 눌러야 들어오게 하면 창을 놓쳤을 때 보상이 사라진다.
 */
@Composable
private fun IdleRewardDialog(reward: IdleReward, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { LText("자리를 비운 사이", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                LText(
                    // 시즌1에는 가리킬 구역이 없다 - 대장간이 대신 일한 것이다.
                    text = "${IdleRewards.durationText(reward.seconds)} 동안 " +
                        (reward.zone?.let { "${it.displayName}에서" } ?: "대장간에서") +
                        " 벌어 두었다.",
                    fontSize = 14.sp,
                )
                Spacer(Modifier.height(10.dp))
                LText(
                    text = "골드 %,d".format(reward.gold),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFD24A),
                )
                if (reward.stones > 0) {
                    LText(
                        text = "강화석 ${reward.stones}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { LText("확인") }
        },
    )
}

/**
 * 아이콘 + 값 + 이름.
 *
 * 아이콘만으로는 무슨 숫자인지 알 수 없다는 말을 들었다. 이름을 아주 작게 아래 붙인다 —
 * 값은 자주 보고 이름은 한 번만 확인하면 되므로 크기를 다르게 준다.
 */
/**
 * 파괴 직후의 갈림길.
 *
 * 남은 시간이 막대로 줄어드는 동안 눌러야 한다. 바깥을 눌러도 닫히지 않는다 —
 * 검 한 자루가 걸린 자리라 잘못 스친 손가락이 대신 고르면 안 된다.
 * 시간을 넘기면 [ForgeViewModel] 이 알아서 다음 단계로 넘긴다.
 */
@Composable
private fun DestroyDialog(
    progress: Float,
    canPrevent: Boolean,
    /** 부서지고도 검이 남았는지(전설검·조합검). 되살릴 것이 없어 방지권만 잠긴다. */
    survived: Boolean,
    preventTickets: Int,
    legendPrevent: Boolean,
    onPrevent: () -> Unit,
    onSalvage: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
        title = {
            LText(
                text = "검이 부서졌다",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error,
            )
        },
        text = {
            Column {
                LText(
                    text = when {
                        survived && canPrevent -> "전설 방지권으로 원래 단계 복구"
                        survived -> "검 유지 · 복구권 없음 · 조각 회수 가능"
                        canPrevent -> "검 복구 또는 조각 회수"
                        else -> "방지권 없음 · 조각만 회수 가능"
                    },
                    fontSize = 14.sp,
                )
                Spacer(Modifier.height(12.dp))
                PixelProgressBar(
                    progress = progress,
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.height(4.dp))
                LText(
                    text = if (survived) {
                        "시간 초과 시 조각 소멸"
                    } else {
                        "시간 초과 시 복구·조각 기회 소멸"
                    },
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
            }
        },
        // 왼쪽이 되살리기, 오른쪽이 줍기다. 창 하나에서 둘을 한 번에 고른다 —
        // AlertDialog 는 dismiss 를 왼쪽에 놓으므로 그 자리를 방지권이 쓴다.
        dismissButton = {
            TextButton(onClick = onPrevent, enabled = canPrevent) {
                LText(
                    text = if (legendPrevent) {
                        "전설 방지권 $preventTickets"
                    } else {
                        "방지권 $preventTickets"
                    },
                    fontWeight = FontWeight.Bold,
                    color = if (canPrevent) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                    },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onSalvage) {
                LText("조각 줍기", fontWeight = FontWeight.Bold)
            }
        },
    )
}

/**
 * 담금질 게이지.
 *
 * 무한 구간에서만 나온다. 실패가 쌓인 만큼 차오르고, 성공하면 비워진다.
 * **실패가 눈에 보이는 무언가를 남기는 유일한 자리**라 화면에 있어야 한다.
 */
@Composable
private fun TemperBar(temper: TemperUi) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            LText(
                text = "담금질 · 실패 보너스",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = ForgeAmber,
            )
            LText(
                text = if (temper.fails > 0) {
                    "실패 %d회 · %.1f%% → %.1f%%".format(
                        temper.fails,
                        temper.basePercent,
                        temper.currentPercent,
                    )
                } else {
                    "%.1f%%".format(temper.basePercent)
                },
                fontSize = 11.sp,
                fontWeight = if (temper.fails > 0) FontWeight.Bold else FontWeight.Normal,
                color = if (temper.fails > 0) {
                    ForgeAmber
                } else {
                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                },
            )
        }
        Spacer(Modifier.height(4.dp))
        PixelProgressBar(
            progress = temper.ratio,
            modifier = Modifier.fillMaxWidth(),
            color = ForgeAmber,
        )
        Spacer(Modifier.height(4.dp))
        // 0회일 때 "0.5% → 0.5%" 만 보이면 이게 무슨 장치인지 알 수 없다.
        // 다음 한 번의 실패가 무엇을 주는지 늘 적어 둔다.
        LText(
            text = "실패 +%.2f%%p · 최대 %.0f%% · 성공 시 초기화".format(
                temper.gainPerFail,
                temper.maxPercent,
            ),
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
        )
    }
}

// 최근 자취(MarkStrip)는 v2.1에서 삭제했다. 범례까지 붙여 봤지만
// "그래서 뭘 하라는 건지" 가 없었다 - 읽어도 행동이 바뀌지 않는 표시는 장식이다.

@Composable
fun Stat(
    @DrawableRes icon: Int,
    label: String,
    value: String,
    color: Color = Color.Unspecified,
) {
    val resolvedColor = if (color == Color.Unspecified) {
        MaterialTheme.colorScheme.onSurface
    } else {
        color
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        PixelIcon(
            resource = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        LText(
            text = value,
            fontSize = 15.sp,
            lineHeight = 17.sp,
            fontWeight = FontWeight.Black,
            color = resolvedColor,
        )
        LText(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private data class OutcomeDisplay(
    @DrawableRes val icon: Int,
    val label: String,
    val value: String,
    val color: Color = Color.Unspecified,
)

@Composable
private fun CostStat(
    @DrawableRes icon: Int,
    label: String,
    value: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        PixelIcon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Column(Modifier.padding(start = 8.dp)) {
            LText(
                text = label,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LText(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

@Composable
private fun PixelToggle(
    @DrawableRes icon: Int,
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = CutCornerShape(4.dp)
    Row(
        modifier = modifier
            .height(38.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline, shape)
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                else MaterialTheme.colorScheme.surface,
                shape,
            )
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else 0.4f)
            .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PixelIcon(icon, contentDescription = null, modifier = Modifier.size(25.dp))
        LText(
            text = label,
            modifier = Modifier.padding(start = 7.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DeepActionStrip(
    requiredStones: Int,
    attackPower: Long,
    huntEnabled: Boolean,
    onOpenHunt: () -> Unit,
) {
    ForgePanel(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PixelIcon(
                    resource = R.drawable.ui_pixel_stone,
                    contentDescription = null,
                    modifier = Modifier.size(23.dp),
                )
                Spacer(Modifier.width(6.dp))
                LText(
                    text = "강화석 $requiredStones",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(
                Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.outline),
            )
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(enabled = huntEnabled, onClick = onOpenHunt)
                    .alpha(if (huntEnabled) 1f else 0.38f),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PixelIcon(
                    resource = R.drawable.ui_pixel_hunt,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.width(6.dp))
                Column {
                    LText(
                        text = "사냥터",
                        fontSize = 12.sp,
                        lineHeight = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    LText(
                        text = "공격 ${compactGold(attackPower)}",
                        fontSize = 8.sp,
                        lineHeight = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}


/**
 * 아이콘 입구 하나. 큰 아이콘 + 아주 작은 라벨.
 *
 * 글자 버튼이 다섯 개 늘어서면 화면이 문장으로 뒤덮인다. 아이콘이 뜻을 말하고
 * 라벨은 확인용으로만 작게 붙인다.
 */
@Composable
private fun IconEntry(
    @DrawableRes icon: Int,
    label: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    highlight: Boolean = false,
    badge: String? = null,
) {
    Column(
        modifier = modifier
            .height(62.dp)
            .border(0.5.dp, MaterialTheme.colorScheme.outline)
            .background(
                if (highlight) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                } else {
                    MaterialTheme.colorScheme.surface
                },
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 6.dp)
            .alpha(if (enabled) 1f else 0.4f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        PixelIcon(
            resource = icon,
            contentDescription = label,
            modifier = Modifier.size(27.dp),
        )
        LText(
            text = label,
            fontSize = 11.sp,
            lineHeight = 12.sp,
            maxLines = 1,
            color = if (highlight) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            },
        )
        badge?.let {
            LText(
                text = it,
                fontSize = 7.sp,
                lineHeight = 8.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }
    }
}

/**
 * 쌓아 온 보너스가 이번 강화에 얼마나 얹혔는지 — **합계 한 줄만.**
 *
 * 출처별로 다섯 줄을 늘어놓아 봤는데(v2.3) 강화 화면이 표가 됐다. 이 화면은
 * 누르는 곳이지 읽는 곳이 아니다. **출처별 내역은 스킬 화면에 그대로 있다**
 * ([TrainingScreen]) — 거기가 올릴지 말지 정하는 자리이므로 내역이 필요한 곳도 거기다.
 *
 * 0일 때도 지우지 않는다. 흐리게라도 서 있어야 도감·스킬을 올릴 때 이 줄이
 * 자라는 것이 보인다.
 */
@Composable
private fun BonusBreakdown(sources: List<BonusSource>) {
    if (sources.isEmpty()) return
    val success = sources.sumOf { it.bonus.successRate }
    val guard = sources.sumOf { it.bonus.dropGuard }
    val earned = success > 0 || guard > 0

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        LText(
            text = "보너스",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
        )
        LText(
            text = "성공 +%.2f%%p · 하락 방지 +%.2f%%p".format(success * 100, guard * 100),
            fontSize = 11.sp,
            color = if (earned) {
                ForgeGreen
            } else {
                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f)
            },
        )
    }
}

/**
 * 계열의 끝.
 *
 * 강화 버튼을 잠그기만 하면 "골드가 모자란가" 하고 상점을 헤맨다. 여기서 길이
 * 끊긴 것이 아니라 **다른 길로 갈아타는 자리**라는 것을 말해 준다.
 */
@Composable
private fun FamilyCapNotice() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LText(
            text = "계열 강화 완료",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = ForgeAmber,
        )
        Spacer(Modifier.height(2.dp))
        LText(
            text = "다음 단계: 조합소에서 전설검 제작",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
        )
    }
}

/**
 * 별 강화(특수강화) 막대는 「단련」 화면으로 갔다([TrainingScreen]).
 */
@Composable
private fun ResultBanner(result: ForgeResult?) {
    val (text, color) = when (result) {
        is ForgeResult.Success -> "성공 · +${result.newLevel}" to ForgeGreen
        is ForgeResult.Stay -> "실패 · 단계 유지" to Color(0xFFD4C87F)
        // 부서졌지만 사라지지는 않은 검(전설검·조합검)은 그 사실을 말해 준다.
        // "하락… +1" 만 뜨면 갑자기 바닥으로 간 것이 버그로 읽힌다.
        is ForgeResult.Drop -> if (result.shattered) {
            "파괴! +${result.newLevel}로 복귀" to ForgeRed
        } else {
            "하락 · +${result.newLevel}" to Color(0xFFD49A5A)
        }
        is ForgeResult.Destroyed -> "파괴!" to ForgeRed
        null -> "" to Color.Transparent
    }
    LText(text = text, color = color, fontSize = 18.sp, fontWeight = FontWeight.Bold)
}

/**
 * 시스템 애니메이션 배율을 쓰지 않고 실제 프레임 시간으로 좌우로 흔든다.
 *
 * 이 방식이면 애니메이션 배율이 0인 S20 Ultra와 1인 A16이 같은 시간 동안 연출한다.
 */
private suspend fun Animatable<Float, AnimationVector1D>.shakeOnceRealtime(
    amplitude: Float,
    durationMillis: Int,
) {
    val points = floatArrayOf(
        0f,
        amplitude,
        -amplitude,
        amplitude * 0.6f,
        -amplitude * 0.35f,
        amplitude * 0.15f,
        0f,
    )
    snapTo(0f)
    playRealtime(durationMillis) { progress ->
        val scaled = progress * (points.size - 1)
        val segment = scaled.toInt().coerceAtMost(points.size - 2)
        val local = scaled - segment
        snapTo(points[segment] + (points[segment + 1] - points[segment]) * local)
    }
    snapTo(0f)
}

/** 강화 버튼을 즉시 밝힌 뒤 짧게 가라앉힌다. 입력 가능 여부와는 완전히 독립적이다. */
private suspend fun Animatable<Float, AnimationVector1D>.buttonFeedbackOnceRealtime(
    durationMillis: Int,
) {
    snapTo(1f)
    playRealtime(durationMillis) { progress -> snapTo(1f - progress) }
    snapTo(0f)
}

/** Compose 애니메이션 배율 대신 모노토닉 프레임 시간으로 진행률을 계산한다. */
private suspend fun playRealtime(durationMillis: Int, update: suspend (Float) -> Unit) {
    require(durationMillis > 0)
    val startNanos = withFrameNanos { it }
    update(0f)
    while (true) {
        val frameNanos = withFrameNanos { it }
        val progress = ((frameNanos - startNanos) / 1_000_000f / durationMillis)
            .coerceIn(0f, 1f)
        update(progress)
        if (progress >= 1f) return
    }
}

/**
 * 현재 화면이 생기기 전에 이미 끝난 강화 결과는 소비한 것으로 본다.
 * 그래야 상점·가방에서 돌아왔을 때 버튼이 자동으로 눌린 것처럼 빛나지 않는다.
 */
internal class ForgeEffectGate(initialSequence: Long) {
    private var handledSequence = initialSequence

    fun consume(sequence: Long): Boolean {
        if (sequence == handledSequence) return false
        handledSequence = sequence
        return true
    }
}
