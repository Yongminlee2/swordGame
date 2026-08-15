package com.geomgang.game

import android.media.MediaPlayer
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** 배포 APK 안의 두 배경음이 실제 Android 미디어 디코더에서 열리고 재생되는지 확인한다. */
@RunWith(AndroidJUnit4::class)
class BgmResourceDeviceTest {

    @Test
    fun forgeAndHuntTracksDecodeAndPlay() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val tracks = listOf(
            "forge" to R.raw.bgm_forge_on_the_offensive,
            "hunt" to R.raw.bgm_hunt_battle_theme_a,
        )

        tracks.forEach { (name, resource) ->
            val player = MediaPlayer.create(context, resource)
            assertNotNull("$name background music did not decode", player)
            val decoded = checkNotNull(player)
            try {
                assertTrue("$name track was too short: ${decoded.duration}ms", decoded.duration >= 30_000)
                // 기기 테스트 중 실제 소리가 튀어나오지 않게 음량만 0으로 둔다.
                decoded.setVolume(0f, 0f)
                decoded.start()
                SystemClock.sleep(150)
                assertTrue("$name background music did not start", decoded.isPlaying)
            } finally {
                decoded.release()
            }
        }
    }
}
