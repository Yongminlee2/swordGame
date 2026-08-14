package com.geomgang.game.security

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import androidx.annotation.RequiresApi
import com.geomgang.core.AuthenticatedSaveCodec
import com.geomgang.core.SaveCodec
import com.geomgang.core.SaveKeySource
import com.geomgang.game.BuildConfig
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.security.ProviderException
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/** 실제 앱에서 쓰는 기기 귀속 세이브 코덱. */
fun secureSaveCodec(): SaveCodec = AuthenticatedSaveCodec(
    AndroidSaveKeySource(allowDebugLegacyPlaintext = BuildConfig.DEBUG),
)

/** 키 원문이 앱 파일에 나타나지 않도록 Android Keystore 안에서 생성·사용한다. */
private class AndroidSaveKeySource(
    /** 개발 중 옛 APK를 다시 설치해 평문으로 돌아간 경우만 재이전을 허용한다. 릴리스는 항상 false다. */
    private val allowDebugLegacyPlaintext: Boolean,
) : SaveKeySource {
    private val keyStore = KeyStore.getInstance(PROVIDER).apply { load(null) }

    @Volatile
    private var cachedKey: SecretKey? = null

    /** 릴리스는 완료 표식이 없던 기존 설치만, 디버그는 APK 롤백 뒤에도 평문 이전을 허용한다. */
    override var allowLegacyPlaintext: Boolean =
        allowDebugLegacyPlaintext || !keyStore.containsAlias(MIGRATION_ALIAS)
        private set

    @Synchronized
    override fun getOrCreate(): SecretKey {
        cachedKey?.let { return it }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let {
            cachedKey = it
            return it
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                return StrongBoxGenerator.generate(KEY_ALIAS).also { cachedKey = it }
            } catch (_: StrongBoxUnavailableException) {
                // 모든 기기가 StrongBox AES를 지원하지는 않는다. 일반 Keystore로 물러선다.
            } catch (_: ProviderException) {
                // 제조사 구현이 StrongBox 미지원 상황을 ProviderException으로 주는 경우가 있다.
            } catch (_: GeneralSecurityException) {
                // 일부 제조사는 지원하지 않는 키 조합을 검사 예외로 돌려준다.
            }
        }
        return generateKey(KEY_ALIAS, strongBox = false).also { cachedKey = it }
    }

    @Synchronized
    override fun markLegacyMigrationComplete() {
        if (!keyStore.containsAlias(MIGRATION_ALIAS)) {
            generateKey(MIGRATION_ALIAS, strongBox = false)
        }
        allowLegacyPlaintext = allowDebugLegacyPlaintext
    }

    companion object {
        private const val PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "com.geomgang.game.save.aes.v1"
        // v1 시험 설치는 Keystore의 IV 정책과 맞지 않아 이전 완료 전에 종료될 수 있었다.
        // v2 표식으로 한 번 더 전체 파일을 확인한 뒤에만 평문 허용 창을 닫는다.
        private const val MIGRATION_ALIAS = "com.geomgang.game.save.migrated.v2"

        private fun generateKey(alias: String, strongBox: Boolean): SecretKey {
            val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER)
            val builder = KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setKeySize(256)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && strongBox) {
                builder.setIsStrongBoxBacked(true)
            }
            generator.init(builder.build())
            return generator.generateKey()
        }

        @RequiresApi(Build.VERSION_CODES.P)
        private object StrongBoxGenerator {
            fun generate(alias: String): SecretKey = generateKey(alias, strongBox = true)
        }
    }
}
