package com.geomgang.game

import com.geomgang.core.AuthenticatedSaveCodec
import com.geomgang.core.Difficulty
import com.geomgang.core.GameState
import com.geomgang.core.SaveKeySource
import com.geomgang.core.SaveStore
import com.geomgang.core.Sword
import com.geomgang.core.WeaponFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

@OptIn(ExperimentalCoroutinesApi::class)
class ForgeViewModelSaveSecurityTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `변조 파일을 백업으로 복구하면 화면에 한 번 알린다`() {
        val key = SecretKeySpec(ByteArray(32) { (it * 13 + 1).toByte() }, "AES")
        val codec = AuthenticatedSaveCodec(FixedKeySource(key))
        val store = SaveStore(tmp.root, codec)
        store.saveGame(GameState(Difficulty.ENDLESS, gold = 123, sword = Sword(WeaponFamily.DRAGON, 20)))
        store.saveGame(GameState(Difficulty.ENDLESS, gold = 456, sword = Sword(WeaponFamily.DRAGON, 20)))

        val file = File(tmp.root, "save_endless.json")
        val bytes = file.readBytes().also { it[it.lastIndex] = (it.last().toInt() xor 1).toByte() }
        file.writeBytes(bytes)

        val vm = ForgeViewModel(store, Difficulty.ENDLESS, now = { 1_000L })
        assertNotNull(vm.ui.value.saveSecurityMessage)

        vm.dismissSaveSecurityMessage()
        assertNull(vm.ui.value.saveSecurityMessage)
    }

    private class FixedKeySource(private val key: SecretKey) : SaveKeySource {
        override val allowLegacyPlaintext: Boolean = false
        override fun getOrCreate(): SecretKey = key
    }
}
