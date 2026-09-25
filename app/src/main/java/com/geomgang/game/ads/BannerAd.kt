package com.geomgang.game.ads

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/**
 * 화면 맨 위에 붙는 배너.
 *
 * 광고가 **실제로 들어왔을 때만** 자리를 차지한다. 미리 자리를 비워 두면 광고가
 * 안 들어오는 동안 빈 회색 띠가 남아 게임이 망가져 보인다.
 *
 * 기기 폭에 맞춘 adaptive 배너를 쓴다. 고정 320×50 을 쓰면 넓은 폰에서
 * 가운데만 작게 떠 어색하다.
 */
@Composable
fun ForgeBannerAd(modifier: Modifier = Modifier) {
    if (!AdConsent.canRequestAds) return

    val context = LocalContext.current
    val widthDp = LocalConfiguration.current.screenWidthDp
    var loaded by remember { mutableStateOf(false) }

    val adView = remember(widthDp) {
        AdView(context).apply {
            setAdSize(adaptiveSize(context, widthDp))
            adUnitId = AdConfig.bannerUnitId
            adListener = object : AdListener() {
                override fun onAdLoaded() {
                    loaded = true
                }
            }
            loadAd(AdRequest.Builder().build())
        }
    }

    // 게임을 내려 두면 배너도 멈춘다. 안 그러면 보이지도 않는 광고가 계속 돈다.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, adView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> adView.resume()
                Lifecycle.Event.ON_PAUSE -> adView.pause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            adView.destroy()
        }
    }

    if (!loaded) return
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF09090B))
            // 맨 위에는 카메라 구멍(노치)이 있다. 광고가 그 밑으로 들어가면 가려진다.
            .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Top)),
        contentAlignment = Alignment.Center,
    ) {
        AndroidView(factory = { adView })
    }
}

private fun adaptiveSize(context: Context, widthDp: Int): AdSize =
    AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp)
