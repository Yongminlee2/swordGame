package com.geomgang.game.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geomgang.core.BonusSource
import com.geomgang.core.Smithy
import com.geomgang.game.ForgeUiState

/**
 * 단련 — 오래 한 것이 확률이 되는 자리.
 *
 * 스킬과 특수강화가 강화 화면에 버튼으로 붙어 있었다. 그 화면은 **한 번의 강화**에
 * 집중해야 하는데 성격이 다른 성장 장치가 같은 자리에서 눈을 갈라 놓았고,
 * 줄이 늘어날수록 정작 강화 버튼이 아래로 밀렸다.
 *
 * 성장은 자주 만지지 않는다. 한 화면으로 모아 들어와서 하게 한다.
 */
@Composable
fun TrainingScreen(
    state: ForgeUiState,
    onUpgradeSkill: () -> Unit,
    onOpenStar: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        ScreenHeader(title = "단련", onBack = onBack, wallet = state.wallet())
        Spacer(Modifier.height(12.dp))

        SkillCard(state, onUpgradeSkill)
        Spacer(Modifier.height(12.dp))
        StarCard(state, onOpenStar)
        Spacer(Modifier.height(12.dp))
        BonusCard(state.bonusSources)
    }
}

/** 골드로 사는 영구 확률. 후반에 쌓이기만 하던 골드가 여기 묶인다. */
@Composable
private fun SkillCard(state: ForgeUiState, onUpgrade: () -> Unit) {
    val maxed = state.skillLevel >= Smithy.MAX_LEVEL
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("스킬", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = "Lv ${state.skillLevel} / ${Smithy.MAX_LEVEL}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { state.skillLevel.toFloat() / Smithy.MAX_LEVEL },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            // 지금 몇 % 를 받고 있는지 먼저 말한다. 다음 한 칸의 값어치가 거기서 읽힌다.
            Text(
                text = "지금  성공률 +%.2f%%p  ·  파괴방지 +%.2f%%p".format(
                    state.skillLevel * Smithy.PER_LEVEL * 100,
                    state.skillLevel * Smithy.PER_LEVEL * 100,
                ),
                fontSize = 12.sp,
                color = Color(0xFF7FD48A),
            )
            Text(
                text = "한 칸 올릴 때마다 둘 다 +%.2f%%p".format(Smithy.PER_LEVEL * 100),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
            )
            Spacer(Modifier.height(10.dp))
            if (maxed) {
                Text(
                    text = "더 올릴 수 없다",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
            } else {
                Button(
                    onClick = onUpgrade,
                    enabled = state.canUpgradeSkill,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("💰 ${compactGold(state.skillPrice)} 로 올리기")
                }
                if (!state.canUpgradeSkill) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "골드가 모자라다",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

/** 별 강화. 여기서는 지금 상태만 보여 주고 실제 강화는 자기 화면에서 한다. */
@Composable
private fun StarCard(state: ForgeUiState, onOpenStar: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Text("특수강화", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            val star = state.star
            if (star == null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "검이 조건을 갖추면 별을 붙일 수 있다",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                )
                return@Column
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "★".repeat(star.stars) + "☆".repeat(star.maxStars - star.stars),
                fontSize = 18.sp,
                color = Color(0xFFFFD24A),
            )
            Text(
                text = if (star.attackBonusPercent > 0) {
                    "공격력 +${star.attackBonusPercent}%  ·  강화 확률은 건드리지 않는다"
                } else {
                    "별은 공격력만 올린다. 강화 확률과는 무관하다"
                },
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = onOpenStar, modifier = Modifier.fillMaxWidth()) {
                Text("별 강화 하러 가기")
            }
        }
    }
}

/**
 * 확률이 어디서 왔는지.
 *
 * 강화 화면에도 같은 내역이 있지만 거기는 **지금 굴림에 붙는 값**만 짧게 보여 준다.
 * 여기는 채운 칸과 못 채운 칸을 함께 두어 "다음에 무엇을 올릴지"를 고르는 자리다.
 */
@Composable
private fun BonusCard(sources: List<BonusSource>) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Text("강화 확률 내역", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            sources.forEach { source ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(source.label, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text(
                            text = source.detail,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "성공 +%.2f%%p".format(source.bonus.successRate * 100),
                            fontSize = 12.sp,
                            color = if (source.bonus.successRate > 0) {
                                Color(0xFF7FD48A)
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                            },
                        )
                        Text(
                            text = "방지 +%.2f%%p".format(source.bonus.destroyGuard * 100),
                            fontSize = 12.sp,
                            color = if (source.bonus.destroyGuard > 0) {
                                Color(0xFF7FA8D4)
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                            },
                        )
                    }
                }
            }
        }
    }
}
