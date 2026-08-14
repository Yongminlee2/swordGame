package com.geomgang.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

class EncryptedSaveStoreTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val key = SecretKeySpec(ByteArray(32) { (it * 11 + 5).toByte() }, "AES")

    private fun store(allowLegacy: Boolean = false): SaveStore = SaveStore(
        tmp.root,
        AuthenticatedSaveCodec(FixedKeySource(key, allowLegacy)),
    )

    private fun sample(gold: Long = 12_345) = GameState(
        difficulty = Difficulty.ENDLESS,
        gold = gold,
        sword = Sword(WeaponFamily.DRAGON, 20),
    )

    @Test
    fun `기존 JSON은 한 번 읽은 뒤 본 파일과 백업 모두 암호화한다`() {
        val file = File(tmp.root, "save_endless.json")
        file.writeText("""{"difficulty":"ENDLESS","gold":777}""")

        assertEquals(777L, store(allowLegacy = true).loadGame(Difficulty.ENDLESS).gold)
        assertFalse(file.readBytes().toString(Charsets.UTF_8).trimStart().startsWith("{"))
        assertFalse(File(tmp.root, "save_endless.json.bak").readBytes()
            .toString(Charsets.UTF_8).trimStart().startsWith("{"))

        // 다음 실행은 평문 이전을 닫아도 같은 키로 정상 복원한다.
        assertEquals(777L, store(allowLegacy = false).loadGame(Difficulty.ENDLESS).gold)
    }

    @Test
    fun `본 파일 변조 시 격리하고 인증된 백업으로 복구한다`() {
        val store = store()
        store.saveGame(sample())
        store.saveGame(sample(gold = 999))
        val file = File(tmp.root, "save_endless.json")
        val bytes = file.readBytes().also { it[it.lastIndex] = (it.last().toInt() xor 1).toByte() }
        file.writeBytes(bytes)

        assertEquals(12_345L, store.loadGame(Difficulty.ENDLESS).gold)
        assertTrue(SaveSecurityEvent.RECOVERED_FROM_BACKUP in store.consumeSecurityEvents())
        assertTrue(File(tmp.root, "save_endless.json.rejected").exists())
    }

    @Test
    fun `본 파일과 백업이 모두 변조되면 값을 쓰지 않는다`() {
        val store = store()
        store.saveGame(sample())
        store.saveGame(sample(gold = 999))
        listOf("save_endless.json", "save_endless.json.bak").forEach { name ->
            val file = File(tmp.root, name)
            val bytes = file.readBytes().also { it[it.lastIndex] = (it.last().toInt() xor 1).toByte() }
            file.writeBytes(bytes)
        }

        assertEquals(1_000L, store.loadGame(Difficulty.ENDLESS).gold)
        assertTrue(SaveSecurityEvent.REJECTED_INVALID_SAVE in store.consumeSecurityEvents())
    }

    @Test
    fun `키가 생긴 뒤 평문 JSON으로 바꿔치기하면 거부한다`() {
        File(tmp.root, "save_endless.json").writeText(
            """{"difficulty":"ENDLESS","gold":999999999}""",
        )
        val store = store(allowLegacy = false)

        assertEquals(1_000L, store.loadGame(Difficulty.ENDLESS).gold)
        assertTrue(SaveSecurityEvent.REJECTED_INVALID_SAVE in store.consumeSecurityEvents())
    }

    @Test
    fun `이전 종료 때 선택하지 않은 모드와 그 백업까지 각각 암호화한다`() {
        val primary = File(tmp.root, "save_normal.json")
        val backup = File(tmp.root, "save_normal.json.bak")
        primary.writeText("""{"difficulty":"NORMAL","gold":111}""")
        backup.writeText("""{"difficulty":"NORMAL","gold":99}""")
        val source = MutableKeySource(key)
        val store = SaveStore(tmp.root, AuthenticatedSaveCodec(source))

        store.finishSecurityMigration()

        assertFalse(primary.readText().trimStart().startsWith("{"))
        assertFalse(backup.readText().trimStart().startsWith("{"))
        assertFalse(source.allowLegacyPlaintext)
        assertEquals(111L, store.loadGame(Difficulty.NORMAL).gold)

        val damaged = primary.readBytes().also {
            it[it.lastIndex] = (it.last().toInt() xor 1).toByte()
        }
        primary.writeBytes(damaged)
        assertEquals(99L, store.loadGame(Difficulty.NORMAL).gold)
    }

    private class FixedKeySource(
        private val key: SecretKey,
        override val allowLegacyPlaintext: Boolean,
    ) : SaveKeySource {
        override fun getOrCreate(): SecretKey = key
    }

    private class MutableKeySource(private val key: SecretKey) : SaveKeySource {
        override var allowLegacyPlaintext: Boolean = true
        override fun getOrCreate(): SecretKey = key
        override fun markLegacyMigrationComplete() {
            allowLegacyPlaintext = false
        }
    }
}
