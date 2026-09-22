package com.geomgang.game.ads

import android.app.Activity
import android.util.Log
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 광고를 켜기 전에 이용자 동의를 받고 SDK 를 시작한다.
 *
 * 유럽·영국 이용자에게는 **구글이 인증한 도구로 동의를 받아야** 광고를 내보낼 수
 * 있다. 그 도구가 user-messaging-platform 이고, 동의 창의 글과 번역은 구글이
 * 만들어 띄워 준다 — 우리가 문구를 따로 옮길 것이 없다.
 *
 * 동의가 필요 없는 지역에서는 창이 뜨지 않고 그대로 넘어간다. 동의를 물어보다
 * 실패하더라도 게임은 멈추지 않는다 — 광고만 안 나올 뿐이다.
 */
object AdConsent {

    private const val TAG = "AdConsent"
    private val started = AtomicBoolean(false)

    /** 광고를 요청해도 되는 상태인지. 배너는 이 값이 참이 된 뒤에 붙는다. */
    @Volatile
    var canRequestAds: Boolean = false
        private set

    /**
     * 액티비티가 열릴 때 한 번 부른다.
     *
     * @param onReady 광고를 요청해도 되는 상태가 됐을 때 불린다. 화면 갱신용.
     */
    fun start(activity: Activity, onReady: () -> Unit) {
        val consent = UserMessagingPlatform.getConsentInformation(activity)
        val params = ConsentRequestParameters.Builder()
            // 13세 이상 대상 앱이다. 미성년 전용으로 표시하지 않는다.
            .setTagForUnderAgeOfConsent(false)
            .build()

        consent.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    if (formError != null) {
                        Log.w(TAG, "동의 창을 띄우지 못했다: ${formError.message}")
                    }
                    finish(activity, consent, onReady)
                }
            },
            { requestError ->
                // 네트워크가 없을 때도 여기로 온다. 광고만 포기하고 게임은 그대로 간다.
                Log.w(TAG, "동의 상태를 못 읽었다: ${requestError.message}")
                finish(activity, consent, onReady)
            },
        )

        // 지난번에 이미 동의를 받아 뒀다면 창을 기다릴 것 없이 곧바로 시작한다.
        if (consent.canRequestAds()) finish(activity, consent, onReady)
    }

    private fun finish(
        activity: Activity,
        consent: ConsentInformation,
        onReady: () -> Unit,
    ) {
        if (!consent.canRequestAds()) return
        canRequestAds = true
        if (started.compareAndSet(false, true)) {
            MobileAds.setRequestConfiguration(
                RequestConfiguration.Builder()
                    // 13세 이상이라 청소년 등급까지만 받는다.
                    .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_T)
                    .setTagForChildDirectedTreatment(
                        RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE,
                    )
                    .build(),
            )
            // initialize 는 디스크를 읽어 몇백 밀리초가 걸린다. 주 화면을 막지 않는다.
            Thread { MobileAds.initialize(activity.applicationContext) }.start()
            if (AdConfig.usingTestUnits) {
                Log.w(TAG, "아직 시험용 광고 단위다. AdConfig.kt 의 ID 를 바꿔야 수익이 잡힌다.")
            }
        }
        onReady()
    }
}
