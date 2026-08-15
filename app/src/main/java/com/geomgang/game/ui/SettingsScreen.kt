package com.geomgang.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geomgang.core.SaveStore
import com.geomgang.core.Settings
import com.geomgang.core.Timing

/**
 * 설정.
 *
 * 데이터 초기화는 이 화면 아래쪽에 있다. 두 군데 두지 않는다.
 */
@Composable
fun SettingsScreen(
    settings: Settings,
    deepUnlocked: Boolean,
    onAutoPreventChange: (Boolean) -> Unit,
    onSoundChange: (Boolean) -> Unit,
    onMusicChange: (Boolean) -> Unit,
    onHapticsChange: (Boolean) -> Unit,
    backupBusy: Boolean,
    backupMessage: String?,
    backupError: Boolean,
    onRequestExportBackup: () -> Unit,
    onRequestImportBackup: () -> Unit,
    onReset: () -> Unit,
    onBack: () -> Unit,
) {
    ScrollableForgeScreen(title = "설정", onBack = onBack) {
        ForgePanel(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("효과음", fontWeight = FontWeight.Medium)
                        Text(
                            text = if (deepUnlocked) {
                                "강화 결과·복구·조각·사냥 타격 효과음. " +
                                    "단계가 높을수록 성공음도 높아진다."
                            } else {
                                "강화 결과·복구·조각 효과음. " +
                                    "단계가 높을수록 성공음도 높아진다."
                            },
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                    }
                    PixelToggle(checked = settings.soundOn, onCheckedChange = onSoundChange)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        ForgePanel(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("배경음", fontWeight = FontWeight.Medium)
                        Text(
                            text = if (deepUnlocked) {
                                "대장간과 사냥터의 분위기에 맞춰 음악이 바뀐다."
                            } else {
                                "불씨 대장간의 8비트 배경 음악."
                            },
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                    }
                    PixelToggle(checked = settings.musicOn, onCheckedChange = onMusicChange)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        ForgePanel(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("진동", fontWeight = FontWeight.Medium)
                        Text(
                            text = if (deepUnlocked) {
                                "강화 결과마다 다른 진동. 사냥 연타에는 진동이 없다."
                            } else {
                                "강화 결과마다 다른 진동으로 성공과 실패를 구분한다."
                            },
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                    }
                    PixelToggle(checked = settings.hapticsOn, onCheckedChange = onHapticsChange)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        ForgePanel(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("방지권 자동 사용", fontWeight = FontWeight.Medium)
                        Text(
                            text = "파괴 직후 사용 가능한 방지권을 즉시 쓴다",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                    }
                    PixelToggle(checked = settings.autoPrevent, onCheckedChange = onAutoPreventChange)
                }
                Spacer(Modifier.height(6.dp))
            }
        }

        Spacer(Modifier.height(12.dp))

        ForgePanel(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("기기 이전 백업", fontWeight = FontWeight.Medium)
                Text(
                    text = "모든 진행과 설정을 비밀번호로 암호화해 파일로 옮긴다. " +
                        "새 기기에서도 같은 비밀번호가 필요하다.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    BackupActionButton(
                        onClick = onRequestExportBackup,
                        enabled = !backupBusy,
                        filled = true,
                        modifier = Modifier.weight(1f),
                        label = "내보내기",
                    )
                    BackupActionButton(
                        onClick = onRequestImportBackup,
                        enabled = !backupBusy,
                        filled = false,
                        modifier = Modifier.weight(1f),
                        label = "가져오기",
                    )
                }
                Text(
                    text = when {
                        backupBusy -> "백업을 안전하게 처리하는 중..."
                        backupMessage != null -> backupMessage
                        else -> "비밀번호를 잊으면 백업을 복원할 수 없다."
                    },
                    modifier = Modifier.padding(top = 8.dp),
                    fontSize = 11.sp,
                    color = when {
                        backupError -> MaterialTheme.colorScheme.error
                        backupMessage != null -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.52f)
                    },
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        ForgePanel(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("진행 초기화", fontWeight = FontWeight.Medium)
                Text(
                    text = if (deepUnlocked) {
                        "현재 판의 검·재화·아이템·사냥 진행 삭제. " +
                            "도감·업적·통계·설정은 유지된다."
                    } else {
                        "현재 판의 검·재화·아이템 삭제. " +
                            "도감·업적·통계·설정은 유지된다."
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
                Spacer(Modifier.height(10.dp))
                HoldToReset(label = "5초 길게 눌러 초기화", onComplete = onReset)
            }
        }

        Spacer(Modifier.height(12.dp))

        ForgePanel(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("리소스 라이선스", fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "프로젝트 전용 일러스트",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "강화검 7계열 +0~+20 · 전설 +21~+50\n" +
                        "사냥 몬스터 24구역 144종 · 전투 효과 16종\n" +
                        "대장간 배경과 UI 픽셀 아이콘",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "CC0 보조 스프라이트",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    // CC0 은 표기 의무가 없지만 출처를 남기는 것이 이 프로젝트의 방침이다.
                    text = "Dungeon Crawl 32x32 tiles · CC0\n" +
                        "제작: Dungeon Crawl Stone Soup 팀 외 다수\n" +
                        "https://opengameart.org/content/dungeon-crawl-32x32-tiles\n\n" +
                        "고유검과 펫 등 일부 보조 스프라이트에 사용.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "사운드와 기타",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "배경음 · CC0\n" +
                        "On The Offensive — Ted Kerr (Wolfgang_)\n" +
                        "https://opengameart.org/content/8-bit-theme-on-the-offensive\n\n" +
                        "Battle Theme A — cynicmusic\n" +
                        "https://opengameart.org/content/battle-theme-a\n\n" +
                        "효과음은 앱에서 실시간 합성한다.\n" +
                        "타사 상용 게임의 그래픽·사운드는 사용하지 않았다.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
        }
    }

}

enum class BackupDialogMode { Export, Import }

@Composable
private fun BackupActionButton(
    label: String,
    filled: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = CutCornerShape(5.dp)
    val accent = MaterialTheme.colorScheme.primary
    val border = if (enabled) accent else MaterialTheme.colorScheme.outline
    val background = when {
        !enabled -> MaterialTheme.colorScheme.surfaceVariant
        filled -> accent
        else -> Color.Transparent
    }
    val content = when {
        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        filled -> MaterialTheme.colorScheme.onPrimary
        else -> accent
    }
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(shape)
            .background(background)
            .border(1.dp, border, shape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = content, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun BackupPasswordDialog(
    mode: BackupDialogMode,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var password by remember(mode) { mutableStateOf("") }
    var confirmation by remember(mode) { mutableStateOf("") }
    val longEnough = password.length >= SaveStore.PORTABLE_BACKUP_MIN_PASSWORD_CHARS
    val matches = mode == BackupDialogMode.Import || password == confirmation
    val canConfirm = longEnough && matches

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (mode == BackupDialogMode.Export) "암호화 백업 만들기" else "암호화 백업 불러오기")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (mode == BackupDialogMode.Export) {
                        "8글자 이상의 비밀번호를 정한다. 이 비밀번호는 게임에 저장되지 않는다."
                    } else {
                        "백업을 만들 때 사용한 비밀번호를 입력한다. 가져오면 현재 진행이 백업 내용으로 교체된다."
                    },
                    fontSize = 12.sp,
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("백업 비밀번호") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    isError = password.isNotEmpty() && !longEnough,
                    supportingText = if (password.isNotEmpty() && !longEnough) {
                        { Text("8글자 이상 입력") }
                    } else {
                        null
                    },
                )
                if (mode == BackupDialogMode.Export) {
                    OutlinedTextField(
                        value = confirmation,
                        onValueChange = { confirmation = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("비밀번호 다시 입력") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        isError = confirmation.isNotEmpty() && !matches,
                        supportingText = if (confirmation.isNotEmpty() && !matches) {
                            { Text("비밀번호가 서로 다름") }
                        } else {
                            null
                        },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(enabled = canConfirm, onClick = { onConfirm(password) }) {
                Text(if (mode == BackupDialogMode.Export) "파일 선택" else "백업 선택")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        },
    )
}
