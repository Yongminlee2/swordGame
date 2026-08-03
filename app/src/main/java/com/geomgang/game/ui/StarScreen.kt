package com.geomgang.game.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geomgang.core.SwordNames
import com.geomgang.game.ForgeUiState

/**
 * 특수강화(별) 전용 화면.
 *
 * 강화 화면 한가운데 있던 것을 떼어 냈다. 메인은 **한 번의 강화**에 집중해야 하는데
 * 성격이 다른 두 번째 강화가 같은 자리에 있으면 눈이 갈라진다.
 *
 * 규칙 자체가 다르다는 것도 여기서 분명해진다 — 별은 **실패해도 검이 부서지지 않는다.**
 * 별 하나를 잃을 뿐이다.
 */
@Composable
fun StarScreen(
    state: ForgeUiState,
    onStarUp: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ScreenHeader(title = "특수강화", onBack = onBack, wallet = state.wallet())

        val sword = state.sword
        val star = state.star
        if (sword == null || star == null) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("아직 별을 붙일 수 없다", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (sword == null) {
                            "검이 없다."
                        } else {
                            "용검(+${com.geomgang.core.StarForce.MIN_LEVEL})부터 " +
                                "별을 붙일 수 있다. 계열 검은 +20에서 조합 재료가 되므로 " +
                                "별을 붙여도 곧 사라진다."
                        },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
            }
            return@Column
        }

        SwordView(sword = sword, modifier = Modifier.size(140.dp))
        Text(SwordNames.nameFor(sword), fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(
            text = "+${sword.level}",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
        )

        Spacer(Modifier.height(14.dp))
        Text(
            text = "★".repeat(star.stars) + "☆".repeat(star.maxStars - star.stars),
            fontSize = 26.sp,
            color = Color(0xFFFFD24A),
        )
        if (star.attackBonusPercent > 0) {
            Text(
                text = "공격력 +${star.attackBonusPercent}%",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        star.lastUp?.let {
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (it) "별이 하나 올랐다" else "실패 — 별 하나를 잃었다 (검은 무사하다)",
                fontSize = 13.sp,
                color = if (it) Color(0xFF7FD48A) else Color(0xFFE0906A),
            )
        }

        Spacer(Modifier.height(16.dp))

        if (star.stars >= star.maxStars) {
            Text(
                "더 올릴 별이 없다",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Stat("🎯", "성공", "${star.successPercent}%", MaterialTheme.colorScheme.primary)
                Stat("💎", "조각", "${star.shardCost}")
                Stat("💰", "골드", compactGold(star.goldCost))
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onStarUp,
                enabled = !state.busy && star.affordable,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                Text("별 올리기", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            if (!star.affordable) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "조각이나 골드가 모자라다",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        Text(
            text = "별은 강화와 별개의 계층이다. 실패해도 검은 부서지지 않고 별 하나만 잃는다.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
        )
    }
}
