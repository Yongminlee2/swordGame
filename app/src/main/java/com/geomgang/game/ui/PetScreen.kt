package com.geomgang.game.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geomgang.core.PetKind
import com.geomgang.core.Pets
import com.geomgang.core.Zone
import com.geomgang.game.ForgeUiState

/**
 * 펫.
 *
 * 보스가 5% 확률로 알을 떨어뜨린다. 같은 알을 또 얻으면 레벨이 오른다(상한 5).
 * 장착은 한 마리 - 여러 효과가 겹치면 수치가 풀린다.
 */
@Composable
fun PetScreen(
    state: ForgeUiState,
    onEquip: (String?) -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
    ) {
        ScreenHeader(title = "펫", onBack = onBack)

        // 수집 진행도는 전역 기록(도감)을 센다. 지금 보유는 모드 세이브가 들고 있다.
        val foundCount = state.progress.petsFound.size
        Text(
            text = "$foundCount / ${PetKind.entries.size} · 보스가 5% 확률로 알을 떨어뜨린다",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
        )
        Spacer(Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(PetKind.entries) { pet ->
                PetRow(
                    pet = pet,
                    owned = Pets.owns(state.pets, pet.id),
                    level = Pets.levelOf(state.pets, pet.id),
                    equipped = state.pets.equippedId == pet.id,
                    busy = state.busy,
                    onEquip = onEquip,
                )
            }
        }
    }
}

@Composable
private fun PetRow(
    pet: PetKind,
    owned: Boolean,
    level: Int,
    equipped: Boolean,
    busy: Boolean,
    onEquip: (String?) -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PetSprite(petId = pet.id, owned = owned, size = 42.dp)
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
            ) {
                Text(
                    text = if (owned) "${pet.displayName}  Lv.$level" else "???",
                    fontWeight = FontWeight.Bold,
                    color = if (owned) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                    },
                )
                Text(
                    text = if (owned) {
                        Pets.effectLine(pet, level)
                    } else {
                        "${Zone.fromId(pet.zoneId).displayName}의 보스가 품고 있다"
                    },
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
                if (owned) {
                    Text(
                        text = pet.blurb,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                    )
                }
            }
            when {
                equipped -> OutlinedButton(
                    onClick = { onEquip(null) },
                    enabled = !busy,
                ) { Text("해제", fontSize = 12.sp) }
                owned -> Button(
                    onClick = { onEquip(pet.id) },
                    enabled = !busy,
                ) { Text("장착", fontSize = 12.sp) }
            }
        }
    }
}
