package com.geomgang.core

import kotlinx.serialization.json.Json
import java.io.File

/**
 * 세이브 파일 저장소.
 *
 * `java.io.File` 만 쓰므로 안드로이드 의존성이 없고 JVM 테스트로 검증된다.
 *
 * 자동 저장이 매 강화마다 일어나서 중단 타이밍이 많다. 그래서 쓰기를 세 걸음으로 나눈다.
 * 1. 임시 파일에 쓴다
 * 2. 기존 파일을 `.bak` 으로 옮긴다
 * 3. 임시 파일을 정식 이름으로 rename 한다
 *
 * 어느 걸음에서 죽어도 정식 파일이나 `.bak` 중 하나는 온전하다.
 *
 * 모드별 진행은 파일이 분리되어 있어 한 모드가 깨져도 나머지는 살아남는다.
 * 도감·업적·통계는 [PROGRESS_FILE] 한 곳에 모으고 모드 초기화의 영향을 받지 않는다.
 */
class SaveStore(private val dir: File) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun loadGame(difficulty: Difficulty): GameState =
        read(gameFile(difficulty)) { json.decodeFromString(GameState.serializer(), it) }
            ?: GameState(difficulty)

    fun saveGame(state: GameState) {
        write(gameFile(state.difficulty), json.encodeToString(GameState.serializer(), state))
    }

    fun loadProgress(): ProgressState =
        read(File(dir, PROGRESS_FILE)) { json.decodeFromString(ProgressState.serializer(), it) }
            ?: ProgressState()

    fun saveProgress(p: ProgressState) {
        write(File(dir, PROGRESS_FILE), json.encodeToString(ProgressState.serializer(), p))
    }

    fun loadSettings(): Settings =
        read(File(dir, SETTINGS_FILE)) { json.decodeFromString(Settings.serializer(), it) }
            ?: Settings()

    fun saveSettings(settings: Settings) {
        write(File(dir, SETTINGS_FILE), json.encodeToString(Settings.serializer(), settings))
    }

    /** 해당 모드의 진행만 지운다. 도감·업적·통계·설정은 남는다. */
    fun resetGame(difficulty: Difficulty) {
        val file = gameFile(difficulty)
        file.delete()
        backupOf(file).delete()
        tempOf(file).delete()
    }

    private fun gameFile(difficulty: Difficulty) = File(dir, "save_${difficulty.id}.json")

    private fun backupOf(file: File) = File(file.parentFile, file.name + ".bak")

    private fun tempOf(file: File) = File(file.parentFile, file.name + ".tmp")

    /** 정식 파일을 먼저 읽고, 깨졌으면 백업으로 물러선다. 둘 다 실패하면 null. */
    private fun <T> read(file: File, parse: (String) -> T): T? =
        tryRead(file, parse) ?: tryRead(backupOf(file), parse)

    private fun <T> tryRead(file: File, parse: (String) -> T): T? = try {
        if (file.exists()) parse(file.readText()) else null
    } catch (e: Exception) {
        // 손상된 세이브는 예외가 아니라 "없음"으로 다룬다. 앱이 죽는 것보다 낫다.
        null
    }

    private fun write(file: File, text: String) {
        dir.mkdirs()
        val tmp = tempOf(file)
        tmp.writeText(text)
        if (file.exists()) {
            val bak = backupOf(file)
            bak.delete()
            file.renameTo(bak)
        }
        if (!tmp.renameTo(file)) {
            // 드물게 rename 이 실패하면 복사로 대체하고 임시 파일을 치운다
            file.writeText(text)
            tmp.delete()
        }
    }

    companion object {
        const val PROGRESS_FILE = "collection.json"
        const val SETTINGS_FILE = "settings.json"
    }
}
