package com.geomgang.game.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geomgang.core.Achievement
import com.geomgang.core.Progress
import com.geomgang.core.ProgressState
import com.geomgang.core.WeaponCatalog

/** 도감·업적·통계·설정으로 가는 갈림길. 강화 화면을 버튼으로 채우지 않으려고 한 단계 둔다. */
@Composable
fun RecordsMenuScreen(
    progress: ProgressState,
    ownedPets: Int,
    onOpenCodex: () -> Unit,
    onOpenPets: () -> Unit,
    onOpenAchievements: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenHelp: () -> Unit,
    onOpenSettings: () -> Unit,
    onBack: () -> Unit,
) {
    // 도감 화면([CodexScreen])과 **같은 셈법**이어야 한다.
    // 예전에는 여기만 `계열 to 티어` 로 묶어 세고 있었다 — 티어가 곧 칸이던 시절의
    // 공식이라, 칸이 단계별로 쪼개진 뒤로는 40칸이 4로 뭉개져 보였다.
    val codexCount = Progress.entriesOf(progress).size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
    ) {
        ScreenHeader(title = "기록", onBack = onBack)

        MenuRow(
            icon = "📖",
            title = "도감",
            subtitle = "$codexCount / ${WeaponCatalog.ENTRIES.size}",
            onClick = onOpenCodex,
        )
        MenuRow(
            icon = "🐾",
            title = "펫",
            subtitle = "$ownedPets / ${com.geomgang.core.PetKind.entries.size}",
            onClick = onOpenPets,
        )
        MenuRow(
            icon = "🏆",
            title = "업적 · 칭호",
            subtitle = "${progress.achievements.size} / ${Achievement.entries.size}",
            onClick = onOpenAchievements,
        )
        MenuRow(
            icon = "📊",
            title = "통계",
            subtitle = "확률 비교",
            onClick = onOpenStats,
        )
        MenuRow(
            icon = "❓",
            title = "도움말",
            subtitle = "재료·계열·조합·스킬 규칙",
            onClick = onOpenHelp,
        )
        MenuRow(
            icon = "⚙️",
            title = "설정",
            subtitle = "소리 · 라이선스",
            onClick = onOpenSettings,
        )
    }
}

@Composable
private fun MenuRow(icon: String, title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(icon, fontSize = 26.sp, modifier = Modifier.padding(end = 12.dp))
                Column {
                    Text(title, fontWeight = FontWeight.Bold)
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
            }
            Text("›", fontSize = 20.sp, color = MaterialTheme.colorScheme.secondary)
        }
    }
    Spacer(Modifier.height(0.dp))
}
