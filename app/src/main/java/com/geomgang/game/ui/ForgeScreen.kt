package com.geomgang.game.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.geomgang.core.BonusSource
import com.geomgang.core.Difficulty
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
import com.geomgang.game.TemperUi

/**
 * 결과별 연출 길이(ms).
 *
 * 결과가 몸에 남을 만큼만, 진행을 막지 않을 만큼 짧게. 이 값이 커지면
 * 연타로 굴리는 맛이 사라지고 게임이 답답해진다.
 */
private const val SUCCESS_MILLIS = 350
private const val STAY_MILLIS = 250
private const val DROP_MILLIS = 400
private const val DESTROY_MILLIS = 300

/** 흔들림 진폭(px). 하락이 유지보다 크게 흔들려야 손해를 체감한다. */
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
    onAnimationEnd: () -> Unit,
) {
    val shake = remember { Animatable(0f) }
    val flash = remember { Animatable(0f) }
    var flashColor by remember { mutableStateOf(Color.White) }

    // 파괴는 사용자의 응답을 기다려야 하므로 잠금을 자동으로 풀지 않는다.
    LaunchedEffect(state.lastResult) {
        when (val result = state.lastResult) {
            null -> {
                shake.snapTo(0f)
                flash.snapTo(0f)
            }

            is ForgeResult.Success -> {
                flashColor = Color(0xFFFFF3D0)
                flash.flashOnce(SUCCESS_MILLIS)
                onAnimationEnd()
            }

            is ForgeResult.Stay -> {
                shake.shakeOnce(STAY_SHAKE, STAY_MILLIS)
                onAnimationEnd()
            }

            is ForgeResult.Drop -> {
                // 부서졌다가 바닥으로 살아 돌아온 것은 평범한 하락과 다른 사건이다.
                // 같은 흔들림으로 지나가면 +14 가 +1 이 된 것이 버그로 보인다.
                if (result.shattered) {
                    flashColor = Color(0xFFE05A5A)
                    flash.flashOnce(DESTROY_MILLIS)
                }
                shake.shakeOnce(DROP_SHAKE, DROP_MILLIS)
                onAnimationEnd()
            }

            is ForgeResult.Destroyed -> {
                flashColor = Color(0xFFE05A5A)
                flash.flashOnce(DESTROY_MILLIS)
                // 여기서 onAnimationEnd 를 부르지 않는다.
                // 제한 시간 창이 열려 있고, 그 잠금은 ViewModel 이 푼다.
            }
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
            preventTickets = state.preventTickets,
            onPrevent = onPrevent,
            onSalvage = onSalvage,
        )

        DestroyPhase.None -> Unit
    }

    // 세로로 스크롤된다. 이 화면은 판이 갈수록 줄이 늘어나는데(요구량·스킬·재료·별·
    // 회랑·아이콘) 고정 높이로 두면 짧은 화면이나 큰 글꼴에서 아래가 잘려 나간다.
    // v1.3에서 실제로 검 이름이 그렇게 사라졌다.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.width(64.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "최고 +${state.bestLevel}",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    fontSize = 13.sp,
                )
                state.progress.selectedTitle?.let {
                    Text(
                        text = it.title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            TextButton(onClick = onOpenMenu, enabled = !state.busy) {
                Text("🏅", fontSize = 22.sp)
            }
        }

        // 재화는 **맨 위**에 둔다. 검 그림 아래에 두었더니 화면이 짧은 기기에서는
        // 스크롤해야 보여서 "몇 개 있는지 모르겠다" 는 말이 나왔다.
        Spacer(Modifier.height(4.dp))
        WalletBar(state.wallet())
        Spacer(Modifier.height(6.dp))

        // 그림 영역. 스크롤되는 열 안에서는 weight 를 쓸 수 없으므로(높이가 무한대다)
        // 최소 높이로 자리를 잡는다. 이름·강화 단계는 이 밖에 둔다 — 안에 넣으면
        // 가운데 정렬 탓에 위아래로 잘린다.
        Box(
            modifier = Modifier
                .heightIn(min = 170.dp)
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

                // 표시 크기는 고정이다. 가변으로 두면 주변 UI(별 강화 바 등)가 나타날
                // 때마다 검이 커졌다 작아졌다 해서 들쭉날쭉해 보인다.
                DestroyPhase.None -> SwordView(
                    sword = state.sword,
                    modifier = Modifier.size(140.dp),
                    shake = shake.value,
                    flash = flash.value,
                    flashColor = flashColor,
                )
            }
        }

        // 이름·강화 단계는 가변 영역 밖이라 어떤 화면에서도 잘리지 않는다.
        Text(
            text = when (state.destroyPhase) {
                is DestroyPhase.Choice -> "지금 골라야 한다"
                // 이름은 단계마다 다르다. 계열은 형태만 정하고 부제로 내려간다.
                // 고유검은 고유 이름을 금색으로.
                DestroyPhase.None -> state.sword?.let {
                    SwordNames.nameFor(it)
                } ?: "검이 없다"
            },
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = if (state.destroyPhase == DestroyPhase.None &&
                state.sword?.uniqueId != null
            ) {
                Color(0xFFFFD54A)
            } else {
                Color.Unspecified
            },
        )
        if (state.destroyPhase == DestroyPhase.None && state.sword != null) {
            Text(
                text = "+${state.sword.level} · ${state.sword.familyLabel}" +
                    if (state.sword.stars > 0) "  ${"★".repeat(state.sword.stars)}" else "",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
            )
            // 스킬은 +15부터 열린다. 강화 단계를 올릴 이유를 하나 더 보여 준다.
            val skill = com.geomgang.core.Skills.of(state.sword.family)
            val unlocked = com.geomgang.core.Skills.unlocked(state.sword)
            Text(
                text = if (unlocked) {
                    "⚡ ${skill.name} — ${skill.blurb}"
                } else {
                    "🔒 ${skill.name} — +${com.geomgang.core.Skills.MIN_LEVEL}부터"
                },
                fontSize = 11.sp,
                color = if (unlocked) {
                    Color(0xFF7FE8FF)
                } else {
                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                },
            )
        }
        Spacer(Modifier.height(4.dp))
        ResultBanner(state.lastResult)
        Spacer(Modifier.height(6.dp))

        // 재화(골드·조각·강화석·방지권)는 맨 위 지갑 줄이 이미 보여 준다.
        // 여기 남는 것은 **이번 강화의 정보** 뿐이다 - 성공률·비용·판매가.
        if (state.sword != null) {
            // 이번 한 번이 어떻게 끝날 수 있는지. 성공률만 보여 주던 탓에
            // 무한 구간에서 **실패가 곧 파괴**라는 걸 모르고 누르게 됐다.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Stat("🎯", "성공", "${state.odds.success}%", MaterialTheme.colorScheme.primary)
                if (state.odds.stay > 0) {
                    Stat("＝", "유지", "${state.odds.stay}%")
                }
                if (state.odds.drop > 0) {
                    Stat("↓", "하락", "${state.odds.drop}%", Color(0xFFE0A060))
                }
                if (state.odds.destroy > 0) {
                    Stat("💥", "파괴", "${state.odds.destroy}%", MaterialTheme.colorScheme.error)
                }
            }
            // 부서지지 않는 검은 그 사실을 **미리** 말한다. 결과창에서 처음 알면
            // 갑자기 바닥으로 떨어진 것이 버그로 보인다.
            state.shatterFloor?.let { floor ->
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "🛡 부서져도 사라지지 않는다 — 대신 +$floor 로 되돌아간다",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFC79BFF),
                )
            }
            state.temper?.let { temper ->
                Spacer(Modifier.height(8.dp))
                TemperBar(temper)
            }
            if (state.bonusSources.any { it.bonus.successRate > 0 || it.bonus.dropGuard > 0 }) {
                Spacer(Modifier.height(8.dp))
                BonusBreakdown(state.bonusSources)
            }
            // 각인은 지녔을 때만 말한다. 없을 때 자리를 잡아 두면 늘 빈 줄이다.
            if (state.wardCharm && state.sword?.isLegend() == true) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "🛡 수호 각인 — 미끄러져도 한 단계만 잃는다",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFC79BFF),
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Stat("🔥", "강화 비용", compactGold(state.upgradeCost))
                Stat("🏷", "판매가", compactGold(state.sellPrice))
            }

            if (!state.awaitingDestroyChoice) {
                Spacer(Modifier.height(8.dp))
                // 배타는 v2.1에서 풀렸다 - 함께 켤 수 있고, 값(골드)이 선택을 가른다.
                Text(
                    text = "둘 다 함께 켤 수 있다 · 켠 것은 이번 강화에 쓰인다",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                )
                Spacer(Modifier.height(4.dp))
                // 아이템은 한 번 쓰면 토글이 내려간다. 매번 다시 켜야 한다.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = state.useBlessing,
                        onClick = onToggleBlessing,
                        enabled = state.blessingScrolls > 0 && !state.busy,
                        label = {
                            Text("📜 축복서 ${state.blessingScrolls}", fontSize = 12.sp)
                        },
                        modifier = Modifier.weight(1f),
                    )
                    FilterChip(
                        selected = state.useLuckCharm,
                        onClick = onToggleLuckCharm,
                        enabled = state.luckCharms > 0 && !state.busy,
                        label = {
                            Text("🍀 행운부적 ${state.luckCharms}", fontSize = 12.sp)
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // 창이 열려 있으면 하단 버튼을 감춘다. 원과 파편을 눌러야 하기 때문이다.
        if (state.awaitingDestroyChoice) {
            Text(
                text = if (state.canPrevent) {
                    "왼쪽 원은 되살리기, 오른쪽 파편은 조각"
                } else {
                    "방지권이 없다 — 파편을 눌러 조각을 회수한다"
                },
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            )
            Spacer(Modifier.height(64.dp))
        } else {
            // 고단계는 골드가 아니라 재료가 화폐다. 무엇이 드는지 버튼 위에 먼저 알린다.
            if (state.requiredStones > 0) {
                Text(
                    text = "이번 강화에 들어가는 것  ·  🪨 강화석 ${state.requiredStones}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (state.canForge) {
                        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                )
                Spacer(Modifier.height(4.dp))
            }
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
                Button(
                    onClick = onForge,
                    enabled = state.canForge,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                ) {
                    Text("강 화", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
                state.forgeBlockedReason?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                }
            }
            if (state.isRecord) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "★ 최고 기록!",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFD54A),
                )
            }
            // 스킬과 특수강화는 「단련」 화면으로 옮겼다([TrainingScreen]).
            // 성장 장치를 여기 늘어놓으면 정작 강화 버튼이 아래로 밀린다.
            Spacer(Modifier.height(10.dp))
            // 사냥터는 **용검을 손에 쥔 뒤에** 열린다([com.geomgang.core.Unlocks]).
            // 초반에 사냥이 골드를 벌어다 주면 검을 팔 이유가 사라지고,
            // 그러면 이 게임의 심장인 "강화해서 팔고 또 강화한다" 가 통째로 묻힌다.
            if (state.huntOpen) {
                Button(
                    onClick = onOpenHunt,
                    enabled = !state.busy && state.sword != null,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = Color(0xFF10222E),
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                ) {
                    Text(
                        text = "⚔ 사냥터  %,d".format(state.attackPower),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            // 잠긴 동안에는 **아무것도 두지 않는다.** 자물쇠 한 줄이 초반 내내 붙어 있으면
            // 아직 손도 못 댈 것을 계속 들여다보게 된다. 열릴 때 나타나면 그걸로 족하다.
            Spacer(Modifier.height(10.dp))
            // 회랑은 사냥터 안으로 갔다. 퀘스트는 v2.1에서 숨겼다 — 다섯 개면 한 줄에 선다.
            val enabled = !state.busy
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                IconEntry("🛒", "상점", enabled, Modifier.weight(1f), onOpenShop)
                IconEntry("⚗️", "조합", enabled, Modifier.weight(1f), onOpenCraft)
                IconEntry(
                    icon = "🎒",
                    label = "${state.storage.size}/${state.storageCapacity}",
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenStorage,
                )
                IconEntry(
                    icon = "⚒",
                    label = "단련",
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenTraining,
                    // 올릴 돈이 있으면 알려 준다 - 안 그러면 들어가 볼 이유를 잊는다
                    highlight = state.canUpgradeSkill,
                )
                IconEntry("📖", "도감", enabled, Modifier.weight(1f), onOpenCodex)
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
        title = { Text("자리를 비운 사이", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    // 시즌1에는 가리킬 구역이 없다 - 대장간이 대신 일한 것이다.
                    text = "${IdleRewards.durationText(reward.seconds)} 동안 " +
                        (reward.zone?.let { "${it.displayName}에서" } ?: "대장간에서") +
                        " 벌어 두었다.",
                    fontSize = 14.sp,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "💰 %,d".format(reward.gold),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFD24A),
                )
                if (reward.stones > 0) {
                    Text(
                        text = "🪨 강화석 ${reward.stones}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("확인") }
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
            Text(
                text = "검이 부서졌다",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error,
            )
        },
        text = {
            Column {
                Text(
                    text = when {
                        survived -> "검은 사라지지 않았다. 떨어져 나간 조각을 주울 수 있다."
                        canPrevent -> "되살릴 것인가, 조각이라도 챙길 것인가."
                        else -> "방지권이 없다. 지금 주우면 조각이라도 남는다."
                    },
                    fontSize = 14.sp,
                )
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (survived) {
                        "시간을 넘기면 조각이 사라진다"
                    } else {
                        "시간을 넘기면 아무것도 남지 않는다"
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
                Text(
                    text = "🛡 방지권 $preventTickets",
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
                Text("💎 파편 줍기", fontWeight = FontWeight.Bold)
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
            Text(
                text = "담금질 — 실패가 다음 확률을 올린다",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE0A458),
            )
            Text(
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
                    Color(0xFFE0A458)
                } else {
                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                },
            )
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { temper.ratio },
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFFE0A458),
        )
        Spacer(Modifier.height(4.dp))
        // 0회일 때 "0.5% → 0.5%" 만 보이면 이게 무슨 장치인지 알 수 없다.
        // 다음 한 번의 실패가 무엇을 주는지 늘 적어 둔다.
        Text(
            text = "실패할 때마다 +%.2f%%p · 최대 %.0f%% · 성공하면 처음으로".format(
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
fun Stat(icon: String, label: String, value: String, color: Color = Color.Unspecified) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(icon, fontSize = 15.sp)
            Spacer(Modifier.width(4.dp))
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = color,
            )
        }
        Text(
            text = label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
        )
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
    icon: String,
    label: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    highlight: Boolean = false,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (highlight) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                } else {
                    MaterialTheme.colorScheme.surface
                },
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 8.dp)
            .alpha(if (enabled) 1f else 0.4f),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = icon, fontSize = 22.sp)
        Text(
            text = label,
            fontSize = 10.sp,
            maxLines = 1,
            color = if (highlight) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            },
        )
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
        Text(
            text = "쌓은 보너스",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
        )
        Text(
            text = "성공 +%.2f%%p  ·  하락방지 +%.2f%%p".format(success * 100, guard * 100),
            fontSize = 11.sp,
            color = if (earned) {
                Color(0xFF7FD48A)
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
        Text(
            text = "여기가 계열의 끝",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFE0A458),
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "+${LegendForge.MATERIAL_LEVEL} 위는 강화로 가지 않는다. " +
                "조합소에서 전설검으로 조합한다",
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
        is ForgeResult.Success -> "성공!  +${result.newLevel}" to Color(0xFF7FD48A)
        is ForgeResult.Stay -> "실패 — 단계 유지" to Color(0xFFD4C87F)
        // 부서졌지만 사라지지는 않은 검(전설검·조합검)은 그 사실을 말해 준다.
        // "하락… +1" 만 뜨면 갑자기 바닥으로 간 것이 버그로 읽힌다.
        is ForgeResult.Drop -> if (result.shattered) {
            "부서졌다!  +${result.newLevel} 로 되돌아갔다" to Color(0xFFE05A5A)
        } else {
            "하락…  +${result.newLevel}" to Color(0xFFD49A5A)
        }
        is ForgeResult.Destroyed -> "파괴!!" to Color(0xFFE05A5A)
        null -> "" to Color.Transparent
    }
    Text(text = text, color = color, fontSize = 18.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
        Text(value, fontWeight = FontWeight.Medium)
    }
}

/** 좌우로 몇 번 흔들리다 제자리로 돌아온다. */
private suspend fun Animatable<Float, *>.shakeOnce(amplitude: Float, durationMillis: Int) {
    snapTo(0f)
    animateTo(
        targetValue = 0f,
        animationSpec = keyframes {
            this.durationMillis = durationMillis
            0f at 0
            amplitude at durationMillis / 6
            -amplitude at durationMillis * 2 / 6
            amplitude * 0.6f at durationMillis * 3 / 6
            -amplitude * 0.35f at durationMillis * 4 / 6
            amplitude * 0.15f at durationMillis * 5 / 6
            0f at durationMillis
        },
    )
}

/** 확 밝아졌다가 가라앉는다. */
private suspend fun Animatable<Float, *>.flashOnce(durationMillis: Int) {
    snapTo(0f)
    animateTo(
        targetValue = 0f,
        animationSpec = keyframes {
            this.durationMillis = durationMillis
            0f at 0
            1f at durationMillis / 5
            0f at durationMillis
        },
    )
}

private fun Difficulty.displayLabel(): String = when (this) {
    Difficulty.EASY -> "쉬움"
    Difficulty.NORMAL -> "일반"
    Difficulty.HARD -> "지옥"
    Difficulty.ENDLESS -> "무한"
}
