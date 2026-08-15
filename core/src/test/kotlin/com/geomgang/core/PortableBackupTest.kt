package com.geomgang.core

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

class PortableBackupTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val password = "쇠망치-백업-2026".toCharArray()

    @Test
    fun `휴대용 백업은 평문을 숨기고 같은 비밀번호로 복원된다`() {
        val plaintext = """{"gold":100000000,"sword":"dragon"}""".toByteArray()

        val encoded = PortableBackupCodec.encode(plaintext, password)

        assertFalse(encoded.toString(Charsets.UTF_8).contains("100000000"))
        assertArrayEquals(plaintext, PortableBackupCodec.decode(encoded, password))
    }

    @Test
    fun `같은 자료와 비밀번호도 매번 다른 백업이 된다`() {
        val plaintext = """{"gold":7}""".toByteArray()

        val first = PortableBackupCodec.encode(plaintext, password)
        val second = PortableBackupCodec.encode(plaintext, password)

        assertFalse(first.contentEquals(second))
    }

    @Test
    fun `비밀번호가 틀리거나 한 바이트가 바뀌면 열리지 않는다`() {
        val encoded = PortableBackupCodec.encode("{}".toByteArray(), password)
        assertThrows(PortableBackupException::class.java) {
            PortableBackupCodec.decode(encoded, "다른-비밀번호-1234".toCharArray())
        }

        encoded[encoded.lastIndex] = (encoded.last().toInt() xor 1).toByte()
        assertThrows(PortableBackupException::class.java) {
            PortableBackupCodec.decode(encoded, password)
        }
    }

    @Test
    fun `여덟 글자보다 짧은 비밀번호는 거부한다`() {
        assertThrows(PortableBackupException::class.java) {
            PortableBackupCodec.encode("{}".toByteArray(), "1234567".toCharArray())
        }
    }

    @Test
    fun `서로 다른 기기 키 사이에서 게임 도감 설정을 옮긴다`() {
        val source = store(File(tmp.root, "source"), keySeed = 3)
        source.saveGame(GameState(Difficulty.ENDLESS, gold = 88_000, sword = Sword(WeaponFamily.DRAGON, 20)))
        source.saveProgress(ProgressState(achievements = setOf(Achievement.REACH_10)))
        source.saveSettings(Settings(soundOn = false, musicOn = false, hapticsOn = false))
        val backup = source.exportPortableBackup(password)

        val destinationDir = File(tmp.root, "destination")
        val destination = store(destinationDir, keySeed = 91)
        destination.importPortableBackup(backup, password)

        assertEquals(88_000L, destination.loadGame(Difficulty.ENDLESS).gold)
        assertTrue(Achievement.REACH_10 in destination.loadProgress().achievements)
        assertFalse(destination.loadSettings().soundOn)
        assertFalse(destination.loadSettings().musicOn)
        assertTrue(File(destinationDir, "save_endless.json").readBytes().startsWithSFSV())
        assertTrue(File(destinationDir, "collection.json").readBytes().startsWithSFSV())
        assertTrue(File(destinationDir, "settings.json").readBytes().startsWithSFSV())
    }

    @Test
    fun `가져오기 검증 실패는 기존 저장값을 바꾸지 않는다`() {
        val destination = store(File(tmp.root, "destination"), keySeed = 41)
        destination.saveGame(GameState(Difficulty.ENDLESS, gold = 321))
        val source = store(File(tmp.root, "source"), keySeed = 7)
        source.saveGame(GameState(Difficulty.ENDLESS, gold = 999_999))
        source.saveProgress(ProgressState())
        val backup = source.exportPortableBackup(password)

        assertThrows(PortableBackupException::class.java) {
            destination.importPortableBackup(backup, "틀린-비밀번호-0000".toCharArray())
        }

        assertEquals(321L, destination.loadGame(Difficulty.ENDLESS).gold)
    }

    @Test
    fun `허용하지 않은 파일 이름은 복호화돼도 가져오지 않는다`() {
        val payload = """{"version":1,"files":{"save_endless.json":"{\"difficulty\":\"ENDLESS\",\"gold\":77}","collection.json":"{}","../escape.json":"{}"}}"""
            .toByteArray()
        val backup = PortableBackupCodec.encode(payload, password)
        val destinationDir = File(tmp.root, "destination")
        val destination = store(destinationDir, keySeed = 11)

        assertThrows(PortableBackupException::class.java) {
            destination.importPortableBackup(backup, password)
        }

        assertFalse(File(tmp.root, "escape.json").exists())
        assertFalse(File(destinationDir, "save_endless.json").exists())
    }

    private fun store(dir: File, keySeed: Int): SaveStore {
        val key = SecretKeySpec(ByteArray(32) { (it * keySeed + 17).toByte() }, "AES")
        return SaveStore(dir, AuthenticatedSaveCodec(FixedKeySource(key)))
    }

    private fun ByteArray.startsWithSFSV(): Boolean =
        size >= 4 && copyOfRange(0, 4).contentEquals("SFSV".toByteArray())

    private class FixedKeySource(private val key: SecretKey) : SaveKeySource {
        override val allowLegacyPlaintext: Boolean = false
        override fun getOrCreate(): SecretKey = key
    }
}
