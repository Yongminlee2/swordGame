package com.geomgang.game.feel

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * 이 기기의 진동기. 없으면 null 이다.
 *
 * API 31 부터 [VibratorManager] 를 거쳐야 한다. 예전 방식은 남아 있지만 경고가 붙는다.
 */
fun systemVibrator(context: Context): Vibrator? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)
            ?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

/**
 * 손끝으로 결과를 알린다.
 *
 * 소리는 처음부터 있었는데 진동이 없었다. 폰 게임에서 손맛의 절반이 진동이라,
 * 성공도 파괴도 손에는 똑같이 아무것도 오지 않았다.
 *
 * [com.geomgang.game.sound.SoundEngine] 과 같은 모양이다 — 켜짐 여부를 그때그때 읽고,
 * 실패해도 게임이 멈추지 않는다. **진동은 있으면 좋은 것이지 필수가 아니다.**
 *
 * 사냥 탭마다는 울리지 않는다. 연타라 손이 아프고 배터리만 먹는다.
 */
class HapticEngine(
    private val vibrator: Vibrator?,
    private val enabled: () -> Boolean,
) {

    /** 강화 성공. 짧고 밝게 톡. */
    fun forgeSuccess(level: Int) {
        // 단계가 높을수록 아주 조금 길게 - 어렵게 얻은 한 칸이 더 오래 남는다.
        val length = 18L + (level.coerceIn(0, 40) / 10)
        play(longArrayOf(0, length), intArrayOf(0, 140))
    }

    /** 실패했지만 단계 유지. 둔탁하게 한 번. */
    fun forgeStay() = play(longArrayOf(0, 45), intArrayOf(0, 90))

    /** 하락. 아래로 미끄러지듯 둘. */
    fun forgeDrop() = play(longArrayOf(0, 30, 25, 60), intArrayOf(0, 150, 0, 70))

    /** 파괴. 가장 길고 세게. 이 게임에서 제일 아픈 순간이다. */
    fun forgeDestroy() = play(longArrayOf(0, 90, 50, 180), intArrayOf(0, 255, 0, 200))

    /** 최고 기록 경신. 톡·톡·톡 올라간다. */
    fun newRecord() =
        play(longArrayOf(0, 20, 40, 20, 40, 45), intArrayOf(0, 120, 0, 170, 0, 255))

    /** 방지권으로 살렸다. */
    fun preventUsed() = play(longArrayOf(0, 25, 20, 25), intArrayOf(0, 200, 0, 200))

    /** 별이 올랐다. */
    fun starUp() = play(longArrayOf(0, 16, 30, 24), intArrayOf(0, 130, 0, 180))

    /** 별을 잃었다. 검은 무사하므로 파괴보다 훨씬 약하게. */
    fun starDown() = play(longArrayOf(0, 40), intArrayOf(0, 110))

    /** 몬스터를 잡았다. 아주 짧게. */
    fun monsterDown() = play(longArrayOf(0, 12), intArrayOf(0, 100))

    private fun play(timings: LongArray, amplitudes: IntArray) {
        if (!enabled()) return
        val device = vibrator ?: return
        runCatching {
            if (!device.hasVibrator()) return
            device.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        }
    }
}
