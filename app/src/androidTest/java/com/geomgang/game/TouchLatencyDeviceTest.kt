package com.geomgang.game

import android.os.SystemClock
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.geomgang.core.Difficulty
import com.geomgang.core.GameState
import com.geomgang.core.SaveStore
import com.geomgang.core.Sword
import com.geomgang.core.WeaponFamily
import com.geomgang.game.security.secureSaveCodec
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.random.Random

/** 실제 기기의 Keystore 암호화 비용이 강화 터치 경로를 막지 않는지 측정한다. */
@RunWith(AndroidJUnit4::class)
class TouchLatencyDeviceTest {

    @Test
    fun compareSynchronousAndQueuedSaveLatency() {
        val sync = measure("sync", saveDispatcher = null)
        val queued = measure("queued", saveDispatcher = Dispatchers.IO)
        val result = "syncMedian=${sync.medianMillis}ms syncMax=${sync.maxMillis}ms " +
            "queuedMedian=${queued.medianMillis}ms queuedMax=${queued.maxMillis}ms"

        Log.i(TAG, result)
        println("TOUCH_LATENCY $result")

        // 100ms를 넘으면 저장을 옮겼어도 입력 계산 자체가 막힌 것이므로 회귀다.
        assertTrue("queued touch path was ${queued.maxMillis}ms", queued.maxMillis < 100.0)
    }

    private fun measure(label: String, saveDispatcher: CoroutineDispatcher?): Measurement {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val dir = File(context.cacheDir, "touch-latency-$label").apply {
            deleteRecursively()
            mkdirs()
        }
        val store = SaveStore(dir, secureSaveCodec())
        store.saveGame(
            GameState(
                difficulty = Difficulty.ENDLESS,
                gold = 100_000_000,
                sword = Sword(WeaponFamily.STRAIGHT, 0),
            ),
        )

        lateinit var vm: ForgeViewModel
        instrumentation.runOnMainSync {
            vm = ForgeViewModel(
                store = store,
                difficulty = Difficulty.ENDLESS,
                rng = AlwaysSucceed,
                saveDispatcher = saveDispatcher,
            )
        }

        val samples = ArrayList<Long>(ATTEMPTS)
        repeat(ATTEMPTS) {
            instrumentation.runOnMainSync {
                val started = SystemClock.elapsedRealtimeNanos()
                vm.forge()
                samples += SystemClock.elapsedRealtimeNanos() - started
                assertEquals(false, vm.ui.value.busy)
            }
        }

        vm.dispose()
        assertEquals(ATTEMPTS, store.loadGame(Difficulty.ENDLESS).sword?.level)
        dir.deleteRecursively()
        return Measurement(samples)
    }

    private data class Measurement(val nanos: List<Long>) {
        private val sorted = nanos.sorted()
        val medianMillis: Double = sorted[sorted.size / 2] / NANOS_PER_MILLI
        val maxMillis: Double = sorted.last() / NANOS_PER_MILLI
    }

    private object AlwaysSucceed : Random() {
        override fun nextBits(bitCount: Int): Int = 0
    }

    private companion object {
        const val TAG = "SwordForgeTouch"
        const val ATTEMPTS = 5
        const val NANOS_PER_MILLI = 1_000_000.0
    }
}
