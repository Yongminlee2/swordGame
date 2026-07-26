package com.geomgang.game.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.geomgang.core.WeaponFamily

/**
 * 계열 고르기.
 *
 * 검을 살 때와 조각으로 검을 바꿀 때 쓴다. 무작위 배정이 아니라 직접 고르게 하는 이유는
 * 도감의 빈칸을 의도적으로 채울 수 있어야 하기 때문이다.
 */
@Composable
fun FamilyPicker(
    families: List<WeaponFamily>,
    selected: WeaponFamily,
    onSelect: (WeaponFamily) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        families.forEach { family ->
            FilterChip(
                selected = family == selected,
                onClick = { onSelect(family) },
                label = { Text(family.displayName) },
            )
        }
    }
}
