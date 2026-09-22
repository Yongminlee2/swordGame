package com.geomgang.game

import android.graphics.Color
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Surface
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.geomgang.core.Difficulty
import com.geomgang.core.SaveStore
import com.geomgang.core.WeaponFamily
import com.geomgang.game.ads.AdConsent
import com.geomgang.game.ads.ForgeBannerAd
import com.geomgang.game.feel.HapticEngine
import com.geomgang.game.feel.systemVibrator
import com.geomgang.game.i18n.GameLanguage
import com.geomgang.game.i18n.GameTranslator
import com.geomgang.game.security.secureSaveCodec
import com.geomgang.game.sound.BgmEngine
import com.geomgang.game.sound.BgmScene
import com.geomgang.game.sound.SoundEngine
import com.geomgang.game.ui.AchievementScreen
import com.geomgang.game.ui.BackupDialogMode
import com.geomgang.game.ui.BackupPasswordDialog
import com.geomgang.game.ui.CodexScreen
import com.geomgang.game.ui.CraftScreen
import com.geomgang.game.ui.ForgeScreen
import com.geomgang.game.ui.ForgeBackdrop
import com.geomgang.game.ui.ForgeSeasonTheme
import com.geomgang.game.ui.GauntletScreen
import com.geomgang.game.ui.HelpScreen
import com.geomgang.game.ui.HuntScreen
import com.geomgang.game.ui.LText
import com.geomgang.game.ui.LocalGameTranslator
import com.geomgang.game.ui.PetScreen
import com.geomgang.game.ui.QuestScreen
import com.geomgang.game.ui.RecordsMenuScreen
import com.geomgang.game.ui.SettingsScreen
import com.geomgang.game.ui.ShopScreen
import com.geomgang.game.ui.StarScreen
import com.geomgang.game.ui.StatsScreen
import com.geomgang.game.ui.StorageScreen
import com.geomgang.game.ui.TrainingScreen
import com.geomgang.game.ui.SwordForgeTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** 강화 화면 위에 무엇이 올라와 있는지. */
private enum class Overlay {
    None, Hunt, Gauntlet, Storage, Shop, Craft, Training, Star,
    Records, Codex, Quests, Pets, Achievements, Stats, Help, Settings,
}

/** 뒤로 가면 어디로 돌아가는지. 하위 화면은 자기를 연 화면으로 돌아간다. */
private fun Overlay.parent(): Overlay = when (this) {
    Overlay.Codex, Overlay.Pets, Overlay.Achievements, Overlay.Stats,
    Overlay.Help, Overlay.Settings,
    -> Overlay.Records
    // 별 강화는 단련에서 들어간다. 강화 화면으로 튕기면 왔던 길을 잃는다.
    Overlay.Star -> Overlay.Training
    else -> Overlay.None
}

/**
 * 모드는 하나다.
 *
 * 쉬움·일반·지옥을 없애고 상한 없는 무한 모드만 남겼다. 덕분에 모드 선택 화면이
 * 사라져 앱을 켜면 곧바로 강화 화면이다 — "시작까지 길다"는 문제가 같이 풀렸다.
 *
 * [Difficulty] 는 지우지 않았다. 확률표에 배수를 곱하는 장치는 그대로 쓸모가 있고
 * 테스트가 그 계산을 지키고 있다. 다만 게임이 쓰는 것은 [Difficulty.ENDLESS] 하나뿐이다.
 */
private val ONLY_MODE = Difficulty.ENDLESS

class MainActivity : ComponentActivity() {

    /**
     * 광고를 요청해도 되는 상태가 됐는지.
     *
     * 유럽 이용자는 동의 창을 닫아야 참이 된다. 그 전에는 배너를 붙이지 않는다.
     */
    private var adsReady by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AdConsent.start(this) { adsReady = true }
        // targetSdk 35 부터 **edge-to-edge 가 강제**다 - 앱이 상태바·내비게이션 바
        // 아래까지 그려진다. 끌 수 없으므로 명시적으로 켜 두고, 인셋은 아래에서 뺀다.
        val systemBarColor = Color.rgb(9, 9, 11)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(systemBarColor),
            navigationBarStyle = SystemBarStyle.dark(systemBarColor),
        )
        enterImmersiveMode()
        val saveStore = SaveStore(filesDir, secureSaveCodec())
        setContent {
            SwordForgeTheme {
                Surface {
                    App(saveStore, adsReady)
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enterImmersiveMode()
    }

    private fun enterImmersiveMode() {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            // 하단의 뒤로·홈·최근 앱 버튼은 항상 보이게 둔다. 숨겼다가 나타나는
            // 내비게이션 바는 게임 버튼 위에 겹치므로 상태바만 숨긴다.
            hide(WindowInsetsCompat.Type.statusBars())
        }
    }
}

@Composable
private fun App(store: SaveStore, adsReady: Boolean) {
    // 소리를 켤지는 ViewModel 의 설정을 그때그때 읽는다. 설정을 바꾸면 즉시 반영된다.
    val context = LocalContext.current
    val bgm = remember { BgmEngine(context.applicationContext) }
    val vm = remember {
        lateinit var holder: ForgeViewModel
        val engine = SoundEngine { holder.soundEnabled() }
        val feel = HapticEngine(systemVibrator(context)) { holder.hapticsEnabled() }
        holder = ForgeViewModel(
            store,
            ONLY_MODE,
            sound = engine,
            haptics = feel,
            saveDispatcher = Dispatchers.IO,
        )
        holder
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, vm, bgm) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> bgm.onForeground()
                Lifecycle.Event.ON_STOP -> {
                    bgm.onBackground()
                    vm.flushPendingSaves()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            bgm.onForeground()
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    DisposableEffect(vm, bgm) {
        onDispose {
            bgm.release()
            vm.dispose()
        }
    }
    val state by vm.ui.collectAsStateWithLifecycle()
    var overlay by remember { mutableStateOf(Overlay.None) }
    val bgmScene = if (overlay == Overlay.Hunt || overlay == Overlay.Gauntlet) {
        BgmScene.Hunt
    } else {
        BgmScene.Forge
    }
    SideEffect { bgm.update(bgmScene, state.settings.musicOn) }
    // 도감은 강화 화면과 기록 메뉴 두 곳에서 열린다. 들어온 곳으로 돌아가야 한다.
    var codexOrigin by remember { mutableStateOf(Overlay.Records) }
    // 상점에서 고른 계열. **화면 밖에 둬야** 나갔다 와도 고른 것이 남는다 —
    // 상점 안에 remember 로 두면 화면을 닫는 순간 첫 계열로 되돌아갔다.
    var shopFamily by rememberSaveable { mutableStateOf(WeaponFamily.STRAIGHT.name) }
    val backupTransfer = rememberBackupTransfer(vm, store)
    var backupDialog by remember { mutableStateOf<BackupDialogMode?>(null) }

    BackHandler(enabled = !state.busy) {
        when {
            // 사냥 중이면 먼저 사냥터 목록으로, 거기서 한 번 더 누르면 강화 화면으로
            state.hunt != null -> vm.leaveHunt()
            overlay == Overlay.Codex -> overlay = codexOrigin
            overlay != Overlay.None -> overlay = overlay.parent()
            else -> Unit
        }
    }

    val configuration = LocalConfiguration.current
    val localeSignature = configuration.locales.toLanguageTags()
    val gameLanguage = remember(state.settings.languageTag, localeSignature) {
        val preferred = (0 until configuration.locales.size()).map { configuration.locales[it] }
        GameLanguage.resolve(state.settings.languageTag, preferred)
    }
    val translator = remember(gameLanguage) {
        GameTranslator.load(context.assets, gameLanguage)
    }

    CompositionLocalProvider(LocalGameTranslator provides translator) {
        ForgeSeasonTheme(season = state.season) {
            ForgeBackdrop(
                bottomBar = {
                    // 사냥터와 무한 회랑은 화면을 쉬지 않고 두드리는 곳이다.
                    // 거기에 배너를 두면 잘못 눌러 광고로 튕겨 나간다.
                    val tapping = overlay == Overlay.Hunt || overlay == Overlay.Gauntlet
                    if (adsReady && !tapping) ForgeBannerAd()
                },
            ) {
                when (overlay) {
                    Overlay.Hunt -> HuntScreen(
                        state = state,
                        adventure = vm.adventure(),
                        onEnterGauntlet = {
                            vm.leaveHunt()
                            vm.enterGauntlet()
                            overlay = Overlay.Gauntlet
                        },
                        onEnterZone = vm::enterZone,
                        onTap = vm::tapTarget,
                        onChallengeBoss = vm::challengeBoss,
                        onTapNugget = vm::tapNugget,
                        onBuyMerchant = vm::buyMerchantOffer,
                        onGiveUpBoss = vm::giveUpBoss,
                        onStayInZone = vm::stayInZone,
                        onNextZone = vm::nextZone,
                        onLeave = vm::leaveHunt,
                        onBack = {
                            vm.leaveHunt()
                            overlay = Overlay.None
                        },
                    )

                    // 퀘스트 화면은 v2.1에서 숨겼다. Overlay.Quests 로 오는 길이 없지만
                    // enum 은 남겨 둔다 - 되살릴 때 화면과 길만 다시 잇는다.
                    Overlay.Quests -> QuestScreen(
                        state = state,
                        onClaim = vm::claimQuest,
                        onBack = { overlay = Overlay.None },
                    )

                    Overlay.Gauntlet -> GauntletScreen(
                        state = state,
                        onTap = vm::tapGauntlet,
                        onChoose = vm::chooseGauntlet,
                        onLeave = {
                            vm.leaveGauntlet()
                            overlay = Overlay.None
                        },
                    )

                    Overlay.Pets -> PetScreen(
                        state = state,
                        onEquip = vm::equipPet,
                        onBack = { overlay = Overlay.Records },
                    )

                    Overlay.Storage -> StorageScreen(
                        state = state,
                        onStore = vm::storeSword,
                        onEquip = vm::equipFromStorage,
                        onSell = vm::sellFromStorage,
                        onScrap = vm::scrapFromStorage,
                        onOffer = vm::offerFromStorage,
                        onOpenSource = {
                            overlay = if (state.deepUnlocked) Overlay.Hunt else Overlay.Shop
                        },
                        onBack = { overlay = Overlay.None },
                    )

                    Overlay.Shop -> ShopScreen(
                        state = state,
                        family = WeaponFamily.entries.firstOrNull { it.name == shopFamily }
                            ?: state.unlockedFamilies.first(),
                        onSelectFamily = { shopFamily = it.name },
                        onBuySword = vm::buySword,
                        onBuySwordToStorage = vm::buySwordToStorage,
                        onBuyStone = vm::buyStone,
                        onSellSword = vm::sellSword,
                        onBuyItem = vm::buyItem,
                        onBuyLegendPreventWithGold = vm::buyLegendPreventWithGold,
                        onBuyLegendPreventWithShards = vm::buyLegendPreventWithShards,
                        onCraft = { id, count, family -> vm.craft(id, count, family) },
                        onBack = { overlay = Overlay.None },
                    )

                    Overlay.Training -> TrainingScreen(
                        state = state,
                        onUpgradeSkill = vm::upgradeSkill,
                        onOpenStar = { overlay = Overlay.Star },
                        onBack = { overlay = Overlay.None },
                    )

                    Overlay.Star -> StarScreen(
                        state = state,
                        onStarUp = vm::starUp,
                        onBack = { overlay = Overlay.Training },
                    )

                    Overlay.Craft -> CraftScreen(
                        state = state,
                        onFuse = vm::fuse,
                        onRefine = vm::refine,
                        onCraftLegend = vm::craftLegend,
                        onRecraftLegend = vm::recraftLegend,
                        onBuyWard = vm::buyWardCharm,
                        onBack = { overlay = Overlay.None },
                    )

                    Overlay.Records -> RecordsMenuScreen(
                        progress = state.progress,
                        ownedPets = state.progress.petsFound.size,
                        deepUnlocked = state.deepUnlocked,
                        onOpenCodex = {
                            codexOrigin = Overlay.Records
                            overlay = Overlay.Codex
                        },
                        onOpenPets = { overlay = Overlay.Pets },
                        onOpenAchievements = { overlay = Overlay.Achievements },
                        onOpenHelp = { overlay = Overlay.Help },
                        onOpenStats = { overlay = Overlay.Stats },
                        onOpenSettings = { overlay = Overlay.Settings },
                        onBack = { overlay = Overlay.None },
                    )

                    Overlay.Codex -> CodexScreen(
                        progress = state.progress,
                        onBack = { overlay = codexOrigin },
                    )

                    Overlay.Achievements -> AchievementScreen(
                        progress = state.progress,
                        onSelectTitle = vm::selectTitle,
                        onBack = { overlay = Overlay.Records },
                    )

                    Overlay.Stats -> StatsScreen(
                        difficulty = state.difficulty,
                        progress = state.progress,
                        deepUnlocked = state.deepUnlocked,
                        onBack = { overlay = Overlay.Records },
                    )

                    Overlay.Help -> HelpScreen(
                        season = state.season,
                        onBack = { overlay = Overlay.Records },
                    )

                    Overlay.Settings -> SettingsScreen(
                        settings = state.settings,
                        deepUnlocked = state.deepUnlocked,
                        onAutoPreventChange = vm::setAutoPrevent,
                        onSoundChange = vm::setSoundOn,
                        onMusicChange = vm::setMusicOn,
                        onHapticsChange = vm::setHapticsOn,
                        onLanguageChange = vm::setLanguageTag,
                        backupBusy = backupTransfer.busy,
                        backupMessage = backupTransfer.message,
                        backupError = backupTransfer.error,
                        onRequestExportBackup = { backupDialog = BackupDialogMode.Export },
                        onRequestImportBackup = { backupDialog = BackupDialogMode.Import },
                        onReset = vm::resetProgress,
                        onBack = { overlay = Overlay.Records },
                    )

                    Overlay.None -> ForgeScreen(
                        state = state,
                        onForge = vm::forge,
                        onPrevent = vm::usePrevent,
                        onSalvage = vm::salvage,
                        onToggleBlessing = vm::toggleBlessing,
                        onToggleLuckCharm = vm::toggleLuckCharm,
                        onOpenHunt = { overlay = Overlay.Hunt },
                        onOpenStorage = { overlay = Overlay.Storage },
                        onOpenShop = { overlay = Overlay.Shop },
                        onOpenCraft = { overlay = Overlay.Craft },
                        onOpenCodex = {
                            codexOrigin = Overlay.None
                            overlay = Overlay.Codex
                        },
                        onOpenMenu = { overlay = Overlay.Records },
                        onDismissIdle = vm::dismissIdleReward,
                        onOpenTraining = { overlay = Overlay.Training },
                    )
                }
                state.saveSecurityMessage?.let { message ->
                    AlertDialog(
                        onDismissRequest = {},
                        title = { LText("세이브 보호 작동") },
                        text = { LText(message) },
                        confirmButton = {
                            TextButton(onClick = vm::dismissSaveSecurityMessage) {
                                LText("확인")
                            }
                        },
                    )
                }
                backupDialog?.let { mode ->
                    BackupPasswordDialog(
                        mode = mode,
                        onDismiss = { backupDialog = null },
                        onConfirm = { password ->
                            backupDialog = null
                            if (mode == BackupDialogMode.Export) {
                                backupTransfer.onExport(password)
                            } else {
                                backupTransfer.onImport(password)
                            }
                        },
                    )
                }
            }
        }
    }
}

private data class BackupTransferUi(
    val busy: Boolean,
    val message: String?,
    val error: Boolean,
    val onExport: (String) -> Unit,
    val onImport: (String) -> Unit,
)

/** 시스템 파일 선택기와 저장 계층 사이를 잇는다. 저장소 권한은 요청하지 않는다. */
@Composable
private fun rememberBackupTransfer(
    vm: ForgeViewModel,
    store: SaveStore,
): BackupTransferUi {
    val context = LocalContext.current
    val translator = LocalGameTranslator.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf(false) }
    var exportPassword by remember { mutableStateOf<CharArray?>(null) }
    var importPassword by remember { mutableStateOf<CharArray?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(BACKUP_MIME),
    ) { uri ->
        val password = exportPassword
        exportPassword = null
        if (uri == null || password == null) {
            password?.fill('\u0000')
            return@rememberLauncherForActivityResult
        }
        busy = true
        message = null
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val backup = store.exportPortableBackup(password)
                    try {
                        context.contentResolver.openOutputStream(uri, "w")?.use { output ->
                            output.write(backup)
                            output.flush()
                        } ?: throw IllegalStateException("cannot open backup destination")
                    } finally {
                        backup.fill(0)
                    }
                }
                error = false
                message = "암호화 백업을 저장했다."
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                error = true
                message = "내보내지 못했다. 비밀번호와 저장 위치를 확인해 주세요."
            } finally {
                password.fill('\u0000')
                busy = false
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        val password = importPassword
        importPassword = null
        if (uri == null || password == null) {
            password?.fill('\u0000')
            return@rememberLauncherForActivityResult
        }
        busy = true
        message = null
        vm.preparePortableImport()
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val backup = readBackupBytes(context, uri)
                    try {
                        store.importPortableBackup(backup, password)
                    } finally {
                        backup.fill(0)
                    }
                }
                error = false
                message = "백업을 불러왔다. 게임을 다시 여는 중..."
                Toast.makeText(
                    context,
                    translator.translate("백업 복원 완료"),
                    Toast.LENGTH_SHORT,
                ).show()
                context.findActivity()?.recreate()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                error = true
                message = "가져오지 못했다. 비밀번호가 틀렸거나 백업 파일이 손상됐다."
            } finally {
                password.fill('\u0000')
                busy = false
            }
        }
    }

    return BackupTransferUi(
        busy = busy,
        message = message,
        error = error,
        onExport = { password ->
            if (!busy && vm.preparePortableBackup()) {
                exportPassword?.fill('\u0000')
                exportPassword = password.toCharArray()
                val stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"))
                val prefix = translator.translate("검강화_백업_")
                exportLauncher.launch("$prefix$stamp.sfgbackup")
            }
        },
        onImport = { password ->
            if (!busy) {
                importPassword?.fill('\u0000')
                importPassword = password.toCharArray()
                importLauncher.launch(arrayOf(BACKUP_MIME, "application/octet-stream"))
            }
        },
    )
}

private fun readBackupBytes(context: Context, uri: Uri): ByteArray {
    val input = context.contentResolver.openInputStream(uri)
        ?: throw IllegalStateException("cannot open backup")
    return input.use {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(16 * 1024)
        var total = 0
        while (true) {
            val read = it.read(buffer)
            if (read < 0) break
            total += read
            if (total > SaveStore.PORTABLE_BACKUP_MAX_BYTES) {
                throw IllegalArgumentException("backup is too large")
            }
            output.write(buffer, 0, read)
        }
        output.toByteArray()
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private const val BACKUP_MIME = "application/vnd.swordforge.backup"
