package com.geomgang.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

enum class SaveSecurityEvent {
    /** 변조된 본 파일을 거부하고 인증된 백업본을 사용했다. */
    RECOVERED_FROM_BACKUP,

    /** 본 파일과 백업 모두 인증하지 못해 해당 저장값을 사용하지 않았다. */
    REJECTED_INVALID_SAVE,
}

@Serializable
private data class PortableBackupPayload(
    val version: Int = 1,
    val files: Map<String, String>,
)

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
class SaveStore(
    private val dir: File,
    private val codec: SaveCodec = PlaintextSaveCodec,
) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val securityEvents = linkedSetOf<SaveSecurityEvent>()

    fun loadGame(difficulty: Difficulty): GameState =
        read(gameFile(difficulty)) { json.decodeFromString(GameState.serializer(), it) }
            ?: newGame(difficulty)

    /**
     * 갓 시작한 판.
     *
     * 직검 한 자루를 쥐고 골드 1,000 으로 시작한다. 예전에는 빈손 + 300 이라
     * 첫 행동이 "상점에 가서 검을 산다" 였는데, 이 게임의 첫 행동은
     * **강화 버튼을 누르는 것**이어야 한다.
     */
    fun newGame(difficulty: Difficulty): GameState = GameState(
        difficulty = difficulty,
        gold = 1_000,
        sword = Sword(WeaponFamily.STRAIGHT, 0),
    )

    fun saveGame(state: GameState) {
        write(gameFile(state.difficulty), json.encodeToString(GameState.serializer(), state))
    }

    /**
     * 도감·업적·통계를 읽는다.
     *
     * 옛 도감 기록의 이관을 **여기서** 한다. 불러오기가 반드시 지나는 문이라
     * 화면이든 계산이든 티어 시절 모양을 볼 일이 없다 — 이관을 [Progress.refresh] 에
     * 맡겼더니 그 함수를 거치지 않는 경로가 있어 도감이 계열마다 한 칸으로 뭉쳤다.
     */
    fun loadProgress(): ProgressState {
        val loaded = read(File(dir, PROGRESS_FILE)) {
            json.decodeFromString(ProgressState.serializer(), it)
        } ?: ProgressState()
        return Progress.migrateCodex(loaded)
    }

    fun saveProgress(p: ProgressState) {
        write(File(dir, PROGRESS_FILE), json.encodeToString(ProgressState.serializer(), p))
    }

    fun loadSettings(): Settings =
        read(File(dir, SETTINGS_FILE)) { json.decodeFromString(Settings.serializer(), it) }
            ?: Settings()

    fun saveSettings(settings: Settings) {
        write(File(dir, SETTINGS_FILE), json.encodeToString(Settings.serializer(), settings))
    }

    /** 모든 모드와 공용 진행·설정을 기기 독립 비밀번호 백업으로 묶는다. */
    fun exportPortableBackup(password: CharArray): ByteArray {
        val files = linkedMapOf<String, String>()
        logicalFiles().forEach { logical ->
            val text = read(logical) { candidate ->
                validateContents(logical, candidate)
                candidate
            }
            if (text != null) files[logical.name] = text
        }
        val hasGame = files.keys.any { it.startsWith("save_") && it.endsWith(".json") }
        if (!hasGame || PROGRESS_FILE !in files) {
            throw PortableBackupException("backup has no playable save")
        }

        val plaintext = json.encodeToString(
            PortableBackupPayload.serializer(),
            PortableBackupPayload(files = files),
        ).toByteArray(Charsets.UTF_8)
        return try {
            PortableBackupCodec.encode(plaintext, password)
        } finally {
            plaintext.fill(0)
        }
    }

    /** 휴대용 백업을 전부 검증·암호화한 뒤에만 이 기기의 저장 파일로 교체한다. */
    fun importPortableBackup(stored: ByteArray, password: CharArray) {
        val plaintext = PortableBackupCodec.decode(stored, password)
        val payload = try {
            json.decodeFromString(PortableBackupPayload.serializer(), plaintext.toString(Charsets.UTF_8))
        } catch (e: Exception) {
            throw PortableBackupException("invalid backup contents", e)
        } finally {
            plaintext.fill(0)
        }
        if (payload.version != PORTABLE_PAYLOAD_VERSION || payload.files.isEmpty()) {
            throw PortableBackupException("unsupported backup contents")
        }

        val allowed = logicalFiles().associateBy { it.name }
        val hasGame = payload.files.keys.any { it.startsWith("save_") && it.endsWith(".json") }
        if (!hasGame || PROGRESS_FILE !in payload.files || payload.files.keys.any { it !in allowed }) {
            throw PortableBackupException("backup contains unexpected files")
        }

        // 검증과 기기 키 암호화를 먼저 끝낸다. 여기까지는 기존 저장 파일을 전혀 바꾸지 않는다.
        val encrypted = try {
            payload.files.map { (name, text) ->
                val logical = allowed.getValue(name)
                validateContents(logical, text)
                logical to codec.encode(name, text.toByteArray(Charsets.UTF_8))
            }
        } catch (e: Exception) {
            if (e is PortableBackupException) throw e
            throw PortableBackupException("backup validation failed", e)
        }

        try {
            encrypted.forEach { (file, bytes) -> writeEncoded(file, bytes) }
        } catch (e: Exception) {
            throw PortableBackupException("cannot store imported backup", e)
        }
    }

    /** 이번 실행의 변조 감지 결과를 한 번만 꺼낸다. */
    fun consumeSecurityEvents(): Set<SaveSecurityEvent> =
        securityEvents.toSet().also { securityEvents.clear() }

    /** 모든 모드·진행도·설정과 백업까지 이전한 뒤 평문 이전 창을 영구히 닫는다. */
    fun finishSecurityMigration() {
        logicalFiles().forEach { logical ->
            migrateRemainingLegacyFile(actual = logical, logical = logical)
            migrateRemainingLegacyFile(actual = backupOf(logical), logical = logical)
        }
        codec.finishLegacyMigration()
    }

    /** 해당 모드의 진행만 지운다. 도감·업적·통계·설정은 남는다. */
    fun resetGame(difficulty: Difficulty) {
        val file = gameFile(difficulty)
        file.delete()
        backupOf(file).delete()
        tempOf(file).delete()
        rejectedOf(file).delete()
        rejectedOf(backupOf(file)).delete()
    }

    private fun gameFile(difficulty: Difficulty) = File(dir, "save_${difficulty.id}.json")

    private fun logicalFiles(): List<File> = Difficulty.entries.map(::gameFile) + listOf(
        File(dir, PROGRESS_FILE),
        File(dir, SETTINGS_FILE),
    )

    private fun backupOf(file: File) = File(file.parentFile, file.name + ".bak")

    private fun tempOf(file: File) = File(file.parentFile, file.name + ".tmp")

    /** 정식 파일을 먼저 읽고, 검증에 실패하면 인증된 백업으로 물러선다. */
    private fun <T> read(file: File, parse: (String) -> T): T? {
        val primary = tryRead(actual = file, logical = file, parse = parse)
        primary.value?.let { return it }

        val backup = tryRead(actual = backupOf(file), logical = file, parse = parse)
        backup.value?.let {
            if (primary.integrityFailure) {
                securityEvents += SaveSecurityEvent.RECOVERED_FROM_BACKUP
            }
            return it
        }

        if (primary.integrityFailure || backup.integrityFailure) {
            securityEvents += SaveSecurityEvent.REJECTED_INVALID_SAVE
        }
        return null
    }

    private data class ReadAttempt<T>(
        val value: T? = null,
        val integrityFailure: Boolean = false,
    )

    private fun <T> tryRead(
        actual: File,
        logical: File,
        parse: (String) -> T,
    ): ReadAttempt<T> {
        if (!actual.exists()) return ReadAttempt()
        if (actual.length() !in 1L..MAX_SAVE_BYTES) {
            quarantine(actual)
            return ReadAttempt(integrityFailure = true)
        }

        return try {
            val decoded = codec.decode(logical.name, actual.readBytes())
            val value = parse(decoded.plaintext.toString(Charsets.UTF_8))
            if (decoded.legacyPlaintext) {
                migrateLegacy(logical, decoded.plaintext)
            }
            ReadAttempt(value = value)
        } catch (e: SaveIntegrityException) {
            quarantine(actual)
            ReadAttempt(integrityFailure = true)
        } catch (e: Exception) {
            // 암호 인증은 통과했지만 JSON이 손상된 경우도 백업으로 물러선다.
            ReadAttempt()
        }
    }

    private fun write(file: File, text: String) {
        val encoded = codec.encode(file.name, text.toByteArray(Charsets.UTF_8))
        writeEncoded(file, encoded)
    }

    private fun writeEncoded(file: File, encoded: ByteArray) {
        dir.mkdirs()
        val tmp = tempOf(file)
        tmp.writeBytes(encoded)
        if (file.exists()) {
            val bak = backupOf(file)
            bak.delete()
            if (!file.renameTo(bak)) {
                file.copyTo(bak, overwrite = true)
                file.delete()
            }
        }
        if (!tmp.renameTo(file)) {
            // 드물게 rename 이 실패하면 복사로 대체하고 임시 파일을 치운다
            file.writeBytes(encoded)
            tmp.delete()
        }
    }

    /**
     * 옛 평문은 인증된 본 파일과 백업으로 동시에 교체한다.
     *
     * 백업을 먼저 바꾸므로 중간에 앱이 종료돼도 다음 실행은 암호화 백업으로 복구할 수 있다.
     */
    private fun migrateLegacy(file: File, plaintext: ByteArray) {
        dir.mkdirs()
        replaceRaw(backupOf(file), codec.encode(file.name, plaintext))
        replaceRaw(file, codec.encode(file.name, plaintext))
    }

    /** 현재 선택하지 않은 모드와 서로 다른 백업 내용도 잃지 않고 각각 암호화한다. */
    private fun migrateRemainingLegacyFile(actual: File, logical: File) {
        if (!actual.exists()) return
        if (actual.length() !in 1L..MAX_SAVE_BYTES) {
            quarantine(actual)
            securityEvents += SaveSecurityEvent.REJECTED_INVALID_SAVE
            return
        }

        val decoded = try {
            codec.decode(logical.name, actual.readBytes())
        } catch (_: SaveIntegrityException) {
            quarantine(actual)
            securityEvents += SaveSecurityEvent.REJECTED_INVALID_SAVE
            return
        }
        if (!decoded.legacyPlaintext) return

        try {
            validateContents(logical, decoded.plaintext.toString(Charsets.UTF_8))
        } catch (_: Exception) {
            quarantine(actual)
            securityEvents += SaveSecurityEvent.REJECTED_INVALID_SAVE
            return
        }
        // 키 저장소나 디스크 쓰기가 실패하면 평문 원본을 그대로 두고 완료 표식도 만들지 않는다.
        val encrypted = codec.encode(logical.name, decoded.plaintext)
        replaceRaw(actual, encrypted)
    }

    /** 파일 이름에 맞는 자료형인지 확인해 임의 JSON을 정상 이전본으로 승인하지 않는다. */
    private fun validateContents(logical: File, text: String) {
        when (logical.name) {
            PROGRESS_FILE -> json.decodeFromString(ProgressState.serializer(), text)
            SETTINGS_FILE -> json.decodeFromString(Settings.serializer(), text)
            else -> {
                val expected = Difficulty.entries.firstOrNull { gameFile(it).name == logical.name }
                    ?: throw IllegalArgumentException("unknown save file: ${logical.name}")
                val state = json.decodeFromString(GameState.serializer(), text)
                require(state.difficulty == expected) { "difficulty does not match file name" }
            }
        }
    }

    private fun replaceRaw(file: File, bytes: ByteArray) {
        val tmp = File(file.parentFile, file.name + ".secure.tmp")
        tmp.writeBytes(bytes)
        if (file.exists() && !file.delete()) {
            tmp.delete()
            throw IllegalStateException("cannot replace ${file.name}")
        }
        if (!tmp.renameTo(file)) {
            file.writeBytes(bytes)
            tmp.delete()
        }
    }

    /** 인증에 실패한 파일은 다시 읽지 않되 원인 확인을 위해 한 개만 격리한다. */
    private fun quarantine(file: File) {
        val rejected = rejectedOf(file)
        rejected.delete()
        if (!file.renameTo(rejected)) file.delete()
    }

    private fun rejectedOf(file: File) = File(file.parentFile, file.name + ".rejected")

    companion object {
        const val PROGRESS_FILE = "collection.json"
        const val SETTINGS_FILE = "settings.json"
        const val PORTABLE_BACKUP_MAX_BYTES = PortableBackupCodec.MAX_BACKUP_BYTES
        const val PORTABLE_BACKUP_MIN_PASSWORD_CHARS = PortableBackupCodec.MIN_PASSWORD_CHARS
        private const val PORTABLE_PAYLOAD_VERSION = 1
        private const val MAX_SAVE_BYTES = 4L * 1024 * 1024
    }
}
