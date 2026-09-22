package com.geomgang.game.ads

import com.geomgang.game.BuildConfig

/**
 * 광고 단위 ID 를 한곳에 모아 둔다.
 *
 * **디버그 빌드는 언제나 시험용 ID 로 간다.** 내 폰에서 내 광고를 누르면
 * 애드몹이 부정 클릭으로 보고 계정을 막는다. 시험용 ID 는 구글이 공개한
 * 것으로, 늘 가짜 광고가 나오고 수익도 집계되지 않는다.
 *
 * 애드몹에서 광고 단위를 만든 뒤 [RELEASE_BANNER] 를 거기서 받은 값으로
 * 바꾼다. 앱 ID 는 여기가 아니라 AndroidManifest.xml 에 있다.
 */
object AdConfig {

    /** 구글이 공개한 시험용 배너 단위. 바꾸지 않는다. */
    private const val TEST_BANNER = "ca-app-pub-3940256099942544/9214589741"

    /** ↓↓↓ 애드몹에서 만든 진짜 배너 단위 ID 로 바꿀 곳 ↓↓↓ */
    private const val RELEASE_BANNER = TEST_BANNER

    /** 지금 쓸 배너 단위. */
    val bannerUnitId: String
        get() = if (BuildConfig.DEBUG) TEST_BANNER else RELEASE_BANNER

    /** 진짜 ID 를 아직 안 넣었는지. 넣으라고 알리는 데만 쓴다. */
    val usingTestUnits: Boolean
        get() = bannerUnitId == TEST_BANNER
}
