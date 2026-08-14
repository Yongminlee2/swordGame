package com.geomgang.core

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

class SaveCodecTest {

    private val key = SecretKeySpec(ByteArray(32) { (it * 7 + 3).toByte() }, "AES")

    private fun codec(allowLegacy: Boolean = false, secret: SecretKey = key) =
        AuthenticatedSaveCodec(FixedKeySource(secret, allowLegacy))

    @Test
    fun `암호화한 세이브는 평문이 보이지 않고 다시 복원된다`() {
        val plain = "{\"gold\":100000000}".toByteArray()
        val encoded = codec().encode("save_endless.json", plain)

        assertFalse(encoded.toString(Charsets.UTF_8).contains("100000000"))
        assertArrayEquals(plain, codec().decode("save_endless.json", encoded).plaintext)
    }

    @Test
    fun `같은 세이브도 매번 다른 난수값으로 암호화된다`() {
        val plain = "{\"gold\":7}".toByteArray()
        val first = codec().encode("save_endless.json", plain)
        val second = codec().encode("save_endless.json", plain)

        assertFalse(first.contentEquals(second))
    }

    @Test
    fun `암호문 한 바이트를 바꾸면 인증에 실패한다`() {
        val encoded = codec().encode("save_endless.json", "{\"gold\":7}".toByteArray())
        encoded[encoded.lastIndex] = (encoded.last().toInt() xor 1).toByte()

        assertThrows(SaveIntegrityException::class.java) {
            codec().decode("save_endless.json", encoded)
        }
    }

    @Test
    fun `다른 저장 파일의 암호문을 바꿔 끼울 수 없다`() {
        val encoded = codec().encode("collection.json", "{\"codex\":[]}".toByteArray())

        assertThrows(SaveIntegrityException::class.java) {
            codec().decode("save_endless.json", encoded)
        }
    }

    @Test
    fun `평문은 기존 설치의 첫 이전 실행에서만 허용한다`() {
        val plain = "{\"gold\":7}".toByteArray()
        val migrated = codec(allowLegacy = true).decode("save_endless.json", plain)
        assertArrayEquals(plain, migrated.plaintext)
        assertTrue(migrated.legacyPlaintext)

        assertThrows(SaveIntegrityException::class.java) {
            codec(allowLegacy = false).decode("save_endless.json", plain)
        }
    }

    @Test
    fun `이전 완료 뒤에는 같은 실행에서도 평문 허용을 닫는다`() {
        val source = MutableKeySource(key)
        val codec = AuthenticatedSaveCodec(source)
        val plain = "{\"gold\":7}".toByteArray()
        codec.decode("save_endless.json", plain)

        codec.finishLegacyMigration()

        assertThrows(SaveIntegrityException::class.java) {
            codec.decode("save_endless.json", plain)
        }
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
