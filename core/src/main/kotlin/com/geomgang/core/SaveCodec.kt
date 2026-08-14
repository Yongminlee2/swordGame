package com.geomgang.core

import java.security.GeneralSecurityException
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** 저장 파일을 디스크 형식과 평문 JSON 사이에서 변환한다. */
interface SaveCodec {
    fun encode(fileName: String, plaintext: ByteArray): ByteArray
    fun decode(fileName: String, stored: ByteArray): DecodedSave
    fun finishLegacyMigration() = Unit
}

data class DecodedSave(
    val plaintext: ByteArray,
    /** 암호화 전 버전이라 읽은 직후 안전한 형식으로 다시 써야 하는지. */
    val legacyPlaintext: Boolean = false,
)

/** JVM 단위 테스트와 도메인 모듈의 기본 저장 방식. 실제 앱은 인증 암호화를 주입한다. */
object PlaintextSaveCodec : SaveCodec {
    override fun encode(fileName: String, plaintext: ByteArray): ByteArray = plaintext

    override fun decode(fileName: String, stored: ByteArray): DecodedSave = DecodedSave(stored)
}

/** 암호문, 인증 태그, 헤더가 올바르지 않을 때 발생한다. */
class SaveIntegrityException(message: String, cause: Throwable? = null) :
    GeneralSecurityException(message, cause)

/**
 * 키 보관소와 암호화 코드를 분리한다.
 *
 * 앱은 Android Keystore 구현을 넣고, JVM 테스트는 메모리 키를 넣는다. [allowLegacyPlaintext]는
 * 이전 완료 표식이 없는 기존 설치에서만 true다. 모든 파일을 읽고 완료 표식을 남긴 뒤에는
 * 평문 JSON 바꿔치기를 더 이상 옛 세이브로 인정하지 않는다.
 */
interface SaveKeySource {
    val allowLegacyPlaintext: Boolean
    fun getOrCreate(): SecretKey
    fun markLegacyMigrationComplete() = Unit
}

/** AES-256-GCM 기반의 기밀성·무결성 보호 저장 형식. */
class AuthenticatedSaveCodec(
    private val keys: SaveKeySource,
) : SaveCodec {

    override fun finishLegacyMigration() = keys.markLegacyMigrationComplete()

    override fun encode(fileName: String, plaintext: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        // Android Keystore에서 randomizedEncryptionRequired=true인 키는 호출자가 만든 IV를
        // 암호화에 넣지 못하게 한다. 제공자가 안전한 난수 IV를 만들게 하고 결과만 봉투에 싣는다.
        cipher.init(Cipher.ENCRYPT_MODE, keys.getOrCreate())
        val iv = cipher.iv
            ?.takeIf { it.size == IV_BYTES }
            ?: throw GeneralSecurityException("unexpected GCM IV size")
        cipher.updateAAD(aad(fileName))
        val ciphertext = cipher.doFinal(plaintext)

        return ByteArray(HEADER_BYTES + iv.size + ciphertext.size).also { output ->
            MAGIC.copyInto(output, destinationOffset = 0)
            output[MAGIC.size] = FORMAT_VERSION
            output[MAGIC.size + 1] = iv.size.toByte()
            iv.copyInto(output, destinationOffset = HEADER_BYTES)
            ciphertext.copyInto(output, destinationOffset = HEADER_BYTES + iv.size)
        }
    }

    override fun decode(fileName: String, stored: ByteArray): DecodedSave {
        if (!stored.startsWith(MAGIC)) {
            if (keys.allowLegacyPlaintext && stored.looksLikeJsonObject()) {
                return DecodedSave(stored, legacyPlaintext = true)
            }
            throw SaveIntegrityException("unencrypted or unknown save format: $fileName")
        }

        if (stored.size < HEADER_BYTES + IV_BYTES + TAG_BYTES) {
            throw SaveIntegrityException("truncated save: $fileName")
        }
        if (stored[MAGIC.size] != FORMAT_VERSION) {
            throw SaveIntegrityException("unsupported save version: $fileName")
        }
        val ivSize = stored[MAGIC.size + 1].toInt() and 0xff
        if (ivSize != IV_BYTES || stored.size < HEADER_BYTES + ivSize + TAG_BYTES) {
            throw SaveIntegrityException("invalid save header: $fileName")
        }

        val iv = stored.copyOfRange(HEADER_BYTES, HEADER_BYTES + ivSize)
        val ciphertext = stored.copyOfRange(HEADER_BYTES + ivSize, stored.size)
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, keys.getOrCreate(), GCMParameterSpec(TAG_BITS, iv))
            cipher.updateAAD(aad(fileName))
            DecodedSave(cipher.doFinal(ciphertext))
        } catch (e: Exception) {
            throw SaveIntegrityException("save authentication failed: $fileName", e)
        }
    }

    private fun aad(fileName: String): ByteArray =
        "SwordForge|save|${FORMAT_VERSION.toInt()}|$fileName".toByteArray(Charsets.UTF_8)

    private fun ByteArray.startsWith(prefix: ByteArray): Boolean =
        size >= prefix.size && prefix.indices.all { this[it] == prefix[it] }

    private fun ByteArray.looksLikeJsonObject(): Boolean =
        toString(Charsets.UTF_8).trimStart().startsWith("{")

    companion object {
        private val MAGIC = byteArrayOf('S'.code.toByte(), 'F'.code.toByte(), 'S'.code.toByte(), 'V'.code.toByte())
        private const val FORMAT_VERSION: Byte = 1
        private const val IV_BYTES = 12
        private const val TAG_BITS = 128
        private const val TAG_BYTES = TAG_BITS / 8
        private const val HEADER_BYTES = 6
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
