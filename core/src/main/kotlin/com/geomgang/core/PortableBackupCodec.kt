package com.geomgang.core

import java.nio.ByteBuffer
import java.security.GeneralSecurityException
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** 비밀번호가 틀렸거나 백업 형식·인증 태그가 올바르지 않을 때 발생한다. */
class PortableBackupException(message: String, cause: Throwable? = null) :
    GeneralSecurityException(message, cause)

/**
 * 기기 Keystore 밖으로 옮길 수 있는 비밀번호 기반 백업 봉투.
 *
 * 로컬 세이브 키는 기기 밖으로 내보내지 않는다. 평문 자료를 PBKDF2-HMAC-SHA256으로
 * 만든 별도 키와 AES-256-GCM으로 감싼 뒤, 새 기기에서는 다시 그 기기의 Keystore 키로 쓴다.
 */
internal object PortableBackupCodec {
    private val magic = byteArrayOf('S'.code.toByte(), 'F'.code.toByte(), 'B'.code.toByte(), 'K'.code.toByte())

    fun encode(plaintext: ByteArray, password: CharArray): ByteArray {
        requirePassword(password)
        val salt = ByteArray(SALT_BYTES).also(SecureRandom()::nextBytes)
        val key = deriveKey(password, salt, ITERATIONS)
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val iv = cipher.iv?.takeIf { it.size == IV_BYTES }
                ?: throw PortableBackupException("unexpected backup IV size")
            val header = header(salt.size, iv.size, ITERATIONS)
            cipher.updateAAD(header + salt)
            val ciphertext = cipher.doFinal(plaintext)
            header + salt + iv + ciphertext
        } catch (e: PortableBackupException) {
            throw e
        } catch (e: GeneralSecurityException) {
            throw PortableBackupException("cannot encrypt backup", e)
        }
    }

    fun decode(stored: ByteArray, password: CharArray): ByteArray {
        requirePassword(password)
        if (stored.size !in MIN_BACKUP_BYTES..MAX_BACKUP_BYTES) {
            throw PortableBackupException("invalid backup size")
        }
        if (!stored.startsWith(magic) || stored[4] != FORMAT_VERSION) {
            throw PortableBackupException("unknown backup format")
        }

        val saltSize = stored[5].toInt() and 0xff
        val ivSize = stored[6].toInt() and 0xff
        val flags = stored[7].toInt() and 0xff
        val iterations = ByteBuffer.wrap(stored, 8, Int.SIZE_BYTES).int
        if (saltSize != SALT_BYTES || ivSize != IV_BYTES || flags != 0) {
            throw PortableBackupException("invalid backup header")
        }
        if (iterations !in MIN_ITERATIONS..MAX_ITERATIONS) {
            throw PortableBackupException("invalid backup key cost")
        }
        val cipherStart = HEADER_BYTES + saltSize + ivSize
        if (stored.size < cipherStart + TAG_BYTES) {
            throw PortableBackupException("truncated backup")
        }

        val salt = stored.copyOfRange(HEADER_BYTES, HEADER_BYTES + saltSize)
        val iv = stored.copyOfRange(HEADER_BYTES + saltSize, cipherStart)
        val ciphertext = stored.copyOfRange(cipherStart, stored.size)
        val key = deriveKey(password, salt, iterations)
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
            cipher.updateAAD(stored.copyOfRange(0, HEADER_BYTES + saltSize))
            cipher.doFinal(ciphertext)
        } catch (e: AEADBadTagException) {
            throw PortableBackupException("wrong password or damaged backup", e)
        } catch (e: GeneralSecurityException) {
            throw PortableBackupException("cannot decrypt backup", e)
        }
    }

    private fun deriveKey(password: CharArray, salt: ByteArray, iterations: Int): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, iterations, KEY_BITS)
        return try {
            val bytes = SecretKeyFactory.getInstance(KDF).generateSecret(spec).encoded
            try {
                SecretKeySpec(bytes, "AES")
            } finally {
                bytes.fill(0)
            }
        } catch (e: GeneralSecurityException) {
            throw PortableBackupException("cannot derive backup key", e)
        } finally {
            spec.clearPassword()
        }
    }

    private fun requirePassword(password: CharArray) {
        if (password.size < MIN_PASSWORD_CHARS) {
            throw PortableBackupException("backup password is too short")
        }
    }

    private fun header(saltSize: Int, ivSize: Int, iterations: Int): ByteArray =
        ByteBuffer.allocate(HEADER_BYTES).apply {
            put(magic)
            put(FORMAT_VERSION)
            put(saltSize.toByte())
            put(ivSize.toByte())
            put(0)
            putInt(iterations)
        }.array()

    private fun ByteArray.startsWith(prefix: ByteArray): Boolean =
        size >= prefix.size && prefix.indices.all { this[it] == prefix[it] }

    const val MAX_BACKUP_BYTES = 8 * 1024 * 1024
    const val MIN_PASSWORD_CHARS = 8
    private const val FORMAT_VERSION: Byte = 1
    private const val HEADER_BYTES = 12
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private const val TAG_BITS = 128
    private const val TAG_BYTES = TAG_BITS / 8
    private const val KEY_BITS = 256
    private const val ITERATIONS = 310_000
    private const val MIN_ITERATIONS = 100_000
    private const val MAX_ITERATIONS = 1_000_000
    private const val MIN_BACKUP_BYTES = HEADER_BYTES + SALT_BYTES + IV_BYTES + TAG_BYTES
    private const val KDF = "PBKDF2WithHmacSHA256"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
}
