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
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geomgang.core.Settings
import com.geomgang.core.Timing

/**
 * 설정.
 *
 * 효과음 토글은 넣지 않는다 — 아직 소리가 없다. 없는 기능의 스위치를 두지 않는다.
 * 데이터 초기화는 모드 선택 화면의 5초 길게 누르기에 있다. 두 군데 두지 않는다.
 */
@Composable
fun SettingsScreen(
    settings: Settings,
    onAutoPreventChange: (Boolean) -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        ScreenHeader(title = "설정", onBack = onBack)

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("방지권 자동 사용", fontWeight = FontWeight.Medium)
                        Text(
                            text = "파괴되면 ${Timing.PREVENT_WINDOW_MILLIS / 1000.0}초 창을 " +
                                "열지 않고 바로 방지권을 쓴다",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                    }
                    Switch(checked = settings.autoPrevent, onCheckedChange = onAutoPreventChange)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "기본값은 꺼짐이다. 파괴 순간 손이 먼저 나가는 그 긴장이 " +
                        "이 게임의 핵심이라, 편의를 원하는 사람만 켜게 두었다.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("데이터 초기화", fontWeight = FontWeight.Medium)
                Text(
                    text = "모드 선택 화면에서 해당 모드를 5초 길게 눌러 초기화한다. " +
                        "도감과 업적은 지워지지 않는다.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("리소스 라이선스", fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "이 앱의 검 그림은 전부 코드로 그린다. 외부 이미지 파일을 쓰지 않는다.\n\n" +
                        "타사 게임의 그래픽·사운드를 사용하지 않았으며, 변형해 사용하지도 않았다. " +
                        "곡선날·가시·비늘·십자 코등이 같은 요소는 판타지 무기의 일반적 표현 관습이다.\n\n" +
                        "외부 에셋을 추가하게 되면 그 출처와 라이선스를 이 화면에 표기한다.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
        }
    }
}
