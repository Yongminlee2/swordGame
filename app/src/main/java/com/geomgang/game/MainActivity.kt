package com.geomgang.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.geomgang.core.Difficulty
import com.geomgang.core.SaveStore
import com.geomgang.core.WeaponFamily
import com.geomgang.game.ui.ForgeScreen
import com.geomgang.game.ui.SwordForgeTheme

/**
 * M2 에서는 일반 모드 강화 화면 하나뿐이다.
 * 모드 선택·상점·조합소·도감은 M4 이후에 붙는다.
 *
 * DI 프레임워크를 쓰지 않는다. 모듈이 둘이고 의존성이 SaveStore 하나다.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm = remember { ForgeViewModel(SaveStore(filesDir), Difficulty.NORMAL) }
            val state by vm.ui.collectAsStateWithLifecycle()
            SwordForgeTheme {
                Surface {
                    ForgeScreen(
                        state = state,
                        onForge = vm::forge,
                        onPrevent = vm::usePrevent,
                        onSalvage = vm::salvage,
                        onSell = vm::sellSword,
                        onBuy = { vm.buySword(WeaponFamily.STRAIGHT) },
                        onAnimationEnd = vm::onAnimationFinished,
                    )
                }
            }
        }
    }
}
