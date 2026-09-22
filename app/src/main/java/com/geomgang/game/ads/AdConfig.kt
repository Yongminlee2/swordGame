package com.geomgang.game.ads

import com.geomgang.game.BuildConfig

/**
 * 광고 단위 ID 를 한곳에 모아 둔다.
 *
 * **디버그 빌드는 언제나 시험용 ID 로 간다.** 내 폰에서 내 광고를 누르면
 * 애드몹이 부정 클릭으로 보고 계정을 막는다. 시험용 ID 는 구글이 공개한
 * 것으로, 늘 가짜 광고가 나오고 수익도 집계되지 않는다.
 *
 * 앱 ID 는 여기가 아니라 AndroidManifest.xml 에 있다 — 광고 SDK 가 앱이
 * 켜질 때 매니페스트에서 직접 읽기 때문이다.
 */
object AdConfig {

    /** 구글이 공개한 시험용 단위. 바꾸지 않는다. */
    private const val TEST_BANNER = "ca-app-pub-3940256099942544/9214589741"
    private const val TEST_INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"
    private const val TEST_REWARDED = "ca-app-pub-3940256099942544/5224354917"

    private const val LIVE_BANNER = "ca-app-pub-6583185616347720/8354733748"
    private const val LIVE_INTERSTITIAL = "ca-app-pub-6583185616347720/7041652070"
    private const val LIVE_REWARDED = "ca-app-pub-6583185616347720/4180608455"

    /** 강화 화면 아래에 늘 붙어 있는 띠. */
    val bannerUnitId: String
        get() = if (BuildConfig.DEBUG) TEST_BANNER else LIVE_BANNER

    /** 사냥터·무한 회랑에서 나올 때 한 번씩 덮는 광고. */
    val interstitialUnitId: String
        get() = if (BuildConfig.DEBUG) TEST_INTERSTITIAL else LIVE_INTERSTITIAL

    /** 자리비움 보상을 두 배로 받을 때 보는 광고. */
    val rewardedUnitId: String
        get() = if (BuildConfig.DEBUG) TEST_REWARDED else LIVE_REWARDED
}
