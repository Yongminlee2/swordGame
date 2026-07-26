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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geomgang.core.Recipe
import com.geomgang.core.RecipeReward
import com.geomgang.core.Recipes
import com.geomgang.core.WeaponFamily
import com.geomgang.game.ForgeUiState

/**
 * 조합소.
 *
 * [Recipes.ALL] 을 그대로 훑는다. 교환식이 늘어도 이 화면은 그대로다.
 *
 * 조각은 골드와 분리된 화폐라, 돈이 다 떨어져도 여기서 재기할 수 있다.
 */
@Composable
fun CraftScreen(
    state: ForgeUiState,
    onCraft: (recipeId: String, family: WeaponFamily) -> Unit,
    onBack: () -> Unit,
) {
    var family by remember { mutableStateOf(state.unlockedFamilies.first()) }
    val needsFamily = Recipes.ALL.any { it.reward is RecipeReward.GrantSword }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        ScreenHeader(title = "조합소", onBack = onBack)

        Text(
            text = "보유 조각  ${state.shards}",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "파괴된 검에서 주운 조각으로 바꾼다",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
        )

        if (needsFamily) {
            Spacer(Modifier.height(12.dp))
            Text(
                "검 교환에 쓸 계열",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            )
            Spacer(Modifier.height(4.dp))
            FamilyPicker(
                families = state.unlockedFamilies,
                selected = family,
                onSelect = { family = it },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(16.dp))

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Recipes.ALL.forEachIndexed { index, recipe ->
                    if (index > 0) HorizontalDivider(Modifier.padding(vertical = 4.dp))
                    RecipeRow(
                        recipe = recipe,
                        state = state,
                        family = family,
                        onCraft = { onCraft(recipe.id, family) },
                    )
                }
            }
        }
    }
}

@Composable
private fun RecipeRow(
    recipe: Recipe,
    state: ForgeUiState,
    family: WeaponFamily,
    onCraft: () -> Unit,
) {
    val grantsSword = recipe.reward is RecipeReward.GrantSword
    val blockedBySword = grantsSword && state.sword != null
    val enough = state.shards >= recipe.shardCost
    val enabled = enough && !blockedBySword && !state.busy

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = if (grantsSword) "${family.displayName} ${recipe.displayName}"
                else recipe.displayName,
                fontWeight = FontWeight.Medium,
            )
            val reason = when {
                blockedBySword -> "검을 들고 있으면 바꿀 수 없다"
                !enough -> "조각 ${recipe.shardCost - state.shards}개 부족"
                else -> null
            }
            if (reason != null) {
                Text(
                    text = reason,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
            }
        }
        Button(onClick = onCraft, enabled = enabled) {
            Text("조각 ${recipe.shardCost}")
        }
    }
}
