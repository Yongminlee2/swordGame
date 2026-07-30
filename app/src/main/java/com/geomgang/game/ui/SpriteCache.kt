package com.geomgang.game.ui

import android.content.res.Resources
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext

/**
 * 스프라이트 시트를 **한 번만** 읽는다.
 *
 * 예전에는 `remember { decodeResource(...) }` 를 썼다. `remember` 는 컴포저블
 * **한 자리**의 기억이라, 같은 그림을 쓰는 자리가 늘어나면 그 수만큼 다시 읽는다.
 *
 * 도감이 그걸 정면으로 밟았다. 칸이 324개인데 칸마다 `sword_sheet3` 을 통째로
 * 디코드했고, 그 시트는 1344×960 = **4.9MB** 다. 스크롤로 새 칸이 들어올 때마다
 * 5MB를 새로 만들고 있었으니 느릴 수밖에 없었다.
 *
 * 시트는 앱이 사는 동안 바뀌지 않으므로 프로세스 수준에 둔다. 컴포지션은
 * 메인 스레드에서만 일어나지만 `synchronized` 로 굳혀 둔다 — 나중에 미리 읽기를
 * 넣더라도 여기가 깨지지 않게.
 */
object SpriteCache {

    private val cache = HashMap<Int, ImageBitmap>()

    /**
     * `inScaled = false` 는 필수다.
     *
     * 켜 두면 기기 dpi 에 맞춰 시트가 늘어나고, 격자 좌표를 픽셀로 세는
     * [SwordSheet]·[MonsterSheet] 의 계산이 전부 어긋난다.
     */
    private val options = BitmapFactory.Options().apply { inScaled = false }

    fun get(resources: Resources, resId: Int): ImageBitmap = synchronized(cache) {
        cache.getOrPut(resId) {
            BitmapFactory.decodeResource(resources, resId, options).asImageBitmap()
        }
    }
}

/** 시트 한 장. 자리마다 다시 읽지 않는다([SpriteCache]). */
@Composable
fun rememberSheet(resId: Int): ImageBitmap {
    val resources = LocalContext.current.resources
    return SpriteCache.get(resources, resId)
}
