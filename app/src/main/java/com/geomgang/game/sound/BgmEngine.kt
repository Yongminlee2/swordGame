package com.geomgang.game.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import com.geomgang.game.R

/** 화면 성격에 맞춰 계속 이어지는 배경음 두 갈래. */
enum class BgmScene { Forge, Hunt }

/**
 * 대장간과 사냥터 배경음을 비동기로 준비하고 전환한다.
 *
 * 음원 디코딩을 터치 경로에서 하지 않도록 두 곡을 앱 시작 때 [MediaPlayer.prepareAsync]로
 * 준비한다. 화면을 오갈 때는 재생 위치를 보존한 채 멈추고 이어서, 메뉴를 잠깐 열었다고
 * 매번 곡 첫 부분만 반복되는 일을 막는다.
 */
class BgmEngine(context: Context) {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val players = mutableMapOf<BgmScene, MediaPlayer>()
    private val prepared = mutableSetOf<BgmScene>()

    private var requestedScene = BgmScene.Forge
    private var enabled = true
    private var foreground = true
    private var focusHeld = false
    private var ducked = false
    private var released = false

    private val musicAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()

    private val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(musicAttributes)
        .setAcceptsDelayedFocusGain(true)
        .setOnAudioFocusChangeListener(::onAudioFocusChange, mainHandler)
        .build()

    init {
        prepare(BgmScene.Forge, R.raw.bgm_forge_on_the_offensive)
        prepare(BgmScene.Hunt, R.raw.bgm_hunt_battle_theme_a)
    }

    /** 현재 화면과 설정을 한 번에 맞춘다. 같은 값이면 플레이어를 건드리지 않는다. */
    fun update(scene: BgmScene, isEnabled: Boolean) = onMain {
        if (requestedScene == scene && enabled == isEnabled) return@onMain
        requestedScene = scene
        enabled = isEnabled
        syncPlayback()
    }

    fun onForeground() = onMain {
        foreground = true
        syncPlayback()
    }

    fun onBackground() = onMain {
        foreground = false
        pauseAll()
        abandonFocus()
    }

    fun release() = onMain {
        if (released) return@onMain
        released = true
        abandonFocus()
        players.values.forEach { player -> runCatching { player.release() } }
        players.clear()
        prepared.clear()
    }

    private fun prepare(scene: BgmScene, rawResource: Int) {
        val player = MediaPlayer()
        players[scene] = player
        runCatching {
            player.setAudioAttributes(musicAttributes)
            appContext.resources.openRawResourceFd(rawResource).use { asset ->
                player.setDataSource(asset.fileDescriptor, asset.startOffset, asset.length)
            }
            player.isLooping = true
            player.setVolume(volumeFor(scene), volumeFor(scene))
            player.setOnPreparedListener {
                if (released) {
                    runCatching { it.release() }
                    return@setOnPreparedListener
                }
                prepared += scene
                syncPlayback()
            }
            player.setOnErrorListener { broken, _, _ ->
                prepared -= scene
                players.remove(scene)
                runCatching { broken.release() }
                true
            }
            player.prepareAsync()
        }.onFailure {
            players.remove(scene)
            runCatching { player.release() }
        }
    }

    private fun syncPlayback() {
        if (released) return
        if (!enabled || !foreground) {
            pauseAll()
            abandonFocus()
            return
        }
        if (!focusHeld && !requestFocus()) {
            pauseAll()
            return
        }

        players.forEach { (scene, player) ->
            if (scene == requestedScene && scene in prepared) {
                val volume = volumeFor(scene)
                runCatching {
                    player.setVolume(volume, volume)
                    if (!player.isPlaying) player.start()
                }
            } else {
                runCatching { if (player.isPlaying) player.pause() }
            }
        }
    }

    private fun requestFocus(): Boolean {
        val result = audioManager.requestAudioFocus(focusRequest)
        focusHeld = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        return focusHeld
    }

    private fun abandonFocus() {
        if (!focusHeld) return
        audioManager.abandonAudioFocusRequest(focusRequest)
        focusHeld = false
        ducked = false
    }

    private fun onAudioFocusChange(change: Int) {
        when (change) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                focusHeld = true
                ducked = false
                syncPlayback()
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                ducked = true
                players.forEach { (scene, player) ->
                    val volume = volumeFor(scene)
                    runCatching { player.setVolume(volume, volume) }
                }
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> pauseAll()
            AudioManager.AUDIOFOCUS_LOSS -> {
                focusHeld = false
                pauseAll()
            }
        }
    }

    private fun pauseAll() {
        players.values.forEach { player ->
            runCatching { if (player.isPlaying) player.pause() }
        }
    }

    private fun volumeFor(scene: BgmScene): Float {
        val base = when (scene) {
            BgmScene.Forge -> FORGE_VOLUME
            BgmScene.Hunt -> HUNT_VOLUME
        }
        return if (ducked) base * DUCK_MULTIPLIER else base
    }

    private inline fun onMain(crossinline block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block() else mainHandler.post { block() }
    }

    private companion object {
        // 효과음이 게임 판정을 알려 주므로 배경음은 의도적으로 낮게 둔다.
        const val FORGE_VOLUME = 0.16f
        const val HUNT_VOLUME = 0.20f
        const val DUCK_MULTIPLIER = 0.25f
    }
}
