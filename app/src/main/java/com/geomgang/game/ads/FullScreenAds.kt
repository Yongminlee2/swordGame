package com.geomgang.game.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

private const val TAG = "FullScreenAds"

/**
 * 사냥터·무한 회랑에서 나올 때 한 번씩 덮는 전면 광고.
 *
 * **얼마나 자주 뜨는지가 전부다.** 나올 때마다 띄우면 사냥을 두 번 다녀온
 * 사람이 광고를 두 번 본다. 그러면 사냥터를 안 간다. 그래서 두 가지로 묶어 둔다.
 *
 * 1. 앱을 켠 뒤 [FIRST_DELAY_MILLIS] 안에는 띄우지 않는다 — 켜자마자 광고를
 *    맞는 것이 가장 나쁜 첫인상이다
 * 2. 한 번 띄운 뒤 [COOLDOWN_MILLIS] 동안은 다시 띄우지 않는다
 *
 * 광고가 아직 안 받아졌으면 **그냥 넘어간다.** 광고를 기다리느라 화면 전환이
 * 멈추면 게임이 느린 것처럼 느껴진다.
 */
object InterstitialAds {

    private const val FIRST_DELAY_MILLIS = 3 * 60 * 1000L
    private const val COOLDOWN_MILLIS = 4 * 60 * 1000L

    private var ad: InterstitialAd? = null
    private var loading = false
    private val startedAt = System.currentTimeMillis()
    private var lastShownAt = 0L

    fun preload(context: Context) {
        if (ad != null || loading || !AdConsent.canRequestAds) return
        loading = true
        InterstitialAd.load(
            context.applicationContext,
            AdConfig.interstitialUnitId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(loaded: InterstitialAd) {
                    ad = loaded
                    loading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    // 네트워크가 없거나 재고가 없을 때 온다. 다음 기회에 다시 받는다.
                    Log.w(TAG, "전면 광고를 못 받았다: ${error.message}")
                    ad = null
                    loading = false
                }
            },
        )
    }

    /** 띄울 때가 됐으면 띄운다. 아니면 아무 일도 하지 않는다. */
    fun showIfDue(activity: Activity) {
        val now = System.currentTimeMillis()
        if (now - startedAt < FIRST_DELAY_MILLIS) return
        if (lastShownAt != 0L && now - lastShownAt < COOLDOWN_MILLIS) return
        val ready = ad ?: run {
            preload(activity)
            return
        }
        ready.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                ad = null
                preload(activity)
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.w(TAG, "전면 광고를 못 띄웠다: ${error.message}")
                ad = null
                preload(activity)
            }
        }
        lastShownAt = now
        ready.show(activity)
    }
}

/**
 * 자리비움 보상을 두 배로 받을 때 보는 보상형 광고.
 *
 * **광고를 끝까지 봤을 때만** [onReward] 가 불린다. 중간에 닫으면 아무 일도
 * 일어나지 않는다 — 그게 보상형 광고의 약속이다.
 *
 * [isReady] 는 화면이 읽는 값이다. 광고가 없으면 「두 배로」 단추를 **아예
 * 그리지 않는다.** 눌러도 안 되는 단추를 보여 주는 것보다 없는 편이 낫다.
 */
object RewardedAds {

    private var ad: RewardedAd? = null
    private var loading = false

    var isReady by mutableStateOf(false)
        private set

    fun preload(context: Context) {
        if (ad != null || loading || !AdConsent.canRequestAds) return
        loading = true
        RewardedAd.load(
            context.applicationContext,
            AdConfig.rewardedUnitId,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(loaded: RewardedAd) {
                    ad = loaded
                    loading = false
                    isReady = true
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "보상형 광고를 못 받았다: ${error.message}")
                    ad = null
                    loading = false
                    isReady = false
                }
            },
        )
    }

    fun show(activity: Activity, onReward: () -> Unit) {
        val ready = ad ?: return
        ready.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                ad = null
                isReady = false
                preload(activity)
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.w(TAG, "보상형 광고를 못 띄웠다: ${error.message}")
                ad = null
                isReady = false
                preload(activity)
            }
        }
        ready.show(activity) { onReward() }
    }
}
