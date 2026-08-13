package com.geomgang.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geomgang.core.Sword
import com.geomgang.core.GameSeason
import com.geomgang.core.WeaponFamily
import com.geomgang.game.ui.ForgeBackdrop
import com.geomgang.game.ui.ForgeSeasonTheme
import com.geomgang.game.ui.ForgeText
import com.geomgang.game.ui.SwordForgeTheme
import com.geomgang.game.ui.SwordThumb
import com.geomgang.game.ui.SwordView

/** 저장 데이터를 바꾸지 않고 검 스프라이트만 기기에서 확인하는 디버그 화면. */
class SwordArtPreviewActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterImmersiveMode()
        val family = intent.getStringExtra("family")
            ?.let { name -> WeaponFamily.entries.firstOrNull { it.name == name } }
            ?: WeaponFamily.DRAGON
        val level = intent.getIntExtra("level", 20).coerceAtLeast(0)
        val sword = Sword(family, level)

        setContent {
            SwordForgeTheme {
                ForgeSeasonTheme(
                    season = if (level >= 20) GameSeason.LEGEND else GameSeason.ABYSS,
                ) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        ForgeBackdrop {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 18.dp, vertical = 34.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "검 스프라이트 확인",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Black,
                                    )
                                    Text(
                                        text = if (level > 20) "전설  +$level" else "${family.name}  +$level",
                                        color = ForgeText,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(top = 8.dp),
                                    )
                                }

                                SwordView(
                                    sword = sword,
                                    modifier = Modifier.size(340.dp),
                                )

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    SwordThumb(
                                        sword = sword.copy(level = (level - 1).coerceAtLeast(0)),
                                        size = 64.dp,
                                        dimmed = true,
                                    )
                                    SwordThumb(sword = sword, size = 80.dp)
                                    SwordThumb(
                                        sword = sword.copy(level = level + 1),
                                        size = 64.dp,
                                        dimmed = true,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enterImmersiveMode()
    }

    private fun enterImmersiveMode() {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}
