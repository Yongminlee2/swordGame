package com.geomgang.game.sound

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * 효과음을 파일 없이 즉석에서 합성해 낸다.
 *
 * 음원 파일을 쓰지 않는 이유가 둘 있다. 라이선스를 또 따질 필요가 없고,
 * **강화 단계에 따라 음을 올리는** 것 같은 변화를 마음대로 줄 수 있다.
 * 이 게임의 검 그림도 같은 이유로 전부 벡터다.
 *
 * 소리는 전부 0.1~0.6초짜리 짧은 것이라 메모리에 담아 한 번에 재생한다.
 */
class SoundEngine(private val enabled: () -> Boolean) {

    /** 강화 성공. 단계가 높을수록 맑고 높은 소리가 난다. */
    fun forgeSuccess(level: Int) = play {
        val base = 620.0 * 1.035.pow(level.coerceIn(0, 30))
        tone(base, 0.16, decay = 9.0, gain = 0.42)
        at(0.05) { tone(base * 1.5, 0.20, decay = 7.0, gain = 0.30) }
        at(0.10) { tone(base * 2.0, 0.22, decay = 6.0, gain = 0.18) }
    }

    /** 실패했지만 단계 유지. 둔탁하게 한 번. */
    fun forgeStay() = play {
        tone(190.0, 0.13, decay = 16.0, gain = 0.40)
        noise(0.05, decay = 40.0, gain = 0.10)
    }

    /** 하락. 음이 아래로 미끄러진다. */
    fun forgeDrop() = play {
        sweep(from = 420.0, to = 130.0, seconds = 0.34, gain = 0.38)
        noise(0.20, decay = 12.0, gain = 0.12)
    }

    /** 파괴. 가장 크고 길게. 유리와 금속이 함께 깨지는 소리. */
    fun forgeDestroy() = play {
        noise(0.42, decay = 7.0, gain = 0.50)
        tone(110.0, 0.40, decay = 6.0, gain = 0.42)
        at(0.02) { tone(1_650.0, 0.18, decay = 18.0, gain = 0.26) }
        at(0.07) { tone(2_300.0, 0.14, decay = 22.0, gain = 0.18) }
        at(0.14) { sweep(from = 900.0, to = 240.0, seconds = 0.22, gain = 0.18) }
    }

    /** 방지권으로 살렸다. 짧게 올라가는 소리. */
    fun preventUsed() = play {
        sweep(from = 300.0, to = 1_150.0, seconds = 0.22, gain = 0.36)
        at(0.18) { tone(1_400.0, 0.16, decay = 12.0, gain = 0.24) }
    }

    /** 방지권 창을 놓쳤다. 힘 빠지는 소리. */
    fun preventMissed() = play {
        sweep(from = 520.0, to = 180.0, seconds = 0.28, gain = 0.26)
    }

    /** 조각을 주웠다. 자잘하게 두 번. */
    fun salvage() = play {
        tone(1_180.0, 0.07, decay = 30.0, gain = 0.26)
        at(0.06) { tone(1_560.0, 0.09, decay = 26.0, gain = 0.24) }
        at(0.13) { tone(1_920.0, 0.08, decay = 30.0, gain = 0.16) }
    }

    /** 몬스터를 쳤다. 연속으로 칠수록 음이 조금씩 오른다. */
    fun hit(combo: Int) = play {
        val base = 340.0 + (combo.coerceAtMost(20) * 9.0)
        noise(0.045, decay = 70.0, gain = 0.20)
        tone(base, 0.07, decay = 34.0, gain = 0.24)
    }

    /** 보스를 쳤다. 더 무겁게. */
    fun bossHit(combo: Int) = play {
        val base = 190.0 + (combo.coerceAtMost(20) * 5.0)
        noise(0.07, decay = 42.0, gain = 0.26)
        tone(base, 0.12, decay = 20.0, gain = 0.32)
    }

    /** 잡몹을 잡았다. */
    fun monsterDown() = play {
        sweep(from = 760.0, to = 300.0, seconds = 0.16, gain = 0.28)
        noise(0.10, decay = 26.0, gain = 0.14)
    }

    /** 구역을 깼다. 세 음이 올라가는 짧은 팡파르. */
    fun zoneCleared() = play {
        tone(660.0, 0.16, decay = 11.0, gain = 0.34)
        at(0.13) { tone(880.0, 0.16, decay = 11.0, gain = 0.34) }
        at(0.26) { tone(1_320.0, 0.30, decay = 6.0, gain = 0.38) }
    }

    /** 보스 제한 시간을 넘겼다. */
    fun bossFailed() = play {
        tone(220.0, 0.30, decay = 8.0, gain = 0.34)
        at(0.10) { tone(165.0, 0.34, decay = 7.0, gain = 0.30) }
    }

    /** 상점·조합소에서 무언가를 얻었다. */
    fun purchase() = play {
        tone(880.0, 0.09, decay = 26.0, gain = 0.26)
        at(0.08) { tone(1_320.0, 0.12, decay = 20.0, gain = 0.22) }
    }

    /** 사냥 이벤트가 나타났다. 물음표 같은 두 음. */
    fun eventAppear() = play {
        tone(990.0, 0.10, decay = 18.0, gain = 0.28)
        at(0.12) { sweep(from = 660.0, to = 1_100.0, seconds = 0.14, gain = 0.24) }
    }

    /** 고유검이 태어났다. 가장 긴 팡파르 - 발견은 사건이어야 한다. */
    fun uniqueBorn() = play {
        tone(523.0, 0.18, decay = 8.0, gain = 0.32)
        at(0.14) { tone(659.0, 0.18, decay = 8.0, gain = 0.32) }
        at(0.28) { tone(784.0, 0.18, decay = 8.0, gain = 0.34) }
        at(0.42) { tone(1_046.0, 0.42, decay = 4.0, gain = 0.40) }
        at(0.42) { tone(1_318.0, 0.42, decay = 4.0, gain = 0.20) }
    }

    /** 펫 알을 얻었다. 통통 튀는 두 음. */
    fun eggGet() = play {
        tone(740.0, 0.08, decay = 24.0, gain = 0.26)
        at(0.10) { tone(1_180.0, 0.10, decay = 18.0, gain = 0.28) }
        at(0.22) { tone(1_480.0, 0.12, decay = 16.0, gain = 0.22) }
    }

    /** 회랑 체크포인트 - 보상이 확정됐다. 묵직한 화음. */
    fun gauntletCheckpoint() = play {
        tone(392.0, 0.30, decay = 6.0, gain = 0.30)
        tone(494.0, 0.30, decay = 6.0, gain = 0.24)
        tone(587.0, 0.30, decay = 6.0, gain = 0.24)
        at(0.24) { tone(784.0, 0.34, decay = 5.0, gain = 0.34) }
    }

    private fun play(build: Mixer.() -> Unit) {
        if (!enabled()) return
        try {
            val mixer = Mixer()
            mixer.build()
            emit(mixer.finish())
        } catch (e: Throwable) {
            // 소리는 있으면 좋은 것이다. 실패해도 게임이 멈추면 안 된다.
        }
    }

    private fun emit(samples: ShortArray) {
        if (samples.isEmpty()) return
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setBufferSizeInBytes(samples.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        track.write(samples, 0, samples.size)
        // 재생이 끝나면 스스로 정리한다. 짧은 소리라 마커 하나로 충분하다.
        track.setNotificationMarkerPosition(samples.size)
        track.setPlaybackPositionUpdateListener(
            object : AudioTrack.OnPlaybackPositionUpdateListener {
                override fun onMarkerReached(t: AudioTrack?) {
                    runCatching {
                        t?.stop()
                        t?.release()
                    }
                }

                override fun onPeriodicNotification(t: AudioTrack?) = Unit
            },
        )
        track.play()
    }

    /**
     * 짧은 소리를 겹쳐 담는 그릇.
     *
     * 파형을 직접 더한다. 라이브러리를 쓰지 않는 이유는 필요한 게
     * 사인파·잡음·주파수 하강 셋뿐이기 때문이다.
     */
    class Mixer {
        private val buffer = FloatArray(SAMPLE_RATE) // 최대 1초
        private var length = 0
        private var offset = 0

        /** [seconds] 만큼 뒤에서 시작하는 소리를 담는다. */
        fun at(seconds: Double, block: Mixer.() -> Unit) {
            val saved = offset
            offset = (seconds * SAMPLE_RATE).toInt()
            block()
            offset = saved
        }

        /** 사인파. [decay] 가 크면 빨리 잦아든다. */
        fun tone(freq: Double, seconds: Double, decay: Double, gain: Double) {
            val n = (seconds * SAMPLE_RATE).toInt()
            for (i in 0 until n) {
                val t = i.toDouble() / SAMPLE_RATE
                val env = exp(-decay * t)
                add(offset + i, (sin(2 * PI * freq * t) * env * gain).toFloat())
            }
        }

        /** 주파수가 [from] 에서 [to] 로 미끄러지는 소리. */
        fun sweep(from: Double, to: Double, seconds: Double, gain: Double) {
            val n = (seconds * SAMPLE_RATE).toInt()
            var phase = 0.0
            for (i in 0 until n) {
                val p = i.toDouble() / n
                val freq = from + (to - from) * p
                phase += 2 * PI * freq / SAMPLE_RATE
                val env = (1.0 - p) * gain
                add(offset + i, (sin(phase) * env).toFloat())
            }
        }

        /** 잡음. 깨지는 소리와 타격음의 몸통이 된다. */
        fun noise(seconds: Double, decay: Double, gain: Double) {
            val n = (seconds * SAMPLE_RATE).toInt()
            val rng = Random(SEED)
            for (i in 0 until n) {
                val t = i.toDouble() / SAMPLE_RATE
                val env = exp(-decay * t)
                add(offset + i, ((rng.nextDouble() * 2 - 1) * env * gain).toFloat())
            }
        }

        private fun add(index: Int, value: Float) {
            if (index < 0 || index >= buffer.size) return
            buffer[index] += value
            if (index + 1 > length) length = index + 1
        }

        fun finish(): ShortArray {
            val out = ShortArray(length)
            for (i in 0 until length) {
                // 겹쳐 더한 결과가 범위를 넘으면 잘라 낸다
                val v = buffer[i].coerceIn(-1f, 1f)
                out[i] = (v * 32_000).toInt().toShort()
            }
            return out
        }
    }

    private companion object {
        const val SAMPLE_RATE = 22_050

        /** 잡음을 고정 시드로 만든다. 매번 다르면 같은 사건이 다르게 들려 어수선하다. */
        const val SEED = 20_260_726L
    }
}

private fun Double.pow(n: Int): Double {
    var r = 1.0
    repeat(n) { r *= this }
    return r
}
